package com.example.swd392_se1907_aives.exceptions.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@RequiredArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED(999, "Uncategorized error"),

    // 404 errors
    USER_NOT_FOUND(404, "User not found"),

    // 403 errors
    INVALID_CREDENTIALS(403, "Invalid credentials"),
    USER_NOT_ACTIVE(403, "User is not active"),
    USER_ALREADY_ACTIVE(403, "User already active"),
    USER_ALREADY_INACTIVE(403, "User already InActive"),
    FORBIDDEN(403, "Access denied"),

    // 409 errors
    USER_EXISTS(409, "User already exists"),

    // 400 errors
    PASSWORD_CONFIRM_NOT_MATCH(400, "Confirm password not match"),
    NEW_PASSWORD_SAME_AS_OLD(400, "New password same as old"),
    INVALID_USERNAME_LENGTH(400, "Invalid username length"),
    INVALID_FULLNAME_LENGTH(400, "Invalid fullname length"),
    INVALID_EMAIL_FORMAT(400, "Invalid email format"),
    INVALID_PASSWORD_LENGTH(400, "Password must be 8-16 characters"),
    INVALID_OLD_PASSWORD(400, "Invalid old password");

    final int code;
    final String message;
}
