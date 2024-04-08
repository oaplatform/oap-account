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
import oap.application.testng.AbstractKernelFixture;
import oap.http.test.HttpAsserts;
import oap.json.Binder;
import oap.mail.MailQueue;
import oap.storage.mongo.MongoFixture;
import oap.testng.TestDirectoryFixture;
import oap.ws.account.AccountMailman;
import oap.ws.account.Accounts;
import oap.ws.account.AccountsService;
import oap.ws.account.OrganizationStorage;
import oap.ws.account.User;
import oap.ws.account.UserData;
import oap.ws.account.UserStorage;
import oap.ws.sso.UserProvider;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static oap.io.Resources.urlOrThrow;

/**
 * variables:
 * <ul>
 *     <li>mongo.* - {@link oap.storage.mongo.MongoFixture}</li>
 *     <li>SESSION_MANAGER_EXPIRATION_TIME</li>
 *     <li>MONGO_MIGRATIONS_PACKAGE</li>
 *     <li>DEFAULT_SYSTEM_ADMIN_EMAIL</li>
 *     <li>DEFAULT_SYSTEM_ADMIN_PASSWORD</li>
 *     <li>DEFAULT_SYSTEM_ADMIN_FIRST_NAME</li>
 *     <li>DEFAULT_SYSTEM_ADMIN_LAST_NAME</li>
 *     <li>DEFAULT_SYSTEM_ADMIN_ROLES</li>
 *     <li>DEFAULT_SYSTEM_READ_ONLY</li>
 *     <li>DEFAULT_ORGANIZATION_ID</li>
 *     <li>DEFAULT_ORGANIZATION_NAME</li>
 *     <li>DEFAULT_ORGANIZATION_DESCRIPTION</li>
 *     <li>DEFAULT_ORGANIZATION_READ_ONLY</li>
 * </ul>
 *
 * @see oap.application.testng.AbstractKernelFixture
 */
@Slf4j
public class AccountFixture extends AbstractKernelFixture<AccountFixture> {
    public static final String DEFAULT_PASSWORD = "Xenoss123";
    public static final String DEFAULT_ACCOUNT_ID = "DFLTACCT";
    public static final String DEFAULT_ORGANIZATION_ID = "DFLT";
    public static final String DEFAULT_ORGANIZATION_ADMIN_EMAIL = "orgadmin@admin.com";
    public static final String SYSTEM_ORGANIZATION_ADMIN_EMAIL = "systemadmin@admin.com";
    public static final String DEFAULT_ADMIN_EMAIL = "xenoss@xenoss.io";
    public static final User ORG_ADMIN_USER = new User( "org@admin.com", "Joe", "Haserton", DEFAULT_PASSWORD, true );
    public static final User REGULAR_USER = new User( "user@admin.com", "Joe", "Epstein", DEFAULT_PASSWORD, true );

    public AccountFixture( TestDirectoryFixture testDirectoryFixture, MongoFixture mongoFixture ) {
        super( testDirectoryFixture, urlOrThrow( AccountFixture.class, "/application-account.fixture.conf" ) );

        addDependency( "mongo", mongoFixture );

        define( "SESSION_MANAGER_EXPIRATION_TIME", "24h" );

        withMigration( "oap.ws.account.testing.migration" );
    }

    public AccountFixture withMigration( String migrationPackage ) {
        define( "MONGO_MIGRATIONS_PACKAGE", migrationPackage );
        return this;
    }

    public AccountFixture withSessionManagerExpirationTime( long value, TimeUnit timeUnit ) {
        define( "SESSION_MANAGER_EXPIRATION_TIME", timeUnit.toMillis( value ) );
        return this;
    }

    public void assertAdminLogin() {
        assertLogin( DEFAULT_ADMIN_EMAIL, DEFAULT_PASSWORD );
    }

    public void assertSystemAdminLogin() {
        assertLogin( SYSTEM_ORGANIZATION_ADMIN_EMAIL, DEFAULT_PASSWORD );
    }

    public void assertOrgAdminLogin() {
        assertLogin( DEFAULT_ORGANIZATION_ADMIN_EMAIL, DEFAULT_PASSWORD );
        SecureWSHelper.assertSwitchOrganization( DEFAULT_ORGANIZATION_ID, defaultHttpPort() );
    }

