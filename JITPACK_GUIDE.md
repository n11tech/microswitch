# JitPack Usage Guide for Microswitch

## For Library Users (No Authentication Required!)

### Maven Setup

Add JitPack repository and the dependency:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.n11tech</groupId>
        <artifactId>microswitch</artifactId>
        <version>v1.4.7</version>
    </dependency>
</dependencies>
```

### Gradle Setup

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.n11tech:microswitch:v1.4.7'
}
```

### Version Options

You can use various version formats:

```xml
<!-- Release tags -->
<version>v1.4.7</version>        <!-- Latest release -->
<version>v1.4.6</version>        <!-- Previous release -->

<!-- Branch snapshots -->
<version>main-SNAPSHOT</version>  <!-- Latest from main branch -->
<version>master-SNAPSHOT</version><!-- Latest from master branch -->

<!-- Commit hash -->
<version>1234abcd</version>       <!-- Specific commit (first 10 chars) -->

<!-- Latest release -->
<version>latest.release</version> <!-- Always use latest tag -->
```

## For Library Maintainers (You!)

### How to Release a New Version

1. **Create and push a tag:**
```bash
git tag v1.4.8
git push origin v1.4.8
```

2. **That's it!** JitPack will automatically:
   - Detect the new tag
   - Build your project
   - Make it available to users

### Build Status & Logs

Check build status at:
- https://jitpack.io/#n11tech/microswitch

View specific version build:
- https://jitpack.io/#n11tech/microswitch/v1.4.7

### JitPack Build Badge

Add this to your README for build status:

```markdown
[![](https://jitpack.io/v/n11tech/microswitch.svg)](https://jitpack.io/#n11tech/microswitch)
```

## How JitPack Builds Your Project

JitPack automatically:
1. Clones your repository
2. Checks out the requested tag/branch/commit
3. Runs `mvn install -DskipTests` (or `gradle build`)
4. Publishes the artifacts

## Advantages Over GitHub Packages

| Feature | JitPack | GitHub Packages |
|---------|---------|-----------------|
| **Authentication** | ❌ Not required | ✅ Always required |
| **Setup** | None | Configure tokens |
| **GroupId** | com.github.n11tech | io.github.n11tech |
| **Public Access** | ✅ True public access | ⚠️ Token needed |
| **Build Triggers** | Automatic on tag | Manual workflow |
| **Multi-module** | ✅ Supported | ✅ Supported |

## Module Support (JPMS)

Users with module-info.java should use:

```java
module com.example.app {
    requires io.github.n11tech.microswitch;  // Module name stays the same!
    requires spring.boot;
    requires spring.boot.autoconfigure;
}
```

Note: The module name (`io.github.n11tech.microswitch`) remains the same regardless of the Maven coordinates.

## Troubleshooting

### Build Failed on JitPack
- Check build log at: https://jitpack.io/#n11tech/microswitch/TAG/build.log
- Ensure your project builds with: `mvn clean install`
- JitPack uses Java 8, 11, 17, or 21 (auto-detected from pom.xml)

### Module-Path Error
If you see `invalid flag: --module-path` error:
- Ensure `jitpack.yml` exists with `jdk: openjdk21`
- The project uses Java 21 and JPMS modules
- JitPack must use Java 21 to build correctly

### Dependency Not Found
```
Could not find com.github.n11tech:microswitch:vX.X.X
```
- Ensure the tag exists on GitHub
- Check if build passed at: https://jitpack.io/#n11tech/microswitch
- First-time builds take 2-3 minutes

### Wrong Version Downloaded
- Clear local Maven cache: `rm -rf ~/.m2/repository/com/github/n11tech/microswitch`
- Force update: `mvn clean install -U`

## Examples

### Simple Spring Boot App Using Microswitch via JitPack

```java
package com.example;

import com.microswitch.infrastructure.manager.DeploymentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class DemoApp {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public static void main(String[] args) {
        SpringApplication.run(DemoApp.class, args);
    }
    
    @GetMapping("/api/test")
    public String test() {
        return deploymentManager.execute(
            () -> "Stable Version 1.0",
            () -> "Experimental Version 2.0",
            "demo-service"
        );
    }
}
```

### Configuration (application.yml)

```yaml
microswitch:
  enabled: true
  services:
    demo-service:
      activeStrategy: canary
      canary:
        percentage: 80/20
        algorithm: SEQUENCE
```

## Quick Test Commands

Test if JitPack can resolve your library:
```bash
# Create a minimal pom.xml and test
curl -o /tmp/test-pom.xml https://raw.githubusercontent.com/n11tech/microswitch/main/JITPACK_GUIDE.md
cd /tmp
mvn dependency:resolve
```

## Links

- **Your JitPack Page**: https://jitpack.io/#n11tech/microswitch
- **Latest Build Status**: https://jitpack.io/#n11tech/microswitch/latest
- **JitPack Documentation**: https://jitpack.io/docs/
