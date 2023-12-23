package io.jenkins.plugins.postgresql;

import static hudson.init.InitMilestone.SYSTEM_CONFIG_ADAPTED;

import hudson.init.Initializer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import jenkins.fingerprints.FingerprintStorage;
import org.flywaydb.core.Flyway;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class DatabaseSchemaLoader {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSchemaLoader.class.getName());

    static boolean MIGRATED;

    @Initializer(after = SYSTEM_CONFIG_ADAPTED)
    public static void migrateSchema() {
        FingerprintStorage configuration = FingerprintStorage.get();
        if (configuration instanceof PostgreSQLFingerprintStorage) {
            try {
                PostgreSQLFingerprintStorage storage = (PostgreSQLFingerprintStorage) configuration;
                DataSource dataSource =
                        storage.getConnectionSupplier().database().getDataSource();
                Database database = GlobalDatabaseConfiguration.get().getDatabase();
                assert database != null;
                Flyway flyway = Flyway.configure(DatabaseSchemaLoader.class.getClassLoader())
                        .baselineOnMigrate(true)
                        .table("fingerprint_flyway_schema_history")
                        .dataSource(dataSource)
                        .locations("db/migration/postgres")
                        .load();
                flyway.migrate();
                MIGRATED = true;
            } catch (Exception e) {
                // TODO add admin monitor
                LOGGER.log(
                        Level.SEVERE,
                        "Error migrating database, correct this error before using the fingerprint plugin",
                        e);
            }
        }
    }
}
