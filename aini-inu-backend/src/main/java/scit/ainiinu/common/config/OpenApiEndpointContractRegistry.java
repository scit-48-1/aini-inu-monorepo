package scit.ainiinu.common.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OpenApiEndpointContractRegistry {

    private static final String METHOD_GET = "get";
    private static final String METHOD_POST = "post";
    private static final String METHOD_PUT = "put";
    private static final String METHOD_PATCH = "patch";
    private static final String METHOD_DELETE = "delete";

    private static final Map<EndpointKey, List<ErrorSpec>> DOMAIN_ERRORS = new LinkedHashMap<>();

    static {
        // Member/Auth
        register(METHOD_POST, "/api/v1/members/profile", 409, "M003", "이미 사용 중인 닉네임입니다.", "프로필 닉네임이 기존 회원과 중복된 경우");
        register(METHOD_POST, "/api/v1/members/profile", 422, "C002", "입력값 검증에 실패했습니다.", "프로필 생성 요청 바디 검증에 실패한 경우");
        register(METHOD_POST, "/api/v1/members/signup", 409, "M007", "이미 사용 중인 이메일입니다.", "회원가입 이메일이 기존 회원과 중복된 경우");
        register(METHOD_GET, "/api/v1/members/{memberId}", 404, "M001", "회원을 찾을 수 없습니다.", "memberId에 해당하는 회원이 없는 경우");
        register(METHOD_GET, "/api/v1/members/{memberId}/pets", 404, "M001", "회원을 찾을 수 없습니다.", "memberId에 해당하는 회원이 없는 경우");
        register(METHOD_POST, "/api/v1/members/me/follows/{targetId}", 404, "M001", "회원을 찾을 수 없습니다.", "팔로우 대상 회원이 없는 경우");

        register(METHOD_POST, "/api/v1/auth/login", 401, "M008", "이메일 또는 비밀번호가 올바르지 않습니다.", "이메일/비밀번호 인증에 실패한 경우");
        register(METHOD_POST, "/api/v1/auth/login", 403, "M005", "정지된 회원입니다.", "정지된 회원이 로그인 시도한 경우");
        register(METHOD_POST, "/api/v1/auth/refresh", 401, "C102", "유효하지 않은 토큰입니다.", "Refresh token이 유효하지 않거나 DB에 존재하지 않는 경우");

        // Pet
        register(METHOD_PATCH, "/api/v1/pets/{petId}", 404, "P001", "반려견을 찾을 수 없습니다", "petId에 해당하는 반려견이 없는 경우");
        register(METHOD_PATCH, "/api/v1/pets/{petId}", 403, "P006", "본인 소유의 반려견이 아닙니다", "내 소유가 아닌 반려견을 수정한 경우");
        register(METHOD_DELETE, "/api/v1/pets/{petId}", 404, "P001", "반려견을 찾을 수 없습니다", "petId에 해당하는 반려견이 없는 경우");
        register(METHOD_DELETE, "/api/v1/pets/{petId}", 403, "P006", "본인 소유의 반려견이 아닙니다", "내 소유가 아닌 반려견을 삭제한 경우");
        register(METHOD_PATCH, "/api/v1/pets/{petId}/main", 404, "P001", "반려견을 찾을 수 없습니다", "petId에 해당하는 반려견이 없는 경우");
        register(METHOD_PATCH, "/api/v1/pets/{petId}/main", 403, "P006", "본인 소유의 반려견이 아닙니다", "내 소유가 아닌 반려견을 메인으로 변경한 경우");

        // Walk threads
        register(METHOD_POST, "/api/v1/threads", 403, "T403_NON_PET_OWNER_CREATE_FORBIDDEN", "비애견인은 스레드를 생성할 수 없습니다", "비애견인 회원이 모집글을 생성한 경우");
        register(METHOD_POST, "/api/v1/threads", 409, "T409_THREAD_ALREADY_ACTIVE", "이미 활성 스레드가 존재합니다", "동일 조건의 활성 모집글이 이미 존재하는 경우");
        register(METHOD_GET, "/api/v1/threads/{threadId}", 404, "T404_THREAD_NOT_FOUND", "스레드를 찾을 수 없습니다.", "threadId에 해당하는 모집글이 없는 경우");
        register(METHOD_PATCH, "/api/v1/threads/{threadId}", 404, "T404_THREAD_NOT_FOUND", "스레드를 찾을 수 없습니다.", "threadId에 해당하는 모집글이 없는 경우");
        register(METHOD_PATCH, "/api/v1/threads/{threadId}", 403, "T403_THREAD_OWNER_ONLY", "작성자만 처리할 수 있습니다", "작성자가 아닌 사용자가 모집글을 수정한 경우");
        register(METHOD_DELETE, "/api/v1/threads/{threadId}", 404, "T404_THREAD_NOT_FOUND", "스레드를 찾을 수 없습니다.", "threadId에 해당하는 모집글이 없는 경우");
        register(METHOD_DELETE, "/api/v1/threads/{threadId}", 403, "T403_THREAD_OWNER_ONLY", "작성자만 처리할 수 있습니다", "작성자가 아닌 사용자가 모집글을 삭제한 경우");
        register(METHOD_POST, "/api/v1/threads/{threadId}/apply", 404, "T404_THREAD_NOT_FOUND", "스레드를 찾을 수 없습니다.", "신청 대상 모집글이 없는 경우");
        register(METHOD_POST, "/api/v1/threads/{threadId}/apply", 403, "T403_APPLY_FORBIDDEN_SELF", "본인 스레드에는 신청할 수 없습니다", "작성자가 자기 글에 신청한 경우");
        register(METHOD_POST, "/api/v1/threads/{threadId}/apply", 409, "T409_CAPACITY_FULL", "스레드 정원이 가득 찼습니다", "정원 초과 또는 만료 상태로 신청이 불가한 경우");
        register(METHOD_DELETE, "/api/v1/threads/{threadId}/apply", 404, "T404_THREAD_APPLY_NOT_FOUND", "신청 정보를 찾을 수 없습니다", "취소할 신청 정보가 없는 경우");

        // Walk diaries
        register(METHOD_POST, "/api/v1/walk-diaries", 404, "WD404_THREAD_NOT_FOUND", "연결된 스레드를 찾을 수 없습니다", "연결된 threadId가 존재하지 않는 경우");
        register(METHOD_POST, "/api/v1/walk-diaries", 422, "WD422_IMAGE_COUNT_EXCEEDED", "이미지는 최대 5장까지 가능합니다", "요청 이미지 개수가 5장을 초과한 경우");
        register(METHOD_GET, "/api/v1/walk-diaries/{diaryId}", 404, "WD404_DIARY_NOT_FOUND", "일기를 찾을 수 없습니다.", "diaryId가 없거나 비공개 일기에 접근한 경우");
        register(METHOD_PATCH, "/api/v1/walk-diaries/{diaryId}", 404, "WD404_DIARY_NOT_FOUND", "일기를 찾을 수 없습니다.", "diaryId가 없거나 연결 threadId가 유효하지 않은 경우");
        register(METHOD_PATCH, "/api/v1/walk-diaries/{diaryId}", 403, "WD403_DIARY_OWNER_ONLY", "작성자만 처리할 수 있습니다", "작성자가 아닌 사용자가 수정한 경우");
        register(METHOD_PATCH, "/api/v1/walk-diaries/{diaryId}", 422, "WD422_IMAGE_COUNT_EXCEEDED", "이미지는 최대 5장까지 가능합니다", "요청 이미지 개수가 5장을 초과한 경우");
        register(METHOD_DELETE, "/api/v1/walk-diaries/{diaryId}", 404, "WD404_DIARY_NOT_FOUND", "일기를 찾을 수 없습니다.", "diaryId가 없는 경우");
        register(METHOD_DELETE, "/api/v1/walk-diaries/{diaryId}", 403, "WD403_DIARY_OWNER_ONLY", "작성자만 처리할 수 있습니다", "작성자가 아닌 사용자가 삭제한 경우");

        // Chat
        register(METHOD_GET, "/api/v1/chat-rooms", 400, "CH400_INVALID_REQUEST", "잘못된 채팅 요청입니다", "status 파라미터가 허용되지 않은 값인 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/direct", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "대상 회원이 존재하지 않거나 접근이 허용되지 않는 경우");

        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}", 404, "CH404_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다", "chatRoomId에 해당하는 채팅방이 없는 경우");
        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");

        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}/messages", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");
        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}/messages", 400, "CH400_INVALID_CURSOR", "유효하지 않은 커서입니다", "cursor/direction 파라미터 형식이 잘못된 경우");

        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/messages", 404, "CH404_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다", "chatRoomId에 해당하는 채팅방이 없는 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/messages", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/messages", 409, "CH409_ROOM_CLOSED", "종료된 채팅방에는 메시지를 보낼 수 없습니다", "종료된 채팅방에 메시지 전송을 시도한 경우");

        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/messages/read", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/messages/read", 404, "CH404_MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다", "messageId가 없거나 채팅방과 일치하지 않는 경우");

        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/leave", 404, "CH404_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다", "chatRoomId에 해당하는 채팅방이 없는 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/leave", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/leave", 409, "CH409_ROOM_ALREADY_LEFT", "이미 채팅방에서 나간 상태입니다", "이미 채팅방에서 이탈한 사용자인 경우");

        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 404, "CH404_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다", "chatRoomId에 해당하는 채팅방이 없는 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 400, "CH400_INVALID_WALK_CONFIRM_ACTION", "지원하지 않는 산책확인 액션입니다", "action 값이 CONFIRM/CANCEL 이외인 경우");

        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 404, "CH404_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다", "chatRoomId에 해당하는 채팅방이 없는 경우");
        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");

        register(METHOD_DELETE, "/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 404, "CH404_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다", "chatRoomId에 해당하는 채팅방이 없는 경우");
        register(METHOD_DELETE, "/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");

        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}/reviews/me", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");

        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/reviews", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");
        register(METHOD_POST, "/api/v1/chat-rooms/{chatRoomId}/reviews", 409, "CH409_REVIEW_ALREADY_EXISTS", "이미 리뷰를 작성했습니다", "동일 리뷰어/리뷰이 조합으로 리뷰가 이미 존재하는 경우");

        register(METHOD_GET, "/api/v1/chat-rooms/{chatRoomId}/reviews", 403, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다", "채팅방 참여자가 아닌 경우");

        // Community
        register(METHOD_GET, "/api/v1/posts/{postId}", 404, "CO001", "게시물을 찾을 수 없습니다", "postId에 해당하는 게시글이 없는 경우");
        register(METHOD_GET, "/api/v1/posts/{postId}/comments", 404, "CO001", "게시물을 찾을 수 없습니다", "postId에 해당하는 게시글이 없는 경우");

        register(METHOD_PATCH, "/api/v1/posts/{postId}", 404, "CO001", "게시물을 찾을 수 없습니다", "postId에 해당하는 게시글이 없는 경우");
        register(METHOD_PATCH, "/api/v1/posts/{postId}", 403, "CO002", "게시물 작성자가 아닙니다", "작성자가 아닌 사용자가 게시글을 수정한 경우");

        register(METHOD_DELETE, "/api/v1/posts/{postId}", 404, "CO001", "게시물을 찾을 수 없습니다", "postId에 해당하는 게시글이 없는 경우");
        register(METHOD_DELETE, "/api/v1/posts/{postId}", 403, "CO002", "게시물 작성자가 아닙니다", "작성자가 아닌 사용자가 게시글을 삭제한 경우");

        register(METHOD_POST, "/api/v1/posts/{postId}/like", 404, "CO001", "게시물을 찾을 수 없습니다", "postId에 해당하는 게시글이 없는 경우");
        register(METHOD_POST, "/api/v1/posts/{postId}/comments", 404, "CO001", "게시물을 찾을 수 없습니다", "postId에 해당하는 게시글이 없는 경우");

        register(METHOD_DELETE, "/api/v1/posts/{postId}/comments/{commentId}", 404, "CO003", "댓글을 찾을 수 없습니다", "commentId가 없거나 postId와 매칭되지 않는 경우");
        register(METHOD_DELETE, "/api/v1/posts/{postId}/comments/{commentId}", 403, "CO004", "댓글 삭제 권한이 없습니다", "댓글 작성자/게시글 작성자가 아닌 사용자가 삭제한 경우");

        register(METHOD_POST, "/api/v1/images/presigned-url", 415, "CO006", "허용되지 않는 이미지 타입입니다", "요청 contentType이 허용 MIME(image/jpeg,png,webp)가 아닌 경우");
        register(METHOD_PUT, "/api/v1/images/presigned-upload/{token}", 403, "CO009", "업로드 URL이 만료되었거나 유효하지 않습니다", "토큰이 없거나 만료되어 업로드 컨텍스트를 찾을 수 없는 경우");
        register(METHOD_PUT, "/api/v1/images/presigned-upload/{token}", 413, "CO007", "파일 크기는 10MB를 초과할 수 없습니다", "업로드 페이로드가 최대 허용 크기를 초과한 경우");
        register(METHOD_PUT, "/api/v1/images/presigned-upload/{token}", 415, "CO006", "허용되지 않는 이미지 타입입니다", "요청 헤더 Content-Type이 발급 시 MIME과 다르거나 허용 범위를 벗어난 경우");
        register(METHOD_PUT, "/api/v1/images/presigned-upload/{token}", 503, "CO010", "저장소를 사용할 수 없습니다", "파일 저장소 I/O 오류가 발생한 경우");
        register(METHOD_GET, "/api/v1/images/local", 403, "CO009", "업로드 URL이 만료되었거나 유효하지 않습니다", "key가 유효하지 않거나 로컬 파일이 존재하지 않는 경우");

        // Lost-pet
        register(METHOD_POST, "/api/v1/lost-pets", 409, "L409_DUPLICATE_ACTIVE_REPORT", "미해결 중복 실종 신고가 존재합니다.", "동일 반려견/품종 기준 활성 실종 신고가 이미 존재하는 경우");

        register(METHOD_GET, "/api/v1/lost-pets/{lostPetId}", 404, "L404", "요청한 대상을 찾을 수 없습니다.", "lostPetId에 해당하는 실종 신고가 없는 경우");
        register(METHOD_GET, "/api/v1/lost-pets/{lostPetId}", 403, "L403", "권한이 없습니다.", "신고 소유자가 아닌 사용자가 상세 조회한 경우");

        register(METHOD_POST, "/api/v1/lost-pets/analyze", 404, "L404", "요청한 대상을 찾을 수 없습니다.", "lostPetId에 해당하는 실종 신고가 없는 경우");
        register(METHOD_POST, "/api/v1/lost-pets/analyze", 403, "L403", "권한이 없습니다.", "신고 소유자가 아닌 사용자가 분석을 시도한 경우");
        register(METHOD_POST, "/api/v1/lost-pets/analyze", 500, "L500_AI_ANALYZE_FAILED", "AI 분석에 실패했습니다.", "AI 분석 결과가 비정상 또는 외부 호출에 실패한 경우");

        register(METHOD_GET, "/api/v1/lost-pets/{lostPetId}/match", 404, "L404_SEARCH_SESSION_NOT_FOUND", "탐색 세션을 찾을 수 없습니다.", "조회 가능한 탐색 세션이 없는 경우");
        register(METHOD_GET, "/api/v1/lost-pets/{lostPetId}/match", 403, "L403", "권한이 없습니다.", "신고 소유자가 아닌 사용자가 후보 조회한 경우");
        register(METHOD_GET, "/api/v1/lost-pets/{lostPetId}/match", 410, "L410_SEARCH_SESSION_EXPIRED", "탐색 세션이 만료되었습니다.", "탐색 세션이 만료된 경우");

        register(METHOD_POST, "/api/v1/lost-pets/{lostPetId}/match", 404, "L404_SEARCH_SESSION_NOT_FOUND", "탐색 세션을 찾을 수 없습니다.", "승인 대상 탐색 세션을 찾을 수 없는 경우");
        register(METHOD_POST, "/api/v1/lost-pets/{lostPetId}/match", 403, "L403", "권한이 없습니다.", "신고 소유자가 아닌 사용자가 승인한 경우");
        register(METHOD_POST, "/api/v1/lost-pets/{lostPetId}/match", 409, "L409_MATCH_CONFLICT", "현재 매치 상태에서는 승인할 수 없습니다.", "후보/매치 상태가 승인 가능한 상태가 아닌 경우");
        register(METHOD_POST, "/api/v1/lost-pets/{lostPetId}/match", 410, "L410_SEARCH_SESSION_EXPIRED", "탐색 세션이 만료되었습니다.", "탐색 세션 또는 신고 상태가 만료/종료된 경우");
    }

    private OpenApiEndpointContractRegistry() {
    }

    public static boolean isBinaryRequest(String method, String path) {
        return is(method, METHOD_PUT, path, "/api/v1/images/presigned-upload/{token}");
    }

    public static boolean isBinaryResponse(String method, String path, String status) {
        return "200".equals(status) && is(method, METHOD_GET, path, "/api/v1/images/local");
    }

    public static List<ErrorSpec> effectiveErrors(String method, String path, boolean isPublic, boolean hasRequestBody) {
        String normalizedMethod = normalizeMethod(method);
        LinkedHashMap<Integer, ErrorSpec> byStatus = new LinkedHashMap<>();

        if (hasRequestBody) {
            byStatus.put(400, new ErrorSpec(400, "C002", "입력값 검증에 실패했습니다.", "요청 본문/파라미터 검증 제약을 위반한 경우"));
        }

        if (!isPublic) {
            byStatus.put(401, new ErrorSpec(401, "C101", "인증이 필요합니다", "Authorization 헤더가 없거나 인증 토큰이 유효하지 않은 경우"));
            byStatus.put(403, new ErrorSpec(403, "C201", "권한이 없습니다", "인증은 되었지만 리소스 접근 권한이 없는 경우"));
        }

        for (ErrorSpec domain : domainErrors(normalizedMethod, path)) {
            byStatus.put(domain.status(), domain);
        }

        byStatus.putIfAbsent(500, new ErrorSpec(500, "C999", "서버 내부 오류가 발생했습니다", "예상하지 못한 서버 오류가 발생한 경우"));
        return List.copyOf(byStatus.values());
    }

    public static List<DomainErrorRow> domainErrorRows() {
        List<DomainErrorRow> rows = new ArrayList<>();
        DOMAIN_ERRORS.forEach((key, errors) -> {
            for (ErrorSpec error : errors) {
                rows.add(new DomainErrorRow(
                        key.method().toUpperCase(Locale.ROOT),
                        key.path(),
                        error.status(),
                        error.code(),
                        error.condition(),
                        error.message()
                ));
            }
        });
        return rows;
    }

    public static List<ErrorSpec> domainErrors(String method, String path) {
        EndpointKey key = new EndpointKey(normalizeMethod(method), path);
        List<ErrorSpec> errors = DOMAIN_ERRORS.get(key);
        return errors == null ? List.of() : errors;
    }

    private static void register(String method, String path, int status, String code, String message, String condition) {
        EndpointKey key = new EndpointKey(normalizeMethod(method), path);
        DOMAIN_ERRORS.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(new ErrorSpec(status, code, message, condition));
    }

    private static String normalizeMethod(String method) {
        return method == null ? "" : method.toLowerCase(Locale.ROOT);
    }

    private static boolean is(String method, String expectedMethod, String path, String expectedPath) {
        return expectedMethod.equals(normalizeMethod(method)) && expectedPath.equals(path);
    }

    public record EndpointKey(String method, String path) {
    }

    public record ErrorSpec(int status, String code, String message, String condition) {
    }

    public record DomainErrorRow(String method, String path, int status, String code, String condition, String exampleMessage) {
    }
}
