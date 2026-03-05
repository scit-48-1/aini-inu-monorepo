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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@IntegrationTestProfile
class OpenApiPaginationSortContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Pageable API 정렬 파라미터 문서 계약이 명시되어야 한다")
    void openApiPageableSortContractAligned() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        JsonNode paths = root.path("paths");
        assertThat(paths.isObject()).isTrue();

        List<SortContract> contracts = List.of(
                new SortContract("/api/v1/walk-diaries", "get", false, List.of("createdAt,desc", "id,desc")),
                new SortContract("/api/v1/walk-diaries/following", "get", true, List.of("createdAt,desc", "id,desc")),
                new SortContract("/api/v1/threads", "get", true, List.of("createdAt,desc", "id,desc")),
                new SortContract("/api/v1/posts", "get", false, List.of("createdAt,desc", "likeCount,desc", "commentCount,desc", "id,desc")),
                new SortContract("/api/v1/posts/{postId}/comments", "get", true, List.of("createdAt,asc")),
                new SortContract("/api/v1/stories", "get", true, List.of("createdAt,desc", "memberId,desc")),
                new SortContract("/api/v1/chat-rooms", "get", true, List.of("updatedAt,desc", "id,desc")),
                new SortContract("/api/v1/chat-rooms/{chatRoomId}/reviews", "get", true, List.of("createdAt,desc", "id,desc")),
                new SortContract("/api/v1/members/search", "get", false, List.of("id,desc", "createdAt,desc", "nickname,asc")),
                new SortContract("/api/v1/members/me/followers", "get", true, List.of("createdAt,desc")),
                new SortContract("/api/v1/members/me/following", "get", true, List.of("createdAt,desc")),
                new SortContract("/api/v1/lost-pets", "get", false, List.of("id,desc", "createdAt,desc", "lastSeenAt,desc")),
                new SortContract("/api/v1/lost-pets/{lostPetId}/match", "get", true, List.of("rankOrder,asc"))
        );

        for (SortContract contract : contracts) {
            JsonNode operation = operation(paths, contract.path(), contract.method());
            JsonNode parameters = operation.path("parameters");

            JsonNode pageParam = findQueryParameter(parameters, "page");
            assertThat(pageParam.isMissingNode()).as("page parameter missing: %s %s", contract.method().toUpperCase(), contract.path()).isFalse();

            JsonNode sizeParam = findQueryParameter(parameters, "size");
            assertThat(sizeParam.isMissingNode()).as("size parameter missing: %s %s", contract.method().toUpperCase(), contract.path()).isFalse();

            JsonNode sortParam = findQueryParameter(parameters, "sort");
            assertThat(sortParam.isMissingNode()).as("sort parameter missing: %s %s", contract.method().toUpperCase(), contract.path()).isFalse();

            String description = sortParam.path("description").asText("");
            assertThat(description)
                    .as("sort JSON array warning missing: %s %s", contract.method().toUpperCase(), contract.path())
                    .contains("JSON 배열 형식 미지원");

            if (contract.fixedSort()) {
                assertThat(description)
                        .as("sort fixed-order warning missing: %s %s", contract.method().toUpperCase(), contract.path())
                        .contains("무시");
            }

            for (String fragment : contract.expectedDescriptionFragments()) {
                assertThat(description)
                        .as("sort description fragment missing: %s %s -> %s", contract.method().toUpperCase(), contract.path(), fragment)
                        .contains(fragment);
            }
        }
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

    private record SortContract(
            String path,
            String method,
            boolean fixedSort,
            List<String> expectedDescriptionFragments
    ) {
    }
}
