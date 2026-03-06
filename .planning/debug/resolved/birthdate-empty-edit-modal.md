---
status: resolved
trigger: "birthDate is empty when opening pet edit modal"
created: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:00:00Z
---

## Current Focus

hypothesis: birthDate is hardcoded to empty string '' on init, ignoring initialData
test: read PetForm.tsx line 49 vs other fields
expecting: birthDate useState should reference initialData like other fields
next_action: document root cause

## Symptoms

expected: When opening DogRegisterModal in edit mode, birthDate should be pre-filled from pet data
actual: birthDate field is always empty in edit mode; user must re-enter it
errors: none
reproduction: Click edit on any pet -> observe birthDate field is blank
started: since PetForm was created

## Eliminated

(none needed - root cause found on first inspection)

## Evidence

- timestamp: 2026-03-06
  checked: PetForm.tsx line 49 - birthDate useState initializer
  found: `const [birthDate, setBirthDate] = useState('');` -- hardcoded empty string, does NOT read from initialData
  implication: This is the direct cause. Every other field reads from initialData (e.g. line 45 `initialData?.name || ''`) but birthDate was missed.

- timestamp: 2026-03-06
  checked: PetForm.tsx lines 45-61 - all other field initializers
  found: name, breedId, gender, size, isNeutered, mbti, photoUrl, walkingStyles, personalities all read from initialData
  implication: birthDate is the ONLY field that doesn't initialize from initialData - this is clearly a missed field, not a design choice

- timestamp: 2026-03-06
  checked: PetResponse type in api/pets.ts lines 31-46
  found: PetResponse does NOT include a birthDate field. It has `age: number` instead.
  implication: Two problems: (1) PetForm doesn't try to init birthDate from initialData, and (2) even if it did, PetResponse doesn't carry birthDate - only a computed `age`. The backend likely returns age but not the raw birthDate.

## Resolution

root_cause: Two-layer bug -- (1) PetForm.tsx line 49 hardcodes birthDate to '' instead of reading from initialData, and (2) PetResponse type lacks a birthDate field entirely (only has computed `age`), so even fixing the form init would yield nothing unless the backend response also includes birthDate.
fix: (not applied)
verification: (not done)
files_changed: []
