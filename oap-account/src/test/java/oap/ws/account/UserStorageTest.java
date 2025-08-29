package oap.ws.account;

import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserStorageTest {
    @Test
    public void testGetInfo() {
        UserStorage userStorage = new UserStorage( "1", "2", "3", "4", Map.of(), true );
        userStorage.store( new UserData( new User( "test@email", "fn", "ln" ) ) );

        assertThat( userStorage.getInfo( "test@email", "unknown" ) )
            .containsExactly(
                new UserService.UserInfo( "test@email", "fn", "ln" ),
                new UserService.UserInfo( "unknown", null, null ) );
    }
}
