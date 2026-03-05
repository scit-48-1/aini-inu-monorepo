package scit.ainiinu.common.entity.vo;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;

import static org.assertj.core.api.Assertions.*;

class LocationTest {

    @Nested
    @DisplayName("Location 생성")
    class CreateLocation {

        @Test
        @DisplayName("모든 정보를 포함하여 생성하면 성공한다")
        void successWithAllFields() {
            // given
            String placeName = "서울역";
            BigDecimal latitude = new BigDecimal("37.55630000");
            BigDecimal longitude = new BigDecimal("126.97220000");
            String address = "서울특별시 용산구 한강대로 405";

            // when
            Location location = Location.of(placeName, latitude, longitude, address);

            // then
            assertThat(location.getPlaceName()).isEqualTo(placeName);
            assertThat(location.getLatitude()).isEqualByComparingTo(latitude);
            assertThat(location.getLongitude()).isEqualByComparingTo(longitude);
            assertThat(location.getAddress()).isEqualTo(address);
        }

        @Test
        @DisplayName("주소 없이 생성하면 성공한다")
        void successWithoutAddress() {
            // given
            String placeName = "서울역";
            BigDecimal latitude = new BigDecimal("37.55630000");
            BigDecimal longitude = new BigDecimal("126.97220000");

            // when
            Location location = Location.of(placeName, latitude, longitude);

            // then
            assertThat(location.getPlaceName()).isEqualTo(placeName);
            assertThat(location.getLatitude()).isEqualByComparingTo(latitude);
            assertThat(location.getLongitude()).isEqualByComparingTo(longitude);
            assertThat(location.getAddress()).isNull();
        }
    }

    @Nested
    @DisplayName("좌표 경계값 테스트")
    class CoordinateBoundaryTest {

        @Test
        @DisplayName("위도 +90도(북극)로 생성하면 성공한다")
        void successWithMaxLatitude() {
            // given
            BigDecimal latitude = new BigDecimal("90");
            BigDecimal longitude = new BigDecimal("0");

            // when
            Location location = Location.of("북극", latitude, longitude);

            // then
            assertThat(location.getLatitude()).isEqualByComparingTo(new BigDecimal("90"));
        }

        @Test
        @DisplayName("위도 -90도(남극)로 생성하면 성공한다")
        void successWithMinLatitude() {
            // given
            BigDecimal latitude = new BigDecimal("-90");
            BigDecimal longitude = new BigDecimal("0");

            // when
            Location location = Location.of("남극", latitude, longitude);

            // then
            assertThat(location.getLatitude()).isEqualByComparingTo(new BigDecimal("-90"));
        }

        @Test
        @DisplayName("경도 +180도로 생성하면 성공한다")
        void successWithMaxLongitude() {
            // given
            BigDecimal latitude = new BigDecimal("0");
            BigDecimal longitude = new BigDecimal("180");

            // when
            Location location = Location.of("날짜변경선(동)", latitude, longitude);

            // then
            assertThat(location.getLongitude()).isEqualByComparingTo(new BigDecimal("180"));
        }

        @Test
        @DisplayName("경도 -180도로 생성하면 성공한다")
        void successWithMinLongitude() {
            // given
            BigDecimal latitude = new BigDecimal("0");
            BigDecimal longitude = new BigDecimal("-180");

            // when
            Location location = Location.of("날짜변경선(서)", latitude, longitude);

            // then
            assertThat(location.getLongitude()).isEqualByComparingTo(new BigDecimal("-180"));
        }
    }

    @Nested
    @DisplayName("유효성 검증 실패")
    class ValidationFailure {

