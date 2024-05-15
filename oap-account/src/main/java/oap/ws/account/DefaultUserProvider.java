package oap.ws.account;

import oap.util.Result;
import oap.ws.SessionManager;
import oap.ws.sso.Authentication;
import oap.ws.sso.AuthenticationFailure;
import oap.ws.sso.JWTExtractor;
import oap.ws.sso.JwtToken;
import oap.ws.sso.JwtTokenGenerator;
import oap.ws.sso.SSO;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.User;
import oap.ws.sso.UserWithCookies;
import oap.ws.sso.WsSecurity;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;

import static oap.ws.account.utils.TfaUtils.getTOTPCode;
import static oap.ws.sso.AuthenticationFailure.TFA_REQUIRED;
import static oap.ws.sso.AuthenticationFailure.UNAUTHENTICATED;
import static oap.ws.sso.AuthenticationFailure.WRONG_TFA_CODE;
import static org.joda.time.DateTimeZone.UTC;

public class DefaultUserProvider implements oap.ws.sso.UserProvider {
    private final UserStorage userStorage;
    private final JWTExtractor jwtExtractor;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final SessionManager sessionManager;
    private final SecurityRoles roles;
    private final boolean useOrganizationLogin;

    public DefaultUserProvider( UserStorage userStorage,
                                JWTExtractor jwtExtractor, JwtTokenGenerator jwtTokenGenerator,
                                SessionManager sessionManager,
                                SecurityRoles roles, boolean useOrganizationLogin ) {
        this.userStorage = userStorage;
        this.jwtExtractor = jwtExtractor;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.sessionManager = sessionManager;
        this.roles = roles;
        this.useOrganizationLogin = useOrganizationLogin;
    }

    @Override
    public Optional<? extends User> getUser( String email ) {
        return userStorage.get( email );
    }

    @Override
    public Result<UserWithCookies, String> getAuthenticatedByAccessToken( Optional<String> accessToken, Optional<String> refreshToken,
                                                                          Optional<String> sessionUserEmail,
                                                                          SecurityRoles clientRoles, String realm, String... wssPermissions ) {
        String organization = null;
        String email = null;

        Optional<SSO.Tokens> responseAccessCookie = Optional.empty();

        JwtToken jwtToken = null;
        if( accessToken.isPresent() || refreshToken.isPresent() ) {
            String token = "";

            JWTExtractor.TokenStatus tokenStatus;
            if( accessToken.isPresent() ) {
                token = JWTExtractor.extractBearerToken( accessToken.get() );
                tokenStatus = jwtExtractor.verifyToken( token );
            } else {
                tokenStatus = JWTExtractor.TokenStatus.EXPIRED;
            }

            if( tokenStatus == JWTExtractor.TokenStatus.EXPIRED ) {
                if( refreshToken.isPresent() ) {
                    String rt = JWTExtractor.extractBearerToken( refreshToken.get() );
                    JWTExtractor.TokenStatus refreshTokenStatus = jwtExtractor.verifyToken( rt );
                    if( refreshTokenStatus == JWTExtractor.TokenStatus.VALID ) {
                        JwtToken jwtRefreshToken = jwtExtractor.decodeJWT( rt );
                        UserData currentUser = userStorage.get( jwtRefreshToken.getUserEmail() ).orElse( null );

                        if( currentUser == null || currentUser.getCounter() != jwtRefreshToken.getCounter() ) {
                            return Result.failure( "an outdated version of the refresh token" );
                        }

                        Authentication.Token responseAccessToken = jwtTokenGenerator.generateAccessToken( currentUser );
                        Authentication.Token responseRefreshAccessToken = jwtTokenGenerator.generateRefreshToken( currentUser );
                        Authentication authentication = new Authentication( responseAccessToken, responseRefreshAccessToken, currentUser );

                        responseAccessCookie = Optional.of( SSO.createAccessAndRefreshTokensFromRefreshToken( authentication, sessionManager.cookieDomain, sessionManager.cookieSecure ) );
                        token = responseAccessToken.jwt;
                        tokenStatus = JWTExtractor.TokenStatus.VALID;
                    }

                }
            }
            if( tokenStatus != JWTExtractor.TokenStatus.VALID ) {
                return Result.failure( "Invalid token: " + token + ", reason: " + tokenStatus );
            }
            jwtToken = jwtExtractor.decodeJWT( token );
            email = jwtToken.getUserEmail();
            organization = jwtToken.getOrganizationId();
        }

        if( hasRealmMismatchError( organization, useOrganizationLogin, realm ) ) {
            return Result.failure( "realm is different from organization logged in" );
        }

        if( sessionUserEmail.isPresent() && email == null ) {
            email = sessionUserEmail.get();
        }

        if( email == null ) {
            return Result.failure( "JWT token is empty" );
        }

        UserData userData = userStorage.get( email ).orElse( null );

        if( userData == null ) {
            return Result.failure( "User not found with email: " + email );
        } else if( userData.banned ) {
            return Result.failure( "User with email " + email + " is banned" );
        } else if( !userData.user.isConfirmed() ) {
            return Result.failure( "User with email " + email + " is not confirmed" );
        }

        if( jwtToken != null && userData.getCounter() != jwtToken.getCounter() ) {
            return Result.failure( "an outdated version of the token" );
        }

        if( !WsSecurity.USER.equals( realm ) ) {
            String role = userData.getRole( realm ).orElse( null );
            if( role == null ) {
                return Result.failure( "user doesn't have access to realm '" + realm + "'" );
            }

            SecurityRoles allRoles = roles.merge( clientRoles );
            if( !allRoles.granted( role, wssPermissions ) ) {
                return Result.failure( "user doesn't have required permissions: '" + List.of( wssPermissions ) + "', user permissions: '" + allRoles.permissionsOf( role ) + "'" );
            }
        }

        userStorage.update( userData.getEmail(), ud -> {
            ud.lastAccess = DateTime.now( UTC );

            return ud;
        } );

        return Result.success( new UserWithCookies( userData, responseAccessCookie.map( c -> c.accessToken ), responseAccessCookie.map( c -> c.refreshToken ) ) );
    }


