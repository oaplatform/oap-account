/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.account;

import oap.validation.ValidationMessage;

public enum AccountValidationMessage implements ValidationMessage {
    ORGANIZATION_ACCESS_DENIED( "OAP-ACCOUNT-100", "${email} cannot access organization ${organizationId}" ),
    ACCOUNT_ACCESS_DENIED( "OAP-ACCOUNT-101", "User (${email}) cannot access account ${accountId} of organization ${organizationId}" ),
    SECURITY_DISABLED_ONLY( "OAP-ACCOUNT-102", "this method is only allowed with disabled security" ),
    SYSTEM_ADMIN_REQUIRED( "OAP-ACCOUNT-103", "Only System ADMIN can access to this api" ),
    USER_ACCESS_DENIED( "OAP-ACCOUNT-104", "User [${email}] doesn't have enough permissions" ),
    TFA_CODE_REQUIRED( "OAP-ACCOUNT-105", "TFA code is required" ),
    TFA_CODE_INCORRECT( "OAP-ACCOUNT-106", "TFA code is incorrect" ),
    USERNAME_OR_PASSWORD_INVALID( "OAP-ACCOUNT-107", "Username or password is invalid" ),
    LOGIN_USER_NOT_FOUND( "OAP-ACCOUNT-108", "User not found" ),
    TOKEN_EMPTY( "OAP-ACCOUNT-109", "Token is empty" ),
    ORGANIZATION_MISMATCH( "OAP-ACCOUNT-110", "User doesn't belong to organization" ),
    TOKEN_INVALID( "OAP-ACCOUNT-111", "Token is invalid" ),
    USER_NOT_FOUND( "OAP-ACCOUNT-112", "not found ${idOrEmail}" ),
    CANNOT_MANAGE_USER( "OAP-ACCOUNT-113", "cannot manage ${email}" ),
    USER_NOT_IN_ORGANIZATION( "OAP-ACCOUNT-114", "User ${email} does not belong to organization ${organizationId}" ),
    APIKEY_CHANGE_FORBIDDEN( "OAP-ACCOUNT-115", "User ${loggedEmail} is not allowed to change apikey of another user ${idOrEmail}" ),
    REGISTRATION_NOT_AVAILABLE( "OAP-ACCOUNT-116", "not available" ),
    USER_ALREADY_EXISTS( "OAP-ACCOUNT-117", "user with email ${email} already exists" ),
    USER_DOES_NOT_EXIST( "OAP-ACCOUNT-118", "user ${email} does not exists" ),
    ADMIN_CREATION_DENIED( "OAP-ACCOUNT-119", "Only ADMIN can create another ADMIN" ),
    USER_ROLE_REQUIRED( "OAP-ACCOUNT-120", "User role is required" ),
    ADMIN_BAN_DENIED( "OAP-ACCOUNT-121", "ADMIN can be banned only by other ADMIN" ),
    ORGANIZATION_ADD_USER_NOT_ALLOWED( "OAP-ACCOUNT-122", "User is not allowed to add users to organization (${organizationId})" ),
    ONLY_ADMIN_ADD_USER( "OAP-ACCOUNT-123", "Only ADMIN can add user to organization" ),
    USER_NOT_EXIST( "OAP-ACCOUNT-124", "User (${idOrEmail}) doesn't exist" ),
    ORGANIZATION_NOT_EXIST( "OAP-ACCOUNT-125", "Organization (${organizationId}) does not exist" ),
    ORGANIZATION_ALREADY_DEFAULT( "OAP-ACCOUNT-126", "Organization (${organizationId}) is already marked as default" ),
    ACCOUNT_NOT_IN_ORGANIZATION( "OAP-ACCOUNT-127", "Account (${accountId}) does not exist in organization (${organizationId})" ),
    ACCOUNT_ALREADY_DEFAULT( "OAP-ACCOUNT-128", "Account (${accountId}) is already marked as default in organization (${organizationId})" ),
    ROLE_NOT_EXIST( "OAP-ACCOUNT-129", "Role (${role}) does not exist" ),
    OUTDATED_REFRESH_TOKEN( "OAP-ACCOUNT-130", "an outdated version of the refresh token" ),
    INVALID_TOKEN( "OAP-ACCOUNT-131", "Invalid token" ),
    REALM_MISMATCH( "OAP-ACCOUNT-132", "realm is different from organization logged in" ),
    JWT_TOKEN_EMPTY( "OAP-ACCOUNT-133", "JWT token is empty" ),
    USER_BANNED( "OAP-ACCOUNT-134", "User is banned" ),
    USER_NOT_CONFIRMED( "OAP-ACCOUNT-135", "User is not confirmed" ),
    OUTDATED_TOKEN( "OAP-ACCOUNT-136", "an outdated version of the token" ),
    REALM_ACCESS_DENIED( "OAP-ACCOUNT-137", "user doesn't have access to realm" ),
    PERMISSIONS_REQUIRED( "OAP-ACCOUNT-138", "user doesn't have required permissions" );

    private final String code;
    private final String message;

    AccountValidationMessage( String code, String message ) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
