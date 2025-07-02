package oap.ws.account;

import lombok.ToString;

import java.io.Serializable;

@ToString
public class RecoverPasswordRequest implements Serializable {
    public String email;
}
