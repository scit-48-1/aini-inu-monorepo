package scit.ainiinu.member.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import scit.ainiinu.member.entity.MemberPersonalityType;
import scit.ainiinu.member.repository.MemberPersonalityTypeRepository;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Profile("!test") // 테스트 프로필이 아닐 때만 실행
public class MemberDataInitializer implements ApplicationRunner {

    private final MemberPersonalityTypeRepository personalityTypeRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (personalityTypeRepository.count() == 0) {
            List<MemberPersonalityType> types = List.of(
                new MemberPersonalityType("동네친구", "LOCAL_FRIEND"),
                new MemberPersonalityType("반려견정보공유", "PET_INFO_SHARING"),
                new MemberPersonalityType("랜선집사", "ONLINE_PET_LOVER"),
                new MemberPersonalityType("강아지만좋아함", "DOG_LOVER_ONLY")
            );
            personalityTypeRepository.saveAll(types);
        }
    }
}
