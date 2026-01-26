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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static oap.storage.Storage.Lock.SERIALIZED;

@Slf4j
public class UserStorage extends MemoryStorage<String, UserData> implements UserService {
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
    public UserStorage( int transactionLogSize,
                        String defaultSystemAdminEmail,
                        String defaultSystemAdminPassword,
                        String defaultSystemAdminFirstName,
                        String defaultSystemAdminLastName,
                        Map<String, String> defaultSystemAdminRoles,
                        boolean defaultSystemAdminReadOnly ) {
        super( Identifier.<UserData>forId( u -> u.user.id, ( o, id ) -> o.user.id = id )
            .suggestion( u -> u.user.email )
            .options( Identifier.Option.COMPACT )
            .build(), SERIALIZED, transactionLogSize );

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
            User user = new oap.ws.account.User( null, defaultSystemAdminEmail, defaultSystemAdminFirstName, defaultSystemAdminLastName, defaultSystemAdminPassword, true );
            user.encryptPassword( defaultSystemAdminPassword );

            user.defaultOrganization = defaultSystemAdminRoles.keySet().stream().findAny().get();
            return new UserData( user, defaultSystemAdminRoles );
        }, MODIFIED_BY_SYSTEM );
    }

    public Optional<Metadata<UserData>> addAccountToUser( String idOrEmail, String organizationId, String accountId, String changedBy ) {
        log.debug( "add account: {} to user: {} in organization: {}", accountId, idOrEmail, organizationId );

        update( idOrEmail, u -> u.addAccount( organizationId, accountId ), changedBy );

        return getMetadata( idOrEmail );
    }

    public Optional<Metadata<UserData>> removeAccountFromUser( String idOrEmail, String organizationId, String accountId, String changedBy ) {
        log.debug( "remove account: {} from user: {} in organization: {}", accountId, idOrEmail, organizationId );

        update( idOrEmail, u -> u.removeAccount( organizationId, accountId ), changedBy );

        return getMetadata( idOrEmail );
    }

    public List<Metadata<UserData>> getUsers( String organizationId ) {
        return selectMetadata()
            .filter( u -> u.object.belongsToOrganization( organizationId ) )
            .toList();
    }

    public Metadata<UserData> createUser( User user, Map<String, String> roles, String changedBy ) {
        log.debug( "createUser user {} roles {}", user, roles );
        user.email = StringUtils.toRootLowerCase( user.email );

        Metadata<UserData> metadata = getMetadataNullable( user.email );

        if( metadata != null && metadata.object.getEmail().equals( user.email ) ) {
            throw new IllegalArgumentException( "user: " + user.email + " is already registered" );
        }

        user.password = User.encrypt( user.password );
        store( new UserData( user, roles ), changedBy );

        return getMetadata( user.id ).orElseThrow();
    }

    public Optional<Metadata<UserData>> updateUser( String idOrEmail, Consumer<User> update, String changedBy ) {
        log.debug( "updateUser id/email {}", idOrEmail );

        update( idOrEmail, u -> {
            update.accept( u.user );
            return u;
        }, changedBy );

        return getMetadata( idOrEmail );
    }

    public Optional<Metadata<UserData>> addOrganizationToUser( String idOrEmail, String organizationId, String role, String changedBy ) {
        update( idOrEmail, u -> u.addOrganization( organizationId, role ), changedBy );

        return getMetadata( idOrEmail );
    }

    public Optional<Metadata<UserData>> removeUserFromOrganization( String idOrEmail, String organizationId, String changedBy ) {
        update( idOrEmail, u -> u.removeOrganization( organizationId ), changedBy );

        return getMetadata( idOrEmail );
    }

    public Optional<Metadata<UserData>> assignRole( String idOrEmail, String organizationId, String role, String changedBy ) {
        log.debug( "assign role: {} to user: {} in organization: {}", role, idOrEmail, organizationId );
        update( idOrEmail, u -> u.assignRole( organizationId, role ), changedBy );

        return getMetadata( idOrEmail );
    }

    public Optional<Metadata<UserData>> passwd( String idOrEmail, String password, String changedBy ) {
        update( idOrEmail, user -> user.encryptPassword( password ), changedBy );

        return getMetadata( idOrEmail );
    }

    public Optional<Metadata<UserData>> ban( String idOrEmail, boolean banStatus, String changedBy ) {
        log.debug( ( banStatus ? "ban" : "unban" ) + " user " + idOrEmail );
        update( idOrEmail, user -> user.ban( banStatus ), changedBy );

        return getMetadata( idOrEmail );
    }

    public Optional<UserData> refreshApikey( String idOrEmail, String changedBy ) {
        log.debug( "refresh apikey to user: {}", idOrEmail );

        return update( idOrEmail, UserData::refreshApikey, changedBy );
    }

    public Optional<UserData> confirm( String idOrEmail, String changedBy ) {
        log.debug( "confirming: {}", idOrEmail );
        return update( idOrEmail, user -> user.confirm( true ), changedBy );
    }

    @Override
    public Optional<Metadata<UserData>> getMetadata( @Nonnull String idOrEmail ) {
        return Optional.ofNullable( getMetadataNullable( idOrEmail ) );
    }

    @Override
    public List<UserInfo> getInfo( String... idOrEmails ) {
        ArrayList<UserInfo> list = new ArrayList<>();

        for( String idOrEmail : idOrEmails ) {
            get( idOrEmail )
                .ifPresentOrElse(
                    u -> {
                        list.add( new UserInfo( u.getId(), u.getEmail(), u.user.firstName, u.user.lastName ) );
                    },
                    () -> list.add( new UserInfo( idOrEmail, idOrEmail, null, null ) ) );
        }

        return list;
    }


    @Override
    public @Nullable Metadata<UserData> getMetadataNullable( @NonNull String idOrEmail ) {
        Metadata<UserData> metadataNullable = super.getMetadataNullable( idOrEmail );

        if( metadataNullable != null ) {
            return metadataNullable;
        }

        for( Metadata<UserData> userMetadata : listMetadata() ) {
            if( userMetadata.object.getEmail().equalsIgnoreCase( idOrEmail ) ) {
                return userMetadata;
            }
        }

        return null;
    }

    public Optional<UserData> get( @Nonnull String idOrEmail ) {
        return getMetadata( idOrEmail ).map( metadata -> metadata.object );
    }

    public Optional<UserData> update( @Nonnull String idOrEmail, @Nonnull Function<UserData, UserData> update, String modifiedBy ) {
        Metadata<UserData> metadataNullable = getMetadataNullable( idOrEmail );

        if( metadataNullable == null ) {
            return super.update( idOrEmail, update, modifiedBy );
        }

        return super.update( metadataNullable.object.getId(), update, modifiedBy );
    }

    public UserData update( String idOrEmail, @Nonnull Function<UserData, UserData> update, @Nonnull Supplier<UserData> init, String modifiedBy ) {
        Metadata<UserData> metadataNullable = getMetadataNullable( idOrEmail );

        if( metadataNullable == null ) {
            return super.update( idOrEmail, update, init, modifiedBy );
        }

        return super.update( metadataNullable.object.getId(), update, init, modifiedBy );
    }

    public Optional<Metadata<UserData>> deleteMetadata( @Nonnull String idOrEmail ) {
        Metadata<UserData> metadataNullable = getMetadataNullable( idOrEmail );

        if( metadataNullable == null ) {
            return super.deleteMetadata( idOrEmail );
        }

        return super.deleteMetadata( metadataNullable.object.getId() );
    }

    public Optional<UserData> delete( @Nonnull String idOrEmail ) {
        return deleteMetadata( idOrEmail ).map( m -> m.object );
    }

    @Override
    public UserData store( @Nonnull UserData userData, String modifiedBy ) throws EmailDuplicateException {
        if( userData.getId() == null ) {
            UserData existUserData = select()
                .filter( ud -> ud.getEmail().equalsIgnoreCase( userData.getEmail() ) )
                .findAny()
                .orElse( null );
            if( existUserData != null ) {
                throw new EmailDuplicateException( existUserData.getId(), userData.getEmail() );
            }
        }

        return super.store( userData, modifiedBy );
    }
}
