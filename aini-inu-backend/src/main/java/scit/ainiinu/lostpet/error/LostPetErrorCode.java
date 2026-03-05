package scit.ainiinu.lostpet.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum LostPetErrorCode implements ErrorCode {
    L001_IMAGE_REQUIRED("L001", HttpStatus.BAD_REQUEST, "분석 이미지가 필요합니다."),
    L002_INVALID_IMAGE("L002", HttpStatus.BAD_REQUEST, "분석 이미지가 유효하지 않습니다."),
    L400_SEARCH_REQUEST_INVALID("L400_SEARCH_REQUEST_INVALID", HttpStatus.BAD_REQUEST, "후보 탐색 요청이 유효하지 않습니다."),
    L404_NOT_FOUND("L404", HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    L404_SEARCH_SESSION_NOT_FOUND("L404_SEARCH_SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "탐색 세션을 찾을 수 없습니다."),
    L403_FORBIDDEN("L403", HttpStatus.FORBIDDEN, "권한이 없습니다."),
    L409_DUPLICATE_ACTIVE_REPORT("L409_DUPLICATE_ACTIVE_REPORT", HttpStatus.CONFLICT, "미해결 중복 실종 신고가 존재합니다."),
    L409_MATCH_CONFLICT("L409_MATCH_CONFLICT", HttpStatus.CONFLICT, "현재 매치 상태에서는 승인할 수 없습니다."),
    L409_SEARCH_CANDIDATE_INVALID("L409_SEARCH_CANDIDATE_INVALID", HttpStatus.CONFLICT, "탐색 세션에 없는 후보입니다."),
    L410_REPORT_RESOLVED("L410_REPORT_RESOLVED", HttpStatus.GONE, "해결된 실종 신고입니다."),
    L410_SEARCH_SESSION_EXPIRED("L410_SEARCH_SESSION_EXPIRED", HttpStatus.GONE, "탐색 세션이 만료되었습니다."),
    L500_AI_ANALYZE_FAILED("L500_AI_ANALYZE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석에 실패했습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
