package scit.ainiinu.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class ChatStompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || authorization.isBlank() || !authorization.startsWith(BEARER_PREFIX)) {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
            }
            String token = authorization.substring(BEARER_PREFIX.length());
            Long memberId = jwtTokenProvider.validateAndGetMemberId(token);
            Principal principal = new UsernamePasswordAuthenticationToken(memberId.toString(), null);
            accessor.setUser(principal);
        }

        return message;
    }
}
