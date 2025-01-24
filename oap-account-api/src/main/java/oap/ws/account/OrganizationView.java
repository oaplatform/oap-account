package oap.ws.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.DateTime;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrganizationView implements Serializable {
    @Serial
    private static final long serialVersionUID = 9049298204022935855L;

    public final String id;
    public final String name;
    public final String description;
    public final List<Account> accounts;
    public final DateTime created;
    public final DateTime modified;
}
