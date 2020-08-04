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
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

public class PostgreSQLQueryTest {

    private static final String DATE = new DateConverter().toString(new Date());
    private static final String INSTANCE_ID = "185d72052231445badce445130a11414";
    private static final String FINGERPRINT_FILENAME = "foo.jar";
    private static final String FINGERPRINT_ID = Util.getDigestOf("id");
    private static final String JOB_NAME = "Random Job";
    private static final int BUILD = 3;

    @Rule
    public PostgreSQLContainer postgres = new PostgreSQLContainer();

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Test
    public void testCreateFingerprintSchema() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("check_schema_exists"))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt("total"), is(1));
            }
        }
    }

    @Test
    public void testCreateFingerprintTable() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("check_fingerprint_table_exists"))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt("total"), is(1));
            }
        }
    }

    @Test
    public void testCreateFingerprintJobBuildRelationTable() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_job_build_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("check_fingerprint_job_build_relation_table_exists"))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt("total"), is(1));
            }
        }
    }

    @Test
    public void testCreateFingerprintFacetRelationTable() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_facet_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("check_fingerprint_facet_relation_table_exists"))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getInt("total"), is(1));
            }
        }
    }

    @Test
    public void testInsertAndSelectFingerprint() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_job_build_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_facet_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, DATE);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, FINGERPRINT_ID);
                preparedStatement.setString(4, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("timestamp"), is(DATE));
                assertThat(resultSet.getString("filename"), is(FINGERPRINT_FILENAME));
                assertThat(resultSet.getString("original_job_name"), is(JOB_NAME));
                assertThat(resultSet.getString("original_job_build"), is(Integer.toString(BUILD)));
                assertThat(resultSet.getString("usages"), is(nullValue()));
                assertThat(resultSet.getString("facets"), is(nullValue()));
            }
        }
    }

    @Test
    public void testInsertAndSelectFingerprintJobBuildRelation() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_job_build_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_facet_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, DATE);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint_job_build_relation"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, JOB_NAME);
                preparedStatement.setInt(4, BUILD);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, FINGERPRINT_ID);
                preparedStatement.setString(4, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("timestamp"), is(DATE));
                assertThat(resultSet.getString("filename"), is(FINGERPRINT_FILENAME));
                assertThat(resultSet.getString("original_job_name"), is(JOB_NAME));
                assertThat(resultSet.getString("original_job_build"), is(Integer.toString(BUILD)));
                assertThat(resultSet.getString("usages"), is(equalTo("[{\"job\" : \"" + JOB_NAME + "\", " +
                        "\"build\" : " + BUILD + "}]")));
                assertThat(resultSet.getString("facets"), is(nullValue()));;
            }
        }
    }

    @Test
    public void testInsertAndSelectFingerprintFacetRelation() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_job_build_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_facet_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, DATE);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD);
                preparedStatement.executeUpdate();
            }

            JSONObject facetEntry = new JSONObject();
            facetEntry.put("foo", "bar");

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint_facet_relation"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, FINGERPRINT_ID);
                preparedStatement.setString(4, INSTANCE_ID);

                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, JOB_NAME);
                preparedStatement.setString(4, String.valueOf(BUILD));
                ResultSet resultSet = preparedStatement.executeQuery();
                assertThat(resultSet.next(), is(true));
                assertThat(resultSet.getString("timestamp"), is(DATE));
                assertThat(resultSet.getString("filename"), is(FINGERPRINT_FILENAME));
                assertThat(resultSet.getString("original_job_name"), is(JOB_NAME));
                assertThat(resultSet.getString("original_job_build"), is(Integer.toString(BUILD)));
                assertThat(resultSet.getString("usages"), is(nullValue()));
                assertThat(resultSet.getString("facets"), is(nullValue()));
            }
        }
    }

    @Test
    public void testDeleteFingerprint() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_job_build_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_facet_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, DATE);
                preparedStatement.setString(4, FINGERPRINT_FILENAME);
                preparedStatement.setString(5, JOB_NAME);
                preparedStatement.setInt(6, BUILD);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint_job_build_relation"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, JOB_NAME);
                preparedStatement.setInt(4, BUILD);
                preparedStatement.executeUpdate();
            }

            JSONObject json = new JSONObject();
            json.put("foo", "bar");

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint_facet_relation"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.setString(3, "FingerprintFacet");
                preparedStatement.setString(4, json.toString());
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("delete_fingerprint"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                preparedStatement.executeUpdate();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint_count"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt("total");
                    assertThat(fingerprintCount, is(0));
                }
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint_job_build_relation_count"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt("total");
                    assertThat(fingerprintCount, is(0));
                }
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint_facet_relation_count"))) {
                preparedStatement.setString(1, FINGERPRINT_ID);
                preparedStatement.setString(2, INSTANCE_ID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt("total");
                    assertThat(fingerprintCount, is(0));
                }
            }
        }
    }

}
