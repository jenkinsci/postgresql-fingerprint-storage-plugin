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
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PostgreSQLQueryTest {

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
    public void testSave() throws IOException, SQLException {
        setConfiguration();
        String instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
        String id = Util.getDigestOf("testSave");
        Fingerprint fingerprint = new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        try (Connection connection = PostgreSQLFingerprintStorage.get().getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint_count"))) {
                preparedStatement.setString(1, id);
                preparedStatement.setString(2, instanceId);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt("total");
                    assertThat(fingerprintCount, is(1));
                }
            }

            fingerprint.add("a", 3);
            fingerprint.getPersistedFacets().add(new PostgreSQLFingerprintStorageTest.TestFacet(fingerprint, 3, "a"));

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint_count"))) {
                preparedStatement.setString(1, id);
                preparedStatement.setString(2, instanceId);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt("total");
                    assertThat(fingerprintCount, is(1));
                }
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("select_fingerprint_count"))) {
                preparedStatement.setString(1, id);
                preparedStatement.setString(2, instanceId);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int fingerprintCount = resultSet.getInt("total");
                    assertThat(fingerprintCount, is(1));
                }
            }
        }
    }

}
