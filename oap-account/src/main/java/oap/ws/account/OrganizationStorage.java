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

import java.util.Optional;

import static oap.storage.Storage.Lock.SERIALIZED;

@Slf4j
public class OrganizationStorage extends MemoryStorage<String, OrganizationData> implements OrganizationService {
    public final String defaultOrganizationId;
    public final String defaultOrganizationName;
    public final String defaultOrganizationDescription;
    public final boolean defaultOrganizationReadOnly;

    /**
     * @param defaultOrganizationId          default organization id
     * @param defaultOrganizationName        default organization name
     * @param defaultOrganizationDescription default organization description
     * @param defaultOrganizationReadOnly    if true, the storage modifies the default organization to the default values on startup
     */
    public OrganizationStorage( String defaultOrganizationId,
                                String defaultOrganizationName,
                                String defaultOrganizationDescription,
                                boolean defaultOrganizationReadOnly ) {
        super( Identifier.<OrganizationData>forId( o -> o.organization.id, ( o, id ) -> o.organization.id = id )
            .suggestion( o -> o.organization.name )
            .build(), SERIALIZED );

        this.defaultOrganizationId = defaultOrganizationId;
        this.defaultOrganizationName = defaultOrganizationName;
        this.defaultOrganizationDescription = defaultOrganizationDescription;
        this.defaultOrganizationReadOnly = defaultOrganizationReadOnly;
    }

    public void start() {
        log.info( "id {} name {} description {} ro {}",
            defaultOrganizationId, defaultOrganizationName, defaultOrganizationDescription, defaultOrganizationReadOnly );

        update( defaultOrganizationId, d -> {
            if( defaultOrganizationReadOnly ) {
                d.organization.name = defaultOrganizationName;
                d.organization.description = defaultOrganizationDescription;
            }
            return d;
        }, () -> {
            Organization defaultOrganization = new Organization( defaultOrganizationId, defaultOrganizationName, defaultOrganizationDescription );
            return new OrganizationData( defaultOrganization );
        } );
    }

    public void deleteAllPermanently() {
        for( OrganizationData organizationData : this ) {
            memory.removePermanently( organizationData.organization.id );
        }
    }

    public OrganizationData storeOrganization( Organization organization ) {
        return update( organization.id,
            o -> o.update( organization ),
            () -> new OrganizationData( organization ) );
    }

    public Optional<Metadata<OrganizationData>> storeAccount( String organizationId, Account account ) {
        log.debug( "storeAccount organizationId {} account {}", organizationId, account );

        update( organizationId, o -> {
            o.addOrUpdateAccount( account );
            return o;
        } );

        return getMetadata( organizationId );
    }
}
