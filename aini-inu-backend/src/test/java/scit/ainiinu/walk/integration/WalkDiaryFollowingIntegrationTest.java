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
import scit.ainiinu.member.entity.MemberFollow;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberFollowRepository;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.testsupport.IntegrationTestProfile;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:walkdiary-following-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class WalkDiaryFollowingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberFollowRepository memberFollowRepository;

    @Autowired
    private WalkThreadRepository walkThreadRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("팔로잉 피드는 비공개를 제외하고 hasNext 기반 배치 조회를 지원한다")
    void followingFeed_privateExcludedAndBatchLoading_success() throws Exception {
        // given
        Member viewer = memberRepository.save(Member.builder()
                .email("viewer2@test.com")
                .nickname("viewer2")
                .memberType(MemberType.PET_OWNER)
                .build());
        Member author = memberRepository.save(Member.builder()
                .email("author2@test.com")
                .nickname("author2")
                .memberType(MemberType.PET_OWNER)
                .build());

        memberFollowRepository.save(MemberFollow.builder()
                .followerId(viewer.getId())
                .followingId(author.getId())
                .build());

        String viewerToken = jwtTokenProvider.generateAccessToken(viewer.getId());
        String authorToken = jwtTokenProvider.generateAccessToken(author.getId());

        for (int i = 0; i < 21; i++) {
            WalkThread thread = createCompletedThread(author.getId());

            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(thread.getId());
            request.setTitle("공개 일기 " + i);
            request.setContent("내용 " + i);
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of());
            request.setIsPublic(true);

            mockMvc.perform(post("/api/v1/walk-diaries")
                            .with(csrf())
                            .header("Authorization", "Bearer " + authorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        WalkThread privateThread = createCompletedThread(author.getId());

        WalkDiaryCreateRequest privateRequest = new WalkDiaryCreateRequest();
        privateRequest.setThreadId(privateThread.getId());
        privateRequest.setTitle("비공개 일기");
        privateRequest.setContent("비공개");
        privateRequest.setWalkDate(LocalDate.now());
        privateRequest.setPhotoUrls(List.of());
        privateRequest.setIsPublic(false);

        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privateRequest)))
                .andExpect(status().isOk());

        // when & then - first page
        mockMvc.perform(get("/api/v1/walk-diaries/following")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        // second page
        mockMvc.perform(get("/api/v1/walk-diaries/following")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    private WalkThread createCompletedThread(Long authorId) {
        WalkThread thread = walkThreadRepository.save(WalkThread.builder()
                .authorId(authorId)
                .title("완료 스레드")
                .description("설명")
                .walkDate(LocalDate.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("서울숲")
                .latitude(BigDecimal.valueOf(37.54))
                .longitude(BigDecimal.valueOf(127.04))
                .address("성동구")
                .status(WalkThreadStatus.RECRUITING)
                .build());
        thread.complete();
        return walkThreadRepository.save(thread);
    }
}