        @Test
        @DisplayName("placeName이 null이면 예외가 발생한다")
        void failWithNullPlaceName() {
            // given
            BigDecimal latitude = new BigDecimal("37.5663");
            BigDecimal longitude = new BigDecimal("126.9779");

            // when & then
            assertThatThrownBy(() -> Location.of(null, latitude, longitude))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("placeName이 빈 문자열이면 예외가 발생한다")
        void failWithBlankPlaceName() {
            // given
            BigDecimal latitude = new BigDecimal("37.5663");
            BigDecimal longitude = new BigDecimal("126.9779");

            // when & then
            assertThatThrownBy(() -> Location.of("   ", latitude, longitude))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("위도가 null이면 예외가 발생한다")
        void failWithNullLatitude() {
            // given
            BigDecimal longitude = new BigDecimal("126.9779");

            // when & then
            assertThatThrownBy(() -> Location.of("서울시청", null, longitude))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("경도가 null이면 예외가 발생한다")
        void failWithNullLongitude() {
            // given
            BigDecimal latitude = new BigDecimal("37.5663");

            // when & then
            assertThatThrownBy(() -> Location.of("서울시청", latitude, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("위도가 +90을 초과하면 예외가 발생한다")
        void failWithLatitudeOverMax() {
            // given
            BigDecimal latitude = new BigDecimal("90.00000001");
            BigDecimal longitude = new BigDecimal("126.9779");

            // when & then
            assertThatThrownBy(() -> Location.of("서울시청", latitude, longitude))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("위도가 -90 미만이면 예외가 발생한다")
        void failWithLatitudeUnderMin() {
            // given
            BigDecimal latitude = new BigDecimal("-90.00000001");
            BigDecimal longitude = new BigDecimal("126.9779");

            // when & then
            assertThatThrownBy(() -> Location.of("서울시청", latitude, longitude))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("경도가 +180을 초과하면 예외가 발생한다")
        void failWithLongitudeOverMax() {
            // given
            BigDecimal latitude = new BigDecimal("37.5663");
            BigDecimal longitude = new BigDecimal("180.00000001");

            // when & then
            assertThatThrownBy(() -> Location.of("서울시청", latitude, longitude))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("경도가 -180 미만이면 예외가 발생한다")
        void failWithLongitudeUnderMin() {
            // given
            BigDecimal latitude = new BigDecimal("37.5663");
            BigDecimal longitude = new BigDecimal("-180.00000001");

            // when & then
            assertThatThrownBy(() -> Location.of("서울시청", latitude, longitude))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }
    }

    @Nested
    @DisplayName("Haversine 거리 계산")
    class DistanceCalculation {

        @Test
        @DisplayName("서울과 부산 사이의 거리를 계산하면 약 325km이다")
        void calculateDistanceBetweenSeoulAndBusan() {
            // given
            // 서울역 좌표
            Location seoul = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // 부산역 좌표
            Location busan = Location.of("부산역",
                    new BigDecimal("35.11500000"),
                    new BigDecimal("129.04170000"));

            // when
            double distance = seoul.distanceTo(busan);

            // then
            // 서울-부산 직선거리: 약 325km (오차 범위 ±30km 허용)
            assertThat(distance).isBetween(320.0, 350.0);
        }

        @Test
        @DisplayName("동일한 위치 사이의 거리는 0이다")
        void calculateDistanceSameLocation() {
            // given
            Location location1 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location location2 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // when
            double distance = location1.distanceTo(location2);

            // then
            assertThat(distance).isEqualTo(0.0);
        }

        @Test
        @DisplayName("null 위치와의 거리 계산 시 예외가 발생한다")
        void failDistanceCalculationWithNull() {
            // given
            Location location = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // when & then
            assertThatThrownBy(() -> location.distanceTo(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("도쿄와 서울 사이의 거리를 계산하면 약 1160km이다")
        void calculateDistanceBetweenSeoulAndTokyo() {
            // given
            // 서울역 좌표
            Location seoul = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // 도쿄역 좌표
            Location tokyo = Location.of("도쿄역",
                    new BigDecimal("35.68150000"),
                    new BigDecimal("139.76700000"));

            // when
            double distance = seoul.distanceTo(tokyo);

            // then
            // 서울-도쿄 직선거리: 약 1160km (오차 범위 ±50km 허용)
            assertThat(distance).isBetween(1100.0, 1200.0);
        }
    }

    @Nested
    @DisplayName("반경 내 확인")
    class WithinRadius {

        @Test
        @DisplayName("반경 내에 있으면 true를 반환한다")
        void returnTrueWhenWithinRadius() {
            // given
            Location seoul = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location gangnam = Location.of("강남역",
                    new BigDecimal("37.49810000"),
                    new BigDecimal("127.02760000"));

            // when
            boolean isWithin = seoul.isWithinRadius(gangnam, 10.0);

            // then
            // 서울역에서 강남역까지 약 8.5km
            assertThat(isWithin).isTrue();
        }

        @Test
        @DisplayName("반경 밖에 있으면 false를 반환한다")
        void returnFalseWhenOutsideRadius() {
            // given
            Location seoul = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location busan = Location.of("부산역",
                    new BigDecimal("35.11500000"),
                    new BigDecimal("129.04170000"));

            // when
            boolean isWithin = seoul.isWithinRadius(busan, 100.0);

            // then
            // 서울-부산 약 325km이므로 100km 반경 밖
            assertThat(isWithin).isFalse();
        }

        @Test
        @DisplayName("null 위치에 대해 반경 확인 시 예외가 발생한다")
        void failWithNullLocation() {
            // given
            Location location = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // when & then
            assertThatThrownBy(() -> location.isWithinRadius(null, 10.0))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("음수 반경으로 확인하면 예외가 발생한다")
        void failWithNegativeRadius() {
            // given
            Location location1 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location location2 = Location.of("강남역",
                    new BigDecimal("37.49810000"),
                    new BigDecimal("127.02760000"));

            // when & then
            assertThatThrownBy(() -> location1.isWithinRadius(location2, -1.0))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("반경 경계에 정확히 위치하면 true를 반환한다")
        void returnTrueWhenExactlyOnBoundary() {
            // given
            Location seoul = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location gangnam = Location.of("강남역",
                    new BigDecimal("37.49810000"),
                    new BigDecimal("127.02760000"));

            double distance = seoul.distanceTo(gangnam);

            // when
            boolean isWithin = seoul.isWithinRadius(gangnam, distance);

            // then
            assertThat(isWithin).isTrue();
        }
    }

    @Nested
    @DisplayName("동등성 테스트")
    class Equality {

        @Test
        @DisplayName("모든 필드가 같으면 동등하다")
        void equalWhenAllFieldsMatch() {
            // given
            Location location1 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"),
                    "서울특별시 용산구");

            Location location2 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"),
                    "서울특별시 용산구");

            // when & then
            assertThat(location1).isEqualTo(location2);
            assertThat(location1.hashCode()).isEqualTo(location2.hashCode());
        }

        @Test
        @DisplayName("placeName이 다르면 동등하지 않다")
        void notEqualWhenPlaceNameDiffers() {
            // given
            Location location1 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location location2 = Location.of("용산역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // when & then
            assertThat(location1).isNotEqualTo(location2);
        }

        @Test
        @DisplayName("좌표가 다르면 동등하지 않다")
        void notEqualWhenCoordinatesDiffer() {
            // given
            Location location1 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location location2 = Location.of("서울역",
                    new BigDecimal("37.55630100"),
                    new BigDecimal("126.97220000"));

            // when & then
            assertThat(location1).isNotEqualTo(location2);
        }

        @Test
        @DisplayName("주소가 다르면 동등하지 않다")
        void notEqualWhenAddressDiffers() {
            // given
            Location location1 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"),
                    "서울특별시 용산구");

            Location location2 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"),
                    "서울시 용산구");

            // when & then
            assertThat(location1).isNotEqualTo(location2);
        }

        @Test
        @DisplayName("주소가 null인 경우와 있는 경우는 동등하지 않다")
        void notEqualWhenOneAddressIsNull() {
            // given
            Location location1 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            Location location2 = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"),
                    "서울특별시 용산구");

            // when & then
            assertThat(location1).isNotEqualTo(location2);
        }

        @Test
        @DisplayName("자기 자신과는 동등하다")
        void equalToItself() {
            // given
            Location location = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // when & then
            assertThat(location).isEqualTo(location);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void notEqualToNull() {
            // given
            Location location = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // when & then
            assertThat(location).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과는 동등하지 않다")
        void notEqualToDifferentType() {
            // given
            Location location = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"));

            // when & then
            assertThat(location).isNotEqualTo("서울역");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("모든 필드가 포함된 문자열을 반환한다")
        void containsAllFields() {
            // given
            Location location = Location.of("서울역",
                    new BigDecimal("37.55630000"),
                    new BigDecimal("126.97220000"),
                    "서울특별시 용산구");

            // when
            String result = location.toString();

            // then
            assertThat(result).contains("서울역");
            assertThat(result).contains("37.556300");
            assertThat(result).contains("126.972200");
            assertThat(result).contains("서울특별시 용산구");
        }
    }
}
