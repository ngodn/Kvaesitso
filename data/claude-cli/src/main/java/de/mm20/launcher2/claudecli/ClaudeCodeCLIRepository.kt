package de.mm20.launcher2.claudecli

import android.util.Log
import de.mm20.launcher2.preferences.search.ClaudeSearchSettings
import de.mm20.launcher2.search.Searchable
import de.mm20.launcher2.search.SearchableRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOf

class ClaudeCodeCLIRepository(
    private val settings: ClaudeSearchSettings,
) : SearchableRepository<Searchable> {

    private val runner = ClaudeCommandRunner()

    // Matches file paths in Claude responses
    private val filePathPatterns = listOf(
        Regex("""/android-root/[^\s,;:'"()|]+\.\w{2,5}"""),  // /android-root/sdcard/.../file.ext
        Regex("""/android-root/[^\s,;:'"()|]+/"""),            // /android-root/sdcard/.../dir/
        Regex("""/sdcard/[^\s,;:'"()|]+\.\w{2,5}"""),         // /sdcard/.../file.ext
        Regex("""/storage/emulated/0/[^\s,;:'"()|]+\.\w{2,5}"""), // /storage/emulated/0/.../file.ext
        // Any filename with media extension (captures bare filenames in text)
        Regex("""\b[\w._-]+\.(?:jpg|jpeg|png|gif|webp|mp4|mov|mkv|webm|mp3|m4a|pdf|apk|zip)\b""", RegexOption.IGNORE_CASE),
    )

    override fun search(query: String, allowNetwork: Boolean): Flow<ImmutableList<Searchable>> {
        // Claude CLI runs locally via chroot — don't gate on allowNetwork

        return combineTransform(
            settings.enabled,
            settings.model,
            settings.minQueryLength,
        ) { enabled, model, minQueryLength ->
            emit(persistentListOf())

            if (!enabled) { Log.d("ClaudeCLI", "Disabled"); return@combineTransform }
            if (query.length < minQueryLength) { Log.d("ClaudeCLI", "Query too short: ${query.length} < $minQueryLength"); return@combineTransform }
            if (query.isBlank()) return@combineTransform

            Log.d("ClaudeCLI", "Executing query: $query (model=$model)")
            val resultText = runner.execute(query, model)
            if (resultText == null) { Log.e("ClaudeCLI", "Runner returned null"); return@combineTransform }
            Log.d("ClaudeCLI", "Got result: ${resultText.take(200)}")

            val results = mutableListOf<Searchable>()

            // Position 0: summary text result
            results.add(
                ClaudeResult(
                    title = query,
                    description = resultText,
                    type = ClaudeResult.ClaudeResultType.TEXT,
                )
            )

            // Find directory context from the response (e.g., "/android-root/sdcard/Pictures/MyInsta/")
            val dirContext = Regex("""/(?:android-root)?/(?:sdcard|storage/emulated/0)/[^\s,;:'"()|]*?/""")
                .findAll(resultText)
                .map { it.value.removePrefix("/android-root").trimEnd('/') + "/" }
                .lastOrNull() ?: "/sdcard/DCIM/Camera/"

            // Extract file path results from all patterns, deduplicate by filename
            val filesByName = mutableMapOf<String, String>() // filename -> full path
            for (pattern in filePathPatterns) {
                pattern.findAll(resultText).forEach { match ->
                    var path = match.value.trimEnd('.', ',', ')', '|')
                    path = path.removePrefix("/android-root")
                    if (!path.startsWith("/")) {
                        path = dirContext + path
                    }
                    if (path.contains('.') && !path.endsWith("/")) {
                        val fileName = path.substringAfterLast('/')
                        // Prefer longer paths (more specific)
                        val existing = filesByName[fileName]
                        if (existing == null || path.length > existing.length) {
                            filesByName[fileName] = path
                        }
                    }
                }
            }

            for ((fileName, filePath) in filesByName) {
                results.add(
                    ClaudeResult(
                        title = fileName,
                        description = filePath,
                        type = ClaudeResult.ClaudeResultType.FILE,
                        path = filePath,
                    )
                )
            }

            emit(results.toPersistentList())
        }
    }
}
