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

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@IntegrationTestProfile
class OpenApiRequestSchemaContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI 요청 스키마 필수/문자열 제약 계약이 정합해야 한다")
    void openApiRequestSchemaContractAligned() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        JsonNode schemas = root.path("components").path("schemas");
        assertThat(schemas.isObject()).isTrue();

        assertRequired(schemas, "AuthLoginRequest", "email", "password");
        assertRequired(schemas, "TokenRefreshRequest", "refreshToken");
        assertRequired(schemas, "TokenRevokeRequest", "refreshToken");
        assertRequired(schemas, "MemberSignupRequest", "email", "password", "nickname");
        assertRequired(schemas, "MemberCreateRequest", "nickname");
        assertRequired(schemas, "PetCreateRequest", "name");
        assertRequired(schemas, "ThreadCreateRequest", "title", "description", "chatType", "petIds");
        assertRequired(schemas, "LocationRequest", "placeName");
        assertRequired(schemas, "ThreadFilterRequest", "type");
        assertRequired(schemas, "ChatMessageCreateRequest", "content");
        assertRequired(schemas, "CommentCreateRequest", "content");
        assertRequired(schemas, "PresignedImageRequest", "purpose", "fileName", "contentType");
        assertRequired(schemas, "LostPetCreateRequest", "petName", "photoUrl", "lastSeenLocation");
        assertRequired(schemas, "SightingCreateRequest", "photoUrl", "foundLocation");
        assertRequired(schemas, "WalkDiaryCreateRequest", "title", "content");

        assertMinLength(schemas, "ThreadCreateRequest", "title", 1);
        assertMinLength(schemas, "ThreadCreateRequest", "description", 1);
        assertMinLength(schemas, "PetCreateRequest", "name", 1);
        assertMinLength(schemas, "ChatMessageCreateRequest", "content", 1);
        assertMinLength(schemas, "CommentCreateRequest", "content", 1);
        assertMinLength(schemas, "WalkDiaryCreateRequest", "title", 1);
        assertMinLength(schemas, "WalkDiaryCreateRequest", "content", 1);
        assertMaxLength(schemas, "WalkDiaryCreateRequest", "content", 300);
        assertMaxLength(schemas, "WalkDiaryPatchRequest", "content", 300);

        // Optional request body endpoint should not be forced required at schema level.
        assertNotRequired(schemas, "WalkConfirmRequest", "action");
    }

    private void assertRequired(JsonNode schemas, String schemaName, String... fieldNames) {
        Set<String> required = requiredFields(schema(schemas, schemaName));
        for (String fieldName : fieldNames) {
            assertThat(required)
                    .as("required field missing: %s.%s", schemaName, fieldName)
                    .contains(fieldName);
        }
    }

    private void assertNotRequired(JsonNode schemas, String schemaName, String fieldName) {
        Set<String> required = requiredFields(schema(schemas, schemaName));
        assertThat(required)
                .as("field should not be required: %s.%s", schemaName, fieldName)
                .doesNotContain(fieldName);
    }

    private void assertMinLength(JsonNode schemas, String schemaName, String fieldName, int expected) {
        JsonNode fieldSchema = schema(schemas, schemaName).path("properties").path(fieldName);
        assertThat(fieldSchema.isObject())
                .as("field schema missing: %s.%s", schemaName, fieldName)
                .isTrue();
        assertThat(fieldSchema.path("minLength").asInt(-1))
                .as("minLength mismatch: %s.%s", schemaName, fieldName)
                .isEqualTo(expected);
    }

    private void assertMaxLength(JsonNode schemas, String schemaName, String fieldName, int expected) {
        JsonNode fieldSchema = schema(schemas, schemaName).path("properties").path(fieldName);
        assertThat(fieldSchema.isObject())
                .as("field schema missing: %s.%s", schemaName, fieldName)
                .isTrue();
        assertThat(fieldSchema.path("maxLength").asInt(-1))
                .as("maxLength mismatch: %s.%s", schemaName, fieldName)
                .isEqualTo(expected);
    }

    private JsonNode schema(JsonNode schemas, String schemaName) {
        JsonNode schema = schemas.path(schemaName);
        assertThat(schema.isObject())
                .as("schema not found: %s", schemaName)
                .isTrue();
        return schema;
    }

    private Set<String> requiredFields(JsonNode schema) {
        JsonNode requiredNode = schema.path("required");
        Set<String> required = new HashSet<>();
        if (!requiredNode.isArray()) {
            return required;
        }
        for (JsonNode item : requiredNode) {
            required.add(item.asText());
        }
        return required;
    }
}
