package scit.ainiinu.community.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StoryReadRepositoryImpl implements StoryReadRepository {

    private final EntityManager entityManager;

    @Override
    public Slice<Long> findVisibleAuthorIdsForFollower(Long followerId, LocalDateTime cutoff, Pageable pageable) {
        String jpql = """
                select wd.memberId
                from WalkDiary wd
                where wd.isPublic = true
                  and wd.deletedAt is null
                  and wd.createdAt >= :cutoff
                  and wd.memberId in (
                    select mf.followingId
                    from MemberFollow mf
                    where mf.followerId = :followerId
                  )
                group by wd.memberId
                order by max(wd.createdAt) desc, wd.memberId desc
                """;

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("followerId", followerId)
                .setParameter("cutoff", cutoff)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize() + 1);

        List<Long> rows = query.getResultList();
        boolean hasNext = rows.size() > pageable.getPageSize();

        List<Long> content;
        if (hasNext) {
            content = new ArrayList<>(rows.subList(0, pageable.getPageSize()));
        } else {
            content = new ArrayList<>(rows);
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public List<WalkDiary> findVisibleDiariesByAuthorIds(Collection<Long> authorIds, LocalDateTime cutoff) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Collections.emptyList();
        }

        String jpql = """
                select wd
                from WalkDiary wd
                where wd.isPublic = true
                  and wd.deletedAt is null
                  and wd.createdAt >= :cutoff
                  and wd.memberId in :authorIds
                order by wd.createdAt desc, wd.id desc
                """;

        return entityManager.createQuery(jpql, WalkDiary.class)
                .setParameter("cutoff", cutoff)
                .setParameter("authorIds", authorIds)
                .getResultList();
    }
}
