package oap.ws.account;

import java.util.Optional;

public interface OrganizationService {
    Optional<OrganizationData> get( String id );
}
