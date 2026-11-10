package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RecentPdfEntity
import com.example.pdf.PdfFileUtil
import com.example.pdf.PdfRenderEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
  object Home : Screen
  data class Viewer(
    val uriString: String,
    val fileName: String,
    val initialPage: Int = 0
  ) : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val dao = AppDatabase.getDatabase(application).recentPdfDao()

  val currentScreen = MutableStateFlow<Screen>(Screen.Home)
  val searchQuery = MutableStateFlow("")

  val recentPdfs: StateFlow<List<RecentPdfEntity>> = combine(
    dao.getAllRecentPdfs(),
    searchQuery
  ) { list, query ->
    if (query.isBlank()) {
      list
    } else {
      list.filter { it.fileName.contains(query, ignoreCase = true) }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun onSearchQueryChanged(query: String) {
    searchQuery.value = query
  }

  fun openPdfFromUri(uri: Uri) {
    val context = getApplication<Application>().applicationContext

    // Persist read URI permission
    try {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }

    viewModelScope.launch(Dispatchers.IO) {
      val meta = PdfFileUtil.getMetaData(context, uri)
      var totalPages = 0

      // Render engine quick check for page count
      try {
        PdfRenderEngine(context, uri).use { engine ->
          if (engine.init()) {
            totalPages = engine.pageCount
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      val existingEntity = try {
        // Query if exists to preserve last read page if re-opening
        recentPdfs.value.find { it.uriString == uri.toString() }
      } catch (e: Exception) {
        null
      }

      val lastPage = existingEntity?.lastPageRead ?: 0

      val entity = RecentPdfEntity(
        uriString = uri.toString(),
        fileName = meta.name,
        fileSizeFormatted = meta.sizeFormatted,
        lastOpenedTimestamp = System.currentTimeMillis(),
        pageCount = totalPages,
        lastPageRead = lastPage
      )

      dao.insertOrUpdate(entity)

      withContext(Dispatchers.Main) {
        currentScreen.value = Screen.Viewer(
          uriString = uri.toString(),
          fileName = meta.name,
          initialPage = lastPage
        )
      }
    }
  }

  fun updateLastReadPage(uriString: String, pageIndex: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      recentPdfs.value.find { it.uriString == uriString }?.let { existing ->
        val updated = existing.copy(
          lastOpenedTimestamp = System.currentTimeMillis(),
          lastPageRead = pageIndex
        )
        dao.insertOrUpdate(updated)
      }
    }
  }

  fun removeRecentPdf(entity: RecentPdfEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      dao.delete(entity)
    }
  }

  fun clearAllRecents() {
    viewModelScope.launch(Dispatchers.IO) {
      dao.deleteAll()
    }
  }

  fun navigateToHome() {
    currentScreen.value = Screen.Home
  }
}
