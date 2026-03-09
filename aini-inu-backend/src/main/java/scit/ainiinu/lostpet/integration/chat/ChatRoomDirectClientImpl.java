package scit.ainiinu.lostpet.integration.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.dto.request.ChatRoomDirectCreateRequest;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.service.ChatRoomService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomDirectClientImpl implements ChatRoomDirectClient {

    private final ChatRoomService chatRoomService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createDirectRoom(Long memberId, Long partnerId, String origin, String roomTitle) {
        ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
        request.setPartnerId(partnerId);
        request.setOrigin(origin);
        request.setRoomTitle(roomTitle);

        ChatRoomDetailResponse response = chatRoomService.createDirectRoom(memberId, request);
        return response.getChatRoomId();
    }
}
