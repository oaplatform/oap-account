package oap.ws.account;

import oap.storage.Metadata;

public final class Users {
    private Users() {
    }

    public static UserData.View userMetadataToView( Metadata<UserData> userDataMetadata ) {
        UserData userData = userDataMetadata.object;
        User user = userData.user;
        return new UserData.View( user.email, user.firstName, user.lastName, userData.accounts,
            userData.roles, userData.banned, user.confirmed, user.tfaEnabled, user.defaultAccounts,
            user.defaultOrganization, userData.lastLogin, user.ext );
    }

    public static UserData.SecureView userMetadataToSecureView( Metadata<UserData> userDataMetadata ) {
        UserData userData = userDataMetadata.object;
        User user = userData.user;
        return new UserData.SecureView( user.email, user.firstName, user.lastName, userData.accounts,
            userData.roles, userData.banned, user.confirmed, user.tfaEnabled, user.defaultAccounts,
            user.defaultOrganization, userData.lastLogin, user.ext,
            user.apiKey, user.getAccessKey(), user.secretKey );
    }
}
