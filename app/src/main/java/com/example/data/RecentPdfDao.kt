package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentPdfDao {
  @Query("SELECT * FROM recent_pdfs ORDER BY lastOpenedTimestamp DESC")
  fun getAllRecentPdfs(): Flow<List<RecentPdfEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(pdf: RecentPdfEntity)

  @Delete
  suspend fun delete(pdf: RecentPdfEntity)

  @Query("DELETE FROM recent_pdfs WHERE uriString = :uriString")
  suspend fun deleteByUri(uriString: String)

  @Query("DELETE FROM recent_pdfs")
  suspend fun deleteAll()
}
