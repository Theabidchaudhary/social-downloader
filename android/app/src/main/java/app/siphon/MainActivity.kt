package app.siphon

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.siphon.data.settings.SiphonSettings
import app.siphon.ui.downloads.DownloadsScreen
import app.siphon.ui.downloads.DownloadsViewModel
import app.siphon.ui.home.HomeScreen
import app.siphon.ui.home.HomeViewModel
import app.siphon.ui.settings.SettingsScreen
import app.siphon.ui.settings.SettingsViewModel
import app.siphon.ui.theme.SiphonTheme

class MainActivity : AppCompatActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val container = (application as SiphonApp).container
        val initialTab = intent?.getIntExtra(EXTRA_OPEN_TAB, TAB_HOME) ?: TAB_HOME

        setContent {
            val settings by container.settings.settings.collectAsState(initial = SiphonSettings())
            SiphonTheme(themeMode = settings.theme) {
                MainScaffold(initialTab = initialTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_HOME = 0
        const val TAB_DOWNLOADS = 1
    }
}

/**
 * Two bottom tabs (Home, Downloads) per the product spec; Settings is a
 * full-screen destination reached from the top bar's gear, dismissed with back.
 */
@Composable
private fun MainScaffold(initialTab: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as SiphonApp).container

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val downloadsViewModel: DownloadsViewModel = viewModel(factory = DownloadsViewModel.factory(container))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))

    BackHandler(enabled = settingsOpen) { settingsOpen = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    stringResource(R.string.app_name).lowercase(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                IconButton(onClick = { settingsOpen = true }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        bottomBar = {
            if (!settingsOpen) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = selectedTab == MainActivity.TAB_HOME,
                        onClick = { selectedTab = MainActivity.TAB_HOME },
                        icon = {
                            Icon(
                                if (selectedTab == MainActivity.TAB_HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_home)) },
                        colors = navItemColors(),
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainActivity.TAB_DOWNLOADS,
                        onClick = { selectedTab = MainActivity.TAB_DOWNLOADS },
                        icon = {
                            Icon(
                                if (selectedTab == MainActivity.TAB_DOWNLOADS) Icons.Filled.Download else Icons.Outlined.Download,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_downloads)) },
                        colors = navItemColors(),
                    )
                }
            }
        },
    ) { padding ->
        when {
            settingsOpen -> SettingsScreen(
                viewModel = settingsViewModel,
                modifier = Modifier.padding(padding),
            )
            selectedTab == MainActivity.TAB_DOWNLOADS -> DownloadsScreen(
                viewModel = downloadsViewModel,
                modifier = Modifier.padding(padding),
            )
            else -> HomeScreen(
                viewModel = homeViewModel,
                onOpenDownloads = { selectedTab = MainActivity.TAB_DOWNLOADS },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
