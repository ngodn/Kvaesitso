package de.mm20.launcher2.ui.launcher.search.claude

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.mm20.launcher2.claudecli.ClaudeResult
import de.mm20.launcher2.ui.launcher.search.common.list.ListItemSurface

fun LazyGridScope.ClaudeResults(
    results: List<ClaudeResult>,
    isLoading: Boolean,
    reverse: Boolean,
) {
    // Show loading indicator when Claude is thinking
    if (isLoading && results.isEmpty()) {
        item(
            key = "claude-loading",
            contentType = "claude-loading",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            ListItemSurface(
                isFirst = true,
                isLast = true,
                reverse = reverse,
            ) {
                ClaudeLoadingIndicator()
            }
        }
        return
    }

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

@Composable
private fun ClaudeLoadingIndicator() {
    val transition = rememberInfiniteTransition(label = "claude-loading")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✦",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Claude is thinking…",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.alpha(alpha),
        )
    }
}
