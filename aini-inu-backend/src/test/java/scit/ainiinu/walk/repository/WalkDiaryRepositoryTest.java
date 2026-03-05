package scit.ainiinu.walk.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.member.entity.MemberFollow;
import scit.ainiinu.member.repository.MemberFollowRepository;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:walkdiary;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class WalkDiaryRepositoryTest {

    @Autowired
    private WalkDiaryRepository walkDiaryRepository;

    @Autowired
    private MemberFollowRepository memberFollowRepository;

    @Nested
    @DisplayName("일기 저장소 조회")
    class QueryDiary {

        @Test
        @DisplayName("본인 일기 목록에서 soft delete 된 항목은 제외된다")
        void findByMemberIdAndDeletedAtIsNull_success() {
            // given
            WalkDiary active = WalkDiary.create(1L, null, "제목1", "내용1", List.of("a.jpg"), LocalDate.now(), true);
            WalkDiary deleted = WalkDiary.create(1L, null, "제목2", "내용2", List.of("b.jpg"), LocalDate.now(), true);
            deleted.softDelete(LocalDateTime.now());

            walkDiaryRepository.save(active);
            walkDiaryRepository.save(deleted);

            // when
            Slice<WalkDiary> slice = walkDiaryRepository.findByMemberIdAndDeletedAtIsNull(
                    1L,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.getContent().get(0).getTitle()).isEqualTo("제목1");
        }

        @Test
        @DisplayName("팔로잉 피드는 공개 일기만 조회한다")
        void findFollowingPublicSlice_success() {
            // given
            WalkDiary publicDiary = WalkDiary.create(2L, null, "공개", "내용", List.of(), LocalDate.now(), true);
            WalkDiary privateDiary = WalkDiary.create(2L, null, "비공개", "내용", List.of(), LocalDate.now(), false);
            WalkDiary anotherPublic = WalkDiary.create(3L, null, "다른사람", "내용", List.of(), LocalDate.now(), true);
            walkDiaryRepository.save(publicDiary);
            walkDiaryRepository.save(privateDiary);
            walkDiaryRepository.save(anotherPublic);

            memberFollowRepository.save(MemberFollow.builder().followerId(1L).followingId(2L).build());

            // when
            Slice<WalkDiary> slice = walkDiaryRepository.findFollowingPublicSlice(
                    1L,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.getContent().get(0).getTitle()).isEqualTo("공개");
        }
    }
}
