package scit.ainiinu.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Configuration
public class OpenApiDocumentationCustomizer {

    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_WEBP = "image/webp";
    private static final String WILDCARD_MEDIA_TYPE = "*/*";
    private static final String DATE_EXAMPLE_UTC = "2026-03-05";
    private static final String DATE_TIME_EXAMPLE_UTC = "2026-03-05T01:20:00Z";

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "options", "head", "trace"
    );

    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "password", "token", "accessToken", "refreshToken"
    );

    private static final Map<String, String> SPECIFIC_FIELD_DESCRIPTIONS = Map.ofEntries(
            Map.entry("MemberProfilePatchRequest.nickname", "닉네임입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.profileImageUrl", "프로필 이미지 URL입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.linkedNickname", "연동 닉네임입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.phone", "연락처입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.age", "나이입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.gender", "성별 값입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.mbti", "MBTI 문자열입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.personality", "성격 소개 문구입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.selfIntroduction", "자기소개 문구입니다. null이면 변경하지 않습니다."),
            Map.entry("MemberProfilePatchRequest.personalityTypeIds", "회원 성향 타입 ID 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),

            Map.entry("PetUpdateRequest.name", "반려견 이름입니다. null이면 변경하지 않습니다."),
            Map.entry("PetUpdateRequest.birthDate", "반려견 생년월일입니다. null이면 변경하지 않습니다."),
            Map.entry("PetUpdateRequest.isNeutered", "중성화 여부입니다. null이면 변경하지 않습니다."),
            Map.entry("PetUpdateRequest.mbti", "반려견 MBTI입니다. null이면 변경하지 않습니다."),
            Map.entry("PetUpdateRequest.photoUrl", "반려견 프로필 이미지 URL입니다. null이면 변경하지 않습니다."),
            Map.entry("PetUpdateRequest.personalityIds", "성향 ID 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),
            Map.entry("PetUpdateRequest.walkingStyles", "산책 스타일 코드 목록(레거시 필드)입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),
            Map.entry("PetUpdateRequest.walkingStyleCodes", "산책 스타일 코드 목록(권장 필드)입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),

            Map.entry("ThreadPatchRequest.title", "산책 모집글 제목입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.description", "산책 모집글 설명입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.walkDate", "산책 날짜입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.startTime", "산책 시작 시각(UTC)입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.endTime", "산책 종료 시각(UTC)입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.chatType", "채팅 타입입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.maxParticipants", "최대 참여 인원입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.allowNonPetOwner", "비애견인 참여 허용 여부입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.isVisibleAlways", "상시 노출 여부입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.location", "장소 정보 객체입니다. null이면 변경하지 않습니다."),
            Map.entry("ThreadPatchRequest.petIds", "모집글에 연결할 반려견 ID 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),
            Map.entry("ThreadPatchRequest.filters", "모집 필터 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),

            Map.entry("WalkDiaryPatchRequest.threadId", "연결된 스레드 ID입니다. null이면 변경하지 않습니다."),
            Map.entry("WalkDiaryPatchRequest.title", "일기 제목입니다. null이면 변경하지 않습니다."),
            Map.entry("WalkDiaryPatchRequest.content", "일기 본문입니다. null이면 변경하지 않습니다."),
            Map.entry("WalkDiaryPatchRequest.photoUrls", "일기 사진 URL 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),
            Map.entry("WalkDiaryPatchRequest.walkDate", "산책 날짜입니다. null이면 변경하지 않습니다."),
            Map.entry("WalkDiaryPatchRequest.isPublic", "공개 여부입니다. null이면 변경하지 않습니다."),

            Map.entry("PostUpdateRequest.content", "게시글 본문입니다. null이면 변경하지 않습니다."),
            Map.entry("PostUpdateRequest.caption", "게시글 캡션입니다. null이면 변경하지 않습니다."),
            Map.entry("PostUpdateRequest.imageUrls", "게시글 이미지 URL 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다."),

            Map.entry("ThreadCreateRequest.LocationRequest.placeName", "장소명입니다."),
            Map.entry("ThreadCreateRequest.LocationRequest.latitude", "위도입니다."),
            Map.entry("ThreadCreateRequest.LocationRequest.longitude", "경도입니다."),
            Map.entry("ThreadCreateRequest.LocationRequest.address", "상세 주소입니다."),
            Map.entry("ThreadCreateRequest.ThreadFilterRequest.type", "필터 유형 코드입니다."),
            Map.entry("ThreadCreateRequest.ThreadFilterRequest.values", "필터 값 목록입니다."),
            Map.entry("ThreadCreateRequest.ThreadFilterRequest.isRequired", "필수 필터 여부입니다."),

            Map.entry("CursorResponse.nextCursor", "다음 조회를 위한 커서 값입니다."),
            Map.entry("SliceResponse.content", "현재 페이지의 데이터 목록입니다."),
            Map.entry("ApiResponse.success", "요청 성공 여부입니다."),
            Map.entry("ApiResponse.status", "HTTP 상태 코드입니다."),
            Map.entry("ApiResponse.data", "실제 응답 데이터입니다."),
            Map.entry("ApiResponse.errorCode", "에러 코드입니다. 성공 시 null입니다."),
            Map.entry("ApiResponse.message", "에러 메시지입니다. 성공 시 null입니다.")
    );

    private static final Map<String, String> GENERIC_FIELD_DESCRIPTIONS = Map.ofEntries(
            Map.entry("id", "리소스 식별자입니다."),
            Map.entry("memberId", "회원 ID입니다."),
            Map.entry("authorId", "작성자 회원 ID입니다."),
            Map.entry("ownerId", "실종 신고 등록자 회원 ID입니다."),
            Map.entry("finderId", "목격 신고자 회원 ID입니다."),
            Map.entry("reviewerId", "리뷰 작성자 회원 ID입니다."),
            Map.entry("targetId", "대상 회원 ID입니다."),
            Map.entry("revieweeId", "리뷰 대상 회원 ID입니다."),
            Map.entry("chatRoomId", "채팅방 ID입니다."),
            Map.entry("roomId", "채팅방 ID입니다."),
            Map.entry("threadId", "산책 모집글 ID입니다."),
            Map.entry("diaryId", "산책일기 ID입니다."),
            Map.entry("postId", "게시글 ID입니다."),
            Map.entry("commentId", "댓글 ID입니다."),
            Map.entry("petId", "반려견 ID입니다."),
            Map.entry("breedId", "견종 ID입니다."),
            Map.entry("lostPetId", "실종 신고 ID입니다."),
            Map.entry("sightingId", "목격 신고 ID입니다."),
            Map.entry("matchId", "실종-목격 매칭 ID입니다."),
            Map.entry("sessionId", "분석 세션 ID입니다."),
            Map.entry("candidateId", "후보 ID입니다."),
            Map.entry("q", "검색 키워드입니다."),
            Map.entry("email", "이메일 주소입니다."),
            Map.entry("password", "비밀번호입니다."),
            Map.entry("nickname", "닉네임입니다."),
            Map.entry("profileImageUrl", "프로필 이미지 URL입니다."),
            Map.entry("phone", "전화번호입니다."),
            Map.entry("age", "나이입니다."),
            Map.entry("gender", "성별 코드입니다."),
            Map.entry("mbti", "MBTI 문자열입니다."),
            Map.entry("personality", "성격 설명 문자열입니다."),
            Map.entry("selfIntroduction", "자기소개 문구입니다."),
            Map.entry("linkedNickname", "연동 계정 표시 닉네임입니다."),
            Map.entry("personalityTypeIds", "회원 성향 타입 ID 목록입니다."),
            Map.entry("personalityTypes", "회원 성향 타입 상세 목록입니다."),
            Map.entry("content", "본문 내용입니다."),
            Map.entry("title", "제목입니다."),
            Map.entry("description", "설명 문구입니다."),
            Map.entry("caption", "캡션 텍스트입니다."),
            Map.entry("message", "메시지 내용입니다."),
            Map.entry("summary", "AI 분석 요약입니다."),
            Map.entry("memo", "추가 메모입니다."),
            Map.entry("comment", "코멘트 내용입니다."),
            Map.entry("messageType", "메시지 타입 코드입니다."),
            Map.entry("action", "동작 액션 코드입니다."),
            Map.entry("status", "상태 코드입니다."),
            Map.entry("token", "토큰 문자열입니다."),
            Map.entry("refreshToken", "리프레시 토큰 문자열입니다."),
            Map.entry("accessToken", "액세스 토큰 문자열입니다."),
            Map.entry("tokenType", "토큰 타입 코드입니다."),
            Map.entry("photoUrl", "이미지 URL입니다."),
            Map.entry("uploadUrl", "업로드용 사전서명 URL입니다."),
            Map.entry("imageUrl", "이미지 접근 URL입니다."),
            Map.entry("image", "이미지 바이너리(Base64 등 인라인 전달)입니다."),
            Map.entry("queryText", "이미지 분석 보조 텍스트 질의입니다."),
            Map.entry("mode", "AI 분석 모드 코드입니다."),
            Map.entry("imageUrls", "이미지 URL 목록입니다."),
            Map.entry("photoUrls", "사진 URL 목록입니다."),
            Map.entry("fileName", "파일명입니다."),
            Map.entry("contentType", "파일 MIME 타입입니다."),
            Map.entry("purpose", "업로드 목적 코드입니다."),
            Map.entry("maxFileSizeBytes", "허용 최대 파일 크기(Byte)입니다."),
            Map.entry("expiresIn", "만료까지 남은 초(second)입니다."),
            Map.entry("walkDate", "산책 날짜입니다."),
            Map.entry("startTime", "시작 시각(UTC)입니다."),
            Map.entry("endTime", "종료 시각(UTC)입니다."),
            Map.entry("foundAt", "목격 시각(UTC)입니다."),
            Map.entry("createdAt", "생성 시각(UTC)입니다."),
            Map.entry("updatedAt", "수정 시각(UTC)입니다."),
            Map.entry("deletedAt", "삭제 시각(UTC)입니다."),
            Map.entry("sentAt", "메시지 전송 시각(UTC)입니다."),
            Map.entry("readAt", "읽음 처리 시각(UTC)입니다."),
            Map.entry("lastSeenAt", "마지막 목격 시각(UTC)입니다."),
            Map.entry("followedAt", "팔로우 생성 시각(UTC)입니다."),
            Map.entry("nicknameChangedAt", "닉네임 최근 변경 시각(UTC)입니다."),
            Map.entry("startDate", "통계 집계 시작 날짜입니다."),
            Map.entry("endDate", "통계 집계 종료 날짜입니다."),
            Map.entry("date", "통계 집계 날짜입니다."),
            Map.entry("birthDate", "생년월일입니다."),
            Map.entry("latitude", "위도입니다."),
            Map.entry("longitude", "경도입니다."),
            Map.entry("placeName", "장소명입니다."),
            Map.entry("address", "상세 주소입니다."),
            Map.entry("location", "산책 장소 정보 객체입니다."),
            Map.entry("lastSeenLocation", "마지막 목격 위치 설명입니다."),
            Map.entry("region", "핫스팟 지역명입니다."),
            Map.entry("maxParticipants", "최대 참여 인원입니다."),
            Map.entry("currentParticipants", "현재 참여 인원입니다."),
            Map.entry("allowNonPetOwner", "비애견인 참여 허용 여부입니다."),
            Map.entry("isVisibleAlways", "상시 노출 여부입니다."),
            Map.entry("isPublic", "공개 여부입니다."),
            Map.entry("isMain", "대표 반려견 여부입니다."),
            Map.entry("isNeutered", "중성화 여부입니다."),
            Map.entry("isLiked", "현재 로그인 사용자의 좋아요 여부입니다."),
            Map.entry("isFollowing", "현재 로그인 사용자의 팔로우 여부입니다."),
            Map.entry("isApplied", "현재 로그인 사용자의 신청 여부입니다."),
            Map.entry("isNewMember", "회원 최초 로그인 여부입니다."),
            Map.entry("isIdempotentReplay", "동일 멱등 키 재요청으로 기존 결과를 재사용했는지 여부입니다."),
            Map.entry("left", "채팅방 퇴장 여부입니다."),
            Map.entry("exists", "리소스 존재 여부입니다."),
            Map.entry("walkConfirmed", "산책 확정 완료 여부입니다."),
            Map.entry("allConfirmed", "참여자 전원 산책 확정 여부입니다."),
            Map.entry("isRequired", "필터 필수 여부입니다."),
            Map.entry("verified", "인증 여부입니다."),
            Map.entry("isVerified", "인증 여부입니다."),
            Map.entry("rating", "평점입니다."),
            Map.entry("score", "점수입니다."),
            Map.entry("scoreSimilarity", "이미지/특징 유사도 점수입니다."),
            Map.entry("scoreDistance", "거리 기반 유사도 점수입니다."),
            Map.entry("scoreRecency", "시간 최신성 점수입니다."),
            Map.entry("scoreTotal", "가중 합산 최종 점수입니다."),
            Map.entry("rank", "후보 우선순위(1부터 시작)입니다."),
            Map.entry("page", "페이지 번호(0부터 시작)입니다."),
            Map.entry("size", "페이지 크기입니다."),
            Map.entry("sort", "정렬 조건입니다."),
            Map.entry("cursor", "커서 값입니다."),
            Map.entry("nextCursor", "다음 조회용 커서입니다."),
            Map.entry("hasMore", "추가 데이터 존재 여부입니다."),
            Map.entry("hasNext", "다음 페이지 존재 여부입니다."),
            Map.entry("first", "첫 페이지 여부입니다."),
            Map.entry("last", "마지막 페이지 여부입니다."),
            Map.entry("errorCode", "에러 코드입니다."),
            Map.entry("code", "코드 문자열입니다."),
            Map.entry("name", "이름입니다."),
            Map.entry("count", "개수입니다."),
            Map.entry("petName", "반려견 이름입니다."),
            Map.entry("breed", "견종명 또는 견종 코드입니다."),
            Map.entry("totalActivities", "총 활동 횟수입니다."),
            Map.entry("likeCount", "좋아요 수입니다."),
            Map.entry("commentCount", "댓글 수입니다."),
            Map.entry("windowDays", "집계 윈도우 일수입니다."),
            Map.entry("timezone", "시간대 식별자입니다."),
            Map.entry("points", "일자별 집계 포인트 목록입니다."),
            Map.entry("author", "작성자 요약 정보입니다."),
            Map.entry("sender", "메시지 발신자 정보입니다."),
            Map.entry("review", "리뷰 상세 정보입니다."),
            Map.entry("comments", "댓글 목록입니다."),
            Map.entry("candidates", "분석/매칭 후보 목록입니다."),
            Map.entry("participants", "채팅방 참여자 목록입니다."),
            Map.entry("pets", "참여자 반려견 목록입니다."),
            Map.entry("lastMessage", "채팅방 마지막 메시지 정보입니다."),
            Map.entry("applicants", "신청자 회원 ID 목록입니다."),
            Map.entry("walkingStyles", "산책 스타일 코드 목록입니다."),
            Map.entry("personalities", "반려견 성향 태그 목록입니다."),
            Map.entry("petIds", "연결된 반려견 ID 목록입니다."),
            Map.entry("personalityIds", "반려견 성향 ID 목록입니다."),
            Map.entry("confirmedMemberIds", "산책 확정을 완료한 회원 ID 목록입니다."),
            Map.entry("lastReadMessageId", "마지막으로 읽은 메시지 ID입니다."),
            Map.entry("partnerId", "1:1 채팅 상대 회원 ID입니다."),
            Map.entry("messageId", "메시지 ID입니다."),
            Map.entry("myState", "요청자 본인의 산책 확정 상태입니다."),
            Map.entry("values", "필터 값 목록입니다."),
            Map.entry("memberType", "회원 유형 코드입니다."),
            Map.entry("chatType", "채팅 타입 코드입니다."),
            Map.entry("linkedThreadStatus", "연결 스레드 상태입니다."),
            Map.entry("applicationStatus", "신청 상태 코드입니다."),
            Map.entry("roomStatus", "채팅방 상태 코드입니다.")
    );

    private static final Map<String, String> PARAMETER_DESCRIPTION_OVERRIDES = Map.ofEntries(
            Map.entry("memberId", "회원 ID입니다."),
            Map.entry("postId", "게시글 ID입니다."),
            Map.entry("commentId", "댓글 ID입니다."),
            Map.entry("petId", "반려견 ID입니다."),
            Map.entry("threadId", "산책 모집글 ID입니다."),
            Map.entry("diaryId", "산책일기 ID입니다."),
            Map.entry("chatRoomId", "채팅방 ID입니다."),
            Map.entry("messageId", "메시지 ID입니다."),
            Map.entry("reviewId", "리뷰 ID입니다."),
            Map.entry("lostPetId", "실종 신고 ID입니다."),
            Map.entry("token", "토큰 문자열입니다."),
            Map.entry("q", "검색 키워드입니다."),
            Map.entry("page", "페이지 번호(0부터 시작)"),
            Map.entry("size", "페이지 크기"),
            Map.entry("sort", "정렬 조건"),
            Map.entry("cursor", "조회 커서 값"),
            Map.entry("sessionId", "분석 세션 ID"),
            Map.entry("hours", "핫스팟 집계 시간 범위(시간)")
    );

    private static final Map<String, String> ENUM_LIKE_FIELD_GUIDES = Map.ofEntries(
            Map.entry("ThreadCreateRequest.chatType", "INDIVIDUAL(1:1), GROUP(그룹 모집)"),
            Map.entry("ThreadPatchRequest.chatType", "INDIVIDUAL(1:1), GROUP(그룹 모집)"),
            Map.entry("ThreadSummaryResponse.chatType", "INDIVIDUAL(1:1), GROUP(그룹 모집)"),
            Map.entry("ThreadMapResponse.chatType", "INDIVIDUAL(1:1), GROUP(그룹 모집)"),
            Map.entry("ThreadResponse.chatType", "INDIVIDUAL(1:1), GROUP(그룹 모집)"),
            Map.entry("ChatRoomSummaryResponse.chatType", "DIRECT(1:1 채팅), GROUP(그룹 채팅)"),
            Map.entry("ChatRoomDetailResponse.chatType", "DIRECT(1:1 채팅), GROUP(그룹 채팅)"),
            Map.entry("ChatParticipantResponse.walkConfirmState", "UNCONFIRMED(미확정), CONFIRMED(확정)"),
            Map.entry("WalkConfirmResponse.myState", "UNCONFIRMED(미확정), CONFIRMED(확정)"),
            Map.entry("WalkConfirmRequest.action", "CONFIRM(산책 확정), CANCEL(산책 확정 취소)"),
            Map.entry("ChatMessageResponse.messageType", "USER(사용자 메시지), SYSTEM(시스템 메시지)"),
            Map.entry("ChatMessageCreateRequest.messageType", "USER(사용자 메시지), SYSTEM(시스템 메시지)"),
            Map.entry("ThreadApplyResponse.applicationStatus", "JOINED(신청 완료), CANCELED(신청 취소)"),
            Map.entry("ChatRoomSummaryResponse.status", "ACTIVE(활성), CLOSED(종료)"),
            Map.entry("ChatRoomDetailResponse.status", "ACTIVE(활성), CLOSED(종료)"),
            Map.entry("LeaveRoomResponse.roomStatus", "ACTIVE(활성), CLOSED(종료)"),
            Map.entry("ChatMessageResponse.status", "CREATED(생성 완료)"),
            Map.entry("ThreadSummaryResponse.status", "RECRUITING(모집중), EXPIRED(마감), DELETED(삭제)"),
            Map.entry("ThreadResponse.status", "RECRUITING(모집중), EXPIRED(마감), DELETED(삭제)"),
            Map.entry("WalkDiaryResponse.linkedThreadStatus", "RECRUITING(모집중), EXPIRED(마감), DELETED(삭제)"),
            Map.entry("LostPetResponse.status", "ACTIVE(진행중), RESOLVED(해결), CLOSED(종료)"),
            Map.entry("LostPetSummaryResponse.status", "ACTIVE(진행중), RESOLVED(해결), CLOSED(종료)"),
            Map.entry("LostPetDetailResponse.status", "ACTIVE(진행중), RESOLVED(해결), CLOSED(종료)"),
            Map.entry("SightingResponse.status", "OPEN(접수), CLOSED(종료)"),
            Map.entry("LostPetMatchResponse.status", "PENDING_APPROVAL(승인대기), APPROVED(승인), PENDING_CHAT_LINK(채팅방 연동대기), CHAT_LINKED(연동완료), REJECTED(거절), INVALIDATED(무효화)"),
            Map.entry("LostPetMatchCandidateResponse.status", "CANDIDATE(후보), APPROVED(승인)"),
            Map.entry("LostPetAnalyzeCandidateResponse.status", "CANDIDATE(후보), APPROVED(승인)"),
            Map.entry("MemberCreateRequest.memberType", "PET_OWNER(반려견 보호자), NON_PET_OWNER(비보호자), ADMIN(관리자)"),
            Map.entry("MemberSignupRequest.memberType", "PET_OWNER(반려견 보호자), NON_PET_OWNER(비보호자), ADMIN(관리자)"),
            Map.entry("MemberResponse.memberType", "PET_OWNER(반려견 보호자), NON_PET_OWNER(비보호자), ADMIN(관리자)"),
            Map.entry("MemberResponse.status", "ACTIVE(활성), INACTIVE(비활성), BANNED(정지)"),
            Map.entry("MemberCreateRequest.gender", "MALE(남성), FEMALE(여성), UNKNOWN(미선택)"),
            Map.entry("MemberProfilePatchRequest.gender", "MALE(남성), FEMALE(여성), UNKNOWN(미선택)"),
            Map.entry("MemberResponse.gender", "MALE(남성), FEMALE(여성), UNKNOWN(미선택)"),
            Map.entry("PetCreateRequest.gender", "MALE(수컷), FEMALE(암컷)"),
            Map.entry("PetUpdateRequest.gender", "MALE(수컷), FEMALE(암컷)"),
            Map.entry("PetResponse.gender", "MALE(수컷), FEMALE(암컷)"),
            Map.entry("PetCreateRequest.size", "SMALL(소형견), MEDIUM(중형견), LARGE(대형견)"),
            Map.entry("PetUpdateRequest.size", "SMALL(소형견), MEDIUM(중형견), LARGE(대형견)"),
            Map.entry("PetResponse.size", "SMALL(소형견), MEDIUM(중형견), LARGE(대형견)"),
            Map.entry("LoginResponse.tokenType", "Bearer(JWT Bearer 토큰)")
    );

    private static final Map<String, List<String>> ENUM_LIKE_FIELD_VALUES = Map.ofEntries(
            Map.entry("ThreadCreateRequest.chatType", List.of("INDIVIDUAL", "GROUP")),
            Map.entry("ThreadPatchRequest.chatType", List.of("INDIVIDUAL", "GROUP")),
            Map.entry("ThreadSummaryResponse.chatType", List.of("INDIVIDUAL", "GROUP")),
            Map.entry("ThreadMapResponse.chatType", List.of("INDIVIDUAL", "GROUP")),
            Map.entry("ThreadResponse.chatType", List.of("INDIVIDUAL", "GROUP")),
            Map.entry("ChatRoomSummaryResponse.chatType", List.of("DIRECT", "GROUP")),
            Map.entry("ChatRoomDetailResponse.chatType", List.of("DIRECT", "GROUP")),
            Map.entry("ChatParticipantResponse.walkConfirmState", List.of("UNCONFIRMED", "CONFIRMED")),
            Map.entry("WalkConfirmResponse.myState", List.of("UNCONFIRMED", "CONFIRMED")),
            Map.entry("WalkConfirmRequest.action", List.of("CONFIRM", "CANCEL")),
            Map.entry("ChatMessageResponse.messageType", List.of("USER", "SYSTEM")),
            Map.entry("ChatMessageCreateRequest.messageType", List.of("USER", "SYSTEM")),
            Map.entry("ThreadApplyResponse.applicationStatus", List.of("JOINED", "CANCELED")),
            Map.entry("ChatRoomSummaryResponse.status", List.of("ACTIVE", "CLOSED")),
            Map.entry("ChatRoomDetailResponse.status", List.of("ACTIVE", "CLOSED")),
            Map.entry("LeaveRoomResponse.roomStatus", List.of("ACTIVE", "CLOSED")),
            Map.entry("ChatMessageResponse.status", List.of("CREATED")),
            Map.entry("ThreadSummaryResponse.status", List.of("RECRUITING", "EXPIRED", "DELETED")),
            Map.entry("ThreadResponse.status", List.of("RECRUITING", "EXPIRED", "DELETED")),
            Map.entry("WalkDiaryResponse.linkedThreadStatus", List.of("RECRUITING", "EXPIRED", "DELETED")),
            Map.entry("LostPetResponse.status", List.of("ACTIVE", "RESOLVED", "CLOSED")),
            Map.entry("LostPetSummaryResponse.status", List.of("ACTIVE", "RESOLVED", "CLOSED")),
            Map.entry("LostPetDetailResponse.status", List.of("ACTIVE", "RESOLVED", "CLOSED")),
            Map.entry("SightingResponse.status", List.of("OPEN", "CLOSED")),
            Map.entry("LostPetMatchResponse.status", List.of("PENDING_APPROVAL", "APPROVED", "PENDING_CHAT_LINK", "CHAT_LINKED", "REJECTED", "INVALIDATED")),
            Map.entry("LostPetMatchCandidateResponse.status", List.of("CANDIDATE", "APPROVED")),
            Map.entry("LostPetAnalyzeCandidateResponse.status", List.of("CANDIDATE", "APPROVED")),
            Map.entry("MemberCreateRequest.memberType", List.of("PET_OWNER", "NON_PET_OWNER", "ADMIN")),
            Map.entry("MemberSignupRequest.memberType", List.of("PET_OWNER", "NON_PET_OWNER", "ADMIN")),
            Map.entry("MemberResponse.memberType", List.of("PET_OWNER", "NON_PET_OWNER", "ADMIN")),
            Map.entry("MemberCreateRequest.gender", List.of("MALE", "FEMALE", "UNKNOWN")),
            Map.entry("MemberProfilePatchRequest.gender", List.of("MALE", "FEMALE", "UNKNOWN")),
            Map.entry("MemberResponse.gender", List.of("MALE", "FEMALE", "UNKNOWN")),
            Map.entry("PetCreateRequest.gender", List.of("MALE", "FEMALE")),
            Map.entry("PetUpdateRequest.gender", List.of("MALE", "FEMALE")),
            Map.entry("PetResponse.gender", List.of("MALE", "FEMALE")),
            Map.entry("PetCreateRequest.size", List.of("SMALL", "MEDIUM", "LARGE")),
            Map.entry("PetUpdateRequest.size", List.of("SMALL", "MEDIUM", "LARGE")),
            Map.entry("PetResponse.size", List.of("SMALL", "MEDIUM", "LARGE")),
            Map.entry("LoginResponse.tokenType", List.of("Bearer"))
    );

    @Bean
    public OpenApiCustomizer openApiDocumentationEnhancer() {
        return this::enhance;
    }

    private void enhance(OpenAPI openApi) {
        if (openApi == null) {
            return;
        }
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }

        ensureApiErrorSchema(openApi.getComponents());
        normalizeSchemas(openApi.getComponents().getSchemas());
        normalizePaths(openApi, openApi.getPaths());
    }

    private void ensureApiErrorSchema(Components components) {
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            schemas = new LinkedHashMap<>();
            components.setSchemas(schemas);
        }

        if (schemas.containsKey("ApiResponseError")) {
            return;
        }

        ObjectSchema schema = new ObjectSchema();
        schema.setDescription("표준 에러 응답 래퍼");

        BooleanSchema success = new BooleanSchema();
        success.setDescription("요청 성공 여부");
        success.setExample(false);

        IntegerSchema status = new IntegerSchema();
        status.setDescription("HTTP 상태 코드");
        status.setExample(400);

        Schema<Object> data = new Schema<>();
        data.setNullable(true);
        data.setDescription("에러 부가 데이터(없으면 null)");
        data.setExample(null);

        StringSchema errorCode = new StringSchema();
        errorCode.setDescription("도메인/공통 에러 코드");
        errorCode.setExample("C002");

        StringSchema message = new StringSchema();
        message.setDescription("에러 메시지");
        message.setExample("입력값 검증에 실패했습니다");

        schema.addProperties("success", success);
        schema.addProperties("status", status);
        schema.addProperties("data", data);
        schema.addProperties("errorCode", errorCode);
        schema.addProperties("message", message);
        schema.setRequired(List.of("success", "status", "errorCode", "message"));

        schemas.put("ApiResponseError", schema);
    }

    private void normalizeSchemas(Map<String, Schema> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Schema> schemaEntry : schemas.entrySet()) {
            String schemaName = schemaEntry.getKey();
            Schema schema = schemaEntry.getValue();
            if (schema == null) {
                continue;
            }

            if (isBlank(schema.getDescription())) {
                schema.setDescription(resolveSchemaDescription(schemaName));
            }

            Map<String, Schema> properties = schema.getProperties();
            if (properties == null || properties.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
                String propertyName = propertyEntry.getKey();
                Schema property = propertyEntry.getValue();
                if (property == null) {
                    continue;
                }
                normalizeProperty(schemaName, propertyName, property);
            }
        }
    }

    private void normalizeProperty(String schemaName, String propertyName, Schema property) {
        String fieldKey = schemaName + "." + propertyName;

        String currentDescription = Objects.toString(property.getDescription(), "");
        if (shouldRefreshDescription(currentDescription, propertyName)) {
            String resolvedDescription = resolvePropertyDescription(schemaName, propertyName);
            if (isBlank(currentDescription) || !resolvedDescription.equals(propertyName + " 값입니다.")) {
                property.setDescription(resolvedDescription);
            }
        }

        if (schemaName.endsWith("PatchRequest") && !property.getDescription().contains("null이면")) {
            String patchNote = property instanceof ArraySchema
                    ? " null이면 변경하지 않고, [] 전달 시 목록을 비웁니다."
                    : " null이면 변경하지 않습니다.";
            property.setDescription(property.getDescription() + patchNote);
        }

        if (property.getEnum() != null && !property.getEnum().isEmpty()) {
            appendEnumGuide(schemaName, propertyName, property);
        }

        applyEnumLikeAllowableValues(schemaName, propertyName, property);
        appendEnumLikeGuide(schemaName, propertyName, property);

        if (!isSensitiveField(propertyName)) {
            Object example = property.getExample();
            if (example == null) {
                example = resolveExample(schemaName, propertyName, property);
            } else {
                example = coerceExample(schemaName, propertyName, property, example);
            }

            if (example != null) {
                property.setExample(example);
            }
        }

        if (property instanceof ArraySchema arraySchema) {
            Object normalized = coerceExample(schemaName, propertyName, arraySchema, arraySchema.getExample());
            if (normalized != null) {
                arraySchema.setExample(normalized);
            }

            if (arraySchema.getExample() == null) {
                Object example = resolveArrayExample(schemaName, fieldKey, propertyName, arraySchema);
                if (example != null) {
                    arraySchema.setExample(example);
                }
            }
        }
    }

    private void appendEnumGuide(String schemaName, String propertyName, Schema property) {
        String current = Objects.toString(property.getDescription(), "");
        if (current.contains("가능 값")) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object> rawValues = property.getEnum();
        if (rawValues == null || rawValues.isEmpty()) {
            return;
        }

        String key = schemaName + "." + propertyName;
        String explicitGuide = ENUM_LIKE_FIELD_GUIDES.get(key);
        String valueGuide = !isBlank(explicitGuide)
                ? explicitGuide
                : switch (key) {
            case "MemberResponse.memberType", "MemberSignupRequest.memberType" ->
                    "PET_OWNER(반려견 보호자), NON_PET_OWNER(비보호자), ADMIN(관리자)";
            case "MemberCreateRequest.gender", "MemberProfilePatchRequest.gender", "MemberResponse.gender" ->
                    "MALE(남성), FEMALE(여성), UNKNOWN(미선택)";
            default -> rawValues.stream().map(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("");
        };

        property.setDescription(current + " 가능 값: " + valueGuide + ".");
    }

    private void appendEnumLikeGuide(String schemaName, String propertyName, Schema property) {
        String current = Objects.toString(property.getDescription(), "");
        if (current.contains("가능 값")) {
            return;
        }

        String propertyType = resolveSchemaType(property);
        if (!"string".equals(propertyType)) {
            return;
        }

        if (!isEnumLikeField(propertyName)) {
            return;
        }

        String key = schemaName + "." + propertyName;
        String guide = ENUM_LIKE_FIELD_GUIDES.getOrDefault(
                key,
                "도메인에서 정의한 코드 문자열(값 의미는 API 도메인 규칙 참조)"
        );
        property.setDescription(current + " 가능 값: " + guide + ".");
    }

    private void applyEnumLikeAllowableValues(String schemaName, String propertyName, Schema property) {
        String propertyType = resolveSchemaType(property);
        if (!"string".equals(propertyType)) {
            return;
        }

        String key = schemaName + "." + propertyName;
        List<String> values = ENUM_LIKE_FIELD_VALUES.get(key);
        if (values == null || values.isEmpty()) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object> current = property.getEnum();
        if (current == null || current.isEmpty()) {
            property.setEnum(new ArrayList<>(values));
        }

        Object currentExample = property.getExample();
        if (currentExample == null || !values.contains(String.valueOf(currentExample))) {
            property.setExample(values.get(0));
        }
    }

    private boolean isEnumLikeField(String propertyName) {
        if (propertyName == null) {
            return false;
        }
        String lower = propertyName.toLowerCase(Locale.ROOT);
        return lower.endsWith("type")
                || lower.endsWith("status")
                || lower.endsWith("action")
                || lower.endsWith("state")
                || lower.endsWith("messagetype");
    }

    private Object coerceExample(String schemaName, String propertyName, Schema schema, Object candidate) {
        if (candidate == null) {
            return resolveExample(schemaName, propertyName, schema);
        }

        String type = resolveSchemaType(schema);
        String format = Objects.toString(schema.getFormat(), "");
        String lowerName = propertyName.toLowerCase(Locale.ROOT);

        if (candidate instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty() || "sample".equalsIgnoreCase(trimmed) || "예시값".equals(trimmed)) {
                return resolveExample(schemaName, propertyName, schema);
            }
        }

        try {
            return switch (type) {
                case "boolean" -> coerceBooleanExample(candidate, lowerName);
                case "integer" -> coerceIntegerExample(candidate);
                case "number" -> coerceNumberExample(candidate);
                case "array" -> (candidate instanceof List<?>) ? candidate : resolveArrayExample(
                        schemaName,
                        schemaName + "." + propertyName,
                        propertyName,
                        schema instanceof ArraySchema ? (ArraySchema) schema : new ArraySchema()
                );
                case "object" -> coerceObjectExample(propertyName, candidate);
                case "string" -> coerceStringExample(candidate, format, lowerName);
                default -> candidate;
            };
        } catch (RuntimeException ignored) {
            return resolveExample(schemaName, propertyName, schema);
        }
    }

    private String resolveSchemaType(Schema schema) {
        if (schema == null) {
            return "";
        }

        String type = Objects.toString(schema.getType(), "");
        if (type.isBlank()) {
            type = resolveTypeFromOpenApi31Types(schema);
        }
        if (!type.isBlank()) {
            return type;
        }

        if (schema instanceof IntegerSchema) {
            return "integer";
        }
        if (schema instanceof NumberSchema) {
            return "number";
        }
        if (schema instanceof BooleanSchema) {
            return "boolean";
        }
        if (schema instanceof StringSchema) {
            return "string";
        }
        if (schema instanceof ArraySchema || schema.getItems() != null) {
            return "array";
        }
        if (schema instanceof ObjectSchema || schema.getProperties() != null || schema.getAdditionalProperties() != null) {
            return "object";
        }
        if (!isBlank(schema.get$ref())) {
            return "object";
        }
        return "";
    }

    private String resolveTypeFromOpenApi31Types(Schema schema) {
        try {
            Object types = schema.getClass().getMethod("getTypes").invoke(schema);
            if (types instanceof Set<?> set && !set.isEmpty()) {
                Object first = set.iterator().next();
                return first == null ? "" : String.valueOf(first);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return "";
    }

    private Boolean coerceBooleanExample(Object candidate, String lowerName) {
        if (candidate instanceof Boolean value) {
            return value;
        }
        if (candidate instanceof Number value) {
            return value.intValue() != 0;
        }
        if (candidate instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return lowerName.startsWith("is")
                || lowerName.startsWith("has")
                || lowerName.contains("allow")
                || lowerName.contains("enabled")
                || lowerName.contains("active")
                || lowerName.contains("visible")
                || lowerName.contains("public")
                || lowerName.contains("exists")
                || lowerName.contains("verified");
    }

    private Number coerceIntegerExample(Object candidate) {
        if (candidate instanceof Integer || candidate instanceof Long) {
            return (Number) candidate;
        }
        if (candidate instanceof Number number) {
            return number.longValue();
        }
        if (candidate instanceof String text) {
            return Long.parseLong(text.trim());
        }
        return 1L;
    }

    private Number coerceNumberExample(Object candidate) {
        if (candidate instanceof Number) {
            return (Number) candidate;
        }
        if (candidate instanceof String text) {
            return Double.parseDouble(text.trim());
        }
        return 1.0;
    }

    private String coerceStringExample(Object candidate, String format, String lowerName) {
        if ("date-time".equals(format)) {
            return DATE_TIME_EXAMPLE_UTC;
        }
        if ("date".equals(format)) {
            return DATE_EXAMPLE_UTC;
        }
        if (candidate instanceof String text) {
            return text;
        }
        if (candidate instanceof Number || candidate instanceof Boolean) {
            return String.valueOf(candidate);
        }
        if (lowerName.contains("content-type")) {
            return IMAGE_JPEG;
        }
        if (lowerName.contains("url")) {
            return "https://cdn.example.com/sample.jpg";
        }
        return "예시 문자열";
    }

    private Map<String, Object> coerceObjectExample(String propertyName, Object candidate) {
        if (candidate instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            return normalized;
        }
        if (candidate instanceof List<?> list) {
            return new LinkedHashMap<>(Map.of("items", list));
        }

        String lowerName = propertyName.toLowerCase(Locale.ROOT);
        if (lowerName.contains("location")) {
            return new LinkedHashMap<>(Map.of(
                    "placeName", "한강공원",
                    "latitude", 37.566295,
                    "longitude", 126.977945
            ));
        }
        if (lowerName.contains("filter")) {
            return new LinkedHashMap<>(Map.of(
                    "type", "AGE_GROUP",
                    "values", List.of("20", "30"),
                    "isRequired", false
            ));
        }
        return new LinkedHashMap<>(Map.of("note", "객체 예시"));
    }

    private void normalizePaths(OpenAPI openApi, Paths paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }

        for (Map.Entry<String, PathItem> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();
            if (pathItem == null) {
                continue;
            }

            Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
            if (operations == null || operations.isEmpty()) {
                continue;
            }

            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : operations.entrySet()) {
                PathItem.HttpMethod httpMethod = operationEntry.getKey();
                Operation operation = operationEntry.getValue();
                if (operation == null) {
                    continue;
                }

                normalizeOperation(openApi, path, httpMethod, operation);
            }
        }
    }

    private void normalizeOperation(OpenAPI openApi, String path, PathItem.HttpMethod httpMethod, Operation operation) {
        String method = httpMethod.name().toLowerCase(Locale.ROOT);
        if (!HTTP_METHODS.contains(method)) {
            return;
        }

        if (isBlank(operation.getSummary())) {
            operation.setSummary(method.toUpperCase(Locale.ROOT) + " " + path);
        }

        boolean isPublic = isPublicOperation(operation);
        boolean hasRequestBody = operation.getRequestBody() != null;
        List<OpenApiEndpointContractRegistry.ErrorSpec> effectiveErrors = OpenApiEndpointContractRegistry.effectiveErrors(
                method,
                path,
                isPublic,
                hasRequestBody
        );

        String description = isBlank(operation.getDescription())
                ? "요청/응답 계약과 제약사항을 정의한 API입니다."
                : operation.getDescription();
        description = mergeErrorGuide(description, effectiveErrors);
        operation.setDescription(description);

        normalizeParameters(path, operation.getParameters());
        normalizeRequestBody(path, method, operation);
        normalizeResponses(path, method, operation, openApi.getComponents(), effectiveErrors);
    }

    private void normalizeParameters(String path, List<Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        for (Parameter parameter : parameters) {
            if (parameter == null) {
                continue;
            }

            if (isBlank(parameter.getDescription())) {
                parameter.setDescription(resolveParameterDescription(path, parameter.getName()));
            }

            Schema schema = parameter.getSchema();
            if (schema != null && schema.getExample() == null && !isSensitiveField(parameter.getName())) {
                Object example = resolveExample("Parameter", parameter.getName(), schema);
                if (example != null) {
                    schema.setExample(example);
                }
            }
        }
    }

    private void normalizeRequestBody(String path, String method, Operation operation) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return;
        }

        Content normalized = normalizeRequestContent(path, method, operation.getRequestBody().getContent());
        if (OpenApiEndpointContractRegistry.isBinaryRequest(method, path)) {
            ensureBinarySchema(normalized);
        }
        operation.getRequestBody().setContent(normalized);
    }

    private void normalizeResponses(
            String path,
            String method,
            Operation operation,
            Components components,
            List<OpenApiEndpointContractRegistry.ErrorSpec> effectiveErrors
    ) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        LinkedHashMap<String, OpenApiEndpointContractRegistry.ErrorSpec> expectedErrorByStatus = new LinkedHashMap<>();
        for (OpenApiEndpointContractRegistry.ErrorSpec error : effectiveErrors) {
            expectedErrorByStatus.put(String.valueOf(error.status()), error);
        }

        List<String> unexpectedStatusCodes = new ArrayList<>();
        responses.forEach((status, ignored) -> {
            if (isErrorStatus(status) && !expectedErrorByStatus.containsKey(status)) {
                unexpectedStatusCodes.add(status);
            }
        });
        for (String status : unexpectedStatusCodes) {
            responses.remove(status);
        }

        for (OpenApiEndpointContractRegistry.ErrorSpec error : effectiveErrors) {
            String status = String.valueOf(error.status());
            ensureErrorResponse(
                    responses,
                    status,
                    status + " 응답",
                    components,
                    error.code(),
                    error.message()
            );
        }

        responses.forEach((status, response) -> {
            if (response != null && response.getContent() != null) {
                response.setContent(normalizeResponseContent(path, method, status, response.getContent()));
                if (OpenApiEndpointContractRegistry.isBinaryResponse(method, path, status)) {
                    ensureBinarySchema(response.getContent());
                }
                ensureSuccessResponseExample(status, response);
            }
        });
    }

    private void ensureErrorResponse(
            ApiResponses responses,
            String status,
            String description,
            Components components,
            String errorCode,
            String message
    ) {
        ApiResponse response = responses.get(status);
        if (response == null) {
            response = new ApiResponse();
            responses.addApiResponse(status, response);
        }

        if (isBlank(response.getDescription())) {
            response.setDescription(description);
        }

        Content content = response.getContent();
        if (content == null || content.isEmpty()) {
            content = new Content();
        }

        MediaType mediaType = content.get(APPLICATION_JSON);
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType(APPLICATION_JSON, mediaType);
        }

        if (mediaType.getSchema() == null) {
            ensureApiErrorSchema(components);
            Schema<Object> schemaRef = new Schema<>();
            schemaRef.set$ref("#/components/schemas/ApiResponseError");
            mediaType.setSchema(schemaRef);
        }

        if (mediaType.getExample() == null) {
            Map<String, Object> errorExample = new LinkedHashMap<>();
            errorExample.put("success", false);
            errorExample.put("status", Integer.parseInt(status));
            errorExample.put("data", null);
            errorExample.put("errorCode", errorCode);
            errorExample.put("message", message);
            mediaType.setExample(errorExample);
        }

        response.setContent(normalizeJsonContent(content));
    }

    private Content normalizeRequestContent(String path, String method, Content source) {
        if (OpenApiEndpointContractRegistry.isBinaryRequest(method, path)) {
            return normalizeBinaryContent(source);
        }
        return normalizeJsonContent(source);
    }

    private Content normalizeResponseContent(String path, String method, String status, Content source) {
        if (OpenApiEndpointContractRegistry.isBinaryResponse(method, path, status)) {
            return normalizeBinaryContent(source);
        }
        return normalizeJsonContent(source);
    }

    private Content normalizeJsonContent(Content source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        Content normalized = new Content();

        MediaType json = source.get(APPLICATION_JSON);
        MediaType wildcard = source.get(WILDCARD_MEDIA_TYPE);

        if (json != null) {
            normalized.addMediaType(APPLICATION_JSON, json);
        } else if (wildcard != null) {
            normalized.addMediaType(APPLICATION_JSON, wildcard);
        }

        if (normalized.isEmpty() && wildcard != null) {
            normalized.addMediaType(APPLICATION_JSON, wildcard);
        }

        return normalized;
    }

    private Content normalizeBinaryContent(Content source) {
        Content normalized = new Content();
        if (source == null || source.isEmpty()) {
            MediaType mediaType = new MediaType();
            StringSchema schema = new StringSchema();
            schema.setFormat("binary");
            mediaType.setSchema(schema);
            normalized.addMediaType(APPLICATION_OCTET_STREAM, mediaType);
            return normalized;
        }

        MediaType wildcard = source.get(WILDCARD_MEDIA_TYPE);
        List<String> binaryTypes = List.of(APPLICATION_OCTET_STREAM, IMAGE_JPEG, IMAGE_PNG, IMAGE_WEBP);
        for (String binaryType : binaryTypes) {
            MediaType mediaType = source.get(binaryType);
            if (mediaType != null) {
                normalized.addMediaType(binaryType, mediaType);
            }
        }

        if (normalized.isEmpty()) {
            MediaType fallback = source.get(APPLICATION_OCTET_STREAM);
            if (fallback == null) {
                fallback = source.get(APPLICATION_JSON);
            }
            if (fallback == null) {
                fallback = wildcard;
            }
            if (fallback == null) {
                fallback = new MediaType();
            }
            normalized.addMediaType(APPLICATION_OCTET_STREAM, fallback);
        }

        return normalized;
    }

    private void ensureBinarySchema(Content content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        content.forEach((mediaTypeName, mediaType) -> {
            if (mediaType == null) {
                return;
            }

            Schema<?> schema = mediaType.getSchema();
            if (schema == null || "byte".equals(schema.getFormat())) {
                StringSchema binary = new StringSchema();
                binary.setFormat("binary");
                mediaType.setSchema(binary);
            }
        });
    }

    private void ensureSuccessResponseExample(String status, ApiResponse response) {
        if (response == null || isErrorStatus(status) || response.getContent() == null) {
            return;
        }

        MediaType mediaType = response.getContent().get(APPLICATION_JSON);
        if (mediaType == null || mediaType.getExample() != null) {
            return;
        }

        int statusCode = 200;
        try {
            statusCode = Integer.parseInt(status);
        } catch (NumberFormatException ignored) {
        }

        Map<String, Object> successExample = new LinkedHashMap<>();
        successExample.put("success", true);
        successExample.put("status", statusCode);
        successExample.put("data", Map.of("note", "응답 데이터 예시"));
        successExample.put("errorCode", null);
        successExample.put("message", null);
        mediaType.setExample(successExample);
    }

    private boolean isErrorStatus(String status) {
        if (status == null || status.length() != 3) {
            return false;
        }
        return status.startsWith("4") || status.startsWith("5");
    }

    private String mergeErrorGuide(String description, List<OpenApiEndpointContractRegistry.ErrorSpec> effectiveErrors) {
        String marker = "가능한 에러 코드:";
        String base = description;
        int markerIndex = description.indexOf(marker);
        if (markerIndex >= 0) {
            base = description.substring(0, markerIndex).trim();
        }

        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (OpenApiEndpointContractRegistry.ErrorSpec error : effectiveErrors) {
            lines.add("- " + error.code() + " (" + error.status() + "): " + error.message());
        }

        return base + "\n\n" + marker + "\n" + String.join("\n", lines);
    }

    private String resolveSchemaDescription(String schemaName) {
        if (schemaName.startsWith("ApiResponse")) {
            return "공통 API 응답 래퍼 스키마";
        }
        if (schemaName.startsWith("SliceResponse")) {
            return "무한 스크롤 Slice 응답 스키마";
        }
        if (schemaName.startsWith("CursorResponse")) {
            return "커서 기반 응답 스키마";
        }
        if (schemaName.endsWith("Request")) {
            return schemaName + " 요청 스키마";
        }
        if (schemaName.endsWith("Response")) {
            return schemaName + " 응답 스키마";
        }
        if (schemaName.endsWith("Type") || schemaName.endsWith("Status")) {
            return schemaName + " 코드 스키마";
        }
        return schemaName + " 스키마";
    }

    private String resolvePropertyDescription(String schemaName, String propertyName) {
        String specificKey = schemaName + "." + propertyName;
        if (SPECIFIC_FIELD_DESCRIPTIONS.containsKey(specificKey)) {
            return SPECIFIC_FIELD_DESCRIPTIONS.get(specificKey);
        }

        String normalizedSchemaName = schemaName
                .replace("LocationRequest", "ThreadCreateRequest.LocationRequest")
                .replace("ThreadFilterRequest", "ThreadCreateRequest.ThreadFilterRequest")
                .replace("ApiResponseMapStringString", "ApiResponse");

        specificKey = normalizedSchemaName + "." + propertyName;
        if (SPECIFIC_FIELD_DESCRIPTIONS.containsKey(specificKey)) {
            return SPECIFIC_FIELD_DESCRIPTIONS.get(specificKey);
        }

        if (GENERIC_FIELD_DESCRIPTIONS.containsKey(propertyName)) {
            return GENERIC_FIELD_DESCRIPTIONS.get(propertyName);
        }

        return propertyName + " 값입니다.";
    }

    private boolean shouldRefreshDescription(String description, String propertyName) {
        if (isBlank(description)) {
            return true;
        }

        String trimmed = description.trim();
        if (trimmed.equals(propertyName + " 값입니다.") || trimmed.equals(propertyName + " 값입니다")) {
            return true;
        }
        if ("성별 값입니다.".equals(trimmed)) {
            return true;
        }
        return trimmed.endsWith(" 값입니다.")
                && !trimmed.contains("가능 값")
                && !trimmed.contains("null이면");
    }

    private String resolveParameterDescription(String path, String parameterName) {
        if (PARAMETER_DESCRIPTION_OVERRIDES.containsKey(parameterName)) {
            return PARAMETER_DESCRIPTION_OVERRIDES.get(parameterName);
        }
        if (path.contains("{" + parameterName + "}")) {
            return parameterName + " 경로 식별자";
        }
        return parameterName + " 파라미터";
    }

    private Object resolveArrayExample(String schemaName, String fieldKey, String propertyName, ArraySchema arraySchema) {
        if (arraySchema.getItems() == null) {
            return List.of();
        }

        Schema<?> items = arraySchema.getItems();
        Object itemExample = items.getExample() != null
                ? items.getExample()
                : resolveExample(schemaName, propertyName, items);

        if (itemExample == null) {
            if (propertyName.toLowerCase(Locale.ROOT).contains("url")) {
                itemExample = "https://cdn.example.com/sample.jpg";
            } else if (propertyName.toLowerCase(Locale.ROOT).contains("id")) {
                itemExample = 101L;
            } else {
                itemExample = "예시 항목";
            }
        }

        if (fieldKey.contains("personalityTypeIds") || fieldKey.contains("petIds") || fieldKey.contains("personalityIds")) {
            return List.of(1L, 2L);
        }

        if (fieldKey.contains("imageUrls") || fieldKey.contains("photoUrls")) {
            return List.of("https://cdn.example.com/sample.jpg");
        }

        if (fieldKey.contains("filters")) {
            return List.of(Map.of(
                    "type", "AGE_GROUP",
                    "values", List.of("20", "30"),
                    "isRequired", false
            ));
        }

        return List.of(itemExample);
    }

    private Object resolveExample(String schemaName, String propertyName, Schema schema) {
        if (schema == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Object> enums = schema.getEnum();
        if (enums != null && !enums.isEmpty()) {
            return enums.get(0);
        }

        if (schema instanceof ArraySchema arraySchema) {
            return resolveArrayExample(schemaName, schemaName + "." + propertyName, propertyName, arraySchema);
        }

        String type = resolveSchemaType(schema);
        String format = Objects.toString(schema.getFormat(), "");
        if (type.isBlank()) {
            if ("int32".equals(format) || "int64".equals(format)) {
                type = "integer";
            } else if ("float".equals(format) || "double".equals(format) || "decimal".equals(format)) {
                type = "number";
            }
        }
        String lowerName = propertyName.toLowerCase(Locale.ROOT);

        if (type.isBlank()) {
            if (looksBooleanField(lowerName)) {
                type = "boolean";
            } else if (looksNumberField(lowerName)) {
                type = "number";
            } else if (looksIntegerField(lowerName)) {
                type = "integer";
            }
        }

        if ("array".equals(type)) {
            ArraySchema syntheticArraySchema = new ArraySchema();
            syntheticArraySchema.setItems(schema.getItems());
            return resolveArrayExample(schemaName, schemaName + "." + propertyName, propertyName, syntheticArraySchema);
        }

        if ("object".equals(type)) {
            return coerceObjectExample(propertyName, schema.getExample());
        }

        if ("boolean".equals(type)) {
            if (lowerName.contains("has") || lowerName.startsWith("is") || lowerName.contains("exists")) {
                return true;
            }
            return false;
        }

        if ("integer".equals(type)) {
            if (lowerName.contains("age")) {
                return 28;
            }
            if (lowerName.contains("score") || lowerName.contains("rating")) {
                return 5;
            }
            if (lowerName.contains("count") || lowerName.contains("size") || lowerName.contains("participants")) {
                return 20;
            }
            if ("int64".equals(format) || lowerName.endsWith("id") || lowerName.contains("id")) {
                return 101L;
            }
            return 1;
        }

        if ("number".equals(type)) {
            if (lowerName.contains("latitude")) {
                return 37.566295;
            }
            if (lowerName.contains("longitude")) {
                return 126.977945;
            }
            return 5.0;
        }

        if ("string".equals(type) || type.isBlank()) {
            if ("date-time".equals(format)) {
                return DATE_TIME_EXAMPLE_UTC;
            }
            if ("date".equals(format)) {
                return DATE_EXAMPLE_UTC;
            }
            if ("email".equals(format) || lowerName.contains("email")) {
                return "user@example.com";
            }
            if (lowerName.contains("content-type")) {
                return IMAGE_JPEG;
            }
            if ("uri".equals(format) || lowerName.contains("url")) {
                return "https://cdn.example.com/sample.jpg";
            }
            if (lowerName.contains("phone")) {
                return "01012345678";
            }
            if (lowerName.contains("nickname")) {
                return "몽이아빠";
            }
            if (lowerName.contains("title")) {
                return "아침 산책 메이트 구해요";
            }
            if (lowerName.contains("description")) {
                return "한강공원에서 30분 산책 예정입니다.";
            }
            if (lowerName.contains("content") || lowerName.contains("message")) {
                return "강아지와 즐거운 산책을 했어요.";
            }
            if (lowerName.contains("comment")) {
                return "상대방과의 산책 매너가 좋았어요.";
            }
            if (lowerName.contains("cursor")) {
                return "2701";
            }
            if (lowerName.contains("token")) {
                return "<TOKEN>";
            }
            if (lowerName.contains("id")) {
                return "101";
            }
            if (lowerName.contains("timezone")) {
                return "Asia/Seoul";
            }
            if (lowerName.contains("status")) {
                return "ACTIVE";
            }
            if (lowerName.contains("type")) {
                return "INDIVIDUAL";
            }
            if (lowerName.contains("name")) {
                return "몽이";
            }
            if (lowerName.contains("address")) {
                return "서울시 중구 세종대로 110";
            }
            if (lowerName.contains("code")) {
                return "C002";
            }
            return "예시 문자열";
        }

        return null;
    }

    private boolean looksBooleanField(String lowerName) {
        return lowerName.startsWith("is")
                || lowerName.startsWith("has")
                || lowerName.contains("allow")
                || lowerName.contains("enabled")
                || lowerName.contains("active")
                || lowerName.contains("visible")
                || lowerName.contains("public")
                || lowerName.contains("exists")
                || lowerName.contains("verified");
    }

    private boolean looksIntegerField(String lowerName) {
        return lowerName.endsWith("id")
                || lowerName.contains("id")
                || lowerName.contains("age")
                || lowerName.contains("count")
                || lowerName.contains("size")
                || lowerName.contains("rank")
                || lowerName.contains("participants")
                || lowerName.contains("days");
    }

    private boolean looksNumberField(String lowerName) {
        return lowerName.contains("score")
                || lowerName.contains("rating")
                || lowerName.contains("latitude")
                || lowerName.contains("longitude")
                || lowerName.contains("temperature")
                || lowerName.contains("value")
                || lowerName.contains("sum");
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_NAMES.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private boolean isPublicOperation(Operation operation) {
        return operation.getSecurity() != null && operation.getSecurity().isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
