# Honeycomb

## Versioning and releases

- Do not change the project version in the poms as part of a change, even for breaking or feature changes.
  Versions are set only at release time by the `Release` workflow (`.github/workflows/release.yml`), which
  takes a `release_type` input of `patch`, `minor` or `major`.
- The workflow derives the release from the current `-SNAPSHOT` version: `patch` releases that version as-is,
  `minor` and `major` bump it first. After releasing it sets the next `-SNAPSHOT` to the following patch version.
- If a change warrants a minor or major release, say so (for example in the PR description) so the right
  `release_type` is chosen when releasing. Do not bump the version manually.
