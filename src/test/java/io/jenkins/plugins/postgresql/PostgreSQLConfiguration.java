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

        PostgreSQLSchemaManager.performSchemaInitialization();
    }

    public static void setConfiguration(String username, String password, String host, int port, String databaseName)
            throws IOException {
        setConfiguration(username, password, host, port, databaseName, 3000, 3000, false);
    }

}
