package oap.ws.account;

import oap.storage.Metadata;
import org.joda.time.DateTime;

import static org.joda.time.DateTimeZone.UTC;

public final class Users {
    private Users() {
    }

    public static UserView userMetadataToView( Metadata<UserData> userDataMetadata ) {
        UserData userData = userDataMetadata.object;
        User user = userData.user;
        return new UserView( user.email, user.firstName, user.lastName, userData.accounts,
            userData.roles, userData.banned, user.confirmed, user.tfaEnabled, user.defaultAccounts,
            user.defaultOrganization, userData.lastLogin,
            new DateTime( userDataMetadata.created, UTC ), new DateTime( userDataMetadata.modified, UTC ) );
    }

    public static UserSecureView userMetadataToSecureView( Metadata<UserData> userDataMetadata ) {
        UserData userData = userDataMetadata.object;
        User user = userData.user;
        return new UserSecureView( user.email, user.firstName, user.lastName, userData.accounts,
            userData.roles, userData.banned, user.confirmed, user.tfaEnabled, user.defaultAccounts,
            user.defaultOrganization, userData.lastLogin,
            new DateTime( userDataMetadata.created, UTC ), new DateTime( userDataMetadata.modified, UTC ),
            user.apiKey, user.getAccessKey(), user.secretKey );
    }
}
