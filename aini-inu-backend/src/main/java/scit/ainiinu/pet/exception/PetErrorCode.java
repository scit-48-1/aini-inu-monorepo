package scit.ainiinu.pet.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PetErrorCode implements ErrorCode {
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "반려견을 찾을 수 없습니다"),
    PET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P002", "등록 가능한 반려견 수(10마리)를 초과했습니다"),
    INVALID_PET_INFO(HttpStatus.BAD_REQUEST, "P003", "잘못된 반려견 정보입니다"),
    BREED_NOT_FOUND(HttpStatus.BAD_REQUEST, "P004", "존재하지 않는 견종입니다"),
    PERSONALITY_NOT_FOUND(HttpStatus.BAD_REQUEST, "P005", "존재하지 않는 성향입니다"),
    NOT_YOUR_PET(HttpStatus.FORBIDDEN, "P006", "본인 소유의 반려견이 아닙니다"),
    INVALID_PET_NAME(HttpStatus.BAD_REQUEST, "P007", "반려견 이름이 유효하지 않습니다"),
    PET_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "P008", "반려견 이름은 10자를 초과할 수 없습니다"),
    INVALID_CERTIFICATION_NUMBER(HttpStatus.BAD_REQUEST, "P009", "동물등록번호 형식이 올바르지 않습니다"),
    CERTIFICATION_API_ERROR(HttpStatus.BAD_GATEWAY, "P010", "동물등록정보 조회에 실패했습니다"),
    CERTIFICATION_INFO_MISMATCH(HttpStatus.BAD_REQUEST, "P011", "등록된 견종 또는 소유자 정보가 일치하지 않습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
