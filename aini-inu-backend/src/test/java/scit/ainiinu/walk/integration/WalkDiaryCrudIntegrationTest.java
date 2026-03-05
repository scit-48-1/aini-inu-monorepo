package scit.ainiinu.walk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.testsupport.IntegrationTestProfile;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:walkdiary-crud-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class WalkDiaryCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("일기 CRUD 전체 흐름과 비작성자 수정 거절이 동작한다")
    void diaryCrudAndOwnerGuard_success() throws Exception {
        // given
        Member owner = memberRepository.save(Member.builder()
                .email("diary-owner2@test.com")
                .nickname("owner2")
                .memberType(MemberType.PET_OWNER)
                .build());
        Member other = memberRepository.save(Member.builder()
                .email("diary-other@test.com")
                .nickname("other")
                .memberType(MemberType.PET_OWNER)
                .build());

        String ownerToken = jwtTokenProvider.generateAccessToken(owner.getId());
        String otherToken = jwtTokenProvider.generateAccessToken(other.getId());

        WalkDiaryCreateRequest createRequest = new WalkDiaryCreateRequest();
        createRequest.setTitle("CRUD 통합 테스트");
        createRequest.setContent("본문");
        createRequest.setWalkDate(LocalDate.now());
        createRequest.setPhotoUrls(List.of());
        createRequest.setIsPublic(true);

        String createdBody = mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long diaryId = objectMapper.readTree(createdBody).path("data").path("id").asLong();

        // when & then - non owner patch reject
        mockMvc.perform(patch("/api/v1/walk-diaries/{diaryId}", diaryId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"타인 수정\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("WD403_DIARY_OWNER_ONLY"));

        // when & then - owner patch
        mockMvc.perform(patch("/api/v1/walk-diaries/{diaryId}", diaryId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"작성자 수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("작성자 수정"));

        // when & then - owner delete
        mockMvc.perform(delete("/api/v1/walk-diaries/{diaryId}", diaryId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/walk-diaries")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("일기 본문이 300자를 초과하면 생성 요청은 400으로 거절된다")
    void createDiary_contentTooLong_fail() throws Exception {
        Member owner = memberRepository.save(Member.builder()
                .email("diary-owner3@test.com")
                .nickname("owner3")
                .memberType(MemberType.PET_OWNER)
                .build());
        String ownerToken = jwtTokenProvider.generateAccessToken(owner.getId());

        WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
        request.setTitle("본문 길이 검증");
        request.setContent("a".repeat(301));
        request.setWalkDate(LocalDate.now());
        request.setPhotoUrls(List.of());
        request.setIsPublic(true);

        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("C002"));
    }
}
