package de.mm20.launcher2.claudecli

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.mm20.launcher2.search.Searchable

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
}
