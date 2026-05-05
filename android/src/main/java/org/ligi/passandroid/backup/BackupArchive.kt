package org.ligi.passandroid.backup

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupArchive {

    private const val MANIFEST_ENTRY = "manifest.json"
    private const val PREFERENCES_ENTRY = "preferences.json"
    private const val CLASSIFIER_STATE = "classifier_state.json"
    private const val MAX_BACKUP_TOTAL_BYTES = 100L * 1024L * 1024L
    private const val MAX_BACKUP_ENTRY_BYTES = 25L * 1024L * 1024L
    private const val MAX_BACKUP_ENTRIES = 1_024
    private const val MAX_PREFERENCES_BYTES = 256L * 1024L

    fun exportBackup(
        passesDir: File,
        stateDir: File,
        preferences: Map<String, Any?>,
        appVersionName: String,
        appVersionCode: Int,
        outputStream: OutputStream,
    ) {
        ZipOutputStream(outputStream.buffered()).use { zip ->
            zip.writeTextEntry(
                MANIFEST_ENTRY,
                buildManifest(appVersionName, appVersionCode),
            )
            if (passesDir.exists()) {
                passesDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        zip.writeFileEntry(file, "passes/${file.relativeTo(passesDir).invariantSeparatorsPath}")
                    }
            }

            val classifierState = File(stateDir, CLASSIFIER_STATE)
            if (classifierState.exists()) {
                zip.writeFileEntry(classifierState, "state/$CLASSIFIER_STATE")
            }

            zip.writeTextEntry(PREFERENCES_ENTRY, encodePreferences(preferences))
        }
    }

    fun importBackup(
        inputStream: InputStream,
        passesDir: File,
        stateDir: File,
        restorePreferences: (Map<String, Any>) -> Unit,
    ) {
        val preferences = mutableMapOf<String, Any>()

        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entryCount = 0
            var totalBytes = 0L
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_BACKUP_ENTRIES) {
                    throw IllegalArgumentException("Backup contains too many files")
                }
                if (!entry.isDirectory) {
                    when {
                        entry.name.startsWith("passes/") -> {
                            val relative = entry.name.removePrefix("passes/")
                            totalBytes += zip.copyEntryToSafeFile(passesDir, relative, MAX_BACKUP_ENTRY_BYTES)
                        }
                        entry.name == "state/$CLASSIFIER_STATE" -> {
                            totalBytes += zip.copyEntryToSafeFile(stateDir, CLASSIFIER_STATE, MAX_BACKUP_ENTRY_BYTES)
                        }
                        entry.name == PREFERENCES_ENTRY -> {
                            val preferencesJson = zip.readEntryTextWithLimit(MAX_PREFERENCES_BYTES)
                            totalBytes += preferencesJson.toByteArray(Charsets.UTF_8).size
                            preferences.putAll(decodePreferences(preferencesJson))
                        }
                    }
                    if (totalBytes > MAX_BACKUP_TOTAL_BYTES) {
                        throw IllegalArgumentException("Backup is too large")
                    }
                }
                zip.closeEntry()
            }
        }

        if (preferences.isNotEmpty()) {
            restorePreferences(preferences)
        }
    }

    private fun buildManifest(appVersionName: String, appVersionCode: Int): String {
        return """
            {
              "format":1,
              "app":"PassWallet",
              "package":"org.baumweg.passwallet",
              "appVersionName":"${escapeJson(appVersionName)}",
              "appVersionCode":$appVersionCode,
              "createdAt":"${escapeJson(nowIsoUtc())}"
            }
        """.trimIndent()
    }

    private fun nowIsoUtc(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    private fun encodePreferences(preferences: Map<String, Any?>): String {
        val entries = preferences.entries
            .filter { it.value is String || it.value is Boolean || it.value is Int || it.value is Long || it.value is Float }
            .sortedBy { it.key }
            .joinToString(",\n") { (key, value) ->
                val encodedValue = when (value) {
                    is String -> "\"${escapeJson(value)}\""
                    is Boolean, is Int, is Long, is Float -> value.toString()
                    else -> "null"
                }
                "  \"${escapeJson(key)}\":$encodedValue"
            }
        return "{\n$entries\n}"
    }

    private fun decodePreferences(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val regex = Regex("\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*:\\s*(\\\"(?:\\\\.|[^\\\"])*\\\"|true|false|-?\\d+(?:\\.\\d+)?)")
        regex.findAll(json).forEach { match ->
            val key = unescapeJson(match.groupValues[1])
            val rawValue = match.groupValues[2]
            val value: Any = when {
                rawValue == "true" -> true
                rawValue == "false" -> false
                rawValue.startsWith("\"") -> unescapeJson(rawValue.removeSurrounding("\""))
                rawValue.contains('.') -> rawValue.toFloat()
                else -> rawValue.toIntOrNull() ?: rawValue
            }
            result[key] = value
        }
        return result
    }

    private fun ZipOutputStream.writeTextEntry(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.writeFileEntry(file: File, name: String) {
        putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    private fun ZipInputStream.copyEntryToSafeFile(baseDir: File, relativeName: String, byteLimit: Long): Long {
        require(relativeName.isNotBlank()) { "Unsafe empty backup entry" }
        val target = File(baseDir, relativeName)
        val canonicalBase = baseDir.canonicalFile
        val canonicalTarget = target.canonicalFile
        if (!canonicalTarget.path.startsWith(canonicalBase.path + File.separator)) {
            throw IllegalArgumentException("Unsafe backup entry: $relativeName")
        }
        canonicalTarget.parentFile?.mkdirs()
        canonicalTarget.outputStream().use { output ->
            return copyEntryToWithLimit(output, byteLimit)
        }
    }

    private fun ZipInputStream.readEntryTextWithLimit(byteLimit: Long): String {
        val bytes = java.io.ByteArrayOutputStream()
        copyEntryToWithLimit(bytes, byteLimit)
        return bytes.toString(Charsets.UTF_8.name())
    }

    private fun ZipInputStream.copyEntryToWithLimit(output: OutputStream, byteLimit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            copied += read
            if (copied > byteLimit) {
                throw IllegalArgumentException("Backup entry is too large")
            }
            output.write(buffer, 0, read)
        }
        return copied
    }

    private fun escapeJson(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    private fun unescapeJson(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                append(
                    when (val escaped = value[index + 1]) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> escaped
                    }
                )
                index += 2
            } else {
                append(char)
                index++
            }
        }
    }
}
