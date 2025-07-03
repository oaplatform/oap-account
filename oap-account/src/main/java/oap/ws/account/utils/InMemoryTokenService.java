/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class InMemoryTokenService implements RecoveryTokenService {
    private final Cache<String, String> tokens;

    public InMemoryTokenService( long ttlMillis ) {
        this.tokens = CacheBuilder.newBuilder()
            .expireAfterWrite( ttlMillis, TimeUnit.MILLISECONDS )
            .build();
    }

    public InMemoryTokenService() {
        this.tokens = CacheBuilder.newBuilder()
            .expireAfterWrite( Duration.ofMinutes( 30 ).toMillis(), TimeUnit.MILLISECONDS )
            .build();
    }

    @Override
    public void store( String token, String email ) {
        tokens.put( token, email );
    }

    @Override
    public Optional<String> getEmailByToken( String token ) {
        return Optional.ofNullable( tokens.getIfPresent( token ) );
    }

    @Override
    public void invalidate( String token ) {
        tokens.invalidate( token );
    }
}