    public void assertLogin( String login, String password ) {
        SecureWSHelper.assertLogin( login, password, defaultHttpPort() );
    }

    public void assertLogin( String login, String password, String tfaCode ) {
        SecureWSHelper.assertLogin( login, password, tfaCode, defaultHttpPort() );
    }

    public void assertLoginIntoOrg( String login, String password, String orgId ) {
        assertLogin( login, password );
        assertSwitchOrganization( orgId );
    }

    public void assertSwitchOrganization( String orgId ) {
        SecureWSHelper.assertSwitchOrganization( orgId, defaultHttpPort() );
    }

    public void assertWrongTfaLogin( String login, String password, String tfaCode ) {
        SecureWSHelper.assertWrongTfaLogin( login, password, tfaCode, defaultHttpPort() );
    }

    public void assertLoginWithFBToken() {
        SecureWSHelper.assertLoginWithFBToken( defaultHttpPort() );
    }

    public void assertTfaRequiredLogin( String login, String password ) {
        SecureWSHelper.assertTfaRequiredLogin( login, password, defaultHttpPort() );
    }

    public void assertLoginWithFBTokenWithTfaRequired() {
        SecureWSHelper.assertLoginWithFBTokenWithTfaRequired( defaultHttpPort() );
    }

    public void assertLoginWithFBTokenWithWrongTfa() {
        SecureWSHelper.assertLoginWithFBTokenWithWrongTfa( defaultHttpPort() );
    }

    public void assertLogout() {
        SecureWSHelper.assertLogout( defaultHttpPort() );
    }

    public OrganizationStorage organizationStorage() {
        return service( "oap-account", OrganizationStorage.class );
    }

    public Accounts accounts() {
        return service( "oap-account", AccountsService.class );
    }

    public UserStorage userStorage() {
        return service( "oap-account", UserStorage.class );
    }

    public AccountMailman accountMailman() {
        return service( "oap-account", AccountMailman.class );
    }

    public MailQueue mailQueue() {
        return service( "oap-mail", MailQueue.class );
    }

    public String httpUrl( String url ) {
        return HttpAsserts.httpUrl( defaultHttpPort(), url );
    }

    public AccountFixture withDefaultSystemAdmin( String email, String password, String firstName, String lastName, Map<String, String> roles, boolean ro ) {
        define( "DEFAULT_SYSTEM_ADMIN_EMAIL", email );
        define( "DEFAULT_SYSTEM_ADMIN_PASSWORD", password );
        define( "DEFAULT_SYSTEM_ADMIN_FIRST_NAME", firstName );
        define( "DEFAULT_SYSTEM_ADMIN_LAST_NAME", lastName );
        define( "DEFAULT_SYSTEM_ADMIN_ROLES", "json(" + Binder.json.marshal( roles ) + ")" );
        define( "DEFAULT_SYSTEM_READ_ONLY", ro );

        return this;
    }

    public AccountFixture withDefaultOrganization( String id, String name, String description, boolean ro ) {
        define( "DEFAULT_ORGANIZATION_ID", id );
        define( "DEFAULT_ORGANIZATION_NAME", name );
        define( "DEFAULT_ORGANIZATION_DESCRIPTION", description );
        define( "DEFAULT_ORGANIZATION_READ_ONLY", ro );

        return this;
    }

    @Override
    public void after() {
        try {
            assertLogout();
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
        }
        super.after();
    }

    public User addUser( String mail, String pass, Map<String, String> roles ) {
        return accounts().createUser( new User( mail, mail, mail, pass, true ), roles ).user;
    }

    public User addUser( String mail, String pass, Map<String, String> roles, boolean tfaEnabled ) {
        User user = new User( mail, mail, mail, pass, true, tfaEnabled );
        if( tfaEnabled ) {
            user.secretKey = UserProvider.toAccessKey( mail );
        }
        return accounts().createUser( user, roles ).user;
    }

    public UserData addUser( UserData userData ) {
        UserData cloned = Binder.json.clone( userData );
        cloned.user.encryptPassword( cloned.user.password );

        return userStorage().store( cloned );
    }

    public AccountFixture withMailMock() {
        withConfResource( getClass(), "/application-account.fixture-mail-mock.conf" );

        return this;
    }
}
