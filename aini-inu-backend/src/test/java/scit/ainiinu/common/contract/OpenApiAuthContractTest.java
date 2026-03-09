package scit.ainiinu.common.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.testsupport.IntegrationTestProfile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@IntegrationTestProfile
class OpenApiAuthContractTest {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "options", "head", "trace"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI 인증/보안 계약이 정합해야 한다")
    void openApiAuthContractAligned() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        JsonNode paths = root.path("paths");

        assertThat(paths.isObject()).isTrue();

        // 테스트 토큰 API는 문서 계약에 노출되어야 한다.
        assertThat(paths.has("/api/v1/test/auth/token")).isTrue();
        assertThat(paths.has("/api/v1/test/auth/me")).isFalse();

        // @Public 엔드포인트는 operation-level security가 빈 배열이어야 한다.
        assertNoSecurity(paths, "/api/v1/members/signup", "post");
        assertNoSecurity(paths, "/api/v1/auth/login", "post");
        assertNoSecurity(paths, "/api/v1/auth/refresh", "post");
        assertNoSecurity(paths, "/api/v1/images/presigned-upload/{token}", "put");
        assertNoSecurity(paths, "/api/v1/images/local", "get");

        // 일반 인증 엔드포인트는 bearerAuth가 유지되어야 한다.
        assertBearerSecurity(paths, "/api/v1/posts", "get");

        JsonNode walkDiariesGet = operation(paths, "/api/v1/walk-diaries", "get");
        JsonNode walkDiaryMemberIdParam = findQueryParameter(walkDiariesGet.path("parameters"), "memberId");
        assertThat(walkDiaryMemberIdParam.isMissingNode()).isFalse();
        assertThat(walkDiaryMemberIdParam.path("required").asBoolean(true)).isFalse();

        // 인증 주체(@CurrentMember)로 유래한 query 파라미터는 문서에 노출되면 안 된다.
        List<String> violations = collectCurrentMemberQueryViolations(paths);
        assertThat(violations).isEmpty();
    }

    private List<String> collectCurrentMemberQueryViolations(JsonNode paths) {
        List<String> violations = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.fields();

        while (pathIterator.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
            String path = pathEntry.getKey();
            JsonNode pathItem = pathEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> operationIterator = pathItem.fields();

            while (operationIterator.hasNext()) {
                Map.Entry<String, JsonNode> operationEntry = operationIterator.next();
                String method = operationEntry.getKey();
                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }

                JsonNode operation = operationEntry.getValue();
                JsonNode parameters = operation.path("parameters");
                if (!parameters.isArray()) {
                    continue;
                }

                for (JsonNode parameter : parameters) {
                    if (!"query".equals(parameter.path("in").asText())) {
                        continue;
                    }

                    String name = parameter.path("name").asText();
                    if ("authorId".equals(name)) {
                        // GET /api/v1/posts?authorId= 는 프로필 페이지에서 특정 사용자의 게시글 필터링용 정당한 파라미터
                        boolean isPostListFilter = "/api/v1/posts".equals(path) && "get".equals(method);
                        if (!isPostListFilter) {
                            violations.add(method.toUpperCase() + " " + path + " has forbidden query parameter: authorId");
                        }
                        continue;
                    }

                    if ("memberId".equals(name)) {
                        boolean isWalkDiaryList = "/api/v1/walk-diaries".equals(path) && "get".equals(method);
                        boolean isTestTokenEndpoint = "/api/v1/test/auth/token".equals(path) && "post".equals(method);
                        if (!isWalkDiaryList && !isTestTokenEndpoint) {
                            violations.add(method.toUpperCase() + " " + path + " has forbidden query parameter: memberId");
                            continue;
                        }

                        if (parameter.path("required").asBoolean(false)) {
                            violations.add("GET /api/v1/walk-diaries has memberId query parameter with required=true");
                        }
                    }
                }
            }
        }
        return violations;
    }

    private void assertNoSecurity(JsonNode paths, String path, String method) {
        JsonNode security = operation(paths, path, method).path("security");
        assertThat(security.isArray()).as("security array missing for %s %s", method.toUpperCase(), path).isTrue();
        assertThat(security.size()).as("security must be [] for %s %s", method.toUpperCase(), path).isZero();
    }

    private void assertBearerSecurity(JsonNode paths, String path, String method) {
        JsonNode security = operation(paths, path, method).path("security");
        assertThat(security.isArray()).as("security array missing for %s %s", method.toUpperCase(), path).isTrue();

        boolean hasBearerAuth = false;
        for (JsonNode securityRequirement : security) {
            if (securityRequirement.has("bearerAuth")) {
                hasBearerAuth = true;
                break;
            }
        }
        assertThat(hasBearerAuth).as("bearerAuth missing for %s %s", method.toUpperCase(), path).isTrue();
    }

    private JsonNode operation(JsonNode paths, String path, String method) {
        JsonNode pathItem = paths.path(path);
        assertThat(pathItem.isObject()).as("path not found: %s", path).isTrue();

        JsonNode operation = pathItem.path(method);
        assertThat(operation.isObject()).as("operation not found: %s %s", method.toUpperCase(), path).isTrue();
        return operation;
    }

    private JsonNode findQueryParameter(JsonNode parameters, String name) {
        if (!parameters.isArray()) {
            return MissingNode.getInstance();
        }
        for (JsonNode parameter : parameters) {
            if ("query".equals(parameter.path("in").asText()) && name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        return MissingNode.getInstance();
    }
}
