# Claude Code CLI Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `ClaudeCodeCLIRepository` to Kvaesitso that runs natural language search queries via Claude Code CLI in a rooted Android chroot, returning results alongside apps, contacts, files, etc.

**Architecture:** New `data/claude-cli` module following the Wikipedia pattern — `SearchableRepository<Searchable>` returning non-savable results. Invokes `su -c 'chroot-distro login archlinux --bind /:/android-root -- claude -p "query" ...'` with a system prompt for filesystem context. Results rendered as a new section in SearchColumn. Gated behind a settings toggle.

**Tech Stack:** Kotlin, Coroutines/Flow, Koin DI, kotlinx.serialization JSON parsing, Android Process API for root command execution

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `data/claude-cli/build.gradle.kts` | Module build config |
| Create | `data/claude-cli/src/main/AndroidManifest.xml` | Empty manifest |
| Create | `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeResult.kt` | Search result data class |
| Create | `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeCommandRunner.kt` | Process execution wrapper |
| Create | `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeCodeCLIRepository.kt` | Repository implementation |
| Create | `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/Module.kt` | Koin DI registration |
| Create | `core/preferences/src/main/java/de/mm20/launcher2/preferences/search/ClaudeSearchSettings.kt` | Settings class |
| Modify | `core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt` | Add claude settings fields |
| Modify | `core/preferences/src/main/java/de/mm20/launcher2/preferences/Module.kt` | Register ClaudeSearchSettings |
| Modify | `core/base/src/main/java/de/mm20/launcher2/search/SearchFilters.kt` | Add `claude` filter |
| Modify | `services/search/src/main/java/de/mm20/launcher2/search/SearchService.kt` | Add ClaudeResult to SearchResults + wire repository |
| Modify | `services/search/src/main/java/de/mm20/launcher2/search/Module.kt` | Add claude dependency to SearchServiceImpl |
| Modify | `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/search/SearchColumn.kt` | Render claude results |
| Modify | `settings.gradle.kts` | Include new module |
| Modify | `app/app/build.gradle.kts` | Add dependency on data:claude-cli |
| Modify | `app/app/src/main/java/de/mm20/launcher2/LauncherApplication.kt` | Register claudeCliModule |

---

### Task 1: Create data/claude-cli module scaffold

**Files:**
- Create: `data/claude-cli/build.gradle.kts`
- Create: `data/claude-cli/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create build.gradle.kts**

```kotlin
// data/claude-cli/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "de.mm20.launcher2.claudecli"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.androidx.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core:base"))
    implementation(project(":core:preferences"))
}
```

- [ ] **Step 2: Create AndroidManifest.xml**

```xml
<!-- data/claude-cli/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 3: Add to settings.gradle.kts**

Add after the existing `include(":data:calculator")` line:

```kotlin
include(":data:claude-cli")
```

- [ ] **Step 4: Add dependency in app/app/build.gradle.kts**

Add after `implementation(project(":data:calculator"))`:

```kotlin
implementation(project(":data:claude-cli"))
```

- [ ] **Step 5: Verify project syncs**

Run: `cd /home/eins0fx/development/google-pixel/Kvaesitso && ./gradlew :data:claude-cli:assemble 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add data/claude-cli/ settings.gradle.kts app/app/build.gradle.kts
git commit -m "feat: add data/claude-cli module scaffold"
```

---

### Task 2: Create ClaudeResult data class

**Files:**
- Create: `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeResult.kt`

- [ ] **Step 1: Create ClaudeResult**

```kotlin
package de.mm20.launcher2.claudecli

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import de.mm20.launcher2.icons.ColorLayer
import de.mm20.launcher2.icons.StaticLauncherIcon
import de.mm20.launcher2.icons.TextLayer
import de.mm20.launcher2.search.Searchable

/**
 * A search result from Claude Code CLI.
 * Not savable — these are ephemeral results from a single query.
 */
data class ClaudeResult(
    val title: String,
    val description: String,
    val type: ClaudeResultType,
    val path: String? = null,
    val uri: String? = null,
) : Searchable {

    enum class ClaudeResultType {
        TEXT,
        FILE,
        ACTION,
    }

    /**
     * Attempts to open the result — file paths open in file manager,
     * URIs open in browser, text results are no-op.
     */
    fun launch(context: Context): Boolean {
        val intent = when {
            uri != null -> Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            path != null -> Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.externalstorage.documents/document/primary:${path.removePrefix("/sdcard/")}")
            }
            else -> return false
        }
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getIcon(): StaticLauncherIcon {
        return StaticLauncherIcon(
            foregroundLayer = TextLayer(
                text = when (type) {
                    ClaudeResultType.TEXT -> "AI"
                    ClaudeResultType.FILE -> "📄"
                    ClaudeResultType.ACTION -> "⚡"
                },
                color = 0xFF6B4CE6.toInt(),
            ),
            backgroundLayer = ColorLayer(0xFFEDE9FE.toInt()),
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeResult.kt
git commit -m "feat: add ClaudeResult data class"
```

