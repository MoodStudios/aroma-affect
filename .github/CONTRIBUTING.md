# Contributing to Aroma Affect

Thank you for your interest in contributing to **Aroma Affect**! This document provides guidelines and information for contributors.

> **Note:** This is a proprietary project developed by [Mood Studios](https://moodstudios.io) for [OVR Technology](https://ovrtechnology.com). External contributions are welcome but subject to approval and licensing terms.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Contact](#contact)

---

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment. We expect all contributors to:

- Be respectful and considerate in all interactions
- Accept constructive criticism gracefully
- Focus on what is best for the project and community
- Show empathy towards other contributors

---

## Getting Started

1. **Fork** the repository (if you have access)
2. **Clone** your fork locally
3. **Create a branch** for your changes
4. **Make your changes** following our guidelines
5. **Test thoroughly** before submitting
6. **Submit a Pull Request**

---

## Development Setup

### Prerequisites

- **Java Development Kit (JDK)** 21 or higher
- **Git** for version control
- **IDE** with Gradle support (IntelliJ IDEA recommended)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/MoodStudios/aroma-affect.git
cd aroma-affect

# Build all platforms
./gradlew build

# Build specific platform
./gradlew :neoforge:build
./gradlew :fabric:build

# Run the game (development)
./gradlew :neoforge:runClient
./gradlew :fabric:runClient
```

### IDE Setup

For **IntelliJ IDEA**:
1. Open the project folder
2. Import as Gradle project
3. Wait for indexing to complete
4. Run `./gradlew genSources` to generate Minecraft sources

---

## Project Structure

Aroma Affect uses [Architectury](https://architectury.dev/) for cross-platform development:

```
aroma-affect/
├── common/          # Shared platform-agnostic code
│   └── src/main/
│       ├── java/    # Common Java sources
│       └── resources/
├── fabric/          # Fabric loader implementation
│   └── src/main/
│       ├── java/    # Fabric-specific code
│       └── resources/
├── neoforge/        # NeoForge loader implementation
│   └── src/main/
│       ├── java/    # NeoForge-specific code
│       └── resources/
└── .github/         # GitHub configuration
```

### Module Guidelines

| Module | Purpose | What Goes Here |
|--------|---------|----------------|
| `common` | Shared logic | Game mechanics, data structures, utilities |
| `neoforge` | NeoForge glue | NeoForge events, registries, platform hooks |
| `fabric` | Fabric glue | Fabric events, registries, platform hooks |

> ⚠️ **Important:** Never put platform-specific imports in `common`. Use Architectury abstractions or create platform interfaces.

---

## Coding Standards

### Java Style

- Use **4 spaces** for indentation (no tabs)
- Maximum line length: **120 characters**
- Use **Lombok** annotations where appropriate (`@Getter`, `@Setter`, `@RequiredArgsConstructor`)
- Follow standard Java naming conventions:
  - `PascalCase` for classes
  - `camelCase` for methods and variables
  - `SCREAMING_SNAKE_CASE` for constants

### Documentation

- Add Javadoc to all public classes and methods
- Include `@param`, `@return`, and `@throws` where applicable
- Keep comments concise and meaningful

### Example

```java
/**
 * Handles scent emission for environmental sources.
 *
 * @author Mood Studios
 */
@RequiredArgsConstructor
public class ScentEmitter {
    
    private final ScentType scentType;
    private final float intensity;
    
    /**
     * Emits a scent at the specified position.
     *
     * @param world the game world
     * @param pos   the emission position
     * @return true if the scent was successfully emitted
     */
    public boolean emit(Level world, BlockPos pos) {
        // Implementation
    }
}
```

---

## Commit Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/) for clear and consistent commit history.

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `style` | Code style (formatting, no logic change) |
| `refactor` | Code refactoring |
| `perf` | Performance improvements |
| `test` | Adding or updating tests |
| `chore` | Maintenance tasks |
| `build` | Build system or dependencies |

### Examples

```
feat(mask): add tier 3 tracking capabilities
fix(scent): resolve emission cooldown not resetting
docs(readme): update installation instructions
refactor(common): extract scent priority logic
```

---

## Pull Request Process

1. **Update documentation** if your changes affect user-facing features
2. **Test on both platforms** (NeoForge and Fabric) when possible
3. **Keep PRs focused** - one feature or fix per PR
4. **Fill out the PR template** completely
5. **Request review** from a code owner
6. **Address feedback** promptly

### PR Checklist

Before submitting, ensure:

- [ ] Code compiles without errors
- [ ] Code follows project style guidelines
- [ ] Changes are tested in-game
- [ ] Documentation is updated if needed
- [ ] Commit messages follow conventions
- [ ] PR description clearly explains the changes

---

## Contact

For questions or contribution inquiries:

- **Email:** abraham@moodstudios.co
- **GitHub Issues:** For bug reports and feature requests

---

<p align="center">
  <strong>© 2025 OVR Technology. All rights reserved.</strong><br>
  Developed with ❤️ by <a href="https://moodstudios.io">Mood Studios</a>
</p>

