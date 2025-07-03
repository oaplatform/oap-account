/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.ToString;

import java.io.Serializable;

@ToString
public class ResetPasswordRequest implements Serializable {
    public String token;
    public String newPassword;
}
