package com.example.swd392_se1907_aives.exceptions;

import com.example.swd392_se1907_aives.domain.dto.response.ApiResponse;
import com.example.swd392_se1907_aives.exceptions.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Handle general exceptions here
    // More specific exception handlers can be added here
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<String>> handleAppException(AppException ex) {
        ApiResponse<String> response = new ApiResponse<>();
        ErrorCode errorCode = ex.getErrorCode();

        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());
        response.setData(null);

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    // Handle validation exceptions here
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ApiResponse<String> response = new ApiResponse<>();

        String enumString = ex.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.valueOf(enumString);

        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());
        response.setData(null);

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    // Handle AccessDeniedException (403 Forbidden)
    @ExceptionHandler(value = java.nio.file.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException ex) {
        ApiResponse<String> response = new ApiResponse<>();

        response.setCode(403);
        response.setMessage("Access denied. You don't have permission to access this resource.");
        response.setData(null);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    // Fallback for uncategorized errors
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(Exception ex) {
        ApiResponse<String> response = new ApiResponse<>();
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED;

        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());
        response.setData(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

}