package oap.ws.account;

import lombok.Getter;
import org.joda.time.DateTime;

import java.io.Serial;
import java.util.List;
import java.util.Map;

@Getter
public class UserSecureView extends UserView {
    @Serial
    private static final long serialVersionUID = -6998816008822366323L;

    public final String apiKey;
    public final String accessKey;
    public final String secretKey;

    public UserSecureView( String id, String email, String firstName, String lastName, Map<String, List<String>> accounts,
                           Map<String, String> roles, boolean banned, boolean confirmed, boolean tfaEnabled,
                           Map<String, String> defaultAccounts, String defaultOrganization, DateTime lastLogin,
                           DateTime created, DateTime modified,
                           String apiKey, String accessKey, String secretKey ) {

        super( id, email, firstName, lastName, accounts, roles, banned, confirmed, tfaEnabled, defaultAccounts, defaultOrganization, lastLogin,
            created, modified );

        this.apiKey = apiKey;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}
