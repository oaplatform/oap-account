package oap.ws.account;

import oap.storage.Storage;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserStorageTest {
    @Test
    public void testGetInfo() {
        UserStorage userStorage = new UserStorage( 100, "1", "2", "3", "4", Map.of(), true );
        userStorage.store( new UserData( new User( null, "test@email", "fn", "ln" ) ), Storage.MODIFIED_BY_SYSTEM );

        assertThat( userStorage.getInfo( "test@email", "unknown" ) )
            .containsExactly(
                new UserService.UserInfo( "TSTML", "test@email", "fn", "ln" ),
                new UserService.UserInfo( "unknown", "unknown", null, null ) );
    }
}
