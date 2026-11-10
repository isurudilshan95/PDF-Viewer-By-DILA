package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pdf.PdfRenderEngine
import com.example.ui.theme.DarkReaderBg
import com.example.ui.theme.DarkReaderSurface
import com.example.ui.theme.DarkReaderText
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
  uriString: String,
  fileName: String,
  initialPage: Int,
  onBackClick: () -> Unit,
  onPageChanged: (Int) -> Unit
) {
  BackHandler {
    onBackClick()
  }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uri = remember(uriString) { Uri.parse(uriString) }

  var pdfEngine by remember { mutableStateOf<PdfRenderEngine?>(null) }
  var totalPages by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }
  var loadError by remember { mutableStateOf<String?>(null) }

  // Dark Reading Mode state
  var isDarkReadingMode by remember { mutableStateOf(false) }

  // Jump to Page Dialog state
  var showJumpDialog by remember { mutableStateOf(false) }
  var jumpInputText by remember { mutableStateOf("") }

  // Initialize PDF Engine
  DisposableEffect(uriString) {
    val engine = PdfRenderEngine(context, uri)
    pdfEngine = engine

    scope.launch {
      isLoading = true
      loadError = null
      val success = engine.init()
      if (success) {
        totalPages = engine.pageCount
        isLoading = false
      } else {
        loadError = "Failed to load PDF document. File may be encrypted or corrupted."
        isLoading = false
      }
    }

    onDispose {
      engine.close()
    }
  }

  val initialValidPage = if (totalPages > 0) initialPage.coerceIn(0, totalPages - 1) else 0
  val pagerState = rememberPagerState(
    initialPage = initialValidPage,
    pageCount = { totalPages.coerceAtLeast(1) }
  )

  // Track page change to report back to ViewModel
  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }.collect { currentPage ->
      if (totalPages > 0) {
        onPageChanged(currentPage)
      }
    }
  }

  val backgroundColor = if (isDarkReadingMode) DarkReaderBg else Color(0xFFF1F5F9)
  val toolbarColor = if (isDarkReadingMode) DarkReaderSurface else Color.White
  val contentTextColor = if (isDarkReadingMode) DarkReaderText else Color(0xFF0F172A)

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = backgroundColor,
    topBar = {
      TopAppBar(
        modifier = Modifier.testTag("viewer_toolbar"),
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = toolbarColor,
          titleContentColor = contentTextColor,
          navigationIconContentColor = contentTextColor,
          actionIconContentColor = contentTextColor
        ),
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("viewer_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to list"
            )
          }
        },
        title = {
          Column {
            Text(
              text = fileName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (totalPages > 0) {
              Text(
                text = "Page ${pagerState.currentPage + 1} of $totalPages",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkReadingMode) TextMuted else TextSecondary
              )
            }
          }
        },
        actions = {
          // Dark Reading Mode toggle
          IconButton(
            onClick = { isDarkReadingMode = !isDarkReadingMode },
            modifier = Modifier.testTag("dark_mode_toggle")
          ) {
            Icon(
              imageVector = if (isDarkReadingMode) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = "Toggle Dark Reading Mode",
              tint = if (isDarkReadingMode) Color(0xFFFFD54F) else TextSecondary
            )
          }

          // Search / Jump to page icon
          IconButton(
            onClick = {
              jumpInputText = (pagerState.currentPage + 1).toString()
              showJumpDialog = true
            },
            modifier = Modifier.testTag("search_page_button")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search / Jump to Page",
              tint = if (isDarkReadingMode) DarkReaderText else TextSecondary
            )
          }
        }
      )
    },
    bottomBar = {
      if (totalPages > 1 && !isLoading && loadError == null) {
        Surface(
          color = toolbarColor,
          shadowElevation = 8.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("viewer_bottom_bar")
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              IconButton(
                onClick = {
                  if (pagerState.currentPage > 0) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                  }
                },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                  contentDescription = "Previous Page",
                  tint = if (pagerState.currentPage > 0) contentTextColor else TextMuted
                )
              }

              // Page count indicator text (tappable to jump)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    jumpInputText = (pagerState.currentPage + 1).toString()
                    showJumpDialog = true
                  }
                  .background(if (isDarkReadingMode) Color(0xFF2C2C2C) else Color(0xFFE2E8F0))
                  .padding(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "${pagerState.currentPage + 1} / $totalPages",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = contentTextColor
                )
              }

              IconButton(
                onClick = {
                  if (pagerState.currentPage < totalPages - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                  }
                },
                enabled = pagerState.currentPage < totalPages - 1,
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                  contentDescription = "Next Page",
                  tint = if (pagerState.currentPage < totalPages - 1) contentTextColor else TextMuted
                )
              }
            }

            // Bottom Slider for rapid scrolling
            Slider(
              value = pagerState.currentPage.toFloat(),
              onValueChange = { value ->
                val targetPage = value.roundToInt().coerceIn(0, totalPages - 1)
                scope.launch { pagerState.scrollToPage(targetPage) }
              },
              valueRange = 0f..(totalPages - 1).toFloat(),
              steps = if (totalPages > 2) totalPages - 2 else 0,
              colors = SliderDefaults.colors(
                thumbColor = RedPrimary,
                activeTrackColor = RedPrimary,
                inactiveTrackColor = if (isDarkReadingMode) Color(0xFF333333) else Color(0xFFCBD5E1)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .testTag("page_slider")
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.Center
    ) {
      when {
        isLoading -> {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(color = RedPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Opening PDF document...",
              style = MaterialTheme.typography.bodyMedium,
              color = contentTextColor
            )
          }
        }

        loadError != null -> {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
          ) {
            Text(
              text = loadError ?: "Error loading document",
              style = MaterialTheme.typography.bodyLarge,
              color = RedPrimary,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBackClick) {
              Text("Go Back", color = RedPrimary, fontWeight = FontWeight.Bold)
            }
          }
        }

        totalPages > 0 && pdfEngine != null -> {
          HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
          ) { pageIndex ->
            PdfPageView(
              pageIndex = pageIndex,
              engine = pdfEngine!!,
              isDarkMode = isDarkReadingMode
            )
          }
        }
      }
    }
  }

  // Jump to Page Dialog
  if (showJumpDialog) {
    AlertDialog(
      onDismissRequest = { showJumpDialog = false },
      title = {
        Text(
          text = "Go to Page",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column {
          Text(
            text = "Enter a page number between 1 and $totalPages:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = jumpInputText,
            onValueChange = { jumpInputText = it.filter { char -> char.isDigit() } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Number,
              imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
              onGo = {
                val pageNum = jumpInputText.toIntOrNull()
                if (pageNum != null && pageNum in 1..totalPages) {
                  scope.launch { pagerState.scrollToPage(pageNum - 1) }
                  showJumpDialog = false
                }
              }
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("jump_page_input")
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val pageNum = jumpInputText.toIntOrNull()
            if (pageNum != null && pageNum in 1..totalPages) {
              scope.launch { pagerState.scrollToPage(pageNum - 1) }
              showJumpDialog = false
            }
          }
        ) {
          Text("Go", color = RedPrimary, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showJumpDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
fun PdfPageView(
  pageIndex: Int,
  engine: PdfRenderEngine,
  isDarkMode: Boolean
) {
  var bitmap by remember(pageIndex, isDarkMode) { mutableStateOf<Bitmap?>(null) }
  var isRendering by remember(pageIndex, isDarkMode) { mutableStateOf(true) }

  // Zoom & Pan gesture states
  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 4f)
    if (scale > 1f) {
      offsetX += offsetChange.x
      offsetY += offsetChange.y
    } else {
      offsetX = 0f
      offsetY = 0f
    }
  }

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize()
  ) {
    val targetWidthPx = constraints.maxWidth

    LaunchedEffect(pageIndex, isDarkMode, targetWidthPx) {
      isRendering = true
      val rendered = engine.renderPage(pageIndex, targetWidthPx, isDarkMode)
      bitmap = rendered
      isRendering = false
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .transformable(state = transformableState),
      contentAlignment = Alignment.Center
    ) {
      if (isRendering) {
        CircularProgressIndicator(
          color = RedPrimary,
          modifier = Modifier.size(36.dp)
        )
      } else {
        bitmap?.let { bmt ->
          Image(
            bitmap = bmt.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
              )
              .testTag("pdf_page_image_$pageIndex")
          )
        } ?: run {
          Text(
            text = "Failed to render page",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
        }
      }

      // Quick Zoom reset indicator when zoomed in
      if (scale > 1f) {
        Surface(
          onClick = {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
          },
          shape = CircleShape,
          color = RedPrimary,
          contentColor = Color.White,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.ZoomOut,
              contentDescription = "Reset Zoom",
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
