-- Core sample seed data for local manual testing (idempotent by fixed IDs)

-- Member personality mapping
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 201, 1, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'LOCAL_FRIEND'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 202, 1, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'PET_INFO_SHARING'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 203, 2, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'LOCAL_FRIEND'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 204, 2, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'DOG_LOVER_ONLY'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 205, 3, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'ONLINE_PET_LOVER'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 206, 3, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'PET_INFO_SHARING'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 207, 4, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'LOCAL_FRIEND'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 208, 5, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'DOG_LOVER_ONLY'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 209, 5, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'LOCAL_FRIEND'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 210, 6, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'PET_INFO_SHARING'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 211, 7, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'ONLINE_PET_LOVER'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
INSERT INTO member_personality (id, member_id, personality_type_id, created_at, updated_at)
SELECT 212, 8, mpt.id, NOW(), NOW() FROM member_personality_type mpt WHERE mpt.code = 'LOCAL_FRIEND'
ON CONFLICT (id) DO UPDATE SET member_id = EXCLUDED.member_id, personality_type_id = EXCLUDED.personality_type_id, updated_at = NOW();
