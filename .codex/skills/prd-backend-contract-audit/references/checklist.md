# PRD Backend Contract Audit Checklist

## 0) Detect-Only Principle

- Audit phase must not mix implementation fixes.
- Produce findings first, with evidence.
- If fix is requested later, run as separate workstream.

## 1) Finding Categories

Use exactly one primary category per finding.

- `계약상 불일치`
  - PRD/명세가 요구하는 동작과 구현/Swagger 계약이 다른 경우
  - 예: 필수값/타입/에러코드/인증 조건 상충
- `미구현`
  - PRD/명세에 정의된 기능이 코드/엔드포인트/흐름으로 존재하지 않는 경우
- `초과구현`
  - PRD/명세에 없는 기능/엔드포인트/필드가 외부 계약에 노출된 경우
- `논리적 어긋남`
  - 문서 내부 주장끼리 또는 문서 vs 코드 상태가 논리적으로 충돌하는 경우

## 2) Severity Guide

- `Critical`: 결제/보안/데이터 무결성/치명적 운영 리스크
- `High`: 핵심 기능 계약 위반, 클라이언트 연동 실패 가능성 높음
- `Medium`: 기능은 동작하나 계약/정책/문서 정합성에 명확한 문제
- `Low`: 영향이 제한적이거나 예외 규칙 여부 확인이 필요한 항목

## 3) Status Guide

- `확정`: 코드/테스트/OpenAPI/문서 근거로 사실관계가 충분히 입증됨
- `의심`: 해석 여지 또는 추가 증거가 필요한 항목

`의심`으로 판정할 때는 어떤 근거가 부족한지 반드시 적는다.

## 4) Evidence Rules

Every finding must include at least one concrete evidence item:

- File + line reference
- Endpoint + method + schema path
- Test class/method behavior
- Runtime OpenAPI snippet source (artifact path)
- Automated signal id/code (`signals.json`) when applicable

Avoid statements without direct evidence.

## 4.1) Mandatory Matrices

Every audit must cover these matrices (manual or script-assisted):

1. PRD/API ref vs OpenAPI endpoint presence
2. OpenAPI endpoint vs controller mapping
3. Controller endpoint vs test evidence
4. Public service method vs test evidence
5. Seed/fixture vs backend auth/domain invariants

If any matrix is skipped, report why.

## 5) Contract Comparison Order

Compare in this order to reduce false positives:

1. `common-docs/PROJECT_PRD.md` policy/contract statement
2. Runtime OpenAPI (`/v3/api-docs` extracted artifact)
3. Backend implementation code
4. Tests validating intended behavior

If (2) and (3) differ, report both and classify root issue location.

## 6) Report Completeness Check

Before finalizing report, confirm:

- All findings have category + severity + status
- Each finding has evidence path
- Confirmed/uncertain claims are clearly separated
- Report file is saved under `common-docs`
- Automated scan outputs are referenced:
  - `/tmp/aini_contract_audit/signals.json`
  - `/tmp/aini_contract_audit/signals.md`
