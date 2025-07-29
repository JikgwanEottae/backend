package yagu.yagu.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 클라이언트 요청 오류
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),

    // 비즈니스 로직 오류
    OPERATION_DENIED(HttpStatus.FORBIDDEN, "요청을 수행할 권한이 없습니다."),

    // 서버 오류
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
