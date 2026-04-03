# Implementation Notes

## Kvaesitso Integration Points

### Where to add the new repository

**New module:** `data/claude-cli/`

Register in `settings.gradle.kts`:
```kotlin
include(":data:claude-cli")
```

Register in `LauncherApplication.kt` Koin modules:
```kotlin
val claudeCliModule = module {
    single<ClaudeCodeCLIRepository> { ClaudeCodeCLIRepositoryImpl(get()) }
}
```

### SearchService modification

File: `services/search/src/main/java/de/mm20/launcher2/search/SearchServiceImpl.kt`

Add Claude CLI alongside existing delayed sources:

```kotlin
// Existing pattern:
launch {
    delay(250)  // Locations
    locationRepository.search(query, params).collect { ... }
}
launch {
    delay(750)  // Wikipedia
    wikipediaRepository.search(query, params).collect { ... }
}

// New:
launch {
    delay(1000)  // Claude Code CLI — fires last
    if (query.length >= 5 && settings.claudeSearchEnabled) {
        claudeCodeCLIRepository.search(query, params).collect { ... }
    }
}
```

### SearchResults modification

File: `services/search/src/main/java/de/mm20/launcher2/search/SearchResults.kt`

Add new field:
```kotlin
data class SearchResults(
    val apps: List<Application> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    // ... existing fields ...
    val claudeResults: List<ClaudeResult> = emptyList(),  // NEW
)
```

### SearchFilters modification

Add toggle:
```kotlin
data class SearchFilters(
    val apps: Boolean = true,
    // ... existing ...
    val claude: Boolean = true,  // NEW
)
```

### UI — SearchColumn

File: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/search/SearchColumn.kt`

Add new section for Claude results (rendered as cards with title + description + optional file preview).

## Process Execution Details

### Root Command Chain

```
Runtime.exec()
  → su -c '...'
    → /data/adb/modules/chroot-distro/system/bin/chroot-distro login archlinux --bind /:/android-root -- ...
      → env PATH=... HOME=... claude -p "query" --model haiku --output-format json ...
```

### Full command string

```kotlin
val command = arrayOf(
    "su", "-c",
    "/data/adb/modules/chroot-distro/system/bin/chroot-distro " +
    "login archlinux --bind /:/android-root -- " +
    "env PATH=/home/maplestar/.local/bin:/home/maplestar/.local/share/mise/shims:\$PATH " +
    "HOME=/home/maplestar " +
    "claude -p '${query.replace("'", "'\\''")}' " +
    "--model $model " +
    "--output-format json " +
    "--permission-mode dontAsk " +
    "--allowedTools 'Bash,Read,Glob,Grep' " +
    "--system-prompt '${SYSTEM_PROMPT}'"
)
```

### Non-Interactive Execution

Claude Code CLI runs fully autonomous — no permission prompts, no user interaction:

- **`-p`** — pipe mode, non-interactive, runs once and exits
- **`--permission-mode dontAsk`** — auto-denies everything except explicitly allowed tools
- **`--allowedTools 'Bash,Read,Glob,Grep'`** — only read-only tools approved, Write/Edit denied
- **`--output-format json`** — structured output for parsing

No max-turns, no timeout — let Claude run as many tool loops as it needs. Some queries need many steps (browse directories, read metadata, filter results). Don't cut them short.

This is the core value: Claude can browse `/android-root/sdcard/DCIM/`, find matching photos, read file metadata, follow directories, and return real results.
```

## Claude Response Format

### Prompt Engineering

System prompt baked into the query:

```
You are a search assistant for an Android phone. The Android filesystem is at /android-root/.
Answer the following search query. Return ONLY a JSON array of results.
Each result: {"type": "file|text|action", "title": "...", "description": "...", "path": "..."}
No explanation, no markdown, just JSON.

Query: {user_query}
```

### Response Parsing

```kotlin
data class ClaudeSearchResult(
    val type: String,      // "file", "text", "action"
    val title: String,
    val description: String,
    val path: String? = null,
    val uri: String? = null,
)
```

Map to Kvaesitso types:
- `type: "file"` → create `LocalFile` searchable with the path
- `type: "text"` → create custom `ClaudeTextResult` (display only)
- `type: "action"` → create `SearchAction` with intent

## Settings Integration

### Preferences

Add to `core/preferences`:

```kotlin
data class ClaudeSearchSettings(
    val enabled: Boolean = true,
    val model: String = "claude-haiku-4-5-latest",
    val delayMs: Long = 1000,
    val minQueryLength: Int = 5,
)
```

### Settings UI

Add under Settings > Search > Claude Code:
- Toggle enable/disable
- Model selector dropdown (haiku/sonnet)
- Delay slider (500ms - 3000ms)
- Min query length (3-10)

## Edge Cases

- **No chroot running:** Check if chroot is mounted before exec, show graceful "Claude offline" state
- **No internet:** Claude CLI needs internet for API calls — handle timeout gracefully
- **Large responses:** Truncate to first 10 results
- **Malformed JSON:** Fallback to displaying raw text as single result
- **Concurrent queries:** Cancel previous claude process when new query arrives (user still typing)
- **Special characters in query:** Escape single quotes properly

## File Permissions

Since we run via `su`, the process has root access. But Kvaesitso itself runs as a normal app. The `su` call bridges this gap. The chroot already mounts `/android-root` read-write.

## Dependencies

New module `data/claude-cli` depends on:
- `core:base` (Searchable interfaces)
- `core:preferences` (settings)
- `kotlinx-coroutines` (Flow, async)
- `kotlinx-serialization-json` (response parsing)
