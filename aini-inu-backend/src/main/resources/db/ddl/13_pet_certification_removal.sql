-- 동물등록번호 인증 기능 제거에 따른 컬럼 정리
ALTER TABLE pet DROP COLUMN IF EXISTS is_certified;
ALTER TABLE pet DROP COLUMN IF EXISTS certification_number;
