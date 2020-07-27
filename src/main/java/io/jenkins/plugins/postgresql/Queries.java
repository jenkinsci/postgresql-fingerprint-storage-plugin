package io.jenkins.plugins.postgresql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class Queries {

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

    public static String getQuery(String query) throws SQLException {
        if (properties == null) {
            throw new SQLException("Unable to load property file: " + propertiesFileName);
        }
        return properties.getProperty(query);
    }

}
