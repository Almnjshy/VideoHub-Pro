package com.videohub.pro.ui.navigation

enum class Screen(val route: String) {
    Home("home"),
    Downloads("downloads"),
    Library("library"),
    Discover("discover"),
    Search("search"),
    Plugins("plugins"),
    Stats("stats"),
    Notifications("notifications"),
    Settings("settings"),
    Diagnostics("diagnostics"),
    Workspace("workspace/{taskId}"),
    ;

    fun workspaceRoute(taskId: String) = "workspace/$taskId"
}

val BOTTOM_NAV_SCREENS = listOf(
    Screen.Home,
    Screen.Downloads,
    Screen.Library,
    Screen.Discover,
    Screen.Search,
)
