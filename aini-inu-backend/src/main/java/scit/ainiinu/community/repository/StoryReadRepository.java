package scit.ainiinu.community.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface StoryReadRepository {

    Slice<Long> findVisibleAuthorIdsForFollower(Long followerId, LocalDateTime cutoff, Pageable pageable);

    List<WalkDiary> findVisibleDiariesByAuthorIds(Collection<Long> authorIds, LocalDateTime cutoff);
}
