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

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;
import oap.util.Result;
import oap.ws.Response;
import oap.ws.Session;
import oap.ws.SessionManager;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.AbstractSecureWS;
import oap.ws.sso.Authentication;
import oap.ws.sso.AuthenticationFailure;
import oap.ws.sso.Authenticator;
import oap.ws.sso.Credentials;
import oap.ws.sso.TokenCredentials;
import oap.ws.sso.User;
import oap.ws.sso.WsSecurity;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.util.Map;
import java.util.Optional;

import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.COOKIE;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.sso.AuthenticationFailure.TFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.TOKEN_NOT_VALID;
import static oap.ws.sso.AuthenticationFailure.WRONG_ORGANIZATION;
import static oap.ws.sso.AuthenticationFailure.WRONG_TFA_CODE;
import static oap.ws.sso.SSO.authenticatedResponse;
import static oap.ws.sso.SSO.logoutResponse;
import static oap.ws.sso.SSO.notAuthenticatedResponse;
import static oap.ws.validate.ValidationErrors.empty;

@Slf4j
@SuppressWarnings( "unused" )
public class AuthWS extends AbstractSecureWS {

    private final Authenticator authenticator;
    private final SessionManager sessionManager;
    private final UserStorage userStorage;

    private final OauthService oauthService;

    public AuthWS( Authenticator authenticator, UserStorage userStorage,
                   SessionManager sessionManager, OauthService oauthService ) {
        this.authenticator = authenticator;
        this.userStorage = userStorage;
        this.sessionManager = sessionManager;
        this.oauthService = oauthService;
    }

    public AuthWS( Authenticator authenticator, UserStorage userStorage, SessionManager sessionManager ) {
        this( authenticator, userStorage, sessionManager, null );
    }

    @WsMethod( method = POST, path = "/login", description = "Logs in with email/password credentials from the request body" )
    public Response login( @WsParam( from = BODY, description = "Login credentials: email, password and an optional TFA code" )
                           Credentials credentials,
                           @WsParam( from = SESSION, description = "Currently authenticated user, if any" )
                           Optional<User> loggedUser,
                           Session session ) {
        return login( credentials.email, credentials.password, Optional.ofNullable( credentials.tfaCode ), loggedUser, session );
    }

    @WsMethod( method = GET, path = "/login", description = "Logs in with email/password query parameters" )
    public Response login( @WsParam( description = "User email" )
                           String email,
                           @WsParam( description = "User password" )
                           String password,
                           @WsParam( from = BODY, description = "TFA code, required when the user has TFA enabled" )
                           Optional<String> tfaCode,
                           @WsParam( from = SESSION, description = "Currently authenticated user, if any" )
                           Optional<oap.ws.sso.User> loggedUser,
                           Session session ) {
        loggedUser.ifPresent( user -> logout( loggedUser, session ) );
        Result<Authentication, AuthenticationFailure> result = authenticator.authenticate( email, password, tfaCode );
        if( result.isSuccess() ) return authenticatedResponse( result.getSuccessValue(),
            sessionManager.cookieDomain, sessionManager.cookieSecure );
        else if( TFA_REQUIRED == result.getFailureValue() )
            return notAuthenticatedResponse( BAD_REQUEST, AccountValidationMessage.TFA_CODE_REQUIRED.message(), sessionManager.cookieDomain );
        else if( WRONG_TFA_CODE == result.getFailureValue() ) {
            return notAuthenticatedResponse( BAD_REQUEST, AccountValidationMessage.TFA_CODE_INCORRECT.message(), sessionManager.cookieDomain );
        } else
            return notAuthenticatedResponse( UNAUTHORIZED, AccountValidationMessage.USERNAME_OR_PASSWORD_INVALID.message(), sessionManager.cookieDomain );
    }

