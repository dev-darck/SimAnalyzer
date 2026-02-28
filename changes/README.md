# Release Notes

`Publish Release` reads curated notes from this folder before it creates a GitHub Release.

Supported files:

- `changes/<major>.<minor>.<patch>.md`
  Use this for notes that belong to a specific release version, for example `changes/0.1.0.md`.
- `changes/unreleased.md`
  Optional fallback if you want to keep one rolling notes file before cutting a release.

Resolution order during publish:

1. `changes/<version>.md`
2. `changes/unreleased.md`
3. fallback text if neither file exists

The selected file is prepended to GitHub generated release notes.
