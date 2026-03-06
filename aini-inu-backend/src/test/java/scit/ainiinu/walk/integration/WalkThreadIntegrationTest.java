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
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.testsupport.IntegrationTestProfile;
import scit.ainiinu.walk.dto.request.ThreadApplyRequest;
import scit.ainiinu.walk.dto.request.ThreadCreateRequest;
import scit.ainiinu.walk.dto.request.ThreadPatchRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:walkthread-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class WalkThreadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Test
    @DisplayName("스레드 생성 후 목록 조회까지 통합 흐름이 동작한다")
    void createAndListThread_success() throws Exception {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("thread-owner@test.com")
                .nickname("throwner1")
                .memberType(MemberType.PET_OWNER)
                .build());

        String token = jwtTokenProvider.generateAccessToken(member.getId());

        ThreadCreateRequest request = new ThreadCreateRequest();
        request.setTitle("한강 산책 모집");
        request.setDescription("저녁 산책 함께해요");
        request.setWalkDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        request.setChatType("GROUP");
        request.setMaxParticipants(5);
        request.setAllowNonPetOwner(true);
        request.setIsVisibleAlways(true);
        ThreadCreateRequest.LocationRequest location = new ThreadCreateRequest.LocationRequest();
        location.setPlaceName("서울숲");
        location.setLatitude(37.54);
        location.setLongitude(127.04);
        location.setAddress("성동구");
        request.setLocation(location);
        request.setPetIds(List.of(1L));

        // when & then
        mockMvc.perform(post("/api/v1/threads")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/v1/threads")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("스레드 신청 시 실제 chat_room이 생성되고 채팅 상세 조회가 가능하다")
    void applyThread_createsRealChatRoom_andCanReadChatDetail() throws Exception {
        Member author = memberRepository.save(Member.builder()
                .email("thread-author@test.com")
                .nickname("thrauthor")
                .memberType(MemberType.PET_OWNER)
                .build());
        Member applicant = memberRepository.save(Member.builder()
                .email("thread-applicant@test.com")
                .nickname("thrapply")
                .memberType(MemberType.PET_OWNER)
                .build());

        String authorToken = jwtTokenProvider.generateAccessToken(author.getId());
        String applicantToken = jwtTokenProvider.generateAccessToken(applicant.getId());

        ThreadCreateRequest createRequest = new ThreadCreateRequest();
        createRequest.setTitle("공원 산책 모집");
        createRequest.setDescription("같이 걸어요");
        createRequest.setWalkDate(LocalDate.now().plusDays(1));
        createRequest.setStartTime(LocalDateTime.now().plusDays(1));
        createRequest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        createRequest.setChatType("GROUP");
        createRequest.setMaxParticipants(4);
        createRequest.setAllowNonPetOwner(true);
        createRequest.setIsVisibleAlways(true);
        ThreadCreateRequest.LocationRequest location = new ThreadCreateRequest.LocationRequest();
        location.setPlaceName("올림픽공원");
        location.setLatitude(37.52);
        location.setLongitude(127.12);
        location.setAddress("송파구");
        createRequest.setLocation(location);
        createRequest.setPetIds(List.of(1L));

        String createBody = mockMvc.perform(post("/api/v1/threads")
                        .with(csrf())
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long threadId = objectMapper.readTree(createBody).path("data").path("id").asLong();

        ThreadApplyRequest applyRequest = new ThreadApplyRequest();
        applyRequest.setPetIds(List.of(2L));
        String applyBody = mockMvc.perform(post("/api/v1/threads/{threadId}/apply", threadId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationStatus").value("JOINED"))
                .andExpect(jsonPath("$.data.chatRoomId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long chatRoomId = objectMapper.readTree(applyBody).path("data").path("chatRoomId").asLong();

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", chatRoomId)
                        .header("Authorization", "Bearer " + applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatRoomId").value(chatRoomId))
                .andExpect(jsonPath("$.data.chatType").value("GROUP"));

        chatRoomRepository.findById(chatRoomId).ifPresentOrElse(room -> {
            assertThat(room.getThreadId()).isEqualTo(threadId);
        }, () -> {
            throw new AssertionError("chat_room should exist for thread apply");
        });

        Set<Long> activeMemberIds = chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(chatRoomId).stream()
                .map(participant -> participant.getMemberId())
                .collect(Collectors.toSet());
        assertThat(activeMemberIds)
                .containsExactlyInAnyOrder(author.getId(), applicant.getId());
    }

    @Test
    @DisplayName("스레드 수정 시 제목/날짜/채팅유형과 petIds를 함께 변경하면 모든 필드가 DB에 반영된다")
    void updateThread_fieldsAndPets_allPersistedAfterClear() throws Exception {
        // given — create a thread first
        Member member = memberRepository.save(Member.builder()
                .email("patch-owner@test.com")
                .nickname("patchowner")
                .memberType(MemberType.PET_OWNER)
                .build());

        String token = jwtTokenProvider.generateAccessToken(member.getId());

        ThreadCreateRequest createReq = new ThreadCreateRequest();
        createReq.setTitle("원래 제목");
        createReq.setDescription("원래 설명");
        createReq.setWalkDate(LocalDate.now().plusDays(1));
        createReq.setStartTime(LocalDateTime.now().plusDays(1));
        createReq.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        createReq.setChatType("GROUP");
        createReq.setMaxParticipants(5);
        createReq.setAllowNonPetOwner(true);
        createReq.setIsVisibleAlways(true);
        ThreadCreateRequest.LocationRequest location = new ThreadCreateRequest.LocationRequest();
        location.setPlaceName("서울숲");
        location.setLatitude(37.54);
        location.setLongitude(127.04);
        location.setAddress("성동구");
        createReq.setLocation(location);
        createReq.setPetIds(List.of(1L));

        String createBody = mockMvc.perform(post("/api/v1/threads")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long threadId = objectMapper.readTree(createBody).path("data").path("id").asLong();

        // when — PATCH with title, walkDate, chatType AND petIds changed
        ThreadPatchRequest patchReq = new ThreadPatchRequest();
        patchReq.setTitle("수정된 제목");
        patchReq.setDescription("수정된 설명");
        patchReq.setWalkDate(LocalDate.now().plusDays(3));
        patchReq.setStartTime(LocalDateTime.now().plusDays(3));
        patchReq.setEndTime(LocalDateTime.now().plusDays(3).plusHours(2));
        patchReq.setChatType("INDIVIDUAL");
        patchReq.setMaxParticipants(2);
        patchReq.setPetIds(List.of(2L));

        mockMvc.perform(patch("/api/v1/threads/{threadId}", threadId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));

        // then — re-GET must reflect ALL changes (not just pet changes)
        mockMvc.perform(get("/api/v1/threads/{threadId}", threadId)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.description").value("수정된 설명"))
                .andExpect(jsonPath("$.data.chatType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.data.maxParticipants").value(2))
                .andExpect(jsonPath("$.data.walkDate").value(LocalDate.now().plusDays(3).toString()))
                .andExpect(jsonPath("$.data.petIds[0]").value(2));
    }

    @Test
    @DisplayName("스레드 수정 시 petIds 없이 제목만 변경해도 DB에 반영된다")
    void updateThread_fieldsOnlyWithoutPets_persisted() throws Exception {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("patch-nopet@test.com")
                .nickname("patchnopet")
                .memberType(MemberType.PET_OWNER)
                .build());

        String token = jwtTokenProvider.generateAccessToken(member.getId());

        ThreadCreateRequest createReq = new ThreadCreateRequest();
        createReq.setTitle("기존 제목");
        createReq.setDescription("기존 설명");
        createReq.setWalkDate(LocalDate.now().plusDays(1));
        createReq.setStartTime(LocalDateTime.now().plusDays(1));
        createReq.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        createReq.setChatType("GROUP");
        createReq.setMaxParticipants(5);
        createReq.setAllowNonPetOwner(true);
        createReq.setIsVisibleAlways(true);
        ThreadCreateRequest.LocationRequest location = new ThreadCreateRequest.LocationRequest();
        location.setPlaceName("올림픽공원");
        location.setLatitude(37.52);
        location.setLongitude(127.12);
        location.setAddress("송파구");
        createReq.setLocation(location);
        createReq.setPetIds(List.of(1L));

        String createBody = mockMvc.perform(post("/api/v1/threads")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long threadId = objectMapper.readTree(createBody).path("data").path("id").asLong();

        // when — PATCH only title (no petIds)
        ThreadPatchRequest patchReq = new ThreadPatchRequest();
        patchReq.setTitle("제목만 변경");

        mockMvc.perform(patch("/api/v1/threads/{threadId}", threadId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk());

        // then — re-GET must reflect title change
        mockMvc.perform(get("/api/v1/threads/{threadId}", threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목만 변경"))
                .andExpect(jsonPath("$.data.description").value("기존 설명"));
    }
}
