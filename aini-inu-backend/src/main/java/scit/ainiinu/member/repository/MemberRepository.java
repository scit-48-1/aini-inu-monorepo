package scit.ainiinu.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import scit.ainiinu.member.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByNickname(String nickname);
    Optional<Member> findByEmail(String email);
    Slice<Member> findByNicknameContainingIgnoreCaseOrLinkedNicknameContainingIgnoreCaseAndIdNot(
            String nickname,
            String linkedNickname,
            Long memberId,
            Pageable pageable
    );
}
