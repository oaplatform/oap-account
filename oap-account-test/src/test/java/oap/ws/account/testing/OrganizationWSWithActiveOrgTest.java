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

package oap.ws.account.testing;

import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.storage.mongo.MongoFixture;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.ws.account.Account;
import oap.ws.account.Organization;
import oap.ws.account.OrganizationData;
import oap.ws.account.User;
import oap.ws.account.UserData;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.test.HttpAsserts.assertGet;
import static oap.http.test.HttpAsserts.assertPost;
import static oap.mail.test.MessageAssertion.assertMessage;
import static oap.mail.test.MessagesAssertion.assertMessages;
import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.assertString;
import static oap.ws.account.Roles.ADMIN;
import static oap.ws.account.Roles.ORGANIZATION_ADMIN;
import static oap.ws.account.Roles.USER;
import static oap.ws.account.testing.AbstractAccountFixture.DEFAULT_ORGANIZATION_ADMIN_EMAIL;
import static oap.ws.account.testing.AbstractAccountFixture.DEFAULT_ORGANIZATION_ID;
import static oap.ws.account.testing.AbstractAccountFixture.DEFAULT_PASSWORD;
import static oap.ws.account.testing.AbstractAccountFixture.ORG_ADMIN_USER;
import static oap.ws.account.testing.AbstractAccountFixture.REGULAR_USER;
import static oap.ws.validate.testng.ValidationAssertion.assertValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.AssertJUnit.assertTrue;

@Slf4j
public class OrganizationWSWithActiveOrgTest extends Fixtures {
    public static final String TODAY = DateTimeFormat.forPattern( "yyyy-MM-dd" ).print( DateTime.now( UTC ) );

    protected final AccountFixture accountFixture;

    public OrganizationWSWithActiveOrgTest() {
        fixture( new SystemTimerFixture( false ) );
        TestDirectoryFixture testDirectoryFixture = fixture( new TestDirectoryFixture() );
        var mongoFixture = fixture( new MongoFixture( "MONGO" ) );
        accountFixture = fixture( new AccountFixture( testDirectoryFixture, mongoFixture )
            .withConfResource( AbstractAccountFixture.class, "/application-account.fixture-org.conf" ) );
    }

    @BeforeMethod
    @Override
    public void fixBeforeMethod() {
        Dates.setTimeFixed( 2010, 1, 23, 17, 22, 49 );

        super.fixBeforeMethod();
    }

    @AfterMethod
    public void afterMethod() {
        accountFixture.assertLogout();
    }

    @Test
    public void storeOrgAdmin() {
        OrganizationData data = accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.addUser( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLoginIntoOrg( user.user.email, DEFAULT_PASSWORD, data.organization.id );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id ), "{\"id\":\"" + data.organization.id + "\", \"name\":\"newname\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.organizationStorage().get( data.organization.id ) ).isPresent().get()
            .satisfies( d -> assertString( d.organization.name ).isEqualTo( "newname" ) );
    }

