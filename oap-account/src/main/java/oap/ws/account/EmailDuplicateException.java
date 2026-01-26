package oap.ws.account;

public class EmailDuplicateException extends RuntimeException {
    public final String id;
    public final String email;

    public EmailDuplicateException( String id, String email ) {
        super( "Email duplicate: " + email + " id: " + id );
        this.id = id;
        this.email = email;
    }
}
