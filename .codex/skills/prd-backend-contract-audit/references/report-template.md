# PRD-계약 정합성 감사 보고서 (YYYY-MM-DD)

## 1. 감사 범위 및 기준
- 기준 문서: `common-docs/PROJECT_PRD.md`
- 대조 대상:
  - Backend 코드: `aini-inu-backend` (branch/commit: `<fill>`)
  - 테스트 코드: `aini-inu-backend/src/test/java`
  - Swagger/OpenAPI: 런타임 `/v3/api-docs` 추출본 (`<artifact path>`)
- 분류 체계:
  - 유형: `계약상 불일치` / `미구현` / `초과구현` / `논리적 어긋남`
  - 심각도: `Critical` / `High` / `Medium` / `Low`
  - 판정: `확정` / `의심`

## 2. 실행 검증 결과
- 테스트 실행: `<command>`
  - 결과: `<pass/fail summary>`
  - 근거: `<report path>`
- OpenAPI 추출: `<command or script>`
  - 결과: `<success/fail + size>`
  - 근거: `<artifact path>`

## 3. 이슈 요약

| No | 유형 | 심각도 | 판정 | 제목 |
|---|---|---|---|---|
| 1 | 계약상 불일치 | High | 확정 | `<title>` |

## 4. 상세 이슈

### 4.1 [High][계약상 불일치][확정] `<title>`
- PRD 근거:
  - `<PRD section/path>`
- Swagger/OpenAPI 근거:
  - `<method path + field>`
- 코드/테스트 근거:
  - `<file:line>`
- 영향:
  - `<impact>`
- 판정 사유:
  - `<why this category/severity>`

### 4.2 [Medium][미구현][확정] `<title>`
- PRD 근거:
  - `<...>`
- 코드/테스트 근거:
  - `<...>`
- 영향:
  - `<...>`
- 판정 사유:
  - `<...>`

## 5. 정합 항목(일치 확인)
- `<area>`: `<why aligned>`
- `<area>`: `<why aligned>`

## 6. 근거 파일 목록
- `common-docs/PROJECT_PRD.md`
- `aini-inu-backend/src/main/java/...`
- `aini-inu-backend/src/test/java/...`
- `<openapi artifact path>`
- `<test report path>`
