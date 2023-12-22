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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Result;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Shell;
import hudson.util.Secret;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jenkins.fingerprints.GlobalFingerprintConfiguration;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jenkinsci.plugins.database.postgresql.PostgreSQLDatabase;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@WithJenkins
@Testcontainers
public class JobsIntegrationTest {

    @Container
    public PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PostgreSQLContainer.IMAGE);

    public void setConfiguration(JenkinsRule j) throws Exception {
        j.createSlave(Label.get("test-agent"));
        PostgreSQLDatabase database = new PostgreSQLDatabase(
                postgres.getHost() + ":" + postgres.getMappedPort(5432),
                postgres.getDatabaseName(),
                postgres.getUsername(),
                Secret.fromString(postgres.getPassword()),
                null);
        database.setValidationQuery("SELECT 1");
        GlobalDatabaseConfiguration.get().setDatabase(database);
        PostgreSQLFingerprintStorage postgreSQLFingerprintStorage = PostgreSQLFingerprintStorage.get();
        GlobalFingerprintConfiguration.get().setStorage(postgreSQLFingerprintStorage);
        PostgreSQLSchemaInitialization.performSchemaInitialization(postgreSQLFingerprintStorage);
    }

    @Test
    public void shouldRunFreestyleJob(JenkinsRule j) throws Exception {
        setConfiguration(j);
        FreeStyleProject freeStyleProject = createFreeStyleProject(j);
        FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        assertThat(build.getResult(), is(Result.SUCCESS));
        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement("SELECT COUNT(*) FROM FINGERPRINT")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        assertThat(resultSet.getInt(1), is(1));
                    }
                }
            }
        }
    }

    @Test
    public void shouldRunPipeline(JenkinsRule j) throws Exception {
        setConfiguration(j);
        String pipeline = IOUtils.toString(
                JobsIntegrationTest.class.getResourceAsStream("pipelines/pipeline.groovy"), StandardCharsets.UTF_8);
        WorkflowJob workflowJob = j.createProject(WorkflowJob.class);
        workflowJob.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run1 = workflowJob.scheduleBuild2(0).waitForStart();
        j.waitForCompletion(run1);
        assertThat(run1.getResult(), is(Result.SUCCESS));

        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement("SELECT COUNT(*) FROM FINGERPRINT")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        assertThat(resultSet.getInt(1), is(1));
                    }
                }
            }
        }
    }

    private FreeStyleProject createFreeStyleProject(JenkinsRule j) throws IOException {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject();
        ArtifactArchiver archiver = new ArtifactArchiver("foo.txt");
        archiver.setFingerprint(true);
        freeStyleProject.getBuildersList().add(new Shell("echo foo > foo.txt"));
        freeStyleProject.getPublishersList().add(archiver);
        return freeStyleProject;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
