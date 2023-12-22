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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.FormValidation;
import jenkins.fingerprints.FingerprintStorage;
import jenkins.fingerprints.FingerprintStorageDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Descriptor class for {@link PostgreSQLFingerprintStorage}.
 */
@Restricted(NoExternalUse.class)
public class PostgreSQLFingerprintStorageDescriptor extends FingerprintStorageDescriptor {

    private static final String SUCCESS = "Success";

    @Override
    public @NonNull String getDisplayName() {
        return Messages.PostgreSQLFingerprintStorage_DisplayName();
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public FormValidation doInitializePostgreSQL() {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return FormValidation.error("Need admin permission to perform this action");
        }
        try {
            FingerprintStorage fingerprintStorage = FingerprintStorage.get();
            if (fingerprintStorage instanceof PostgreSQLFingerprintStorage) {
                Database database = GlobalDatabaseConfiguration.get().getDatabase();
                if (database == null) {
                    return FormValidation.error("No database configured on global configuration");
                }
                PostgreSQLFingerprintStorage postgreSQLFingerprintStorage =
                        (PostgreSQLFingerprintStorage) fingerprintStorage;
                PostgreSQLSchemaInitialization.performSchemaInitialization(
                        postgreSQLFingerprintStorage.getConnectionSupplier());
            }
            return FormValidation.ok(SUCCESS);
        } catch (Exception e) {
            return FormValidation.error("Schema initialization failed." + e.getMessage());
        }
    }
}
