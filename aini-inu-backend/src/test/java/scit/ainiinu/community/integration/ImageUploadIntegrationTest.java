package scit.ainiinu.community.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.testsupport.IntegrationTestProfile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
@TestPropertySource(properties = {
        "community.storage.local.base-dir=${java.io.tmpdir}/aini-inu-test-uploads",
        "community.storage.public-base-url=http://localhost:8080"
})
class ImageUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("presigned URL 발급 후 PUT 업로드와 로컬 조회가 성공한다")
    void uploadByPresignedUrl() throws Exception {
        String accessToken = jwtTokenProvider.generateAccessToken(1L);

        MvcResult issueResult = mockMvc.perform(post("/api/v1/images/presigned-url")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "POST",
                                  "fileName": "post.jpg",
                                  "contentType": "image/jpeg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadUrl").exists())
                .andExpect(jsonPath("$.data.imageUrl").exists())
                .andReturn();

        JsonNode issueJson = objectMapper.readTree(issueResult.getResponse().getContentAsString());
        String uploadUrl = issueJson.path("data").path("uploadUrl").asText();
        String imageUrl = issueJson.path("data").path("imageUrl").asText();

        String uploadPath = uploadUrl.replace("http://localhost:8080", "");
        String imagePath = imageUrl.replace("http://localhost:8080", "");

        mockMvc.perform(put(uploadPath)
                        .with(csrf())
                        .contentType(MediaType.IMAGE_JPEG)
                        .content("binary-image-jpeg".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get(imagePath))
                .andExpect(status().isOk())
                .andExpect(content().bytes("binary-image-jpeg".getBytes()));
    }

    @Test
    @DisplayName("presigned 업로드 URL은 1회만 사용할 수 있다")
    void presignedUrlSingleUse() throws Exception {
        String accessToken = jwtTokenProvider.generateAccessToken(1L);

        MvcResult issueResult = mockMvc.perform(post("/api/v1/images/presigned-url")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "POST",
                                  "fileName": "post.jpg",
                                  "contentType": "image/jpeg"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode issueJson = objectMapper.readTree(issueResult.getResponse().getContentAsString());
        String uploadUrl = issueJson.path("data").path("uploadUrl").asText();
        String uploadPath = uploadUrl.replace("http://localhost:8080", "");

        mockMvc.perform(put(uploadPath)
                        .with(csrf())
                        .contentType(MediaType.IMAGE_JPEG)
                        .content("binary-image-jpeg".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put(uploadPath)
                        .with(csrf())
                        .contentType(MediaType.IMAGE_JPEG)
                        .content("binary-image-jpeg-2".getBytes()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CO009"));
    }

    @Test
    @DisplayName("발급 시 허용되지 않은 MIME 타입이면 415를 반환한다")
    void issuePresignedUrlInvalidMime() throws Exception {
        String accessToken = jwtTokenProvider.generateAccessToken(1L);

        mockMvc.perform(post("/api/v1/images/presigned-url")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "POST",
                                  "fileName": "post.gif",
                                  "contentType": "image/gif"
                                }
                                """))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CO006"));
    }
}
