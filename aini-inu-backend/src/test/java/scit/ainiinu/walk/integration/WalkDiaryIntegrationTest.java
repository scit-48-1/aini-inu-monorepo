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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:walkdiary-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class WalkDiaryIntegrationTest {

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
    @DisplayName("일기 생성/수정/목록 조회 통합 흐름이 동작한다")
    void createPatchListDiary_success() throws Exception {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("diary-owner@test.com")
                .nickname("diaryowner")
                .memberType(MemberType.PET_OWNER)
                .build());
        String token = jwtTokenProvider.generateAccessToken(member.getId());

        WalkThread thread = createCompletedThread(member.getId());

        WalkDiaryCreateRequest createRequest = new WalkDiaryCreateRequest();
        createRequest.setThreadId(thread.getId());
        createRequest.setTitle("한강 일기");
        createRequest.setContent("좋은 하루");
        createRequest.setWalkDate(LocalDate.now());
        createRequest.setPhotoUrls(List.of("https://cdn/1.jpg"));

        // when & then - create
        String responseBody = mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long diaryId = objectMapper.readTree(responseBody).path("data").path("id").asLong();

        // patch
        mockMvc.perform(patch("/api/v1/walk-diaries/{diaryId}", diaryId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 내용\",\"isPublic\":false}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));

        // list
        mockMvc.perform(get("/api/v1/walk-diaries")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("팔로잉 피드는 공개 일기만 노출한다")
    void followingFeed_publicOnly_success() throws Exception {
        // given
        Member viewer = memberRepository.save(Member.builder()
                .email("viewer@test.com")
                .nickname("viewer")
                .memberType(MemberType.PET_OWNER)
                .build());
        Member author = memberRepository.save(Member.builder()
                .email("author@test.com")
                .nickname("author")
                .memberType(MemberType.PET_OWNER)
                .build());

        memberFollowRepository.save(MemberFollow.builder()
                .followerId(viewer.getId())
                .followingId(author.getId())
                .build());

        String viewerToken = jwtTokenProvider.generateAccessToken(viewer.getId());
        String authorToken = jwtTokenProvider.generateAccessToken(author.getId());

        WalkThread thread1 = createCompletedThread(author.getId());
        WalkThread thread2 = createCompletedThread(author.getId());

        WalkDiaryCreateRequest publicDiary = new WalkDiaryCreateRequest();
        publicDiary.setThreadId(thread1.getId());
        publicDiary.setTitle("공개 일기");
        publicDiary.setContent("공개 내용");
        publicDiary.setWalkDate(LocalDate.now());
        publicDiary.setPhotoUrls(List.of());
        publicDiary.setIsPublic(true);

        WalkDiaryCreateRequest privateDiary = new WalkDiaryCreateRequest();
        privateDiary.setThreadId(thread2.getId());
        privateDiary.setTitle("비공개 일기");
        privateDiary.setContent("비공개 내용");
        privateDiary.setWalkDate(LocalDate.now());
        privateDiary.setPhotoUrls(List.of());
        privateDiary.setIsPublic(false);

        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publicDiary)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privateDiary)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/api/v1/walk-diaries/following")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("공개 일기"));
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
