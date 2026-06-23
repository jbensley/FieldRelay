package us.bensley.fieldrelay.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import us.bensley.fieldrelay.data.Settings as AppSettings
import us.bensley.fieldrelay.di.ServiceLocator
import us.bensley.fieldrelay.ui.screens.HomeScreen
import us.bensley.fieldrelay.ui.screens.ReportScreen
import us.bensley.fieldrelay.ui.screens.SettingsScreen
import us.bensley.fieldrelay.weather.WeatherProviderRegistry

@Composable
fun FieldRelayNavHost(
    showDurationDialogRequest: Boolean,
    confirmStopDialogRequest: Boolean,
    requestPermissions: Boolean,
    openReportRequest: Boolean,
    onDurationDialogConsumed: () -> Unit,
    onConfirmStopDialogConsumed: () -> Unit,
    onPermissionsConsumed: () -> Unit,
    onOpenReportConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val settings by ServiceLocator.settings.settings.collectAsState(initial = AppSettings.DEFAULT)
    val weatherReportingAvailable = WeatherProviderRegistry.hasConfiguredProvider(settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = buildList {
        add(NavItem.HomeItem)
        if (weatherReportingAvailable) add(NavItem.ReportItem)
        add(NavItem.SettingsItem)
    }

    LaunchedEffect(openReportRequest, weatherReportingAvailable) {
        if (openReportRequest) {
            onOpenReportConsumed()
            navController.navigateTo(if (weatherReportingAvailable) Report else Home)
        }
    }

    LaunchedEffect(currentDestination, weatherReportingAvailable) {
        if (!weatherReportingAvailable && currentDestination?.hasRoute<Report>() == true) {
            navController.navigateTo(Home)
        }
    }

    BoxWithConstraints {
        if (maxWidth >= 720.dp) {
            Row {
                NavigationRail {
                    items.forEach { item ->
                        NavigationRailItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = item.isSelected(currentDestination),
                            onClick = { navController.navigateTo(item.route) },
                        )
                    }
                }
                AppNavHost(
                    modifier = Modifier.weight(1f),
                    showDurationDialogRequest = showDurationDialogRequest,
                    confirmStopDialogRequest = confirmStopDialogRequest,
                    requestPermissions = requestPermissions,
                    onDurationDialogConsumed = onDurationDialogConsumed,
                    onConfirmStopDialogConsumed = onConfirmStopDialogConsumed,
                    onPermissionsConsumed = onPermissionsConsumed,
                    navController = navController,
                    weatherReportingAvailable = weatherReportingAvailable,
                )
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        items.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = item.isSelected(currentDestination),
                                onClick = { navController.navigateTo(item.route) },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                AppNavHost(
                    modifier = Modifier.padding(innerPadding),
                    showDurationDialogRequest = showDurationDialogRequest,
                    confirmStopDialogRequest = confirmStopDialogRequest,
                    requestPermissions = requestPermissions,
                    onDurationDialogConsumed = onDurationDialogConsumed,
                    onConfirmStopDialogConsumed = onConfirmStopDialogConsumed,
                    onPermissionsConsumed = onPermissionsConsumed,
                    navController = navController,
                    weatherReportingAvailable = weatherReportingAvailable,
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(
    modifier: Modifier,
    showDurationDialogRequest: Boolean,
    confirmStopDialogRequest: Boolean,
    requestPermissions: Boolean,
    onDurationDialogConsumed: () -> Unit,
    onConfirmStopDialogConsumed: () -> Unit,
    onPermissionsConsumed: () -> Unit,
    navController: NavHostController,
    weatherReportingAvailable: Boolean,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {
        composable<Home> {
            HomeScreen(
                showDurationDialogRequest = showDurationDialogRequest,
                confirmStopDialogRequest = confirmStopDialogRequest,
                requestPermissions = requestPermissions,
                onDurationDialogConsumed = onDurationDialogConsumed,
                onConfirmStopDialogConsumed = onConfirmStopDialogConsumed,
                onPermissionsConsumed = onPermissionsConsumed,
                onOpenSettings = { navController.navigateTo(Settings) },
            )
        }
        composable<Settings> { SettingsScreen() }
        composable<Report> { ReportScreen(weatherReportingAvailable = weatherReportingAvailable) }
    }
}

private fun NavHostController.navigateTo(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private sealed class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any,
) {
    data object HomeItem : NavItem("Home", Icons.Default.Home, Home)
    data object ReportItem : NavItem("Weather", Icons.Default.Report, Report)
    data object SettingsItem : NavItem("Settings", Icons.Default.Settings, Settings)

    fun isSelected(destination: NavDestination?): Boolean = when (this) {
        HomeItem -> destination?.hasRoute<Home>() == true
        ReportItem -> destination?.hasRoute<Report>() == true
        SettingsItem -> destination?.hasRoute<Settings>() == true
    }
}
