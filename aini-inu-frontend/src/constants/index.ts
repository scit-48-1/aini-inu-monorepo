import { UserType, ThreadType, ChatRoom, FeedPostType } from '../types';
import { MOCK_USERS } from './users';

export { MOCK_USERS };

export const POINT_COLOR = 'amber-500';
export const NAVY_COLOR = 'navy-900';

export const MOCK_USER = MOCK_USERS[0];
export const MOCK_PARTNER = MOCK_USERS[1]; 

export const MOCK_WALKERS = [
  { id: 'w1', user: MOCK_USERS[1], location: { lat: 37.518, lng: 126.972, name: '이촌 한강공원' }, status: 'WALKING', message: '날씨가 너무 좋네요! 인사해요 👋', startTime: '30분 전' },
  { id: 'w2', user: MOCK_USERS[5], location: { lat: 37.525, lng: 126.985, name: '서빙고동 산책로' }, status: 'RESTING', message: '잠시 벤치에서 쉬고 있어요.', startTime: '1시간 전' }
];

export const MOCK_CHAT_ROOMS: ChatRoom[] = [
  {
    id: 'c1',
    threadId: 't1',
    title: '이촌 한강공원 벚꽃 산책',
    lastMessage: '네! 이따가 2시에 뵐게요 ㅎㅎ',
    lastTime: '오후 1:20',
    lastMessageTime: '오후 1:20',
    unreadCount: 2,
    type: 'INDIVIDUAL',
    isArchived: false,
    partner: MOCK_PARTNER,
    thumbnail: MOCK_PARTNER.dogs[0].image,
    isConfirmed: false,
    messages: [
      { id: 'm1', text: '안녕하세요! 산책 신청했습니다.', time: '오후 1:00', type: 'TALK', senderId: 'neighbor-2', timestamp: '2026-02-20T13:00:00.000Z' },
      { id: 'm2', text: '반가워요! 몽이도 기대 중이에요.', time: '오후 1:05', type: 'TALK', senderId: 'neighbor-1', timestamp: '2026-02-20T13:05:00.000Z' },
    ]
  }
];

