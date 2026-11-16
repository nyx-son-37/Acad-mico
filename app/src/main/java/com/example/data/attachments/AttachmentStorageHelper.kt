package com.example.data.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SavedLocalAttachment(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localFilePath: String,
)

data class PreparedAiAttachmentPayload(
    val images: List<Bitmap>,
    val extractedText: String,
)

object AttachmentStorageHelper {

    suspend fun copyUriToPrivateStorage(context: Context, uri: Uri): SavedLocalAttachment =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            var fileName = "anexo_${System.currentTimeMillis()}"
            var sizeBytes = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { fileName = it }
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }

            val folder = File(context.filesDir, "attachments").apply { mkdirs() }
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            val destFile = File(folder, "${UUID.randomUUID()}_$safeName")

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Não foi possível ler o arquivo selecionado.")

            if (sizeBytes <= 0L) {
                sizeBytes = destFile.length()
            }

            SavedLocalAttachment(
                fileName = fileName.take(255),
                mimeType = mimeType.take(120),
                sizeBytes = sizeBytes,
                localFilePath = destFile.absolutePath,
            )
        }

    suspend fun prepareForAi(
        localFilePath: String,
        mimeType: String,
        fileName: String,
        textNote: String
    ): PreparedAiAttachmentPayload = withContext(Dispatchers.IO) {
        val images = mutableListOf<Bitmap>()
        val textBuilder = StringBuilder()

        if (textNote.isNotBlank()) {
            textBuilder.appendLine("Nota / texto adicional do aluno:")
            textBuilder.appendLine(textNote.trim())
            textBuilder.appendLine()
        }

        val file = File(localFilePath)
        if (localFilePath.isNotBlank() && file.exists()) {
            when {
                mimeType.startsWith("image/") -> {
                    decodeScaledBitmap(file, maxDim = 1600)?.let { images.add(it) }
                }
                mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    renderPdfPages(file, maxPages = 5, targetWidth = 1240).forEach { images.add(it) }
                }
                mimeType.startsWith("text/") ||
                    mimeType.contains("json") ||
                    mimeType.contains("xml") ||
                    mimeType.contains("csv") ||
                    fileName.endsWith(".txt", true) ||
                    fileName.endsWith(".md", true) ||
                    fileName.endsWith(".csv", true) ||
                    fileName.endsWith(".json", true) ||
                    fileName.endsWith(".py", true) ||
                    fileName.endsWith(".kt", true) ||
                    fileName.endsWith(".java", true) -> {
                    val content = runCatching { file.readText(Charsets.UTF_8).take(30_000) }.getOrDefault("")
                    if (content.isNotBlank()) {
                        textBuilder.appendLine("Conteúdo extraído do arquivo ($fileName):")
                        textBuilder.appendLine(content)
                    }
                }
                else -> {
                    textBuilder.appendLine("Arquivo binário anexado: $fileName ($mimeType, ${file.length()} bytes).")
                }
            }
        }

        PreparedAiAttachmentPayload(
            images = images,
            extractedText = textBuilder.toString().trim()
        )
    }

    private fun decodeScaledBitmap(file: File, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun renderPdfPages(file: File, maxPages: Int, targetWidth: Int): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = minOf(renderer.pageCount, maxPages)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val ratio = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                            val height = (page.height * ratio).toInt().coerceIn(200, 2200)
                            val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
        }
        return bitmaps
    }
}
