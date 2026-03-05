# Springdoc OpenAPI Standardization Rules

## 1) DTO Field Annotation Rules

- Annotate every request/response DTO field with `@Schema`.
- Fill at least:
  - `description`
  - `example` (except sensitive fields)
- Add constraints when applicable:
  - `requiredMode`
  - `minLength`, `maxLength`
  - `minimum`, `maximum`
  - `pattern`
  - `format` (`date`, `date-time`, `uri`)
- For date-time examples, always use UTC Z format:
  - `2026-03-05T01:20:00Z`

## 2) Sensitive Example Rules

- Mask examples for sensitive fields:
  - `password`
  - `token`
  - `accessToken`
  - `refreshToken`
- Use placeholder values such as `<MASKED>` or `<TOKEN>`.

## 3) Enum Rules

- Prefer Java enum types when domain enum exists.
- For enum-like string fields:
  - set `allowableValues` in `@Schema`, or
  - inject enum list via OpenAPI customizer map.
- Include value semantics in description:
  - Example: `가능 값: DIRECT(1:1 채팅), GROUP(그룹 채팅).`

## 4) PATCH Rules

- Explicitly document null semantics per field:
  - scalar: `null이면 변경하지 않습니다.`
  - array: `null이면 변경하지 않고, [] 전달 시 전체 해제합니다.`

## 5) Endpoint Rules

- Ensure every `/api/v1/**` operation has:
  - summary
  - description
  - explicit success and failure responses
- Keep request/response media type explicit:
  - `application/json` for normal endpoints
  - binary media type only for binary endpoints

## 6) Verification Rules

- Run:
  - backend tests
  - OpenAPI export
  - DTO/OpenAPI quality scripts
- Update `common-docs/openapi/openapi.v1.json` in same workstream.
