/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.ws;

import oap.http.Http;
import oap.ws.account.UserData;
import oap.ws.sso.AbstractSecureWS;
import oap.ws.validate.ValidationErrors;

import javax.annotation.Nonnull;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.ws.account.Roles.ORGANIZATION_ADMIN;
import static oap.ws.sso.WsSecurity.SYSTEM;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

public abstract class AbstractWS extends AbstractSecureWS {
    protected boolean securityDisabled = false;

    protected ValidationErrors validateOrganizationAccess( UserData loggedUser, String organizationId ) {
        return canAccessOrganization( loggedUser, organizationId )
            ? empty()
            : error( Http.StatusCode.FORBIDDEN, "%s cannot access organization %s", loggedUser.user.email, organizationId );
    }

    protected ValidationErrors validateAccountAccess( UserData loggedUser, String organizationId, String accountId ) {
        return canAccessAccount( loggedUser, organizationId, accountId )
            ? empty()
            : error( Http.StatusCode.FORBIDDEN, "User (%s) cannot access account %s of organization %s", loggedUser.user.email, accountId, organizationId );
    }

    protected ValidationErrors validateSecurityDisabled() {
        return securityDisabled
            ? empty()
            : error( Http.StatusCode.FORBIDDEN, "this method is only allowed with disabled security" );
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
            return error( FORBIDDEN, "Only System ADMIN can access to this api" );
        } else return empty();
    }
}
