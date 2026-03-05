// 글로벌 서비스 대응 번역 사전 (English Master Data 기반)

export type Locale = 'ko' | 'jp';

export const BREED_MAP: Record<string, { ko: string; jp: string }> = {
  "Golden Retriever": { ko: "골든 리트리버", jp: "ゴールデン・レトリバー" },
  "Pomeranian": { ko: "포메라니안", jp: "ポメラニアン" },
  "Poodle": { ko: "푸들", jp: "プードル" },
  "Maltese": { ko: "말티즈", jp: "マルチーズ" },
  "Shiba Inu": { ko: "시바견", jp: "柴犬" },
  "Bichon Frise": { ko: "비숑 프리제", jp: "ビション・フリーゼ" },
  "Chihuahua": { ko: "치와와", jp: "チワワ" },
  "Jindo Dog": { ko: "진돗개", jp: "珍島犬" },
  "Bulldog": { ko: "불독", jp: "ブルドッグ" },
  "Yorkshire Terrier": { ko: "요크셔 테리어", jp: "ヨークシャー・テリア" },
  "Welsh Corgi": { ko: "웰시 코기", jp: "ウェルシュ・コーギー" },
};

export const FEATURE_MAP: Record<string, { ko: string; jp: string }> = {
  "Friendly": { ko: "사람을 잘 따라요", jp: "人懐っこい" },
  "Red collar": { ko: "빨간 목줄 착용", jp: "赤い首輪着用" },
  "Large size": { ko: "대형견", jp: "大型犬" },
  "Small size": { ko: "소형견", jp: "小型犬" },
  "Active": { ko: "활발함", jp: "活発" },
  "Quiet": { ko: "얌전함", jp: "おとなしい" },
};

/**
 * 영어 데이터를 현재 언어 설정에 맞춰 번역합니다.
 * 매핑 데이터가 없으면 원본(English)을 반환합니다.
 */
export const t = (key: string, category: 'breed' | 'feature', locale: Locale): string => {
  const map = category === 'breed' ? BREED_MAP : FEATURE_MAP;
  
  // 정확한 매칭 시도
  if (map[key]) return map[key][locale];
  
  // 부분 매칭 또는 기본값 반환
  return key; 
};