---

### Task 3: Create ClaudeCommandRunner

**Files:**
- Create: `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeCommandRunner.kt`

- [ ] **Step 1: Create ClaudeCommandRunner**

```kotlin
package de.mm20.launcher2.claudecli

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Executes Claude Code CLI commands via su + chroot-distro.
 * Returns the parsed result text from Claude's JSON output.
 */
class ClaudeCommandRunner {

    companion object {
        private const val CHROOT_CMD = "/data/adb/modules/chroot-distro/system/bin/chroot-distro"
        private const val CLAUDE_PATH = "/home/maplestar/.local/bin/claude"
        private const val MISE_SHIMS = "/home/maplestar/.local/share/mise/shims"
        private const val USER_HOME = "/home/maplestar"

        private const val SYSTEM_PROMPT = "You are a search assistant for an Android phone. " +
            "The Android filesystem is mounted at /android-root/. Key paths: " +
            "photos at /android-root/sdcard/DCIM/Camera/, " +
            "downloads at /android-root/sdcard/Download/, " +
            "apps at /android-root/data/app/, " +
            "system info at /android-root/system/build.prop. " +
            "Be fast and direct. Return a concise answer."
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Execute a Claude Code CLI query and return the result text.
     * Returns null if the command fails or produces no output.
     */
    suspend fun execute(query: String, model: String = "haiku"): String? = withContext(Dispatchers.IO) {
        val escapedQuery = query.replace("'", "'\\''")
        val escapedPrompt = SYSTEM_PROMPT.replace("'", "'\\''")

        val command = arrayOf(
            "su", "-c",
            "$CHROOT_CMD login archlinux --bind /:/android-root -- " +
            "env PATH=$USER_HOME/.local/bin:$MISE_SHIMS:\$PATH HOME=$USER_HOME " +
            "$CLAUDE_PATH -p '$escapedQuery' " +
            "--model $model " +
            "--output-format json " +
            "--permission-mode dontAsk " +
            "--allowedTools 'Bash,Read,Glob,Grep' " +
            "--system-prompt '$escapedPrompt'"
        )

        try {
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0 || output.isBlank()) return@withContext null

            // Parse the JSON response to extract the result field
            val jsonObj = json.parseToJsonElement(output).jsonObject
            val isError = jsonObj["is_error"]?.jsonPrimitive?.content?.toBoolean() ?: false
            if (isError) return@withContext null

            jsonObj["result"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeCommandRunner.kt
git commit -m "feat: add ClaudeCommandRunner for su + chroot CLI execution"
```

---

### Task 4: Create ClaudeSearchSettings

**Files:**
- Create: `core/preferences/src/main/java/de/mm20/launcher2/preferences/search/ClaudeSearchSettings.kt`
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/LauncherSettingsData.kt`
- Modify: `core/preferences/src/main/java/de/mm20/launcher2/preferences/Module.kt`

- [ ] **Step 1: Add fields to LauncherSettingsData**

In `LauncherSettingsData.kt`, add after the `wikipediaCustomUrl` field:

```kotlin
val claudeSearchEnabled: Boolean = true,
val claudeSearchModel: String = "haiku",
val claudeSearchDelayMs: Long = 1000,
val claudeSearchMinQueryLength: Int = 5,
```

- [ ] **Step 2: Create ClaudeSearchSettings**

```kotlin
package de.mm20.launcher2.preferences.search

import de.mm20.launcher2.preferences.LauncherDataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ClaudeSearchSettings internal constructor(
    private val dataStore: LauncherDataStore,
) {
    val enabled
        get() = dataStore.data.map { it.claudeSearchEnabled }.distinctUntilChanged()

    fun setEnabled(enabled: Boolean) {
        dataStore.update { it.copy(claudeSearchEnabled = enabled) }
    }

    val model
        get() = dataStore.data.map { it.claudeSearchModel }.distinctUntilChanged()

    fun setModel(model: String) {
        dataStore.update { it.copy(claudeSearchModel = model) }
    }

    val delayMs
        get() = dataStore.data.map { it.claudeSearchDelayMs }.distinctUntilChanged()

    fun setDelayMs(delayMs: Long) {
        dataStore.update { it.copy(claudeSearchDelayMs = delayMs) }
    }

    val minQueryLength
        get() = dataStore.data.map { it.claudeSearchMinQueryLength }.distinctUntilChanged()

    fun setMinQueryLength(length: Int) {
        dataStore.update { it.copy(claudeSearchMinQueryLength = length) }
    }
}
```

- [ ] **Step 3: Register in preferences Module.kt**

Find the preferences module file and add `ClaudeSearchSettings` factory. Look for the pattern where `WikipediaSearchSettings` is registered and add alongside it:

```kotlin
factory { ClaudeSearchSettings(get()) }
```

- [ ] **Step 4: Commit**

```bash
git add core/preferences/src/main/java/de/mm20/launcher2/preferences/
git commit -m "feat: add ClaudeSearchSettings to preferences"
```

---

### Task 5: Create ClaudeCodeCLIRepository

**Files:**
- Create: `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeCodeCLIRepository.kt`

- [ ] **Step 1: Create the repository**

```kotlin
package de.mm20.launcher2.claudecli

