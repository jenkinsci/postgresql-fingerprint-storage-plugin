/*
 * The MIT License
 *
 * Copyright (c) 2020, Jenkins project contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.postgresql;

import com.thoughtworks.xstream.converters.basic.DateConverter;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Fingerprint;
import jenkins.fingerprints.FileFingerprintStorage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Helper class for handling converting of data from and to different data structures.
 */
@Restricted({NoExternalUse.class})
public class DataConversion {

    private static final Logger LOGGER = Logger.getLogger(DataConversion.class.getName());
    private static final DateConverter DATE_CONVERTER = new DateConverter();

    /**
     * Constructs the JSON for fingerprint from the given metadata about the fingerprint fetched from
     * PostgreSQL.
     * @param fingerprintMetadata See {@link DataConversion#extractFingerprintMetadata(String, Timestamp, String, String, String)}
     * @param usageMetadata See {@link DataConversion#extractUsageMetadata(String)}
     * @param facets See {@link DataConversion#extractFacets(String)}
     * @return
     */
    static @NonNull String constructFingerprintJSON(@NonNull Map<String, String> fingerprintMetadata,
                                           @NonNull Map<String, Fingerprint.RangeSet> usageMetadata,
                                           @NonNull JSONArray facets) {
        JSONObject json = new JSONObject();
        JSONObject fingerprint = new JSONObject();
        JSONArray md5sum = new JSONArray();
        JSONArray usages = new JSONArray();
        JSONObject original = null;

        if (fingerprintMetadata.get("original_job_build") != null) {
            original = new JSONObject();
            original.put("name", fingerprintMetadata.get("original_job_name"));
            original.put("number", Integer.parseInt(fingerprintMetadata.get("original_job_build")));
        }

        md5sum.put(fingerprintMetadata.get("id"));

        if (facets.length() == 0) {
            facets.put("");
        }

        if (usageMetadata.size() != 0) {
            JSONObject entry = new JSONObject();
            JSONArray entryArray = new JSONArray();

            for (Map.Entry<String, Fingerprint.RangeSet> usage : usageMetadata.entrySet()) {
                JSONObject jobAndBuild = new JSONObject();
                jobAndBuild.put("string", usage.getKey());
                jobAndBuild.put("ranges", FileFingerprintStorage.serialize(usage.getValue()));

                entryArray.put(jobAndBuild);
            }

            entry.put("entry", entryArray);
            usages.put(entry);
        } else {
            usages.put("");
        }

        fingerprint.put("timestamp", fingerprintMetadata.get("timestamp"));
        fingerprint.put("fileName", fingerprintMetadata.get("filename"));
        fingerprint.put("md5sum", md5sum);
        fingerprint.put("facets", facets);
        fingerprint.put("usages", usages);
        fingerprint.put("original", original);

        json.put("fingerprint", fingerprint);

        LOGGER.fine("Fingerprint loaded: " + json.toString());
        return json.toString();
    }

    /**
     * Store Fingerprint metadata into a Map.
     */
    static @NonNull Map<String,String> extractFingerprintMetadata(@NonNull String id,
                                                                  Timestamp timestamp,
                                                                  @NonNull String filename,
                                                                  @CheckForNull String originalJobName,
                                                                  @CheckForNull String originalJobBuild) {
        Map<String, String> fingerprintMetadata = new HashMap<>();

        fingerprintMetadata.put("timestamp", DATE_CONVERTER.toString(new Date(timestamp.getTime())));
        fingerprintMetadata.put("filename", filename);
        fingerprintMetadata.put("id", id);
        fingerprintMetadata.put("original_job_name", originalJobName);
        fingerprintMetadata.put("original_job_build", originalJobBuild);

        return Collections.unmodifiableMap(fingerprintMetadata);
    }

    /**
     * Extracts the fingerprint's usage metadata (jobs and builds) obtained from PostgreSQL.
     */
    static @NonNull Map<String, Fingerprint.RangeSet> extractUsageMetadata(@CheckForNull String usagesAsJSONString) {
        Map<String, Fingerprint.RangeSet> usageMetadata = new HashMap<>();

        if (usagesAsJSONString != null) {
            JSONArray usages = new JSONArray(usagesAsJSONString);

            for (int i = 0; i < usages.length(); i++) {
                JSONObject usage = usages.getJSONObject(i);

                String jobName = usage.getString("job");
                int build = usage.getInt("build");

                if (usageMetadata.containsKey(jobName)) {
                    usageMetadata.get(jobName).add(build);
                } else {
                    Fingerprint.RangeSet rangeSet = new Fingerprint.RangeSet();
                    rangeSet.add(build);
                    usageMetadata.put(jobName, rangeSet);
                }
            }
        }
        
        return Collections.unmodifiableMap(usageMetadata);
    }

    /**
     * Extracts the fingerprint's facet metadata obtained from PostgreSQL in the form of {@link ResultSet}.
     */
    static @NonNull JSONArray extractFacets(@CheckForNull String facetsAsJSONString) {
        JSONArray facetsArray = new JSONArray();
        JSONObject facetsObject = new JSONObject();

        if (facetsAsJSONString != null) {
            JSONArray facetsFromResultSet = new JSONArray(facetsAsJSONString);

            for (int i = 0; i < facetsFromResultSet.length(); i++) {
                JSONObject facetFromResultSet = facetsFromResultSet.getJSONObject(i);
                String facetName = facetFromResultSet.getString("facet_name");
                if (facetName.equals("")) {
                    break;
                }

                if (facetsObject.has(facetName)) {
                    facetsObject.getJSONArray(facetName).put(facetFromResultSet.getJSONObject("facet_entry"));
                } else {
                    JSONArray facetEntries = new JSONArray();
                    facetEntries.put(facetFromResultSet.getJSONObject("facet_entry"));
                    facetsObject.put(facetName, facetEntries);
                }
            }
        }

        facetsArray.put(facetsObject);
        return facetsArray;
    }

    /**
     * Extracts the given fingerprint's facets in the form of JSON Strings.
     */
    static @NonNull Map<String, List<String>> extractFacets(@NonNull Fingerprint fingerprint) throws SQLException {
        Map<String, List<String>> facetsMap = new HashMap<>();

        JSONObject fingerprintJSON = new JSONObject(XStreamHandler.getXStream().toXML(fingerprint))
                .getJSONObject("fingerprint");

        JSONArray facetsJSON = fingerprintJSON.getJSONArray("facets");

        if (facetsJSON.get(0).equals("")) {
            return Collections.emptyMap();
        }

        for (int i = 0; i < facetsJSON.length(); i++) {
            JSONObject facet = facetsJSON.getJSONObject(i);

            for (String facetName : facet.keySet()) {
                List<String> facetEntriesList = new ArrayList<>();
                Object facetEntryObject = facet.get(facetName);

                if (facetEntryObject instanceof JSONObject) {
                    JSONObject facetEntry = (JSONObject) facetEntryObject;
                    facetEntriesList.add(facetEntry.toString());
                } else if (facetEntryObject instanceof JSONArray) {
                    JSONArray facetEntries = (JSONArray) facetEntryObject;

                    for (int j = 0; j < facetEntries.length(); j++) {
                        JSONObject facetEntry = facetEntries.getJSONObject(j);
                        facetEntriesList.add(facetEntry.toString());
                    }
                } else {
                    throw new SQLException("Unrecognized JSON Structure.");
                }

                facetsMap.put(facetName, Collections.unmodifiableList(facetEntriesList));
            }
        }

        return Collections.unmodifiableMap(facetsMap);
    }

}
