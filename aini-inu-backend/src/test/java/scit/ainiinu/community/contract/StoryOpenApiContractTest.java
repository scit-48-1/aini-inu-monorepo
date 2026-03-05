package scit.ainiinu.community.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.testsupport.IntegrationTestProfile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@IntegrationTestProfile
class StoryOpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger 문서에 스토리 그룹 응답 스키마가 노출된다")
    void storyOpenApiSchemaExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/stories'].get.summary").value("스토리 목록 조회"))
                .andExpect(jsonPath("$.components.schemas.StoryGroupResponse.properties.memberId").exists())
                .andExpect(jsonPath("$.components.schemas.StoryGroupResponse.properties.diaries").exists())
                .andExpect(jsonPath("$.components.schemas.StoryDiaryItemResponse.properties.diaryId").exists())
                .andExpect(jsonPath("$.components.schemas.StoryDiaryItemResponse.properties.createdAt").exists())
                .andExpect(jsonPath("$.components.schemas.StoryDiaryItemResponse.properties.content.maxLength").value(300))
                .andExpect(jsonPath("$.components.schemas.WalkDiaryResponse.properties.content.maxLength").value(300));
    }
}
