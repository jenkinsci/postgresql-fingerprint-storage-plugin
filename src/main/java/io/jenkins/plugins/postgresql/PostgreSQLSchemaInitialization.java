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

import io.jenkins.plugins.postgresql.PostgreSQLFingerprintStorage.ConnectionSupplier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Handles PostgreSQL schema initialization.
 */
@Restricted(NoExternalUse.class)
public class PostgreSQLSchemaInitialization {

    private static final Logger LOGGER = Logger.getLogger(PostgreSQLSchemaInitialization.class.getName());

    static synchronized void performSchemaInitialization(PostgreSQLFingerprintStorage postgreSQLFingerprintStorage) {
        performSchemaInitialization(postgreSQLFingerprintStorage.getConnectionSupplier());
    }

    /**
     * Responsible for creating the fingerprint schema in PostgreSQL if it doesn't already exist.
     */
    static synchronized void performSchemaInitialization(ConnectionSupplier connectionSupplier) {
        try (Connection connection = connectionSupplier.connection()) {
            connection.setAutoCommit(false);

            // Create fingerprint table

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.CREATE_FINGERPRINT_TABLE))) {
                preparedStatement.execute();
            }

            // Create fingerprint job build relation table

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CREATE_FINGERPRINT_JOB_BUILD_RELATION_TABLE))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery(Queries.CREATE_FINGERPRINT_JOB_BUILD_RELATION_INDEX))) {
                preparedStatement.execute();
            }

            // Create fingerprint facet relation table

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.CREATE_FINGERPRINT_FACET_RELATION_TABLE))) {
                preparedStatement.execute();
            }

            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(Queries.getQuery(Queries.CREATE_FINGERPRINT_FACET_RELATION_INDEX))) {
                preparedStatement.execute();
            }

            connection.commit();
            LOGGER.log(Level.INFO, "Successfully initialized PostgreSQL schema");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to initialize PostgreSQL schema", e);
        }
    }
}
