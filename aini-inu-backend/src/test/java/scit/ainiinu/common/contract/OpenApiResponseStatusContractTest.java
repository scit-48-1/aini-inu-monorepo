package scit.ainiinu.common.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.config.OpenApiEndpointContractRegistry;
import scit.ainiinu.testsupport.IntegrationTestProfile;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@IntegrationTestProfile
class OpenApiResponseStatusContractTest {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "options", "head", "trace"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI 응답 코드 집합이 레지스트리와 정확히 일치해야 한다")
    void openApiResponseStatusAlignedWithRegistry() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        JsonNode paths = root.path("paths");
        assertThat(paths.isObject()).isTrue();

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
                JsonNode responses = operation.path("responses");
                String opLabel = method.toUpperCase() + " " + path;

                assertThat(responses.isObject())
                        .as("responses missing: %s", opLabel)
                        .isTrue();
                assertThat(hasSuccessResponse(responses))
                        .as("2xx success response missing: %s", opLabel)
                        .isTrue();

                boolean isPublic = operation.path("security").isArray() && operation.path("security").size() == 0;
                boolean hasRequestBody = operation.path("requestBody").isObject();

                List<OpenApiEndpointContractRegistry.ErrorSpec> expectedErrors =
                        OpenApiEndpointContractRegistry.effectiveErrors(method, path, isPublic, hasRequestBody);

                Set<String> expectedStatuses = expectedErrors.stream()
                        .map(error -> String.valueOf(error.status()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                Set<String> actualErrorStatuses = collectErrorStatuses(responses);

                assertThat(actualErrorStatuses)
                        .as("error status mismatch: %s", opLabel)
                        .containsExactlyInAnyOrderElementsOf(expectedStatuses);

                for (String statusCode : expectedStatuses) {
                    JsonNode response = responses.path(statusCode);
                    assertThat(response.path("content").path("application/json").isObject())
                            .as("error response media type must be application/json: %s :: %s", opLabel, statusCode)
                            .isTrue();
                }
            }
        }
    }

    private Set<String> collectErrorStatuses(JsonNode responses) {
        Set<String> statuses = new LinkedHashSet<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = responses.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (isErrorStatus(entry.getKey())) {
                statuses.add(entry.getKey());
            }
        }
        return statuses;
    }

    private boolean hasSuccessResponse(JsonNode responses) {
        Iterator<String> iterator = responses.fieldNames();
        while (iterator.hasNext()) {
            String status = iterator.next();
            if (isSuccessStatus(status)) {
                return true;
            }
        }
        return false;
    }

    private boolean isErrorStatus(String status) {
        return status != null && status.length() == 3 && (status.startsWith("4") || status.startsWith("5"));
    }

    private boolean isSuccessStatus(String status) {
        return status != null && status.length() == 3 && status.startsWith("2");
    }
}
