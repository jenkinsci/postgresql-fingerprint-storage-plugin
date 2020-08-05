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

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles PostgreSQL schema initialization.
 */
@Restricted(NoExternalUse.class)
public class PostgreSQLSchemaInitialization {

    static synchronized void performSchemaInitialization(PostgreSQLFingerprintStorage postgreSQLFingerprintStorage) {
        performSchemaInitialization(postgreSQLFingerprintStorage.getHost(), postgreSQLFingerprintStorage.getPort(),
                postgreSQLFingerprintStorage.getDatabaseName(), postgreSQLFingerprintStorage.getCredentialsId(),
                postgreSQLFingerprintStorage.getSsl(), postgreSQLFingerprintStorage.getConnectionTimeout(),
                postgreSQLFingerprintStorage.getSocketTimeout());
    }

    /**
     * Responsible for creating the fingerprint schema in PostgreSQL if it doesn't already exist.
     */
    static synchronized void performSchemaInitialization(String host, int port, String databaseName,
                                                         String credentialsId, boolean ssl, int connectionTimeout,
                                                         int socketTimeout) {
        try (Connection connection = PostgreSQLConnection.getConnection(host, port, databaseName, credentialsId, ssl,
                connectionTimeout, socketTimeout)) {
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("check_schema_exists"))) {
                ResultSet resultSet = preparedStatement.executeQuery();
                boolean schemaExists = false;
                if (resultSet.next()) {
                    schemaExists = (resultSet.getInt("total") == 1);
                }
                resultSet.close();
                if (schemaExists) {
                    return;
                }
            }

            // Create schema

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_schema"))) {
                preparedStatement.execute();
            }

            // Create fingerprint table

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_table"))) {
                preparedStatement.execute();
            }

            // Create fingerprint job build relation table

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_job_build_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_job_build_relation_index"))) {
                preparedStatement.execute();
            }

            // Create fingerprint facet relation table

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_facet_relation_table"))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("create_fingerprint_facet_relation_index"))) {
                preparedStatement.execute();
            }

            connection.commit();
        } catch (SQLException ignored) {

        }
    }

}
