package com.videohub.pro

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.videohub.pro.ui.components.SmartShareOverlay
import com.videohub.pro.ui.navigation.Screen
import com.videohub.pro.ui.player.MediaPlayerScreen
import com.videohub.pro.ui.screens.discover.DiscoverScreen
import com.videohub.pro.ui.screens.downloads.DownloadsScreen
import com.videohub.pro.ui.screens.workspace.MediaWorkspaceScreen
import com.videohub.pro.ui.screens.diagnostics.DiagnosticsScreen
import com.videohub.pro.ui.screens.home.HomeScreen
import com.videohub.pro.ui.screens.library.LibraryScreen
import com.videohub.pro.ui.screens.notifications.NotificationsScreen
import com.videohub.pro.ui.screens.plugins.PluginsScreen
import com.videohub.pro.ui.screens.search.SearchScreen
import com.videohub.pro.ui.screens.settings.SettingsScreen
import com.videohub.pro.ui.screens.stats.StatsScreen
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.ThemeProvider
import com.videohub.pro.ui.theme.DarkBgSecondary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.VideoHubTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appSettings: com.videohub.pro.data.AppSettings

    @Inject
    lateinit var clipboardMonitor: com.videohub.pro.clipboard.ClipboardMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialSharedUrl = getSharedUrl(intent)

        setContent {
            VideoHubTheme {
                ThemeProvider(appSettings = appSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBgPrimary,
                ) {
                    val navController = rememberNavController()
                    var sharedUrl by remember { mutableStateOf(initialSharedUrl) }

                    RequestNotificationPermission()

                    // Clipboard Monitor — يكتشف الروابط المنسوخة تلقائياً
                    val clipboardUrl by clipboardMonitor.detectedUrl.collectAsState()

                    LaunchedEffect(Unit) {
                        clipboardMonitor.start()
                    }
                    DisposableEffect(Unit) {
                        onDispose { clipboardMonitor.stop() }
                    }

                    // When a URL is detected from clipboard, show it as shared URL
                    LaunchedEffect(clipboardUrl) {
                        if (clipboardUrl != null && sharedUrl == null) {
                            sharedUrl = clipboardUrl
                            clipboardMonitor.consumeUrl()
                        }
                    }

                    LaunchedEffect(intent) {
                        val newUrl = getSharedUrl(intent)
                        if (newUrl != null) {
                            sharedUrl = newUrl
                        }
                    }

                    // Media Player state — declared BEFORE Scaffold so NavHost can access it
                    var playerFile by remember { mutableStateOf<Pair<String, String>?>(null) }
                    var playerIsVideo by remember { mutableStateOf(true) }

                    Scaffold(
                        topBar = {
                            VideoHubTopBar(
                                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        },
                        bottomBar = {
                            VideoHubBottomBar(
                                navController = navController,
                                modifier = Modifier.navigationBarsPadding(),
                            )
                        },
                        containerColor = DarkBgPrimary,
                        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
                            exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onShareUrl = { url -> sharedUrl = url },
                                )
                            }
                            composable(Screen.Downloads.route) {
                                DownloadsScreen(
                                    onPlayFile = { filePath, title, isVideo ->
                                        playerFile = filePath to title
                                        playerIsVideo = isVideo
                                    },
                                    onOpenWorkspace = { task ->
                                        navController.navigate(Screen.Workspace.workspaceRoute(task.id))
                                    },
                                )
                            }
                            composable(Screen.Library.route) {
                                LibraryScreen(
                                    onPlayFile = { filePath, title, isVideo ->
                                        playerFile = filePath to title
                                        playerIsVideo = isVideo
                                    },
                                    onDownloadUrl = { url -> sharedUrl = url },
                                )
                            }
                            composable(Screen.Discover.route) {
                                DiscoverScreen(onDownloadUrl = { url -> sharedUrl = url })
                            }
                            composable(Screen.Search.route) {
                                SearchScreen(onDownloadUrl = { url -> sharedUrl = url })
                            }
                            composable(Screen.Plugins.route) { PluginsScreen() }
                            composable(Screen.Stats.route) { StatsScreen() }
                            composable(Screen.Notifications.route) { NotificationsScreen() }
                            composable(Screen.Settings.route) { SettingsScreen() }
                            composable(Screen.Diagnostics.route) { DiagnosticsScreen() }
                            composable(
                                route = Screen.Workspace.route,
                                arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                            ) { backStackEntry ->
                                val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
                                WorkspaceHolder(
                                    taskId = taskId,
                                    onPlay = { filePath, title, isVideo ->
                                        playerFile = filePath to title
                                        playerIsVideo = isVideo
                                    },
                                    onClose = { navController.popBackStack() },
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }

                    // Show media player overlay if set
                    if (playerFile != null) {
                        PlayerOverlay(
                            filePath = playerFile!!.first,
                            title = playerFile!!.second,
                            isVideo = playerIsVideo,
                            onClose = { playerFile = null },
                        )
                    }

                    val currentUrl = sharedUrl
                    if (currentUrl != null) {
                        SmartShareOverlay(
                            url = currentUrl,
                            onDismiss = { sharedUrl = null },
                        )
                    }
                }
                }
            }
        }
    }

    @Composable
    private fun RequestNotificationPermission() {
        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { _ -> }

            LaunchedEffect(Unit) {
                val granted = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Request media/storage permissions for saving downloaded files
        val mediaLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { _ -> }

        LaunchedEffect(Unit) {
            val permissionsToRequest = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                )
                else -> arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            }

            val needsRequest = permissionsToRequest.any { perm ->
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    perm,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (needsRequest) {
                mediaLauncher.launch(permissionsToRequest)
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Composable
    private fun PlayerOverlay(
        filePath: String,
        title: String,
        isVideo: Boolean,
        onClose: () -> Unit,
    ) {
        MediaPlayerScreen(
            filePath = filePath,
            title = title,
            isVideo = isVideo,
            onClose = onClose,
        )
    }

    private fun getSharedUrl(intent: Intent?): String? {
        if (intent == null) return null

        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                val urlRegex = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
                val match = urlRegex.find(text)
                if (match != null) {
                    return match.value
                }
            }
        }

        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data?.toString()
            if (data != null && (data.startsWith("http://") || data.startsWith("https://"))) {
                return data
            }
        }

        return null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

/**
 * WorkspaceHolder — loads a TaskEntity by ID and displays the MediaWorkspace.
 */
@Composable
private fun WorkspaceHolder(
    taskId: String,
    onPlay: (String, String, Boolean) -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit = onClose,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = com.videohub.pro.data.database.VideoHubDatabase.getInstance(context)
    val task = remember(taskId) {
        kotlinx.coroutines.runBlocking {
            database.taskDao().getById(taskId)
        }
    }

    if (task != null) {
        MediaWorkspaceScreen(
            task = task,
            onPlay = onPlay,
            onShare = { filePath ->
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        java.io.File(filePath),
                    ))
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة"))
            },
            onClose = onClose,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("المهمة غير موجودة", color = DarkTextSecondary)
        }
    }
}

@Composable
private fun VideoHubTopBar(
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkBgSecondary,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Logo + App Name
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        "VideoHub Pro",
                        color = DarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Enterprise v3.2",
                        color = DarkTextSecondary,
                        fontSize = 9.sp,
                    )
                }
            }

            // Right-side action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onNotificationsClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "الإشعارات",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoHubBottomBar(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    data class TabItem(val screen: Screen, val label: String, val icon: ImageVector)

    // Only 5 primary tabs in bottom nav — rest accessible via Top Bar
    val tabs = listOf(
        TabItem(Screen.Home, "الرئيسية", Icons.Default.Home),
        TabItem(Screen.Downloads, "التنزيلات", Icons.Default.Download),
        TabItem(Screen.Library, "الوسائط", Icons.Default.LibraryMusic),
        TabItem(Screen.Discover, "اكتشف", Icons.Default.Explore),
        TabItem(Screen.Search, "بحث", Icons.Default.Search),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkBgSecondary,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.screen.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            navController.navigate(tab.screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) AmberPrimary.copy(alpha = 0.15f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) AmberPrimary else DarkTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        tab.label,
                        color = if (selected) AmberPrimary else DarkTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
