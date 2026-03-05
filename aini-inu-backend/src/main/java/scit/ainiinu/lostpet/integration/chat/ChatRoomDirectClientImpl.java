package scit.ainiinu.lostpet.integration.chat;

import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ChatRoomDirectClientImpl implements ChatRoomDirectClient {

    private final RestTemplate restTemplate;

    @Value("${lostpet.chat.base-url}")
    private String chatBaseUrl;

    @Value("${lostpet.chat.direct-create-path}")
    private String directCreatePath;

    public ChatRoomDirectClientImpl(@Qualifier("chatRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "lostpetChatDirect")
    public Long createDirectRoom(Long partnerId, String authorizationHeader) {
        String endpoint = chatBaseUrl + directCreatePath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (hasText(authorizationHeader)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(Map.of("partnerId", partnerId), headers);

        ResponseEntity<Map<String, Object>> responseEntity;
        try {
            responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    httpEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
        } catch (HttpClientErrorException exception) {
            int statusCode = exception.getStatusCode().value();
            if (statusCode == 401 || statusCode == 403) {
                throw new ChatDirectAuthException(
                        "chat direct authorization failed",
                        exception
                );
            }
            throw new ChatDirectResponseSchemaException(
                    "chat direct request rejected",
                    exception
            );
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new ChatDirectConnectException(
                    "chat direct connection failed",
                    exception
            );
        } catch (RestClientException exception) {
            throw new ChatDirectClientException(
                    ChatDirectFailureType.UNKNOWN,
                    "chat direct rest client failure",
                    exception
            );
        } catch (Exception exception) {
            throw new ChatDirectClientException(
                    ChatDirectFailureType.UNKNOWN,
                    "chat direct unknown failure",
                    exception
            );
        }

        Map<String, Object> response = responseEntity.getBody();
        if (response == null) {
            throw new ChatDirectResponseSchemaException("chat direct response body is null");
        }

        Object dataObj = response.get("data");
        if (dataObj instanceof Map<?, ?> dataMap) {
            Object roomIdObj = dataMap.get("chatRoomId");
            if (roomIdObj instanceof Number number) {
                return number.longValue();
            }
        }
        throw new ChatDirectResponseSchemaException("chat direct response schema invalid");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
