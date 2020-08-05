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
import edu.umd.cs.findbugs.annotations.NonNull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides connection to a PostgreSQL instance.
 */
@Restricted(NoExternalUse.class)
public class PostgreSQLConnection {

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SSL = "ssl";
    private static final String CONNECTION_TIMEOUT = "connectTimeout";
    private static final String SOCKET_TIMEOUT = "socketTimeout";
    private static final String JDBC_URL = "jdbc:postgresql://";

    /**
     * Create a connection to PostgreSQL.
     */
    static @NonNull Connection getConnection(PostgreSQLFingerprintStorage postgreSQLFingerprintStorage)
            throws SQLException {
        StandardUsernamePasswordCredentials standardUsernamePasswordCredentials =
                CredentialLookup.getCredential(postgreSQLFingerprintStorage.getCredentialsId());
        String username = CredentialLookup.getUsernameFromCredential(standardUsernamePasswordCredentials);
        String password = CredentialLookup.getPasswordFromCredential(standardUsernamePasswordCredentials);
        return getConnection(
                postgreSQLFingerprintStorage.getHost(),
                postgreSQLFingerprintStorage.getPort(),
                postgreSQLFingerprintStorage.getDatabaseName(),
                username,
                password,
                postgreSQLFingerprintStorage.getSsl(),
                postgreSQLFingerprintStorage.getConnectionTimeout(),
                postgreSQLFingerprintStorage.getSocketTimeout());
    }

    static @NonNull Connection getConnection(String host, int port, String databaseName, String credentialsId,
                                             boolean ssl, int connectionTimeout, int socketTimeout)
            throws SQLException {
        StandardUsernamePasswordCredentials credentials = CredentialLookup.getCredential(credentialsId);
        String username = CredentialLookup.getUsernameFromCredential(credentials);
        String password = CredentialLookup.getPasswordFromCredential(credentials);
        return getConnection(host, port, databaseName, username, password, ssl, connectionTimeout, socketTimeout);
    }

    /**
     * Create a connection to PostgreSQL.
     */
    static @NonNull Connection getConnection(String host, int port, String databaseName, String username, String password,
                             boolean ssl, int connectionTimeout, int socketTimeout) throws SQLException {
        String url = JDBC_URL + host + ":" + port + "/" + databaseName;

        Properties properties = new Properties();
        properties.setProperty(USER, username);
        properties.setProperty(PASSWORD, password);
        if (ssl) {
            properties.setProperty(SSL, Boolean.toString(true));
        }
        properties.setProperty(CONNECTION_TIMEOUT, Integer.toString(connectionTimeout));
        properties.setProperty(SOCKET_TIMEOUT, Integer.toString(socketTimeout));

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        return DriverManager.getConnection(url, properties);
    }

}
