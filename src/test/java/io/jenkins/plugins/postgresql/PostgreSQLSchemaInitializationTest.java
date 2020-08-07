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
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class PostgreSQLSchemaInitializationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public PostgreSQLContainer postgres = new PostgreSQLContainer();

    @Before
    public void setConfiguration() throws Exception {
        PostgreSQLConfiguration.setConfiguration(postgres.getUsername(), postgres.getPassword(), postgres.getHost(),
                postgres.getFirstMappedPort(), postgres.getDatabaseName());
    }

    @Test
    public void testSchemaInitialization() throws Exception {
        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();
        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);

        try (Connection connection = PostgreSQLConnection.getConnection(PostgreSQLFingerprintStorage.get())) {

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CHECK_SCHEMA_EXISTS))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt(ColumnName.TOTAL), is(1));
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CHECK_FINGERPRINT_TABLE_EXISTS))) {
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
    public void testSchemaIntializationDoesNotDeleteData() throws Exception {
        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();
        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);

        String id = Util.getDigestOf("testSchemaIntializationDoesNotDeleteData");
        Fingerprint fingerprintSaved = new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        fingerprintSaved.add(id, 3);
        fingerprintSaved.getPersistedFacets().add(new PostgreSQLFingerprintStorageTest.TestFacet(
                fingerprintSaved, 3, id));

        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);

        Fingerprint fingerprintLoaded = Fingerprint.load(id);
        assertThat(fingerprintLoaded, is(not(Matchers.nullValue())));
        assertThat(fingerprintLoaded.toString(), is(Matchers.equalTo(fingerprintSaved.toString())));
    }

}
