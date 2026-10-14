# Branching and Release Strategy

This document outlines the Git branching model and release workflow for `datatesting-core`.

## Branch Model

- **`main`**: The primary development and default branch. All features, bug fixes, and improvements target `main` via pull requests.
- **`staging`**: The pre-release integration branch. Used to validate release candidates and test suite integrations before cutting a release.
- **`feature/<name>`** or **`fix/<name>`**: Short-lived contributor branches created from `main`.
- **`release/x.y.z`**: Dedicated branch created when preparing a major or minor release version.

## Contributor Flow

1. Create a feature branch from latest `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/my-feature
   ```
2. Make focused commits and ensure all tests pass:
   ```bash
   ./gradlew check
   ```
3. Open a Pull Request targeting `main`.
4. Once reviewed and CI checks pass, the PR is merged into `main`.

## Release Workflow (Maintainers)

1. When ready to prepare a release candidate, synchronize `main` into `staging`.
2. Create release branch `release/x.y.z` from `staging`.
3. Validate candidate artifacts and documentation.
4. Tag production release (e.g. `v1.0.0`) from the release branch.
5. Merge release branch back into `main` and `staging`.
