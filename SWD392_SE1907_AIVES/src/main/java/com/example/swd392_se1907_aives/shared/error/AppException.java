package com.example.swd392_se1907_aives.shared.error;



import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AppException extends RuntimeException {
    ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
