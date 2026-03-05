import { http, HttpResponse } from 'msw';
import { MOCK_FEEDS, MOCK_THREADS, MOCK_CHAT_ROOMS, MOCK_WALK_DIARIES, MOCK_USERS } from '@/constants';
import { UserType, DogType, DogTendency } from '@/types';

// --- 가상 DB 엔진 (V6.1: Full Coverage) ---
const DB_KEY = 'aini_inu_v6_db';

const seedDatabase = () => {
  const neighborData = [
    { name: '보리누나',   breed: '말티즈',      img: 'Maltese.jpg',        loc: '서울시 성수동',       gender: 'F', mbti: 'ENFP', score: 9.5, avatar: 100, age: 28, dogGender: 'F' },
    { name: '초코아빠',   breed: '푸들',         img: 'Poodle.jpg',          loc: '서울시 이촌동',       gender: 'M', mbti: 'ISTJ', score: 9.2, avatar: 101, age: 32, dogGender: 'M' },
    { name: '해피매니저', breed: '시바이누',     img: 'Shiba Inu.jpg',       loc: '서울시 마포구',       gender: 'M', mbti: 'ENTP', score: 8.8, avatar: 102, age: 27, dogGender: 'M' },
    { name: '몽이파파',   breed: '비글',         img: 'Beagle.jpg',          loc: '서울시 용산구',       gender: 'M', mbti: 'ESFJ', score: 9.0, avatar: 103, age: 35, dogGender: 'M' },
    { name: '루이언니',   breed: '포메라니안',   img: 'Pomeranian.jpg',      loc: '서울시 강남구',       gender: 'F', mbti: 'INFP', score: 8.5, avatar: 104, age: 26, dogGender: 'F' },
    { name: '두부맘',     breed: '골든리트리버', img: 'Golden Retriever.jpg', loc: '서울시 서대문구',     gender: 'F', mbti: 'ISFJ', score: 9.3, avatar: 105, age: 30, dogGender: 'M' },
    { name: '별이달이',   breed: '웰시코기',     img: 'Welsh Corgi.jpg',     loc: '서울시 송파구',       gender: 'F', mbti: 'ENFJ', score: 8.7, avatar: 106, age: 29, dogGender: 'F' },
    { name: '맥스가디언', breed: '사모예드',     img: 'Samoyed.jpg',         loc: '서울시 강동구',       gender: 'M', mbti: 'ESTP', score: 9.1, avatar: 107, age: 31, dogGender: 'M' },
    { name: '코코리',     breed: '비숑프리제',   img: 'Bichon Frise.jpg',    loc: '서울시 은평구',       gender: 'F', mbti: 'INFJ', score: 8.9, avatar: 108, age: 25, dogGender: 'F' },
    { name: '힐링펫',     breed: '치와와',       img: 'Chihuahua.jpg',       loc: '서울시 종로구',       gender: 'M', mbti: 'INTJ', score: 8.6, avatar: 109, age: 33, dogGender: 'M' },
  ];

  const walkStyles: DogType['walkStyle'][] = ['느긋함', '냄새맡기집중', '전력질주', '무한동력', '냄새탐정', '공원벤치휴식형', '저질체력', '느긋함', '전력질주', '냄새탐정'];

  const neighbors: UserType[] = neighborData.map((data, i) => {
    const avatarUrl = `https://api.dicebear.com/7.x/avataaars/svg?seed=neighbor-${i + 1}`;

    return {
      id: `neighbor-${i + 1}`,
      email: `neighbor${i + 1}@aini.com`,
      nickname: data.name,
      handle: `@neighbor_${i + 1}`,
      avatar: avatarUrl,
      mannerScore: data.score,
    isOwner: true,
    age: data.age,
    birthDate: '1998-01-01',
    gender: data.gender as 'M' | 'F',
    location: data.loc,
    about: `${data.name}입니다. 함께 즐겁게 산책해요!`,
    mbti: data.mbti,
    followerCount: 80 + i * 15,
    followingCount: 60 + i * 10,
    dogs: [{
      id: `d-n-${i}`,
      name: data.name.substring(0, 2),
      breed: data.breed,
      age: 2 + (i % 4),
      birthDate: '2021-05-05',
      gender: data.dogGender as 'M' | 'F',
      image: `/images/dog-portraits/${data.img}`,
      tendencies: ['사람좋아함', '친구구함'] as DogTendency[],
      walkStyle: walkStyles[i],
      isNeutralized: true,
      isVerified: false,
      isMain: true,
    }],
  };
  });

  // 초기 팔로우 관계 시드
  const follows = [
    { followerId: 'neighbor-1', followingId: 'neighbor-2' },
    { followerId: 'neighbor-1', followingId: 'neighbor-3' },
    { followerId: 'neighbor-2', followingId: 'neighbor-1' },
    { followerId: 'neighbor-2', followingId: 'neighbor-3' },
    { followerId: 'neighbor-3', followingId: 'neighbor-1' },
    { followerId: 'neighbor-3', followingId: 'neighbor-2' },
    { followerId: 'neighbor-1', followingId: 'neighbor-4' },
    { followerId: 'neighbor-4', followingId: 'neighbor-1' },
    { followerId: 'neighbor-2', followingId: 'neighbor-5' },
    { followerId: 'neighbor-5', followingId: 'neighbor-2' },
  ];

  return {
    users: [...MOCK_USERS, ...neighbors],
    currentUserId: null,
    follows,
    reviews: [] as { revieweeId: string; reviewerId: string; score: number; createdAt: string }[],
    feeds: MOCK_FEEDS.map((f, i) => ({ ...f, author: neighbors[i % neighbors.length] })),
    threads: MOCK_THREADS.map((t, i) => ({ ...t, author: neighbors[i % neighbors.length] })),
    chatRooms: MOCK_CHAT_ROOMS.map((r, i) => ({ ...r, partner: neighbors[i % neighbors.length] })),
    diaries: {}
  };
};

