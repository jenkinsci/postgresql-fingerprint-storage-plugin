/*
 * The MIT License
 *
 * Copyright (c) 2023, Jenkins project contributors
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import hudson.Util;
import hudson.model.Fingerprint;
import hudson.util.Secret;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jenkins.fingerprints.FingerprintStorage;
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import jenkins.model.FingerprintFacet;
import org.hamcrest.Matchers;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jenkinsci.plugins.database.postgresql.PostgreSQLDatabase;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@WithJenkins
@Testcontainers
public class PostgreSQLFingerprintStorageTest {

    @Container
    public PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PostgreSQLContainer.IMAGE);

    public void setConfiguration() throws IOException {
        PostgreSQLDatabase database = new PostgreSQLDatabase(
                postgres.getHost() + ":" + postgres.getMappedPort(5432),
                postgres.getDatabaseName(),
                postgres.getUsername(),
                Secret.fromString(postgres.getPassword()),
                null);
        database.setValidationQuery("SELECT 1");
        GlobalDatabaseConfiguration.get().setDatabase(database);
        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();
        GlobalFingerprintConfiguration.get().setStorage(postgreSQLFingerprintStorage);
        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);
    }

    @Test
    public void checkFingerprintStorageIsPostgreSQL(JenkinsRule j) throws IOException {
        setConfiguration();
        Object fingerprintStorage = FingerprintStorage.get();
        assertThat(fingerprintStorage, instanceOf(PostgreSQLFingerprintStorage.class));
    }

    @Test
    public void testSave(JenkinsRule j) throws IOException, SQLException {
        setConfiguration();

        String instanceId = Util.getDigestOf(
                new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        String id = Util.getDigestOf("testSave");
        Fingerprint fingerprint = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprint.add("a", 3);
        fingerprint.getPersistedFacets().add(new TestFacet(fingerprint, 3, "a"));

        try (Connection connection =
                PostgreSQLFingerprintStorage.get().getConnectionSupplier().connection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.SELECT_FINGERPRINT))) {
                preparedStatement.setString(1, id);
                preparedStatement.setString(2, instanceId);
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(
                        resultSet.getTimestamp(ColumnName.TIMESTAMP).getTime(),
                        is(fingerprint.getTimestamp().getTime()));
                assertThat(resultSet.getString(ColumnName.FILENAME), is(fingerprint.getFileName()));
                assertThat(resultSet.getString(ColumnName.ORIGINAL_JOB_NAME), is(nullValue()));
                assertThat(resultSet.getString(ColumnName.ORIGINAL_JOB_BUILD_NUMBER), is(nullValue()));
                assertThat(
                        resultSet.getString(ColumnName.USAGES),
                        is(equalTo("[{\"job\" : \"a\", \"build_number\" : 3}]")));
                assertThat(
                        resultSet.getString(ColumnName.FACETS),
                        is(equalTo("[{"
                                + "\"facet_name\" : \"io.jenkins.plugins.postgresql.PostgreSQLFingerprintStorageTest$TestFacet\", "
                                + "\"facet_entry\" : {\"property\": \"a\", \"timestamp\": 3}}]")));
            }
        }
    }

    @Test
    public void roundTripEmptyFingerprint(JenkinsRule j) throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("roundTrip");

        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(Matchers.nullValue())));
        assertThat(fingerprintSaved.toString(), is(Matchers.equalTo(fingerprintLoaded.toString())));
    }

    @Test
    public void roundTripWithMultipleFingerprints(JenkinsRule j) throws IOException {
        setConfiguration();

        String[] fingerprintIds = {
            Util.getDigestOf("id1"), Util.getDigestOf("id2"), Util.getDigestOf("id3"),
        };

        List<Fingerprint> savedFingerprints = new ArrayList<>();

        for (String fingerprintId : fingerprintIds) {
            Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(fingerprintId));
            fingerprintSaved.add(fingerprintId, 3);
            fingerprintSaved.getPersistedFacets().add(new TestFacet(fingerprintSaved, 3, fingerprintId));
            savedFingerprints.add(fingerprintSaved);
        }

        for (Fingerprint fingerprintSaved : savedFingerprints) {
            Fingerprint fingerprintLoaded = Fingerprint.load(fingerprintSaved.getHashString());
            assertThat(fingerprintLoaded, is(not(Matchers.nullValue())));
            assertThat(fingerprintLoaded.toString(), is(Matchers.equalTo(fingerprintSaved.toString())));
        }
    }

    @Test
    public void roundTripWithMultipleUsages(JenkinsRule j) throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("roundTripWithUsages");

        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.add("a", 3);
        fingerprintSaved.add("b", 33);
        fingerprintSaved.add("c", 333);

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(Matchers.nullValue())));
        assertThat(fingerprintLoaded.toString(), is(Matchers.equalTo(fingerprintSaved.toString())));
    }

    @Test
    public void roundTripWithMultipleFacets(JenkinsRule j) throws IOException {
        String id = Util.getDigestOf("roundTripWithFacets");

        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.getPersistedFacets().add(new TestFacet(fingerprintSaved, 3, "a"));
        fingerprintSaved.getPersistedFacets().add(new TestFacet(fingerprintSaved, 33, "b"));
        fingerprintSaved.getPersistedFacets().add(new TestFacet(fingerprintSaved, 333, "c"));

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(Matchers.nullValue())));
        assertThat(fingerprintLoaded.getHashString(), is(Matchers.equalTo(fingerprintSaved.getHashString())));
        assertThat(fingerprintLoaded.getFileName(), is(Matchers.equalTo(fingerprintSaved.getFileName())));
        assertThat(fingerprintLoaded.getTimestamp(), is(Matchers.equalTo(fingerprintSaved.getTimestamp())));
        assertThat(
                fingerprintSaved.getPersistedFacets(),
                Matchers.containsInAnyOrder(
                        fingerprintLoaded.getPersistedFacets().toArray()));
    }

    @Test
    public void loadingNonExistentFingerprintShouldReturnNull(JenkinsRule j) throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("loadingNonExistentFingerprintShouldReturnNull");
        Fingerprint fingerprint = Fingerprint.load(id);
        assertThat(fingerprint, is(Matchers.nullValue()));
    }

    @Test
    public void shouldDeleteFingerprint(JenkinsRule j) throws IOException {
        setConfiguration();
        String id = Util.getDigestOf("shouldDeleteFingerprint");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        Fingerprint.delete(id);
        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(Matchers.nullValue()));
        Fingerprint.delete(id);
        fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(Matchers.nullValue()));
    }

    @Test
    public void testIsReady(JenkinsRule j) throws IOException {
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

        @Override
        public String toString() {
            return "TestFacet[" + property + "@" + getTimestamp() + "]";
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }

            if (!(object instanceof TestFacet)) {
                return false;
            }

            TestFacet testFacet = (TestFacet) object;
            return this.toString().equals(testFacet.toString());
        }
    }
}
