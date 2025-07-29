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
}
