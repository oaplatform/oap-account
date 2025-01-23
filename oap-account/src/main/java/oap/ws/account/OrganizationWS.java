/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.json.ext.Ext;
import oap.storage.Metadata;
import oap.util.Stream;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.account.utils.TfaUtils;
import oap.ws.account.ws.AbstractWS;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.WsSecurity;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;
import oap.ws.validate.WsValidateJson;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.undertow.util.StatusCodes.BAD_REQUEST;
import static io.undertow.util.StatusCodes.NOT_FOUND;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.account.Permissions.ACCOUNT_ADD;
import static oap.ws.account.Permissions.ACCOUNT_DELETE;
import static oap.ws.account.Permissions.ACCOUNT_LIST;
import static oap.ws.account.Permissions.ACCOUNT_READ;
import static oap.ws.account.Permissions.ACCOUNT_STORE;
import static oap.ws.account.Permissions.ASSIGN_ROLE;
import static oap.ws.account.Permissions.BAN_USER;
import static oap.ws.account.Permissions.MANAGE_SELF;
import static oap.ws.account.Permissions.ORGANIZATION_APIKEY;
import static oap.ws.account.Permissions.ORGANIZATION_LIST_USERS;
import static oap.ws.account.Permissions.ORGANIZATION_READ;
import static oap.ws.account.Permissions.ORGANIZATION_STORE;
import static oap.ws.account.Permissions.ORGANIZATION_STORE_USER;
import static oap.ws.account.Permissions.ORGANIZATION_UPDATE;
import static oap.ws.account.Permissions.ORGANIZATION_USER_PASSWD;
import static oap.ws.account.Permissions.UNBAN_USER;
import static oap.ws.account.Permissions.USER_APIKEY;
import static oap.ws.account.Permissions.USER_PASSWD;
import static oap.ws.account.Roles.ADMIN;
import static oap.ws.account.Roles.ORGANIZATION_ADMIN;
import static oap.ws.account.utils.TfaUtils.getGoogleAuthenticatorCode;
import static oap.ws.sso.WsSecurity.SYSTEM;
import static oap.ws.sso.WsSecurity.USER;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

@Slf4j
@SuppressWarnings( "unused" )
public class OrganizationWS extends AbstractWS {

    public static final String ORGANIZATION_ID = "organizationId";
    protected final OrganizationStorage organizationStorage;
    protected final UserStorage userStorage;
    protected final OauthService oauthService;
    protected final AccountMailman mailman;
    protected final String confirmUrlFinish;
    protected final boolean selfRegistrationEnabled;
    protected final SecurityRoles roles;

    public OrganizationWS( OrganizationStorage organizationStorage,
                           UserStorage userStorage,
                           AccountMailman mailman,
                           String confirmUrlFinish,
                           boolean selfRegistrationEnabled,
                           OauthService oauthService, SecurityRoles roles ) {
        this.organizationStorage = organizationStorage;
        this.userStorage = userStorage;
        this.mailman = mailman;
        this.confirmUrlFinish = confirmUrlFinish;
        this.selfRegistrationEnabled = selfRegistrationEnabled;
        this.oauthService = oauthService;
        this.roles = roles;
    }

    private static OrganizationData.View organizationMetadataToView( Metadata<OrganizationData> metadata ) {
        OrganizationData organizationData = metadata.object;
        Organization organization = organizationData.organization;

        return new OrganizationData.View(
            organization.id, organization.name, organization.description,
            organizationData.accounts.stream().toList(),
            new DateTime( metadata.created, DateTimeZone.UTC ),
            new DateTime( metadata.modified, DateTimeZone.UTC ),
            organization.ext );
    }

