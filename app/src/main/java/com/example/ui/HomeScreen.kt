package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RecentPdfEntity
import com.example.pdf.PdfFileUtil
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  recentPdfs: List<RecentPdfEntity>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onOpenPdfSelected: (Uri) -> Unit,
  onRecentPdfClick: (RecentPdfEntity) -> Unit,
  onRemoveRecentPdf: (RecentPdfEntity) -> Unit,
  onClearAllRecents: () -> Unit
) {
  val safLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { onOpenPdfSelected(it) }
  }

  var showMenu by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color.White,
    topBar = {
      TopAppBar(
        modifier = Modifier.testTag("home_top_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.White,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RedPrimary.copy(alpha = 0.1f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Fast • Offline • Distraction-Free",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
              )
            }
          }
        },
        actions = {
          if (recentPdfs.isNotEmpty()) {
            Box {
              IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.testTag("home_overflow_menu")
              ) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "Menu Options",
                  tint = MaterialTheme.colorScheme.onBackground
                )
              }
              DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
              ) {
                DropdownMenuItem(
                  text = { Text("Clear recent history") },
                  onClick = {
                    showMenu = false
                    onClearAllRecents()
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = null,
                      tint = RedPrimary
                    )
                  }
                )
              }
            }
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { safLauncher.launch(arrayOf("application/pdf")) },
        containerColor = RedPrimary,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
          .padding(bottom = 16.dp, end = 16.dp)
          .testTag("open_pdf_fab")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = "Open PDF",
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Open PDF",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("search_input"),
        placeholder = {
          Text(
            text = "Search recent PDFs...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = TextSecondary
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChanged("") }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear search",
                tint = TextSecondary
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = RedPrimary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Content List or Empty State
      if (recentPdfs.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
          ) {
            Box(
              modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(RedPrimary.copy(alpha = 0.08f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier.size(40.dp)
              )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No PDFs match your search" else "No recent PDFs yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "Try searching with a different file name." else "Tap the 'Open PDF' button below to select and view a PDF document.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
          }
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "RECENT DOCUMENTS (${recentPdfs.size})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          contentPadding = PaddingValues(bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(
            items = recentPdfs,
            key = { it.uriString }
          ) { pdf ->
            RecentPdfItem(
              pdf = pdf,
              onClick = { onRecentPdfClick(pdf) },
              onRemove = { onRemoveRecentPdf(pdf) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun RecentPdfItem(
  pdf: RecentPdfEntity,
  onClick: () -> Unit,
  onRemove: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Surface(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("recent_pdf_item_${pdf.fileName.hashCode()}"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 0.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // PDF File Badge Icon
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(RedPrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = RedPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = pdf.fileName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = pdf.fileSizeFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
          Text(
            text = " • ",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
          )
          Text(
            text = PdfFileUtil.formatTimestamp(pdf.lastOpenedTimestamp),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
          if (pdf.pageCount > 0) {
            Text(
              text = " • ",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted
            )
            Text(
              text = if (pdf.lastPageRead > 0) "Page ${pdf.lastPageRead + 1}/${pdf.pageCount}" else "${pdf.pageCount} pages",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = RedPrimary
            )
          }
        }
      }

      Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Remove from recent") },
            onClick = {
              showMenu = false
              onRemove()
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = RedPrimary
              )
            }
          )
        }
      }
    }
  }
}
