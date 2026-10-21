package com.skyora.weather.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skyora.weather.core.util.decodeLocationFromRoute
import com.skyora.weather.core.util.encodeForRoute
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.Location
import com.skyora.weather.ui.city.CityWeatherScreen
import com.skyora.weather.ui.designsystem.SkyoraBottomNavigation
import com.skyora.weather.ui.home.HomeScreen
import com.skyora.weather.ui.onboarding.OnboardingScreen
import com.skyora.weather.ui.permission.LocationPermissionScreen
import com.skyora.weather.ui.saved.SavedCitiesScreen
import com.skyora.weather.ui.search.SearchMode
import com.skyora.weather.ui.search.SearchScreen
import com.skyora.weather.ui.settings.SettingsScreen
import com.skyora.weather.ui.splash.SplashScreen
import com.skyora.weather.ui.splash.StartDestination
import com.skyora.weather.ui.widgets.WidgetReliabilityScreen
import com.skyora.weather.ui.theme.LocalBottomBarInset
import com.skyora.weather.ui.theme.SkyoraColors

object SkyoraRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SEARCH_ADD = "search_add"
    const val SEARCH_PICK = "search_pick"
    const val SEARCH_PICK_SETUP = "search_pick_setup"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
    const val WIDGET_RELIABILITY = "widget_reliability"
    const val CITY = "city/{payload}"

    fun city(location: Location): String = "city/${location.encodeForRoute()}"
}

/**
 * Opens [target] on top of Home for a tap that started outside the app (widget,
 * notification).
 *
 * A cold start still sits on Splash, so Home has to replace it; a warm start
 * already has Home somewhere in the stack, and pushing a second one would make
 * the back button return to the same screen. Popping back to the existing Home
 * covers both cases with one path.
 */
