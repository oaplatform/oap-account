package oap.ws.account.testing;

import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.storage.Storage;
import oap.storage.mongo.MongoFixture;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.ws.account.Organization;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

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
                    List<List<Object>> response = RowBinaryUtils.read( resp.content(), new String[] { "id", "name" }, new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } } );
                    assertThat( response.size() ).isEqualTo( 3 );
                    assertThat( response.get( 0 ) ).isEqualTo( List.of( "id", "name" ) );
                    assertThat( response.get( 1 ) ).isEqualTo( List.of( "DFLT", "Default" ) );
                    assertThat( response.get( 2 ) ).isEqualTo( List.of( "TST", "test" ) );
                } catch( IOException e ) {
                    throw new RuntimeException( e );
                }
            } );
    }
}
