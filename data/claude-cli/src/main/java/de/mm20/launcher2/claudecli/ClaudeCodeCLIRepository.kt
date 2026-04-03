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

    // Matches paths like /android-root/sdcard/Download/foo.pdf
    private val filePathRegex = Regex("""/android-root/[^\s,;:'"()]+""")

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

            // Extract file path results
            val filePaths = filePathRegex.findAll(resultText)
                .map { it.value }
                .distinct()
                .toList()

            for (filePath in filePaths) {
                val androidPath = filePath.removePrefix("/android-root")
                val fileName = filePath.substringAfterLast('/')
                results.add(
                    ClaudeResult(
                        title = fileName,
                        description = filePath,
                        type = ClaudeResult.ClaudeResultType.FILE,
                        path = androidPath,
                    )
                )
            }

            emit(results.toPersistentList())
        }
    }
}
