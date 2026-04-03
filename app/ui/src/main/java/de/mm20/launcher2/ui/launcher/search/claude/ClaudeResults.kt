package de.mm20.launcher2.ui.launcher.search.claude

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import de.mm20.launcher2.claudecli.ClaudeResult
import de.mm20.launcher2.ui.launcher.search.common.list.ListItemSurface

fun LazyGridScope.ClaudeResults(
    results: List<ClaudeResult>,
    reverse: Boolean,
) {
    if (results.isEmpty()) return

    items(
        results.size,
        key = { "claude-${results[it].title}-$it" },
        contentType = { "claude" },
        span = {
            GridItemSpan(maxLineSpan)
        }
    ) {
        ListItemSurface(
            isFirst = it == 0,
            isLast = it == results.lastIndex,
            reverse = reverse,
        ) {
            ClaudeResultItem(result = results[it])
        }
    }
}
