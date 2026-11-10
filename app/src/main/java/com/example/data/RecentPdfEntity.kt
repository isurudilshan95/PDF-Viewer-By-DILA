package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_pdfs")
data class RecentPdfEntity(
  @PrimaryKey val uriString: String,
  val fileName: String,
  val fileSizeFormatted: String,
  val lastOpenedTimestamp: Long = System.currentTimeMillis(),
  val pageCount: Int = 0,
  val lastPageRead: Int = 0
)
