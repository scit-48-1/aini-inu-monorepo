package scit.ainiinu.common.entity.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;

/**
 * 위치 정보를 나타내는 Value Object.
 * Walk, LostPet 등 여러 컨텍스트에서 공유됩니다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int COORDINATE_SCALE = 6;

    @Column(nullable = false, length = 200)
    private String placeName;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 6)
    private BigDecimal longitude;

    @Column(length = 500)
    private String address;

    private Location(String placeName, BigDecimal latitude, BigDecimal longitude, String address) {
        validate(placeName, latitude, longitude);
        this.placeName = placeName;
        this.latitude = normalizeCoordinate(latitude);
        this.longitude = normalizeCoordinate(longitude);
        this.address = address;
    }

    /**
     * Location 인스턴스를 생성합니다.
     *
     * @param placeName 장소명 (필수)
     * @param latitude  위도 (-90 ~ +90)
     * @param longitude 경도 (-180 ~ +180)
     * @param address   주소 (선택)
     * @return Location 인스턴스
     * @throws BusinessException placeName이 null/blank이거나 좌표가 유효 범위를 벗어난 경우
     */
    public static Location of(String placeName, BigDecimal latitude, BigDecimal longitude, String address) {
        return new Location(placeName, latitude, longitude, address);
    }

    /**
     * 주소 없이 Location 인스턴스를 생성합니다.
     *
     * @param placeName 장소명 (필수)
     * @param latitude  위도 (-90 ~ +90)
     * @param longitude 경도 (-180 ~ +180)
     * @return Location 인스턴스
     * @throws BusinessException placeName이 null/blank이거나 좌표가 유효 범위를 벗어난 경우
     */
    public static Location of(String placeName, BigDecimal latitude, BigDecimal longitude) {
        return new Location(placeName, latitude, longitude, null);
    }

    /**
     * Haversine 공식을 사용하여 두 위치 간의 거리를 계산합니다.
     *
     * @param other 비교할 위치
     * @return 두 위치 간의 거리 (km)
     * @throws BusinessException other가 null인 경우
     */
    public double distanceTo(Location other) {
        if (other == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }

        double lat1 = Math.toRadians(this.latitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double deltaLat = Math.toRadians(other.latitude.subtract(this.latitude).doubleValue());
        double deltaLon = Math.toRadians(other.longitude.subtract(this.longitude).doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 다른 위치가 지정된 반경 내에 있는지 확인합니다.
     *
     * @param other    비교할 위치
     * @param radiusKm 반경 (km)
     * @return 반경 내에 있으면 true
     * @throws BusinessException other가 null이거나 radiusKm이 음수인 경우
     */
    public boolean isWithinRadius(Location other, double radiusKm) {
        if (other == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (radiusKm < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        return distanceTo(other) <= radiusKm;
    }

    private void validate(String placeName, BigDecimal latitude, BigDecimal longitude) {
        validatePlaceName(placeName);
        validateLatitude(latitude);
        validateLongitude(longitude);
    }

    private void validatePlaceName(String placeName) {
        if (placeName == null || placeName.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private void validateLatitude(BigDecimal latitude) {
        if (latitude == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private void validateLongitude(BigDecimal longitude) {
        if (longitude == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private BigDecimal normalizeCoordinate(BigDecimal coordinate) {
        return coordinate.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Location location = (Location) o;
        return Objects.equals(placeName, location.placeName)
                && Objects.equals(latitude, location.latitude)
                && Objects.equals(longitude, location.longitude)
                && Objects.equals(address, location.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeName, latitude, longitude, address);
    }

    @Override
    public String toString() {
        return "Location{" +
                "placeName='" + placeName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", address='" + address + '\'' +
                '}';
    }
}