const getDB = () => {
  if (typeof window === 'undefined') return seedDatabase();
  const saved = localStorage.getItem(DB_KEY);
  if (saved) {
    const db = JSON.parse(saved);
    if (db.users?.[0] && !db.users[0].email) { // 구버전 데이터 초기화
      const fresh = seedDatabase();
      localStorage.setItem(DB_KEY, JSON.stringify(fresh));
      return fresh;
    }
    // 필드 마이그레이션: 구버전 DB에 follows 없으면 seed 값 추가
    if (!db.follows) {
      const seed = seedDatabase();
      db.follows = seed.follows;
      localStorage.setItem(DB_KEY, JSON.stringify(db));
    }
    // 필드 마이그레이션: reviews 배열 없으면 초기화
    if (!db.reviews) {
      db.reviews = [];
      localStorage.setItem(DB_KEY, JSON.stringify(db));
    }
    return db;
  }
  const fresh = seedDatabase();
  localStorage.setItem(DB_KEY, JSON.stringify(fresh));
  return fresh;
};

const saveDB = (db: any) => {
  if (typeof window !== 'undefined') localStorage.setItem(DB_KEY, JSON.stringify(db));
};

export const handlers = [
  // 1. 멤버 세션
  http.get('/api/v1/members/me', () => {
    const db = getDB();
    const user = db.users.find((u: any) => u.id === db.currentUserId);
    if (!user) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });
    return HttpResponse.json({ success: true, data: user });
  }),

  // 2. 가입
  http.post('/api/v1/members/signup', async ({ request }) => {
    const data = await request.json() as any;
    const db = getDB();
    
    if (!data.email) return HttpResponse.json({ success: false, message: '이메일 정보가 누락되었습니다.' }, { status: 400 });

    const isDuplicate = (db.users || []).some((u: any) => 
      u.email && u.email.toLowerCase() === data.email.toLowerCase()
    );
    if (isDuplicate) return HttpResponse.json({ success: false, message: '이미 가입된 이메일입니다.' }, { status: 409 });

    const newId = `u-${Date.now()}`;
    const newUser: UserType = { 
      ...data, 
      id: newId, 
      email: data.email.toLowerCase(), // 이메일 강제 저장 및 소문화
      handle: `@${data.nickname || 'user'}_${Math.floor(Math.random()*1000)}`,
      mannerScore: 5.0, 
      isOwner: true, 
      followerCount: 0, 
      followingCount: 0,
      avatar: data.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${newId}`,
      dogs: (data.dogs || []).map((d: any, idx: number) => ({
        ...d,
        id: `d-${newId}-${idx}`,
        image: d.image || `/images/dog-portraits/Mixed Breed.png`,
        tendencies: d.tendencies || [],
        walkStyle: d.walkStyle || '느긋함',
        isNeutralized: d.isNeutralized ?? true,
        isVerified: d.isVerified || false,
        isMain: idx === 0
      }))
    };

    db.users.push(newUser);
    db.currentUserId = newId; 
    saveDB(db);
    return HttpResponse.json({ success: true, data: newUser });
  }),

  // 3. 로그인
  http.post('/api/v1/auth/login', async ({ request }) => {
    const { email } = await request.json() as any;
    const db = getDB();
    
    // 더 견고한 이메일 검색 로직
    const user = db.users.find((u: any) => 
      u.email && email && u.email.toLowerCase() === email.toLowerCase()
    );

    if (user) {
      db.currentUserId = user.id;
      saveDB(db);
      return HttpResponse.json({ success: true, data: user });
    }
    return HttpResponse.json({ success: false, message: '가입되지 않은 이메일입니다.' }, { status: 404 });
  }),

  // 4. 멤버 프로필 수정
  http.put('/api/v1/members/me', async ({ request }) => {
    const data = await request.json() as any;
    const db = getDB();
    const idx = db.users.findIndex((u: any) => u.id === db.currentUserId);
    if (idx === -1) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });

    // 닉네임 30일 쿨다운 검증
    if (data.nickname && data.nickname !== db.users[idx].nickname) {
      const changedAt = db.users[idx].nicknameChangedAt;
      if (changedAt) {
        const daysPassed = (Date.now() - new Date(changedAt).getTime()) / (1000 * 60 * 60 * 24);
        if (daysPassed < 30) {
          const remaining = Math.ceil(30 - daysPassed);
          return HttpResponse.json({ success: false, message: `닉네임은 ${remaining}일 후에 변경할 수 있습니다.` }, { status: 429 });
        }
      }
      data.nicknameChangedAt = new Date().toISOString();
    }

    db.users[idx] = { ...db.users[idx], ...data };
    saveDB(db);
    return HttpResponse.json({ success: true, data: db.users[idx] });
  }),

  // 5. 산책 통계 (잔디 히트맵)
  http.get('/api/v1/members/me/stats/walk', () => {
    const db = getDB();
    const user = db.users.find((u: any) => u.id === db.currentUserId);
    if (!user) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });
    // 126칸 (18주 × 7일) 시드 기반 산책 데이터 생성
    const seed = user.id.split('').reduce((acc: number, c: string) => acc + c.charCodeAt(0), 0);
    const stats = Array.from({ length: 126 }, (_, i) => {
      const rand = Math.abs(Math.sin(seed + i) * 10000) % 1;
      if (rand < 0.45) return 0;
      if (rand < 0.70) return 1;
      if (rand < 0.90) return 2;
      return 3;
    });
    return HttpResponse.json({ success: true, data: stats });
  }),

  // 6. 멤버 전체 검색 (GET /members?q=검색어)
  http.get('/api/v1/members', ({ request }) => {
    const url = new URL(request.url);
    const q = (url.searchParams.get('q') || '').toLowerCase();
    const db = getDB();
    let users = (db.users as any[]).filter((u: any) => u.id !== db.currentUserId);
    if (q) {
      const stripped = q.startsWith('@') ? q.slice(1) : q;
      users = users.filter((u: any) =>
        u.nickname?.toLowerCase().includes(stripped) ||
        u.handle?.toLowerCase().replace('@', '').includes(stripped)
      );
    }
    return HttpResponse.json({ success: true, data: users.slice(0, 30) });
  }),

  // 7. 팔로워 / 팔로잉
  http.get('/api/v1/members/me/followers', () => {
    const db = getDB();
    const follows = db.follows || [];
    const followerIds = follows
      .filter((f: any) => f.followingId === db.currentUserId)
      .map((f: any) => f.followerId);
    const followers = db.users.filter((u: any) => followerIds.includes(u.id));
    // currentUserId가 없으면 전체 이웃 반환 (비로그인 시 fallback)
    if (!db.currentUserId) return HttpResponse.json({ success: true, data: db.users });
    return HttpResponse.json({ success: true, data: followers });
  }),

  http.get('/api/v1/members/me/following', () => {
    const db = getDB();
    const follows = db.follows || [];
    const followingIds = follows
      .filter((f: any) => f.followerId === db.currentUserId)
      .map((f: any) => f.followingId);
    const following = db.users.filter((u: any) => followingIds.includes(u.id));
    if (!db.currentUserId) return HttpResponse.json({ success: true, data: db.users });
    return HttpResponse.json({ success: true, data: following });
  }),

  http.post('/api/v1/members/me/follow/:targetId', ({ params }) => {
    const db = getDB();
    if (!db.currentUserId) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });
    if (db.currentUserId === params.targetId) {
      return HttpResponse.json({ success: false, message: '자기 자신을 팔로우할 수 없습니다.' }, { status: 400 });
    }
    db.follows = db.follows || [];
    const alreadyFollowing = db.follows.some(
      (f: any) => f.followerId === db.currentUserId && f.followingId === params.targetId
    );
    if (!alreadyFollowing) {
      db.follows.push({ followerId: db.currentUserId, followingId: params.targetId });
      // followerCount / followingCount 업데이트
      const me = db.users.find((u: any) => u.id === db.currentUserId);
      const target = db.users.find((u: any) => u.id === params.targetId);
      if (me) me.followingCount = (me.followingCount || 0) + 1;
      if (target) target.followerCount = (target.followerCount || 0) + 1;
      saveDB(db);
    }
    return HttpResponse.json({ success: true, data: { isFollowing: true } });
  }),

  http.delete('/api/v1/members/me/follow/:targetId', ({ params }) => {
    const db = getDB();
    if (!db.currentUserId) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });
    db.follows = db.follows || [];
    const before = db.follows.length;
    db.follows = db.follows.filter(
      (f: any) => !(f.followerId === db.currentUserId && f.followingId === params.targetId)
    );
    if (db.follows.length < before) {
      const me = db.users.find((u: any) => u.id === db.currentUserId);
      const target = db.users.find((u: any) => u.id === params.targetId);
      if (me) me.followingCount = Math.max(0, (me.followingCount || 1) - 1);
      if (target) target.followerCount = Math.max(0, (target.followerCount || 1) - 1);
      saveDB(db);
    }
    return HttpResponse.json({ success: true, data: { isFollowing: false } });
  }),

  // 7. 강아지 관리
  http.get('/api/v1/members/me/dogs', () => {
    const db = getDB();
    const user = db.users.find((u: any) => u.id === db.currentUserId);
    return HttpResponse.json({ success: true, data: user?.dogs || [] });
  }),

  http.post('/api/v1/members/me/dogs', async ({ request }) => {
    const data = await request.json() as any;
    const db = getDB();
    const idx = db.users.findIndex((u: any) => u.id === db.currentUserId);
    if (idx === -1) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });
    const newDog: DogType = {
      ...data,
      id: `d-${db.currentUserId}-${Date.now()}`,
      image: data.image || `/images/dog-portraits/Mixed Breed.png`,
      tendencies: data.tendencies || [],
      walkStyle: data.walkStyle || '느긋함',
      isNeutralized: data.isNeutralized ?? true,
      isVerified: data.isVerified || false,
      isMain: (db.users[idx].dogs || []).length === 0,
    };
    db.users[idx].dogs = [...(db.users[idx].dogs || []), newDog];
    saveDB(db);
    return HttpResponse.json({ success: true, data: newDog });
  }),

  http.put('/api/v1/members/me/dogs/:dogId', async ({ params, request }) => {
    const data = await request.json() as any;
    const db = getDB();
    const idx = db.users.findIndex((u: any) => u.id === db.currentUserId);
    if (idx === -1) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });
    const dogIdx = (db.users[idx].dogs || []).findIndex((d: any) => d.id === params.dogId);
    if (dogIdx === -1) return HttpResponse.json({ success: false, message: '강아지를 찾을 수 없습니다.' }, { status: 404 });
    db.users[idx].dogs[dogIdx] = { ...db.users[idx].dogs[dogIdx], ...data };
    saveDB(db);
    return HttpResponse.json({ success: true, data: db.users[idx].dogs[dogIdx] });
  }),

  // 타인 프로필 강아지 조회 (GET /members/:memberId 보다 먼저 등록되어야 함)
  http.get('/api/v1/members/:memberId/dogs', ({ params }) => {
    const db = getDB();
    const user = db.users.find((u: any) => u.id === params.memberId);
    return HttpResponse.json({ success: true, data: user?.dogs || [] });
  }),

  http.delete('/api/v1/members/me/dogs/:dogId', ({ params }) => {
    const db = getDB();
    const idx = db.users.findIndex((u: any) => u.id === db.currentUserId);
    if (idx === -1) return HttpResponse.json({ success: false, message: '로그인이 필요합니다.' }, { status: 401 });
    db.users[idx].dogs = (db.users[idx].dogs || []).filter((d: any) => d.id !== params.dogId);
    saveDB(db);
    return HttpResponse.json({ success: true });
  }),

  // 8. 스레드 (모집 및 조회)
  http.get('/api/v1/threads', ({ request }) => {
    const url = new URL(request.url);
    const lat = parseFloat(url.searchParams.get('lat') || '');
    const lng = parseFloat(url.searchParams.get('lng') || '');
    const db = getDB();

    if (isNaN(lat) || isNaN(lng)) {
      return HttpResponse.json({ success: true, data: db.threads });
    }

    // Haversine 공식으로 5km 이내 스레드 필터링
    const toRad = (d: number) => d * Math.PI / 180;
    const R = 6371;
    const nearby = db.threads.filter((t: any) => {
      if (!t.lat || !t.lng) return true; // 좌표 없는 스레드는 항상 포함
      const dLat = toRad(t.lat - lat);
      const dLng = toRad(t.lng - lng);
      const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat)) * Math.cos(toRad(t.lat)) * Math.sin(dLng / 2) ** 2;
      return 2 * R * Math.asin(Math.sqrt(a)) <= 5;
    });
    return HttpResponse.json({ success: true, data: nearby });
  }),
  
  http.post('/api/v1/threads', async ({ request }) => {
    const body = await request.json() as any;
    if (!body.title || !body.description) {
      return HttpResponse.json({ success: false, message: '제목과 설명은 필수입니다.' }, { status: 400 });
    }
    const db = getDB();
    const user = db.users.find((u: any) => u.id === db.currentUserId);
    const newThread = {
      ...body,
      id: `t-${Date.now()}`,
      author: user || db.users[0],
      currentParticipants: 1,
      isJoined: true
    };
    db.threads = [newThread, ...db.threads];
    saveDB(db);
    return HttpResponse.json({ success: true, data: newThread });
  }),

  http.put('/api/v1/threads/:id', async ({ params, request }) => {
    const db = getDB();
    const threadIdx = db.threads.findIndex((t: any) => t.id === params.id);
    if (threadIdx === -1) return HttpResponse.json({ success: false, message: '스레드를 찾을 수 없습니다.' }, { status: 404 });
    if (db.threads[threadIdx].author?.id !== db.currentUserId) {
      return HttpResponse.json({ success: false, message: '수정 권한이 없습니다.' }, { status: 403 });
    }
    const body = await request.json() as any;
    db.threads[threadIdx] = { ...db.threads[threadIdx], ...body };
    saveDB(db);
    return HttpResponse.json({ success: true, data: db.threads[threadIdx] });
  }),

  http.delete('/api/v1/threads/:id', ({ params }) => {
    const db = getDB();
    const thread = db.threads.find((t: any) => t.id === params.id);
    if (!thread) return HttpResponse.json({ success: false, message: '스레드를 찾을 수 없습니다.' }, { status: 404 });
    if (thread.author?.id !== db.currentUserId) {
      return HttpResponse.json({ success: false, message: '삭제 권한이 없습니다.' }, { status: 403 });
    }
    db.threads = db.threads.filter((t: any) => t.id !== params.id);
    saveDB(db);
    return HttpResponse.json({ success: true, message: '삭제되었습니다.' });
  }),

  http.post('/api/v1/threads/:id/join', ({ params }) => {
    const db = getDB();
    const thread = db.threads.find((t: any) => t.id === params.id);
    if (!thread) return HttpResponse.json({ success: false, message: '스레드를 찾을 수 없습니다.' }, { status: 404 });

    thread.joinedUsers = thread.joinedUsers || [];
    if (thread.joinedUsers.includes(db.currentUserId)) {
      return HttpResponse.json({ success: false, message: '이미 참여한 산책입니다.' }, { status: 409 });
    }
    thread.joinedUsers.push(db.currentUserId);
    thread.currentParticipants = (thread.currentParticipants || 0) + 1;
    thread.isJoined = true;
    saveDB(db);
    return HttpResponse.json({ success: true, message: 'Joined successfully' });
  }),

  http.delete('/api/v1/threads/:id/join', ({ params }) => {
    const db = getDB();
    const thread = db.threads.find((t: any) => t.id === params.id);
    if (!thread) return HttpResponse.json({ success: false, message: '스레드를 찾을 수 없습니다.' }, { status: 404 });

    thread.joinedUsers = thread.joinedUsers || [];
    if (!thread.joinedUsers.includes(db.currentUserId)) {
      return HttpResponse.json({ success: false, message: '참여하지 않은 산책입니다.' }, { status: 400 });
    }
    thread.joinedUsers = thread.joinedUsers.filter((uid: string) => uid !== db.currentUserId);
    thread.currentParticipants = Math.max(0, (thread.currentParticipants || 1) - 1);
    thread.isJoined = false;
    saveDB(db);
    return HttpResponse.json({ success: true, message: '참여가 취소되었습니다.' });
  }),

  http.get('/api/v1/threads/hotspot', ({ request }) => {
    const hours = parseInt(new URL(request.url).searchParams.get('hours') || '3', 10);
    // hours가 클수록 더 많은 활동 집계 (mock)
    const count = Math.min(10 + hours * 3, 99);
    return HttpResponse.json({ success: true, data: { region: '성수동 서울숲', count } });
  }),

  // 6. 산책 일기
  http.get('/api/v1/walk-diaries', ({ request }) => {
    const memberId = new URL(request.url).searchParams.get('memberId');
    if (!memberId) return HttpResponse.json({ success: true, data: MOCK_WALK_DIARIES });
    const filtered = Object.fromEntries(
      Object.entries(MOCK_WALK_DIARIES).filter(([, diary]: [string, any]) => diary.authorId === memberId)
    );
    return HttpResponse.json({ success: true, data: filtered });
  }),
  http.get('/api/v1/walk-diaries/following', () => {
    const db = getDB();
    const follows = db.follows || [];
    const followingIds = follows
      .filter((f: any) => f.followerId === db.currentUserId)
      .map((f: any) => f.followingId);
    // 팔로잉 중인 유저의 공개 일기만 반환. 팔로잉이 없으면 전체 반환 (신규 유저 fallback)
    if (followingIds.length === 0) return HttpResponse.json({ success: true, data: MOCK_WALK_DIARIES });
    const filtered = Object.fromEntries(
      Object.entries(MOCK_WALK_DIARIES).filter(([, diary]: [string, any]) =>
        followingIds.includes(diary.authorId)
      )
    );
    return HttpResponse.json({ success: true, data: filtered });
  }),

  http.post('/api/v1/walk-diaries/:id', async ({ request, params }) => {
    const body = await request.json() as any;
    const db = getDB();
    db.diaries = db.diaries || {};
    db.diaries[params.id as string] = { ...body, id: params.id };
    saveDB(db);
    return HttpResponse.json({ success: true, data: db.diaries[params.id as string] });
  }),

  // 7. 피드 및 댓글
  http.get('/api/v1/posts', ({ request }) => {
    const url = new URL(request.url);
    const memberId = url.searchParams.get('memberId');
    const location = url.searchParams.get('location');
    const db = getDB();
    let feeds = db.feeds as any[];
    if (memberId) {
      feeds = feeds.filter((f: any) => f.author?.id === memberId);
    }
    if (location) {
      const keyword = location.split(' ').slice(0, 2).join(' ');
      feeds = feeds.filter((f: any) => !f.location || f.location.includes(keyword));
    }
    return HttpResponse.json({ success: true, data: feeds });
  }),
  
  http.post('/api/v1/posts', async ({ request }) => {
    const body = await request.json() as any;
    const db = getDB();
    const user = db.users.find((u: any) => u.id === db.currentUserId);
    const newPost = { id: `f-${Date.now()}`, author: user || db.users[0], images: body.images || ['/AINIINU_ROGO_B.png'], caption: body.caption, likes: 0, comments: 0, createdAt: '방금 전' };
    db.feeds = [newPost, ...db.feeds];
    saveDB(db);
    return HttpResponse.json({ success: true, data: newPost });
  }),

  http.post('/api/v1/posts/:postId/comments', async ({ request, params }) => {
    const { content } = await request.json() as any;
    const db = getDB();
    const user = db.users.find((u: any) => u.id === db.currentUserId) || db.users[0];
    const newComment = {
      id: `c-${Date.now()}`,
      author: { id: user.id, nickname: user.nickname, avatar: user.avatar },
      content,
      createdAt: new Date().toISOString()
    };
    
    const feed = db.feeds.find((f: any) => f.id === params.postId);
    if (feed) {
      feed.commentsList = [newComment, ...(feed.commentsList || [])];
      feed.commentCount = (feed.commentCount || 0) + 1;
      saveDB(db);
    }
    return HttpResponse.json({ success: true, data: newComment });
  }),

  // 피드 수정 / 삭제 / 좋아요 / 댓글 조회
  http.put('/api/v1/posts/:postId', async ({ request, params }) => {
    const body = await request.json() as any;
    const db = getDB();
    const idx = db.feeds.findIndex((f: any) => f.id === params.postId);
    if (idx === -1) return HttpResponse.json({ success: false, message: '게시글을 찾을 수 없습니다.' }, { status: 404 });
    db.feeds[idx] = { ...db.feeds[idx], ...body };
    saveDB(db);
    return HttpResponse.json({ success: true, data: db.feeds[idx] });
  }),

  http.delete('/api/v1/posts/:postId', ({ params }) => {
    const db = getDB();
    db.feeds = db.feeds.filter((f: any) => f.id !== params.postId);
    saveDB(db);
    return HttpResponse.json({ success: true });
  }),

  http.post('/api/v1/posts/:postId/like', ({ params }) => {
    const db = getDB();
    const feed = db.feeds.find((f: any) => f.id === params.postId);
    if (!feed) return HttpResponse.json({ success: false, message: '게시글을 찾을 수 없습니다.' }, { status: 404 });
    feed.likedBy = feed.likedBy || [];
    const alreadyLiked = feed.likedBy.includes(db.currentUserId);
    if (alreadyLiked) {
      feed.likedBy = feed.likedBy.filter((id: string) => id !== db.currentUserId);
      feed.likes = Math.max(0, (feed.likes || 0) - 1);
      feed.isLiked = false;
    } else {
      feed.likedBy.push(db.currentUserId);
      feed.likes = (feed.likes || 0) + 1;
      feed.isLiked = true;
    }
    saveDB(db);
    return HttpResponse.json({ success: true, data: { likes: feed.likes, isLiked: feed.isLiked } });
  }),

  http.get('/api/v1/posts/:postId/comments', ({ params }) => {
    const db = getDB();
    const feed = db.feeds.find((f: any) => f.id === params.postId);
    return HttpResponse.json({ success: true, data: feed?.commentsList || [] });
  }),

  // 9. 채팅 (방 생성 및 메시지 전송)
  http.get('/api/v1/chat/rooms', () => HttpResponse.json({ success: true, data: getDB().chatRooms })),
  
  http.post('/api/v1/chat/rooms', async ({ request }) => {
    const { partnerId } = await request.json() as any;
    const db = getDB();
    let room = db.chatRooms.find((r: any) => r.partner.id === partnerId);
    
    if (!room) {
      const partner = db.users.find((u: any) => u.id === partnerId) || db.users[0];
      room = {
        id: `c-${Date.now()}`,
        partner,
        messages: [],
        lastMessage: '대화를 시작해보세요!',
        unreadCount: 0,
        lastMessageTime: '방금'
      };
      db.chatRooms = [room, ...db.chatRooms];
      saveDB(db);
    }
    return HttpResponse.json({ success: true, data: room });
  }),

  http.get('/api/v1/chat/rooms/:roomId/messages', ({ params }) => {
    const db = getDB();
    const room = db.chatRooms.find((r: any) => r.id === params.roomId);
    return HttpResponse.json({ success: true, data: room?.messages || [] });
  }),

  http.post('/api/v1/chat/rooms/:roomId/messages', async ({ request, params }) => {
    const { content } = await request.json() as any;
    const db = getDB();
    const user = db.users.find((u: any) => u.id === db.currentUserId);
    
    const room = db.chatRooms.find((r: any) => r.id === params.roomId);
    if (room) {
      const newMessage = {
        id: `m-${Date.now()}`,
        senderId: user?.id,
        text: content,
        type: 'TALK',
        timestamp: new Date().toISOString(),
        time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
      };
      room.messages = [...(room.messages || []), newMessage];
      room.lastMessage = content;
      room.lastMessageTime = '방금';
      saveDB(db);
      return HttpResponse.json({ success: true, data: newMessage });
    }
    return HttpResponse.json({ success: false, message: 'Chat room not found' }, { status: 404 });
  }),

  // 10. 스토리 — 24시간 이내 다이어리를 작성한 유저만 노출
  http.get('/api/v1/stories', () => {
    const db = getDB();
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;

    // 24시간 이내 다이어리 작성자 ID 수집
    const recentAuthorIds = new Set<string>();

    // MOCK_WALK_DIARIES (walkDate 형식: 'YYYY.MM.DD')
    Object.values(MOCK_WALK_DIARIES).forEach((d: any) => {
      if (d.walkDate) {
        const parsed = new Date(d.walkDate.replace(/\./g, '-'));
        if (!isNaN(parsed.getTime()) && now - parsed.getTime() <= oneDayMs) {
          recentAuthorIds.add(d.authorId);
        }
      }
    });

    // 런타임 db.diaries (createdAt ISO 형식)
    Object.values(db.diaries || {}).forEach((d: any) => {
      const ts = d.createdAt ? new Date(d.createdAt).getTime() : 0;
      if (ts && now - ts <= oneDayMs) recentAuthorIds.add(d.authorId);
    });

    const stories = db.users
      .filter((u: any) => u.id !== db.currentUserId && recentAuthorIds.has(u.id))
      .map((u: any) => ({
        id: `story-${u.id}`,
        user: { id: u.id, nickname: u.nickname, avatar: u.avatar },
        thumbnail: u.avatar,
      }));

    return HttpResponse.json({ success: true, data: stories });
  }),

  // 11. 리뷰
  http.post('/api/v1/members/:partnerId/reviews', async ({ request, params }) => {
    const data = await request.json() as any;
    const db = getDB();
    const revieweeId = params.partnerId as string;
    const reviewerId = db.currentUserId;

    if (reviewerId && data.score) {
      db.reviews = db.reviews || [];
      // 동일 리뷰어의 중복 리뷰는 덮어씀
      db.reviews = db.reviews.filter((r: any) => !(r.revieweeId === revieweeId && r.reviewerId === reviewerId));
      db.reviews.push({ revieweeId, reviewerId, score: data.score, createdAt: new Date().toISOString() });

      // mannerScore 재계산 후 user 업데이트
      const userReviews = db.reviews.filter((r: any) => r.revieweeId === revieweeId);
      const avg = userReviews.reduce((sum: number, r: any) => sum + r.score, 0) / userReviews.length;
      const idx = db.users.findIndex((u: any) => u.id === revieweeId);
      if (idx !== -1) db.users[idx].mannerScore = Math.round(avg * 10) / 10;

      saveDB(db);
    }
    return HttpResponse.json({ success: true });
  }),

  // 12. 인증번호 및 기타
  http.post('/api/v1/auth/email/send', () => HttpResponse.json({ success: true })),
  http.post('/api/v1/auth/email/verify', async ({ request }) => {
    const { code } = await request.json() as any;
    return code === '1234' ? HttpResponse.json({ success: true }) : HttpResponse.json({ success: false, message: '인증번호 오답' }, { status: 400 });
  }),
  http.get('/api/v1/members/:memberId', ({ params }) => {
    const db = getDB();
    const user = db.users.find((u: any) => u.id === params.memberId);
    if (!user) return HttpResponse.json({ success: false, message: '존재하지 않는 사용자입니다.' }, { status: 404 });

    // reviews 기반으로 mannerScore 재계산
    const userReviews = (db.reviews || []).filter((r: any) => r.revieweeId === user.id);
    const mannerScore = userReviews.length > 0
      ? Math.round(userReviews.reduce((sum: number, r: any) => sum + r.score, 0) / userReviews.length * 10) / 10
      : user.mannerScore;

    return HttpResponse.json({ success: true, data: { ...user, mannerScore } });
  }),
];
