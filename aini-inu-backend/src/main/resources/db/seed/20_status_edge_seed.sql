-- Status/edge-case seed data for richer manual testing (idempotent by fixed IDs)

-- Add medium-sized breed for enum coverage
INSERT INTO breed (id, name, size, created_at, updated_at) VALUES
    (30, '코커 스패니얼', 'MEDIUM', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    size = EXCLUDED.size,
    updated_at = NOW();

-- Additional members for status coverage
INSERT INTO member (
    id, email, nickname, profile_image_url, member_type, status, linked_nickname,
    phone, age, gender, mbti, personality, self_introduction, is_verified,
    manner_score_sum, manner_score_count, created_at, updated_at
) VALUES
    (9001, 'inactive9001@test.com', 'inactive1', 'https://cdn.ainiinu.dev/member/9001.jpg', 'PET_OWNER', 'INACTIVE', '인액티브원', '010-9001-0001', 38, 'FEMALE', 'ISTJ', '조용함', '잠시 쉬는 중인 계정입니다', true, 24, 6, NOW(), NOW()),
    (9002, 'banned9002@test.com', 'banned9002', 'https://cdn.ainiinu.dev/member/9002.jpg', 'NON_PET_OWNER', 'BANNED', '밴드투', '010-9002-0002', 41, 'MALE', 'ENTJ', '강경함', '제재 상태 계정입니다', true, 10, 3, NOW(), NOW())
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

-- Medium-sized pet coverage
INSERT INTO pet (
    id, member_id, breed_id, name, age, gender, size, mbti, is_neutered, photo_url,
    is_main, certification_number, is_certified, created_at, updated_at
) VALUES
    (9301, 2, 30, '밤이', 3, 'MALE', 'MEDIUM', 'ENFJ', true, 'https://cdn.ainiinu.dev/pet/9301.jpg', false, 'CERT-9301', true, NOW(), NOW())
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
    (9311, 9301, 2),
    (9312, 9301, 5)
ON CONFLICT (id) DO UPDATE
SET pet_id = EXCLUDED.pet_id,
    personality_id = EXCLUDED.personality_id;

INSERT INTO pet_walking_style (id, pet_id, walking_style_id) VALUES
    (9321, 9301, 4),
    (9322, 9301, 5)
ON CONFLICT (id) DO UPDATE
SET pet_id = EXCLUDED.pet_id,
    walking_style_id = EXCLUDED.walking_style_id;

-- Capacity-full recruiting thread
INSERT INTO thread (
    id, author_id, title, description, walk_date, start_time, end_time, chat_type,
    max_participants, allow_non_pet_owner, is_visible_always, place_name, latitude, longitude,
    address, status, created_at, updated_at
) VALUES
    (9401, 1, '정원 마감 임박 산책', '정원 초과 테스트용 모집입니다', CURRENT_DATE + 1, NOW() + INTERVAL '4 hour', NOW() + INTERVAL '5 hour', 'GROUP', 3, true, true, '서울숲 남문', 37.545900, 127.040500, '서울 성동구 성수동1가', 'RECRUITING', NOW(), NOW())
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
    (9411, 9401, 1001, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET thread_id = EXCLUDED.thread_id,
    pet_id = EXCLUDED.pet_id,
    updated_at = NOW();

INSERT INTO thread_application (id, thread_id, member_id, chat_room_id, status, created_at, updated_at) VALUES
    (9421, 9401, 2, 9501, 'JOINED', NOW(), NOW()),
    (9422, 9401, 3, 9501, 'JOINED', NOW(), NOW()),
    (9423, 9401, 4, 9501, 'JOINED', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET thread_id = EXCLUDED.thread_id,
    member_id = EXCLUDED.member_id,
    chat_room_id = EXCLUDED.chat_room_id,
    status = EXCLUDED.status,
    updated_at = NOW();

-- Chat rooms: one linked group room, one empty direct room
INSERT INTO chat_room (id, thread_id, chat_type, status, walk_confirmed, created_at, updated_at) VALUES
    (9501, 9401, 'GROUP', 'ACTIVE', false, NOW(), NOW()),
    (9502, NULL, 'DIRECT', 'ACTIVE', false, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET thread_id = EXCLUDED.thread_id,
    chat_type = EXCLUDED.chat_type,
    status = EXCLUDED.status,
    walk_confirmed = EXCLUDED.walk_confirmed,
    updated_at = NOW();

INSERT INTO chat_participant (
    id, chat_room_id, member_id, joined_at, left_at, walk_confirm_state, last_read_message_id, created_at, updated_at
) VALUES
    (9511, 9501, 1, NOW() - INTERVAL '2 hour', NULL, 'CONFIRMED', NULL, NOW(), NOW()),
    (9512, 9501, 2, NOW() - INTERVAL '2 hour', NULL, 'UNCONFIRMED', NULL, NOW(), NOW()),
    (9513, 9501, 3, NOW() - INTERVAL '2 hour', NULL, 'CONFIRMED', NULL, NOW(), NOW()),
    (9514, 9501, 4, NOW() - INTERVAL '2 hour', NULL, 'UNCONFIRMED', NULL, NOW(), NOW()),
    (9515, 9502, 1, NOW() - INTERVAL '20 minute', NULL, 'UNCONFIRMED', NULL, NOW(), NOW()),
    (9516, 9502, 6, NOW() - INTERVAL '20 minute', NULL, 'UNCONFIRMED', NULL, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET chat_room_id = EXCLUDED.chat_room_id,
    member_id = EXCLUDED.member_id,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    walk_confirm_state = EXCLUDED.walk_confirm_state,
    last_read_message_id = EXCLUDED.last_read_message_id,
    updated_at = NOW();

INSERT INTO chat_participant_pet (id, chat_participant_id, pet_id) VALUES
    (9521, 9511, 1001),
    (9522, 9512, 1003),
    (9523, 9513, 1004),
    (9524, 9515, 1001),
    (9525, 9516, 1006)
ON CONFLICT (id) DO UPDATE
SET chat_participant_id = EXCLUDED.chat_participant_id,
    pet_id = EXCLUDED.pet_id;

-- Walk diary edge states for story/following filtering
INSERT INTO walk_diary (
    id, member_id, thread_id, title, content, walk_date, is_public, deleted_at, created_at, updated_at
) VALUES
    (9601, 2, NULL, '24시간 경과 공개 일기', '스토리 만료 경계 테스트용입니다.', CURRENT_DATE - 2, true, NULL, NOW() - INTERVAL '30 hour', NOW()),
    (9602, 3, NULL, '삭제된 공개 일기', '삭제 반영 여부 테스트용입니다.', CURRENT_DATE - 1, true, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '2 hour', NOW()),
    (9603, 5, NULL, '비공개 일기', '비공개 필터 테스트용입니다.', CURRENT_DATE - 1, false, NULL, NOW() - INTERVAL '3 hour', NOW())
ON CONFLICT (id) DO UPDATE
SET member_id = EXCLUDED.member_id,
    thread_id = EXCLUDED.thread_id,
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    walk_date = EXCLUDED.walk_date,
    is_public = EXCLUDED.is_public,
    deleted_at = EXCLUDED.deleted_at,
    created_at = EXCLUDED.created_at,
    updated_at = NOW();

DELETE FROM walk_diary_photo_url WHERE walk_diary_id BETWEEN 9601 AND 9603;
INSERT INTO walk_diary_photo_url (walk_diary_id, display_order, photo_url) VALUES
    (9601, 0, 'https://cdn.ainiinu.dev/diary/9601-1.jpg'),
    (9602, 0, 'https://cdn.ainiinu.dev/diary/9602-1.jpg'),
    (9603, 0, 'https://cdn.ainiinu.dev/diary/9603-1.jpg');

-- Lost-pet status/match matrix coverage
INSERT INTO lost_pet_report (
    id, owner_id, pet_name, breed, photo_url, description, last_seen_at, last_seen_location,
    status, resolved_at, created_at, updated_at
) VALUES
    (9701, 1, '루루', '코커 스패니얼', 'https://cdn.ainiinu.dev/lost/9701.jpg', '파란 목줄, MEDIUM 사이즈', NOW() - INTERVAL '3 day', '서울숲 북문', 'CLOSED', NOW() - INTERVAL '2 day', NOW(), NOW())
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

INSERT INTO lost_pet_search_session (
    id, owner_id, lost_pet_id, query_mode, query_image_url, query_text, expires_at, created_at, updated_at
) VALUES
    (9711, 1, 3001, 'LOST', 'https://cdn.ainiinu.dev/lost/9711-query.jpg', '상태 매트릭스 검증 세션', NOW() + INTERVAL '4 hour', NOW(), NOW())
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
    (9731, 9711, 3101, 0.79000, 0.65000, 0.55000, 0.73200, 1, 'CANDIDATE', NOW(), NOW()),
    (9732, 9711, 3104, 0.88000, 0.70000, 0.62000, 0.82100, 2, 'APPROVED', NOW(), NOW())
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
    (9741, 3001, 3104, 0.8210, 'PENDING_APPROVAL', NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL '20 minute', NOW(), NOW()),
    (9742, 3002, 3102, 0.8020, 'APPROVED', 2, NOW() - INTERVAL '2 hour', NULL, NULL, NULL, NOW() - INTERVAL '2 hour', NOW(), NOW()),
    (9743, 3003, 3101, 0.6010, 'REJECTED', 1, NOW() - INTERVAL '1 day', NULL, NULL, NULL, NOW() - INTERVAL '1 day', NOW(), NOW())
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

-- Community edges: zero-content interaction + max image count
INSERT INTO post (
    id, author_id, content, like_count, comment_count, created_at, updated_at
) VALUES
    (9801, 7, '상호작용 없는 기본 게시글입니다.', 0, 0, NOW() - INTERVAL '4 hour', NOW()),
    (9802, 6, '이미지 5장 경계 테스트 게시글입니다.', 5, 1, NOW() - INTERVAL '3 hour', NOW())
ON CONFLICT (id) DO UPDATE
SET author_id = EXCLUDED.author_id,
    content = EXCLUDED.content,
    like_count = EXCLUDED.like_count,
    comment_count = EXCLUDED.comment_count,
    updated_at = NOW();

DELETE FROM post_image_url WHERE post_id BETWEEN 9801 AND 9802;
INSERT INTO post_image_url (post_id, image_url) VALUES
    (9802, 'https://cdn.ainiinu.dev/post/9802-1.jpg'),
    (9802, 'https://cdn.ainiinu.dev/post/9802-2.jpg'),
    (9802, 'https://cdn.ainiinu.dev/post/9802-3.jpg'),
    (9802, 'https://cdn.ainiinu.dev/post/9802-4.jpg'),
    (9802, 'https://cdn.ainiinu.dev/post/9802-5.jpg');

INSERT INTO comment (id, post_id, author_id, content, created_at, updated_at) VALUES
    (9811, 9802, 1, '이미지 구성 좋아요.', NOW() - INTERVAL '2 hour 30 minute', NOW())
ON CONFLICT (id) DO UPDATE
SET post_id = EXCLUDED.post_id,
    author_id = EXCLUDED.author_id,
    content = EXCLUDED.content,
    updated_at = NOW();

INSERT INTO post_like (id, post_id, member_id, created_at, updated_at) VALUES
    (9821, 9802, 1, NOW(), NOW()),
    (9822, 9802, 2, NOW(), NOW()),
    (9823, 9802, 3, NOW(), NOW()),
    (9824, 9802, 5, NOW(), NOW()),
    (9825, 9802, 7, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET post_id = EXCLUDED.post_id,
    member_id = EXCLUDED.member_id,
    updated_at = NOW();
