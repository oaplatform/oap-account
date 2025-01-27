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


import oap.http.Client;
import oap.http.Http;
import oap.http.test.MockCookieStore;
import oap.json.Binder;
import oap.util.Dates;
import oap.ws.account.testing.SecureWSHelper;
import oap.ws.account.utils.TfaUtils;
import oap.ws.sso.interceptor.ThrottleLoginInterceptor;
import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.test.HttpAsserts.assertGet;
import static oap.http.test.HttpAsserts.assertPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.AssertJUnit.assertTrue;

public class AuthWSTest extends IntegratedTest {
    @Test
    public void loginWhoami() {
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .is( r -> assertThat( r.contentString() ).contains( "\"email\":\"admin@admin.com\"" ) );
    }

    @Test
    public void loginResponseTest() {
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );
        assertPost( httpUrl( "/auth/login" ), "{ \"email\":\"admin@admin.com\",\"password\": \"pass\"}" )
            .hasCode( Http.StatusCode.OK ).satisfies( resp -> {
                Map<String, String> response = Binder.json.unmarshal( Map.class, resp.contentString() );
                assertTrue( response.containsKey( "accessToken" ) );
                assertTrue( response.containsKey( "refreshToken" ) );
                assertThat( resp.contentString() ).contains( """
                    "user":{"email":"admin@admin.com","firstName":"admin@admin.com","lastName":"admin@admin.com","roles":{"r1":"ADMIN"},"banned":false,"confirmed":true,"tfaEnabled":false,""" );
            } );
    }

    @Test
    public void loginMfaRequired() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ), true );
        assertTfaRequiredLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void loginMfa() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        var user = addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ), true );
        assertTfaRequiredLogin( "admin@admin.com", "pass" );
        assertLogin( "admin@admin.com", "pass", TfaUtils.getTOTPCode( user.getSecretKey() ) );
        assertGet( httpUrl( "/auth/whoami" ) )
            .is( r -> assertThat( r.contentString() ).contains( "\"email\":\"admin@admin.com\"" ) );
    }

    @Test
    public void loginMfaWrongCode() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ), true );
        assertTfaRequiredLogin( "admin@admin.com", "pass" );
        assertWrongTfaLogin( "admin@admin.com", "pass", "wrong_code" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void logout() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );
        addUser( "user@admin.com", "pass", Map.of( "r1", "USER" ) );
        assertLogin( "admin@admin.com", "pass" );
        assertLogout();
        assertGet( httpUrl( "/auth/whoami" ) )
            .hasCode( UNAUTHORIZED );
        assertLogin( "user@admin.com", "pass" );
        assertGet( httpUrl( "/auth/whoami" ) )
            .isOk()
            .is( r -> assertThat( r.contentString() ).contains( "\"email\":\"user@admin.com\"" ) );
    }

    @Test
    public void loginAndTryToReachOrganization() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );
        addUser( "user@user.com", "pass", Map.of( "r1", "USER" ) );

        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) ).hasCode( OK );

        assertLogin( "admin@admin.com", "pass" );
        assertSwitchOrganization( "r1" );
        assertGet( httpUrl( "/secure/r1" ) ).hasCode( OK );
    }

    @Test
    public void loginThenUseSpecificOrganization() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN", "r2", "USER" ) );
        assertLogin( "admin@admin.com", "pass" );
        assertSwitchOrganization( "r2" );
    }

    @Test
    public void loginThenUseWrongOrganization() throws InterruptedException {
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN", "r2", "USER" ) );
        assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "auth/switch/r3" ) ).hasCode( FORBIDDEN ).hasReason( "User doesn't belong to organization" );
    }

    @Test
    public void loginWithExternalToken() {
        addUser( "newuser@user.com", null, Map.of( "r1", "ADMIN" ) );
        assertLoginWithFBToken();
        assertGet( httpUrl( "/secure/r1" ) )
            .isOk();
        assertGet( httpUrl( "/auth/whoami" ) )
            .is( r -> assertThat( r.contentString() ).contains( "\"email\":\"newuser@user.com\"" ) );
    }

    @Test
    public void loginWithExternalTokenWithTfa() {
        var user = addUser( "newuser@user.com", null, Map.of( "r1", "ADMIN" ), true );
        SecureWSHelper.assertLoginWithFBTokenWithTfa( accountFixture.defaultHttpPort(), TfaUtils.getTOTPCode( user.getSecretKey() ) );
        assertGet( httpUrl( "/secure/r1" ) )
            .isOk();
        assertGet( httpUrl( "/auth/whoami" ) )
            .is( r -> assertThat( r.contentString() ).contains( "\"email\":\"newuser@user.com\"" ) );
    }

    @Test
    public void loginWithExternalTokenWithTfaRequired() {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        addUser( "newuser@user.com", null, Map.of( "r1", "USER" ), true );
        assertLoginWithFBTokenWithTfaRequired();
    }

    @Test
    public void loginWithExternalTokenWithWrongTfa() throws InterruptedException {
        accountFixture.service( "oap-ws-sso-api", ThrottleLoginInterceptor.class ).delay = -1;

        addUser( "newuser@user.com", null, Map.of( "r1", "USER" ), true );
        assertLoginWithFBTokenWithWrongTfa();
    }

    @Test
    public void testInvalidateTokens() {
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );

        try( Client client1 = Client.custom().build();
             Client client2 = Client.custom().build() ) {

            Client.Response response = client1.post( accountFixture.httpUrl( "/auth/login" ), "{  \"email\": \"admin@admin.com\",  \"password\": \"pass\"}", Http.ContentType.APPLICATION_JSON );
            assertThat( response.code ).isEqualTo( OK );

            response = client1.get( accountFixture.httpUrl( "/auth/whoami" ) );
            assertThat( response.code ).isEqualTo( OK );

            response = client2.post( accountFixture.httpUrl( "/auth/login" ), "{  \"email\": \"admin@admin.com\",  \"password\": \"pass\"}", Http.ContentType.APPLICATION_JSON );
            assertThat( response.code ).isEqualTo( OK );

            response = client1.get( accountFixture.httpUrl( "/auth/whoami" ) );
            assertThat( response.code ).isEqualTo( UNAUTHORIZED );
            response = client2.get( accountFixture.httpUrl( "/auth/whoami" ) );
            assertThat( response.code ).isEqualTo( OK );
        }
    }

    @Test
    public void testRefreshToken() {
        addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );

        Dates.setTimeFixed( 2023, 5, 15, 21, 26, 0 );

        DateTime startTime = new DateTime( UTC );

        MockCookieStore cookieStore = new MockCookieStore();
        try( Client client = Client.custom().withCookieStore( cookieStore ).build() ) {

            Client.Response response = client.post( accountFixture.httpUrl( "/auth/login" ), "{  \"email\": \"admin@admin.com\",  \"password\": \"pass\"}", Http.ContentType.APPLICATION_JSON );
            assertThat( response.code ).isEqualTo( OK );

            response = client.get( accountFixture.httpUrl( "/auth/whoami" ) );
            assertThat( response.code ).isEqualTo( OK );
            assertThat( cookieStore.getCookie( SSO.AUTHENTICATION_KEY ) ).extracting( Cookie::getExpiryDate ).isEqualTo( startTime.plusMinutes( 2 ).withMillisOfSecond( 0 ).toDate() );
            assertThat( cookieStore.getCookie( SSO.REFRESH_TOKEN_KEY ) ).extracting( Cookie::getExpiryDate ).isEqualTo( startTime.plusDays( 30 ).withMillisOfSecond( 0 ).toDate() );

            Dates.incFixed( Dates.m( 3 ) );

            response = client.get( accountFixture.httpUrl( "/auth/whoami" ) );
            assertThat( response.code ).isEqualTo( OK );
            assertThat( cookieStore.getCookie( SSO.AUTHENTICATION_KEY ) ).extracting( Cookie::getExpiryDate ).isEqualTo( startTime.plusMinutes( 3 ).plusMinutes( 2 ).withMillisOfSecond( 0 ).toDate() );
            assertThat( cookieStore.getCookie( SSO.REFRESH_TOKEN_KEY ) ).extracting( Cookie::getExpiryDate ).isEqualTo( startTime.plusMinutes( 3 ).plusDays( 30 ).withMillisOfSecond( 0 ).toDate() );

            Dates.incFixed( Dates.d( 30 ) );

            response = client.get( accountFixture.httpUrl( "/auth/whoami" ) );
            assertThat( response.code ).isEqualTo( UNAUTHORIZED );
        }
    }
}
