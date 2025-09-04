# Contributing to Microswitch

Thank you for your interest in contributing to Microswitch! We welcome contributions from the community.

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check the issue list as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

* **Use a clear and descriptive title**
* **Describe the exact steps to reproduce the problem**
* **Provide specific examples to demonstrate the steps**
* **Describe the behavior you observed and what behavior you expected**
* **Include details about your configuration and environment**

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

* **Use a clear and descriptive title**
* **Provide a step-by-step description of the suggested enhancement**
* **Provide specific examples to demonstrate the steps**
* **Describe the current behavior and explain the behavior you expected**
* **Explain why this enhancement would be useful**

### Pull Requests

1. Fork the repository
2. Create a feature branch from `main`: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Add tests for your changes
5. Ensure all tests pass: `mvn clean test`
6. Ensure code quality checks pass: `mvn clean verify`
7. Commit your changes: `git commit -m 'Add amazing feature'`
8. Push to the branch: `git push origin feature/amazing-feature`
9. Open a Pull Request

### Development Setup

1. **Prerequisites**
   - Java 21 or higher
   - Maven 3.8 or higher

2. **Clone the repository**
   ```bash
   git clone https://github.com/n11-development/microswitch.git
   cd microswitch
   ```

3. **Build the project**
   ```bash
   mvn clean compile
   ```

4. **Run tests**
   ```bash
   mvn clean test
   ```

5. **Run all quality checks**
   ```bash
   mvn clean verify
   ```

### Code Style

We use Google Java Style Guide. The build includes Checkstyle validation that will enforce these standards.

* Use meaningful variable and method names
* Write comprehensive Javadoc for public APIs
* Keep methods small and focused
* Write tests for new functionality
* Follow existing patterns in the codebase

### Testing

* Write unit tests for all new functionality
* Ensure integration tests pass
* Aim for high test coverage (minimum 80%)
* Test edge cases and error conditions

### Documentation

* Update README.md if needed
* Add Javadoc for public APIs
* Update DEPLOYMENT_USAGE.md for new features
* Include examples in documentation

## Development Guidelines

### Architecture

Microswitch follows Clean Architecture principles:

* **Domain Layer**: Core business logic and interfaces
* **Application Layer**: Use cases and application services
* **Infrastructure Layer**: External concerns (metrics, configuration)

### Design Patterns Used

* **Strategy Pattern**: For deployment strategies
* **Factory Pattern**: For strategy creation
* **Facade Pattern**: For simplified API
* **Template Method**: For common deployment logic

### Adding New Deployment Strategies

1. Create a new class implementing `DeploymentStrategy`
2. Extend `DeployTemplate` for common functionality
3. Add strategy to `StrategyType` enum
4. Register in `DefaultDeploymentStrategyFactory`
5. Add configuration properties
6. Write comprehensive tests
7. Update documentation

## Release Process

1. Update version in `pom.xml`
2. Update CHANGELOG.md
3. Create release PR
4. Tag release after merge
5. GitHub Actions will handle deployment

## Getting Help

* Check existing [Issues](https://github.com/n11-development/microswitch/issues)
* Create a new issue for questions
* Join discussions in GitHub Discussions

## Recognition

Contributors will be recognized in:
* README.md contributors section
* Release notes
* GitHub contributors page

Thank you for contributing to Microswitch! 🚀