    @WsMethod( method = POST, path = "/oauth/login", description = "Logs in using a social/OAuth provider access token" )
    public Response login( @WsParam( from = BODY, description = "OAuth provider source and access token, with an optional TFA code" )
                           TokenCredentials credentials,
                           @WsParam( from = SESSION, description = "Currently authenticated user, if any" )
                           Optional<oap.ws.sso.User> loggedUser,
                           Session session ) {
        loggedUser.ifPresent( user -> logout( loggedUser, session ) );
        TokenInfo tokenInfo = oauthService.getOauthProvider( credentials.source ).getTokenInfo( credentials.accessToken ).orElse( null );
        if( tokenInfo != null ) {
            Result<Authentication, AuthenticationFailure> result = authenticator.authenticate( tokenInfo.email, credentials.tfaCode );
            if( result.isSuccess() ) return authenticatedResponse( result.getSuccessValue(),
                sessionManager.cookieDomain, sessionManager.cookieSecure );
            else if( TFA_REQUIRED == result.getFailureValue() )
                return notAuthenticatedResponse( BAD_REQUEST, AccountValidationMessage.TFA_CODE_REQUIRED.message(), sessionManager.cookieDomain );
            else if( WRONG_TFA_CODE == result.getFailureValue() ) {
                return notAuthenticatedResponse( BAD_REQUEST, AccountValidationMessage.TFA_CODE_INCORRECT.message(), sessionManager.cookieDomain );
            } else
                return notAuthenticatedResponse( UNAUTHORIZED, AccountValidationMessage.LOGIN_USER_NOT_FOUND.message(), sessionManager.cookieDomain );
        }
        return notAuthenticatedResponse( UNAUTHORIZED, AccountValidationMessage.TOKEN_EMPTY.message(), sessionManager.cookieDomain );
    }

    @SuppressWarnings( "ParameterName" )
    @WsMethod( method = GET, path = "/switch/{organizationId}", description = "Switches the active organization for the current session and issues a new token" )
    public Response switchOrganization( @WsParam( from = PATH, description = "Id of the organization to switch to" )
                                        String organizationId,
                                        @WsParam( from = SESSION, description = "Currently authenticated user, if any" )
                                        Optional<oap.ws.sso.User> loggedUser,
                                        @WsParam( from = COOKIE, description = "Current access token" )
                                        String Authorization,
                                        Session session ) {
        loggedUser.ifPresent( user -> logout( loggedUser, session ) );
        Result<Authentication, AuthenticationFailure> result = authenticator.authenticateWithActiveOrgId( Authorization, organizationId );
        if( result.isSuccess() ) return authenticatedResponse( result.getSuccessValue(),
            sessionManager.cookieDomain, sessionManager.cookieSecure );
        else if( WRONG_ORGANIZATION == result.getFailureValue() )
            return notAuthenticatedResponse( FORBIDDEN, AccountValidationMessage.ORGANIZATION_MISMATCH.message(), sessionManager.cookieDomain );
        else if( TOKEN_NOT_VALID == result.getFailureValue() ) {
            return notAuthenticatedResponse( UNAUTHORIZED, AccountValidationMessage.TOKEN_INVALID.message(), sessionManager.cookieDomain );
        } else
            return notAuthenticatedResponse( UNAUTHORIZED, AccountValidationMessage.LOGIN_USER_NOT_FOUND.message(), sessionManager.cookieDomain );
    }

    @WsMethod( method = GET, path = "/logout", description = "Logs out the current user and invalidates the session" )
    @WsSecurity( realm = WsSecurity.USER, permissions = {} )
    public Response logout( @WsParam( from = SESSION, description = "Currently authenticated user, if any" )
                            Optional<oap.ws.sso.User> loggedUser,
                            Session session ) {
        loggedUser.ifPresent( user -> {
            log.debug( "Invalidating token for user [{}]", user.getEmail() );
            authenticator.invalidate( user.getId() );
        } );
        session.invalidate();
        return logoutResponse( sessionManager.cookieDomain );
    }

    protected ValidationErrors validateUserAccess( Optional<String> email, oap.ws.sso.User loggedUser ) {
        return email
            .filter( e -> !loggedUser.getEmail().equalsIgnoreCase( e ) )
            .map( e -> empty().statusCode( FORBIDDEN ).error( AccountValidationMessage.USER_ACCESS_DENIED, Map.of( "email", loggedUser.getEmail() ), null ).endCode() )
            .orElse( empty() );
    }

    @WsMethod( method = GET, path = "/whoami", description = "Returns the currently authenticated user" )
    @WsValidate( "validateUserLoggedIn" )
    @WsSecurity( realm = WsSecurity.USER, permissions = {} )
    public Optional<UserView> whoami( @WsParam( from = SESSION, description = "Currently authenticated user" )
                                      Optional<oap.ws.sso.User> loggedUser ) {
        return loggedUser
            .flatMap( user -> userStorage.getMetadata( user.getEmail() ) )
            .map( Users::userMetadataToView );
    }
}
