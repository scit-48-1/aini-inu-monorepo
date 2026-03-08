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
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private WalkThreadRepository walkThreadRepository;

    @Autowired
    private WalkThreadApplicationRepository walkThreadApplicationRepository;

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

        WalkThread thread = createCompletedThread(owner.getId());

        WalkDiaryCreateRequest createRequest = new WalkDiaryCreateRequest();
        createRequest.setThreadId(thread.getId());
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

        WalkThread thread = createCompletedThread(owner.getId());

        WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
        request.setThreadId(thread.getId());
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

    @Test
    @DisplayName("미완료 스레드로 일기 생성 시도 → 400 에러코드 검증")
    void createDiary_threadNotCompleted_fail() throws Exception {
        Member owner = memberRepository.save(Member.builder()
                .email("diary-owner4@test.com")
                .nickname("owner4")
                .memberType(MemberType.PET_OWNER)
                .build());
        String ownerToken = jwtTokenProvider.generateAccessToken(owner.getId());

        WalkThread recruitingThread = walkThreadRepository.save(WalkThread.builder()
                .authorId(owner.getId())
                .title("모집중 스레드")
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

        WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
        request.setThreadId(recruitingThread.getId());
        request.setTitle("일기 제목");
        request.setContent("일기 내용");
        request.setWalkDate(LocalDate.now());
        request.setIsPublic(true);

        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WD400_THREAD_NOT_COMPLETED"));
    }

    @Test
    @DisplayName("비참여자가 일기 생성 시도 → 403 에러코드 검증")
    void createDiary_notParticipant_fail() throws Exception {
        Member author = memberRepository.save(Member.builder()
                .email("diary-author5@test.com")
                .nickname("author5")
                .memberType(MemberType.PET_OWNER)
                .build());
        Member nonParticipant = memberRepository.save(Member.builder()
                .email("diary-nonpart@test.com")
                .nickname("nonpart")
                .memberType(MemberType.PET_OWNER)
                .build());

        String nonPartToken = jwtTokenProvider.generateAccessToken(nonParticipant.getId());
        WalkThread thread = createCompletedThread(author.getId());

        WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
        request.setThreadId(thread.getId());
        request.setTitle("일기 제목");
        request.setContent("일기 내용");
        request.setWalkDate(LocalDate.now());
        request.setIsPublic(true);

        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + nonPartToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("WD403_NOT_THREAD_PARTICIPANT"));
    }

    @Test
    @DisplayName("GET /walk-diaries/available-threads → 일기 미작성 COMPLETED 스레드만 반환")
    void getAvailableThreads_success() throws Exception {
        Member owner = memberRepository.save(Member.builder()
                .email("diary-owner6@test.com")
                .nickname("owner6")
                .memberType(MemberType.PET_OWNER)
                .build());
        String ownerToken = jwtTokenProvider.generateAccessToken(owner.getId());

        WalkThread completedThread = createCompletedThread(owner.getId());

        // recruiting thread should not appear
        walkThreadRepository.save(WalkThread.builder()
                .authorId(owner.getId())
                .title("모집중")
                .description("설명")
                .walkDate(LocalDate.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("한강")
                .latitude(BigDecimal.valueOf(37.54))
                .longitude(BigDecimal.valueOf(127.04))
                .address("마포구")
                .status(WalkThreadStatus.RECRUITING)
                .build());

        mockMvc.perform(get("/api/v1/walk-diaries/available-threads")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].threadId").value(completedThread.getId()));
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
