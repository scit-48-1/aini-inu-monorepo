export const locationService = {
  /**
   * 주소를 기반으로 위경도 좌표를 가져옵니다.
   * 네트워크 에러 발생 시 기본 좌표(서울숲)를 반환하여 앱 중단을 방지합니다.
   */
  async getCoordinates(address: string): Promise<[number, number] | null> {
    const DEFAULT_COORDS: [number, number] = [37.5445, 127.0445]; // 성수동 서울숲

    try {
      // 1. 빈 주소 체크
      if (!address || address.trim() === '') return DEFAULT_COORDS;

      // 2. 외부 API 호출 (Nominatim Open API) - apiClient는 /api/v1 고정이므로 여기선 fetch를 유지하되 안전하게 래핑
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(address)}&limit=1`,
        { signal: controller.signal }
      );
      clearTimeout(timeoutId);

      if (!res.ok) throw new Error('Network response was not ok');

      const result = await res.json();
      if (result && result[0]) {
        return [parseFloat(result[0].lat), parseFloat(result[0].lon)];
      }

      console.warn(`[Location] No coordinates found for address: ${address}`);
      return DEFAULT_COORDS;
    } catch (error) {
      console.error('[Location Error] Failed to fetch coordinates:', error);
      // 에러 발생 시 앱이 죽지 않도록 기본 좌표 반환
      return DEFAULT_COORDS;
    }
  }
};
