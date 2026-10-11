/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.ws;

import oap.ws.account.AccountValidationMessage;
import oap.ws.account.UserData;
import oap.ws.sso.AbstractSecureWS;
import oap.ws.validate.ValidationErrors;

import javax.annotation.Nonnull;

import java.util.Map;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.ws.account.Roles.ORGANIZATION_ADMIN;
import static oap.ws.sso.WsSecurity.SYSTEM;
import static oap.ws.validate.ValidationErrors.empty;

public abstract class AbstractWS extends AbstractSecureWS {
    protected boolean securityDisabled = false;

    protected ValidationErrors validateOrganizationAccess( UserData loggedUser, String organizationId ) {
        return canAccessOrganization( loggedUser, organizationId )
            ? empty()
            : empty().statusCode( FORBIDDEN )
            .error( AccountValidationMessage.ORGANIZATION_ACCESS_DENIED, Map.of( "email", loggedUser.user.email, "organizationId", organizationId ), null )
            .endCode();
    }

    protected ValidationErrors validateAccountAccess( UserData loggedUser, String organizationId, String accountId ) {
        return canAccessAccount( loggedUser, organizationId, accountId )
            ? empty()
            : empty().statusCode( FORBIDDEN )
            .error( AccountValidationMessage.ACCOUNT_ACCESS_DENIED, Map.of( "email", loggedUser.user.email, "accountId", accountId, "organizationId", organizationId ), null )
            .endCode();
    }

    protected ValidationErrors validateSecurityDisabled() {
        return securityDisabled
            ? empty()
            : empty().statusCode( FORBIDDEN ).error( AccountValidationMessage.SECURITY_DISABLED_ONLY, null ).endCode();
    }

    protected boolean canAccessOrganization( UserData loggedUser, String organizationId ) {
        return isSystem( loggedUser )
            || loggedUser.canAccessOrganization( organizationId );
    }

    protected boolean isSystem( UserData loggedUser ) {
        return loggedUser.roles.containsKey( SYSTEM );
    }

    protected boolean canAccessAccount( UserData loggedUser, String organizationId, String accountId ) {
        return isSystem( loggedUser )
            || isOrganizationAdmin( loggedUser, organizationId )
            || loggedUser.canAccessAccount( organizationId, accountId );
    }

    protected boolean isOrganizationAdmin( UserData loggedUser, String organizationId ) {
        return ORGANIZATION_ADMIN.equals( loggedUser.roles.get( organizationId ) );
    }

    public ValidationErrors validateSystemAdminRole( @Nonnull UserData loggedUser ) {
        if( !isSystem( loggedUser ) ) {
            return empty().statusCode( FORBIDDEN ).error( AccountValidationMessage.SYSTEM_ADMIN_REQUIRED, null ).endCode();
        } else return empty();
    }
}
