package scit.ainiinu.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.annotation.Public;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aini-Inu API")
                        .version("v1")
                        .description("Aini-Inu 백엔드 REST API 문서")
                        .contact(new Contact().name("Aini-Inu Backend Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public GroupedOpenApi v1OpenApi() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/test/auth/me")
                .build();
    }

    @Bean
    public ParameterCustomizer currentMemberParameterCustomizer() {
        return (Parameter parameter, org.springframework.core.MethodParameter methodParameter) -> {
            if (methodParameter.hasParameterAnnotation(CurrentMember.class)) {
                return null;
            }
            return parameter;
        };
    }

    @Bean
    public OperationCustomizer publicEndpointSecurityCustomizer() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(Public.class) || handlerMethod.getBeanType().isAnnotationPresent(Public.class)) {
                operation.setSecurity(List.of());
            }
            return operation;
        };
    }
}
