# Query History Enhancer

![Build](https://github.com/tjlee/query-history-enhancer/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
Enhances the built-in database console query history in DataGrip and IntelliJ IDEA Database Tools.

**Features:**
- **Timestamps** — every executed query is recorded with the exact time it ran.
- **Cross-console history** — queries from all database consoles are merged into a single unified history, per project.
- **Connection source labels** — each entry shows which data source the query was run against.
- **Date range filter** — quickly narrow the list to Today, Last 7 days, Last 30 days, or All time.
- **Speed search** — type anywhere in the history dialog to filter by query text, timestamp, or connection name.
- **SQL preview pane** — selecting an entry shows the full query with SQL syntax highlighting.
- **Multi-select & delete** — select multiple entries and press Delete to remove them from history.
- **History import** — existing platform query history is imported on first run so no history is lost.

**Usage:** Open a database console and invoke **View > Recent SQL Statements** (or the toolbar history button). The enhanced history dialog replaces the default one.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Query History Enhancer"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/tjlee/query-history-enhancer/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
