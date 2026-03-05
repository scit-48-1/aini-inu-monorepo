package scit.ainiinu.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.MemberPersonality;

import java.util.List;

public interface MemberPersonalityRepository extends JpaRepository<MemberPersonality, Long> {
    void deleteByMember(Member member);
    List<MemberPersonality> findByMember(Member member);
}
