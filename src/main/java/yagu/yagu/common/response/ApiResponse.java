package yagu.yagu.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    /** 처리 성공 여부 */
    private boolean result;

    /** HTTP 상태 코드 */
    private int httpCode;

    /** 실제 반환할 데이터; 실패 시 null */
    private T data;

    /** 응답 메시지 */
    private String message;

    // === 성공 응답 편의 메서드들 ===

    /**
     * 성공 응답 (200 OK)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .result(true)
                .httpCode(200)
                .data(data)
                .message("성공")
                .build();
    }

    /**
     * 성공 응답 with 커스텀 메시지 (200 OK)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .result(true)
                .httpCode(200)
                .data(data)
                .message(message)
                .build();
    }

    /**
     * 생성 성공 응답 (201 Created)
     */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .result(true)
                .httpCode(201)
                .data(data)
                .message("생성 완료")
                .build();
    }

    /**
     * 생성 성공 응답 with 커스텀 메시지 (201 Created)
     */
    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .result(true)
                .httpCode(201)
                .data(data)
                .message(message)
                .build();
    }

    /**
     * 처리 완료 응답 (204 No Content)
     */
    public static <T> ApiResponse<T> noContent() {
        return ApiResponse.<T>builder()
                .result(true)
                .httpCode(204)
                .data(null)
                .message("처리 완료")
                .build();
    }

    /**
     * 처리 완료 응답 with 커스텀 메시지 (204 No Content)
     */
    public static <T> ApiResponse<T> noContent(String message) {
        return ApiResponse.<T>builder()
                .result(true)
                .httpCode(204)
                .data(null)
                .message(message)
                .build();
    }
}
