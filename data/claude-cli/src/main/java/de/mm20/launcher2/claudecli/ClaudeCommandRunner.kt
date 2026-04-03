package de.mm20.launcher2.claudecli

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ClaudeCommandRunner {

    companion object {
        private const val CHROOT_CMD = "/data/adb/modules/chroot-distro/system/bin/chroot-distro"
        private const val USER_HOME = "/home/maplestar"

        private const val SYSTEM_PROMPT = "You are a search assistant for an Android phone. " +
            "The Android filesystem is mounted at /android-root/. Key paths: " +
            "photos at /android-root/sdcard/DCIM/Camera/, " +
            "downloads at /android-root/sdcard/Download/, " +
            "apps at /android-root/data/app/, " +
            "system info at /android-root/system/build.prop. " +
            "Be fast and direct. Return a concise answer."
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun execute(query: String, model: String = "haiku"): String? = withContext(Dispatchers.IO) {
        val escapedQuery = query.replace("'", "'\\''")
        val escapedPrompt = SYSTEM_PROMPT.replace("'", "'\\''")

        val command = arrayOf(
            "su", "-c",
            "$CHROOT_CMD login archlinux --bind /:/android-root -- " +
            "env PATH=$USER_HOME/.local/bin:$USER_HOME/.local/share/mise/shims:\$PATH HOME=$USER_HOME " +
            "$USER_HOME/.local/bin/claude -p '$escapedQuery' " +
            "--model $model " +
            "--output-format json " +
            "--permission-mode dontAsk " +
            "--allowedTools 'Bash,Read,Glob,Grep' " +
            "--system-prompt '$escapedPrompt'"
        )

        try {
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0 || output.isBlank()) return@withContext null

            val jsonObj = json.parseToJsonElement(output).jsonObject
            val isError = jsonObj["is_error"]?.jsonPrimitive?.content?.toBoolean() ?: false
            if (isError) return@withContext null

            jsonObj["result"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}
