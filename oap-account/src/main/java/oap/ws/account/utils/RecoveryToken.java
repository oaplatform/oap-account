/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account.utils;

import lombok.ToString;

import java.io.Serializable;

@ToString
public class RecoveryToken implements Serializable {
    public String token;
    public String email;
    public long created = System.currentTimeMillis();
    public long expiresAt;

    public RecoveryToken( String token, String email, long ttlMillis ) {
        this.token = token;
        this.email = email;
        this.expiresAt = this.created + ttlMillis;
    }
}
