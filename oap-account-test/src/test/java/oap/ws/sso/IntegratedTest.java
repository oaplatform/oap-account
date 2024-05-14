/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.ws.sso;

import lombok.extern.slf4j.Slf4j;
import oap.storage.mongo.MongoFixture;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.ws.account.testing.AccountFixture;
import org.testng.annotations.AfterMethod;

import java.util.Map;

@Slf4j
public class IntegratedTest extends Fixtures {
    protected final AccountFixture accountFixture;

    public IntegratedTest() {
        fixture( new SystemTimerFixture() );
        TestDirectoryFixture testDirectoryFixture = fixture( new TestDirectoryFixture() );
        var mongoFixture = fixture( new MongoFixture( "MONGO" ) );
        accountFixture = fixture( new AccountFixture( testDirectoryFixture, mongoFixture ) );
    }

    protected String httpUrl( String url ) {
        return accountFixture.httpUrl( url );
    }

    protected oap.ws.account.User addUser( String mail, String pass, Map<String, String> roles ) {
        return accountFixture.addUser( mail, pass, roles );
    }

    protected oap.ws.account.User addUser( String mail, String pass, Map<String, String> roles, boolean tfaEnabled ) {
        return accountFixture.addUser( mail, pass, roles, tfaEnabled );
    }

    protected void assertLogin( String login, String password ) {
        accountFixture.assertLogin( login, password );
    }

    protected void assertLogin( String login, String password, String tfaCode ) {
        accountFixture.assertLogin( login, password, tfaCode );
    }

    protected JWTExtractor tokenExtractor() {
        return accountFixture.service( "oap-ws-sso-api", JWTExtractor.class );
    }

    protected void assertTfaRequiredLogin( String login, String password ) {
        accountFixture.assertTfaRequiredLogin( login, password );
    }

    protected void assertWrongTfaLogin( String login, String password, String tfaCode ) {
        accountFixture.assertWrongTfaLogin( login, password, tfaCode );
    }

    protected void assertLoginWithFBToken() {
        accountFixture.assertLoginWithFBToken();
    }

    protected void assertLoginWithFBTokenWithTfaRequired() {
        accountFixture.assertLoginWithFBTokenWithTfaRequired();
    }

    protected void assertLoginWithFBTokenWithWrongTfa() {
        accountFixture.assertLoginWithFBTokenWithWrongTfa();
    }

    protected void assertSwitchOrganization( String orgId ) {
        accountFixture.assertSwitchOrganization( orgId );
    }

    protected void assertLogout() {
        accountFixture.assertLogout();
    }

    @AfterMethod
    public void afterMethod() {
        try {
            assertLogout();
        } catch( AssertionError | Exception e ) {
            if( !e.getMessage().contains( "code = 401" ) ) {
                log.error( e.getMessage(), e );
            }
        }
    }
}
