# Security Policy

We take security seriously and appreciate responsible disclosure. This document explains how to report vulnerabilities and how we handle security updates for Microswitch.

## Supported Versions

We provide security fixes for the latest minor of the 1.x line:

| Version | Supported |
|--------:|:---------:|
| 1.x     | ✅        |

Older minors may receive fixes on a best-effort basis depending on impact and feasibility.

## Reporting a Vulnerability (Private)

Please do not open public GitHub issues for security problems. Instead, use one of the private channels below:

- GitHub Security Advisories: https://github.com/n11tech/microswitch/security/advisories/new
- Email: development@n11.com

Include as much detail as possible: affected versions, environment, a minimal reproduction, potential impact, and suggested mitigations if known.

We follow the [Code of Conduct](CODE_OF_CONDUCT.md) for all interactions.

## Our Process and SLAs

- Triage: within 48 hours we will acknowledge receipt and start triage.
- Assessment: determine severity (CVSS), impact, and affected versions.
- Fix: develop, test, and prepare patches.
- Coordinated disclosure: we will coordinate a release window with you when appropriate.
- Advisory: publish a GitHub Security Advisory with remediation guidance and credits (if desired).

## Dependency Vulnerabilities

Microswitch is built on Spring Boot and Spring Framework. Many vulnerabilities surface via transitive dependencies. Our policy:

- Track upstream CVEs affecting Spring Boot/Framework and Micrometer.
- Prefer upgrading the Spring Boot parent in `pom.xml` to pull in patched Spring Framework and transitive dependencies.
- Avoid pinning Spring modules unless necessary; rely on Spring Boot’s dependency management.

If you discover an issue in a transitive dependency used by Microswitch, please report it upstream and let us know via the private channels above.

## Keeping Your Deployment Secure (Users)

- Use the latest stable Microswitch release.
- Keep Spring Boot and Micrometer up to date.
- Enable and monitor metrics/health endpoints (Actuator) appropriately.
- Apply the principle of least privilege; log using SLF4J and centralize logs.
- Review and test deployment strategies (Canary/Shadow/Blue-Green) for failure modes.

## Contact

Security questions: development@n11.com
