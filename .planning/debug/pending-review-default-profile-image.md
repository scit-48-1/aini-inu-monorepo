---
status: diagnosed
trigger: "PendingReviewCard floating notification shows default profile image instead of member's actual profile image"
created: 2026-03-08T00:00:00Z
updated: 2026-03-08T00:00:00Z
---

## Current Focus

hypothesis: PendingReviewCard never receives or displays a profile image - it uses a hardcoded amber icon circle instead
test: Read component source and props interface
expecting: Missing profileImageUrl prop or hardcoded fallback
next_action: Report diagnosis

## Symptoms

expected: Floating review notification pill displays the logged-in user's actual profile image
actual: Displays a generic/default icon (amber circle with ClipboardList icon)
errors: None
reproduction: Log in, have pending reviews, observe floating notification at bottom-right
started: Since component was created (a58a5f1)

## Eliminated

(none needed - root cause found on first inspection)

## Evidence

- timestamp: 2026-03-08T00:00:00Z
  checked: PendingReviewCard.tsx component source
  found: Component renders a hardcoded amber circle with a ClipboardList Lucide icon. No profile image prop exists in the interface (only pendingCount and onClick). No <img> or <Image> element exists in the component.
  implication: The component was never designed to show a profile image - it shows a clipboard icon instead.

- timestamp: 2026-03-08T00:00:00Z
  checked: Dashboard page.tsx - how PendingReviewCard is invoked
  found: Only passes pendingCount={pendingReviews.length} and onClick handler. No image-related prop.
  implication: Confirms no profile image data is passed to the component.

- timestamp: 2026-03-08T00:00:00Z
  checked: useUserStore and useProfile hook
  found: User profile is available via useProfile() hook in dashboard. profile.avatar contains the profileImageUrl (mapped from MemberResponse.profileImageUrl in mapMemberToUser). The dashboard already calls useProfile() and has userProfile available.
  implication: The profile image URL is readily available in the dashboard scope but simply not passed to PendingReviewCard.

## Resolution

root_cause: PendingReviewCard was designed as an icon-based notification pill. Its props interface only accepts `pendingCount` and `onClick`. The component renders a hardcoded `<div className="w-10 h-10 bg-amber-500 rounded-full">` with a `<ClipboardList>` Lucide icon inside it. There is no mechanism to receive or display a profile image. The dashboard has `userProfile` (with `userProfile.avatar` containing the profile image URL) available in scope but does not pass it to PendingReviewCard.
fix: (not applied - diagnosis only)
verification: (not applied)
files_changed: []
