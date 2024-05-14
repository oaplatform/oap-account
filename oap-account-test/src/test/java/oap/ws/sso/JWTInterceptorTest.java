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

import org.testng.annotations.Test;

import java.util.Map;

import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.test.HttpAsserts.assertGet;

public class JWTInterceptorTest extends IntegratedTest {
    @Test
    public void allowed() {
        accountFixture.addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );
        accountFixture.assertLogin( "admin@admin.com", "pass" );
        assertGet( httpUrl( "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
        assertGet( httpUrl( "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
        assertGet( httpUrl( "/secure/r1" ) )
            .responded( OK, "OK", TEXT_PLAIN, "admin@admin.com" );
    }

    @Test
    public void wrongRealm() {
        accountFixture.addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );
        accountFixture.assertLogin( "admin@admin.com", "pass" );
        assertGet( accountFixture.httpUrl( "/secure/r2" ) )
            .hasCode( UNAUTHORIZED );

    }

    @Test
    public void wrongRealmWithOrganizationLoggedIn() throws InterruptedException {
        accountFixture.addUser( "admin@admin.com", "pass", Map.of( "r1", "ADMIN" ) );
        accountFixture.assertLogin( "admin@admin.com", "pass" );
        assertSwitchOrganization( "r1" );
        assertGet( accountFixture.httpUrl( "/secure/r2" ) )
            .hasCode( UNAUTHORIZED );

    }

    @Test
    public void notLoggedIn() {
        assertGet( accountFixture.httpUrl( "/secure/r1" ) )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void denied() {
        accountFixture.addUser( "user@user.com", "pass", Map.of( "r1", "USER" ) );
        accountFixture.assertLogin( "user@user.com", "pass" );
        assertGet( accountFixture.httpUrl( "/secure/r1" ) )
            .hasCode( UNAUTHORIZED );
    }

}
