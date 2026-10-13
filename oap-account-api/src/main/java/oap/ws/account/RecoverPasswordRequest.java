package oap.ws.account;

import lombok.ToString;
import oap.ws.openapi.OpenApiDescription;

import java.io.Serializable;

@ToString
public class RecoverPasswordRequest implements Serializable {
    @OpenApiDescription( "Email of the user requesting a password recovery" )
    public String email;
}
