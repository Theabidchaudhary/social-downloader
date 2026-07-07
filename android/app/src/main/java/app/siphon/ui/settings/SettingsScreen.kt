package app.siphon.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.core.os.LocaleListCompat
import app.siphon.BuildConfig
import app.siphon.R
import app.siphon.data.settings.TargetDir
import app.siphon.data.settings.ThemeMode
import app.siphon.ui.components.SectionLabel
import app.siphon.ui.theme.SiphonColors

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // Keep write access across reboots for the chosen tree (incl. SD cards).
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setTargetDir(TargetDir.CUSTOM, uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 12.dp),
        )

        // Appearance
        SettingsGroup(stringResource(R.string.settings_appearance)) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.theme == mode,
                        onClick = { viewModel.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            },
                        )
                    }
                }
            }
        }

        // Server (API connection)
        SettingsGroup(stringResource(R.string.settings_server)) {
            var apiUrlText by remember(settings.apiBaseUrl) { mutableStateOf(settings.apiBaseUrl) }
            val connectionState by viewModel.connectionTestState.collectAsState()

            Text(stringResource(R.string.settings_api_url), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = apiUrlText,
                onValueChange = {
                    apiUrlText = it
                    viewModel.resetConnectionTestState()
                },
                placeholder = { Text(BuildConfig.SIPHON_API_BASE_URL) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.settings_api_url_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        viewModel.setApiBaseUrl(apiUrlText)
                        viewModel.resetConnectionTestState()
                    },
                    shape = MaterialTheme.shapes.small,
                ) { Text(stringResource(R.string.action_save)) }
                TextButton(
                    onClick = { viewModel.testConnection(apiUrlText.ifBlank { BuildConfig.SIPHON_API_BASE_URL }) },
                ) { Text(stringResource(R.string.action_test_connection)) }
            }
            when (connectionState) {
                ConnectionTestState.TESTING -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.connection_testing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ConnectionTestState.SUCCESS -> Text(
                    "✓ " + stringResource(R.string.connection_success),
                    style = MaterialTheme.typography.bodySmall,
                    color = SiphonColors.Success,
                )
                ConnectionTestState.FAILED -> Text(
                    stringResource(R.string.connection_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                ConnectionTestState.IDLE -> Text(
                    stringResource(R.string.settings_api_url_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Storage
        SettingsGroup(stringResource(R.string.settings_storage)) {
            Text(stringResource(R.string.settings_location), style = MaterialTheme.typography.titleSmall)
            LocationOption(
                label = stringResource(R.string.location_downloads),
                selected = settings.targetDir == TargetDir.DOWNLOADS,
            ) { viewModel.setTargetDir(TargetDir.DOWNLOADS) }
            LocationOption(
                label = stringResource(R.string.location_movies),
                selected = settings.targetDir == TargetDir.MOVIES,
            ) { viewModel.setTargetDir(TargetDir.MOVIES) }
            LocationOption(
                label = stringResource(R.string.location_music),
                selected = settings.targetDir == TargetDir.MUSIC,
            ) { viewModel.setTargetDir(TargetDir.MUSIC) }
            LocationOption(
                label = stringResource(R.string.location_custom) +
                    (settings.customTreeUri?.let { if (settings.targetDir == TargetDir.CUSTOM) " ✓" else "" } ?: ""),
                selected = settings.targetDir == TargetDir.CUSTOM,
            ) { folderPicker.launch(null) }
        }

        // Downloading
        SettingsGroup(stringResource(R.string.settings_downloading)) {
            Text(
                "${stringResource(R.string.settings_parallel)}: ${settings.maxParallelDownloads}",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = settings.maxParallelDownloads.toFloat(),
                onValueChange = { viewModel.setMaxParallel(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
            )
            ToggleRow(
                title = stringResource(R.string.settings_clipboard),
                subtitle = stringResource(R.string.settings_clipboard_sub),
                checked = settings.clipboardDetection,
                onChange = viewModel::setClipboardDetection,
            )
            ToggleRow(
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.settings_notifications_sub),
                checked = settings.notificationsEnabled,
                onChange = viewModel::setNotificationsEnabled,
            )
        }

        // Language
        SettingsGroup(stringResource(R.string.settings_language)) {
            LocationOption(
                label = stringResource(R.string.language_system),
                selected = settings.languageTag.isEmpty(),
            ) {
                viewModel.setLanguageTag("")
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
            LocationOption(
                label = "English",
                selected = settings.languageTag == "en",
            ) {
                viewModel.setLanguageTag("en")
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            }
        }

        // About
        SettingsGroup(stringResource(R.string.settings_about)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.about_version),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.about_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun LocationOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
