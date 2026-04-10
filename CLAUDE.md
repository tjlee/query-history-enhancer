# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew buildPlugin        # Build the plugin
./gradlew check              # Run tests
./gradlew verifyPlugin       # Verify plugin compatibility
./gradlew runIde             # Run IDE sandbox for manual testing
./gradlew publishPlugin      # Publish to JetBrains Marketplace (requires env vars)
```

Run configurations are also available in `.run/` for use inside IntelliJ.

## Architecture

This is an **IntelliJ Platform plugin** built with Kotlin and Gradle (Kotlin DSL). The plugin targets IntelliJ IDEA 2025.2+ (platformSinceBuild: 252).

**Key configuration:**
- Plugin ID: `com.github.tjlee.queryhistoryenhancer`
- Plugin metadata and platform versions: `gradle.properties`
- Dependency catalog: `gradle/libs.versions.toml`
- Plugin extension declarations: `src/main/resources/META-INF/plugin.xml`

**Source layout:** `src/main/kotlin/com/github/tjlee/queryhistoryenhancer/`
- `services/` — Project-level services (IntelliJ service pattern)
- `startup/` — `PostStartupActivity` implementations (run after project opens)
- `toolWindow/` — `ToolWindowFactory` implementations
- `MyBundle.kt` — i18n wrapper for `messages/MyBundle.properties`

**Plugin lifecycle:** `MyProjectActivity` (post-startup) → tool window created on first access via `MyToolWindowFactory` → UI interacts with `MyProjectService`.

**Note:** The scaffold currently contains template sample code (random number generator, sample rename test). This code is placeholder and should be replaced with actual query history enhancement functionality. Corresponding `plugin.xml` extension registrations must be updated alongside any service/factory changes.

## Testing

Tests live in `src/test/kotlin/`. Test data for file-based tests goes in `src/test/testData/`. The `MyPluginTest` class shows the patterns for light fixture tests and rename refactoring tests.

## CI/CD

- `.github/workflows/build.yml` — runs tests, Qodana analysis, and plugin verification on PRs
- `.github/workflows/release.yml` — publishes to JetBrains Marketplace on release tags
- `qodana.yml` — uses `jvm-community` Qodana profile
