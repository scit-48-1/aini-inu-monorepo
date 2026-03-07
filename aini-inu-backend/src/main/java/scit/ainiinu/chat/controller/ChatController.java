package scit.ainiinu.chat.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.chat.dto.request.ChatMessageCreateRequest;
import scit.ainiinu.chat.dto.request.ChatReviewCreateRequest;
import scit.ainiinu.chat.dto.request.ChatRoomDirectCreateRequest;
import scit.ainiinu.chat.dto.request.MessageReadRequest;
import scit.ainiinu.chat.dto.request.WalkConfirmRequest;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;
import scit.ainiinu.chat.dto.response.ChatReviewResponse;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.dto.response.ChatRoomSummaryResponse;
import scit.ainiinu.chat.dto.response.LeaveRoomResponse;
import scit.ainiinu.chat.dto.response.MessageReadResponse;
import scit.ainiinu.chat.dto.response.MyChatReviewResponse;
import scit.ainiinu.chat.dto.response.WalkConfirmResponse;
import scit.ainiinu.chat.service.ChatReviewService;
import scit.ainiinu.chat.service.ChatRoomService;
import scit.ainiinu.chat.service.MessageService;
import scit.ainiinu.chat.service.WalkConfirmService;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.response.CursorResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-rooms")
@Tag(name = "Chat", description = "채팅 API")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final WalkConfirmService walkConfirmService;
    private final ChatReviewService chatReviewService;

    @GetMapping
    @Operation(summary = "채팅방 목록 조회", description = "상태 필터와 페이지 조건으로 채팅방 목록을 조회합니다.")
    @Parameters({
            @Parameter(
                    name = "page",
                    in = ParameterIn.QUERY,
                    description = "페이지 번호(0부터 시작)",
                    schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
            ),
            @Parameter(
                    name = "size",
                    in = ParameterIn.QUERY,
                    description = "페이지 크기",
                    schema = @Schema(type = "integer", defaultValue = "20", minimum = "1")
            ),
            @Parameter(
                    name = "sort",
                    in = ParameterIn.QUERY,
                    description = "서버 고정 정렬(updatedAt desc, id desc)로 처리되며 sort 파라미터는 무시됩니다. "
                            + "요청 형식 예: sort=updatedAt,desc&sort=id,desc (JSON 배열 형식 미지원)",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "updatedAt,desc"))
            )
    })
    public ResponseEntity<ApiResponse<SliceResponse<ChatRoomSummaryResponse>>> getChatRooms(
            @CurrentMember Long memberId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String origin,
            @Parameter(hidden = true)
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SliceResponse<ChatRoomSummaryResponse> response = chatRoomService.getRooms(memberId, status, origin, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/direct")
    @Operation(summary = "1:1 채팅방 생성", description = "대상 회원과의 direct 채팅방을 생성하거나 기존 방을 반환합니다.")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> createDirectRoom(
            @CurrentMember Long memberId,
            @Valid @RequestBody ChatRoomDirectCreateRequest request
    ) {
        ChatRoomDetailResponse response = chatRoomService.createDirectRoom(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{chatRoomId}")
    @Operation(summary = "채팅방 상세 조회", description = "chatRoomId 기준 채팅방 상세를 조회합니다.")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoom(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId
    ) {
        ChatRoomDetailResponse response = chatRoomService.getRoomDetail(memberId, chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{chatRoomId}/messages")
    @Operation(summary = "채팅 메시지 조회", description = "cursor 기반으로 채팅 메시지를 조회합니다.")
    public ResponseEntity<ApiResponse<CursorResponse<ChatMessageResponse>>> getMessages(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "before") String direction
    ) {
        CursorResponse<ChatMessageResponse> response = messageService.getMessages(memberId, chatRoomId, cursor, size, direction);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{chatRoomId}/messages")
    @Operation(summary = "채팅 메시지 전송", description = "채팅방에 새 메시지를 전송합니다.")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> createMessage(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatMessageCreateRequest request
    ) {
        ChatMessageResponse response = messageService.createMessage(memberId, chatRoomId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{chatRoomId}/messages/read")
    @Operation(summary = "메시지 읽음 처리", description = "채팅 메시지를 읽음 상태로 갱신합니다.")
    public ResponseEntity<ApiResponse<MessageReadResponse>> readMessage(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody MessageReadRequest request
    ) {
        MessageReadResponse response = messageService.markRead(memberId, chatRoomId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{chatRoomId}/leave")
    @Operation(summary = "채팅방 나가기", description = "현재 사용자를 채팅방 참여자에서 이탈 처리합니다.")
    public ResponseEntity<ApiResponse<LeaveRoomResponse>> leaveRoom(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId
    ) {
        LeaveRoomResponse response = chatRoomService.leaveRoom(memberId, chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{chatRoomId}/walk-confirm")
    @Operation(summary = "산책 확정/상태 갱신", description = "산책 확정 상태를 생성하거나 액션 기반으로 갱신합니다.")
    public ResponseEntity<ApiResponse<WalkConfirmResponse>> walkConfirm(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId,
            @RequestBody(required = false) WalkConfirmRequest request
    ) {
        WalkConfirmResponse response;
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            response = walkConfirmService.confirmWalk(memberId, chatRoomId);
        } else {
            response = walkConfirmService.updateWalkConfirm(memberId, chatRoomId, request);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{chatRoomId}/walk-confirm")
    @Operation(summary = "산책 확정 상태 조회", description = "채팅방의 산책 확정 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<WalkConfirmResponse>> getWalkConfirm(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId
    ) {
        WalkConfirmResponse response = walkConfirmService.getWalkConfirm(memberId, chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{chatRoomId}/walk-confirm")
    @Operation(summary = "산책 확정 취소", description = "채팅방 산책 확정을 취소합니다.")
    public ResponseEntity<ApiResponse<Void>> cancelWalkConfirm(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId
    ) {
        walkConfirmService.cancelWalkConfirm(memberId, chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{chatRoomId}/reviews/me")
    @Operation(summary = "내 리뷰 조회", description = "해당 채팅방에서 내가 작성한 리뷰를 조회합니다.")
    public ResponseEntity<ApiResponse<MyChatReviewResponse>> getMyReview(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId
    ) {
        MyChatReviewResponse response = chatReviewService.getMyReview(memberId, chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{chatRoomId}/reviews")
    @Operation(summary = "채팅 리뷰 작성", description = "채팅 상대에게 리뷰를 작성합니다.")
    public ResponseEntity<ApiResponse<ChatReviewResponse>> createReview(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatReviewCreateRequest request
    ) {
        ChatReviewResponse response = chatReviewService.createReview(memberId, chatRoomId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{chatRoomId}/reviews")
    @Operation(summary = "채팅 리뷰 목록 조회", description = "채팅방 리뷰 목록을 Slice로 조회합니다.")
    @Parameters({
            @Parameter(
                    name = "page",
                    in = ParameterIn.QUERY,
                    description = "페이지 번호(0부터 시작)",
                    schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
            ),
            @Parameter(
                    name = "size",
                    in = ParameterIn.QUERY,
                    description = "페이지 크기",
                    schema = @Schema(type = "integer", defaultValue = "20", minimum = "1")
            ),
            @Parameter(
                    name = "sort",
                    in = ParameterIn.QUERY,
                    description = "서버 고정 정렬(createdAt desc, id desc)로 처리되며 sort 파라미터는 무시됩니다. "
                            + "요청 형식 예: sort=createdAt,desc&sort=id,desc (JSON 배열 형식 미지원)",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "createdAt,desc"))
            )
    })
    public ResponseEntity<ApiResponse<SliceResponse<ChatReviewResponse>>> getReviews(
            @CurrentMember Long memberId,
            @PathVariable Long chatRoomId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SliceResponse<ChatReviewResponse> response = chatReviewService.getReviews(memberId, chatRoomId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
