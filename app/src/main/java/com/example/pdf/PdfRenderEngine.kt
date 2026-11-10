package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfRenderEngine(
  private val context: Context,
  private val uri: Uri
) : AutoCloseable {

  private var fileDescriptor: ParcelFileDescriptor? = null
  private var pdfRenderer: PdfRenderer? = null

  var pageCount: Int = 0
    private set

  // Memory cache for rendered bitmaps (up to 32MB)
  private val cacheSize = (Runtime.getRuntime().maxMemory() / 8).toInt()
  private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
    override fun sizeOf(key: String, bitmap: Bitmap): Int {
      return bitmap.byteCount
    }
  }

  suspend fun init(): Boolean = withContext(Dispatchers.IO) {
    try {
      fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
      fileDescriptor?.let { fd ->
        pdfRenderer = PdfRenderer(fd)
        pageCount = pdfRenderer?.pageCount ?: 0
        return@withContext pageCount > 0
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return@withContext false
  }

  suspend fun renderPage(
    pageIndex: Int,
    targetWidth: Int,
    isDarkMode: Boolean
  ): Bitmap? = withContext(Dispatchers.IO) {
    if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null
    val renderer = pdfRenderer ?: return@withContext null

    val cacheKey = "${pageIndex}_${targetWidth}_$isDarkMode"
    bitmapCache.get(cacheKey)?.let { cachedBitmap ->
      if (!cachedBitmap.isRecycled) return@withContext cachedBitmap
    }

    try {
      val page = renderer.openPage(pageIndex)
      val pdfWidth = page.width
      val pdfHeight = page.height

      // Calculate width & height maintaining aspect ratio
      val scale = if (targetWidth > 0) targetWidth.toFloat() / pdfWidth.toFloat() else 1.5f
      val renderWidth = (pdfWidth * scale).toInt().coerceAtLeast(1)
      val renderHeight = (pdfHeight * scale).toInt().coerceAtLeast(1)

      val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
      
      // Render white background & PDF content
      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
      page.close()

      val finalBitmap = if (isDarkMode) {
        invertBitmapColors(bitmap)
      } else {
        bitmap
      }

      bitmapCache.put(cacheKey, finalBitmap)
      return@withContext finalBitmap
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext null
    }
  }

  private fun invertBitmapColors(source: Bitmap): Bitmap {
    val inverted = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(inverted)
    val paint = Paint()

    val invertMatrix = ColorMatrix(
      floatArrayOf(
        -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
         0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
         0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
         0.0f,  0.0f,  0.0f, 1.0f,   0.0f
      )
    )

    paint.colorFilter = ColorMatrixColorFilter(invertMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    source.recycle()
    return inverted
  }

  fun clearCache() {
    bitmapCache.evictAll()
  }

  override fun close() {
    try {
      clearCache()
      pdfRenderer?.close()
      pdfRenderer = null
      fileDescriptor?.close()
      fileDescriptor = null
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
