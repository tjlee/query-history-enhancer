# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew buildPlugin        # Build the plugin zip
./gradlew check              # Run tests
./gradlew verifyPlugin       # Verify plugin compatibility against recommended IDEs
./gradlew runIde             # Run IDE sandbox for manual testing
./gradlew publishPlugin      # Publish to JetBrains Marketplace (requires env vars)
```

Run configurations are also available in `.run/` for use inside IntelliJ.

## Architecture

**IntelliJ Platform plugin** (Kotlin + Gradle Kotlin DSL) targeting IntelliJ IDEA 2025.2+ (`platformSinceBuild: 252`). Depends on the bundled `com.intellij.database` plugin (DataGrip/Database Tools).

**Key configuration files:**
- `gradle.properties` — plugin ID, version, platform version, bundled plugin deps
- `gradle/libs.versions.toml` — dependency catalog
- `src/main/resources/META-INF/plugin.xml` — extension point registrations and action overrides

**All plugin source is in `src/main/kotlin/com/github/tjlee/queryhistoryenhancer/`:**

| File | Role |
|---|---|
| `QueryTimestampAuditor.kt` | Implements `DataAuditor` EP — fires after every executed SQL statement across all DB consoles |
| `QueryTimestampService.kt` | `@Service(Level.PROJECT)` + `PersistentStateComponent` — stores query→timestamp map in `.idea/queryTimestamps.xml` |
| `QueryHistoryImportActivity.kt` | `ProjectActivity` — imports pre-existing platform history files on first run |
| `QueryHistoryBrowseAction.kt` | Overrides `Console.Jdbc.BrowseHistory` — opens a custom dialog with search and preview |

**Data flow:**
```
SQL executed in any DB console
  └─ QueryTimestampAuditor.afterStatement()     [app-level EP, fires per statement]
       └─ QueryTimestampService.record(query)   [project-scoped, deduplicates, capped at 1000]
            └─ persisted to queryTimestamps.xml

Project opens (first time only)
  └─ QueryHistoryImportActivity.execute()
       └─ reads PathManager.getConfigPath()/consoles/.history/db/*.sql
            └─ QueryTimestampService.recordImported(query)  [ts=1L sentinel, no eviction of real entries]

User invokes Console.Jdbc.BrowseHistory (any DB console)
  └─ QueryHistoryDialog (DialogWrapper)
       ├─ JBList + ListWithFilter (speed search, no digit-key-close bug)
       ├─ LanguageTextField(SqlLanguage.INSTANCE) preview pane with SQL highlighting
       └─ on OK: raw SQL pasted into console editor via WriteCommandAction
```

**Important implementation details:**
- `DataAuditor` is an **application-level** EP — it fires for all consoles across all projects. The service is project-scoped, so history is per-project.
- `afterStatement()` fires **once per SQL statement**, not per script. Multi-statement scripts produce multiple calls.
- `ConsoleDataRequest` is in `com.intellij.database.run` (not `com.intellij.database.script.persistence` which doesn't exist).
- Project access from `ConsoleDataRequest`: `request.console.project` (via `JdbcConsoleCore.getProject()`), not `request.owner.project`.
- The dialog uses `ListWithFilter` (from `com.intellij.ui.speedSearch`) instead of `ContentChooser` — `ContentChooser` has a hardcoded digit-key handler that calls `doOKAction()`, which would close the dialog when typing numbers into the speed search.
- Imported entries use `ts = 1L` as a sentinel (displayed as `[imported]`). Real entries use `System.currentTimeMillis()`. `record()` always overwrites imported entries with a real timestamp.

## Platform history file format

The platform stores console history at `PathManager.getConfigPath()/consoles/.history/db/<uuid>.sql`:
```sql
SELECT * FROM users;
-- -. . -..- - / . -. - .-. -.--
INSERT INTO orders VALUES (...);
-- -. . -..- - / . -. - .-. -.--
```
The delimiter is the Morse code string `-- -. . -..- - / . -. - .-. -.--` (decodes to "next entry").

## CI/CD

- `.github/workflows/build.yml` — runs tests, Qodana analysis, and plugin verification on PRs
- `.github/workflows/release.yml` — publishes to JetBrains Marketplace on release tags
- `qodana.yml` — `jvm-community` Qodana profile
