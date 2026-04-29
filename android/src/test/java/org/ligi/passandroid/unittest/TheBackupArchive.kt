package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ligi.passandroid.backup.BackupArchive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class TheBackupArchive {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun exportIncludesManifestPassesStateAndPreferences() {
        val root = temporaryFolder.newFolder()
        val passesDir = File(root, "passes")
        val passDir = File(passesDir, "pass-1")
        passDir.mkdirs()
        File(passDir, "main.json").writeText("{\"id\":\"pass-1\"}")
        val stateDir = File(root, "state")
        stateDir.mkdirs()
        File(stateDir, "classifier_state.json").writeText("{\"pass-1\":\"tickets\"}")
        val output = ByteArrayOutputStream()

        BackupArchive.exportBackup(
            passesDir = passesDir,
            stateDir = stateDir,
            preferences = mapOf("sort" to "0", "autolight" to true),
            appVersionName = "26.4.29",
            appVersionCode = 202604290,
            outputStream = output,
        )

        val entries = zipEntries(output.toByteArray())
        assertThat(entries.keys).contains(
            "manifest.json",
            "passes/pass-1/main.json",
            "state/classifier_state.json",
            "preferences.json",
        )
        assertThat(entries["manifest.json"]).contains("\"app\":\"PassWallet\"")
        assertThat(entries["preferences.json"]).contains("\"sort\":\"0\"")
    }

    @Test
    fun importRestoresPassesStateAndPreferences() {
        val sourceRoot = temporaryFolder.newFolder()
        val sourcePasses = File(sourceRoot, "passes")
        val sourcePass = File(sourcePasses, "pass-1")
        sourcePass.mkdirs()
        File(sourcePass, "main.json").writeText("pass-data")
        val sourceState = File(sourceRoot, "state")
        sourceState.mkdirs()
        File(sourceState, "classifier_state.json").writeText("state-data")
        val archive = ByteArrayOutputStream()
        BackupArchive.exportBackup(
            passesDir = sourcePasses,
            stateDir = sourceState,
            preferences = mapOf("nightmode" to "auto"),
            appVersionName = "26.4.29",
            appVersionCode = 202604290,
            outputStream = archive,
        )

        val targetRoot = temporaryFolder.newFolder()
        val restoredPreferences = mutableMapOf<String, Any>()
        BackupArchive.importBackup(
            inputStream = ByteArrayInputStream(archive.toByteArray()),
            passesDir = File(targetRoot, "passes"),
            stateDir = File(targetRoot, "state"),
            restorePreferences = { restoredPreferences.putAll(it) },
        )

        assertThat(File(targetRoot, "passes/pass-1/main.json").readText()).isEqualTo("pass-data")
        assertThat(File(targetRoot, "state/classifier_state.json").readText()).isEqualTo("state-data")
        assertThat(restoredPreferences).containsEntry("nightmode", "auto")
    }

    private fun zipEntries(bytes: ByteArray): Map<String, String> {
        val result = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    result[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        return result
    }
}
