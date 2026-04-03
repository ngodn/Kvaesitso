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

        private const val SYSTEM_PROMPT = "You are a search assistant on a rooted Android phone running Arch Linux chroot. " +
            "RESPONSE RULES: " +
            "- Be concise. No explanations, no summaries, no follow-up questions. " +
            "- For file queries: list file paths one per line, max 10 results. Short label before each if helpful. " +
            "- For info queries: just the answer, minimal text. " +
            "- NEVER say 'here is what I found' or 'would you like me to'. " +
            "SEARCH RULES: " +
            "- Android filesystem at /android-root/sdcard/. " +
            "- For photos/videos: search /android-root/sdcard/DCIM/ and /android-root/sdcard/Pictures/ FIRST. " +
            "  Skip cache dirs (Android/data/, .thumbnails, .Statuses, .trash). " +
            "- Sort by date: 'find ... -printf \"%T@ %p\\n\" | sort -rn | head -10'. " +
            "- Limit results to what was asked (e.g., '3 recent' = only 3). " +
            "- ADB: 'adb connect localhost:5555' then 'adb shell ...' for live system queries. " +
            "- Full Linux tools (find, grep, df, ffmpeg, etc.)."

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

        // Use chroot-distro login with -- to run command directly (mounts, runs, unmounts cleanly)
        val shellCmd = "$CHROOT_CMD login archlinux -- " +
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
