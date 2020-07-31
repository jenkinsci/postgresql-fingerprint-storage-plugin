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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import jenkins.fingerprints.GlobalFingerprintConfiguration;

import java.io.IOException;

public class PostgreSQLConfiguration {

    public static void setConfiguration(String username, String password, String host, int port, String databaseName,
                                        int connectionTimeout, int socketTimeout, boolean ssl) throws IOException {
        StandardUsernamePasswordCredentials standardUsernamePasswordCredentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                "credentialId",
                null,
                username,
                password
        );
        SystemCredentialsProvider.getInstance().getCredentials().add(standardUsernamePasswordCredentials);
        SystemCredentialsProvider.getInstance().save();

        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();

        postgreSQLFingerprintStorage.setHost(host);
        postgreSQLFingerprintStorage.setPort(port);
        postgreSQLFingerprintStorage.setDatabaseName(databaseName);
        postgreSQLFingerprintStorage.setCredentialsId(standardUsernamePasswordCredentials.getId());
        postgreSQLFingerprintStorage.setConnectionTimeout(connectionTimeout);
        postgreSQLFingerprintStorage.setSocketTimeout(socketTimeout);
        postgreSQLFingerprintStorage.setSsl(ssl);

        GlobalFingerprintConfiguration.get().setStorage(postgreSQLFingerprintStorage);

        PostgreSQLSchemaInitialization.performSchemaInitialization();
    }

    public static void setConfiguration(String username, String password, String host, int port, String databaseName)
            throws IOException {
        setConfiguration(username, password, host, port, databaseName, 3000, 3000, false);
    }

}
