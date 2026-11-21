package com.lnxv.player.ui.files

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.archive.ArchiveEngine
import com.lnxv.player.core.design.*
import com.lnxv.player.core.recycle.RecycleBinManager
import com.lnxv.player.core.storage.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilesScreen(
    initialPath: String?,
    onNavigateToPreview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileManager = remember { FileManager(context) }
    val archiveEngine = remember { ArchiveEngine() }
    val recycleBinManager = remember { RecycleBinManager(context) }

    val defaultPath = remember {
        val root = Environment.getExternalStorageDirectory()
        if (initialPath.isNullOrEmpty() || !File(initialPath).exists()) root.absolutePath else initialPath
    }

    var currentPath by remember { mutableStateOf(defaultPath) }
    var fileItems by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selectedFiles by remember { mutableStateOf<Set<FileItem>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(SortMode.NAME_ASC) }
    var isGridView by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(true) }
    var newFileName by remember { mutableStateOf("") }

    // Advanced Filter state
    var selectedCategoryFilter by remember { mutableStateOf<FileCategory?>(null) }
    var isRecycleBinView by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            fileItems = fileManager.listFiles(currentPath, showHidden = showHidden, sortMode = sortMode)
        }
    }

    LaunchedEffect(currentPath, showHidden, sortMode) {
        refresh()
    }

    val filteredItems = fileItems.filter { item ->
        val matchesSearch = searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
        matchesSearch && matchesCategory
    }

    AmbientGlassBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            // Header: Title & Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRecycleBinView) "Recycle Bin" else "File Explorer",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                    Text(
                        text = "${filteredItems.size} items",
                        fontSize = 12.sp,
                        color = AppColors.textMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // SELECT ALL BUTTON (Always accessible)
                    GlassIconButton(
                        icon = if (isSelectionMode && selectedFiles.size == filteredItems.size && filteredItems.isNotEmpty()) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = "Select All",
                        onClick = {
                            if (isSelectionMode && selectedFiles.size == filteredItems.size) {
                                selectedFiles = emptySet()
                                isSelectionMode = false
                            } else {
                                isSelectionMode = true
                                selectedFiles = filteredItems.toSet()
                            }
                        }
                    )

                    // Toggle List/Grid View
                    GlassIconButton(
                        icon = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        onClick = { isGridView = !isGridView }
                    )

                    // New Folder
                    GlassIconButton(
                        icon = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        onClick = {
                            isCreatingFolder = true
                            newFileName = ""
                            showCreateDialog = true
                        }
                    )

                    // Hidden Files toggle
                    GlassIconButton(
                        icon = if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Hidden files",
                        onClick = { showHidden = !showHidden }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Universal Search & Filter Bar
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search files (*.apk, *.pdf, *.mp4)...", color = AppColors.textMuted, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AppColors.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Filters (All, APK, Videos, Images, Audio, Docs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf(
                    "All" to null,
                    "APK" to FileCategory.APKS,
                    "Videos" to FileCategory.VIDEOS,
                    "Images" to FileCategory.IMAGES,
                    "Audio" to FileCategory.AUDIO,
                    "Zip" to FileCategory.ARCHIVES
                )
                filters.forEach { (label, category) ->
                    val isSelected = selectedCategoryFilter == category
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = if (isSelected) Color(0xFFE0F2FE) else GlassWhiteHigh,
                        borderColor = if (isSelected) PrimarySkyBlue else GlassWhiteBorderCrisp,
                        onClick = { selectedCategoryFilter = if (isSelected) null else category }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimarySkyBlue else AppColors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Breadcrumb navigation pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentFile = File(currentPath)
                if (currentFile.parentFile != null && currentFile.parentFile?.canRead() == true) {
                    GlassIconButton(
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        onClick = {
                            currentFile.parentFile?.let { currentPath = it.absolutePath }
                        },
                        size = 36.dp
                    )
                }
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = currentPath,
                        fontSize = 11.sp,
                        color = AppColors.textMuted,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-selection bar with Select All & Operations
            if (isSelectionMode) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFFE0F2FE),
                    borderColor = PrimarySkyBlue
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedFiles.size} / ${filteredItems.size} selected",
                            color = PrimarySkyBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Select All / Deselect All inside selection bar
                            TextButton(onClick = {
                                if (selectedFiles.size == filteredItems.size) {
                                    selectedFiles = emptySet()
                                } else {
                                    selectedFiles = filteredItems.toSet()
                                }
                            }) {
                                Text(
                                    if (selectedFiles.size == filteredItems.size) "Deselect" else "Select All",
                                    color = PrimarySkyBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Create ZIP Archive from selected files
                            IconButton(onClick = {
                                scope.launch {
                                    val zipOut = File(currentPath, "Archive_${System.currentTimeMillis()}.zip")
                                    val res = archiveEngine.createZip(selectedFiles.map { it.file }, zipOut)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Created ${zipOut.name}", Toast.LENGTH_SHORT).show()
                                        selectedFiles = emptySet()
                                        isSelectionMode = false
                                        refresh()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.FolderZip, "Create ZIP", tint = RadiantLavender)
                            }

                            // Send to Recycle Bin
                            IconButton(onClick = {
                                scope.launch {
                                    selectedFiles.forEach { recycleBinManager.moveToTrash(it.file) }
                                    Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                    selectedFiles = emptySet()
                                    isSelectionMode = false
                                    refresh()
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Recycle", tint = CrimsonDanger)
                            }

                            // Cancel selection
                            IconButton(onClick = {
                                selectedFiles = emptySet()
                                isSelectionMode = false
                            }) {
                                Icon(Icons.Default.Close, "Cancel", tint = AppColors.textPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Files Grid or List
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.path }) { item ->
                        val isSelected = selectedFiles.contains(item)
                        FileGridItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedFiles = if (isSelected) selectedFiles - item else selectedFiles + item
                                } else {
                                    if (item.isDirectory) currentPath = item.path
                                    else onNavigateToPreview(item.path)
                                }
                            },
                            onLongClick = {
                                isSelectionMode = true
                                selectedFiles = selectedFiles + item
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.path }) { item ->
                        val isSelected = selectedFiles.contains(item)
                        FileListItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedFiles = if (isSelected) selectedFiles - item else selectedFiles + item
                                } else {
                                    if (item.isDirectory) currentPath = item.path
                                    else onNavigateToPreview(item.path)
                                }
                            },
                            onLongClick = {
                                isSelectionMode = true
                                selectedFiles = selectedFiles + item
                            }
                        )
                    }
                }
            }
        }

        // Dialog for creating folders/files
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(if (isCreatingFolder) "New Folder" else "New File", fontWeight = FontWeight.Bold, color = AppColors.textPrimary) },
                text = {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimarySkyBlue,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySkyBlue),
                        onClick = {
                            if (newFileName.isNotBlank()) {
                                scope.launch {
                                    if (isCreatingFolder) {
                                        fileManager.createFolder(currentPath, newFileName)
                                    } else {
                                        fileManager.createFile(currentPath, newFileName)
                                    }
                                    showCreateDialog = false
                                    refresh()
                                }
                            }
                        }
                    ) {
                        Text("Create", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = AppColors.textPrimary)
                    }
                }
            )
        }
    }
}

