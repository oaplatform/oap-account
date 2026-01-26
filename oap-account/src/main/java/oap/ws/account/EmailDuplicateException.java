package oap.ws.account;

public class EmailDuplicateException extends RuntimeException {
    public final String id;
    public final String email;

    public EmailDuplicateException( String id, String email ) {
        this.id = id;
        this.email = email;
    }
}
