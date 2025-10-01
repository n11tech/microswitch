# GroupId Migration Guide

## Migration from io.development.n11tech to io.github.n11tech

This guide helps consumers of the microswitch library migrate to the new Maven coordinates.

### Why the Change?

The groupId has been changed from `io.development.n11tech` to `io.github.n11tech` to:
- Follow Maven Central naming conventions for GitHub-hosted projects
- Ensure future compatibility with Maven Central publishing
- Align with standard practices (no domain ownership required for `io.github.*`)

### Migration Steps

#### 1. Update Maven Dependencies

**Old:**
```xml
<dependency>
  <groupId>io.development.n11tech</groupId>
  <artifactId>microswitch</artifactId>
  <version>1.4.8</version>
</dependency>
```

**New:**
```xml
<dependency>
  <groupId>io.github.n11tech</groupId>
  <artifactId>microswitch</artifactId>
  <version>1.4.8</version>
</dependency>
```

#### 2. Update Gradle Dependencies

**Old:**
```gradle
implementation 'io.development.n11tech:microswitch:1.4.8'
```

**New:**
```gradle
implementation 'io.github.n11tech:microswitch:1.4.8'
```

#### 3. Update JPMS module-info.java (if using Java modules)

**Old:**
```java
module your.app {
    requires io.development.n11tech.microswitch;
}
```

**New:**
```java
module your.app {
    requires io.github.n11tech.microswitch;
}
```

#### 4. Update Component Scanning (if needed)

If you explicitly added the package to component scanning:

**Old:**
```java
@ComponentScan(basePackages = {"com.yourpackage", "io.development.n11tech"})
```

**New:**
```java
@ComponentScan(basePackages = {"com.yourpackage", "io.github.n11tech"})
```

### GitHub Packages Repository Configuration

The GitHub Packages repository configuration remains the same:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/n11tech/microswitch</url>
    </repository>
</repositories>
```

### Recommended: Use JitPack Instead

For easier consumption without authentication, consider using JitPack:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.n11tech</groupId>
    <artifactId>microswitch</artifactId>
    <version>v1.4.8</version>
</dependency>
```

**Benefits of JitPack:**
- No authentication required
- Works with public GitHub repositories
- Automatic builds from tags
- Same JPMS module name (`io.github.n11tech.microswitch`)

### Version Compatibility

- Last version with old groupId: `io.development.n11tech:microswitch:1.4.6`
- First version with new groupId: `io.github.n11tech:microswitch:1.4.7`
- Latest stable version: `io.github.n11tech:microswitch:1.4.8`

### FAQ

**Q: Do I need to change my code?**
A: No, only the dependency coordinates and module name (if using JPMS) need to be updated.

**Q: Will the old coordinates still work?**
A: The old coordinates will continue to work for existing versions, but new releases will only be published under the new groupId.

**Q: Is this a breaking change?**
A: Yes, this is a breaking change at the dependency level, but not at the API level. Your code will work exactly the same after updating the coordinates.

### Support

If you encounter any issues during migration, please open an issue at:
https://github.com/n11tech/microswitch/issues
