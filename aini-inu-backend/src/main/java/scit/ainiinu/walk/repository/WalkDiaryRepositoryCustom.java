package scit.ainiinu.walk.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import scit.ainiinu.walk.entity.WalkDiary;

public interface WalkDiaryRepositoryCustom {

    Slice<WalkDiary> findFollowingPublicSlice(Long followerId, Pageable pageable);
}
