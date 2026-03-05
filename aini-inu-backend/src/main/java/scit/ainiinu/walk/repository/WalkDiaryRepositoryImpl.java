package scit.ainiinu.walk.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import scit.ainiinu.walk.entity.WalkDiary;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WalkDiaryRepositoryImpl implements WalkDiaryRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Slice<WalkDiary> findFollowingPublicSlice(Long followerId, Pageable pageable) {
        String jpql = """
                select wd
                from WalkDiary wd
                where wd.deletedAt is null
                  and wd.isPublic = true
                  and wd.memberId in (
                    select mf.followingId
                    from MemberFollow mf
                    where mf.followerId = :followerId
                  )
                order by wd.createdAt desc, wd.id desc
                """;

        TypedQuery<WalkDiary> query = entityManager.createQuery(jpql, WalkDiary.class)
                .setParameter("followerId", followerId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize() + 1);

        List<WalkDiary> rows = query.getResultList();
        boolean hasNext = rows.size() > pageable.getPageSize();

        List<WalkDiary> content;
        if (hasNext) {
            content = new ArrayList<>(rows.subList(0, pageable.getPageSize()));
        } else {
            content = new ArrayList<>(rows);
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
