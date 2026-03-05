package scit.ainiinu.common.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.testsupport.IntegrationTestProfile;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@IntegrationTestProfile
class OpenApiDocumentationQualityContractTest {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "options", "head", "trace"
    );

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "token", "accessToken", "refreshToken"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI 문서 품질 계약이 유지되어야 한다")
    void openApiDocumentationQualityAligned() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        JsonNode paths = root.path("paths");
        JsonNode schemas = root.path("components").path("schemas");

        assertThat(paths.isObject()).isTrue();
        assertThat(schemas.isObject()).isTrue();

        assertApiOperations(paths);
        assertSchemas(schemas);
    }

    private void assertApiOperations(JsonNode paths) {
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
                String opLabel = method.toUpperCase() + " " + path;

                assertThat(operation.path("summary").asText("").trim())
                        .as("summary missing: %s", opLabel)
                        .isNotBlank();

                assertThat(operation.path("description").asText("").trim())
                        .as("description missing: %s", opLabel)
                        .isNotBlank();

                assertParameters(operation.path("parameters"), opLabel);
                assertRequestBody(path, method, operation.path("requestBody"), opLabel);
                assertResponses(path, method, operation, opLabel);
            }
        }
    }

    private void assertParameters(JsonNode parameters, String opLabel) {
        if (!parameters.isArray()) {
            return;
        }

        for (JsonNode parameter : parameters) {
            String name = parameter.path("name").asText("<unknown>");
            assertThat(parameter.path("description").asText("").trim())
                    .as("parameter description missing: %s :: %s", opLabel, name)
                    .isNotBlank();

            JsonNode schema = parameter.path("schema");
            if (schema.isObject() && !isSensitive(name)) {
                assertThat(schema.path("example").isMissingNode())
                        .as("parameter example missing: %s :: %s", opLabel, name)
                        .isFalse();
            }
        }
    }

    private void assertRequestBody(String path, String method, JsonNode requestBody, String opLabel) {
        if (!requestBody.isObject()) {
            return;
        }

        JsonNode content = requestBody.path("content");
        if (isBinaryUpload(path, method)) {
            assertThat(
                    content.has("application/octet-stream")
                            || content.has("image/jpeg")
                            || content.has("image/png")
                            || content.has("image/webp")
            ).as("requestBody media type should be binary: %s", opLabel).isTrue();
            return;
        }

        assertThat(content.has("application/json"))
                .as("requestBody media type should include application/json: %s", opLabel)
                .isTrue();
    }

    private void assertResponses(String path, String method, JsonNode operation, String opLabel) {
        JsonNode responses = operation.path("responses");
        assertThat(responses.isObject())
                .as("responses missing: %s", opLabel)
                .isTrue();

        assertThat(responses.has("500"))
                .as("500 response missing: %s", opLabel)
                .isTrue();

        boolean isPublic = operation.path("security").isArray() && operation.path("security").size() == 0;
        if (!isPublic) {
            assertThat(responses.has("401")).as("401 response missing: %s", opLabel).isTrue();
            assertThat(responses.has("403")).as("403 response missing: %s", opLabel).isTrue();
        }

        boolean hasRequestBody = operation.path("requestBody").isObject();
        if (hasRequestBody) {
            assertThat(responses.has("400")).as("400 response missing: %s", opLabel).isTrue();
        }

        Iterator<Map.Entry<String, JsonNode>> responseIterator = responses.fields();
        while (responseIterator.hasNext()) {
            Map.Entry<String, JsonNode> responseEntry = responseIterator.next();
            String code = responseEntry.getKey();
            JsonNode response = responseEntry.getValue();

            assertThat(response.path("description").asText("").trim())
                    .as("response description missing: %s :: %s", opLabel, code)
                    .isNotBlank();

            JsonNode content = response.path("content");
            if (content.isObject() && content.size() > 0) {
                if (isBinaryDownload(path, method, code)) {
                    assertThat(
                            content.has("application/octet-stream")
                                    || content.has("image/jpeg")
                                    || content.has("image/png")
                                    || content.has("image/webp")
                    ).as("response media type should be binary: %s :: %s", opLabel, code).isTrue();
                } else {
                    assertThat(content.has("application/json"))
                            .as("response media type should include application/json: %s :: %s", opLabel, code)
                            .isTrue();

                    assertThat(content.path("application/json").path("example").isMissingNode())
                            .as("response example missing: %s :: %s", opLabel, code)
                            .isFalse();
                }
            }
        }
    }

    private void assertSchemas(JsonNode schemas) {
        Iterator<Map.Entry<String, JsonNode>> schemaIterator = schemas.fields();

        while (schemaIterator.hasNext()) {
            Map.Entry<String, JsonNode> schemaEntry = schemaIterator.next();
            String schemaName = schemaEntry.getKey();
            JsonNode schema = schemaEntry.getValue();

            if (schema.path("$ref").isTextual()) {
                continue;
            }

            assertThat(schema.path("description").asText("").trim())
                    .as("schema description missing: %s", schemaName)
                    .isNotBlank();

            JsonNode properties = schema.path("properties");
            if (!properties.isObject()) {
                continue;
            }

            Iterator<Map.Entry<String, JsonNode>> propertyIterator = properties.fields();
            while (propertyIterator.hasNext()) {
                Map.Entry<String, JsonNode> propertyEntry = propertyIterator.next();
                String propertyName = propertyEntry.getKey();
                JsonNode property = propertyEntry.getValue();

                String description = property.path("description").asText("").trim();
                assertThat(description)
                        .as("property description missing: %s.%s", schemaName, propertyName)
                        .isNotBlank();

                assertThat(description)
                        .as("generic property description detected: %s.%s", schemaName, propertyName)
                        .doesNotEndWith("필드입니다.");

                if (!isSensitive(propertyName)) {
                    assertThat(property.path("example").isMissingNode())
                            .as("property example missing: %s.%s", schemaName, propertyName)
                            .isFalse();
                }

                JsonNode exampleNode = property.path("example");
                assertExampleTypeAligned(schemaName, propertyName, property, exampleNode);

                if (exampleNode.isTextual()) {
                    String example = exampleNode.asText("");
                    assertThat(example)
                            .as("placeholder example should not be used: %s.%s", schemaName, propertyName)
                            .isNotEqualToIgnoringCase("sample");
                }

                JsonNode formatNode = property.path("format");
                if ("date-time".equals(formatNode.asText())) {
                    String example = property.path("example").asText("");
                    assertThat(example)
                            .as("date-time example should be UTC (Z suffix): %s.%s", schemaName, propertyName)
                            .endsWith("Z");
                }

                JsonNode enumNode = property.path("enum");
                if (enumNode.isArray() && enumNode.size() > 0) {
                    assertThat(description)
                            .as("enum guide missing: %s.%s", schemaName, propertyName)
                            .contains("가능 값");
                }

                if (isEnumLike(propertyName) && "string".equals(property.path("type").asText(""))) {
                    assertThat(description)
                            .as("enum-like value guide missing: %s.%s", schemaName, propertyName)
                            .contains("가능 값");
                }
            }
        }
    }

    private void assertExampleTypeAligned(String schemaName, String propertyName, JsonNode property, JsonNode exampleNode) {
        if (exampleNode.isMissingNode() || exampleNode.isNull()) {
            return;
        }

        String type = property.path("type").asText("");
        switch (type) {
            case "boolean" -> assertThat(exampleNode.isBoolean())
                    .as("boolean example type mismatch: %s.%s", schemaName, propertyName)
                    .isTrue();
            case "integer" -> assertThat(exampleNode.isIntegralNumber())
                    .as("integer example type mismatch: %s.%s", schemaName, propertyName)
                    .isTrue();
            case "number" -> assertThat(exampleNode.isNumber())
                    .as("number example type mismatch: %s.%s", schemaName, propertyName)
                    .isTrue();
            case "string" -> assertThat(exampleNode.isTextual())
                    .as("string example type mismatch: %s.%s", schemaName, propertyName)
                    .isTrue();
            case "array" -> assertThat(exampleNode.isArray())
                    .as("array example type mismatch: %s.%s", schemaName, propertyName)
                    .isTrue();
            case "object" -> assertThat(exampleNode.isObject())
                    .as("object example type mismatch: %s.%s", schemaName, propertyName)
                    .isTrue();
            default -> {
            }
        }
    }

    private boolean isBinaryUpload(String path, String method) {
        return "put".equals(method) && "/api/v1/images/presigned-upload/{token}".equals(path);
    }

    private boolean isBinaryDownload(String path, String method, String code) {
        return "get".equals(method)
                && "200".equals(code)
                && "/api/v1/images/local".equals(path);
    }

    private boolean isEnumLike(String key) {
        String lower = key.toLowerCase();
        return lower.endsWith("type")
                || lower.endsWith("status")
                || lower.endsWith("action")
                || lower.endsWith("messagetype");
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        for (String sensitive : SENSITIVE_KEYS) {
            if (lower.contains(sensitive.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
