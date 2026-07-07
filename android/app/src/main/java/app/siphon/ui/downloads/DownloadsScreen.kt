package app.siphon.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.siphon.R
import app.siphon.data.db.DownloadEntity
import app.siphon.ui.components.DownloadRow
import app.siphon.ui.components.EmptyState
import app.siphon.util.Intents

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val query by viewModel.query.collectAsState()

    var pendingDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var deleteFileToo by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text(
                stringResource(R.string.downloads_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { sortMenuOpen = true }) {
                Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.sort_newest))
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                SortItem(R.string.sort_newest, sort == DownloadSort.NEWEST) {
                    viewModel.setSort(DownloadSort.NEWEST); sortMenuOpen = false
                }
                SortItem(R.string.sort_oldest, sort == DownloadSort.OLDEST) {
                    viewModel.setSort(DownloadSort.OLDEST); sortMenuOpen = false
                }
                SortItem(R.string.sort_title, sort == DownloadSort.TITLE) {
                    viewModel.setSort(DownloadSort.TITLE); sortMenuOpen = false
                }
                SortItem(R.string.sort_size, sort == DownloadSort.SIZE) {
                    viewModel.setSort(DownloadSort.SIZE); sortMenuOpen = false
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text(stringResource(R.string.downloads_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            FilterChipFor(R.string.filter_all, filter == DownloadFilter.ALL) { viewModel.setFilter(DownloadFilter.ALL) }
            FilterChipFor(R.string.filter_active, filter == DownloadFilter.ACTIVE) { viewModel.setFilter(DownloadFilter.ACTIVE) }
            FilterChipFor(R.string.filter_queued, filter == DownloadFilter.QUEUED) { viewModel.setFilter(DownloadFilter.QUEUED) }
            FilterChipFor(R.string.filter_paused, filter == DownloadFilter.PAUSED) { viewModel.setFilter(DownloadFilter.PAUSED) }
            FilterChipFor(R.string.filter_completed, filter == DownloadFilter.COMPLETED) { viewModel.setFilter(DownloadFilter.COMPLETED) }
            FilterChipFor(R.string.filter_failed, filter == DownloadFilter.FAILED) { viewModel.setFilter(DownloadFilter.FAILED) }
        }

        if (downloads.isEmpty()) {
            EmptyState(stringResource(R.string.downloads_empty))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(downloads, key = { it.id }) { entity ->
                    DownloadRow(
                        entity = entity,
                        onPause = { viewModel.pause(entity.id) },
                        onResume = { viewModel.resume(entity.id) },
                        onRetry = { viewModel.retry(entity.id) },
                        onCancel = { viewModel.cancel(entity.id) },
                        onDelete = { pendingDelete = entity; deleteFileToo = false },
                        onOpenFile = { Intents.openFile(context, entity) },
                        onOpenFolder = { Intents.openFolder(context, entity) },
                    )
                }
            }
        }
    }

    pendingDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_dialog_message))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deleteFileToo, onCheckedChange = { deleteFileToo = it })
                        Text(stringResource(R.string.delete_dialog_also_file))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(entity.id, deleteFileToo)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SortItem(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                stringResource(labelRes),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun FilterChipFor(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(labelRes)) },
    )
}
