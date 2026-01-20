/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.account.ws.AbstractWS;
import oap.ws.sso.WsSecurity;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.net.HttpURLConnection;
import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.account.OrganizationWS.ORGANIZATION_ID;
import static oap.ws.account.Permissions.MANAGE_SELF;
import static oap.ws.account.Permissions.USER_READ;
import static oap.ws.sso.WsSecurity.USER;

@Slf4j
public class UserWS extends AbstractWS {

    protected UserStorage userStorage;

    public UserWS( UserStorage userStorage ) {
        this.userStorage = userStorage;
    }

    @WsMethod( method = GET, path = "/{organizationId}/{email}", description = "Returns user with given email" )
    @WsSecurity( realm = ORGANIZATION_ID, permissions = { USER_READ, MANAGE_SELF } )
    @WsValidate( { "validateOrganizationAccess", "validateSameOrganization" } )
    public Optional<UserView> get( @WsParam( from = PATH ) String organizationId,
                                   @WsParam( from = PATH ) String id,
                                   @WsParam( from = SESSION ) UserData loggedUser ) {
        return userStorage.getMetadata( id )
            .map( u -> id.equals( loggedUser.user.id ) || isSystem( loggedUser )
                ? Users.userMetadataToSecureView( u )
                : Users.userMetadataToView( u ) );
    }

    protected ValidationErrors validateSameOrganization( String organizationId, String id ) {
        return userStorage.getMetadata( id )
            .filter( user -> user.object.canAccessOrganization( organizationId ) )
            .map( user -> ValidationErrors.empty() )
            .orElseGet( () -> ValidationErrors.error( HttpURLConnection.HTTP_NOT_FOUND, "not found " + id ) );
    }

    @WsMethod( method = GET, path = "/current", description = "Returns a current logged user" )
    @WsValidate( { "validateUserLoggedIn" } )
    @WsSecurity( realm = USER, permissions = {} )
    public Optional<UserSecureView> current( @WsParam( from = SESSION ) Optional<UserData> loggedUser ) {
        return loggedUser
            .flatMap( u -> userStorage.getMetadata( u.user.id ) )
            .map( Users::userMetadataToSecureView );
    }
}
