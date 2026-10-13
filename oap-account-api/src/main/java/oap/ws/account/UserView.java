package oap.ws.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import oap.ws.openapi.OpenApiDescription;
import org.joda.time.DateTime;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class UserView implements oap.ws.sso.UserView, Serializable {
    @Serial
    private static final long serialVersionUID = -4649450901635794223L;

    @OpenApiDescription( "Id of the user" )
    public final String id;
    @OpenApiDescription( "Email of the user" )
    public final String email;
    @OpenApiDescription( "First name of the user" )
    public final String firstName;
    @OpenApiDescription( "Last name of the user" )
    public final String lastName;
    @OpenApiDescription( "Map of organization id to the list of account ids the user has access to" )
    public final Map<String, List<String>> accounts;
    @OpenApiDescription( "Map of organization id to the role of the user in that organization" )
    public final Map<String, String> roles;
    @OpenApiDescription( "Whether the user is banned" )
    public final boolean banned;
    @OpenApiDescription( "Whether the user has confirmed registration" )
    public final boolean confirmed;
    @OpenApiDescription( "Whether two-factor authentication is enabled for the user" )
    public final boolean tfaEnabled;
    @OpenApiDescription( "Map of organization id to the default account id in that organization" )
    public final Map<String, String> defaultAccounts;
    @OpenApiDescription( "Id of the organization selected as default for the user" )
    public final String defaultOrganization;
    @OpenApiDescription( "Date of the user's last login" )
    @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd" )
    public final DateTime lastLogin;
    @OpenApiDescription( "Date the user was created" )
    public final DateTime created;
    @OpenApiDescription( "Date the user was last modified" )
    public final DateTime modified;
}
