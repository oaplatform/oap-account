package oap.ws.account;

import java.util.List;

public interface UserService {
    List<UserInfo> getInfo( String... emails );

    record UserInfo( String email, String firstName, String lastName ) {
    }
}
