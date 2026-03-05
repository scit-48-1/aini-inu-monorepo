# OpenAPI Snapshot Guide

`openapi.v1.json`은 백엔드 런타임 `/v3/api-docs`를 추출한 최신 스냅샷 파일입니다.

## Source of truth
- API 계약의 최종 원본은 런타임 OpenAPI(`/v3/api-docs`)입니다.
- 이 파일은 리뷰/비교(diff) 편의를 위한 추적 스냅샷입니다.

## Update command

```bash
cd aini-inu-backend
./scripts/export-openapi.sh
```

기본 출력 경로:
- `common-docs/openapi/openapi.v1.json`

## Team rule
- API 경로/요청/응답 계약 변경이 있으면 스냅샷도 함께 갱신합니다.
- 스냅샷은 수동 편집하지 않고, 스크립트 생성 결과만 커밋합니다.
- 커밋은 `aini-inu-backend`와 `common-docs`를 분리해 남깁니다.
