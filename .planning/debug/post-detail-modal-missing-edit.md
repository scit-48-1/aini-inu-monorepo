---
status: diagnosed
trigger: "PostDetailModal three-dot menu only shows delete, not edit option"
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: The PostDetailModal has no three-dot dropdown menu at all; edit/delete are shown as separate buttons at the bottom. The FeedItem component has a three-dot (MoreHorizontal) button but it only triggers a delete confirmation overlay with no edit option.
test: Code inspection of both components
expecting: Confirm absence of dropdown menu and edit option in three-dot flow
next_action: Report diagnosis

## Symptoms

expected: Three-dot menu opens a dropdown with both "edit" and "delete" options
actual: PostDetailModal shows edit/delete as bottom bar buttons (no three-dot menu); FeedItem has a three-dot button that only triggers delete confirmation
errors: none
reproduction: Open any own post in PostDetailModal or expand in feed
started: Always been this way (never implemented)

## Eliminated

- hypothesis: Edit option is conditionally hidden by a flag or permission check
  evidence: PostDetailModal line 112-116 shows edit button IS rendered for owners, but as a bottom bar button not a dropdown. No conditional hiding of edit within a menu.
  timestamp: 2026-03-07

## Evidence

- timestamp: 2026-03-07
  checked: PostDetailModal.tsx (full file, 123 lines)
  found: Component has handleEdit (line 37-40) and handleSave (line 42-55) functions fully implemented. Edit and delete are rendered as two separate buttons in a bottom bar (lines 112-116) guarded by `!isEditing && isOwner`. There is NO three-dot/ellipsis icon, NO dropdown menu component.
  implication: Edit functionality exists but is presented as a bottom-bar button, not inside a dropdown menu.

- timestamp: 2026-03-07
  checked: FeedItem.tsx (full file, 392 lines)
  found: FeedItem uses MoreHorizontal icon (line 295) as a three-dot button, but clicking it only calls `setShowDeleteConfirm(true)` (line 292). The resulting overlay (lines 305-328) shows only a delete confirmation dialog. There is no edit option in this flow at all.
  implication: The FeedItem three-dot button is hardcoded to delete-only. No dropdown menu exists; it jumps straight to delete confirmation.

## Resolution

root_cause: |
  Two separate issues across two components:

  1. **PostDetailModal.tsx**: Has no three-dot menu whatsoever. Edit and delete are rendered as separate bottom-bar buttons (lines 112-116). The edit functionality (handleEdit, handleSave, isEditing state, textarea UI) is fully implemented and works -- it's just not accessible from a dropdown menu.

  2. **FeedItem.tsx**: Has a MoreHorizontal (three-dot) icon button (line 295), but it directly triggers `setShowDeleteConfirm(true)` instead of opening a dropdown menu. There is no dropdown/popover component. The delete confirmation overlay (lines 305-328) has no edit option.

  Neither component has a dropdown menu pattern. The three-dot button in FeedItem is a direct-action button, not a menu trigger.

fix: empty
verification: empty
files_changed: []
