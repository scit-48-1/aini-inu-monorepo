package scit.ainiinu.lostpet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import scit.ainiinu.lostpet.integration.chat.ChatDirectAuthException;
import scit.ainiinu.lostpet.integration.chat.ChatDirectResponseSchemaException;
import scit.ainiinu.lostpet.integration.chat.ChatRoomDirectClient;
import scit.ainiinu.testsupport.IntegrationTestProfile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@IntegrationTestProfile
class ChatRoomDirectClientRetryIntegrationTest {

    @Autowired
    private ChatRoomDirectClient chatRoomDirectClient;

    @MockitoBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        reset(restTemplate);
    }

    @Test
    @DisplayName("5xx 연결 실패는 1회 재시도 후 성공하면 roomId를 반환한다")
    void retryOnServerError() {
        given(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        ))
                .willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .willReturn(successResponse(777L));

        Long result = chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token");

        assertThat(result).isEqualTo(777L);
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        );
    }

    @Test
    @DisplayName("401 인증 실패는 재시도하지 않고 AUTH 예외를 반환한다")
    void noRetryOnAuthFailure() {
        given(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        ))
                .willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token"))
                .isInstanceOf(ChatDirectAuthException.class);

        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        );
    }

    @Test
    @DisplayName("기타 4xx는 재시도하지 않고 RESPONSE_SCHEMA 예외를 반환한다")
    void noRetryOnClientError() {
        given(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        ))
                .willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token"))
                .isInstanceOf(ChatDirectResponseSchemaException.class);

        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        );
    }

    @Test
    @DisplayName("응답 스키마 오류는 재시도하지 않는다")
    void noRetryOnResponseSchemaFailure() {
        given(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        ))
                .willReturn(ResponseEntity.ok(Map.of("data", Map.of())));

        assertThatThrownBy(() -> chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token"))
                .isInstanceOf(ChatDirectResponseSchemaException.class);

        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                anyTypeReference()
        );
    }

    @SuppressWarnings("unchecked")
    private ParameterizedTypeReference<Map<String, Object>> anyTypeReference() {
        return any(ParameterizedTypeReference.class);
    }

    private ResponseEntity<Map<String, Object>> successResponse(Long chatRoomId) {
        return ResponseEntity.ok(Map.of("data", Map.of("chatRoomId", chatRoomId)));
    }
}
