package oap.ws.account.testing;

import oap.storage.mongo.MongoFixture;
import oap.testng.TestDirectoryFixture;
import oap.ws.account.utils.RecoveryTokenService;

public class AccountFixture extends AbstractAccountFixture<AccountFixture> {
    public AccountFixture( TestDirectoryFixture testDirectoryFixture, MongoFixture mongoFixture ) {
        super( testDirectoryFixture, mongoFixture );
    }

    RecoveryTokenService recoveryTokenService() {
        return service( "oap-account", RecoveryTokenService.class );
    }
}