    private boolean hasRealmMismatchError( String organization, boolean useOrganizationLogin, String realmString ) {
        boolean organizationNotEmpty = !StringUtils.isEmpty( organization );
        boolean realmNotEqualOrganization = !realmString.equals( organization );
        boolean realmNotEqualSystem = !WsSecurity.SYSTEM_REALMS.contains( realmString );
        boolean organizationNotEqualSystem = organization != null && !WsSecurity.SYSTEM_REALMS.contains( organization );

        return organizationNotEmpty && useOrganizationLogin
            && realmNotEqualOrganization
            && realmNotEqualSystem
            && organizationNotEqualSystem;
    }

    @Override
    public Result<? extends User, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> tfaCode ) {
        Optional<UserData> authenticated = userStorage.get( email )
            .filter( u -> u.authenticate( password ) );

        return getAuthenticationResult( email, tfaCode, authenticated );
    }

    @Override
    public Result<? extends User, AuthenticationFailure>
    getAuthenticated( String email, Optional<String> tfaCode ) {
        Optional<UserData> authenticated = userStorage.get( email );
        return getAuthenticationResult( email, tfaCode, authenticated );
    }

    private Result<? extends User, AuthenticationFailure> getAuthenticationResult( String email, Optional<String> tfaCode, Optional<UserData> authenticated ) {
        if( authenticated.isPresent() ) {
            UserData userData = authenticated.get();
            if( !userData.user.tfaEnabled ) {
                userStorage.update( email, user -> {
                    user.lastLogin = DateTime.now( UTC );
                    return user;
                } );
                return Result.success( userData );
            } else {
                if( tfaCode.isEmpty() ) {
                    return Result.failure( TFA_REQUIRED );
                }
                boolean tfaCheck = tfaCode.map( code -> getTOTPCode( userData.user.getSecretKey() ).equals( code ) )
                    .orElse( false );
                return tfaCheck ? Result.success( userData ) : Result.failure( WRONG_TFA_CODE );
            }
        }
        return Result.failure( UNAUTHENTICATED );
    }

    @Override
    public Optional<? extends User> getAuthenticatedByApiKey( String accessKey, String apiKey ) {
        return userStorage.select().filter( u -> u.authenticate( accessKey, apiKey ) ).findAny();
    }
}
