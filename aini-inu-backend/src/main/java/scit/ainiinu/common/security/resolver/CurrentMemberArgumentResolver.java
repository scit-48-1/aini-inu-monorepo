package scit.ainiinu.common.security.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;
import scit.ainiinu.common.security.annotation.CurrentMember;

/**
 * @CurrentMember 어노테이션이 붙은 파라미터에 memberId를 주입하는 ArgumentResolver
 *
 * JwtAuthInterceptor에서 request.setAttribute("memberId", memberId)로 설정한 값을
 * Controller 메서드의 @CurrentMember Long memberId 파라미터에 자동 주입합니다.
 *
 * 사용 예시:
 * <pre>
 * {@literal @}GetMapping("/posts")
 * public ResponseEntity<ApiResponse<List<Post>>> getPosts(
 *     {@literal @}CurrentMember Long memberId
 * ) {
 *     // memberId 자동 주입됨
 * }
 * </pre>
 */
@Slf4j
@Component
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String MEMBER_ID_ATTRIBUTE = "memberId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentMember 어노테이션이 있는 파라미터만 지원
        return parameter.hasParameterAnnotation(CurrentMember.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (request == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

        // JwtAuthInterceptor에서 설정한 memberId 추출
        Long memberId = (Long) request.getAttribute(MEMBER_ID_ATTRIBUTE);

        // @CurrentMember의 required 속성 확인
        CurrentMember annotation = parameter.getParameterAnnotation(CurrentMember.class);
        boolean required = annotation != null && annotation.required();

        // required=true인데 memberId가 null이면 예외
        if (memberId == null && required) {
            log.error("Required memberId is null for parameter: {}", parameter.getParameterName());
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return memberId;
    }
}
