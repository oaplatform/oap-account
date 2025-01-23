/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import java.util.Optional;

public interface Accounts {
    OrganizationData storeOrganization( Organization organization );

    Optional<UserData> confirm( String email );

    Optional<UserData> refreshApikey( String email );

}
