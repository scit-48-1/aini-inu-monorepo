# Image Asset Source Of Truth

`common-docs/images` is the single source of truth for frontend image assets.

## Rules

- Add, replace, or remove image files only in this directory.
- `aini-inu-frontend` consumes these files through symbolic links.
- Keep existing runtime URL contracts stable:
  - `/images/dog-portraits/*`
  - `/AINIINU_ROGO_B.png`
  - `/AINIINU_ROGO_W.png`
  - `/favicon.ico`

## Update Flow

1. Edit files in `common-docs/images`.
2. Run `npm run check:assets` in `aini-inu-frontend`.
3. Verify with `npm run build` in `aini-inu-frontend`.

## File Naming

- Preserve existing names and extensions.
- Filenames with spaces are allowed and currently used by the app.