private fun NavHostController.openFromOutside(target: String) {
    val homeOnStack = currentBackStack.value.any {
        it.destination.route == SkyoraRoutes.HOME
    }
    if (homeOnStack) {
        popBackStack(SkyoraRoutes.HOME, inclusive = false)
    } else {
        navigate(SkyoraRoutes.HOME) {
            popUpTo(SkyoraRoutes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }
    navigate(target) { launchSingleTop = true }
}

/**
 * Single NavHost for the whole app. The bottom bar is a measured overlay rather
 * than a Scaffold slot, so the procedural sky truly runs edge to edge behind it;
 * screens read [LocalBottomBarInset] to keep their last row discoverable.
 */
@Composable
fun SkyoraApp(
    container: AppContainer,
    requestedLocation: Location? = null,
    onRequestedLocationHandled: () -> Unit = {},
    requestedRoute: String? = null,
    onRequestedRouteHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == SkyoraRoutes.HOME ||
        currentRoute == SkyoraRoutes.SEARCH ||
        currentRoute == SkyoraRoutes.SAVED

    // Widget / notification tap: open that city's forecast once, then clear the
    // request so recreation does not re-navigate.
    LaunchedEffect(requestedLocation, currentRoute) {
        val location = requestedLocation ?: return@LaunchedEffect
        // A cold start still shows Splash, whose own effect will navigate Home
        // when it is done; going now would put Splash's Home on top of the
        // forecast we just opened. Wait for it to finish first.
        if (currentRoute == null || currentRoute == SkyoraRoutes.SPLASH) return@LaunchedEffect
        navController.openFromOutside(SkyoraRoutes.city(location))
        onRequestedLocationHandled()
    }

    // Same contract as the city request above, but for a screen that is not
    // tied to a location (the widget's "why is this stale?" tap).
    LaunchedEffect(requestedRoute, currentRoute) {
        val route = requestedRoute ?: return@LaunchedEffect
        if (currentRoute == null || currentRoute == SkyoraRoutes.SPLASH) return@LaunchedEffect
        navController.openFromOutside(route)
        onRequestedRouteHandled()
    }

    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { bottomBarHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Ink),
    ) {
        CompositionLocalProvider(
            LocalBottomBarInset provides if (showBottomBar) bottomBarHeight else 0.dp,
        ) {
            NavHost(
                navController = navController,
                startDestination = SkyoraRoutes.SPLASH,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(SkyoraRoutes.SPLASH) {
                    SplashScreen(container = container) { destination ->
                        when (destination) {
                            StartDestination.ONBOARDING -> navController.navigate(SkyoraRoutes.ONBOARDING) {
                                launchSingleTop = true
                            }

                            StartDestination.MAIN -> navController.navigateHome()
                        }
                    }
                }

                composable(SkyoraRoutes.ONBOARDING) {
                    OnboardingScreen(
                        container = container,
                        onComplete = {
                            navController.navigate(SkyoraRoutes.PERMISSION) {
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(SkyoraRoutes.PERMISSION) {
                    LocationPermissionScreen(
                        container = container,
                        onFinished = { navController.navigateHome() },
                        onChooseCity = {
                            navController.navigate(SkyoraRoutes.SEARCH_PICK_SETUP) {
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(SkyoraRoutes.HOME) {
                    HomeScreen(
                        container = container,
                        onOpenSearch = { navController.switchTab(SkyoraRoutes.SEARCH) },
                        onOpenSettings = { navController.navigate(SkyoraRoutes.SETTINGS) },
                        onChooseCity = {
                            navController.navigate(SkyoraRoutes.SEARCH_PICK) {
                                launchSingleTop = true
                            }
                        },
                        onOpenWidgetReliability = {
                            navController.navigate(SkyoraRoutes.WIDGET_RELIABILITY) {
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(SkyoraRoutes.SEARCH) {
                    SearchScreen(
                        container = container,
                        mode = SearchMode.BROWSE,
                        onBack = null,
                        onOpenCity = { location ->
                            navController.navigate(SkyoraRoutes.city(location))
                        },
                        onDone = {},
                    )
                }

                composable(SkyoraRoutes.SEARCH_ADD) {
                    SearchScreen(
                        container = container,
                        mode = SearchMode.ADD_CITY,
                        onBack = { navController.popBackStack() },
                        onOpenCity = {},
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(SkyoraRoutes.SEARCH_PICK) {
                    SearchScreen(
                        container = container,
                        mode = SearchMode.PICK_HOME,
                        onBack = { navController.popBackStack() },
                        onOpenCity = {},
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(SkyoraRoutes.SEARCH_PICK_SETUP) {
                    SearchScreen(
                        container = container,
                        mode = SearchMode.PICK_HOME,
                        onBack = { navController.popBackStack() },
                        onOpenCity = {},
                        onDone = { navController.navigateHome() },
                    )
                }

                composable(SkyoraRoutes.SAVED) {
                    SavedCitiesScreen(
                        container = container,
                        onOpenCity = { location ->
                            navController.navigate(SkyoraRoutes.city(location))
                        },
                        onAddCity = { navController.navigate(SkyoraRoutes.SEARCH_ADD) },
                    )
                }

                composable(SkyoraRoutes.SETTINGS) {
                    SettingsScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                        onChangeHomeCity = {
                            navController.navigate(SkyoraRoutes.SEARCH_PICK) {
                                launchSingleTop = true
                            }
                        },
                        onOpenWidgetReliability = {
                            navController.navigate(SkyoraRoutes.WIDGET_RELIABILITY) {
                                launchSingleTop = true
                            }
                        },
                        onRestartOnboarding = {
                            navController.navigate(SkyoraRoutes.SPLASH) {
                                popUpTo(SkyoraRoutes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(SkyoraRoutes.WIDGET_RELIABILITY) {
                    WidgetReliabilityScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = SkyoraRoutes.CITY,
                    arguments = listOf(navArgument("payload") { type = NavType.StringType }),
                ) { entry ->
                    val location = entry.arguments
                        ?.getString("payload")
                        ?.let(::decodeLocationFromRoute)
                    CityWeatherScreen(
                        container = container,
                        location = location,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        if (showBottomBar) {
            SkyoraBottomNavigation(
                currentRoute = when (currentRoute) {
                    SkyoraRoutes.SEARCH -> SkyoraRoutes.SEARCH
                    else -> currentRoute
                },
                onSelect = { route -> navController.switchTab(route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomBarHeightPx = it.height },
            )
        }
    }
}

/** Clears the onboarding/splash stack and lands on Home with an empty back stack. */
private fun NavHostController.navigateHome() {
    navigate(SkyoraRoutes.HOME) {
        popUpTo(SkyoraRoutes.SPLASH) { inclusive = true }
        launchSingleTop = true
    }
}

/** Standard bottom-tab behaviour: preserve each tab's scroll/state. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(SkyoraRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
