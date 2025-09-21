package oap.ws.account;

import oap.http.Http.StatusCode;
import oap.logstream.formats.rowbinary.RowBinaryOutputStream;
import oap.util.Throwables;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class ExportDictionaryWS {
    private final OrganizationStorage organizationStorage;

    public ExportDictionaryWS( OrganizationStorage organizationStorage ) {
        this.organizationStorage = organizationStorage;
    }

    @WsMethod( path = "/{dictionaryName}" )
    public Response getDictionary( @WsParam( from = WsParam.From.PATH ) String dictionaryName ) {
        switch( dictionaryName ) {
            case "organizations" -> {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    RowBinaryOutputStream rowBinaryOutputStream = new RowBinaryOutputStream( baos, List.of( "id", "name" ) );
                    organizationStorage
                        .select( false )
                        .sorted( Comparator.comparing( o -> o.organization.name ) )
                        .forEach( data -> {
                            try {
                                rowBinaryOutputStream.writeString( data.organization.id );
                                rowBinaryOutputStream.writeString( data.organization.name );
                            } catch( IOException e ) {
                                throw Throwables.propagate( e );
                            }
                        } );

                    return new Response( StatusCode.OK ).withBody( baos.toByteArray(), true );
                } catch( IOException e ) {
                    throw Throwables.propagate( e );
                }
            }
            default -> {
                return Response.notFound();
            }
        }
    }
}
