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
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Reads SQL queries from {@link #propertiesFileName}.
 */
@Restricted(NoExternalUse.class)
public class Queries {

    static final String CREATE_FINGERPRINT_SCHEMA = "create_fingerprint_schema";
    static final String CREATE_FINGERPRINT_TABLE = "create_fingerprint_table";
    static final String CREATE_FINGERPRINT_JOB_BUILD_RELATION_TABLE = "create_fingerprint_job_build_relation_table";
    static final String CREATE_FINGERPRINT_JOB_BUILD_RELATION_INDEX = "create_fingerprint_job_build_relation_index";
    static final String CREATE_FINGERPRINT_FACET_RELATION_TABLE = "create_fingerprint_facet_relation_table";
    static final String CREATE_FINGERPRINT_FACET_RELATION_INDEX = "create_fingerprint_facet_relation_index";
    static final String INSERT_FINGERPRINT = "insert_fingerprint";
    static final String INSERT_FINGERPRINT_JOB_BUILD_RELATION = "insert_fingerprint_job_build_relation";
    static final String INSERT_FINGERPRINT_FACET_RELATION = "insert_fingerprint_facet_relation";
    static final String SELECT_FINGERPRINT = "select_fingerprint";
    static final String SELECT_FINGERPRINT_EXISTS_FOR_INSTANCE = "select_fingerprint_exists_for_instance";
    static final String DELETE_FINGERPRINT = "delete_fingerprint";
    static final String CHECK_SCHEMA_EXISTS = "check_schema_exists";
    static final String CHECK_FINGERPRINT_TABLE_EXISTS = "check_fingerprint_table_exists";
    static final String CHECK_FINGERPRINT_JOB_BUILD_RELATION_TABLE_EXISTS =
            "check_fingerprint_job_build_relation_table_exists";
    static final String CHECK_FINGERPRINT_FACET_RELATION_TABLE_EXISTS = "check_fingerprint_facet_relation_table_exists";
    static final String SELECT_FINGERPRINT_COUNT = "select_fingerprint_count";
    static final String SELECT_FINGERPRINT_JOB_BUILD_RELATION_COUNT = "select_fingerprint_job_build_relation_count";
    static final String SELECT_FINGERPRINT_FACET_RELATION_COUNT = "select_fingerprint_facet_relation_count";
    static final String SELECT_ALL_USAGES_IN_INSTANCE = "select_all_usages_in_instance";
    static final String VACUUM = "vacuum";
    static final String DELETE_JOB_FROM_FINGERPRINT_JOB_BUILD_RELATION =
            "delete_job_from_fingerprint_job_build_relation";

    private static final String propertiesFileName = "Queries.properties";
    private static Properties properties;

    static {
        try (InputStream inputStream = Queries.class.getResourceAsStream(propertiesFileName)) {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            properties = null;
        }
    }

    /**
     * Returns the SQL query with the given query name from {@link #propertiesFileName}.
     */
    static @NonNull String getQuery(@NonNull String query) throws SQLException {
        if (properties == null) {
            throw new SQLException("Unable to load property file: " + propertiesFileName);
        }
        return properties.getProperty(query);
    }

}
