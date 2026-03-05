package scit.ainiinu.walk.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;
import scit.ainiinu.walk.entity.WalkThreadStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:walkthread;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class WalkThreadRepositoryTest {

    @Autowired
    private WalkThreadRepository walkThreadRepository;

    @Autowired
    private WalkThreadApplicationRepository walkThreadApplicationRepository;

    @Nested
    @DisplayName("스레드 저장소 조회")
    class QueryThread {

        @Test
        @DisplayName("작성자의 활성 스레드 존재 여부를 조회한다")
        void existsByAuthorIdAndStatus_success() {
            // given
            WalkThread thread = createThread(1L, "서울숲 산책");
            walkThreadRepository.save(thread);

            // when
            boolean exists = walkThreadRepository.existsByAuthorIdAndStatus(1L, WalkThreadStatus.RECRUITING);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("RECRUITING 상태 스레드를 Slice로 조회한다")
        void findByStatus_slice_success() {
            // given
            walkThreadRepository.save(createThread(1L, "A"));
            walkThreadRepository.save(createThread(2L, "B"));

            // when
            Slice<WalkThread> slice = walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(
                    WalkThreadStatus.RECRUITING,
                    PageRequest.of(0, 20)
            );

            // then
            assertThat(slice.getContent()).hasSize(2);
            assertThat(slice.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("스레드 신청 저장소 조회")
    class QueryApplication {

        @Test
        @DisplayName("스레드 신청 인원 수(JOINED)를 카운트한다")
        void countByThreadIdAndStatus_success() {
            // given
            WalkThread thread = walkThreadRepository.save(createThread(1L, "서울숲 산책"));
            walkThreadApplicationRepository.save(
                    WalkThreadApplication.joined(thread.getId(), 2L, 9002L)
            );
            walkThreadApplicationRepository.save(
                    WalkThreadApplication.joined(thread.getId(), 3L, 9003L)
            );
            walkThreadApplicationRepository.save(
                    WalkThreadApplication.canceled(thread.getId(), 4L)
            );

            // when
            long joinedCount = walkThreadApplicationRepository.countByThreadIdAndStatus(
                    thread.getId(),
                    WalkThreadApplicationStatus.JOINED
            );

            // then
            assertThat(joinedCount).isEqualTo(2);
        }
    }

    private WalkThread createThread(Long authorId, String title) {
        return WalkThread.builder()
                .authorId(authorId)
                .title(title)
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
