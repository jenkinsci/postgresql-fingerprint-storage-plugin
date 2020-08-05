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

    static final String FINGERPRINT = "fingerprint";
    static final String RANGES = "ranges";
    static final String RANGE = "range";
    static final String ID = "id";
    static final String TIMESTAMP = "timestamp";
    static final String FILENAME = "fileName";
    static final String MD5SUM = "md5sum";
    static final String FACETS = "facets";
    static final String USAGES = "usages";
    static final String ORIGINAL = "original";
    static final String ORIGINAL_JOB_BUILD = "original_job_build";
    static final String ORIGINAL_JOB_NAME = "original_job_name";
    static final String NAME = "name";
    static final String NUMBER = "number";
    static final String STRING = "string";
    static final String ENTRY = "entry";
    static final String JOB = "job";
    static final String BUILD = "build";
    static final String FACET_NAME = "facet_name";
    static final String FACET_ENTRY = "facet_entry";

    static final String EMPTY_STRING = "";

    private static final String UNRECOGNIZED_JSON_STRUCTURE = "Unrecognized JSON Structure";

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

        if (fingerprintMetadata.get(ORIGINAL_JOB_BUILD) != null) {
            original = new JSONObject();
            original.put(NAME, fingerprintMetadata.get(ORIGINAL_JOB_NAME));
            original.put(NUMBER, Integer.parseInt(fingerprintMetadata.get(ORIGINAL_JOB_BUILD)));
        }

        md5sum.put(fingerprintMetadata.get(ID));

        if (facets.length() == 0) {
            facets.put(EMPTY_STRING);
        }

        if (usageMetadata.size() != 0) {
            JSONObject entry = new JSONObject();
            JSONArray entryArray = new JSONArray();

            for (Map.Entry<String, Fingerprint.RangeSet> usage : usageMetadata.entrySet()) {
                JSONObject jobAndBuild = new JSONObject();
                jobAndBuild.put(STRING, usage.getKey());
                jobAndBuild.put(RANGES, FileFingerprintStorage.serialize(usage.getValue()));

                entryArray.put(jobAndBuild);
            }

            entry.put(ENTRY, entryArray);
            usages.put(entry);
        } else {
            usages.put(EMPTY_STRING);
        }

        fingerprint.put(TIMESTAMP, fingerprintMetadata.get(TIMESTAMP));
        fingerprint.put(FILENAME, fingerprintMetadata.get(FILENAME));
        fingerprint.put(MD5SUM, md5sum);
        fingerprint.put(FACETS, facets);
        fingerprint.put(USAGES, usages);
        fingerprint.put(ORIGINAL, original);

        json.put(FINGERPRINT, fingerprint);

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

        fingerprintMetadata.put(TIMESTAMP, DATE_CONVERTER.toString(new Date(timestamp.getTime())));
        fingerprintMetadata.put(FILENAME, filename);
        fingerprintMetadata.put(ID, id);
        fingerprintMetadata.put(ORIGINAL_JOB_NAME, originalJobName);
        fingerprintMetadata.put(ORIGINAL_JOB_BUILD, originalJobBuild);

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

                String jobName = usage.getString(JOB);
                int build = usage.getInt(BUILD);

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
                String facetName = facetFromResultSet.getString(FACET_NAME);
                if (facetName.equals(EMPTY_STRING)) {
                    break;
                }

                if (facetsObject.has(facetName)) {
                    facetsObject.getJSONArray(facetName).put(facetFromResultSet.getJSONObject(FACET_ENTRY));
                } else {
                    JSONArray facetEntries = new JSONArray();
                    facetEntries.put(facetFromResultSet.getJSONObject(FACET_ENTRY));
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
                .getJSONObject(FINGERPRINT);

        JSONArray facetsJSON = fingerprintJSON.getJSONArray(FACETS);

        if (facetsJSON.get(0).equals(EMPTY_STRING)) {
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
                    throw new SQLException(UNRECOGNIZED_JSON_STRUCTURE);
                }

                facetsMap.put(facetName, Collections.unmodifiableList(facetEntriesList));
            }
        }

        return Collections.unmodifiableMap(facetsMap);
    }

}
