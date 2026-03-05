package scit.ainiinu.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.member.exception.MemberException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle all BusinessException (from all domains)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Business Exception: code={}, message={}",
                errorCode.getCode(), errorCode.getMessage());

        ApiResponse<Object> response = ApiResponse.error(errorCode);

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(response);
    }

    /**
     * Handle MemberException
     */
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ApiResponse<Object>> handleMemberException(MemberException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Member Exception: code={}, message={}",
            errorCode.getCode(), errorCode.getMessage());

        ApiResponse<Object> response = ApiResponse.error(errorCode);

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(response);
    }

    /**
     * Handle validation errors (@Valid failed)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation Exception: {}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST.value(),
            CommonErrorCode.VALIDATION_FAILED.getCode(),
            CommonErrorCode.VALIDATION_FAILED.getMessage(),
            errors
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    /**
     * Handle unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unexpected Exception", e);

        ApiResponse<Object> response = ApiResponse.error(
            CommonErrorCode.INTERNAL_SERVER_ERROR
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
