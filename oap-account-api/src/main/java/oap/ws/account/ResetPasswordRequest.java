/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import lombok.ToString;
import oap.ws.openapi.OpenApiDescription;

import java.io.Serializable;

@ToString
public class ResetPasswordRequest implements Serializable {
    @OpenApiDescription( "Recovery token issued for the password reset" )
    public String token;
    @OpenApiDescription( "New password to set for the user" )
    public String newPassword;
}
