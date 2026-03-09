package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.chat.dto.request.ChatRoomDirectCreateRequest;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.service.ChatRoomService;
import scit.ainiinu.lostpet.integration.chat.ChatRoomDirectClientImpl;

@ExtendWith(MockitoExtension.class)
class ChatRoomDirectClientImplUnitTest {

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatRoomDirectClientImpl chatRoomDirectClient;

    @Nested
    @DisplayName("createDirectRoom")
    class CreateDirectRoom {

        @Test
        @DisplayName("ChatRoomService 호출 성공 시 chatRoomId를 반환한다")
        void returnsChatRoomId() {
            ChatRoomDetailResponse response = ChatRoomDetailResponse.builder()
                    .chatRoomId(555L)
                    .chatType("DIRECT")
                    .status("ACTIVE")
                    .origin("LOST_PET")
                    .roomTitle("Poodle Momo를 찾습니다")
                    .build();
            given(chatRoomService.createDirectRoom(eq(10L), any(ChatRoomDirectCreateRequest.class)))
                    .willReturn(response);

            Long result = chatRoomDirectClient.createDirectRoom(10L, 22L, "LOST_PET", "Poodle Momo를 찾습니다");

            assertThat(result).isEqualTo(555L);
        }

        @Test
        @DisplayName("ChatRoomService에 올바른 파라미터를 전달한다")
        void passesCorrectParameters() {
            ChatRoomDetailResponse response = ChatRoomDetailResponse.builder()
                    .chatRoomId(100L)
                    .build();
            given(chatRoomService.createDirectRoom(eq(10L), any(ChatRoomDirectCreateRequest.class)))
                    .willReturn(response);

            chatRoomDirectClient.createDirectRoom(10L, 22L, "LOST_PET", "말티즈 몽이를 찾습니다");

            ArgumentCaptor<ChatRoomDirectCreateRequest> captor = ArgumentCaptor.forClass(ChatRoomDirectCreateRequest.class);
            verify(chatRoomService).createDirectRoom(eq(10L), captor.capture());
            ChatRoomDirectCreateRequest captured = captor.getValue();
            assertThat(captured.getPartnerId()).isEqualTo(22L);
            assertThat(captured.getOrigin()).isEqualTo("LOST_PET");
            assertThat(captured.getRoomTitle()).isEqualTo("말티즈 몽이를 찾습니다");
        }

        @Test
        @DisplayName("ChatRoomService 예외가 그대로 전파된다")
        void propagatesServiceException() {
            given(chatRoomService.createDirectRoom(eq(10L), any(ChatRoomDirectCreateRequest.class)))
                    .willThrow(new ChatException(ChatErrorCode.INVALID_REQUEST));

            assertThatThrownBy(() -> chatRoomDirectClient.createDirectRoom(10L, 22L, "LOST_PET", "title"))
                    .isInstanceOf(ChatException.class);
        }

        @Test
        @DisplayName("RuntimeException도 그대로 전파된다")
        void propagatesRuntimeException() {
            given(chatRoomService.createDirectRoom(eq(10L), any(ChatRoomDirectCreateRequest.class)))
                    .willThrow(new RuntimeException("unexpected error"));

            assertThatThrownBy(() -> chatRoomDirectClient.createDirectRoom(10L, 22L, "LOST_PET", "title"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("unexpected error");
        }

        @Test
        @DisplayName("origin이 null이어도 정상적으로 전달한다")
        void handlesNullOrigin() {
            ChatRoomDetailResponse response = ChatRoomDetailResponse.builder()
                    .chatRoomId(200L)
                    .build();
            given(chatRoomService.createDirectRoom(eq(10L), any(ChatRoomDirectCreateRequest.class)))
                    .willReturn(response);

            Long result = chatRoomDirectClient.createDirectRoom(10L, 22L, null, "title");

            assertThat(result).isEqualTo(200L);
            ArgumentCaptor<ChatRoomDirectCreateRequest> captor = ArgumentCaptor.forClass(ChatRoomDirectCreateRequest.class);
            verify(chatRoomService).createDirectRoom(eq(10L), captor.capture());
            assertThat(captor.getValue().getOrigin()).isNull();
        }

        @Test
        @DisplayName("roomTitle이 null이어도 정상적으로 전달한다")
        void handlesNullRoomTitle() {
            ChatRoomDetailResponse response = ChatRoomDetailResponse.builder()
                    .chatRoomId(300L)
                    .build();
            given(chatRoomService.createDirectRoom(eq(10L), any(ChatRoomDirectCreateRequest.class)))
                    .willReturn(response);

            Long result = chatRoomDirectClient.createDirectRoom(10L, 22L, "LOST_PET", null);

            assertThat(result).isEqualTo(300L);
            ArgumentCaptor<ChatRoomDirectCreateRequest> captor = ArgumentCaptor.forClass(ChatRoomDirectCreateRequest.class);
            verify(chatRoomService).createDirectRoom(eq(10L), captor.capture());
            assertThat(captor.getValue().getRoomTitle()).isNull();
        }
    }
}
