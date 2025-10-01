# Example: How to Consume Microswitch from GitHub Packages

## Quick Setup Guide

### Step 1: Create a test project

```bash
mkdir microswitch-consumer-test
cd microswitch-consumer-test
```

### Step 2: Create pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>microswitch-consumer</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <spring.boot.version>3.5.5</spring.boot.version>
    </properties>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
    </parent>

    <repositories>
        <repository>
            <id>github</id>
            <name>GitHub n11tech Apache Maven Packages</name>
            <url>https://maven.pkg.github.com/n11tech/microswitch</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- Microswitch from GitHub Packages -->
        <dependency>
            <groupId>io.github.n11tech</groupId>
            <artifactId>microswitch</artifactId>
            <version>1.4.6</version>
        </dependency>

        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

### Step 3: Configure GitHub authentication

Create `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>ghp_YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

### Step 4: Create application.yml

```yaml
microswitch:
  enabled: true
  services:
    test-service:
      enabled: true
      activeStrategy: canary
      canary:
        percentage: 80/20
```

### Step 5: Create test application

```java
package com.example;

import com.microswitch.infrastructure.manager.DeploymentManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class MicroswitchConsumerApp {
    
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(MicroswitchConsumerApp.class, args);
        
        // Test that DeploymentManager is available
        DeploymentManager deploymentManager = ctx.getBean(DeploymentManager.class);
        
        String result = deploymentManager.execute(
            () -> "Stable Version",
            () -> "Experimental Version",
            "test-service"
        );
        
        System.out.println("Result: " + result);
    }
}
```

### Step 6: Run the application

```bash
mvn clean compile
mvn spring-boot:run
```

## Troubleshooting

### Authentication Error (401)
```
Could not transfer artifact io.github.n11tech:microswitch
```
**Solution**: Check your GitHub token has `read:packages` permission

### Not Found Error (404)
```
Could not find artifact io.github.n11tech:microswitch
```
**Solution**: Ensure the package is published and the repository URL is correct

### Dependency Not Resolved
**Solution**: Force Maven to update:
```bash
mvn clean install -U
```

## Alternative: Using with JPMS

If your project uses Java modules, add to `module-info.java`:

```java
module com.example.consumer {
    requires io.github.n11tech.microswitch;
    requires spring.boot;
    requires spring.boot.autoconfigure;
}
```

## Security Best Practices

1. **Never commit tokens**: Use environment variables or encrypted secrets
2. **Use minimal permissions**: Only `read:packages` for consuming
3. **Rotate tokens regularly**: Update tokens periodically
4. **Use GitHub Secrets in CI**: Never hardcode tokens in workflows

## For Organizations

If using in an organization:
1. Consider using a machine user account for the token
2. Set up organization-wide package repository configuration
3. Use GitHub Actions with GITHUB_TOKEN for internal workflows
