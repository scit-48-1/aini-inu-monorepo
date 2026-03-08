---
status: resolved
trigger: "Dashboard Hero section does not show nickname, manner score, representative dog profile photo, or location"
created: 2026-03-08T00:00:00Z
updated: 2026-03-08T00:00:00Z
---

## Current Focus

hypothesis: confirmed - multiple root causes across mapping layer and component rendering
test: traced full data flow from API -> store -> hook -> page -> component
expecting: N/A - root cause confirmed
next_action: return structured findings

## Symptoms

expected: Hero shows greeting with user's nickname, manner score, representative dog photo, and location
actual: None of these are displayed correctly - nickname shows dog name instead, location hardcoded fallback, dog photo from breed fallback, manner score shows 0
errors: none (silent data loss)
reproduction: Load /dashboard as any authenticated user
started: Phase 11 dashboard rewire

## Eliminated

(none needed - root causes found on first pass)

## Evidence

- timestamp: 2026-03-08
  checked: mapMemberToUser() in useUserStore.ts
  found: location is hardcoded to '' (empty string), dogs is hardcoded to [] (empty array)
  implication: userProfile.location will always be empty, userProfile.dogs will always be empty

- timestamp: 2026-03-08
  checked: MemberResponse type in api/members.ts
  found: MemberResponse has NO location field and NO dogs/pets field
  implication: Backend /members/me endpoint does not return location or pets data

- timestamp: 2026-03-08
  checked: DashboardHero.tsx greeting line 59
  found: Greeting says "Hello, {mainDog.name} 매니저님!" - shows DOG name, not user nickname
  implication: User's nickname is never displayed in the greeting

- timestamp: 2026-03-08
  checked: page.tsx mainDog fallback (line 52-56)
  found: Falls back to { name: '댕댕이', image: '/images/dog-portraits/Mixed Breed.png', breed: '믹스견' } because userProfile.dogs is always []
  implication: Representative dog photo always shows generic Mixed Breed image

- timestamp: 2026-03-08
  checked: DashboardHero location rendering (line 62)
  found: Uses `userProfile.location || '서울시 성수동'` - always falls back to hardcoded value since location is always ''
  implication: Location always shows fallback, never real user location

- timestamp: 2026-03-08
  checked: manner score rendering (line 70)
  found: Uses `userProfile.mannerScore ?? 0` - this DOES work if mannerTemperature is returned by API
  implication: Manner score display works correctly IF the API returns mannerTemperature (mapped correctly in store)

## Resolution

root_cause: Three distinct issues prevent hero data display
fix: (not applied - diagnosis only)
verification: N/A
files_changed: []
