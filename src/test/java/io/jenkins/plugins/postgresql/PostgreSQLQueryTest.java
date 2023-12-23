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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import hudson.Util;
import hudson.util.Secret;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jenkinsci.plugins.database.postgresql.PostgreSQLDatabase;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@WithJenkins
@Testcontainers
public class PostgreSQLQueryTest {

    public static final Timestamp TIMESTAMP = new Timestamp(new Date().getTime());
    private static final String INSTANCE_ID = "185d72052231445badce445130a11414";
    private static final String FINGERPRINT_FILENAME = "foo.jar";
    private static final String FINGERPRINT_ID = Util.getDigestOf("id");
    private static final String JOB_NAME = "Random Job";
    private static final int BUILD_NUMBER = 3;

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
        DatabaseSchemaLoader.migrateSchema();
    }

    private Connection getConnection() throws SQLException, IOException {
        return PostgreSQLFingerprintStorage.get().getConnectionSupplier().connection();
    }

    @Test
    public void testCreateFingerprintTable(JenkinsRule rule) throws SQLException, IOException {
        setConfiguration();
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.CHECK_FINGERPRINT_TABLE_EXISTS))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt(ColumnName.TOTAL), is(1));
            }
        }
    }

    @Test
    public void testCreateFingerprintJobBuildRelationTable(JenkinsRule rule) throws SQLException, IOException {
        setConfiguration();
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CHECK_FINGERPRINT_JOB_BUILD_RELATION_TABLE_EXISTS))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt(ColumnName.TOTAL), is(1));
            }
        }
    }

    @Test
    public void testCreateFingerprintFacetRelationTable(JenkinsRule rule) throws SQLException, IOException {
        setConfiguration();
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CHECK_FINGERPRINT_FACET_RELATION_TABLE_EXISTS))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt(ColumnName.TOTAL), is(1));
            }
        }
    }

    @Test
    public void testInsertAndSelectFingerprint(JenkinsRule rule) throws SQLException, IOException {
        setConfiguration();
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setTimestamp(3, TIMESTAMP);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD_NUMBER);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.SELECT_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getTimestamp(ColumnName.TIMESTAMP), is(TIMESTAMP));
                assertThat(resultSet.getString(ColumnName.FILENAME), is(FINGERPRINT_FILENAME));
                assertThat(resultSet.getString(ColumnName.ORIGINAL_JOB_NAME), is(JOB_NAME));
                assertThat(
                        resultSet.getString(ColumnName.ORIGINAL_JOB_BUILD_NUMBER), is(Integer.toString(BUILD_NUMBER)));
                assertThat(resultSet.getString(ColumnName.USAGES), is(nullValue()));
                assertThat(resultSet.getString(ColumnName.FACETS), is(nullValue()));
            }
        }
    }

    @Test
    public void testInsertAndSelectFingerprintJobBuildRelation(JenkinsRule rule) throws SQLException, IOException {
        setConfiguration();
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setTimestamp(3, TIMESTAMP);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD_NUMBER);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT_JOB_BUILD_RELATION))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, JOB_NAME);
                preparedStatement.setInt(4, BUILD_NUMBER);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.SELECT_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getTimestamp(ColumnName.TIMESTAMP), is(TIMESTAMP));
                assertThat(resultSet.getString(ColumnName.FILENAME), is(FINGERPRINT_FILENAME));
                assertThat(resultSet.getString(ColumnName.ORIGINAL_JOB_NAME), is(JOB_NAME));
                assertThat(
                        resultSet.getString(ColumnName.ORIGINAL_JOB_BUILD_NUMBER), is(Integer.toString(BUILD_NUMBER)));
                assertThat(
                        resultSet.getString(ColumnName.USAGES),
                        is(equalTo(
                                "[{\"job\" : \"" + JOB_NAME + "\", " + "\"build_number\" : " + BUILD_NUMBER + "}]")));
                assertThat(resultSet.getString(ColumnName.FACETS), is(nullValue()));
                ;
            }
        }
    }

    @Test
    public void testInsertAndSelectFingerprintFacetRelation(JenkinsRule rule) throws SQLException, IOException {
        setConfiguration();
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setTimestamp(3, TIMESTAMP);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD_NUMBER);
                preparedStatement.executeUpdate();
            }

            JSONObject facetEntry = new JSONObject();
            facetEntry.put("foo", "bar");

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT_FACET_RELATION))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, "FingerprintFacet");
                preparedStatement.setString(4, facetEntry.toString());
                preparedStatement.setBoolean(5, true);

                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.SELECT_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getTimestamp(ColumnName.TIMESTAMP), is(TIMESTAMP));
                assertThat(resultSet.getString(ColumnName.FILENAME), is(FINGERPRINT_FILENAME));
                assertThat(resultSet.getString(ColumnName.ORIGINAL_JOB_NAME), is(JOB_NAME));
                assertThat(
                        resultSet.getString(ColumnName.ORIGINAL_JOB_BUILD_NUMBER), is(Integer.toString(BUILD_NUMBER)));
                assertThat(resultSet.getString(ColumnName.USAGES), is(nullValue()));
                assertThat(
                        resultSet.getString(ColumnName.FACETS),
                        is(equalTo("[{\"facet_name\" : \"FingerprintFacet\", "
                                + "\"facet_entry\" : {\"foo\": \"bar\"}}]")));
            }
        }
    }

    @Test
    public void testDeleteFingerprint(JenkinsRule rule) throws SQLException, IOException {
        setConfiguration();
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setTimestamp(3, TIMESTAMP);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD_NUMBER);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT_JOB_BUILD_RELATION))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, JOB_NAME);
                preparedStatement.setInt(4, BUILD_NUMBER);
                preparedStatement.executeUpdate();
            }

            JSONObject json = new JSONObject();
            json.put("foo", "bar");

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.INSERT_FINGERPRINT_FACET_RELATION))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, "FingerprintFacet");
                preparedStatement.setString(4, json.toString());
                preparedStatement.setBoolean(5, false);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.DELETE_FINGERPRINT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.SELECT_FINGERPRINT_COUNT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt(ColumnName.TOTAL);
                    assertThat(fingerprintCount, is(0));
                }
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.SELECT_FINGERPRINT_JOB_BUILD_RELATION_COUNT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt(ColumnName.TOTAL);
                    assertThat(fingerprintCount, is(0));
                }
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.SELECT_FINGERPRINT_FACET_RELATION_COUNT))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt(ColumnName.TOTAL);
                    assertThat(fingerprintCount, is(0));
                }
            }
        }
    }
}
