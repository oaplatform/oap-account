package oap.ws.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

    public final String id;
    public final String email;
    public final String firstName;
    public final String lastName;
    public final Map<String, List<String>> accounts;
    public final Map<String, String> roles;
    public final boolean banned;
    public final boolean confirmed;
    public final boolean tfaEnabled;
    public final Map<String, String> defaultAccounts;
    public final String defaultOrganization;
    @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd" )
    public final DateTime lastLogin;
    public final DateTime created;
    public final DateTime modified;
}
