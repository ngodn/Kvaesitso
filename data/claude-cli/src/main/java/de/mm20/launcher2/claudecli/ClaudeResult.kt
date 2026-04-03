package de.mm20.launcher2.claudecli

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import de.mm20.launcher2.search.Searchable
import java.io.File

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

    /** The actual Android filesystem path (strip /android-root prefix, resolve /sdcard/) */
    val androidPath: String?
        get() = path?.removePrefix("/android-root")
            ?.let { if (it.startsWith("/sdcard/")) it.replace("/sdcard/", "/storage/emulated/0/") else it }

    /** MIME type inferred from file extension */
    val mimeType: String?
        get() {
            val ext = path?.substringAfterLast('.', "")?.lowercase() ?: return null
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true

    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true

    val isAudio: Boolean
        get() = mimeType?.startsWith("audio/") == true

    /** Get a File object for the actual Android path */
    fun getFile(): File? {
        val p = androidPath ?: return null
        val file = File(p)
        return if (file.exists()) file else null
    }

    /** Get a content URI for viewing/sharing the file */
    fun getContentUri(context: Context): Uri? {
        val file = getFile() ?: return null
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }

    fun launch(context: Context): Boolean {
        val intent = when {
            type == ClaudeResultType.FILE -> {
                val contentUri = getContentUri(context)
                val mime = mimeType ?: "*/*"
                if (contentUri != null) {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    // Fallback to document provider
                    val docPath = (androidPath ?: path)?.removePrefix("/sdcard/") ?: return false
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("content://com.android.externalstorage.documents/document/primary:$docPath")
                    }
                }
            }
            uri != null -> Intent(Intent.ACTION_VIEW, Uri.parse(uri))
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