@Composable
private fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateStr = remember(item.lastModified) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.lastModified))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (isSelected) Color(0xFFE0F2FE) else GlassWhiteHigh,
        borderColor = if (isSelected) PrimarySkyBlue else GlassWhiteBorderCrisp,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                FileIconBox(item = item)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = if (item.isDirectory) dateStr else "${remember(item.size) { item.formattedSize() }} • $dateStr",
                        fontSize = 11.sp,
                        color = AppColors.textMuted
                    )
                }
            }
            if (item.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SlateSubtleText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (isSelected) Color(0xFFE0F2FE) else GlassWhiteHigh,
        borderColor = if (isSelected) PrimarySkyBlue else GlassWhiteBorderCrisp,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FileIconBox(item = item, size = 48.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary,
                maxLines = 1
            )
            Text(
                text = if (item.isDirectory) "Folder" else remember(item.size) { item.formattedSize() },
                fontSize = 10.sp,
                color = AppColors.textMuted
            )
        }
    }
}

@Composable
private fun FileIconBox(item: FileItem, size: androidx.compose.ui.unit.Dp = 38.dp) {
    val (icon, tint, bg) = when {
        item.isDirectory -> Triple(Icons.Default.Folder, PrimarySkyBlue, Color(0xFFE0F2FE))
        item.category == FileCategory.IMAGES -> Triple(Icons.Default.Image, Color(0xFF0284C7), Color(0xFFE0F2FE))
        item.category == FileCategory.VIDEOS -> Triple(Icons.Default.VideoLibrary, RadiantLavender, Color(0xFFF3E8FF))
        item.category == FileCategory.AUDIO -> Triple(Icons.Default.Audiotrack, EmeraldSuccess, Color(0xFFD1FAE5))
        item.category == FileCategory.APKS -> Triple(Icons.Default.Android, Color(0xFF14B8A6), Color(0xFFCCFBF1))
        item.category == FileCategory.ARCHIVES -> Triple(Icons.Default.FolderZip, Color(0xFF6366F1), Color(0xFFE0E7FF))
        else -> Triple(Icons.Default.InsertDriveFile, AppColors.textMuted, Color(0xFFF1F5F9))
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}
