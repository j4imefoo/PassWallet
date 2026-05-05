package org.ligi.passandroid.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import okio.buffer
import okio.source
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.functions.createPassForImageImport
import org.ligi.passandroid.functions.createPassForPDFImport
import org.ligi.passandroid.functions.readJSONSafely
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object UnzipPassController : KoinComponent {

    private const val MAX_IMPORTED_STREAM_BYTES = 100L * 1024L * 1024L
    private const val MAX_ZIP_TOTAL_BYTES = 100L * 1024L * 1024L
    private const val MAX_ZIP_ENTRY_BYTES = 25L * 1024L * 1024L
    private const val MAX_ZIP_ENTRIES = 512
    private const val MAX_PKPASS_BUNDLE_ENTRIES = 100
    private val SAFE_PASS_ID = Regex("[A-Za-z0-9](?:[A-Za-z0-9._-]{0,126}[A-Za-z0-9])?")

    val tracker: Tracker by inject()
    val settings: Settings by inject()

    interface SuccessCallback {
        fun call(uuids: List<String>)
    }

    interface FailCallback {
        fun fail(reason: String)
    }

    fun processInputStream(spec: InputStreamUnzipControllerSpec) {
        val tempFile = File.createTempFile("ins", "pass", spec.context.cacheDir)
        try {
            spec.inputStreamWithSource.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyToWithLimit(output, MAX_IMPORTED_STREAM_BYTES)
                }
                val importedIds = processFile(FileUnzipControllerSpec(tempFile.absolutePath, spec))

                if (importedIds.isNotEmpty()) {
                    spec.onSuccessCallback?.call(importedIds)
                }
            }
        } catch (e: ImportValidationException) {
            tracker.trackException("unsafe import rejected", e, false)
            spec.failCallback?.fail(e.message ?: "Unsafe import rejected")
        } catch (e: Exception) {
            tracker.trackException("problem processing InputStream", e, false)
            spec.failCallback?.fail("problem with temp file: $e")
        } finally {
            tempFile.delete()
        }
    }

    private fun processFile(spec: FileUnzipControllerSpec): List<String> {
        val sourceFile = File(spec.zipFileString)
        if (isPkpassesBundle(sourceFile)) {
            return processPkpassesBundle(sourceFile, spec)
        }

        var uuid = UUID.randomUUID().toString()
        val path = File(spec.context.cacheDir, "temp/$uuid")

        path.mkdirs()

        if (!path.exists()) {
            spec.failCallback?.fail("Problem creating the temp dir: $path")
            return emptyList()
        }

        File(path, "source.obj").bufferedWriter().use { it.write(spec.source) }

        try {
            val extractedZip = try {
                extractZipSafely(sourceFile, path)
            } catch (e: ImportValidationException) {
            spec.failCallback?.fail(e.message ?: "Unsafe archive")
            return emptyList()
        }

        val manifestFile = File(path, "manifest.json")
        val espassFile = File(path, "main.json")
        val manifestJSON: JSONObject

        when {
            manifestFile.exists() -> try {
                val readToString = manifestFile.bufferedReader().readText()
                manifestJSON = readJSONSafely(readToString)!!
                uuid = manifestJSON.getString("pass.json")
            } catch (e: Exception) {
                spec.failCallback?.fail("Problem with manifest.json: $e")
                return emptyList()
            }
            espassFile.exists() -> try {
                val readToString = espassFile.bufferedReader().readText()
                manifestJSON = readJSONSafely(readToString)!!
                uuid = manifestJSON.getString("id")
            } catch (e: Exception) {
                spec.failCallback?.fail("Problem with manifest.json: $e")
                return emptyList()
            }
            !extractedZip -> {
                val bitmap = BitmapFactory.decodeFile(spec.zipFileString)
                val resources = spec.context.resources

                if (bitmap != null) {
                    val imagePass = createPassForImageImport(resources)
                    val pathForID = spec.passStore.getPathForID(imagePass.id)
                    pathForID.mkdirs()

                    File(spec.zipFileString).copyTo(File(pathForID, "strip.png"))

                    spec.passStore.save(imagePass)
                    spec.passStore.classifier.moveToTopic(imagePass, TopicNames.NEW)
                    return listOf(imagePass.id)
                }

                try {
                    val file = File(spec.zipFileString)
                    val readUtf8 = file.source().buffer().readUtf8(4)
                    if (readUtf8 == "%PDF") {
                        val open = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val pdfRenderer = PdfRenderer(open)

                        val page = pdfRenderer.openPage(0)
                        val ratio = page.height.toFloat() / page.width

                        val widthPixels = resources.displayMetrics.widthPixels
                        val pdfBitmap = createBitmap(widthPixels, (widthPixels * ratio).toInt())
                        page.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val imagePass = createPassForPDFImport(resources)
                        val pathForID = spec.passStore.getPathForID(imagePass.id)
                        pathForID.mkdirs()

                        pdfBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(File(pathForID, "strip.png")))

                        spec.passStore.save(imagePass)
                        spec.passStore.classifier.moveToTopic(imagePass, TopicNames.NEW)
                        return listOf(imagePass.id)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "could not inspect PDF import")
                }

                spec.failCallback?.fail("Pass is not espass or pkpass format :-(")
                return emptyList()
            }
            else -> {
                spec.failCallback?.fail("Pass archive does not contain a readable pass manifest")
                return emptyList()
            }
        }

        if (!isSafePassId(uuid)) {
            spec.failCallback?.fail("Pass archive contains an unsafe pass id")
            return emptyList()
        }

        spec.targetPath.mkdirs()
        val renamedFile = File(spec.targetPath, uuid)
        if (!isInsideDirectory(spec.targetPath, renamedFile)) {
            spec.failCallback?.fail("Pass archive target path is unsafe")
            return emptyList()
        }

        if (spec.overwrite && renamedFile.exists()) {
            renamedFile.deleteRecursively()
        }

        if (!renamedFile.exists()) {
            if (!path.renameTo(renamedFile)) {
                path.deleteRecursively()
                spec.failCallback?.fail("Problem moving imported pass into storage")
                return emptyList()
            }
        } else {
            Timber.i("Pass with same ID exists")
            path.deleteRecursively()
        }

            return listOf(uuid)
        } finally {
            if (path.exists()) {
                path.deleteRecursively()
            }
        }
    }

    private fun isPkpassesBundle(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                var count = 0
                var hasPkpass = false
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory) {
                        entry.safeRelativeName()
                        hasPkpass = true
                        count++
                        if (count > MAX_PKPASS_BUNDLE_ENTRIES) {
                            throw ImportValidationException("pkpasses bundle contains too many passes")
                        }
                        if (!entry.name.endsWith(".pkpass", ignoreCase = true)) {
                            return false
                        }
                    }
                }
                hasPkpass
            }
        } catch (_: ZipException) {
            false
        }
    }

    private fun processPkpassesBundle(file: File, spec: FileUnzipControllerSpec): List<String> {
        val importedIds = mutableListOf<String>()
        try {
            ZipFile(file).use { zip ->
                val pkpassEntries = zip.safeEntries(MAX_PKPASS_BUNDLE_ENTRIES)
                    .filterNot { it.isDirectory }
                    .filter { it.name.endsWith(".pkpass", ignoreCase = true) }

                var copiedBytes = 0L
                pkpassEntries.forEach { entry ->
                    entry.safeRelativeName()
                    if (entry.size > MAX_ZIP_ENTRY_BYTES) {
                        throw ImportValidationException("pkpasses bundle contains an oversized pass")
                    }
                    val tempPass = File.createTempFile("bundle-", ".pkpass", spec.context.cacheDir)
                    try {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(tempPass).use { output ->
                                copiedBytes += input.copyToWithLimit(output, MAX_ZIP_ENTRY_BYTES)
                            }
                        }
                        if (copiedBytes > MAX_ZIP_TOTAL_BYTES) {
                            throw ImportValidationException("pkpasses bundle is too large")
                        }
                        importedIds += processFile(FileUnzipControllerSpec(tempPass.absolutePath, spec))
                    } finally {
                        tempPass.delete()
                    }
                }
            }
        } catch (e: ImportValidationException) {
            tracker.trackException("unsafe pkpasses bundle rejected", e, false)
            spec.failCallback?.fail(e.message ?: "Unsafe pkpasses bundle")
            return emptyList()
        } catch (e: Exception) {
            tracker.trackException("problem processing pkpasses bundle", e, false)
            spec.failCallback?.fail("Problem with pkpasses bundle: $e")
            return emptyList()
        }

        if (importedIds.isEmpty()) {
            spec.failCallback?.fail("pkpasses bundle does not contain valid pkpass files")
        }

        return importedIds
    }

    class InputStreamUnzipControllerSpec(internal val inputStreamWithSource: InputStreamWithSource, context: Context, passStore: PassStore,
                                         onSuccessCallback: SuccessCallback?, failCallback: FailCallback?) : UnzipControllerSpec(context, passStore, onSuccessCallback, failCallback, settings)

    private fun extractZipSafely(sourceFile: File, destination: File): Boolean {
        return try {
            ZipFile(sourceFile).use { zip ->
                val entries = zip.safeEntries(MAX_ZIP_ENTRIES)

                var totalBytes = 0L
                entries.forEach { entry ->
                    val relativeName = entry.safeRelativeName()
                    val target = File(destination, relativeName)
                    if (!isInsideDirectory(destination, target)) {
                        throw ImportValidationException("Archive contains an unsafe path")
                    }

                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        if (entry.size > MAX_ZIP_ENTRY_BYTES) {
                            throw ImportValidationException("Archive contains an oversized file")
                        }
                        target.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(target).use { output ->
                                totalBytes += input.copyToWithLimit(output, MAX_ZIP_ENTRY_BYTES)
                            }
                        }
                        if (totalBytes > MAX_ZIP_TOTAL_BYTES) {
                            throw ImportValidationException("Archive is too large")
                        }
                    }
                }
            }
            true
        } catch (_: ZipException) {
            false
        }
    }

    private fun ZipEntry.safeRelativeName(): String {
        val normalized = name.replace('\\', '/')
        val segments = normalized.split('/')
        if (normalized.isBlank() || normalized.startsWith('/') || segments.any { it == "." || it == ".." }) {
            throw ImportValidationException("Archive contains an unsafe path")
        }
        return normalized
    }

    private fun isSafePassId(id: String): Boolean = SAFE_PASS_ID.matches(id)

    private fun isInsideDirectory(baseDir: File, target: File): Boolean {
        val canonicalBase = baseDir.canonicalFile
        val canonicalTarget = target.canonicalFile
        return canonicalTarget.path.startsWith(canonicalBase.path + File.separator)
    }

    private fun ZipFile.safeEntries(maxEntries: Int): List<ZipEntry> {
        val result = mutableListOf<ZipEntry>()
        val entries = entries()
        while (entries.hasMoreElements()) {
            if (result.size >= maxEntries) {
                throw ImportValidationException("Archive contains too many files")
            }
            result += entries.nextElement()
        }
        return result
    }

    private fun InputStream.copyToWithLimit(output: OutputStream, maxBytes: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) {
                return totalBytes
            }
            totalBytes += read
            if (totalBytes > maxBytes) {
                throw ImportValidationException("Imported file is too large")
            }
            output.write(buffer, 0, read)
        }
    }

    private class ImportValidationException(message: String) : Exception(message)
}
