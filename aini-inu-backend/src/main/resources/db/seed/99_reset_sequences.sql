-- Align PostgreSQL sequences after explicit ID seeding

SELECT setval(pg_get_serial_sequence('breed', 'id'), COALESCE((SELECT MAX(id) FROM breed), 1), true);
SELECT setval(pg_get_serial_sequence('personality', 'id'), COALESCE((SELECT MAX(id) FROM personality), 1), true);
SELECT setval(pg_get_serial_sequence('walking_style', 'id'), COALESCE((SELECT MAX(id) FROM walking_style), 1), true);
SELECT setval(pg_get_serial_sequence('member_personality_type', 'id'), COALESCE((SELECT MAX(id) FROM member_personality_type), 1), true);
