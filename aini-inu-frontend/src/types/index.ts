// 아이니 이누 (Aini Inu) Core Type Definitions v1.3.1 (Fact-Based)
// 현재 프론트엔드 UI 및 로직에서 실제로 사용 중인 필드들입니다.

export type DogTendency = '소심해요' | '에너지넘침' | '간식좋아함' | '사람좋아함' | '친구구함' | '주인바라기' | '까칠해요';
export type WalkStyle = '전력질주' | '냄새맡기집중' | '공원벤치휴식형' | '느긋함' | '냄새탐정' | '무한동력' | '저질체력';

export type DogType = {
  id: string;
  name: string;
  breed: string;
  age: number;
  birthDate: string; // YYYY-MM-DD
  gender: 'M' | 'F';
  image: string;
  tendencies: DogTendency[];
  walkStyle: WalkStyle;
  mbti?: string;
  isNeutralized: boolean;
  isMain: boolean;
};

export type UserType = {
  id: string;
  email: string;
  nickname: string;
  handle: string;
  avatar: string;
  mannerScore: number;
  isOwner: boolean;
  birthDate: string;
  age: number;
  gender: 'M' | 'F';
  mbti?: string;
  phone?: string;
  nicknameChangedAt?: string; // ISO string — 닉네임 마지막 변경 시각 (30일 쿨다운 계산용)
  about: string;
  location: string;
  dogs: DogType[];
  followerCount: number;
  followingCount: number;
  tendencies?: string[]; // 유저 성향 태그 (MOCK_USERS 및 프로필 표시용)
};

export type ThreadMode = 'INDIVIDUAL' | 'GROUP';

export type ThreadType = {
  id: string;
  title: string;
  description: string;
  startTime: string;
  endTime: string;
  location: string;
  lat?: number;
  lng?: number;
  author: UserType;
  mode: ThreadMode;
  maxParticipants: number;
  currentParticipants: number;
  participatingDogs?: DogType[];
  hardFilters?: string[];
  softFilters?: string[];
  isEmergency?: boolean;
  createdAt?: string;      // 생성 시각 (ISO string)
  allowNonOwners?: boolean; // 반려견 없는 참여자 허용 여부
  isJoined?: boolean;
  // 런타임 확장 필드 (useRadarLogic·컴포넌트에서 주입)
  thumbnail?: string;
  image?: string;
  owner?: string;    // author.nickname 표시용
  place?: string;    // location 표시용 alias
  time?: string;     // startTime 포맷 표시용
  name?: string;     // 강아지 이름 (긴급 제보)
  breed?: string;    // 강아지 견종 (긴급 제보)
  content?: string;  // description 표시용 alias
  distance?: number; // useRadarLogic에서 계산되는 현재 위치와의 거리(km)
};

export type MessageType = 'TALK' | 'SYSTEM';

export type ChatMessage = {
  id: string;
  senderId: string;
  text: string;
  content?: string; // text의 alias (핸들러 호환)
  type: MessageType;
  timestamp: string;
  time?: string;    // UI 표시용 포맷 시간 (e.g. 오후 1:00)
};

export type ChatRoom = {
  id: string;
  partner: UserType;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
  messages: ChatMessage[];
  // 확장 UI 필드
  threadId?: string;
  title?: string;
  lastTime?: string;
  type?: 'INDIVIDUAL' | 'GROUP';
  isArchived?: boolean;
  thumbnail?: string;
  isConfirmed?: boolean;
};

export type MapMarker = {
  id: string;
  lat?: number;
  lng?: number;
  image?: string;
  thumbnail?: string;
  isEmergency?: boolean;
};

export type BreedSuggestion = {
  ko: string;
  en: string;
};

export type DiaryTag = {
  id: string;
  nickname: string;
  avatar: string;
};

export type DiaryDog = {
  name: string;
  image: string;
};

export type WalkDiaryType = {
  id: string;
  title: string;
  content: string;
  photos: string[];
  walkDate: string;
  place: string;
  partner?: {
    nickname: string;
    avatar: string;
  };
  // 런타임 확장 필드
  authorId?: string;
  lat?: number;
  lng?: number;
  isPublic?: boolean;
  tags?: DiaryTag[];
  participatingDogs?: DiaryDog[];
  isDraft?: boolean;
  diaryTitle?: string;  // useWalkDiaries processedDiaries 가공 필드
  image?: string;       // 썸네일
  thumbnail?: string;   // 썸네일 alias
  location?: string;    // place alias
};

export type DogFormData = Omit<DogType, 'id' | 'age' | 'isMain'>;

export type PostFormData = Pick<FeedPostType, 'images' | 'caption' | 'location'>;

export type DiaryFormValues = {
  title: string;
  content: string;
  photos: string[];
  isPublic: boolean;
  tags: DiaryTag[];
};

export type FeedPostType = {
  id: string;
  author: UserType;
  images: string[];
  caption: string;
  content?: string;  // caption의 alias (컴포넌트 호환)
  likes: number; // likesCount -> likes (코드 일치)
  comments: number; // commentCount -> comments (코드 일치)
  location: string;
  isLiked: boolean;
  createdAt?: string; // 생성 시각 (ISO string 또는 표시용 문자열)
  commentsList?: CommentType[];
  tags?: string[];
  time?: string; // UI 노출용 시간 (e.g. 2시간 전)
};

export type CommentType = {
  id: string;
  author: {
    nickname: string;
    avatar: string;
  };
  content: string;
  createdAt: string;
};
