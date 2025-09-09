# Semantic Versioning for Microswitch

This project follows [Semantic Versioning (SemVer)](https://semver.org/) principles using a simple, plugin-free approach.

## Version Format

**Current Version**: `1.0.0`

Format: `MAJOR.MINOR.PATCH`

- **MAJOR**: Incompatible API changes
- **MINOR**: Backwards-compatible functionality additions  
- **PATCH**: Backwards-compatible bug fixes

## Configuration

The pom.xml includes semantic versioning properties for reference:

```xml
<!-- Semantic Versioning Configuration -->
<project.version.major>1</project.version.major>
<project.version.minor>0</project.version.minor>
<project.version.patch>0</project.version.patch>
<project.version.snapshot>false</project.version.snapshot>
```

## Manual Version Management

### 1. Update Version in pom.xml

To change versions, manually update line 16 in `pom.xml`:

```xml
<version>1.0.0</version>  <!-- Update this line -->
```

### 2. Update Properties (Optional)

Update the semantic versioning properties to match:

```xml
<project.version.major>1</project.version.major>
<project.version.minor>0</project.version.minor>
<project.version.patch>0</project.version.patch>
```

## Version Increment Guidelines

### PATCH Version (1.0.0 → 1.0.1)
**When**: Bug fixes, security patches, documentation updates

**Examples**:
- Fix null pointer exceptions
- Performance improvements
- Documentation corrections
- Internal refactoring

### MINOR Version (1.0.0 → 1.1.0)
**When**: New features, backwards-compatible changes

**Examples**:
- Add new deployment strategies
- New configuration options
- Enhanced existing functionality
- New public APIs

### MAJOR Version (1.0.0 → 2.0.0)
**When**: Breaking changes, incompatible API changes

**Examples**:
- Remove public APIs
- Change method signatures
- Modify configuration structure
- Change behavior that breaks existing usage

## Development Workflow

### 1. Development Versions
For development, append `-SNAPSHOT` to version:
```xml
<version>1.1.0-SNAPSHOT</version>
```

### 2. Release Process
1. Remove `-SNAPSHOT` from version
2. Build and test: `mvn clean test`
3. Commit changes: `git commit -am "Release version 1.1.0"`
4. Create tag: `git tag -a v1.1.0 -m "Release version 1.1.0"`
5. Push: `git push origin main --tags`

### 3. Next Development Cycle
1. Increment version appropriately
2. Add `-SNAPSHOT` suffix
3. Commit: `git commit -am "Prepare for next development iteration"`

## Pre-release Versions

For testing and pre-releases:

```xml
<!-- Alpha release -->
<version>1.1.0-alpha.1</version>

<!-- Beta release -->
<version>1.1.0-beta.1</version>

<!-- Release candidate -->
<version>1.1.0-rc.1</version>
```

## Best Practices

1. **Always use three numbers**: `1.0.0` not `1.0`
2. **Start from 1.0.0**: Avoid `0.x.x` for production releases
3. **Use SNAPSHOT for development**: `1.1.0-SNAPSHOT`
4. **Tag releases consistently**: `v1.0.0`, `v1.1.0`, etc.
5. **Document breaking changes**: Update README and CHANGELOG
6. **Test before release**: Always run full test suite

## Integration with CI/CD

Your GitHub Actions can trigger on semantic version tags:

```yaml
on:
  push:
    tags:
      - 'v[0-9]+.[0-9]+.[0-9]+'  # Matches v1.0.0, v2.1.3, etc.
```

## Example Version History

```
v1.0.0 - Initial release
v1.0.1 - Bug fixes
v1.1.0 - New canary deployment features
v1.1.1 - Performance improvements
v2.0.0 - Breaking API changes
```

## Quick Reference

| Change Type | Version Update | Example |
|-------------|----------------|---------|
| Bug fix | PATCH | 1.0.0 → 1.0.1 |
| New feature | MINOR | 1.0.0 → 1.1.0 |
| Breaking change | MAJOR | 1.0.0 → 2.0.0 |
| Development | Add -SNAPSHOT | 1.1.0-SNAPSHOT |
| Pre-release | Add suffix | 1.1.0-alpha.1 |

This simple approach gives you full control over versioning without complex tooling.
