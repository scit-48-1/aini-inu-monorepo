package scit.ainiinu.walk.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.member.entity.MemberFollow;
import scit.ainiinu.member.repository.MemberFollowRepository;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:walkdiary-following;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class WalkDiaryFollowingRepositoryTest {

    @Autowired
    private WalkDiaryRepository walkDiaryRepository;

    @Autowired
    private MemberFollowRepository memberFollowRepository;

    @Test
    @DisplayName("팔로잉 피드는 팔로우한 회원의 공개 일기만 createdAt,id 내림차순으로 반환한다")
    void findFollowingPublicSlice_filtersAndSorts() {
        // given
        memberFollowRepository.save(MemberFollow.builder().followerId(1L).followingId(2L).build());

        WalkDiary oldPublic = walkDiaryRepository.save(
                WalkDiary.create(2L, null, "오래된 공개", "내용", List.of(), LocalDate.now(), true)
        );
        WalkDiary latestPublic = walkDiaryRepository.save(
                WalkDiary.create(2L, null, "최신 공개", "내용", List.of(), LocalDate.now(), true)
        );

        walkDiaryRepository.save(WalkDiary.create(2L, null, "비공개", "내용", List.of(), LocalDate.now(), false));
        walkDiaryRepository.save(WalkDiary.create(3L, null, "미팔로우 공개", "내용", List.of(), LocalDate.now(), true));

        ReflectionTestUtils.setField(oldPublic, "createdAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(latestPublic, "createdAt", LocalDateTime.now());

        // when
        Slice<WalkDiary> slice = walkDiaryRepository.findFollowingPublicSlice(
                1L,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );

        // then
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.getContent().get(0).getTitle()).isEqualTo("최신 공개");
        assertThat(slice.getContent().get(1).getTitle()).isEqualTo("오래된 공개");
    }
}
