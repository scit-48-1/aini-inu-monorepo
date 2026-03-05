package scit.ainiinu.walk.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:walkdiary-thread-link;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class WalkDiaryThreadLinkRepositoryTest {

    @Autowired
    private WalkDiaryRepository walkDiaryRepository;

    @Autowired
    private WalkThreadRepository walkThreadRepository;

    @Test
    @DisplayName("연결된 스레드가 삭제되어도 일기 상세 조회는 가능하다")
    void findDiary_whenLinkedThreadDeleted_success() {
        // given
        WalkThread thread = walkThreadRepository.save(createThread(2L));
        WalkDiary diary = walkDiaryRepository.save(
                WalkDiary.create(1L, thread.getId(), "스레드 연결", "본문", List.of(), LocalDate.now(), true)
        );

        thread.markDeleted();

        // when
        WalkDiary found = walkDiaryRepository.findByIdAndDeletedAtIsNull(diary.getId()).orElseThrow();

        // then
        assertThat(found.getThreadId()).isEqualTo(thread.getId());
        assertThat(walkThreadRepository.findById(thread.getId()).orElseThrow().getStatus()).isEqualTo(WalkThreadStatus.DELETED);
    }

    private WalkThread createThread(Long authorId) {
        return WalkThread.builder()
                .authorId(authorId)
                .title("테스트 스레드")
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
                .build();
    }
}
