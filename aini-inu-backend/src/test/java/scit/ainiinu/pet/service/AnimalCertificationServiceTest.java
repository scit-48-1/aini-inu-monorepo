package scit.ainiinu.pet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.pet.exception.PetErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AnimalCertificationServiceTest {

    private final AnimalCertificationService service = new AnimalCertificationService();

    @Nested
    @DisplayName("verify - 외부 API 호출 전 검증")
    class VerifyPreApiValidation {

        @Test
        @DisplayName("null 입력 시 false 반환")
        void null_returnsFalse() {
            // when
            boolean result = service.verify(null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 false 반환")
        void blank_returnsFalse() {
            // when
            boolean result = service.verify("");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("공백만 입력 시 false 반환")
        void whitespace_returnsFalse() {
            // when
            boolean result = service.verify("   ");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("15자리가 아닌 숫자면 INVALID_CERTIFICATION_NUMBER 예외")
        void wrongLength_throwsException() {
            // when & then
            assertThatThrownBy(() -> service.verify("12345"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.INVALID_CERTIFICATION_NUMBER);
        }

        @Test
        @DisplayName("숫자가 아닌 15자리면 INVALID_CERTIFICATION_NUMBER 예외")
        void nonNumeric_throwsException() {
            // when & then
            assertThatThrownBy(() -> service.verify("12345678901234a"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.INVALID_CERTIFICATION_NUMBER);
        }

        @Test
        @DisplayName("특수문자 포함 시 INVALID_CERTIFICATION_NUMBER 예외")
        void specialChars_throwsException() {
            // when & then
            assertThatThrownBy(() -> service.verify("12345-6789-0123"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.INVALID_CERTIFICATION_NUMBER);
        }
    }
}
