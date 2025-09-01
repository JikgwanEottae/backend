package yagu.yagu.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yagu.yagu.common.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        ApiResponse<Object> body = ApiResponse.builder()
                .result(false)
                .httpCode(code.getStatus().value())
                .data(null)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    /**
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);
        String msg = fieldError.getField() + ": " + fieldError.getDefaultMessage();

        ApiResponse<Object> body = ApiResponse.builder()
                .result(false)
                .httpCode(HttpStatus.BAD_REQUEST.value())
                .data(null)
                .message(msg)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * @RequestParam, @PathVariable 검증 실패 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().iterator().next().getMessage();
        ApiResponse<Object> body = ApiResponse.builder()
                .result(false)
                .httpCode(HttpStatus.BAD_REQUEST.value())
                .data(null)
                .message(msg)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 지원하지 않는 미디어 타입 요청 처리
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        ApiResponse<Object> body = ApiResponse.builder()
                .result(false)
                .httpCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .data(null)
                .message("지원하지 않는 Content-Type 입니다.")
                .build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    /**
     * 그 외 모든 예외 처리 (Swagger 관련 경로 제외)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAll(Exception ex,
            jakarta.servlet.http.HttpServletRequest request) {
        // Swagger 관련 경로는 예외 처리에서 제외
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/swagger-resources")) {
            throw new RuntimeException(ex); // 원본 예외를 다시 던져서 Spring이 처리하도록 함
        }

        ApiResponse<Object> body = ApiResponse.builder()
                .result(false)
                .httpCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .data(null)
                .message(ErrorCode.INTERNAL_ERROR.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
