<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Query History Enhancer Changelog

## [Unreleased]

## [0.0.2] - 2026-04-13
### Fixed
- Multi-select delete now correctly removes all selected entries in one batch
- Delete key on macOS (⌫ / Backspace) now works in addition to the forward-delete key

### Added
- Right-click context menu on the history list with a Delete option

## [0.0.1] - 2026-04-13
### Added
- Timestamps for every executed SQL query, persisted per project
- Unified cross-console history — queries from all database consoles merged into one list
- Connection source labels — each history entry shows which data source it was run against
- Date range filter (Today / Last 7 days / Last 30 days / All time)
- Speed search — filter history by query text, timestamp, or connection name
- SQL syntax-highlighted preview pane below the history list
- Multi-select and Delete key support for removing history entries
- One-time import of existing platform query history on first run
