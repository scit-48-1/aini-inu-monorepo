package scit.ainiinu.community.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.MemberFollow;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberFollowRepository;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.entity.enums.PetGender;
import scit.ainiinu.pet.entity.enums.PetSize;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.testsupport.IntegrationTestProfile;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadPet;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadPetRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class StoryListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberFollowRepository memberFollowRepository;

    @Autowired
    private WalkDiaryRepository walkDiaryRepository;

    @Autowired
    private WalkThreadRepository walkThreadRepository;

    @Autowired
    private WalkThreadPetRepository walkThreadPetRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("스토리는 팔로잉한 회원 기준으로 그룹화되고 그룹 내부는 최신 diary 순으로 내려온다")
    void getStoriesGroupedByAuthor() throws Exception {
        Member me = memberRepository.save(Member.builder()
                .email("viewer@example.com")
                .nickname("viewer1")
                .memberType(MemberType.PET_OWNER)
                .build());

        Member author = memberRepository.save(Member.builder()
                .email("author@example.com")
                .nickname("author1")
                .memberType(MemberType.PET_OWNER)
                .build());

        Member author2 = memberRepository.save(Member.builder()
                .email("author2@example.com")
                .nickname("author2")
                .memberType(MemberType.PET_OWNER)
                .build());

        memberFollowRepository.save(MemberFollow.builder()
                .followerId(me.getId())
                .followingId(author.getId())
                .build());

        memberFollowRepository.save(MemberFollow.builder()
                .followerId(me.getId())
                .followingId(author2.getId())
                .build());

        WalkDiary authorOldDiary = walkDiaryRepository.saveAndFlush(
                WalkDiary.create(author.getId(), null, "오래된 일기", "본문", List.of("https://cdn.example.com/old.jpg"), LocalDate.now(), true)
        );
        WalkDiary authorLatestDiary = walkDiaryRepository.saveAndFlush(
                WalkDiary.create(author.getId(), null, "최신 일기", "본문", List.of("https://cdn.example.com/latest.jpg"), LocalDate.now(), true)
        );
        WalkDiary authorPrivateDiary = walkDiaryRepository.saveAndFlush(
                WalkDiary.create(author.getId(), null, "비공개 일기", "본문", List.of("https://cdn.example.com/private.jpg"), LocalDate.now(), false)
        );
        WalkDiary author2Diary = walkDiaryRepository.saveAndFlush(
                WalkDiary.create(author2.getId(), null, "author2 일기", "본문", List.of("https://cdn.example.com/a2.jpg"), LocalDate.now(), true)
        );

        ReflectionTestUtils.setField(authorOldDiary, "createdAt", LocalDateTime.now().minusHours(2));
        ReflectionTestUtils.setField(authorLatestDiary, "createdAt", LocalDateTime.now().minusMinutes(5));
        ReflectionTestUtils.setField(authorPrivateDiary, "createdAt", LocalDateTime.now().minusMinutes(3));
        ReflectionTestUtils.setField(author2Diary, "createdAt", LocalDateTime.now().minusHours(1));

        String accessToken = jwtTokenProvider.generateAccessToken(me.getId());

        MvcResult result = mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("content");

        JsonNode authorGroup = findGroupByMemberId(content, author.getId());
        assertThat(authorGroup).isNotNull();
        assertThat(authorGroup.path("diaries").size()).isEqualTo(2);
        assertThat(authorGroup.path("diaries").get(0).path("title").asText()).isEqualTo("최신 일기");
        assertThat(authorGroup.path("diaries").get(1).path("title").asText()).isEqualTo("오래된 일기");

        JsonNode author2Group = findGroupByMemberId(content, author2.getId());
        assertThat(author2Group).isNotNull();
        assertThat(author2Group.path("diaries").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("산책일기 비공개/삭제는 스토리 조회에 즉시 반영된다")
    void storyReflectsDiaryVisibilityAndDeletion() throws Exception {
        Member me = memberRepository.save(Member.builder()
                .email("viewer2@example.com")
                .nickname("viewer2")
                .memberType(MemberType.PET_OWNER)
                .build());

        Member author = memberRepository.save(Member.builder()
                .email("author3@example.com")
                .nickname("author3")
                .memberType(MemberType.PET_OWNER)
                .build());

        memberFollowRepository.save(MemberFollow.builder()
                .followerId(me.getId())
                .followingId(author.getId())
                .build());

        WalkDiary diary = walkDiaryRepository.saveAndFlush(
                WalkDiary.create(author.getId(), null, "스토리 대상 일기", "본문", List.of("https://cdn.example.com/visible.jpg"), LocalDate.now(), true)
        );
        ReflectionTestUtils.setField(diary, "createdAt", LocalDateTime.now().minusMinutes(10));

        String accessToken = jwtTokenProvider.generateAccessToken(me.getId());

        mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        diary.update(null, null, null, null, null, false);
        walkDiaryRepository.saveAndFlush(diary);

        mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        diary.update(null, null, null, null, null, true);
        walkDiaryRepository.saveAndFlush(diary);

        mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        diary.softDelete(LocalDateTime.now());
        walkDiaryRepository.saveAndFlush(diary);

        mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("스토리 목록에서 diary 아이템에 threadId가 포함된다")
    void getStories_threadIdIncluded() throws Exception {
        Member me = memberRepository.save(Member.builder()
                .email("thread-viewer@test.com")
                .nickname("thrdview")
                .memberType(MemberType.PET_OWNER)
                .build());

        Member author = memberRepository.save(Member.builder()
                .email("thread-author@test.com")
                .nickname("thrdauthr")
                .memberType(MemberType.PET_OWNER)
                .build());

        memberFollowRepository.save(MemberFollow.builder()
                .followerId(me.getId())
                .followingId(author.getId())
                .build());

        WalkThread thread = createCompletedThread(author.getId());

        WalkDiary diary = walkDiaryRepository.saveAndFlush(
                WalkDiary.create(author.getId(), thread.getId(), "스레드 일기", "본문",
                        List.of("https://cdn/1.jpg"), LocalDate.now(), true));
        ReflectionTestUtils.setField(diary, "createdAt", LocalDateTime.now().minusMinutes(5));

        String accessToken = jwtTokenProvider.generateAccessToken(me.getId());

        mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].diaries[0].threadId").value(thread.getId()));
    }

    @Test
    @DisplayName("스토리 목록에서 diary 아이템에 스레드 정보(위치, 반려견)가 포함된다")
    void getStories_threadInfoIncluded() throws Exception {
        Member me = memberRepository.save(Member.builder()
                .email("info-viewer@test.com")
                .nickname("infoview")
                .memberType(MemberType.PET_OWNER)
                .build());

        Member author = memberRepository.save(Member.builder()
                .email("info-author@test.com")
                .nickname("infoauthr")
                .memberType(MemberType.PET_OWNER)
                .build());

        memberFollowRepository.save(MemberFollow.builder()
                .followerId(me.getId())
                .followingId(author.getId())
                .build());

        Pet pet = petRepository.save(Pet.builder()
                .memberId(author.getId())
                .name("보리")
                .age(2)
                .gender(PetGender.FEMALE)
                .size(PetSize.SMALL)
                .isNeutered(false)
                .isMain(true)
                .photoUrl("https://cdn/bori.jpg")
                .build());

        WalkThread thread = createCompletedThread(author.getId());
        walkThreadPetRepository.save(WalkThreadPet.of(thread.getId(), pet.getId()));

        WalkDiary diary = walkDiaryRepository.saveAndFlush(
                WalkDiary.create(author.getId(), thread.getId(), "반려견 일기", "본문",
                        List.of("https://cdn/1.jpg"), LocalDate.now(), true));
        ReflectionTestUtils.setField(diary, "createdAt", LocalDateTime.now().minusMinutes(5));

        String accessToken = jwtTokenProvider.generateAccessToken(me.getId());

        mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].diaries[0].thread.placeName").value("서울숲"))
                .andExpect(jsonPath("$.data.content[0].diaries[0].thread.pets[0].name").value("보리"));
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

    private JsonNode findGroupByMemberId(JsonNode content, Long memberId) {
        for (JsonNode group : content) {
            if (group.path("memberId").asLong() == memberId) {
                return group;
            }
        }
        return null;
    }
}
