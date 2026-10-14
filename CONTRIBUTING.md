# Contributing to datatesting-core

Thank you for your interest in contributing to `datatesting-core`! This guide explains how to set up your development environment, run tests, and submit contributions.

## Code of Conduct

By participating in this project, you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting Started

### Prerequisites

- **Java 21** or higher
- Git

Gradle is included via the Gradle wrapper (`./gradlew`).

### Build and Test

Clone the repository and verify the build:

```bash
git clone https://github.com/maakar/datatesting-core.git
cd datatesting-core
./gradlew build
```

Run tests:

```bash
./gradlew test
```

Run specific test classes:

```bash
./gradlew test --tests "org.qamethod.datatesting.core.compare.SortMergeCompareIT"
```

## Development Workflow

1. **Fork and Branch:**
   - Create a feature branch from `main`:
     ```bash
     git checkout -b feature/my-new-feature
     ```
2. **Make Changes:**
   - Keep changes focused and well-scoped.
   - Follow existing code style and conventions.
   - Use synthetic, deterministic datasets for testing under `src/test/resources/`.
3. **Testing Conventions:**
   - Unit tests: `*Test.java`
   - Integration tests (SQLite, IO, reports): `*IT.java`
4. **Commit:**
   - Write clear, concise commit messages (Conventional Commits encouraged, e.g. `feat: ...`, `fix: ...`, `test: ...`, `docs: ...`).
5. **Open a Pull Request:**
   - Push your branch to your fork and submit a PR targeting `main`.
   - Ensure all CI checks pass.

## Contributor Guides

For in-depth architecture and guidelines, please review:

- [Development Approach](docs/contributors/development-approach.md) - principles, coding standards, and definition of done.
- [Testing Guide](docs/contributors/testing-guide.md) - testing conventions, synthetic datasets, and test execution.
- [Branching Strategy](docs/contributors/branching-strategy.md) - branch lifecycle and release flow.

## Reporting Issues

If you find a bug or have a feature suggestion, please open an issue using the appropriate template in the GitHub issue tracker:

- Describe the problem clearly with reproduction steps or expected behavior.
- Include environment details (Java version, OS, database provider if applicable).
