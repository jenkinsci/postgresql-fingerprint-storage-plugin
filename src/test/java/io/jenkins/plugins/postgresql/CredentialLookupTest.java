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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CredentialLookupTest {

    private StandardUsernamePasswordCredentials standardUsernamePasswordCredentials;
    private static final String ID = "randomCredentialId";
    private static final String USERNAME = "randomUsername";
    private static final String PASSWORD = "randomPassword";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void configureCredentials() throws IOException {
        standardUsernamePasswordCredentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.SYSTEM,
                ID,
                null,
                USERNAME,
                PASSWORD
        );
        SystemCredentialsProvider.getInstance().getCredentials().add(standardUsernamePasswordCredentials);
        SystemCredentialsProvider.getInstance().save();
    }

    @Test
    public void testGetUsernameFromCredential() {
        assertThat(CredentialLookup.getUsernameFromCredential(standardUsernamePasswordCredentials),
                is(equalTo(USERNAME)));
        assertThat(CredentialLookup.getUsernameFromCredential(null), is(equalTo("")));
    }

    @Test
    public void testGetPasswordFromCredential() {
        assertThat(CredentialLookup.getPasswordFromCredential(standardUsernamePasswordCredentials),
                is(equalTo(PASSWORD)));
        assertThat(CredentialLookup.getPasswordFromCredential(null), is(equalTo("")));
    }

    @Test
    public void testGetCredential() {
        assertThat(CredentialLookup.getCredential(ID), is(equalTo(standardUsernamePasswordCredentials)));
    }
}
