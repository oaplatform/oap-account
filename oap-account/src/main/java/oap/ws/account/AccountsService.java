/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AccountsService implements Accounts {
    protected OrganizationStorage organizationStorage;
    protected UserStorage userStorage;

    public AccountsService( OrganizationStorage organizationStorage, UserStorage userStorage ) {
        this.organizationStorage = organizationStorage;
        this.userStorage = userStorage;
    }

    @Override
    public OrganizationData storeOrganization( Organization organization ) {
        return organizationStorage.update( organization.id,
            o -> o.update( organization ),
            () -> new OrganizationData( organization ) );
    }

    @Override
    public Optional<UserData> confirm( String email ) {
        log.debug( "confirming: {}", email );
        return userStorage.update( UserStorage.prepareEmail( email ), user -> user.confirm( true ) );
    }

    @Override
    public Optional<UserData> refreshApikey( String email ) {
        log.debug( "refresh apikey to user: {}", email );

        return userStorage.update( UserStorage.prepareEmail( email ), UserData::refreshApikey );
    }
}
