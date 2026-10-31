package com.example.swd392_se1907_aives.shared.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Same temporary-failure response for database errors in filters and controllers. */
public final class DatabaseUnavailableResponse {
    public static final int RETRY_AFTER_SECONDS = 5;

    private DatabaseUnavailableResponse() {}

    public static ApiResponse<String> body() {
        return ApiResponse.<String>builder().code(HttpStatus.SERVICE_UNAVAILABLE.value())
            .message("Kết nối cơ sở dữ liệu đang bận hoặc bị gián đoạn. Vui lòng thử lại sau vài giây.").build();
    }

    public static ResponseEntity<ApiResponse<String>> response() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(HttpHeaders.RETRY_AFTER, Integer.toString(RETRY_AFTER_SECONDS)).body(body());
    }
}
