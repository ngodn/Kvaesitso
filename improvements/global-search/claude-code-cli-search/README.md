# Claude Code CLI Search — Kvaesitso Integration

## Overview

A new `ClaudeCodeCLIRepository` inside Kvaesitso that enables natural language search powered by Claude Code CLI running in the Arch Linux chroot on rooted Android.

Type anything in the launcher search bar — "show cat photos from my phone", "find large files", "what meetings do I have tomorrow", "convert this image to png" — and Claude Code processes it with full access to the Android filesystem and Linux tools.

## Architecture

```
Kvaesitso Launcher (eins0fx build)
  └── services/search/SearchService
       ├── AppRepository          ← apps
       ├── ContactRepository      ← contacts
       ├── FileRepository         ← files
       ├── CalendarRepository     ← events
       ├── ...
       └── ClaudeCodeCLIRepository  ← NEW
            │
            │  Process.exec()
            ▼
       su -c 'chroot-distro login archlinux -- claude -p "query" --output-format json'
            │
            ▼
       Claude Code CLI
            ├── /android-root/sdcard/   (photos, downloads, etc.)
            ├── /android-root/data/     (app data)
            ├── /android-root/system/   (system info)
            └── Full Linux tools (ffmpeg, imagemagick, find, grep, etc.)
```

### Why This Approach

| Approach | Complexity | Components | Latency |
|----------|-----------|------------|---------|
| **ClaudeCodeCLIRepository** (chosen) | Low | 1 new module in Kvaesitso | Direct exec |
| Plugin APK + Bridge API | High | 3 components (APK + API server + CLI) | HTTP overhead |
| Plugin APK + ADB bridge | Medium | 2 components (APK + ADB relay) | ADB overhead |

Since this is a rooted device running our own fork (`de.mm20.launcher2.eins0fx`), we can modify Kvaesitso directly. No need for external plugins or bridge servers.

### How It Works

1. User types query in Kvaesitso search bar
2. `SearchService` dispatches to all repositories including `ClaudeCodeCLIRepository`
3. `ClaudeCodeCLIRepository` spawns: `su -c 'chroot-distro login archlinux -- claude -p "query" --output-format json'`
4. Claude Code CLI processes the query with access to the full filesystem
5. Response parsed into Kvaesitso `Searchable` items (files, text, actions)
6. Results appear in search alongside apps, contacts, etc.

### Delay Strategy

Following Kvaesitso's existing pattern of staggered delays for expensive sources:

| Source | Delay |
|--------|-------|
| Apps, Contacts, Calendar | 0ms (immediate) |
| Files, Shortcuts | 0ms |
| Locations | +250ms |
| Wikipedia | +750ms |
| **Claude Code CLI** | **+1000ms** (only fires if no local results match well) |

This ensures Claude is only invoked when the query doesn't match local content, avoiding unnecessary API calls for simple app/contact searches.

## Module Structure

```
data/
  claude-cli/
    src/main/java/de/mm20/launcher2/data/claude/
      ClaudeCodeCLIRepository.kt      # Repository implementation
      ClaudeResult.kt                 # Result data models
      ClaudeCommandRunner.kt          # Process execution wrapper
      ClaudeResponseParser.kt         # JSON response parser
    build.gradle.kts
```

### ClaudeCodeCLIRepository

```kotlin
class ClaudeCodeCLIRepository : SearchableRepository<SavableSearchable> {
    
    fun search(query: String, params: SearchParams): Flow<List<SavableSearchable>> {
        // 1. Skip if query matches common patterns (app names, contacts)
        // 2. Spawn claude CLI process
        // 3. Parse JSON response
        // 4. Map to Kvaesitso searchable types (File, SearchAction, etc.)
        // 5. Emit results
    }
}
```

### ClaudeCommandRunner

