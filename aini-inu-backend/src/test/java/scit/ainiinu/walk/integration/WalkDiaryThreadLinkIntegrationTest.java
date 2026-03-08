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
        "spring.datasource.url=jdbc:h2:mem:walkdiary-thread-link-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class WalkDiaryThreadLinkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WalkThreadRepository walkThreadRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("COMPLETED 스레드로 일기 생성 후, 스레드 삭제 시 linkedThreadStatus가 DELETED로 전환된다")
    void threadLinkLifecycle_success() throws Exception {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("thread-link-owner@test.com")
                .nickname("threadlink")
                .memberType(MemberType.PET_OWNER)
                .build());
        String token = jwtTokenProvider.generateAccessToken(member.getId());

        WalkThread thread = createCompletedThread(member.getId());

        WalkDiaryCreateRequest linkedRequest = new WalkDiaryCreateRequest();
        linkedRequest.setThreadId(thread.getId());
        linkedRequest.setTitle("연결 일기");
        linkedRequest.setContent("본문");
        linkedRequest.setWalkDate(LocalDate.now());
        linkedRequest.setPhotoUrls(List.of());
        linkedRequest.setIsPublic(true);

        String linkedBody = mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.threadId").value(thread.getId()))
                .andExpect(jsonPath("$.data.linkedThreadStatus").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long linkedDiaryId = objectMapper.readTree(linkedBody).path("data").path("id").asLong();

        thread.markDeleted();

        // when & then
        mockMvc.perform(get("/api/v1/walk-diaries/{diaryId}", linkedDiaryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedThreadStatus").value("DELETED"));
    }

    @Test
    @DisplayName("미완료 스레드로 일기 생성 시 400을 반환한다")
    void createDiary_recruitingThread_fail() throws Exception {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("thread-link-fail@test.com")
                .nickname("linkfail")
                .memberType(MemberType.PET_OWNER)
                .build());
        String token = jwtTokenProvider.generateAccessToken(member.getId());

        WalkThread recruitingThread = walkThreadRepository.save(WalkThread.builder()
                .authorId(member.getId())
                .title("스레드")
                .description("설명")
                .walkDate(LocalDate.now().plusDays(1))
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
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
        request.setTitle("미완료 연결 일기");
        request.setContent("본문");
        request.setWalkDate(LocalDate.now());
        request.setPhotoUrls(List.of());
        request.setIsPublic(true);

        // when & then
        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WD400_THREAD_NOT_COMPLETED"));
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
