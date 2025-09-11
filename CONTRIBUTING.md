# Contributing to Microswitch

Thank you for your interest in contributing to Microswitch! We welcome contributions from the community and strive to make it easy to get involved.

Microswitch is a Java 21 library built on Spring Boot 3.5.x. It exposes a small public API and hides internals using the Java Platform Module System (JPMS). The build is Maven-based and enforces quality with Checkstyle, JaCoCo, and Javadoc.

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold these standards.

## Table of Contents

- Getting Started
- Development Workflow
- Coding Standards
- Testing Standards
- Documentation Standards
- Architecture Overview
- Adding New Deployment Strategies
- Commit, PR, and Release Guidelines
- Security and Responsible Disclosure
- Getting Help

## Getting Started

1. Prerequisites
   - Java 21 (or higher)
   - Maven 3.8+ (3.9.x recommended)

2. Clone
   ```bash
   git clone https://github.com/n11-development/microswitch.git
   cd microswitch
   ```

3. Build
   ```bash
   mvn -q clean compile
   ```

4. Run Tests and Quality Gates
   ```bash
   mvn -q clean verify
   ```
   This runs unit tests (Surefire), coverage (JaCoCo), Checkstyle, sources, and Javadoc.

## Development Workflow

1. Create a branch from `main`:
   ```bash
   git checkout -b feat/short-title
   ```
2. Make small, incremental commits (see Commit Guidelines below).
3. Keep `pom.xml` aligned with the Spring Boot parent; avoid manually pinning Spring artifacts unless necessary.
4. Ensure the module system stays consistent:
   - Only export intended public APIs in `src/main/java/module-info.java`.
   - Do not introduce `java.util.logging`; use SLF4J (`org.slf4j`) instead.
5. Run the full quality pipeline before pushing:
   ```bash
   mvn clean verify
   ```

## Coding Standards

- Style: Google Java Style; enforced via Checkstyle (`maven-checkstyle-plugin`).
- Java: Target release 21 (`maven.compiler.release`).
- Logging: SLF4J API. Do not use `java.util.logging` (JPMS concerns and project convention).
- Nullability: Prefer fail-fast validation with clear exceptions and messages.
- Methods: Keep small and focused; refactor complex methods into helpers.
- Public API: Provide Javadoc for all exported public types and methods.

## Testing Standards

- Framework: JUnit Jupiter (JUnit 5) via `spring-boot-starter-test` (test scope).
- Naming: Tests must match Surefire includes: `**/*Test.java`, `**/*Tests.java`.
- Coverage: Aim for ≥80% line coverage overall; cover edge cases and error paths.
- Determinism: Avoid time- and randomness-based flakiness. If randomness is needed, inject deterministic seeds.
- Example: See `src/test/java/com/microswitch/domain/strategy/ShadowTest.java` for strategy behavior coverage.

Common commands:
```bash
mvn test          # run unit tests
mvn verify        # tests + coverage + style + docs
```

## Documentation Standards

- Update `README.md` for user-facing changes.
- Update `EXAMPLES.md` and `DEPLOYMENT_USAGE.md` when adding features or usage patterns.
- Keep `SECURITY.md` aligned with reporting channels in the Code of Conduct.
- Maintain accurate Javadoc for exported APIs.

## Architecture Overview

Microswitch follows a layered design and uses Spring Boot auto-configuration:

- Domain: strategies, value objects, and core rules.
- Application: configuration and wiring (e.g., `MicroswitchAutoConfiguration`).
- Infrastructure: integrations (Actuator, Micrometer) and external concerns.

Patterns in use:

- Strategy (deployment strategies such as Canary, Shadow, Blue/Green)
- Template Method (`DeployTemplate` consolidates shared validation/flow)
- Factory/Facade (centralized access via a minimal public API)

JPMS note: The project explicitly exports only the public API package from `module-info.java`. Keep internal packages unexported.

Spring Boot auto-config notes:

- Prefer `@ConditionalOnBean` for optional beans (e.g., Micrometer `MeterRegistry`) instead of `@ConditionalOnClass`.
- Keep autoconfiguration safe when optional dependencies are absent.

## Adding New Deployment Strategies

1. Create a concrete strategy implementing the strategy abstraction.
2. Extend `DeployTemplate` to reuse validation and shared flow.
3. Register the strategy with the executor/factory so it becomes discoverable.
4. Add configuration properties and document them.
5. Write comprehensive tests (positive/negative paths, edge cases).
6. Update `README.md`, `EXAMPLES.md`, and `DEPLOYMENT_USAGE.md`.

## Commit, PR, and Release Guidelines

### Conventional Commits

Use Conventional Commits to enable automated changelogs and semantic versioning:

- `feat: …` new feature
- `fix: …` bug fix
- `docs: …` documentation only
- `refactor: …` code change that neither fixes a bug nor adds a feature
- `test: …` tests only
- `build/chore/ci: …` tooling and process

Examples:
```text
feat(strategy): add weighted shadow mirroring
fix(canary): handle invalid percentage strings gracefully
```

### Pull Requests

Before opening a PR:

- Ensure `mvn clean verify` passes locally.
- Add or update tests for your change.
- Update docs as needed.
- Fill the PR description with context, motivation, and screenshots/logs if relevant.

PR Checklist:

- [ ] Code compiles and builds with Java 21
- [ ] Unit tests added/updated and passing
- [ ] Checkstyle passes (no new warnings/errors)
- [ ] Public APIs documented (if changed)
- [ ] Docs updated (`README.md` / `EXAMPLES.md` / `DEPLOYMENT_USAGE.md`)
- [ ] No forbidden dependencies introduced (e.g., `java.util.logging`)

### Semantic Versioning & Releases

We follow [Semantic Versioning](SEMANTIC_VERSIONING.md).

Release steps (summary):

1. Update version in `pom.xml` as appropriate (patch/minor/major).
2. Update `CHANGELOG.md` based on Conventional Commits.
3. Create a release PR and get approvals.
4. Tag after merge (CI/CD takes over per repository configuration).

## Security and Responsible Disclosure

Please review our [Security Policy](SECURITY.md). Do not open public issues for potential vulnerabilities. Follow the guidelines there and the reporting channel referenced in the Code of Conduct.

## Getting Help

- Check existing [Issues](https://github.com/n11-development/microswitch/issues)
- Open a new issue with full context and reproduction steps
- Use GitHub Discussions if enabled

Thank you for contributing to Microswitch! 🚀
