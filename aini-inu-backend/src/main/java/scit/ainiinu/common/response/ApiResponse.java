package scit.ainiinu.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@AllArgsConstructor
@Schema(description = "공통 API 응답 래퍼")
public class ApiResponse<T> {
    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;
    @Schema(description = "HTTP 상태 코드", example = "200")
    private int status;
    @Schema(description = "실제 응답 데이터")
    private T data;
    @Schema(description = "에러 코드 (성공 시 null)", example = "C101")
    private String errorCode;
    @Schema(description = "에러 메시지 (성공 시 null)", example = "인증이 필요합니다")
    private String message;

    // Factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, data, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                errorCode.getHttpStatus().value(),
                null,
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
    
    // For manual error construction (e.g. validation errors)
    public static <T> ApiResponse<T> error(int status, String code, String message, T data) {
        return new ApiResponse<>(false, status, data, code, message);
    }
}
