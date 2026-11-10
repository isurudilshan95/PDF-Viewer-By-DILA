package com.example.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfFileUtil {

  data class PdfMetaData(
    val name: String,
    val sizeFormatted: String,
    val sizeBytes: Long
  )

  fun getMetaData(context: Context, uri: Uri): PdfMetaData {
    var name = "Document.pdf"
    var sizeBytes: Long = 0

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIndex != -1) {
            val displayName = cursor.getString(nameIndex)
            if (!displayName.isNull_or_empty()) {
              name = displayName
            }
          }
          if (sizeIndex != -1) {
            sizeBytes = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    if (name == "Document.pdf" || name.isBlank()) {
      uri.lastPathSegment?.let { path ->
        val cleanName = path.substringAfterLast('/')
        if (cleanName.isNotBlank()) {
          name = cleanName
        }
      }
    }

    val sizeFormatted = formatFileSize(sizeBytes)
    return PdfMetaData(name = name, sizeFormatted = sizeFormatted, sizeBytes = sizeBytes)
  }

  fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
      gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
      mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
      kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
      else -> "$bytes B"
    }
  }

  fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
      diff < 60_000 -> "Just now"
      diff < 3600_000 -> "${diff / 60_000}m ago"
      diff < 86400_000 -> "${diff / 3600_000}h ago"
      diff < 172800_000 -> "Yesterday"
      else -> {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
      }
    }
  }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
