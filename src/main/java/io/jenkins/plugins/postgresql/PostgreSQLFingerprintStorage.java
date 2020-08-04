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

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.TaskListener;
import jenkins.fingerprints.FingerprintStorage;
import hudson.model.Fingerprint;
import hudson.Util;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Pluggable external fingerprint storage for fingerprints into PostgreSQL.
 */
@Extension
public class PostgreSQLFingerprintStorage extends FingerprintStorage {

    private final String instanceId;
    private static final DateConverter DATE_CONVERTER = new DateConverter();
    private static final Logger LOGGER = Logger.getLogger(PostgreSQLFingerprintStorage.class.getName());

    public static PostgreSQLFingerprintStorage get() {
        return ExtensionList.lookupSingleton(PostgreSQLFingerprintStorage.class);
    }

    @DataBoundConstructor
    public PostgreSQLFingerprintStorage() throws IOException {
        instanceId = Util.getDigestOf(new ByteArrayInputStream(InstanceIdentity.get().getPublic().getEncoded()));
    }

    /**
     * Creates and returns a connection to the PostgreSQL instance.
     */
    @NonNull Connection getConnection(PostgreSQLFingerprintStorage postgreSQLFingerprintStorage) throws SQLException {
        return PostgreSQLConnection.getConnection(postgreSQLFingerprintStorage);
    }

    /**
     * Saves the given fingerprint inside the PostgreSQL instance.
     */
    public synchronized void save(@NonNull Fingerprint fingerprint) throws IOException {
        try (Connection connection = getConnection(this)) {
            connection.setAutoCommit(false);

            delete(fingerprint.getHashString(), connection);

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    Queries.getQuery("insert_fingerprint"))) {
                preparedStatement.setString(1, fingerprint.getHashString());
                preparedStatement.setString(2, instanceId);
                preparedStatement.setString(3, DATE_CONVERTER.toString(fingerprint.getTimestamp()));
                preparedStatement.setString(4, fingerprint.getFileName());

                Fingerprint.BuildPtr original = fingerprint.getOriginal();
                if (original != null) {
                    preparedStatement.setString(5, original.getName());
                    preparedStatement.setInt(6, original.getNumber());
                } else {
                    preparedStatement.setNull(5, Types.NULL);
                    preparedStatement.setNull(6, Types.NULL);
                }

                preparedStatement.executeUpdate();
            }

