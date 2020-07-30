package io.jenkins.plugins.postgresql;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PostgreSQLConnection {

    public static Connection getConnection(String host, int port, String databaseName, String credentialsId,
                                           boolean ssl, int connectionTimeout, int socketTimeout) throws SQLException {
        StandardUsernamePasswordCredentials standardUsernamePasswordCredentials =
                CredentialHelper.getCredential(credentialsId);
        String username = CredentialHelper.getUsernameFromCredential(standardUsernamePasswordCredentials);
        String password = CredentialHelper.getPasswordFromCredential(standardUsernamePasswordCredentials);
        return getConnection(host, port, databaseName, username, password, ssl, connectionTimeout, socketTimeout);
    }

    public static Connection getConnection(String host, int port, String databaseName, String username, String password,
                                           boolean ssl, int connectionTimeout, int socketTimeout) throws SQLException {
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;

        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        if (ssl) {
            properties.setProperty("ssl", Boolean.toString(true));
        }
        properties.setProperty("connectTimeout", Integer.toString(connectionTimeout));
        properties.setProperty("socketTimeout", Integer.toString(socketTimeout));

        return DriverManager.getConnection(url, properties);
    }

}
