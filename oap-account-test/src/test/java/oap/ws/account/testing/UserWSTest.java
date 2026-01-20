/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.testing;

import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.storage.Storage;
import oap.storage.mongo.MongoFixture;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.ws.account.Organization;
import oap.ws.account.OrganizationData;
import oap.ws.account.User;
import oap.ws.account.UserData;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Random;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static oap.http.test.HttpAsserts.assertGet;
import static oap.ws.account.Roles.USER;
import static oap.ws.account.testing.AbstractAccountFixture.DEFAULT_ADMIN_EMAIL;
import static oap.ws.account.testing.AbstractAccountFixture.DEFAULT_ORGANIZATION_ADMIN_EMAIL;
import static oap.ws.account.testing.AbstractAccountFixture.DEFAULT_ORGANIZATION_ID;
import static oap.ws.account.testing.AbstractAccountFixture.DEFAULT_PASSWORD;

@Slf4j
public class UserWSTest extends Fixtures {
    protected final AccountFixture accountFixture;
    private final byte[] randomBytes = new byte[] {
        0x12, 0x21, 0x12, 0x32, 0x42,
        0x59, 0x13, 0x22, 0x12, 0x38,
        0x70, 0x62, 0x14, 0x23, 0x12,
        0x05, 0x12, 0x72, 0x15, 0x24,
    };

    public UserWSTest() {
        fixture( new SystemTimerFixture( false ) );
        TestDirectoryFixture testDirectoryFixture = fixture( new TestDirectoryFixture() );
        MongoFixture mongoFixture = fixture( new MongoFixture( "MONGO" ) );
        accountFixture = fixture( new AccountFixture( testDirectoryFixture, mongoFixture ) );
        User.random = new Random() {
            @Override
            public void nextBytes( byte[] bytes ) {
                System.arraycopy( randomBytes, 0, bytes, 0, 20 );
            }
        };
    }

    @BeforeMethod
    @Override
    public void fixBeforeMethod() {
        Dates.setTimeFixed( 2010, 1, 23, 17, 22, 49 );

        super.fixBeforeMethod();
    }

    @AfterMethod
    public void afterMethod() {
        try {
            accountFixture.assertLogout();
        } catch( AssertionError | Exception e ) {
            if( !e.getMessage().contains( "code = 401" ) ) {
                log.error( e.getMessage(), e );
            }
        }
    }

    @Test
    public void current() {
        Dates.setTimeFixed( 2025, 1, 24, 17, 22, 49 );

        assertGet( accountFixture.httpUrl( "/user/current" ) )
            .hasCode( Http.StatusCode.UNAUTHORIZED );
        accountFixture.assertAdminLogin();
        UserData admin = accountFixture.userStorage().get( DEFAULT_ADMIN_EMAIL ).orElseThrow();
        assertGet( accountFixture.httpUrl( "/user/current" ) )
            .respondedJson( """
                {
                  "accessKey" : "SSKRSYSXXIPP",
                  "apiKey" : "%s",
                  "banned" : false,
                  "confirmed" : true,
                  "created" : "2010-01-23T17:22:49.000Z",
                  "defaultOrganization" : "SYSTEM",
                  "email" : "xenoss@xenoss.io",
                  "firstName" : "System",
                  "lastLogin" : "2025-01-24",
                  "lastName" : "Admin",
                  "modified" : "2025-01-24T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "ADMIN",
                    "SYSTEM" : "ADMIN"
                  },
                  "secretKey" : "CIQREMSCLEJSEERYOBRBIIYSAUJHEFJE",
                  "tfaEnabled" : false
                }""".formatted( admin.user.apiKey ) );
    }