import de.mm20.launcher2.preferences.search.ClaudeSearchSettings
import de.mm20.launcher2.search.Searchable
import de.mm20.launcher2.search.SearchableRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOf

internal class ClaudeCodeCLIRepository(
    private val settings: ClaudeSearchSettings,
    private val commandRunner: ClaudeCommandRunner = ClaudeCommandRunner(),
) : SearchableRepository<Searchable> {

    override fun search(query: String, allowNetwork: Boolean): Flow<ImmutableList<ClaudeResult>> {
        if (!allowNetwork) return flowOf(persistentListOf())

        return combineTransform(
            settings.enabled,
            settings.model,
            settings.minQueryLength,
        ) { enabled, model, minLength ->
            emit(persistentListOf())

            if (!enabled || query.length < minLength || query.isBlank()) return@combineTransform

            val resultText = commandRunner.execute(query, model) ?: return@combineTransform

            val results = parseResults(resultText)
            if (results.isNotEmpty()) {
                emit(results.toImmutableList())
            }
        }
    }

    /**
     * Parse Claude's text response into structured results.
     * Claude returns markdown-formatted text; we split by lines and
     * create results for file paths, numbered lists, and text blocks.
     */
    private fun parseResults(text: String): List<ClaudeResult> {
        val results = mutableListOf<ClaudeResult>()

        // Try to identify file paths in the response
        val filePathRegex = Regex("""/android-root/\S+""")
        val filePaths = filePathRegex.findAll(text).map { it.value }.toList()

        if (filePaths.isNotEmpty()) {
            filePaths.take(10).forEach { path ->
                val displayPath = path.removePrefix("/android-root")
                val name = path.substringAfterLast("/")
                results.add(
                    ClaudeResult(
                        title = name,
                        description = displayPath,
                        type = ClaudeResult.ClaudeResultType.FILE,
                        path = displayPath,
                    )
                )
            }
        }

        // Always add the full text response as a summary result
        if (text.isNotBlank()) {
            val summary = text.lines().take(5).joinToString("\n").take(300)
            results.add(
                0,
                ClaudeResult(
                    title = "Claude",
                    description = summary,
                    type = ClaudeResult.ClaudeResultType.TEXT,
                )
            )
        }

        return results
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/ClaudeCodeCLIRepository.kt
git commit -m "feat: add ClaudeCodeCLIRepository with result parsing"
```

---

### Task 6: Create Koin module and register

**Files:**
- Create: `data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/Module.kt`
- Modify: `app/app/src/main/java/de/mm20/launcher2/LauncherApplication.kt`

- [ ] **Step 1: Create Module.kt**

```kotlin
package de.mm20.launcher2.claudecli

import de.mm20.launcher2.search.Searchable
import de.mm20.launcher2.search.SearchableRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val claudeCliModule = module {
    single<SearchableRepository<Searchable>>(named<ClaudeResult>()) {
        ClaudeCodeCLIRepository(get())
    }
}
```

- [ ] **Step 2: Register in LauncherApplication.kt**

Add `claudeCliModule` to the `modules(listOf(...))` call in `LauncherApplication.kt`. Add the import:

```kotlin
import de.mm20.launcher2.claudecli.claudeCliModule
```

And add to the module list:

```kotlin
claudeCliModule,
```

- [ ] **Step 3: Commit**

```bash
git add data/claude-cli/src/main/java/de/mm20/launcher2/claudecli/Module.kt
git add app/app/src/main/java/de/mm20/launcher2/LauncherApplication.kt
git commit -m "feat: register claudeCliModule in Koin DI"
```

---

### Task 7: Add claude filter and results to search system

**Files:**
- Modify: `core/base/src/main/java/de/mm20/launcher2/search/SearchFilters.kt`
- Modify: `services/search/src/main/java/de/mm20/launcher2/search/SearchService.kt`
- Modify: `services/search/src/main/java/de/mm20/launcher2/search/Module.kt`

- [ ] **Step 1: Add claude to SearchFilters**

In `SearchFilters.kt`, add after the `tools` field:

```kotlin
val claude: Boolean = true,
```

- [ ] **Step 2: Add ClaudeResult to SearchResults**

In `SearchService.kt`, find the `SearchResults` data class and add:

```kotlin
val claudeResults: List<ClaudeResult>? = null,
```

Add import at top of file:

```kotlin
import de.mm20.launcher2.claudecli.ClaudeResult
```

- [ ] **Step 3: Add ClaudeCodeCLIRepository to SearchServiceImpl constructor**

Add the repository parameter to `SearchServiceImpl`:

```kotlin
private val claudeRepository: SearchableRepository<Searchable>,
```

- [ ] **Step 4: Add claude search launch in the search method**

In the `search()` method of `SearchServiceImpl`, add after the locations block (around line 251), before the closing of the `supervisorScope`:

```kotlin
if (filters.claude) {
    launch {
        delay(1000)
        claudeRepository.search(query, filters.allowNetwork)
            .collectLatest { r ->
                results.update {
                    @Suppress("UNCHECKED_CAST")
                    it.copy(claudeResults = r as List<ClaudeResult>)
                }
            }
    }
}
```

- [ ] **Step 5: Update SearchService Module.kt**

In `services/search/src/main/java/de/mm20/launcher2/search/Module.kt`, add the claude repository parameter to the `SearchServiceImpl` constructor call. Add after the last `get()`:

```kotlin
get(named<ClaudeResult>()),
```

Add import:

```kotlin
import de.mm20.launcher2.claudecli.ClaudeResult
```

- [ ] **Step 6: Commit**

```bash
git add core/base/src/main/java/de/mm20/launcher2/search/SearchFilters.kt
git add services/search/src/main/java/de/mm20/launcher2/search/
git commit -m "feat: wire ClaudeCodeCLIRepository into search system"
```

---

### Task 8: Add Claude results to SearchColumn UI

**Files:**
- Modify: `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/search/SearchColumn.kt`

- [ ] **Step 1: Add Claude results section**

In `SearchColumn.kt`, find where other result types are rendered (look for `wikipedia` or `locations` sections). Add a new section for Claude results after the existing sections. The pattern follows existing code — look for how `wikipedia` results are rendered with a header and list items, and add similarly:

```kotlin
// Claude Code results
val claudeResults = searchResults.claudeResults
if (!claudeResults.isNullOrEmpty()) {
    item {
        SearchSectionHeader(
            title = "Claude",
            icon = Icons.Rounded.AutoAwesome,
        )
    }
    items(claudeResults) { result ->
        ClaudeResultItem(
            result = result,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
```

- [ ] **Step 2: Create ClaudeResultItem composable**

Create a new file `app/ui/src/main/java/de/mm20/launcher2/ui/launcher/search/ClaudeResultItem.kt`:

```kotlin
package de.mm20.launcher2.ui.launcher.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.claudecli.ClaudeResult

@Composable
fun ClaudeResultItem(
    result: ClaudeResult,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { result.launch(context) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```

- [ ] **Step 3: Add import for Icons.Rounded.AutoAwesome**

Ensure the import is added in SearchColumn.kt:

```kotlin
import androidx.compose.material.icons.rounded.AutoAwesome
```

- [ ] **Step 4: Commit**

```bash
git add app/ui/src/main/java/de/mm20/launcher2/ui/launcher/search/
git commit -m "feat: add Claude results section to SearchColumn UI"
```

---

### Task 9: Build and verify

- [ ] **Step 1: Full project build**

Run: `cd /home/eins0fx/development/google-pixel/Kvaesitso && ./gradlew assembleDebug 2>&1 | tail -20`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Fix any compilation errors**

Address any type mismatches, missing imports, or API incompatibilities.

- [ ] **Step 3: Install on device**

```bash
adb install -r app/app/build/outputs/apk/default/debug/app-default-debug.apk
```

- [ ] **Step 4: Test search**

Open the launcher, type a query longer than 5 characters, wait ~10s for Claude results to appear below other results.

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: complete Claude Code CLI search integration"
```

---

## Notes

- **No `--bare` flag** — it breaks OAuth authentication
- **Model aliases** — use `haiku`/`sonnet`/`opus` not full model IDs
- **System prompt** — reduces tool turns from 6 to 2 (24s → 9s)
- **1000ms delay** — prevents unnecessary API calls for simple app/contact searches
- **Read-only tools** — `Bash,Read,Glob,Grep` only, no Write/Edit
- **5s startup overhead** — from CLI + chroot init, future optimization target
- **ClaudeResult is NOT SavableSearchable** — ephemeral results like Calculator, not persisted
