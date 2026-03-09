import { UserType, DogType, DogTendency, WalkStyle } from '../types';
import { DOG_BREEDS } from './dogBreeds';
import { PORTRAIT_FILES } from './portraits';

const TENDENCIES: DogTendency[] = ['소심해요', '에너지넘침', '간식좋아함', '사람좋아함', '친구구함', '주인바라기', '까칠해요'];
const WALK_STYLES: WalkStyle[] = ['전력질주', '냄새맡기집중', '공원벤치휴식형', '느긋함', '냄새탐정', '무한동력', '저질체력'];
const MBTIS = ['ENFP', 'INFJ', 'ISTP', 'ESTJ', 'INFP', 'ENTP', 'ESFJ', 'ISFJ'];
const MANAGER_TENDENCIES = ['동네친구', '반려견정보공유', '랜선집사', '강아지만좋아함'];

const getRandomTendencies = (): DogTendency[] => {
  return [...TENDENCIES].sort(() => 0.5 - Math.random()).slice(0, 2);
};

const getDogPortraitPath = (breedName: string) => {
  const breedInfo = DOG_BREEDS.find(b => b.ko === breedName);
  if (!breedInfo) return '/images/dog-portraits/Mixed Breed.png';
  const enName = breedInfo.en;
  const matchedFile = PORTRAIT_FILES.find(filename => filename.split('.')[0] === enName);
  return matchedFile ? `/images/dog-portraits/${matchedFile}` : '/images/dog-portraits/Mixed Breed.png';
};

const realNames = [
  "몽이아빠", "보리누나", "초코쿠키맘", "루이언니", "탄이형", "두부아빠", "다견가족", "맥스킴", "코코리", "해피바이러스",
  "이촌동보안관", "감자엄마", "쿠키대디", "사랑이네", "용산구물개", "한강산책러", "멍멍이선생", "라떼는말이야", "뽀송이언니", "대박이아빠",
  "강아지숲", "별이달이", "햇살가득", "꼬리살랑", "멍뭉미", "댕댕이친구", "이촌로산책왕", "개린이엄마", "산책중독", "힐링펫",
  "멍스타", "개밥주는남자", "꼬소한라떼", "구름발자국", "하늘이네", "토리누나", "루피형", "초보견주", "베테랑산책러", "댕댕이백과",
  "이촌동사랑꾼", "한강의기적", "개구쟁이", "순둥이네", "용맹한백구", "깜찍이맘", "튼튼아빠", "매너산책러", "평화주의자", "강아지친구"
];

const realHandles = [
  "mungchi", "bori_sis", "choco_cookie", "louie", "tan_bro", "dubu_papa", "many_dogs", "max_guard", "coco_lee", "happy_vibe",
  "ichon_guard", "potato_mom", "cookie_dad", "love_home", "yongsan_seal", "hangang_walker", "dog_teacher", "latte_king", "soft_sis", "daebak_bro",
  "dog_forest", "star_moon", "sunshine", "tail_wag", "doggy_beauty", "puppy_friend", "walk_king", "gaerini", "walk_addict", "healing_pet",
  "mung_star", "dog_man", "nutty_latte", "cloud_step", "sky_home", "tori_sis", "luffy_bro", "newbie", "pro_walker", "dog_wiki",
  "ichon_lover", "hangang_miracle", "playful", "gentle_dog", "brave_white", "cute_mom", "strong_dad", "manner_king", "peace_maker", "dog_buddy"
];

const availableBreeds = DOG_BREEDS.map(b => b.ko);

export const MOCK_USERS: UserType[] = [];

for(let i=0; i<50; i++) {
  const rand = Math.random();
  const dogCount = rand > 0.9 ? 3 : (rand > 0.7 ? 2 : 1);
  const userDogs: DogType[] = [];
  
  for(let j=1; j<=dogCount; j++) {
    const breed = availableBreeds[Math.floor(Math.random() * availableBreeds.length)];
    const portrait = getDogPortraitPath(breed);

    userDogs.push({
      id: `d${i+1}_${j}`,
      name: i < 9 ? ["몽이", "보리", "초코", "루이", "탄", "두부", "별이", "맥스", "코코"][i] : `댕댕${i}${j}`,
      breed: breed,
      age: Math.floor(Math.random()*12)+1,
      birthDate: '2020-01-01',
      gender: Math.random() > 0.5 ? 'M' : 'F',
      image: portrait,
      tendencies: getRandomTendencies(),
      walkStyle: WALK_STYLES[Math.floor(Math.random() * WALK_STYLES.length)],
      mbti: Math.random() > 0.3 ? MBTIS[Math.floor(Math.random() * MBTIS.length)] : undefined,
      isNeutralized: true,
      isMain: j === 1
    });
  }

  MOCK_USERS.push({
    id: `u${i+1}`,
    email: `user${i+1}@aini.com`,
    nickname: realNames[i],
    handle: `@${realHandles[i]}`,
    avatar: `https://picsum.photos/id/${200+i}/100/100`,
    mannerScore: parseFloat((Math.random() * 3 + 7).toFixed(1)),
    isOwner: true,
    birthDate: '1990-01-01',
    age: Math.floor(Math.random() * 30) + 20,
    gender: Math.random() > 0.5 ? 'M' : 'F',
    mbti: Math.random() > 0.5 ? MBTIS[Math.floor(Math.random() * MBTIS.length)] : undefined,
    about: `이촌동에서 활동하는 ${realNames[i]}입니다. 매너 있는 산책 함께해요!`,
    tendencies: [MANAGER_TENDENCIES[Math.floor(Math.random() * MANAGER_TENDENCIES.length)]],
    location: i === 0 ? "서울시 이촌동" : `서울시 이촌동 ${Math.floor(Math.random()*50)+1}길`,
    dogs: userDogs,
    followerCount: Math.floor(Math.random() * 100),
    followingCount: Math.floor(Math.random() * 100)
  });
}
