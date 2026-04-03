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
        Regex("""/android-root/[^\s,;:'"()|]+"""),     // /android-root/sdcard/...
        Regex("""/sdcard/[^\s,;:'"()|]+\.\w{2,4}"""),  // /sdcard/.../file.ext
        Regex("""/storage/emulated/0/[^\s,;:'"()|]+\.\w{2,4}"""), // /storage/emulated/0/.../file.ext
        Regex("""(?:PXL|IMG|VID|DSC|MVIMG)_\d+[^\s,;:'"()|]*\.\w{2,4}"""), // PXL_20260401_230817533.jpg
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

            // Extract file path results from all patterns
            val filePaths = mutableSetOf<String>()
            for (pattern in filePathPatterns) {
                pattern.findAll(resultText).forEach { match ->
                    var path = match.value.trimEnd('.', ',', ')', '|')
                    // Normalize to /sdcard/ prefix
                    path = path.removePrefix("/android-root")
                    if (!path.startsWith("/")) {
                        // Bare filename like PXL_xxx.jpg — try to find directory from context
                        val dirMatch = Regex("""/(?:android-root)?/sdcard/[^\s,;:'"()|]+/""")
                            .find(resultText)?.value?.removePrefix("/android-root")
                        path = (dirMatch ?: "/sdcard/DCIM/Camera/") + path
                    }
                    filePaths.add(path)
                }
            }

            for (filePath in filePaths) {
                val fileName = filePath.substringAfterLast('/')
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
