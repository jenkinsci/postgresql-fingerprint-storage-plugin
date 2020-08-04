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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.io.IOException;
import java.sql.SQLException;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class PostgreSQLConnectionTest {

    private static final String CONNECTION_ATTEMPT_FAILED = "The connection attempt failed";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Rule
    public Network network = Network.newNetwork();

    @Rule
    public PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer()
            .withExposedPorts(5432)
            .withNetwork(network);

    @Rule
    public ToxiproxyContainer toxiproxy = new ToxiproxyContainer()
            .withNetwork(network)
            .withNetworkAliases("toxiproxy");

    private void setConfigurationViaProxy(ToxiproxyContainer.ContainerProxy proxy) throws IOException {
        final String host = proxy.getContainerIpAddress();
        final int port = proxy.getProxyPort();

        PostgreSQLConfiguration.setConfiguration(postgres.getUsername(), postgres.getPassword(), host, port,
                postgres.getDatabaseName());
    }

    @Test
    public void testPostgreSQLConnectionFailureForSave() throws IOException {
        String id = Util.getDigestOf("testPostgreSQLConnectionFailureForSave");

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, 5432);
        setConfigurationViaProxy(proxy);
        proxy.setConnectionCut(true);

        try {
            new Fingerprint(null, "foo.jar", Util.fromHexString(id));
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString(CONNECTION_ATTEMPT_FAILED));

            proxy.setConnectionCut(false);
            Fingerprint fingerprintLoaded = Fingerprint.load(id);
            assertThat(fingerprintLoaded, is(nullValue()));
            return;
        }
        fail("Expected IOException");
    }



}
