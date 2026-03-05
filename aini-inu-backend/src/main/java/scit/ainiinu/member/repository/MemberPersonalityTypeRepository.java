package scit.ainiinu.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.member.entity.MemberPersonalityType;

import java.util.Optional;

public interface MemberPersonalityTypeRepository extends JpaRepository<MemberPersonalityType, Long> {
    Optional<MemberPersonalityType> findByCode(String code);
}
