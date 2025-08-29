package oap.ws.account;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public interface UserService {
    List<UserInfo> getInfo( String... emails );

    record UserInfo( String email, String firstName, String lastName ) implements Serializable {
        @Serial
        private static final long serialVersionUID = -8190806637390172209L;
    }
}
