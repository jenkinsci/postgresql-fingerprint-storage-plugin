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
import hudson.Util;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DataConversionTest {

    private static final DateConverter DATE_CONVERTER = new DateConverter();
    public static final String FINGERPRINT_ID = Util.getDigestOf("FINGERPRINT_ID");
    public static final Timestamp TIMESTAMP = new Timestamp(new Date().getTime());
    public static final String FILENAME = "FILENAME";
    public static final String JOB = "JOB";
    public static final int BUILD_NUMBER = 3;

    @Test
    public void testConstructFingerprintJSON() {
        // TODO
    }

    @Test
    public void testExtractFingerprintMetadata() {
        Map<String,String> fingerprintMetadata = DataConversion.extractFingerprintMetadata(
                FINGERPRINT_ID, TIMESTAMP, FILENAME, JOB, String.valueOf(BUILD_NUMBER));
        assertThat(fingerprintMetadata.get(DataConversion.ID), is(equalTo(FINGERPRINT_ID)));
        assertThat(fingerprintMetadata.get(DataConversion.TIMESTAMP), is(equalTo(DATE_CONVERTER.toString(
                new Date(TIMESTAMP.getTime())))));
        assertThat(fingerprintMetadata.get(DataConversion.FILENAME), is(equalTo(FILENAME)));
        assertThat(fingerprintMetadata.get(DataConversion.ORIGINAL_JOB_NAME), is(equalTo(JOB)));
        assertThat(fingerprintMetadata.get(DataConversion.ORIGINAL_JOB_BUILD_NUMBER), is(equalTo(String.valueOf(
                BUILD_NUMBER))));

        fingerprintMetadata = DataConversion.extractFingerprintMetadata(
                FINGERPRINT_ID, TIMESTAMP, FILENAME, null, null);
        assertThat(fingerprintMetadata.get(DataConversion.ID), is(equalTo(FINGERPRINT_ID)));
        assertThat(fingerprintMetadata.get(DataConversion.TIMESTAMP), is(equalTo(DATE_CONVERTER.toString(new Date(
                TIMESTAMP.getTime())))));
        assertThat(fingerprintMetadata.get(DataConversion.FILENAME), is(equalTo(FILENAME)));
        assertThat(fingerprintMetadata.get(DataConversion.ORIGINAL_JOB_NAME), is(nullValue()));
        assertThat(fingerprintMetadata.get(DataConversion.ORIGINAL_JOB_BUILD_NUMBER), is(nullValue()));
    }

    @Test
    public void testExtractUsageMetadata() {
        // TODO
    }

    @Test
    public void testExtractFacets() {
        // TODO
    }
}
