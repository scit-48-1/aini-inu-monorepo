package scit.ainiinu.walk.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:walkdiary-crud;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class WalkDiaryRepositoryCrudTest {

    @Autowired
    private WalkDiaryRepository walkDiaryRepository;

    @Test
    @DisplayName("작성자 목록 조회에서 soft delete 데이터는 제외된다")
    void ownerList_excludesSoftDeleted() {
        // given
        WalkDiary active = WalkDiary.create(1L, null, "활성", "내용", List.of(), LocalDate.now(), true);
        WalkDiary deleted = WalkDiary.create(1L, null, "삭제", "내용", List.of(), LocalDate.now(), true);
        deleted.softDelete(LocalDateTime.now());
        walkDiaryRepository.save(active);
        walkDiaryRepository.save(deleted);

        // when
        Slice<WalkDiary> slice = walkDiaryRepository.findByMemberIdAndDeletedAtIsNull(1L, PageRequest.of(0, 20));

        // then
        assertThat(slice.getContent()).hasSize(1);
        assertThat(slice.getContent().get(0).getTitle()).isEqualTo("활성");
    }

    @Test
    @DisplayName("타인 일기 목록 조회는 공개 일기만 반환한다")
    void otherMemberList_returnsPublicOnly() {
        // given
        walkDiaryRepository.save(WalkDiary.create(2L, null, "공개", "내용", List.of(), LocalDate.now(), true));
        walkDiaryRepository.save(WalkDiary.create(2L, null, "비공개", "내용", List.of(), LocalDate.now(), false));

        // when
        Slice<WalkDiary> slice = walkDiaryRepository.findByMemberIdAndIsPublicTrueAndDeletedAtIsNull(2L, PageRequest.of(0, 20));

        // then
        assertThat(slice.getContent()).hasSize(1);
        assertThat(slice.getContent().get(0).getTitle()).isEqualTo("공개");
    }
}
