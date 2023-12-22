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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import hudson.Util;
import hudson.model.Fingerprint;
import hudson.util.Secret;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import org.hamcrest.Matchers;
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
public class PostgreSQLSchemaInitializationTest {

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
    public void testSchemaInitialization(JenkinsRule rule) throws Exception {
        setConfiguration();
        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();

        try (Connection connection =
                postgreSQLFingerprintStorage.getConnectionSupplier().connection()) {

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.CHECK_FINGERPRINT_TABLE_EXISTS))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt(ColumnName.TOTAL), is(1));
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CHECK_FINGERPRINT_JOB_BUILD_RELATION_TABLE_EXISTS))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt(ColumnName.TOTAL), is(1));
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CHECK_FINGERPRINT_FACET_RELATION_TABLE_EXISTS))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt(ColumnName.TOTAL), is(1));
            }
        }
    }

    @Test
    public void testSchemaIntializationDoesNotDeleteData(JenkinsRule rule) throws Exception {
        setConfiguration();
        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();

        String id = Util.getDigestOf("testSchemaIntializationDoesNotDeleteData");
        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.add(id, 3);
        fingerprintSaved
                .getPersistedFacets()
                .add(new PostgreSQLFingerprintStorageTest.TestFacet(fingerprintSaved, 3, id));

        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(Matchers.nullValue())));
        assertThat(fingerprintLoaded.toString(), is(Matchers.equalTo(fingerprintSaved.toString())));
    }

    @Test
    public void testSchemaIntializationTwice(JenkinsRule rule) throws Exception {
        setConfiguration();
        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();

        String id = Util.getDigestOf("testSchemaIntializationDoesNotDeleteData");
        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.add(id, 3);
        fingerprintSaved
                .getPersistedFacets()
                .add(new PostgreSQLFingerprintStorageTest.TestFacet(fingerprintSaved, 3, id));

        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);
        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(Matchers.nullValue())));
        assertThat(fingerprintLoaded.toString(), is(Matchers.equalTo(fingerprintSaved.toString())));
    }
}
