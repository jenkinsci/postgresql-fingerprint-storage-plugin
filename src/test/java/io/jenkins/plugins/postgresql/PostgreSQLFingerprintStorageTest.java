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

import hudson.Util;
import hudson.model.Fingerprint;
import jenkins.fingerprints.FingerprintStorage;
import jenkins.model.FingerprintFacet;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class PostgreSQLFingerprintStorageTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public PostgreSQLContainer postgres = new PostgreSQLContainer();

    public void setConfiguration() throws IOException {
        PostgreSQLConfiguration.setConfiguration(postgres.getUsername(), postgres.getPassword(), postgres.getHost(),
                postgres.getFirstMappedPort(), postgres.getDatabaseName());
    }

    @Test
    public void checkFingerprintStorageIsPostgreSQL() throws IOException {
        setConfiguration();
        Object fingerprintStorage = FingerprintStorage.get();
        assertThat(fingerprintStorage, instanceOf(PostgreSQLFingerprintStorage.class));
    }

    @Test
    public void roundTrip() throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("roundTrip");

        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.add("bar", 3);

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(nullValue())));
        assertThat(fingerprintSaved.toString(), is(equalTo(fingerprintLoaded.toString())));
    }

    @Test
    public void roundTripWithUsages() throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("roundTrip");

        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.add("a", 3);
        fingerprintSaved.add("b", 33);
        fingerprintSaved.add("c", 333);

        System.out.println(XStreamHandler.getXStream().toXML(fingerprintSaved));

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(nullValue())));
        assertThat(fingerprintSaved.toString(), is(equalTo(fingerprintLoaded.toString())));
    }

    @Test
    public void roundTripWithFacets() throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("roundTrip");

        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.getPersistedFacets().add(new TestFacet(fingerprintSaved, 3, "a"));
        fingerprintSaved.getPersistedFacets().add(new TestFacet(fingerprintSaved, 33, "b"));
        fingerprintSaved.getPersistedFacets().add(new TestFacet(fingerprintSaved, 333, "c"));

        System.out.println("We want to save:");
        System.out.println(XStreamHandler.getXStream().toXML(fingerprintSaved));

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(nullValue())));
        assertThat(fingerprintSaved.toString(), is(equalTo(fingerprintLoaded.toString())));
    }

    @Test
    public void loadingNonExistentFingerprintShouldReturnNull() throws IOException{
        setConfiguration();
        String id = Util.getDigestOf("loadingNonExistentFingerprintShouldReturnNull");
        Fingerprint fingerprint = Fingerprint.load(id);
        assertThat(fingerprint, is(nullValue()));
    }

    @Test
    public void shouldDeleteFingerprint() throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("shouldDeleteFingerprint");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        Fingerprint.delete(id);
        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(nullValue()));
        Fingerprint.delete(id);
        fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(nullValue()));
    }

    @Test
    public void testIsReady() throws IOException {
        setConfiguration();
        FingerprintStorage fingerprintStorage = FingerprintStorage.get();
        assertThat(fingerprintStorage.isReady(), is(false));
        String id = Util.getDigestOf("testIsReady");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        assertThat(fingerprintStorage.isReady(), is(true));
    }

    public static final class TestFacet extends FingerprintFacet {
        final String property;

        public TestFacet(Fingerprint fingerprint, long timestamp, String property) {
            super(fingerprint, timestamp);
            this.property = property;
        }

        @Override public String toString() {
            return "TestFacet[" + property + "@" + getTimestamp() + "]";
        }
    }

}
