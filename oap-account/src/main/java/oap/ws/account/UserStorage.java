/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.storage.MemoryStorage;

import java.util.Map;

import static oap.storage.Storage.Lock.SERIALIZED;

@Slf4j
public class UserStorage extends MemoryStorage<String, UserData> {
    public final String defaultSystemAdminEmail;
    public final String defaultSystemAdminPassword;
    public final String defaultSystemAdminFirstName;
    public final String defaultSystemAdminLastName;
    public final Map<String, String> defaultSystemAdminRoles;
    public final boolean defaultSystemAdminReadOnly;

    /**
     * @param defaultSystemAdminEmail     default user email
     * @param defaultSystemAdminPassword  default user password
     * @param defaultSystemAdminFirstName default user first name
     * @param defaultSystemAdminLastName  default user last name
     * @param defaultSystemAdminRoles     default user roles map ( hocon/json format )
     * @param defaultSystemAdminReadOnly  if true, the storage modifies the default user to the default values on startup
     */
    public UserStorage( String defaultSystemAdminEmail,
                        String defaultSystemAdminPassword,
                        String defaultSystemAdminFirstName,
                        String defaultSystemAdminLastName,
                        Map<String, String> defaultSystemAdminRoles,
                        boolean defaultSystemAdminReadOnly ) {
        super( Identifier.<UserData>forId( u -> u.user.email, ( o, id ) -> o.user.email = id )
            .suggestion( u -> u.user.email )
            .build(), SERIALIZED );

        this.defaultSystemAdminEmail = defaultSystemAdminEmail;
        this.defaultSystemAdminPassword = defaultSystemAdminPassword;
        this.defaultSystemAdminFirstName = defaultSystemAdminFirstName;
        this.defaultSystemAdminLastName = defaultSystemAdminLastName;
        this.defaultSystemAdminRoles = defaultSystemAdminRoles;
        this.defaultSystemAdminReadOnly = defaultSystemAdminReadOnly;
    }

    public void start() {
        log.info( "default email {} firstName {} lastName {} roles {} ro {}",
            defaultSystemAdminEmail, defaultSystemAdminFirstName, defaultSystemAdminLastName, defaultSystemAdminRoles, defaultSystemAdminReadOnly );

        update( defaultSystemAdminEmail, u -> {
            if( defaultSystemAdminReadOnly ) {
                u.user.email = defaultSystemAdminEmail;
                u.user.encryptPassword( defaultSystemAdminPassword );
                u.user.firstName = defaultSystemAdminFirstName;
                u.user.lastName = defaultSystemAdminLastName;
                u.user.confirmed = true;
                u.roles.clear();
                u.roles.putAll( defaultSystemAdminRoles );
                u.user.defaultOrganization = defaultSystemAdminRoles.keySet().stream().findAny().get();
            }

            return u;
        }, () -> {
            var user = new oap.ws.account.User( defaultSystemAdminEmail, defaultSystemAdminFirstName, defaultSystemAdminLastName, defaultSystemAdminPassword, true );
            user.encryptPassword( defaultSystemAdminPassword );

            user.defaultOrganization = defaultSystemAdminRoles.keySet().stream().findAny().get();
            return new UserData( user, defaultSystemAdminRoles );
        } );
    }

    public void deleteAllPermanently() {
        for( var user : this ) memory.removePermanently( user.user.email );
    }
}