const getTimeWithOffset = (minutes: number) => {
  const date = new Date(Date.now() + minutes * 60000);
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

// --- 정예 멤버 10개의 고퀄리티 산책 스레드 ---
export const MOCK_THREADS: ThreadType[] = [];

const scenarios = [
  { authorIdx: 0, offset: 120, title: "몽이와 함께하는 벚꽃 산책 🌸", loc: "이촌 한강공원 중앙광장" },
  { authorIdx: 1, offset: 30, title: "보리와 가벼운 조깅하실 분 🏃‍♀️", loc: "이촌 유원지" },
  { authorIdx: 2, offset: -15, title: "초코&쿠키와 킁킁 여행 (진행중) 🍪", loc: "이촌동 숲길" }, // 진행 중
  { authorIdx: 3, offset: 180, title: "루이와 야간 크루 모집 ✨", loc: "이촌 한강공원" },
  { authorIdx: 4, offset: 5, title: "지금 바로 나오실 분! 급구 ⚡️", loc: "편의점 앞" }, // 임박
  { authorIdx: 5, offset: -45, title: "두부와 친구 사귀기 (진행중) ☁️", loc: "어린이공원" }, // 진행 중
  { authorIdx: 6, offset: 60, title: "노령견 별이와 천천히 걷기 🐢", loc: "아파트 산책로" },
  { authorIdx: 7, offset: 240, title: "대형견 맥스의 카리스마 산책 🦁", loc: "주차장 근처" },
  { authorIdx: 8, offset: -5, title: "코코와 방금 시작했어요! 📸", loc: "갈대밭" }, // 방금 시작
  { authorIdx: 20, offset: 300, title: "가을이와 분위기 있는 산책 🍂", loc: "가로수길" }
];

scenarios.forEach((s, i) => {
  const author = MOCK_USERS[s.authorIdx];
  const participatingDogs = author.dogs;
  const dogNames = participatingDogs.map((d: any) => d.name).join(' & ');

  MOCK_THREADS.push({
    id: `t${i + 1}`,
    title: s.title,
    description: `${dogNames}는 사회성이 좋고 친구를 기다려요.`,
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    startTime: getTimeWithOffset(s.offset),
    endTime: getTimeWithOffset(s.offset + 60),
    location: s.loc,
    author: author,
    mode: participatingDogs.length > 1 ? 'GROUP' : 'INDIVIDUAL',
    maxParticipants: 4,
    currentParticipants: s.offset < 0 ? 2 : 1,
    allowNonOwners: true,
    thumbnail: participatingDogs[0].image,
    hardFilters: ['리드줄 필수'],
    softFilters: ['매너 견주님']
  });
});

export const MOCK_HOTSPOTS = [
  { name: '이촌동 멍카페', category: '애견카페', rating: 4.8 },
  { name: '한강 산책로 3코스', category: '산책로', rating: 4.9 },
];

export const MOCK_FEEDS: FeedPostType[] = [
  { id: 'f1', author: MOCK_USERS[0], images: ['https://images.unsplash.com/photo-1544568100-847a948585b9?w=800'], caption: '오늘 날씨 굿! ☀️', likes: 24, comments: 5, location: '이촌 한강공원', isLiked: false, createdAt: '2시간 전' }
];

// authorId는 handlers.ts seedDatabase()의 유저 ID 체계와 반드시 일치해야 함
// seed 이웃 유저: neighbor-1(보리누나), neighbor-2(초코아빠), neighbor-3(해피매니저), ..., neighbor-10(힐링펫)
export const MOCK_WALK_DIARIES: Record<string, any> = {
  'diary-1': {
    id: 'diary-1',
    authorId: 'neighbor-1',
    title: '보리와 봄 한강 산책',
    walkDate: '2026.02.15',
    place: '이촌 한강공원 중앙광장',
    lat: 37.5172, lng: 126.9662,
    photos: ['/images/dog-portraits/Maltese.jpg', '/images/dog-portraits/Poodle.jpg'],
    content: '오늘은 보리와 함께 한강에 나왔어요. 날씨가 너무 좋아서 강아지들도 신나게 뛰어놀았답니다. 봄바람이 불어오는 한강변을 걸으니 기분이 최고! 보리도 처음으로 수변 데크를 걸어봤는데 처음엔 무서워하다가 금세 적응해서 신나게 달렸어요.',
    isPublic: true,
    tags: [{ id: 'neighbor-2', nickname: '초코아빠', avatar: '/AINIINU_ROGO_B.png' }],
    participatingDogs: [{ name: '보리', image: '/images/dog-portraits/Maltese.jpg' }]
  },
  'diary-2': {
    id: 'diary-2',
    authorId: 'neighbor-2',
    title: '초코 & 쿠키의 주말 모험',
    walkDate: '2026.02.14',
    place: '이촌동 숲길',
    lat: 37.5198, lng: 126.9710,
    photos: ['/images/dog-portraits/Poodle.jpg', '/images/dog-portraits/Beagle.jpg', '/images/dog-portraits/Shiba Inu.jpg'],
    content: '발렌타인데이에 초코랑 쿠키랑 함께 숲길을 걸었어요. 낙엽 밟는 소리에 초코가 너무 신기해하더라고요. 쿠키는 도토리를 발견하고는 20분 동안 냄새만 맡았답니다. 이 두 아이가 함께 있으면 항상 웃음이 끊이질 않아요!',
    isPublic: true,
    tags: [],
    participatingDogs: [
      { name: '초코', image: '/images/dog-portraits/Poodle.jpg' },
      { name: '쿠키', image: '/images/dog-portraits/Beagle.jpg' }
    ]
  },
  'diary-3': {
    id: 'diary-3',
    authorId: 'neighbor-3',
    title: '해피와 야간 한강 산책',
    walkDate: '2026.02.13',
    place: '이촌 한강공원 야경 코스',
    lat: 37.5145, lng: 126.9590,
    photos: ['/images/dog-portraits/Shiba Inu.jpg'],
    content: '해피와 함께한 야간 산책, 반짝이는 도시 불빛 속에서 너무 낭만적인 밤이었어요. 서울 야경을 배경으로 해피가 포즈를 잡아줬는데 정말 화보 같았답니다. 한강의 야경은 언제 봐도 아름다워요.',
    isPublic: true,
    tags: [{ id: 'neighbor-1', nickname: '보리누나', avatar: '/AINIINU_ROGO_B.png' }],
    participatingDogs: [{ name: '해피', image: '/images/dog-portraits/Shiba Inu.jpg' }]
  }
};
