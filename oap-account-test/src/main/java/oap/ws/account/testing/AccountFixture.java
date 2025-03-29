package oap.ws.account.testing;

import oap.storage.mongo.MongoFixture;
import oap.testng.TestDirectoryFixture;

public class AccountFixture extends AbstractAccountFixture<AccountFixture> {
    public AccountFixture( TestDirectoryFixture testDirectoryFixture, MongoFixture mongoFixture ) {
        super( testDirectoryFixture, mongoFixture );
    }
}