```kotlin
class ClaudeCommandRunner {
    
    companion object {
        private const val SYSTEM_PROMPT = "You are a search assistant for an Android phone. " +
            "The Android filesystem is mounted at /android-root/. Key paths: " +
            "photos at /android-root/sdcard/DCIM/Camera/, " +
            "downloads at /android-root/sdcard/Download/, " +
            "apps at /android-root/data/app/, " +
            "system info at /android-root/system/build.prop. " +
            "Be fast and direct."
    }

    fun execute(prompt: String, model: String = "haiku"): String {
        val cmd = arrayOf(
            "su", "-c",
            "/data/adb/modules/chroot-distro/system/bin/chroot-distro " +
            "login archlinux --bind /:/android-root -- " +
            "env PATH=/home/maplestar/.local/bin:/home/maplestar/.local/share/mise/shims:\$PATH " +
            "HOME=/home/maplestar " +
            "claude -p '${prompt.escape()}' " +
            "--model $model " +
            "--output-format json " +
            "--permission-mode dontAsk " +
            "--allowedTools 'Bash,Read,Glob,Grep' " +
            "--system-prompt '${SYSTEM_PROMPT}'"
        )
        return Runtime.getRuntime().exec(cmd).inputStream.bufferedReader().readText()
    }
}
```

## Model & Effort Testing Plan

### Goal

Find the optimal model + effort combination that is:
- Fast enough for search UX (< 3 seconds)
- Smart enough to understand diverse queries
- Cheap enough for frequent use

### Test Matrix

| Model | Effort | Expected Speed | Expected Quality | Token Cost |
|-------|--------|---------------|-----------------|------------|
| haiku | low | ~0.5-1s | Basic | Lowest |
| haiku | medium | ~1-2s | Good | Low |
| haiku | high | ~2-3s | Good+ | Medium |
| sonnet | low | ~1-2s | Good | Medium |
| sonnet | medium | ~2-4s | Very Good | Higher |
| opus | low | ~2-4s | Very Good | High |

### Test Queries

```
Category: File Search
- "find photos from last week"
- "show large files over 100mb"
- "find all pdf documents"

Category: Natural Language
- "show me cat photos"
- "what's taking up space on my phone"
- "find screenshots with text"

Category: Actions
- "convert photo.jpg to png"
- "compress all files in downloads"
- "list installed apps by size"

Category: Info
- "what android version am I running"
- "show battery usage"
- "how much storage is left"
```

### Testing Script

Create a testing playground in the chroot:

```bash
#!/bin/bash
# test-claude-search.sh — Run in Arch Linux chroot

MODELS=("haiku" "sonnet")
EFFORTS=("low" "medium" "high")
QUERIES=(
    "find photos from last week in /android-root/sdcard/DCIM"
    "show large files over 100mb in /android-root/sdcard"
    "what android version from /android-root/system/build.prop"
)

for model in "${MODELS[@]}"; do
    for effort in "${EFFORTS[@]}"; do
        for query in "${QUERIES[@]}"; do
            echo "=== $model / $effort ==="
            echo "Query: $query"
            time claude -p "$query" \
                --model "claude-$model-4-5-latest" \
                --output-format json \
                2>/dev/null
            echo
        done
    done
done
```

### Success Criteria

| Metric | Target |
|--------|--------|
| Response time | < 3 seconds for simple queries |
| Accuracy | Correct file/info for 80%+ of queries |
| Token usage | < 5K tokens per query (input + output) |
| Model recommendation | Best quality/speed ratio |

## Result Types

Claude CLI responses mapped to Kvaesitso searchable types:

| Claude Output | Kvaesitso Type | Example |
|--------------|---------------|---------|
| File paths | `File` (LocalFile) | Photos, documents, downloads |
| Text answers | `SearchAction` (custom) | "Android 15", "42GB free" |
| Commands/actions | `SearchAction` (intent) | Open app, run command |
| URLs | `Website` | Web results |

## Settings

New settings section: **Search > Claude Code**

| Setting | Default | Description |
|---------|---------|-------------|
| Enable | true | Toggle Claude search |
| Model | haiku | Model to use (haiku/sonnet/opus) |
| Delay | 1000ms | Delay before invoking (avoids unnecessary calls) |
| Min query length | 5 | Minimum characters before invoking |

## Prerequisites

- Rooted Android with KernelSU/Magisk
- chroot-distro with Arch Linux installed
- Claude Code CLI installed and authenticated in chroot
- ADB TCP enabled (arch-login script handles this)

## Build Variant

This feature lives exclusively in the `eins0fx` build variant:

| Variant | Application ID | Notes |
|---------|---------------|-------|
| eins0fx | `de.mm20.launcher2.eins0fx` | Debuggable + with our improvements |

Not intended for upstream merge — this is a personal enhancement for rooted devices with the Arch Linux chroot setup.
