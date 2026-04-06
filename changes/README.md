# Release Notes

This directory contains curated Markdown that is prepended to GitHub generated release notes by the `Publish Release`
workflow.

## File layout

- `changes/<major>.<minor>.<patch>.md`
  Release notes fixed to a specific version, for example `changes/0.1.0.md`.
- `changes/unreleased.md`
  Rolling draft used until a version-specific file is created.

## Resolution order during publish

The publish workflow selects release notes in this order:

1. `changes/<version>.md`
2. `changes/unreleased.md`
3. fallback text if neither file exists

The selected Markdown is passed to `gh release create --notes ... --generate-notes`.

## Writing rules

- Write release notes as product documentation.
- Prefer user-facing changes over implementation detail.
- Group notes by feature area or workflow.
- Keep the text concise and scannable.
- Avoid temporary reminders, personal comments, or internal instructions.
- Keep `unreleased.md` as a draft that can later be moved or copied into `changes/<version>.md`.

## Recommended workflow

1. Update `changes/unreleased.md` while work is still in progress.
2. Rewrite the draft into a cleaner release-facing form before publish.
3. When the version is finalized, move or copy the content into `changes/<version>.md`.
