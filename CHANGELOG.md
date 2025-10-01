# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.8] - 2025-01-30

### Fixed
- Fixed JitPack build issues with Java 21 and JPMS modules
- Added `jitpack.yml` configuration to ensure OpenJDK 21 is used
- Updated Maven compiler plugin configuration for better compatibility
- Resolved GitHub Packages version conflict by bumping to 1.4.8

### Documentation
- Added troubleshooting section for module-path errors in JitPack guide

## [1.4.7] - 2025-01-30

### Breaking Changes
- **Maven GroupId Migration**: Changed from `io.development.n11tech` to `io.github.n11tech` to follow Maven Central naming conventions for GitHub-hosted projects
  - Old coordinates: `io.development.n11tech:microswitch`
  - New coordinates: `io.github.n11tech:microswitch`
  - JPMS module name changed: `io.github.n11tech.microswitch`
  - Update your dependencies and module-info.java accordingly

### Added
- **JitPack Support**: Added primary distribution through JitPack for truly public access without authentication
  - Users can now use `com.github.n11tech:microswitch:v1.4.7` via JitPack
  - No GitHub token required for public consumption
  - Added JitPack badge to README
  - Created comprehensive JITPACK_GUIDE.md for users and maintainers
  - Created MIGRATION_GUIDE.md for GroupId migration instructions
  - Created CONSUMER_EXAMPLE.md with usage examples

### Changed
- Updated all documentation to reflect new `io.github.n11tech` groupId
- README now lists JitPack as the primary recommended installation method
- GitHub Packages remains as secondary option (requires authentication)
- Simplified GitHub Actions publish workflow by removing version checking and deletion stages
- Updated all JPMS module references to `io.github.n11tech.microswitch`

### Fixed
- Removed unnecessary `requires java.desktop;` from module-info.java

### Documentation
- Updated README.md with dual installation options (JitPack and GitHub Packages)
- Added comprehensive migration guide for consumers
- Enhanced installation instructions with authentication requirements clarification
- Added JitPack usage guide with version options and troubleshooting

## [1.4.6] - 2025-01-29

### Added
- Enhanced thread safety and performance optimizations across all deployment strategies
- Improved error handling and logging consistency with structured prefixes
- Advanced numeric type equivalence handling in DeepObjectComparator
- Comprehensive validation logic refactoring in DeployTemplate base class

### Changed
- Migrated from static fields to instance fields for better thread safety
- Enhanced logging with context-specific prefixes ([MICROSWITCH-CANARY], [MICROSWITCH-SHADOW], [MICROSWITCH-BLUEGREEN])
- Improved cognitive complexity reduction in percentage parsing methods
- Updated repository governance documentation (CODE_OF_CONDUCT.md, CONTRIBUTING.md)

### Fixed
- Fixed thread-safety issues in Canary strategy with volatile field access
- Resolved "Package 'java.util.logging' is declared in module 'java.logging'" JPMS error by switching to SLF4J
- Fixed "Could not autowire" errors in MicroswitchAutoConfiguration with proper conditional bean creation
- Corrected numeric type comparison issues causing false positive differences in Shadow strategy
- Fixed "Calls to boolean method always inverted" anti-pattern in DeepObjectComparator

### Security
- Updated dependency management and security scanning with CycloneDX SBOM generation
- Enhanced module boundaries with JPMS for better encapsulation

## [1.2.2] - 2025-09-22

### Added
- Shadow deep comparator tuning with performance safeguards.
  - `shadow.comparator.mode: enable|disable`
  - `maxCollectionElements` to activate sampling for very large lists
  - `maxCompareTimeMillis` to enforce comparison time budgets
  - `enableSamplingOnHuge` to toggle sampling
  - `stride` to control sampling step size

### Changed
- Operational visibility: WARN logs when sampling activates or time budget is exceeded.
- Documentation updated with configuration examples and guidance.

### Fixed
- Opened `com.microswitch.domain.strategy` to `spring.core` in `module-info.java` to allow Spring test reflection (tests only).
- Ensured test configuration uses deep comparator via `test-service` in `application.yml`.

### Compatibility
- Legacy `shadow.comparator: enable|disable` continues to map to `shadow.comparator.mode`.
- New comparator fields are optional and have safe defaults; no breaking changes.

## [1.1.2] - 2025-08-15

### Added
- Detailed execution logging toggle via `microswitch.logger`.
- Configurable deep object comparison for Shadow strategy.

### Changed
- Improved logging, error handling, and configuration validation.
- Constructor injection in auto-configuration for more reliable DI.

### Fixed
- Reliable bean availability when `microswitch.enabled=false`.

## [1.1.0] - 2025-07-01

### Added
- Configuration-driven strategy selection via `DeploymentManager.execute()`.
- `activeStrategy` per-service configuration.

### Deprecated
- `canary()`, `shadow()`, and `blueGreen()` methods (planned removal in v2.0.0).


[1.4.6]: https://github.com/n11tech/microswitch/compare/v1.2.2...v1.4.6
[1.2.2]: https://github.com/n11tech/microswitch/compare/v1.1.2...v1.2.2
[1.1.2]: https://github.com/n11tech/microswitch/compare/v1.1.0...v1.1.2
[1.1.0]: https://github.com/n11tech/microswitch/releases/tag/v1.1.0
