/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.extern.slf4j.Slf4j;
import oap.id.Identifier;
import oap.storage.MemoryStorage;
import oap.storage.Metadata;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

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

    public static String prepareEmail( String email ) {
        return StringUtils.lowerCase( email );
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

    public Optional<Metadata<UserData>> addAccountToUser( String email, String organizationId, String accountId ) {
        log.debug( "add account: {} to user: {} in organization: {}", accountId, email, organizationId );

        update( prepareEmail( email ), u -> u.addAccount( organizationId, accountId ) );

        return getMetadata( email );
    }

    public Optional<Metadata<UserData>> removeAccountFromUser( String email, String organizationId, String accountId ) {
        log.debug( "remove account: {} from user: {} in organization: {}", accountId, email, organizationId );

        update( prepareEmail( email ), u -> u.removeAccount( organizationId, accountId ) );

        return getMetadata( email );
    }

    public List<Metadata<UserData>> getUsers( String organizationId ) {
        return selectMetadata()
            .filter( u -> u.object.belongsToOrganization( organizationId ) )
            .toList();
    }

    public Metadata<UserData> createUser( User user, Map<String, String> roles ) {
        log.debug( "createUser user {} roles {}", user, roles );
        user.email = prepareEmail( user.email );
        if( get( user.email ).isPresent() )
            throw new IllegalArgumentException( "user: " + user.email + " is already registered" );
        user.password = User.encrypt( user.password );
        store( new UserData( user, roles ) );

        return getMetadata( user.email ).orElseThrow();
    }

    public Optional<Metadata<UserData>> updateUser( String email, Consumer<User> update ) {
        log.debug( "updateUser email {}", email );

        update( prepareEmail( email ), u -> {
            update.accept( u.user );
            return u;
        } );

        return getMetadata( email );
    }

    public Optional<Metadata<UserData>> addOrganizationToUser( String email, String organizationId, String role ) {
        update( prepareEmail( email ), u -> u.addOrganization( organizationId, role ) );

        return getMetadata( email );
    }

    public Optional<Metadata<UserData>> removeUserFromOrganization( String email, String organizationId ) {
        update( prepareEmail( email ), u -> u.removeOrganization( organizationId ) );

        return getMetadata( email );
    }

    public Optional<Metadata<UserData>> assignRole( String email, String organizationId, String role ) {
        log.debug( "assign role: {} to user: {} in organization: {}", role, email, organizationId );
        update( prepareEmail( email ), u -> u.assignRole( organizationId, role ) );

        return getMetadata( email );
    }

    public Optional<Metadata<UserData>> passwd( String email, String password ) {
        update( prepareEmail( email ), user -> user.encryptPassword( password ) );

        return getMetadata( email );
    }

    public Optional<Metadata<UserData>> ban( String email, boolean banStatus ) {
        log.debug( ( banStatus ? "ban" : "unban" ) + " user " + email );
        update( prepareEmail( email ), user -> user.ban( banStatus ) );

        return getMetadata( email );
    }

    public Optional<Metadata<UserData>> deleteUser( String email ) {
        delete( StringUtils.lowerCase( email ) );

        return getMetadata( email );
    }
}
