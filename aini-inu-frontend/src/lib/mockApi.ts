/**
 * 백엔드 서버가 없을 때 프론트엔드 테스트를 도와주는 가상 API 시뮬레이터입니다.
 */

export const mockApi = {
  // 1. 반려견 국가 인증 시뮬레이션
  verifyDog: async (regNo: string, ownerNm: string) => {
    console.log("Mock Server: Verifying dog info...", { regNo, ownerNm });
    await new Promise((resolve) => setTimeout(resolve, 1500)); // 지연 시간 생성

    // 테스트용: 번호가 '12345'로 시작하면 성공, 아니면 실패 시뮬레이션
    if (regNo.startsWith("12345")) {
      return {
        success: true,
        dogInfo: {
          dogNm: "인증된 멍멍이",
          kindNm: "골든 리트리버",
          sexNm: "암컷",
          neuterYn: "Y",
        },
      };
    }

    return {
      success: false,
      message:
        "국가 데이터베이스에 일치하는 정보가 없습니다. (테스트용: 12345)",
    };
  },

  // 2. AI 사진 분석 시뮬레이션
  analyzeImage: async (base64: string) => {
    console.log("Mock Server: Analyzing image with AI...");
    await new Promise((resolve) => setTimeout(resolve, 2000));

    return {
      breed: { en: "Retriever", ko: "리트리버", jp: "レトリバー" },
      estimatedAge: { en: "2-3 years", ko: "2-3세 추정", jp: "2-3歳推定" },
      color: { en: "Golden", ko: "황금색", jp: "ゴールデン" },
      features: [
        { en: "Red collar", ko: "빨간 목줄", jp: "赤い首輪" },
        { en: "Friendly", ko: "친화력 좋음", jp: "フレンドリー" },
      ],
    };
  },
};
