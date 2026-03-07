-- Core sample seed data for local manual testing (idempotent by fixed IDs)

-- Members
INSERT INTO member (
    id, email, nickname, profile_image_url, member_type, status, linked_nickname,
    phone, age, gender, mbti, personality, self_introduction, is_verified,
    manner_score_sum, manner_score_count, created_at, updated_at
) VALUES
    (1, 'owner01@test.com', 'owner01', 'https://cdn.ainiinu.dev/member/1.jpg', 'PET_OWNER', 'ACTIVE', '오너원', '010-1000-0001', 29, 'MALE', 'ENFP', '활발함', '한강 근처에서 자주 산책해요', true, 45, 9, NOW(), NOW()),
    (2, 'owner02@test.com', 'owner02', 'https://cdn.ainiinu.dev/member/2.jpg', 'PET_OWNER', 'ACTIVE', '오너투', '010-1000-0002', 33, 'FEMALE', 'INTJ', '차분함', '아침 산책을 선호해요', true, 41, 8, NOW(), NOW()),
    (3, 'owner03@test.com', 'owner03', 'https://cdn.ainiinu.dev/member/3.jpg', 'PET_OWNER', 'ACTIVE', '오너쓰리', '010-1000-0003', 31, 'FEMALE', 'ISFP', '친절함', '주말에 공원 산책해요', true, 38, 8, NOW(), NOW()),
    (4, 'viewer04@test.com', 'viewer04', 'https://cdn.ainiinu.dev/member/4.jpg', 'NON_PET_OWNER', 'ACTIVE', '뷰어포', '010-1000-0004', 27, 'MALE', 'ISTP', '관찰형', '산책 메이트를 찾는 중이에요', true, 20, 4, NOW(), NOW()),
    (5, 'finder05@test.com', 'finder05', 'https://cdn.ainiinu.dev/member/5.jpg', 'PET_OWNER', 'ACTIVE', '파인더오', '010-1000-0005', 36, 'MALE', 'ENTP', '외향적', '동네 강아지를 자주 돌봐요', true, 32, 7, NOW(), NOW()),
    (6, 'finder06@test.com', 'finder06', 'https://cdn.ainiinu.dev/member/6.jpg', 'PET_OWNER', 'ACTIVE', '파인더육', '010-1000-0006', 30, 'FEMALE', 'INFJ', '신중함', '퇴근 후 산책 루틴이 있어요', true, 28, 6, NOW(), NOW()),
    (7, 'comm07@test.com', 'comm07', 'https://cdn.ainiinu.dev/member/7.jpg', 'NON_PET_OWNER', 'ACTIVE', '커뮤칠', '010-1000-0007', 25, 'UNKNOWN', 'ESFJ', '소통형', '커뮤니티 글을 자주 올려요', true, 22, 5, NOW(), NOW()),
    (8, 'admin08@test.com', 'admin08', 'https://cdn.ainiinu.dev/member/8.jpg', 'ADMIN', 'ACTIVE', '어드민팔', '010-1000-0008', 34, 'UNKNOWN', 'INTP', '관리형', '운영 점검 계정입니다', true, 50, 10, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    nickname = EXCLUDED.nickname,
    profile_image_url = EXCLUDED.profile_image_url,
    member_type = EXCLUDED.member_type,
    status = EXCLUDED.status,
    linked_nickname = EXCLUDED.linked_nickname,
    phone = EXCLUDED.phone,
    age = EXCLUDED.age,
    gender = EXCLUDED.gender,
    mbti = EXCLUDED.mbti,
    personality = EXCLUDED.personality,
    self_introduction = EXCLUDED.self_introduction,
    is_verified = EXCLUDED.is_verified,
    manner_score_sum = EXCLUDED.manner_score_sum,
    manner_score_count = EXCLUDED.manner_score_count,
    updated_at = NOW();

-- Follow relations
INSERT INTO member_follow (id, follower_id, following_id, created_at, updated_at) VALUES
    (101, 1, 2, NOW(), NOW()),
    (102, 1, 3, NOW(), NOW()),
    (103, 1, 5, NOW(), NOW()),
    (104, 1, 6, NOW(), NOW()),
    (105, 2, 1, NOW(), NOW()),
    (106, 3, 1, NOW(), NOW()),
    (107, 4, 1, NOW(), NOW()),
    (108, 5, 1, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET follower_id = EXCLUDED.follower_id,
    following_id = EXCLUDED.following_id,
    updated_at = NOW();

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

-- Pet
INSERT INTO pet (
    id, member_id, breed_id, name, age, gender, size, mbti, is_neutered, photo_url,
    is_main, certification_number, is_certified, created_at, updated_at
) VALUES
    (1001, 1, 1, '몽이', 3, 'MALE', 'SMALL', 'ENFP', true, 'https://cdn.ainiinu.dev/pet/1001.jpg', true, 'CERT-1001', true, NOW(), NOW()),
    (1002, 1, 2, '보리', 2, 'FEMALE', 'SMALL', 'ISFJ', true, 'https://cdn.ainiinu.dev/pet/1002.jpg', false, 'CERT-1002', true, NOW(), NOW()),
    (1003, 2, 20, '해피', 5, 'MALE', 'LARGE', 'ENTJ', true, 'https://cdn.ainiinu.dev/pet/1003.jpg', true, 'CERT-1003', true, NOW(), NOW()),
    (1004, 3, 3, '솜이', 4, 'FEMALE', 'SMALL', 'INFP', false, 'https://cdn.ainiinu.dev/pet/1004.jpg', true, 'CERT-1004', true, NOW(), NOW()),
    (1005, 5, 21, '초코', 6, 'MALE', 'LARGE', 'ESTP', true, 'https://cdn.ainiinu.dev/pet/1005.jpg', true, 'CERT-1005', true, NOW(), NOW()),
    (1006, 6, 15, '코코', 1, 'FEMALE', 'SMALL', 'ESFP', false, 'https://cdn.ainiinu.dev/pet/1006.jpg', true, 'CERT-1006', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET member_id = EXCLUDED.member_id,
    breed_id = EXCLUDED.breed_id,
    name = EXCLUDED.name,
    age = EXCLUDED.age,
    gender = EXCLUDED.gender,
    size = EXCLUDED.size,
    mbti = EXCLUDED.mbti,
    is_neutered = EXCLUDED.is_neutered,
    photo_url = EXCLUDED.photo_url,
    is_main = EXCLUDED.is_main,
    certification_number = EXCLUDED.certification_number,
    is_certified = EXCLUDED.is_certified,
    updated_at = NOW();

INSERT INTO pet_personality (id, pet_id, personality_id) VALUES
    (1101, 1001, 2), (1102, 1001, 4),
    (1103, 1002, 1), (1104, 1002, 6),
    (1105, 1003, 2), (1106, 1003, 5),
    (1107, 1004, 1), (1108, 1004, 4),
    (1109, 1005, 7), (1110, 1005, 3),
    (1111, 1006, 5), (1112, 1006, 4)
ON CONFLICT (id) DO UPDATE
SET pet_id = EXCLUDED.pet_id,
    personality_id = EXCLUDED.personality_id;

INSERT INTO pet_walking_style (id, pet_id, walking_style_id) VALUES
    (1201, 1001, 1), (1202, 1001, 2),
    (1203, 1002, 4), (1204, 1002, 3),
    (1205, 1003, 6), (1206, 1003, 1),
    (1207, 1004, 5), (1208, 1004, 4),
    (1209, 1005, 6), (1210, 1006, 7)
ON CONFLICT (id) DO UPDATE
SET pet_id = EXCLUDED.pet_id,
    walking_style_id = EXCLUDED.walking_style_id;

-- Walk thread
INSERT INTO thread (
    id, author_id, title, description, walk_date, start_time, end_time, chat_type,
    max_participants, allow_non_pet_owner, is_visible_always, place_name, latitude, longitude,
    address, status, created_at, updated_at
) VALUES
    (2001, 1, '한강 저녁 산책', '저녁 한강 코스 같이 걸어요', CURRENT_DATE + 1, NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 1 hour', 'GROUP', 4, false, true, '여의도 한강공원', 37.528300, 126.932800, '서울 영등포구 여의도동', 'RECRUITING', NOW(), NOW()),
    (2002, 2, '아침 짧은 산책', '30분 가볍게 산책해요', CURRENT_DATE + 2, NOW() + INTERVAL '2 day', NOW() + INTERVAL '2 day 40 minute', 'INDIVIDUAL', 2, true, true, '뚝섬 한강공원', 37.532600, 127.066600, '서울 광진구 자양동', 'RECRUITING', NOW(), NOW()),
    (2003, 3, '주말 공원 산책', '주말 오전 산책 모임', CURRENT_DATE - 2, NOW() - INTERVAL '2 day', NOW() - INTERVAL '2 day 1 hour', 'GROUP', 5, false, true, '서울숲', 37.544500, 127.037400, '서울 성동구 성수동', 'EXPIRED', NOW(), NOW()),
    (2004, 1, '삭제된 테스트 모집', '삭제 상태 테스트용', CURRENT_DATE - 3, NOW() - INTERVAL '3 day', NOW() - INTERVAL '3 day 1 hour', 'GROUP', 3, false, false, '반포 한강공원', 37.510200, 126.995000, '서울 서초구 반포동', 'DELETED', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET author_id = EXCLUDED.author_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    walk_date = EXCLUDED.walk_date,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    chat_type = EXCLUDED.chat_type,
    max_participants = EXCLUDED.max_participants,
    allow_non_pet_owner = EXCLUDED.allow_non_pet_owner,
    is_visible_always = EXCLUDED.is_visible_always,
    place_name = EXCLUDED.place_name,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    address = EXCLUDED.address,
    status = EXCLUDED.status,
    updated_at = NOW();

INSERT INTO thread_pet (id, thread_id, pet_id, created_at, updated_at) VALUES
    (2101, 2001, 1001, NOW(), NOW()),
    (2102, 2002, 1003, NOW(), NOW()),
    (2103, 2003, 1004, NOW(), NOW()),
    (2104, 2004, 1002, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET thread_id = EXCLUDED.thread_id,
    pet_id = EXCLUDED.pet_id,
    updated_at = NOW();

INSERT INTO thread_filter (id, thread_id, type, values_json, is_required, created_at, updated_at) VALUES
    (2201, 2001, 'PET_SIZE', '["SMALL","MEDIUM"]', true, NOW(), NOW()),
    (2202, 2001, 'GENDER', '["MALE","FEMALE"]', false, NOW(), NOW()),
    (2203, 2002, 'PET_SIZE', '["LARGE"]', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET thread_id = EXCLUDED.thread_id,
    type = EXCLUDED.type,
    values_json = EXCLUDED.values_json,
    is_required = EXCLUDED.is_required,
    updated_at = NOW();

-- Chat
INSERT INTO chat_room (id, thread_id, chat_type, status, walk_confirmed, created_at, updated_at) VALUES
    (2401, 2001, 'GROUP', 'ACTIVE', false, NOW(), NOW()),
    (2402, 2002, 'DIRECT', 'ACTIVE', true, NOW(), NOW()),
    (2403, NULL, 'DIRECT', 'ACTIVE', false, NOW(), NOW()),
    (2404, 2003, 'GROUP', 'CLOSED', false, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET thread_id = EXCLUDED.thread_id,
    chat_type = EXCLUDED.chat_type,
    status = EXCLUDED.status,
    walk_confirmed = EXCLUDED.walk_confirmed,
    updated_at = NOW();

INSERT INTO thread_application (id, thread_id, member_id, chat_room_id, status, created_at, updated_at) VALUES
    (2301, 2001, 2, 2401, 'JOINED', NOW(), NOW()),
    (2302, 2001, 3, 2401, 'JOINED', NOW(), NOW()),
    (2303, 2001, 4, 2401, 'CANCELED', NOW(), NOW()),
    (2304, 2002, 1, 2402, 'JOINED', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET thread_id = EXCLUDED.thread_id,
    member_id = EXCLUDED.member_id,
    chat_room_id = EXCLUDED.chat_room_id,
    status = EXCLUDED.status,
    updated_at = NOW();

INSERT INTO chat_participant (
    id, chat_room_id, member_id, joined_at, left_at, walk_confirm_state, last_read_message_id, created_at, updated_at
) VALUES
    (2501, 2401, 1, NOW() - INTERVAL '2 day', NULL, 'CONFIRMED', 2706, NOW(), NOW()),
    (2502, 2401, 2, NOW() - INTERVAL '2 day', NULL, 'UNCONFIRMED', 2705, NOW(), NOW()),
    (2503, 2401, 3, NOW() - INTERVAL '2 day', NULL, 'CONFIRMED', 2704, NOW(), NOW()),
    (2504, 2401, 4, NOW() - INTERVAL '2 day', NOW() - INTERVAL '1 day', 'UNCONFIRMED', 2702, NOW(), NOW()),
    (2505, 2402, 2, NOW() - INTERVAL '1 day', NULL, 'CONFIRMED', 2709, NOW(), NOW()),
    (2506, 2402, 1, NOW() - INTERVAL '1 day', NULL, 'CONFIRMED', 2709, NOW(), NOW()),
    (2507, 2403, 1, NOW() - INTERVAL '12 hour', NULL, 'UNCONFIRMED', 2712, NOW(), NOW()),
    (2508, 2403, 5, NOW() - INTERVAL '12 hour', NULL, 'UNCONFIRMED', 2712, NOW(), NOW()),
    (2509, 2404, 3, NOW() - INTERVAL '5 day', NOW() - INTERVAL '4 day', 'UNCONFIRMED', 2701, NOW(), NOW()),
    (2510, 2404, 6, NOW() - INTERVAL '5 day', NOW() - INTERVAL '4 day', 'UNCONFIRMED', 2701, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET chat_room_id = EXCLUDED.chat_room_id,
    member_id = EXCLUDED.member_id,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    walk_confirm_state = EXCLUDED.walk_confirm_state,
    last_read_message_id = EXCLUDED.last_read_message_id,
    updated_at = NOW();

INSERT INTO chat_participant_pet (id, chat_participant_id, pet_id) VALUES
    (2601, 2501, 1001),
    (2602, 2502, 1003),
    (2603, 2503, 1004),
    (2604, 2504, 1002),
    (2605, 2505, 1003),
    (2606, 2506, 1001),
    (2607, 2507, 1001),
    (2608, 2508, 1005),
    (2609, 2510, 1006)
ON CONFLICT (id) DO UPDATE
SET chat_participant_id = EXCLUDED.chat_participant_id,
    pet_id = EXCLUDED.pet_id;

INSERT INTO message (
    id, chat_room_id, sender_id, content, message_type, client_message_id, sent_at, created_at, updated_at
) VALUES
    (2701, 2401, 1, '안녕하세요! 오늘 산책 같이 하실 분?', 'USER', 'c-2401-1', NOW() - INTERVAL '6 hour', NOW(), NOW()),
    (2702, 2401, 2, '참여합니다. 시간 괜찮아요.', 'USER', 'c-2401-2', NOW() - INTERVAL '5 hour 50 minute', NOW(), NOW()),
    (2703, 2401, 3, '저도 같이 갈게요.', 'USER', 'c-2401-3', NOW() - INTERVAL '5 hour 40 minute', NOW(), NOW()),
    (2704, 2401, 1, '18시에 공원 입구에서 만나요.', 'USER', 'c-2401-4', NOW() - INTERVAL '5 hour 30 minute', NOW(), NOW()),
    (2705, 2401, 2, '네 확인했습니다!', 'USER', 'c-2401-5', NOW() - INTERVAL '5 hour 20 minute', NOW(), NOW()),
    (2706, 2401, 3, '늦으면 채팅 남길게요.', 'USER', 'c-2401-6', NOW() - INTERVAL '5 hour 10 minute', NOW(), NOW()),
    (2707, 2402, 2, '내일 아침 산책 가능하세요?', 'USER', 'c-2402-1', NOW() - INTERVAL '3 hour', NOW(), NOW()),
    (2708, 2402, 1, '네 가능합니다!', 'USER', 'c-2402-2', NOW() - INTERVAL '2 hour 50 minute', NOW(), NOW()),
    (2709, 2402, 2, '좋아요 7시에 봬요.', 'USER', 'c-2402-3', NOW() - INTERVAL '2 hour 40 minute', NOW(), NOW()),
    (2710, 2403, 1, '실종견 제보 관련 대화방입니다.', 'SYSTEM', 'c-2403-1', NOW() - INTERVAL '1 hour', NOW(), NOW()),
    (2711, 2403, 5, '강아지 사진과 위치 공유드려요.', 'USER', 'c-2403-2', NOW() - INTERVAL '50 minute', NOW(), NOW()),
    (2712, 2403, 1, '확인했습니다. 감사합니다.', 'USER', 'c-2403-3', NOW() - INTERVAL '40 minute', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET chat_room_id = EXCLUDED.chat_room_id,
    sender_id = EXCLUDED.sender_id,
    content = EXCLUDED.content,
    message_type = EXCLUDED.message_type,
    client_message_id = EXCLUDED.client_message_id,
    sent_at = EXCLUDED.sent_at,
    updated_at = NOW();

INSERT INTO chat_review (id, chat_room_id, reviewer_id, reviewee_id, score, comment, created_at, updated_at) VALUES
    (2801, 2402, 1, 2, 5, '시간 약속 잘 지키시고 친절했어요.', NOW(), NOW()),
    (2802, 2402, 2, 1, 4, '산책 코스 안내가 좋았습니다.', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET chat_room_id = EXCLUDED.chat_room_id,
    reviewer_id = EXCLUDED.reviewer_id,
    reviewee_id = EXCLUDED.reviewee_id,
    score = EXCLUDED.score,
    comment = EXCLUDED.comment,
    updated_at = NOW();

-- Walk diary
INSERT INTO walk_diary (
    id, member_id, thread_id, title, content, walk_date, is_public, deleted_at, created_at, updated_at
) VALUES
    (2901, 1, 2001, '한강 산책 기록', '몽이와 한강 한 바퀴 완주!', CURRENT_DATE - 1, true, NULL, NOW(), NOW()),
    (2902, 1, NULL, '혼자 산책', '짧게 동네 산책했어요.', CURRENT_DATE - 3, false, NULL, NOW(), NOW()),
    (2903, 2, 2001, '저녁 모임 후기', '여러 강아지들과 즐거웠어요.', CURRENT_DATE - 1, true, NULL, NOW(), NOW()),
    (2904, 2, NULL, '아침 루틴', '아침 공기가 상쾌했어요.', CURRENT_DATE - 4, false, NULL, NOW(), NOW()),
    (2905, 3, 2004, '지난 모집 산책', '오래전 기록 보관용.', CURRENT_DATE - 10, true, NULL, NOW(), NOW()),
    (2906, 5, NULL, '발견 제보 전 산책', '제보 전 동선 기록.', CURRENT_DATE - 2, true, NULL, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET member_id = EXCLUDED.member_id,
    thread_id = EXCLUDED.thread_id,
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    walk_date = EXCLUDED.walk_date,
    is_public = EXCLUDED.is_public,
    deleted_at = EXCLUDED.deleted_at,
    updated_at = NOW();

DELETE FROM walk_diary_photo_url WHERE walk_diary_id BETWEEN 2901 AND 2906;
INSERT INTO walk_diary_photo_url (walk_diary_id, display_order, photo_url) VALUES
    (2901, 0, 'https://cdn.ainiinu.dev/diary/2901-1.jpg'),
    (2901, 1, 'https://cdn.ainiinu.dev/diary/2901-2.jpg'),
    (2902, 0, 'https://cdn.ainiinu.dev/diary/2902-1.jpg'),
    (2903, 0, 'https://cdn.ainiinu.dev/diary/2903-1.jpg'),
    (2904, 0, 'https://cdn.ainiinu.dev/diary/2904-1.jpg'),
    (2905, 0, 'https://cdn.ainiinu.dev/diary/2905-1.jpg'),
    (2906, 0, 'https://cdn.ainiinu.dev/diary/2906-1.jpg');

-- Lost pet
INSERT INTO lost_pet_report (
    id, owner_id, pet_name, breed, photo_url, description, last_seen_at, last_seen_location,
    status, resolved_at, created_at, updated_at
) VALUES
    (3001, 1, '몽이', '말티즈', 'https://cdn.ainiinu.dev/lost/3001.jpg', '흰색 목줄, 오른쪽 귀 작은 점', NOW() - INTERVAL '10 hour', '여의도역 5번 출구', 'ACTIVE', NULL, NOW(), NOW()),
    (3002, 2, '해피', '골든 리트리버', 'https://cdn.ainiinu.dev/lost/3002.jpg', '파란 하네스 착용', NOW() - INTERVAL '20 hour', '뚝섬유원지역 근처', 'ACTIVE', NULL, NOW(), NOW()),
    (3003, 1, '보리', '푸들', 'https://cdn.ainiinu.dev/lost/3003.jpg', '검은 리드줄 착용', NOW() - INTERVAL '15 day', '반포 한강공원', 'RESOLVED', NOW() - INTERVAL '13 day', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET owner_id = EXCLUDED.owner_id,
    pet_name = EXCLUDED.pet_name,
    breed = EXCLUDED.breed,
    photo_url = EXCLUDED.photo_url,
    description = EXCLUDED.description,
    last_seen_at = EXCLUDED.last_seen_at,
    last_seen_location = EXCLUDED.last_seen_location,
    status = EXCLUDED.status,
    resolved_at = EXCLUDED.resolved_at,
    updated_at = NOW();

INSERT INTO sighting (
    id, finder_id, photo_url, found_at, found_location, memo, status, created_at, updated_at
) VALUES
    (3101, 5, 'https://cdn.ainiinu.dev/sighting/3101.jpg', NOW() - INTERVAL '8 hour', '여의나루역 근처', '흰색 소형견, 목줄 있음', 'OPEN', NOW(), NOW()),
    (3102, 6, 'https://cdn.ainiinu.dev/sighting/3102.jpg', NOW() - INTERVAL '6 hour', '샛강공원 입구', '말티즈로 보이는 아이', 'OPEN', NOW(), NOW()),
    (3103, 3, 'https://cdn.ainiinu.dev/sighting/3103.jpg', NOW() - INTERVAL '2 day', '한강진역 인근', '이미 보호소 이송 완료', 'CLOSED', NOW(), NOW()),
    (3104, 2, 'https://cdn.ainiinu.dev/sighting/3104.jpg', NOW() - INTERVAL '12 hour', '여의도공원 중앙', '리본 착용한 강아지', 'OPEN', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET finder_id = EXCLUDED.finder_id,
    photo_url = EXCLUDED.photo_url,
    found_at = EXCLUDED.found_at,
    found_location = EXCLUDED.found_location,
    memo = EXCLUDED.memo,
    status = EXCLUDED.status,
    updated_at = NOW();

INSERT INTO lost_pet_search_session (
    id, owner_id, lost_pet_id, query_mode, query_image_url, query_text, expires_at, created_at, updated_at
) VALUES
    (3201, 1, 3001, 'LOST', 'https://cdn.ainiinu.dev/lost/3001-query.jpg', '흰색 목줄, 귀 점', NOW() + INTERVAL '12 hour', NOW(), NOW()),
    (3202, 1, 3001, 'LOST', 'https://cdn.ainiinu.dev/lost/3001-query-old.jpg', '이전 탐색 세션', NOW() - INTERVAL '2 hour', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET owner_id = EXCLUDED.owner_id,
    lost_pet_id = EXCLUDED.lost_pet_id,
    query_mode = EXCLUDED.query_mode,
    query_image_url = EXCLUDED.query_image_url,
    query_text = EXCLUDED.query_text,
    expires_at = EXCLUDED.expires_at,
    updated_at = NOW();

INSERT INTO lost_pet_search_candidate (
    id, session_id, sighting_id, score_similarity, score_distance, score_recency, score_total,
    rank_order, status, created_at, updated_at
) VALUES
    (3301, 3201, 3101, 0.91000, 0.60000, 0.70000, 0.84700, 1, 'CANDIDATE', NOW(), NOW()),
    (3302, 3201, 3102, 0.93000, 0.50000, 0.80000, 0.85100, 2, 'APPROVED', NOW(), NOW()),
    (3303, 3201, 3103, 0.70000, 0.20000, 0.30000, 0.56000, 3, 'CANDIDATE', NOW(), NOW()),
    (3304, 3201, 3104, 0.82000, 0.60000, 0.50000, 0.74400, 4, 'CANDIDATE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET session_id = EXCLUDED.session_id,
    sighting_id = EXCLUDED.sighting_id,
    score_similarity = EXCLUDED.score_similarity,
    score_distance = EXCLUDED.score_distance,
    score_recency = EXCLUDED.score_recency,
    score_total = EXCLUDED.score_total,
    rank_order = EXCLUDED.rank_order,
    status = EXCLUDED.status,
    updated_at = NOW();

INSERT INTO lost_pet_match (
    id, lost_pet_id, sighting_id, similarity_total, status, approved_by_member_id, approved_at,
    chat_room_id, invalidated_reason, invalidated_at, matched_at, created_at, updated_at
) VALUES
    (3401, 3001, 3102, 0.8510, 'CHAT_LINKED', 1, NOW() - INTERVAL '5 hour', 2403, NULL, NULL, NOW() - INTERVAL '5 hour', NOW(), NOW()),
    (3402, 3001, 3103, 0.5600, 'INVALIDATED', 1, NOW() - INTERVAL '1 day', NULL, 'SIGHTING_CLOSED', NOW() - INTERVAL '20 hour', NOW() - INTERVAL '1 day', NOW(), NOW()),
    (3403, 3002, 3101, 0.7400, 'PENDING_CHAT_LINK', 2, NOW() - INTERVAL '3 hour', NULL, NULL, NULL, NOW() - INTERVAL '3 hour', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET lost_pet_id = EXCLUDED.lost_pet_id,
    sighting_id = EXCLUDED.sighting_id,
    similarity_total = EXCLUDED.similarity_total,
    status = EXCLUDED.status,
    approved_by_member_id = EXCLUDED.approved_by_member_id,
    approved_at = EXCLUDED.approved_at,
    chat_room_id = EXCLUDED.chat_room_id,
    invalidated_reason = EXCLUDED.invalidated_reason,
    invalidated_at = EXCLUDED.invalidated_at,
    matched_at = EXCLUDED.matched_at,
    updated_at = NOW();

-- Community
INSERT INTO post (
    id, author_id, content, like_count, comment_count, version, created_at, updated_at
) VALUES
    (3501, 1, '오늘 한강 산책 다녀왔어요. 날씨가 정말 좋네요!', 4, 3, 0, NOW() - INTERVAL '9 hour', NOW()),
    (3502, 2, '아침 산책 메이트 구해요. 내일 7시 가능하신 분?', 3, 2, 0, NOW() - INTERVAL '8 hour', NOW()),
    (3503, 3, '강아지 발 관리 팁 공유합니다.', 3, 2, 0, NOW() - INTERVAL '7 hour', NOW()),
    (3504, 5, '실종견 제보 받았습니다. 주변 확인 부탁드려요.', 2, 2, 0, NOW() - INTERVAL '6 hour', NOW()),
    (3505, 7, '반려견 없는 분도 산책 동행 괜찮을까요?', 2, 2, 0, NOW() - INTERVAL '5 hour', NOW()),
    (3506, 6, '오늘 산책 코스 추천 부탁드립니다.', 2, 1, 0, NOW() - INTERVAL '4 hour', NOW()),
    (3507, 1, '우리 몽이 최근 사진 올려봐요!', 1, 0, 0, NOW() - INTERVAL '3 hour', NOW()),
    (3508, 2, '간식 추천 리스트 공유합니다.', 1, 0, 0, NOW() - INTERVAL '2 hour', NOW())
ON CONFLICT (id) DO UPDATE
SET author_id = EXCLUDED.author_id,
    content = EXCLUDED.content,
    like_count = EXCLUDED.like_count,
    comment_count = EXCLUDED.comment_count,
    version = EXCLUDED.version,
    updated_at = NOW();

DELETE FROM post_image_url WHERE post_id BETWEEN 3501 AND 3508;
INSERT INTO post_image_url (post_id, image_url) VALUES
    (3501, 'https://cdn.ainiinu.dev/post/3501-1.jpg'),
    (3501, 'https://cdn.ainiinu.dev/post/3501-2.jpg'),
    (3502, 'https://cdn.ainiinu.dev/post/3502-1.jpg'),
    (3503, 'https://cdn.ainiinu.dev/post/3503-1.jpg'),
    (3504, 'https://cdn.ainiinu.dev/post/3504-1.jpg'),
    (3504, 'https://cdn.ainiinu.dev/post/3504-2.jpg'),
    (3506, 'https://cdn.ainiinu.dev/post/3506-1.jpg'),
    (3507, 'https://cdn.ainiinu.dev/post/3507-1.jpg'),
    (3508, 'https://cdn.ainiinu.dev/post/3508-1.jpg');

INSERT INTO comment (id, post_id, author_id, content, created_at, updated_at) VALUES
    (3601, 3501, 2, '사진 너무 귀여워요!', NOW() - INTERVAL '8 hour 40 minute', NOW()),
    (3602, 3501, 3, '코스 정보도 공유 부탁드려요.', NOW() - INTERVAL '8 hour 20 minute', NOW()),
    (3603, 3501, 5, '다음에 같이 걸어요!', NOW() - INTERVAL '8 hour', NOW()),
    (3604, 3502, 1, '저 참여 가능해요!', NOW() - INTERVAL '7 hour 30 minute', NOW()),
    (3605, 3502, 4, '시간만 맞으면 같이 가고 싶어요.', NOW() - INTERVAL '7 hour 10 minute', NOW()),
    (3606, 3503, 7, '발 관리 꿀팁 감사합니다.', NOW() - INTERVAL '6 hour 45 minute', NOW()),
    (3607, 3503, 1, '좋은 정보네요.', NOW() - INTERVAL '6 hour 30 minute', NOW()),
    (3608, 3504, 2, '제보 위치 공유 감사합니다.', NOW() - INTERVAL '5 hour 50 minute', NOW()),
    (3609, 3504, 6, '근처 확인해볼게요.', NOW() - INTERVAL '5 hour 40 minute', NOW()),
    (3610, 3505, 3, '동행 가능해요. 부담 없이 오세요.', NOW() - INTERVAL '4 hour 50 minute', NOW()),
    (3611, 3505, 5, '좋은 질문입니다!', NOW() - INTERVAL '4 hour 40 minute', NOW()),
    (3612, 3506, 1, '저는 서울숲 코스 추천해요.', NOW() - INTERVAL '3 hour 30 minute', NOW())
ON CONFLICT (id) DO UPDATE
SET post_id = EXCLUDED.post_id,
    author_id = EXCLUDED.author_id,
    content = EXCLUDED.content,
    updated_at = NOW();

INSERT INTO post_like (id, post_id, member_id, created_at, updated_at) VALUES
    (3701, 3501, 2, NOW(), NOW()), (3702, 3501, 3, NOW(), NOW()), (3703, 3501, 5, NOW(), NOW()), (3704, 3501, 7, NOW(), NOW()),
    (3705, 3502, 1, NOW(), NOW()), (3706, 3502, 3, NOW(), NOW()), (3707, 3502, 4, NOW(), NOW()),
    (3708, 3503, 1, NOW(), NOW()), (3709, 3503, 2, NOW(), NOW()), (3710, 3503, 5, NOW(), NOW()),
    (3711, 3504, 1, NOW(), NOW()), (3712, 3504, 6, NOW(), NOW()),
    (3713, 3505, 1, NOW(), NOW()), (3714, 3505, 2, NOW(), NOW()),
    (3715, 3506, 3, NOW(), NOW()), (3716, 3506, 5, NOW(), NOW()),
    (3717, 3507, 2, NOW(), NOW()), (3718, 3508, 1, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET post_id = EXCLUDED.post_id,
    member_id = EXCLUDED.member_id,
    updated_at = NOW();