    @Test
    public void getOrgAdmin() {
        OrganizationData data = accountFixture.organizationStorage().store( new OrganizationData( new Organization( "test", "test" ) ) );
        UserData user = accountFixture.addUser( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLoginIntoOrg( user.user.email, DEFAULT_PASSWORD, data.organization.id );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id ) )
            .respondedJson( OK, "OK", """
                {
                  "created" : "2010-01-23T17:22:49.000Z",
                  "description" : "test",
                  "id" : "TST",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "name" : "test"
                }""" );
    }

    @Test
    public void listOrgAdmin() {
        Dates.setTimeFixed( 2015, 1, 23, 17, 22, 49 );

        OrganizationData data = accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.addUser( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLogin( user.user.email, DEFAULT_PASSWORD );
        assertGet( accountFixture.httpUrl( "/organizations" ) )
            .respondedJson( OK, "OK", """
                [ {
                  "created" : "2015-01-23T17:22:49.000Z",
                  "description" : "test",
                  "id" : "TST",
                  "modified" : "2015-01-23T17:22:49.000Z",
                  "name" : "test"
                } ]""" );
    }

    @Test
    public void list() {
        Dates.setTimeFixed( 2015, 1, 23, 17, 22, 49 );

        accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ) );
        accountFixture.assertAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations" ) )
            .respondedJson( OK, "OK", """
                [ {
                  "created" : "2015-01-23T17:22:49.000Z",
                  "description" : "test",
                  "id" : "TST",
                  "modified" : "2015-01-23T17:22:49.000Z",
                  "name" : "test"
                }, {
                  "created" : "2010-01-23T17:22:49.000Z",
                  "description" : "Default organization",
                  "id" : "DFLT",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "name" : "Default"
                } ]""" );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations" ) )
            .respondedJson( OK, "OK", """
                [ {
                  "created" : "2010-01-23T17:22:49.000Z",
                  "description" : "Default organization",
                  "id" : "DFLT",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "name" : "Default"
                } ]""" );
    }

    @Test
    public void storeAccountOrgAdmin() {
        OrganizationData data = accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.addUser( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLoginIntoOrg( user.user.email, DEFAULT_PASSWORD, data.organization.id );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ), "{\"name\":\"acc1\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( data.accounts ).containsOnly( new Account( "CC1", "acc1" ) );
    }

    @Test
    public void listAccountsOrgAdmin() {
        OrganizationData data = accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ) );
        accountFixture.organizationStorage().storeAccount( data.organization.id, new Account( "acc2", "acc2" ) );
        accountFixture.organizationStorage().storeAccount( data.organization.id, new Account( "acc1", "acc1" ) );
        UserData user = accountFixture.addUser( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLoginIntoOrg( user.user.email, DEFAULT_PASSWORD, data.organization.id );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ) )
            .respondedJson( OK, "OK", "[{\"id\":\"acc2\", \"name\":\"acc2\"}, {\"id\":\"acc1\", \"name\":\"acc1\"}]" );
    }

    @Test
    public void listAccountsUser() {
        OrganizationData data = accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ) );
        accountFixture.organizationStorage().storeAccount( data.organization.id, new Account( "acc2", "acc2" ) );
        accountFixture.organizationStorage().storeAccount( data.organization.id, new Account( "acc1", "acc1" ) );
        accountFixture.organizationStorage().store( data );
        UserData user = new UserData( REGULAR_USER, Map.of( data.organization.id, USER ) );
        user.addAccount( data.organization.id, "acc1" );
        accountFixture.addUser( user );
        accountFixture.assertLoginIntoOrg( user.user.email, DEFAULT_PASSWORD, data.organization.id );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ) )
            .respondedJson( OK, "OK", "[{\"id\":\"acc1\", \"name\":\"acc1\"}]" );
    }

    @Test
    public void getAccountUser() {
        Account account2 = new Account( "acc2", "acc2" );
        Account account1 = new Account( "acc1", "acc1" );
        OrganizationData data = new OrganizationData( new Organization( "test", "test" ) )
            .addOrUpdateAccount( account2 )
            .addOrUpdateAccount( account1 );
        accountFixture.organizationStorage().store( data );
        UserData user = new UserData( REGULAR_USER, Map.of( data.organization.id, USER ) );
        user.addAccount( data.organization.id, "acc1" );
        accountFixture.addUser( user );
        accountFixture.assertLoginIntoOrg( user.user.email, DEFAULT_PASSWORD, data.organization.id );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/" + account1.id ) )
            .respondedJson( OK, "OK", "{\"id\":\"acc1\", \"name\":\"acc1\"}" );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/" + account2.id ) )
            .hasCode( Http.StatusCode.FORBIDDEN );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/blabla" ) )
            .hasCode( Http.StatusCode.FORBIDDEN );
    }

    @Test
    public void account404() {
        OrganizationData data = accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ) );
        UserData user = accountFixture.addUser( new UserData( ORG_ADMIN_USER, Map.of( data.organization.id, ORGANIZATION_ADMIN ) ) );
        accountFixture.assertLoginIntoOrg( user.user.email, DEFAULT_PASSWORD, data.organization.id );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ), "{\"id\":\"acc1\", \"name\":\"acc1\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( data.accounts ).containsOnly( new Account( "acc1", "acc1" ) );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/acc1" ) )
            .respondedJson( OK, "OK", "{\"id\":\"acc1\", \"name\":\"acc1\"}" );
        assertPost( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts" ), "{\"id\":\"acc1\", \"name\":\"acc1\"}", Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertGet( accountFixture.httpUrl( "/organizations/" + data.organization.id + "/accounts/acc1" ) )
            .respondedJson( OK, "OK", "{\"id\":\"acc1\", \"name\":\"acc1\"}" );
    }

    @Test
    public void users() {
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users" ) )
            .respondedJson( OK, "OK", """
                [ {
                  "banned" : false,
                  "confirmed" : true,
                  "created" : "2010-01-23T17:22:49.000Z",
                  "defaultOrganization" : "SYSTEM",
                  "email" : "systemadmin@admin.com",
                  "firstName" : "System",
                  "lastName" : "Admin",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "ORGANIZATION_ADMIN",
                    "SYSTEM" : "ADMIN"
                  },
                  "tfaEnabled" : false
                }, {
                  "banned" : false,
                  "confirmed" : true,
                  "created" : "2010-01-23T17:22:49.000Z",
                  "defaultOrganization" : "SYSTEM",
                  "email" : "xenoss@xenoss.io",
                  "firstName" : "System",
                  "lastName" : "Admin",
                  "modified" : "2010-01-23T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "ADMIN",
                    "SYSTEM" : "ADMIN"
                  },
                  "tfaEnabled" : false
                }, {
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
                  "tfaEnabled" : false
                } ]""" );
    }

    @Test
    public void storeUserAdminByAdminCreateNew() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "newadmin@admins.com"
                }""" )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( "newadmin@admins.com" ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.canAccessOrganization( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertString( u.getRole( DEFAULT_ORGANIZATION_ID ).orElse( null ) ).isEqualTo( ADMIN );
            } );
    }

    @Test
    public void addUser() {
        String userEmail = "vk@xenoss.io";
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=USER" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "vk@xenoss.io",
                  "roles": { "DFLT":"USER"}
                }""" )
            .hasCode( OK );
        assertEventually( 100, 100, () -> {
            assertMessages( accountFixture.getTransportMock().messages )
                .sentTo( userEmail, message -> assertMessage( message ).hasSubject( "You've been invited" ) );
        } );
        accountFixture.assertLogout();
        assertPost( accountFixture.httpUrl( "/auth/login" ), "{\"email\": \"" + userEmail + "\", \"password\": \"pass\"}" )
            .hasCode( UNAUTHORIZED );
        UserData user = accountFixture.userStorage().get( userEmail ).orElseThrow();
        String confirmUrl = accountFixture.accountMailman().confirmUrl( user );
        assertGet( confirmUrl )
            .hasCode( Http.StatusCode.FOUND )
            .containsHeader( "Location", "http://xenoss.io?apiKey=" + user.user.apiKey + "&accessKey=" + user.getAccessKey()
                + "&email=vk%40xenoss.io" + "&passwd=true" );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd?accessKey=" + user.getAccessKey() + "&apiKey=" + user.user.apiKey ), "{\"email\": \"vk@xenoss.io\", \"password\": \"pass\"}" )
            .hasCode( OK );
        accountFixture.assertLoginIntoOrg( userEmail, "pass", DEFAULT_ORGANIZATION_ID );
    }

    @Test

    public void registerUser() {
        Dates.setTimeFixed( 2025, 1, 24, 17, 22, 49 );

        String userEmail = "vk@xenoss.io";
        assertPost( accountFixture.httpUrl( "/organizations/register?organizationName=xenoss.io" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "vk@xenoss.io",
                  "organization": "xenoss.io",
                  "password": "pass"
                }""" )
            .respondedJson( """
                {
                  "banned" : false,
                  "confirmed" : false,
                  "created" : "2025-01-24T17:22:49.000Z",
                  "defaultOrganization" : "XNSS",
                  "email" : "vk@xenoss.io",
                  "firstName" : "John",
                  "lastName" : "Smith",
                  "modified" : "2025-01-24T17:22:49.000Z",
                  "roles" : {
                    "XNSS" : "ORGANIZATION_ADMIN"
                  },
                  "tfaEnabled" : false
                }""" );
        UserData user = accountFixture.userStorage().get( userEmail ).orElseThrow();
        assertEventually( 100, 100, () -> {
            assertMessages( accountFixture.getTransportMock().messages )
                .sentTo( userEmail, message -> assertMessage( message )
                    .hasSubject( "Registration successful" ) );
        } );
        assertThat( accountFixture.organizationStorage().get( "XNSS" ) ).isNotEmpty();
        assertPost( accountFixture.httpUrl( "/auth/login" ), "{\"email\": \"" + user.user.email + "\", \"password\": \"pass\"}" )
            .hasCode( UNAUTHORIZED );
        String confirmUrl = accountFixture.accountMailman().confirmUrl( user );
        assertGet( confirmUrl )
            .hasCode( Http.StatusCode.FOUND )
            .containsHeader( "Location", "http://xenoss.io?apiKey=" + user.user.apiKey + "&accessKey=" + user.getAccessKey()
                + "&email=vk%40xenoss.io" + "&passwd=false" );
        accountFixture.assertLoginIntoOrg( user.user.email, "pass", "XNSS" );
    }

    @Test
    public void storeUserAdminByAdminNotNewNotExist() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users" ),
            """
                {
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "newadmin@admins.com",
                  "roles": {
                    "XNSS": "ADMIN"
                  },
                  "tfaEnabled" : false
                }
                """ )
            .hasCode( Http.StatusCode.NOT_FOUND )
            .satisfies( response -> assertValidation( response )
                .hasErrors( "user newadmin@admins.com does not exists" ) );
    }

    @Test
    public void storeUserDuplicate() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "newadmin@admins.com"
                }""" )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "newadmin@admins.com"
                }""" )
            .hasCode( Http.StatusCode.CONFLICT )
            .satisfies( response -> assertValidation( response )
                .hasErrors( "user with email newadmin@admins.com already exists" ) );
    }

    @Test
    public void storeUserAdminByNotAdmin() {
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "newadmin@admins.com"
                }""" )
            .hasCode( Http.StatusCode.FORBIDDEN );
    }

    @Test
    public void storeUserWrongOrg() {
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/fake-org/users" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "newadmin@admins.com"
                }""" )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void passwd() {
        var email = "newuser@gmail.com";
        accountFixture.addUser( new UserData( new User( "newuser@gmail.com", "John", "Smith", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLoginIntoOrg( email, "pass123", DEFAULT_ORGANIZATION_ID );
        assertPost( accountFixture.httpUrl( "/organizations/hackit/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"newpass\"}" )
            .hasCode( UNAUTHORIZED );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + DEFAULT_ORGANIZATION_ADMIN_EMAIL + "\", \"password\": \"newpass\"}" )
            .hasCode( UNAUTHORIZED )
            .satisfies( response -> assertValidation( response ).hasErrors( "cannot manage " + DEFAULT_ORGANIZATION_ADMIN_EMAIL ) );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{}" )
            .hasCode( Http.StatusCode.BAD_REQUEST )
            .satisfies( response -> assertValidation( response )
                .hasErrors(
                    "/password: required property is missing",
                    "/email: required property is missing"
                ) );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"newpass\"}" )
            .hasCode( OK );
        accountFixture.assertLogout();
        accountFixture.assertLogin( email, "newpass" );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"forcedpass\"}" )
            .hasCode( OK );
        accountFixture.assertLogout();
        accountFixture.assertLogin( email, "forcedpass" );
        accountFixture.assertLogout();
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/passwd" ), "{\"email\": \"" + email + "\", \"password\": \"adminforcedpass\"}" )
            .hasCode( OK );
        accountFixture.assertLogout();
        accountFixture.assertLogin( email, "adminforcedpass" );
    }

    @Test
    public void ban() {
        Dates.setTimeFixed( 2025, 1, 24, 17, 22, 49 );

        UserData user = accountFixture.addUser( new UserData( new User( "user@admin.com", "Joe", "Epstein", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLogin( "user@admin.com", "pass123" );
        accountFixture.assertLogout();
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/ban/" + user.user.email ) )
            .respondedJson( OK, "OK", """
                {
                  "banned" : true,
                  "confirmed" : true,
                  "created" : "2025-01-24T17:22:49.000Z",
                  "email" : "user@admin.com",
                  "firstName" : "Joe",
                  "lastLogin" : "2025-01-24",
                  "lastName" : "Epstein",
                  "modified" : "2025-01-24T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "USER"
                  },
                  "tfaEnabled" : false
                }""" );
        accountFixture.assertLogout();
        assertPost( accountFixture.httpUrl( "/auth/login" ), "{\"email\": \"" + user.user.email + "\", \"password\": \"pass\"}" )
            .hasCode( UNAUTHORIZED );
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/unban/" + user.user.email ) )
            .respondedJson( OK, "OK", """
                {
                  "banned" : false,
                  "confirmed" : true,
                  "created" : "2025-01-24T17:22:49.000Z",
                  "email" : "user@admin.com",
                  "firstName" : "Joe",
                  "lastLogin" : "2025-01-24",
                  "lastName" : "Epstein",
                  "modified" : "2025-01-24T17:22:49.000Z",
                  "roles" : {
                    "DFLT" : "USER"
                  },
                  "tfaEnabled" : false
                }""" );
        accountFixture.assertLogout();
        accountFixture.assertLogin( "user@admin.com", "pass123" );
    }

    @Test
    public void deleteUserByAdmin() {
        accountFixture.assertAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users?role=ADMIN" ),
            """
                {
                  "create": true,
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "newadmin@admins.com"
                }""" )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( "newadmin@admins.com" ) ).isPresent();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/delete/newadmin@admins.com" ) )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( "newadmin@admins.com" ) ).isNotPresent();
    }

    @Test
    public void addAccountToUserByAdmin() {
        accountFixture.addUser( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertSystemAdminLogin();
        final String account1 = "account1";
        assertPost( accountFixture.httpUrl( "/organizations/" + "testId" + "/users/" + userEmail + "/accounts/add?accountId=" + account1 ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( "testId" ) ).isTrue();
                assertThat( u.accounts.get( "testId" ) ).contains( account1 );
            } );
    }

    @Test
    public void addAccountToUserByAdminWithLimitedRole() {
        accountFixture.addUser( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER, "SYSTEM", USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertLogin( userEmail, DEFAULT_PASSWORD );
        final String account1 = "account1";
        assertPost( accountFixture.httpUrl( "/organizations/" + "testId" + "/users/" + userEmail + "/accounts/add?accountId=" + account1 ), Http.ContentType.APPLICATION_JSON )
            .hasCode( UNAUTHORIZED );
    }

    @Test
    public void addAccountToUserByOrgAdmin() {
        accountFixture.addUser( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        final String account1 = "account1";
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + account1 ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).contains( account1 );
            } );
    }

    @Test
    public void accessToAllAccountsToUser() {
        accountFixture.addUser( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account1" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "*" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).containsOnly( "account2" );
            } );
    }

    @Test
    public void addSeveralAccountsToUser() {
        accountFixture.addUser( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account1" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).containsOnly( "account1", "account2" );
            } );
    }

    @Test
    public void addSeveralDuplicatedAccountsToUser() {
        accountFixture.addUser( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account1" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertPost( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/" + userEmail + "/accounts/add?accountId=" + "account2" ), Http.ContentType.APPLICATION_JSON )
            .hasCode( OK );
        assertThat( accountFixture.userStorage().get( userEmail ) )
            .isPresent()
            .get()
            .satisfies( u -> {
                assertThat( u.accounts.containsKey( DEFAULT_ORGANIZATION_ID ) ).isTrue();
                assertThat( u.accounts.get( DEFAULT_ORGANIZATION_ID ) ).containsOnly( "account1", "account2" );
            } );
    }

    @Test
    public void refreshApiKey() {
        final UserData userData = accountFixture.addUser( new UserData( REGULAR_USER, Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        final String apikeyCurrent = userData.user.apiKey;
        final String userEmail = REGULAR_USER.email;
        accountFixture.assertOrgAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/apikey/" + userEmail ) )
            .hasCode( OK );
        final String newApikey = accountFixture.userStorage().get( userEmail ).get().user.apiKey;
        assertThat( newApikey ).isNotEmpty();
        assertThat( apikeyCurrent ).isNotEqualTo( newApikey );
    }

    @Test
    public void refreshApiKeyByOneUserToAnother() {
        final UserData userData = accountFixture.addUser( new UserData( new User( "newuser@gmail.com", "John", "Smith", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.addUser( new UserData( new User( "jga@test.com" ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLogin( userData.user.email, "pass123" );
        assertGet( accountFixture.httpUrl( "/organizations/" + DEFAULT_ORGANIZATION_ID + "/users/apikey/" + "jga@test.com" ) )
            .hasCode( FORBIDDEN );
    }

    @Test
    public void generateTfaAuthorizationLink() {
        final String email = "john@test.com";
        accountFixture.addUser( new UserData( new User( email, "John", "Smith", "pass123", true ), Map.of( DEFAULT_ORGANIZATION_ID, USER ) ) );
        accountFixture.assertLogin( email, "pass123" );
        assertGet( accountFixture.httpUrl( "/organizations/users/tfa/" + email ) )
            .isOk().satisfies( response -> {
                byte[] decodedBytes = Base64.getDecoder().decode( response.content() );
                String decodedString = new String( decodedBytes );
                assertThat( decodedString.contains( "test.com" ) );
                assertThat( decodedString.contains( email ) );
                assertThat( decodedString.contains( "secretKey" ) );
            } );
    }

    @Test
    public void addOrganizationToUserBySystemAdmin() {
        OrganizationData org1 = accountFixture.organizationStorage().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.organizationStorage().storeOrganization( new Organization( "Second", "test" ) );
        final String orgId = org1.organization.id;

        Map<String, String> roles = new HashMap<>();
        roles.put( orgId, USER );

        final String mail = "user@usr.com";
        UserData user = new UserData( new User( mail, "John", "Smith", "pass123", true ), roles );
        accountFixture.addUser( user );

        accountFixture.assertSystemAdminLogin();
        assertGet( accountFixture.httpUrl( "/organizations/" + org2.organization.id + "/add?userOrganizationId=" + org2.organization.id + "&email=" + mail + "&role=ADMIN" ) ).hasCode( OK );
        assertTrue( accountFixture.userStorage().get( mail ).get().getRoles().containsKey( org2.organization.id ) );
    }

    @Test
    public void addOrganizationToUserByAdminInSeveralOrganizations() {
        OrganizationData org1 = accountFixture.organizationStorage().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.organizationStorage().storeOrganization( new Organization( "Second", "test" ) );

        Map<String, String> adminRoles = new HashMap<>();
        adminRoles.put( org1.organization.id, ADMIN );
        adminRoles.put( org2.organization.id, ADMIN );

        final String adminMail = "orgadmin@usr.com";
        UserData admin = new UserData( new User( adminMail, "John", "Smith", "pass123", true ), adminRoles );

        final String userMail = "user@usr.com";
        Map<String, String> roles = new HashMap<>();
        roles.put( org1.organization.id, USER );
        UserData user = new UserData( new User( userMail, "John", "Smith", "pass", true ), roles );

        accountFixture.addUser( admin );
        accountFixture.addUser( user );

        accountFixture.assertLoginIntoOrg( adminMail, "pass123", org1.organization.id );

        assertGet( accountFixture.httpUrl( "/organizations/" + org1.organization.id + "/add?userOrganizationId=" + org2.organization.id + "&email=" + userMail + "&role=ADMIN" ) ).hasCode( OK );
        assertTrue( accountFixture.userStorage().get( userMail ).get().getRoles().containsKey( org2.organization.id ) );
    }

    @Test
    public void addOrganizationToUserByUserWithDIfferentRolesInOrganizations() {
        OrganizationData org1 = accountFixture.organizationStorage().storeOrganization( new Organization( "First", "test" ) );
        OrganizationData org2 = accountFixture.organizationStorage().storeOrganization( new Organization( "Second", "test" ) );

        Map<String, String> adminRoles = new HashMap<>();
        adminRoles.put( org1.organization.id, ADMIN );
        adminRoles.put( org2.organization.id, USER );

        final String adminMail = "orgadmin@usr.com";
        UserData admin = new UserData( new User( adminMail, "John", "Smith", "pass123", true ), adminRoles );

        final String userMail = "user@usr.com";
        Map<String, String> roles = new HashMap<>();
        roles.put( org1.organization.id, USER );
        UserData user = new UserData( new User( userMail, "John", "Smith", "pass", true ), roles );

        accountFixture.addUser( admin );
        accountFixture.addUser( user );

        accountFixture.assertLoginIntoOrg( adminMail, "pass123", org1.organization.id );

        assertGet( accountFixture.httpUrl( "/organizations/" + org1.organization.id + "/add?userOrganizationId=" + org2.organization.id + "&email=" + userMail + "&role=ADMIN" ) ).hasCode( FORBIDDEN );
    }
}
