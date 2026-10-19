package com.example.swd392_se1907_aives.exceptions;
import com.example.swd392_se1907_aives.domain.dto.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<String>> missingResource(Exception e){return reply(HttpStatus.NOT_FOUND,"Không tìm thấy API hoặc tài nguyên. Khởi động lại backend nếu vừa cập nhật mã nguồn.");}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> conflictingData(Exception e){return reply(HttpStatus.CONFLICT,"Dữ liệu bị trùng hoặc đang được sử dụng. Không thể thực hiện thay đổi này.");}
    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.multipart.support.MissingServletRequestPartException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<String>> invalidParameters(Exception e){return reply(HttpStatus.BAD_REQUEST,"Dữ liệu yêu cầu không hợp lệ. Kiểm tra tệp, mã rubric và các trường bắt buộc.");}
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> uploadTooLarge(Exception e){return reply(HttpStatus.PAYLOAD_TOO_LARGE,"Tệp vượt giới hạn 20 MB.");}
    private ResponseEntity<ApiResponse<String>> reply(HttpStatusCode code,String message){
        return ResponseEntity.status(code).body(ApiResponse.<String>builder().code(code.value()).message(message).build());
    }
    @ExceptionHandler(BadCredentialsException.class) public ResponseEntity<ApiResponse<String>> auth(BadCredentialsException e){return reply(HttpStatus.UNAUTHORIZED,"Invalid credentials");}
    @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<ApiResponse<String>> denied(AccessDeniedException e){return reply(HttpStatus.FORBIDDEN,"Tài khoản của bạn không có quyền thực hiện thao tác này.");}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ApiResponse<String>> status(ResponseStatusException e){return reply(e.getStatusCode(),e.getReason()==null?"Request failed":e.getReason());}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<ApiResponse<String>> validation(MethodArgumentNotValidException e){return reply(HttpStatus.BAD_REQUEST,e.getFieldError()==null?"Invalid request":e.getFieldError().getField()+": "+e.getFieldError().getDefaultMessage());}
    @ExceptionHandler(AppException.class) public ResponseEntity<ApiResponse<String>> app(AppException e){return reply(HttpStatus.BAD_REQUEST,e.getMessage());}
    @ExceptionHandler(Exception.class) public ResponseEntity<ApiResponse<String>> fallback(Exception e){log.error("Unhandled API exception",e);return reply(HttpStatus.INTERNAL_SERVER_ERROR,"Máy chủ gặp lỗi xử lý. Vui lòng thử lại hoặc kiểm tra log backend.");}
}