            Hashtable<String, Fingerprint.RangeSet> usages = fingerprint.getUsages();
            if (usages != null) {
                for (Map.Entry<String, Fingerprint.RangeSet> usage : usages.entrySet()) {
                    String jobName = usage.getKey();
                    Fingerprint.RangeSet rangeSet = usage.getValue();

                    for (int build : rangeSet.listNumbers()) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(
                                Queries.getQuery("insert_fingerprint_job_build_relation"))) {
                            preparedStatement.setString(1, fingerprint.getHashString());
                            preparedStatement.setString(2, instanceId);
                            preparedStatement.setString(3, jobName);
                            preparedStatement.setInt(4, build);
                            preparedStatement.executeUpdate();
                        }
                    }
                }
            }

            Map<String, List<String>> facets = DataConversion.extractFacets(fingerprint);
            for (Map.Entry<String, List<String>> entry : facets.entrySet()) {
                String facetName = entry.getKey();
                List<String> facetEntries = entry.getValue();

                for (String facetEntry : facetEntries) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            Queries.getQuery("insert_fingerprint_facet_relation"))) {
                        preparedStatement.setString(1, fingerprint.getHashString());
                        preparedStatement.setString(2, instanceId);
                        preparedStatement.setString(3, facetName);
                        preparedStatement.setString(4, facetEntry);

                        preparedStatement.executeUpdate();
                    }
                }
            }
            connection.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "PostgreSQL failed in saving fingerprint: " + fingerprint.toString(), e);
            throw new IOException(e);
        }
    }

    /**
     * Returns the fingerprint associated with the given unique id and the Jenkins instance ID, from the PostgreSQL
     * instance.
     */
    public @CheckForNull Fingerprint load(@NonNull String id) throws IOException {
        try (Connection connection = getConnection(this);
             PreparedStatement preparedStatement = connection.prepareStatement(
                       Queries.getQuery("select_fingerprint"))) {

            preparedStatement.setString(1, id);
            preparedStatement.setString(2, instanceId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                Map<String, String> fingerprintMetadata = DataConversion.extractFingerprintMetadata(
                        id,
                        resultSet.getString("timestamp"),
                        resultSet.getString("filename"),
                        resultSet.getString("original_job_name"),
                        resultSet.getString("original_job_build")
                );
                Map<String, Fingerprint.RangeSet> usageMetadata = DataConversion.extractUsageMetadata(
                        resultSet.getString("usages"));
                JSONArray facets = DataConversion.extractFacets(resultSet.getString("facets"));
                String json = DataConversion.constructFingerprintJSON(fingerprintMetadata, usageMetadata, facets);
                return (Fingerprint) XStreamHandler.getXStream().fromXML(json);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "PostgreSQL failed in loading fingerprint: " + id, e);
            throw new IOException(e);
        }
    }

    /**
     * Deletes the fingerprint with the given id from the PostgreSQL instance.
     */
    public void delete(@NonNull String id) throws IOException {
        try (Connection connection = getConnection(this)) {
            connection.setAutoCommit(false);
            delete(id, connection);
            connection.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "PostgreSQL failed in deleting fingerprint: " + id, e);
            throw new IOException(e);
        }
    }

    private void delete(@NonNull String id, @NonNull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                Queries.getQuery("delete_fingerprint"))) {
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, instanceId);
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Returns true if there are fingerprints associate with the instance ID inside PostgreSQL instance.
     */
    public boolean isReady() throws IOException {
        try (Connection connection = getConnection(this);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     Queries.getQuery("select_fingerprint_exists_for_instance"))) {
            preparedStatement.setString(1, instanceId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return (resultSet.getInt("total") > 0);
                }
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed connecting to PostgreSQL.", e);
            throw new IOException(e);
        }
    }

    @Override
    public void iterateAndCleanupFingerprints(TaskListener taskListener) {
        // TODO
    }

    private String host = PostgreSQLFingerprintStorageDescriptor.DEFAULT_HOST;
    private int port = PostgreSQLFingerprintStorageDescriptor.DEFAULT_PORT;
    private String databaseName = PostgreSQLFingerprintStorageDescriptor.DEFAULT_DATABASE_NAME;
    private boolean ssl = PostgreSQLFingerprintStorageDescriptor.DEFAULT_SSL;
    private int connectionTimeout = PostgreSQLFingerprintStorageDescriptor.DEFAULT_CONNECTION_TIMEOUT;
    private int socketTimeout = PostgreSQLFingerprintStorageDescriptor.DEFAULT_SOCKET_TIMEOUT;
    private String credentialsId = PostgreSQLFingerprintStorageDescriptor.DEFAULT_CREDENTIALS_ID;

    public String getHost() {
        return host;
    }

    @DataBoundSetter
    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    @DataBoundSetter
    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    @DataBoundSetter
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean getSsl() {
        return this.ssl;
    }

    @DataBoundSetter
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    @DataBoundSetter
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    @DataBoundSetter
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public @NonNull String getUsername() {
        StandardUsernamePasswordCredentials credential = CredentialLookup.getCredential(getCredentialsId());
        return CredentialLookup.getUsernameFromCredential(credential);
    }

    public @NonNull String getPassword() {
        StandardUsernamePasswordCredentials credential = CredentialLookup.getCredential(getCredentialsId());
        return CredentialLookup.getPasswordFromCredential(credential);
    }

    @Extension
    public static class DescriptorImpl extends PostgreSQLFingerprintStorageDescriptor {

    }

}
