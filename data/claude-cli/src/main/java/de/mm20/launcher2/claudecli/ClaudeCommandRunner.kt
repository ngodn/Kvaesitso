package de.mm20.launcher2.claudecli

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class ClaudeCommandRunner {

    companion object {
        private const val TAG = "ClaudeCLI"
        private const val CHROOT_CMD = "/data/adb/modules/chroot-distro/system/bin/chroot-distro"
        private const val USER_HOME = "/home/maplestar"

        private const val SYSTEM_PROMPT = "You are a search assistant running inside Arch Linux on a rooted Android phone. " +
            "You have full access to: " +
            "1) The Android filesystem mounted at /android-root/. Media files can be ANYWHERE — " +
            "always search broadly with 'find' command. Common locations: " +
            "/android-root/sdcard/DCIM/ (camera photos/videos), " +
            "/android-root/sdcard/Pictures/, /android-root/sdcard/Movies/, " +
            "/android-root/sdcard/Download/, /android-root/sdcard/WhatsApp/Media/, " +
            "/android-root/sdcard/Telegram/, /android-root/sdcard/Screenshots/, " +
            "/android-root/sdcard/Android/media/ (WhatsApp, Telegram app media). " +
            "IMPORTANT: ALWAYS use 'find /android-root/sdcard/ -type f -name \"*.ext\"' to search ALL locations. " +
            "For videos search: -name \"*.mp4\" -o -name \"*.mov\" -o -name \"*.mkv\" -o -name \"*.webm\". " +
            "Sort by date with '-printf \"%T@ %p\\n\" | sort -rn | head'. " +
            "2) ADB via 'adb connect localhost:5555' — run adb shell commands for live system queries " +
            "(storage, battery, running apps, screen capture, install/uninstall, etc.). " +
            "3) Full Linux tools (find, grep, df, ffmpeg, imagemagick, etc.). " +
            "For file searches, ALWAYS use 'find' to search broadly. Be fast and direct."

        // KernelSU/Magisk su binary paths to try
        private val SU_PATHS = listOf(
            "/system/bin/su",
            "/sbin/su",
            "/system/xbin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/magisk/su",
        )

        private fun findSu(): String? {
            return SU_PATHS.firstOrNull { File(it).exists() }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun execute(query: String, model: String = "haiku"): String? = withContext(Dispatchers.IO) {
        val escapedQuery = query.replace("'", "'\\''")
        val escapedPrompt = SYSTEM_PROMPT.replace("'", "'\\''")

        val fullPath = "$USER_HOME/.local/bin:$USER_HOME/.local/share/mise/shims:$USER_HOME/.cargo/bin:" +
            "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

        // Connect ADB to localhost first (for live Android queries), then run claude
        val shellCmd = "$CHROOT_CMD login archlinux --bind /:/android-root -- " +
            "bash -c 'export PATH=$fullPath HOME=$USER_HOME; " +
            "adb connect localhost:5555 >/dev/null 2>&1; " +
            "$USER_HOME/.local/bin/claude -p \"$escapedQuery\" " +
            "--model $model " +
            "--output-format json " +
            "--permission-mode dontAsk " +
            "--allowedTools Bash,Read,Write,Glob,Grep,WebFetch,WebSearch,Agent,Skill,ToolSearch,NotebookEdit,AskUserQuestion,TodoWrite,EnterPlanMode,ExitPlanMode,EnterWorktree,ExitWorktree,CronCreate,CronDelete,CronList,RemoteTrigger,TaskOutput,TaskStop,LSP,SendMessage " +
            "--system-prompt \"$escapedPrompt\"'"

        try {
            // Try su binary directly first, fall back to sh -c su
            val suPath = findSu()
            val process = if (suPath != null) {
                Log.d(TAG, "Using su at: $suPath")
                ProcessBuilder(suPath, "-c", shellCmd)
                    .redirectErrorStream(false)
                    .start()
            } else {
                // Fallback: use sh to locate su via PATH
                Log.d(TAG, "su not found at known paths, trying sh -c 'su -c ...'")
                ProcessBuilder("sh", "-c", "su -c '$shellCmd'")
                    .redirectErrorStream(false)
                    .start()
            }

            val stderr = process.errorStream.bufferedReader().readText()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            Log.d(TAG, "Exit: $exitCode, stdout: ${output.take(300)}")
            if (stderr.isNotBlank()) Log.w(TAG, "stderr: ${stderr.take(300)}")

            if (exitCode != 0 || output.isBlank()) return@withContext null

            val jsonObj = json.parseToJsonElement(output).jsonObject
            val isError = jsonObj["is_error"]?.jsonPrimitive?.content?.toBoolean() ?: false
            if (isError) { Log.e(TAG, "Claude returned error"); return@withContext null }

            jsonObj["result"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            null
        }
    }
}