    @Test
    public void get() {
        accountFixture.assertAdminLogin();
        UserData organizationAdmin = accountFixture.userStorage().get( DEFAULT_ORGANIZATION_ADMIN_EMAIL ).orElseThrow();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + organizationAdmin.user.email ) )
            .respondedJson( """
                {
                  "accessKey" : "HMWDDRMHNGKU",
                  "apiKey" : "%s",
                  "banned" : false,
                  "confirmed" : true,
                  "created" : "2010-01-23T17:22:49.000Z",
                  "defaultOrganization" : "DFLT",
                  "email" : "orgadmin@admin.com",
                  "firstName" : "Johnny",
                  "lastName" : "Walker",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "ORGANIZATION_ADMIN"
                  },
                  "secretKey" : "CIQREMSCLEJSEERYOBRBIIYSAUJHEFJE",
                  "tfaEnabled" : false
                }""".formatted( organizationAdmin.user.apiKey ) );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + organizationAdmin.user.email ) )
            .respondedJson( """
                {
                  "accessKey" : "HMWDDRMHNGKU",
                  "apiKey" : "%s",
                  "banned" : false,
                  "confirmed" : true,
                  "created" : "2010-01-23T17:22:49.000Z",
                  "defaultOrganization" : "DFLT",
                  "email" : "orgadmin@admin.com",
                  "firstName" : "Johnny",
                  "lastLogin" : "2010-01-23",
                  "lastName" : "Walker",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "ORGANIZATION_ADMIN"
                  },
                  "secretKey" : "CIQREMSCLEJSEERYOBRBIIYSAUJHEFJE",
                  "tfaEnabled" : false
                }""".formatted( organizationAdmin.user.apiKey ) );
        accountFixture.assertLogout();
    }

    @Test
    public void noSecureData() {
        Dates.setTimeFixed( 2025, 1, 24, 17, 22, 49 );

        UserData user = accountFixture.userStorage().createUser( new User( null, "user@user.com", "Johnny", "Walker",
            "pass", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ), Storage.MODIFIED_BY_SYSTEM ).object;
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + user.user.email ) )
            .respondedJson( """
                {
                   "banned" : false,
                   "confirmed" : true,
                   "created" : "2025-01-24T17:22:49.000Z",
                   "email" : "user@user.com",
                   "firstName" : "Johnny",
                   "lastName" : "Walker",
                   "modified" : "2025-01-24T17:22:49.000Z",
                   "roles" : {
                     "DFLT" : "USER"
                   },
                   "tfaEnabled" : false
                 }""" );
        accountFixture.assertLogout();
    }

    @Test
    public void accessOtherOrgUser() {
        OrganizationData organizationData = accountFixture.organizationStorage().storeOrganization( new Organization( "THRRG", "otherOrg" ), Storage.MODIFIED_BY_SYSTEM );
        UserData user = accountFixture.userStorage().createUser( new User( null, "other@other.com", "Other", "User",
            "pass", false ), Map.of( organizationData.organization.id, USER ), Storage.MODIFIED_BY_SYSTEM ).object;

        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/user/" + DEFAULT_ORGANIZATION_ID + "/" + user.user.email ) )
            .respondedJson( HTTP_NOT_FOUND, "validation failed", "{\"errors\":[\"not found other@other.com\"]}" );
        accountFixture.assertLogout();
    }

    @Test
    public void loginEmailCaseInsencitive() {
        accountFixture.assertLogin( DEFAULT_ADMIN_EMAIL.toUpperCase(), DEFAULT_PASSWORD );
        UserData organizationAdmin = accountFixture.userStorage().get( DEFAULT_ORGANIZATION_ADMIN_EMAIL ).orElseThrow();
        assertGet( accountFixture.httpUrl( "/user/DFLT/ORGADMIN@ADMIN.COM" ) )
            .respondedJson( """
                {
                  "accessKey" : "HMWDDRMHNGKU",
                  "apiKey" : "%s",
                  "banned" : false,
                  "confirmed" : true,
                  "created" : "2010-01-23T17:22:49.000Z",
                  "defaultOrganization" : "DFLT",
                  "email" : "orgadmin@admin.com",
                  "firstName" : "Johnny",
                  "lastName" : "Walker",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "ORGANIZATION_ADMIN"
                  },
                  "secretKey" : "CIQREMSCLEJSEERYOBRBIIYSAUJHEFJE",
                  "tfaEnabled" : false
                }""".formatted( organizationAdmin.user.apiKey ) );
        accountFixture.assertLogout();
    }

}
