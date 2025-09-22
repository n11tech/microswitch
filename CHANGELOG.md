# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and adheres to Semantic Versioning.

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

## [1.1.2] - 2025-??-??

### Added
- Detailed execution logging toggle via `microswitch.logger`.
- Configurable deep object comparison for Shadow strategy.

### Changed
- Improved logging, error handling, and configuration validation.
- Constructor injection in auto-configuration for more reliable DI.

### Fixed
- Reliable bean availability when `microswitch.enabled=false`.

## [1.1.0] - 2025-??-??

### Added
- Configuration-driven strategy selection via `DeploymentManager.execute()`.
- `activeStrategy` per-service configuration.

### Deprecated
- `canary()`, `shadow()`, and `blueGreen()` methods (planned removal in v2.0.0).


[1.2.2]: https://github.com/n11-development/microswitch/compare/v1.1.2...v1.2.2
