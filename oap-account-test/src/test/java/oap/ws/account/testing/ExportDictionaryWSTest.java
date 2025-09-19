package oap.ws.account.testing;

import oap.logstream.formats.rowbinary.RowBinaryInputStream;
import oap.storage.Storage;
import oap.storage.mongo.MongoFixture;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.ws.account.Organization;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static oap.http.test.HttpAsserts.assertGet;
import static org.assertj.core.api.Assertions.assertThat;

public class ExportDictionaryWSTest extends Fixtures {
    protected final AccountFixture accountFixture;

    public ExportDictionaryWSTest() {
        fixture( new SystemTimerFixture( false ) );
        TestDirectoryFixture testDirectoryFixture = fixture( new TestDirectoryFixture() );
        MongoFixture mongoFixture = fixture( new MongoFixture() );
        accountFixture = fixture( new AccountFixture( testDirectoryFixture, mongoFixture ) );
    }

    @Test
    public void testGet() {
        accountFixture.organizationStorage().storeOrganization( new Organization( "test", "test" ), Storage.MODIFIED_BY_SYSTEM );

        assertGet( accountFixture.httpUrl( "/export/dictionary/organizations" ) )
            .isOk()
            .satisfies( resp -> {
                try {
                    RowBinaryInputStream rowBinaryInputStream = new RowBinaryInputStream( new ByteArrayInputStream( resp.content() ), true );
                    assertThat( rowBinaryInputStream.headers ).isEqualTo( new String[] { "id", "name" } );
                    assertThat( rowBinaryInputStream.readString() ).isEqualTo( "DFLT" );
                    assertThat( rowBinaryInputStream.readString() ).isEqualTo( "Default" );
                    assertThat( rowBinaryInputStream.readString() ).isEqualTo( "TST" );
                    assertThat( rowBinaryInputStream.readString() ).isEqualTo( "test" );
                } catch( IOException e ) {
                    throw new RuntimeException( e );
                }
            } );
    }
}
