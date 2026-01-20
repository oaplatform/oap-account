package oap.ws.account;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public interface UserService {
    List<UserInfo> getInfo( String... emails );

    @ToString
    @EqualsAndHashCode
    class UserInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = -8190806637390172209L;

        public final String id;
        public final String email;
        public final String firstName;
        public final String lastName;

        public UserInfo( String id, String email, String firstName, String lastName ) {
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
