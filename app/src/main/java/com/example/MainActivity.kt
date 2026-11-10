package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.ViewerScreen
import com.example.ui.theme.PDFViewerTheme

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Handle cold-start intent for opening PDF from another app
    handleIntent(intent)

    setContent {
      PDFViewerTheme {
        MainAppContent(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_VIEW) {
      intent.data?.let { uri ->
        viewModel.openPdfFromUri(uri)
      }
    }
  }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val recentPdfs by viewModel.recentPdfs.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()

  when (val screen = currentScreen) {
    is Screen.Home -> {
      HomeScreen(
        recentPdfs = recentPdfs,
        searchQuery = searchQuery,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onOpenPdfSelected = viewModel::openPdfFromUri,
        onRecentPdfClick = { pdf ->
          viewModel.openPdfFromUri(Uri.parse(pdf.uriString))
        },
        onRemoveRecentPdf = viewModel::removeRecentPdf,
        onClearAllRecents = viewModel::clearAllRecents
      )
    }

    is Screen.Viewer -> {
      ViewerScreen(
        uriString = screen.uriString,
        fileName = screen.fileName,
        initialPage = screen.initialPage,
        onBackClick = viewModel::navigateToHome,
        onPageChanged = { pageIndex ->
          viewModel.updateLastReadPage(screen.uriString, pageIndex)
        }
      )
    }
  }
}
