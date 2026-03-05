-- Lookup seed data (idempotent)

-- Breed
INSERT INTO breed (id, name, size, created_at, updated_at) VALUES
    (1, '말티즈', 'SMALL', NOW(), NOW()),
    (2, '푸들', 'SMALL', NOW(), NOW()),
    (3, '비숑 프리제', 'SMALL', NOW(), NOW()),
    (15, '포메라니안', 'SMALL', NOW(), NOW()),
    (20, '골든 리트리버', 'LARGE', NOW(), NOW()),
    (21, '시베리안 허스키', 'LARGE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    size = EXCLUDED.size,
    updated_at = NOW();

-- Personality
INSERT INTO personality (id, name, code, created_at, updated_at) VALUES
    (1, '소심해요', 'SHY', NOW(), NOW()),
    (2, '에너지넘침', 'ENERGETIC', NOW(), NOW()),
    (3, '간식좋아함', 'TREAT_LOVER', NOW(), NOW()),
    (4, '사람좋아함', 'PEOPLE_LOVER', NOW(), NOW()),
    (5, '친구구함', 'SEEKING_FRIENDS', NOW(), NOW()),
    (6, '주인바라기', 'OWNER_FOCUSED', NOW(), NOW()),
    (7, '까칠해요', 'GRUMPY', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    code = EXCLUDED.code,
    updated_at = NOW();

-- Walking Style
INSERT INTO walking_style (id, name, code, created_at, updated_at) VALUES
    (1, '전력질주', 'ENERGY_BURST', NOW(), NOW()),
    (2, '냄새맡기집중', 'SNIFF_EXPLORER', NOW(), NOW()),
    (3, '공원벤치휴식형', 'BENCH_REST', NOW(), NOW()),
    (4, '느긋함', 'RELAXED', NOW(), NOW()),
    (5, '냄새탐정', 'SNIFF_DETECTIVE', NOW(), NOW()),
    (6, '무한동력', 'ENDLESS_ENERGY', NOW(), NOW()),
    (7, '저질체력', 'LOW_STAMINA', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    code = EXCLUDED.code,
    updated_at = NOW();

-- Member personality types (code-based upsert)
INSERT INTO member_personality_type (name, code, created_at, updated_at) VALUES
    ('동네친구', 'LOCAL_FRIEND', NOW(), NOW()),
    ('반려견정보공유', 'PET_INFO_SHARING', NOW(), NOW()),
    ('랜선집사', 'ONLINE_PET_LOVER', NOW(), NOW()),
    ('강아지만좋아함', 'DOG_LOVER_ONLY', NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    updated_at = NOW();
