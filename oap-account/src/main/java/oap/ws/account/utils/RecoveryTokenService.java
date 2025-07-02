/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.utils;

import java.util.Optional;

public interface RecoveryTokenService {
    void store( String token, String email, long ttlMillis );

    Optional<String> getEmailByToken( String token );

    void invalidate( String token );
}
