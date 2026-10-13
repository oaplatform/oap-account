package oap.ws.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import oap.ws.openapi.OpenApiDescription;
import org.joda.time.DateTime;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrganizationView implements Serializable {
    @Serial
    private static final long serialVersionUID = 9049298204022935855L;

    @OpenApiDescription( "Id of the organization" )
    public final String id;
    @OpenApiDescription( "Name of the organization" )
    public final String name;
    @OpenApiDescription( "Description of the organization" )
    public final String description;
    @OpenApiDescription( "Accounts belonging to the organization" )
    public final List<Account> accounts;
    @OpenApiDescription( "Date the organization was created" )
    public final DateTime created;
    @OpenApiDescription( "Date the organization was last modified" )
    public final DateTime modified;
}
