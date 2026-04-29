package org.ligi.passandroid.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.exception.ZipException
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
import java.util.UUID
import java.util.zip.ZipFile

object UnzipPassController : KoinComponent {

    val tracker: Tracker by inject()
    val settings: Settings by inject()

    interface SuccessCallback {
        fun call(uuids: List<String>)
    }

    interface FailCallback {
        fun fail(reason: String)
    }

    fun processInputStream(spec: InputStreamUnzipControllerSpec) {
        try {
            spec.inputStreamWithSource.inputStream.use {
                val tempFile = File.createTempFile("ins", "pass")
                it.copyTo(FileOutputStream(tempFile))
                val importedIds = processFile(FileUnzipControllerSpec(tempFile.absolutePath, spec))
                tempFile.delete()

                if (importedIds.isNotEmpty()) {
                    spec.onSuccessCallback?.call(importedIds)
                }
            }
        } catch (e: Exception) {
            tracker.trackException("problem processing InputStream", e, false)
            spec.failCallback?.fail("problem with temp file: $e")
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

        File(path, "source.obj").bufferedWriter().write(spec.source)

        try {
            val zipFile = Zip4jFile(spec.zipFileString)
            zipFile.extractAll(path.absolutePath)
        } catch (e: ZipException) {
            e.printStackTrace()
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
            else -> {
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
                        val createBitmap = Bitmap.createBitmap(widthPixels, (widthPixels * ratio).toInt(), Bitmap.Config.ARGB_8888)
                        page.render(createBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val imagePass = createPassForPDFImport(resources)
                        val pathForID = spec.passStore.getPathForID(imagePass.id)
                        pathForID.mkdirs()

                        createBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(File(pathForID, "strip.png")))

                        spec.passStore.save(imagePass)
                        spec.passStore.classifier.moveToTopic(imagePass, TopicNames.NEW)
                        return listOf(imagePass.id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                spec.failCallback?.fail("Pass is not espass or pkpass format :-(")
                return emptyList()
            }
        }

        spec.targetPath.mkdirs()
        val renamedFile = File(spec.targetPath, uuid)

        if (spec.overwrite && renamedFile.exists()) {
            renamedFile.deleteRecursively()
        }

        if (!renamedFile.exists()) {
            path.renameTo(renamedFile)
        } else {
            Timber.i("Pass with same ID exists")
        }

        return listOf(uuid)
    }

    private fun isPkpassesBundle(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                val entries = zip.entries().asSequence().filterNot { it.isDirectory }.toList()
                entries.isNotEmpty() && entries.all { it.name.endsWith(".pkpass", ignoreCase = true) }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun processPkpassesBundle(file: File, spec: FileUnzipControllerSpec): List<String> {
        val importedIds = mutableListOf<String>()
        try {
            ZipFile(file).use { zip ->
                zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { it.name.endsWith(".pkpass", ignoreCase = true) }
                    .forEach { entry ->
                        val tempPass = File.createTempFile("bundle-", ".pkpass", spec.context.cacheDir)
                        try {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(tempPass).use { output -> input.copyTo(output) }
                            }
                            importedIds += processFile(FileUnzipControllerSpec(tempPass.absolutePath, spec))
                        } finally {
                            tempPass.delete()
                        }
                    }
            }
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
}
