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

import eu.rekawek.toxiproxy.model.ToxicDirection;
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

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class PostgreSQLConnectionTest {

    private static final String CONNECTION_ATTEMPT_FAILED = "The connection attempt failed";
    private static final int PORT = 5432;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Rule
    public Network network = Network.newNetwork();

    @Rule
    public PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer()
            .withExposedPorts(PORT)
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

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
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


    @Test
    public void testPostgreSQLConnectionFailureForLoad() throws IOException {
        exceptionRule.expect(IOException.class);
        exceptionRule.expectMessage(CONNECTION_ATTEMPT_FAILED);

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
        setConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testPostgreSQLConnectionFailureForLoad");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        proxy.setConnectionCut(true);
        Fingerprint.load(id);
    }

    @Test
    public void testPostgreSQLConnectionFailureForDelete() throws IOException {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
        setConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testPostgreSQLConnectionFailureForDelete");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));

        proxy.setConnectionCut(true);

        try {
            Fingerprint.delete(id);
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString(CONNECTION_ATTEMPT_FAILED));

            proxy.setConnectionCut(false);
            Fingerprint fingerprintLoaded = Fingerprint.load(id);
            assertThat(fingerprintLoaded, is(not(nullValue())));
            return;
        }
        fail("Expected IOException");
    }

//    @Test
//    public void testPostgreSQLConnectionFailureForIsReady() throws IOException {
//        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
//        setConfigurationViaProxy(proxy);
//
//        proxy.setConnectionCut(true);
//
//        PostgreSQLFingerprintStorage.get().isReady();
//        } catch (IOException e) {
//            assertThat(e.getMessage(), containsString(CONNECTION_ATTEMPT_FAILED));
//
//            proxy.setConnectionCut(false);
//            assertThat(PostgreSQLFingerprintStorage.get().isReady(), is(false));
//            return;
//        }
//        fail("Expected IOException");
//    }

    @Test
    public void testSlowPostgreSQLConnectionForSave() throws IOException {
        exceptionRule.expect(IOException.class);
        exceptionRule.expectMessage(CONNECTION_ATTEMPT_FAILED);

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM,
                10 + (1000 * PostgreSQLConfiguration.DEFAULT_CONNECTION_TIMEOUT));
        setConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testSlowPostgreSQLConnectionForSave");
        new Fingerprint(null, "foo.jar", Util.fromHexString(id));
    }

    @Test
    public void testSlowPostgreSQLConnectionForLoad() throws IOException {
        exceptionRule.expect(IOException.class);
        exceptionRule.expectMessage(CONNECTION_ATTEMPT_FAILED);

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM,
                10 + (1000 * PostgreSQLConfiguration.DEFAULT_CONNECTION_TIMEOUT));
        setConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testSlowPostgreSQLConnectionForLoad");
        Fingerprint.load(id);
    }

    @Test
    public void testSlowPostgreSQLConnectionForDelete() throws IOException {
        exceptionRule.expect(IOException.class);
        exceptionRule.expectMessage(CONNECTION_ATTEMPT_FAILED);

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM,
                10 + (1000 * PostgreSQLConfiguration.DEFAULT_CONNECTION_TIMEOUT));
        setConfigurationViaProxy(proxy);

        String id = Util.getDigestOf("testSlowPostgreSQLConnectionForDelete");
        Fingerprint.delete(id);
    }

    @Test
    public void testSlowPostgreSQLConnectionForIsReady() throws IOException {
        exceptionRule.expect(IOException.class);
        exceptionRule.expectMessage(CONNECTION_ATTEMPT_FAILED);

        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(postgres, PORT);
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM,
                10 + (1000 * PostgreSQLConfiguration.DEFAULT_CONNECTION_TIMEOUT));
        setConfigurationViaProxy(proxy);

        PostgreSQLFingerprintStorage.get().isReady();
    }

}
