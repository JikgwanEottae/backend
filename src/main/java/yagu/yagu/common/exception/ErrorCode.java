package yagu.yagu.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // === 클라이언트 요청 오류 ===
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    INVALID_MONTH(HttpStatus.BAD_REQUEST, "월은 1~12 사이여야 합니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다."),

    // === 사용자 관련 오류 ===
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // === 일기 관련 오류 ===
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "일기를 찾을 수 없습니다."),
    DIARY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 일기에 접근할 권한이 없습니다."),

    // === 경기 관련 오류 ===
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "경기를 찾을 수 없습니다."),
    GAME_DATE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 경기 날짜입니다."),

    // === 파일/이미지 관련 오류 ===
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 처리에 실패했습니다."),

    // === 외부 API 관련 오류 ===
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다."),
    TOURISM_API_ERROR(HttpStatus.BAD_GATEWAY, "관광 정보 조회에 실패했습니다."),

    // === 비즈니스 로직 오류 ===
    OPERATION_DENIED(HttpStatus.FORBIDDEN, "요청을 수행할 권한이 없습니다."),

    // === 서버 오류 ===
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러가 발생했습니다."),

    // ErrorCode.java (추가)
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다. 다시 로그인해주세요."),
    USER_DELETED_FORBIDDEN(HttpStatus.UNAUTHORIZED, "탈퇴한 계정입니다. 다시 로그인할 수 없습니다.");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
