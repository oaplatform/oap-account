/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.utils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTokenService implements RecoveryTokenService {
    private final Map<String, RecoveryToken> tokens = new ConcurrentHashMap<>();

    @Override
    public void store( String token, String email, long ttlMillis ) {
        tokens.put( token, new RecoveryToken( token, email, ttlMillis ) );
    }

    @Override
    public Optional<String> getEmailByToken( String token ) {
        RecoveryToken rt = tokens.get( token );
        if( rt == null || System.currentTimeMillis() > rt.expiresAt ) return Optional.empty();
        return Optional.of( rt.email );
    }

    @Override
    public void invalidate( String token ) {
        tokens.remove( token );
    }
}
