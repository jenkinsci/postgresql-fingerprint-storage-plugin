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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Fingerprint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class JSONHandler {

    private static String serialize(Fingerprint.RangeSet src) {
        StringBuilder buf = new StringBuilder(src.getRanges().size() * 10);
        for (Fingerprint.Range r : src.getRanges()) {
            if(buf.length() > 0) {
                buf.append(',');
            }
            if(r.isSingle()) {
                buf.append(r.getStart());
            } else {
                buf.append(r.getStart()).append('-').append(r.getEnd() - 1);
            }
        }
        return buf.toString();
    }

    static String constructFingerprintJSON(Map<String, String> fingerprintMetadata,
                                           Map<String, Fingerprint.RangeSet> usageMetadata,
                                           JSONArray facets) {
        JSONObject json = new JSONObject();
        JSONObject fingerprint = new JSONObject();
        JSONArray md5sum = new JSONArray();
        JSONArray usages = new JSONArray();

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
                jobAndBuild.put("ranges", serialize(usage.getValue()));

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

        json.put("fingerprint", fingerprint);

        return json.toString();
    }

    static @NonNull Map<String, Fingerprint.RangeSet> extractUsageMetadata(@NonNull ResultSet resultSet)
            throws SQLException {
        Map<String, Fingerprint.RangeSet> usageMetadata = new HashMap<>();

        while (resultSet.next()) {
            String jobName = resultSet.getString("job");
            int build = resultSet.getInt("build");

            if (usageMetadata.containsKey(jobName)) {
                usageMetadata.get(jobName).add(build);
            } else {
                Fingerprint.RangeSet rangeSet = new Fingerprint.RangeSet();
                rangeSet.add(build);
                usageMetadata.put(jobName, rangeSet);
            }
        }

        return Collections.unmodifiableMap(usageMetadata);
    }

    static @NonNull Map<String,String> extractFingerprintMetadata(@NonNull ResultSet resultSet, @NonNull String id)
            throws SQLException {
        Map<String, String> fingerprintMetadata = new HashMap<>();

        while (resultSet.next()) {
            String timestamp = resultSet.getString("timestamp");
            String filename = resultSet.getString("filename");

            fingerprintMetadata.put("timestamp", timestamp);
            fingerprintMetadata.put("filename", filename);
            fingerprintMetadata.put("id", id);
        }

        return Collections.unmodifiableMap(fingerprintMetadata);
    }

    static @NonNull JSONArray extractFacets(@NonNull ResultSet resultSet) throws SQLException {
        JSONArray facets = new JSONArray();

        while (resultSet.next()) {
            String facetJSONString = resultSet.getString("facet");
            if (facetJSONString.equals("")) {
                break;
            }
            JSONObject facet = new JSONObject(facetJSONString);
            facets.put(facet);
        }

        return facets;
    }

    static @NonNull Map<String, List<String>> extractFacets(@NonNull Fingerprint fingerprint) throws SQLException {
        Map<String, List<String>> facets = new HashMap<>();

        JSONObject fingerprintJSON = new JSONObject(XStreamHandler.getXStream().toXML(fingerprint))
                .getJSONObject("fingerprint");

        JSONArray facetsJSON = fingerprintJSON.getJSONArray("facets");

        System.out.println(facetsJSON.get(0));

        if (facetsJSON.get(0).equals("")) {
            return Collections.emptyMap();
        }

        for (Object facetObject : facetsJSON.toList()) {
            JSONObject facet = (JSONObject) facetObject;

            for (String facetName : facet.keySet()) {
                JSONArray facetEntries = facet.getJSONArray(facetName);
                List<String> facetEntriesList = new ArrayList<>();

                for (Object facetEntryObject : facetEntries) {
                    JSONObject facetEntry = (JSONObject) facetEntryObject;
                    facetEntriesList.add(facetEntry.toString());
                }

                facets.put(facetName, Collections.unmodifiableList(facetEntriesList));
            }

        }

        return Collections.unmodifiableMap(facets);
    }

}