    @WsMethod( method = POST, path = "/{organizationId}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_UPDATE } )
    @WsValidate( "validateOrganizationAccess" )
    public Organization store( @WsParam( from = PATH ) String organizationId,
                               @WsValidateJson( schema = Organization.SCHEMA ) @WsParam( from = BODY ) Organization organization,
                               @WsParam( from = SESSION ) UserData loggedUser ) {

        log.debug( "store id {} organization {}", organizationId, organization );
        return organizationStorage.storeOrganization( organization ).organization;
    }

    @WsMethod( method = POST, path = "/" )
    @WsSecurity( permissions = { ORGANIZATION_STORE } )
    public Organization store( @WsValidateJson( schema = Organization.SCHEMA ) @WsParam( from = BODY ) Organization organization,
                               @WsParam( from = SESSION ) UserData loggedUser ) {
        log.debug( "store organization {}", organization );

        return organizationStorage.storeOrganization( organization ).organization;
    }

    @WsMethod( method = GET, path = "/{organizationId}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_READ } )
    @WsValidate( "validateOrganizationAccess" )
    public Optional<OrganizationData.View> get( @WsParam( from = PATH ) String organizationId, @WsParam( from = SESSION ) UserData loggedUser ) {
        return organizationStorage
            .getMetadata( organizationId )
            .map( OrganizationWS::organizationMetadataToView );
    }

    @WsMethod( method = GET, path = "/" )
    @WsValidate( { "validateUserLoggedIn" } )
    @WsSecurity( realm = USER, permissions = {} )
    public List<OrganizationData.View> list( @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        return organizationStorage.selectMetadata()
            .filter( o -> canAccessOrganization( loggedUser.get(), o.object.organization.id ) )
            .map( OrganizationWS::organizationMetadataToView )
            .toList();
    }

    @WsMethod( method = POST, path = "/{organizationId}/accounts" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_STORE } )
    @WsValidate( { "validateOrganizationAccess" } )
    public Optional<OrganizationData.View> storeAccount( @WsParam( from = PATH ) String organizationId,
                                                         @WsParam( from = BODY ) @WsValidateJson( schema = Account.SCHEMA ) Account account,
                                                         @WsParam( from = SESSION ) UserData loggedUser ) {
        return organizationStorage
            .storeAccount( organizationId, account )
            .map( OrganizationWS::organizationMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/accounts" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_LIST } )
    public Optional<List<Account>> accounts( @WsParam( from = PATH ) String organizationId,
                                             @WsParam( from = SESSION ) UserData loggedUser ) {
        return organizationStorage.get( organizationId )
            .map( o -> Stream.of( o.accounts )
                .filter( a -> canAccessAccount( loggedUser, organizationId, a.id ) )
                .toList() );
    }

    @WsMethod( method = GET, path = "/{organizationId}/accounts/{accountId}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_READ } )
    @WsValidate( { "validateOrganizationAccess", "validateAccountAccess" } )
    public Optional<Account> account( @WsParam( from = PATH ) String organizationId,
                                      @WsParam( from = PATH ) String accountId,
                                      @WsParam( from = SESSION ) UserData loggedUser ) {
        return organizationStorage.get( organizationId ).flatMap( o -> o.accounts.get( accountId ) );
    }

    @WsMethod( method = POST, path = "/{organizationId}/users/{email}/accounts/add" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_ADD } )
    public Optional<UserData.View> addAccountToUser( @WsParam( from = PATH ) String organizationId,
                                                     @WsParam( from = PATH ) String email,
                                                     @WsParam( from = QUERY ) String accountId,
                                                     @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.addAccountToUser( email, organizationId, accountId ).map( Users::userMetadataToView );
    }

    @WsMethod( method = POST, path = "/{organizationId}/users/{email}/accounts/remove" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_ADD } )
    public Optional<UserData.View> removeAccountFromUser( @WsParam( from = PATH ) String organizationId,
                                                          @WsParam( from = PATH ) String email,
                                                          @WsParam( from = QUERY ) String accountId,
                                                          @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.removeAccountFromUser( email, organizationId, accountId ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_LIST_USERS } )
    @WsValidate( { "validateOrganizationAccess" } )
    public List<UserData.View> users( @WsParam( from = PATH ) String organizationId,
                                      @WsParam( from = SESSION ) UserData loggedUser ) {
        return Stream.of( userStorage.getUsers( organizationId ) )
            .map( Users::userMetadataToView )
            .toList();
    }

    @WsMethod( method = POST, path = "/{organizationId}/users" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_STORE_USER } )
    @WsValidate( { "validateOrganizationAccess", "validateUsersOrganization", "validateAdminRole", "validateUserRoleNotEmpty", "validateUserRegistered" } )
    public UserData.View storeUser( @WsParam( from = PATH ) String organizationId,
                                    @WsValidateJson( schema = User.SCHEMA ) @WsParam( from = BODY ) User user,
                                    @WsParam( from = QUERY ) Optional<String> role,
                                    @WsParam( from = SESSION ) UserData loggedUser ) {
        user.defaultOrganization = organizationId;
        if( user.create ) {
            Metadata<UserData> userCreated = userStorage.createUser( user, role.map( r -> new HashMap<>( Map.of( organizationId, r ) ) ).orElse( null ) );
            mailman.sendInvitedEmail( userCreated.object );
            return Users.userMetadataToView( userCreated );
        }
        return Users.userMetadataToView( userStorage.updateUser( user.email, u -> u.update( user.firstName, user.lastName, user.tfaEnabled, user.ext ) )
            .orElseThrow() );
    }

    @WsMethod( method = POST, path = "/register" )
    @WsValidate( "validateUserRegistered" )
    public UserData.View register( @WsValidateJson( schema = User.SCHEMA_REGISTRATION ) @WsParam( from = BODY ) User user,
                                   @WsParam( from = QUERY ) String organizationName ) {
        OrganizationData organizationData = organizationStorage.storeOrganization( new Organization( organizationName ) );
        final String orgId = organizationData.organization.id;
        user.defaultOrganization = orgId;
        Metadata<UserData> userCreated = userStorage.createUser( user, new HashMap<>( Map.of( orgId, ORGANIZATION_ADMIN ) ) );
        mailman.sendRegisteredEmail( userCreated.object );
        return Users.userMetadataToView( userCreated );
    }


    @WsMethod( method = POST, path = "/register/oauth" )
    @WsValidate( "validateUserRegistered" )
    public Optional<UserData.View> register( @WsParam( from = QUERY ) String organizationName, String externalOauthToken, OauthProvider source, Ext ext ) {
        OrganizationData organizationData = organizationStorage.storeOrganization( new Organization( organizationName ) );
        final String orgId = organizationData.organization.id;
        final Optional<TokenInfo> tokenInfo = oauthService.getOauthProvider( source ).getTokenInfo( externalOauthToken );
        if( tokenInfo.isPresent() ) {
            final User user = new User( tokenInfo.get().email, tokenInfo.get().firstName, tokenInfo.get().lastName, null, true, false );
            user.ext = ext;
            user.defaultOrganization = orgId;
            Metadata<UserData> userCreated = userStorage.createUser( user, new HashMap<>( Map.of( orgId, ORGANIZATION_ADMIN ) ) );
            mailman.sendRegisteredEmail( userCreated.object );
            return Optional.of( Users.userMetadataToView( userCreated ) );
        }
        return Optional.empty();
    }

    @WsMethod( method = POST, path = "/{organizationId}/users/passwd" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_USER_PASSWD, USER_PASSWD } )
    @WsValidate( { "validateOrganizationAccess", "validatePasswdOrganization", "validateUserAccess" } )
    public Optional<UserData.View> passwd( @WsParam( from = PATH ) String organizationId,
                                           @WsParam( from = BODY ) @WsValidateJson( schema = Passwd.SCHEMA ) Passwd passwd,
                                           @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.passwd( passwd.email, passwd.password ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/apikey/{email}",
        description = "Generate new apikey for user" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_APIKEY, USER_APIKEY } )
    @WsValidate( { "validateOrganizationAccess", "validateCreateApikey" } )
    public Optional<String> refreshApikey( @WsParam( from = PATH ) String organizationId,
                                           @WsParam( from = PATH ) String email,
                                           @WsParam( from = SESSION ) oap.ws.sso.User loggedUser ) {

        return userStorage.refreshApikey( email ).map( u -> u.user.apiKey );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/ban/{email}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { BAN_USER } )
    @WsValidate( { "validateAdminBanAccess" } )
    public Optional<UserData.View> ban( @WsParam( from = PATH ) String organizationId,
                                        @WsParam( from = PATH ) String email,
                                        @WsParam( from = SESSION ) UserData loggedUser ) {


        return userStorage.ban( email, true ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/delete/{email}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ACCOUNT_DELETE } )
    public Optional<UserData.View> delete( @WsParam( from = PATH ) String organizationId,
                                           @WsParam( from = PATH ) String email,
                                           @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.deleteUser( email ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/unban/{email}" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { UNBAN_USER } )
    public Optional<UserData.View> unban( @WsParam( from = PATH ) String organizationId,
                                          @WsParam( from = PATH ) String email,
                                          @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.ban( email, false ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/users/confirm/{email}" )
    @WsValidate( { "validateUserLoggedIn" } )
    @SneakyThrows
    public Response confirm( @WsParam( from = PATH ) String email,
                             @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        log.debug( "confirm email {} loggedUser {} hasPassword {}", email, loggedUser, loggedUser.get().user.hasPassword() );

        User user = loggedUser.get().user;
        URI redirect = new URIBuilder( confirmUrlFinish )
            .addParameter( "apiKey", user.apiKey )
            .addParameter( "accessKey", user.getAccessKey() )
            .addParameter( "email", user.getEmail() )
            .addParameter( "passwd", String.valueOf( !user.hasPassword() ) )
            .build();

        UserData userConfirmed = userStorage.confirm( email ).orElse( null );
        return userConfirmed != null ? Response.redirect( redirect ) : Response.notFound();
    }


    @WsMethod( method = GET, path = "/users/tfa/{email}", description = "Generate authorization link for Google Authenticator" )
    @WsValidate( { "validateUserLoggedIn" } )
    @WsSecurity( realm = USER, permissions = {} )
    public Response generateTfaCode( @WsParam( from = PATH ) String email,
                                     @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        Optional<UserData> user = userStorage.get( email );

        if( user.isPresent() && email.equals( loggedUser.map( u -> u.user.email ).orElse( null ) ) ) {
            String code = getGoogleAuthenticatorCode( user.get().user );
            String encodedCode = Base64.getEncoder().encodeToString( code.getBytes() );
            return Response.ok().withBody( encodedCode );
        }

        return Response.notFound();
    }

    @WsMethod( method = GET, path = "/users/tfa/{email}/{tfacode}/validate", description = "Validate first tfa code from Google Authenticator" )
    @WsValidate( { "validateUserLoggedIn" } )
    public Response validateTfaCode( @WsParam( from = PATH ) String email,
                                     @WsParam( from = PATH ) String tfaCode,
                                     @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        Optional<UserData> user = userStorage.get( email );

        if( user.isPresent() && email.equals( loggedUser.map( u -> u.user.email ).orElse( null ) ) ) {
            final boolean tfaValid = TfaUtils.getTOTPCode( loggedUser.get().user.getSecretKey() ).equals( tfaCode );
            return tfaValid ? Response.ok() : Response.notFound().withReasonPhrase( "TFA code is incorrect" );
        }
        return Response.notFound();
    }

    @WsMethod( method = GET, path = "/users/{email}/default-org/{organizationId}", description = "Set default organization to user" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { MANAGE_SELF } )
    @WsValidate( { "validateUsersOrganization", "validateDefaultOrganization" } )
    public Optional<UserData.View> changeDefaultOrganization( @WsParam( from = PATH ) String email,
                                                              @WsParam( from = PATH ) String organizationId,
                                                              @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.updateUser( email, u -> u.defaultOrganization = organizationId ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/users/{email}/default-account/{accountId}", description = "Set default account in organization to user" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { MANAGE_SELF } )
    @WsValidate( { "validateUsersOrganization", "validateAccountAccess", "validateDefaultAccount" } )
    public Optional<UserData.View> changeDefaultAccount( @WsParam( from = PATH ) String organizationId,
                                                         @WsParam( from = PATH ) String email,
                                                         @WsParam( from = PATH ) String accountId,
                                                         @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.updateUser( email, u -> u.defaultAccounts.put( organizationId, accountId ) ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/add", description = "Add user to existing organization" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_STORE_USER } )
    @WsValidate( "validateAdminOrganizationAccess" )
    public Optional<UserData.View> addUserToOrganization( @WsParam( from = PATH ) String organizationId,
                                                          @WsParam( from = QUERY ) String userOrganizationId,
                                                          @WsParam( from = QUERY ) String email,
                                                          @WsParam( from = QUERY ) String role,
                                                          @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.addOrganizationToUser( email, userOrganizationId, role ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/remove", description = "Remove user from existing organization" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ORGANIZATION_STORE_USER } )
    @WsValidate( "validateAdminOrganizationAccess" )
    public Optional<UserData.View> removeUserFromOrganization( @WsParam( from = PATH ) String organizationId,
                                                               @WsParam( from = QUERY ) String userOrganizationId,
                                                               @WsParam( from = QUERY ) String email,
                                                               @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.removeUserFromOrganization( email, userOrganizationId ).map( Users::userMetadataToView );
    }

    @WsMethod( method = POST, path = "/{organizationId}/assign" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ASSIGN_ROLE } )
    @WsValidate( "validateRole" )
    public Optional<UserData.View> assignRole( @WsParam( from = PATH ) String organizationId,
                                               @WsParam( from = QUERY ) String email,
                                               @WsParam( from = QUERY ) String role,
                                               @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.assignRole( email, organizationId, role ).map( Users::userMetadataToView );
    }

    @WsMethod( method = GET, path = "/{organizationId}/roles", description = "List all available roles with permissions" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ASSIGN_ROLE } )
    public Map<String, Set<String>> listAllRolesWithPermissions( @WsParam( from = PATH ) String organizationId,
                                                                 @WsParam( from = SESSION ) oap.ws.sso.User loggedUser ) {
        return roles.roles().stream().collect( Collectors.toMap( Function.identity(), roles::permissionsOf ) );
    }

    @WsMethod( path = "/{organizationId}/user/roles", method = GET, description = "List user roles with permissions" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { ASSIGN_ROLE, MANAGE_SELF } )
    public Map<String, Set<String>> listUserRolesWithPermissions( @WsParam( from = PATH ) String organizationId,
                                                                  @WsParam( from = SESSION ) oap.ws.sso.User loggedUser ) {
        final Collection<String> userRoles = loggedUser.getRoles().values();
        return userRoles.stream()
            .collect( Collectors.toMap(
                Function.identity(),
                roles::permissionsOf,
                ( a, b ) -> a
            ) );
    }

    protected ValidationErrors validateUserAccess( String organizationId, @Nonnull Passwd passwd, @Nonnull UserData loggedUser ) {
        return Objects.equals( passwd.email, loggedUser.user.email )
            || isSystem( loggedUser )
            || isOrganizationAdmin( loggedUser, organizationId )
            ? empty()
            : error( UNAUTHORIZED, "cannot manage " + passwd.email );
    }

    protected ValidationErrors validateUsersOrganization( String organizationId, UserData loggedUser ) {
        return validateEmailOrganizationAccess( organizationId, loggedUser.user.email );
    }

    protected ValidationErrors validatePasswdOrganization( String organizationId, @Nonnull Passwd passwd ) {
        return validateEmailOrganizationAccess( organizationId, passwd.email );
    }

    protected ValidationErrors validateCreateApikey( UserData loggedUser, String organizationId, @Nonnull String email ) {
        final Optional<String> role = loggedUser.getRole( organizationId );
        if( role.isPresent() && ORGANIZATION_ADMIN.equals( role.get() )
            || loggedUser.user.email.equals( email )
            || isSystemAdmin( loggedUser ) ) {
            return empty();
        }
        return error( FORBIDDEN, "User " + loggedUser.user.email + " is not allowed to change apikey of another user " + email );
    }

    private ValidationErrors validateEmailOrganizationAccess( String organizationId, String email ) {
        return userStorage.get( email )
            .filter( u -> !u.canAccessOrganization( organizationId ) && u.getRole( SYSTEM ).isEmpty() )
            .map( u -> error( FORBIDDEN, "User " + email + " does not belong to organization " + organizationId ) )
            .orElse( empty() );
    }

    protected ValidationErrors validateUserRegistered( @Nonnull User user ) {
        if( !selfRegistrationEnabled ) return error( Http.StatusCode.NOT_FOUND, "not available" );
        var existing = userStorage.get( user.email );
        if( existing.isPresent() && user.create )
            return error( Http.StatusCode.CONFLICT, "user with email " + user.email + " already exists" );
        else if( existing.isEmpty() && !user.create )
            return error( Http.StatusCode.NOT_FOUND, "user " + user.email + " does not exists" );
        else return empty();
    }

    protected ValidationErrors validateAdminRole( @Nonnull String organizationId, Optional<String> role, @Nonnull UserData loggedUser ) {
        if( role.isPresent() && ADMIN.equals( role.get() ) && !isSystemAdmin( loggedUser ) ) {
            return error( FORBIDDEN, "Only ADMIN can create another ADMIN" );
        } else return empty();
    }

    private boolean isSystemAdmin( UserData loggedUser ) {
        return ADMIN.equals( loggedUser.roles.get( SYSTEM ) );
    }

    protected ValidationErrors validateUserRoleNotEmpty( @Nonnull UserData loggedUser ) {
        return loggedUser.roles.isEmpty()
            ? error( FORBIDDEN, "User role is required" )
            : empty();
    }

    protected ValidationErrors validateAdminBanAccess( String email, UserData loggedUser, String organizationId ) {
        if( userStorage.get( email ).isPresent() && ADMIN.equals( userStorage.get( email ).get().getRole( organizationId ).orElse( null ) )
            && !ADMIN.equals( loggedUser.getRole( organizationId ).orElse( null ) )
            && !isSystemAdmin( loggedUser ) ) {
            return error( "ADMIN can be banned only by other ADMIN" );
        } else {
            return empty();
        }
    }

    protected ValidationErrors validateAdminOrganizationAccess( String email, UserData loggedUser, String userOrganizationId ) {
        final String loggedUserRoleInNewOrganization = loggedUser.roles.getOrDefault( userOrganizationId, "" );
        if( loggedUserRoleInNewOrganization.isEmpty() && !isSystemAdmin( loggedUser ) ) {
            return error( FORBIDDEN, "User is not allowed to add users to organization (%s)", userOrganizationId );
        }
        if( !loggedUserRoleInNewOrganization.equals( ADMIN ) && !isSystemAdmin( loggedUser ) ) {
            return error( FORBIDDEN, "Only ADMIN can add user to organization" );
        }
        if( userStorage.get( email ).isPresent() && isSystemAdmin( loggedUser ) ) {
            return empty();
        }
        return empty();
    }

    protected ValidationErrors validateDefaultOrganization( String email, String organizationId ) {
        Optional<UserData> user = userStorage.get( email );
        if( user.isEmpty() ) {
            return error( NOT_FOUND, String.format( "User (%s) doesn't exist", email ) );
        }
        final Optional<OrganizationData> organization = organizationStorage.get( organizationId );
        if( organization.isEmpty() ) {
            return error( NOT_FOUND, String.format( "Organization (%s) does not exist", organizationId ) );
        }
        if( organizationId.equals( user.get().user.defaultOrganization ) ) {
            return error( BAD_REQUEST, String.format( "Organization (%s) is already marked as default", organizationId ) );
        }
        return empty();
    }

    protected ValidationErrors validateDefaultAccount( String email, String organizationId, String accountId ) {
        Optional<UserData> user = userStorage.get( email );
        if( user.isEmpty() ) {
            return error( NOT_FOUND, String.format( "User (%s) doesn't exist", email ) );
        }
        final Optional<OrganizationData> organization = organizationStorage.get( organizationId );
        if( organization.isEmpty() ) {
            return error( NOT_FOUND, String.format( "Organization (%s) does not exist", organizationId ) );
        }
        if( organization.get().accounts.get( accountId ).isEmpty() ) {
            return error( NOT_FOUND, String.format( "Account (%s) does not exist in organization (%s)", accountId, organizationId ) );
        }
        if( accountId.equals( user.get().user.defaultAccounts.get( organizationId ) ) ) {
            return error( BAD_REQUEST, String.format( "Account (%s) is already marked as default in organization (%s)", accountId, organizationId ) );
        }
        return empty();
    }

    protected ValidationErrors validateRole( String role ) {
        if( roles.roles().contains( role ) ) {
            return empty();
        } else {
            return error( BAD_REQUEST, String.format( "Role (%s) does not exist", role ) );
        }
    }


    public static class Passwd {
        public static final String SCHEMA = "/oap/ws/account/passwd.schema.conf";
        String email;
        String password;
    }
}
