package de.mm20.launcher2.ui.launcher.search.claude

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.mm20.launcher2.claudecli.ClaudeResult
import java.io.File

@Composable
fun ClaudeResultItem(
    result: ClaudeResult,
    modifier: Modifier = Modifier,
) {
    when (result.type) {
        ClaudeResult.ClaudeResultType.TEXT -> ClaudeTextResult(result, modifier)
        ClaudeResult.ClaudeResultType.FILE -> ClaudeFileResult(result, modifier)
        ClaudeResult.ClaudeResultType.ACTION -> ClaudeTextResult(result, modifier)
    }
}

@Composable
private fun ClaudeTextResult(
    result: ClaudeResult,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(result.description))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                },
            )
            .padding(16.dp),
    ) {
        Row(
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
                text = "Claude",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = cleanMarkdown(result.description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClaudeFileResult(
    result: ClaudeResult,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { result.launch(context) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Show image/video thumbnail if applicable
        if (result.isImage || result.isVideo) {
            val filePath = result.androidPath
            var thumbnailData by remember(filePath) { mutableStateOf<Any?>(null) }
            LaunchedEffect(filePath) {
                thumbnailData = filePath?.let {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        getMediaThumbnail(context, it, result.isVideo)
                    }
                }
            }
            if (thumbnailData != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailData)
                        .crossfade(true)
                        .build(),
                    contentDescription = result.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        result.isImage -> "🖼"
                        result.isVideo -> "🎬"
                        result.isAudio -> "🎵"
                        result.path?.endsWith("/") == true -> "📁"
                        else -> "📄"
                    },
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = result.androidPath ?: result.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun cleanMarkdown(text: String): String {
    return text
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("^-\\s+", RegexOption.MULTILINE), "• ")
        .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "• ")
        .trim()
}

/**
 * Get a thumbnail for the given media file.
 * For images: returns the File directly (Coil handles it).
 * For videos: uses ThumbnailUtils to extract a frame as Bitmap.
 * Falls back to MediaStore content URI if direct file access fails.
 */
private fun getMediaThumbnail(context: Context, filePath: String, isVideo: Boolean): Any? {
    val file = File(filePath)

    // For images, Coil can load directly from file
    if (!isVideo && file.exists()) return file

    // For videos, extract a frame with ThumbnailUtils
    if (isVideo && file.exists()) {
        try {
            return ThumbnailUtils.createVideoThumbnail(
                file,
                Size(480, 270),
                CancellationSignal(),
            )
        } catch (_: Exception) { }
    }

    // Fallback: query MediaStore by filename
    val collection = if (isVideo) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val fileName = filePath.substringAfterLast('/')
    val projection = arrayOf(MediaStore.MediaColumns._ID)
    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(fileName)

    return try {
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                ContentUris.withAppendedId(collection, id)
            } else null
        }
    } catch (_: Exception) { null }
}
