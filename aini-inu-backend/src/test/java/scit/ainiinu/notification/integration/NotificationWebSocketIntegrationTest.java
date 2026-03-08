package scit.ainiinu.notification.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.notification.dto.response.NotificationResponse;
import scit.ainiinu.testsupport.IntegrationTestProfile;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:notif-ws-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@IntegrationTestProfile
@Transactional
@DirtiesContext
class NotificationWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("/ws/notifications 엔드포인트로 STOMP 연결 후 알림을 수신한다")
    void receivesNotificationEvent() throws Exception {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("notif-ws-member@test.com")
                .nickname("notifws1")
                .memberType(MemberType.PET_OWNER)
                .build());
        String token = jwtTokenProvider.generateAccessToken(member.getId());

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        BlockingQueue<Map<String, Object>> events = new ArrayBlockingQueue<>(1);
        CountDownLatch messageLatch = new CountDownLatch(1);

        StompSession session = stompClient.connectAsync(
                String.format("ws://localhost:%d/ws/notifications", port),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS);

        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof Map<?, ?> mapPayload) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> event = (Map<String, Object>) mapPayload;
                    events.offer(event);
                    messageLatch.countDown();
                }
            }
        });
        TimeUnit.MILLISECONDS.sleep(200);

        // when
        NotificationResponse response = new NotificationResponse(
                "CHAT_NEW_MESSAGE",
                Map.of("roomId", 10, "senderMemberId", 99),
                LocalDateTime.now()
        );
        messagingTemplate.convertAndSendToUser(
                member.getId().toString(),
                "/queue/notifications",
                response
        );

        // then
        assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
        Map<String, Object> event = events.poll(1, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.get("type")).isEqualTo("CHAT_NEW_MESSAGE");

        session.disconnect();
    }

    @Test
    @DisplayName("Authorization 없이 /ws/notifications 연결 시 거부된다")
    void connectWithoutAuthorizationHeader_fails() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        assertThatThrownBy(() -> stompClient.connectAsync(
                String.format("ws://localhost:%d/ws/notifications", port),
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS)).isInstanceOf(Exception.class);
    }
}
