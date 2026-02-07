package com.rightguard.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rightguard.app.ui.home.HomeScreen
import com.rightguard.app.ui.incident.IncidentDetailScreen
import com.rightguard.app.ui.incident.IncidentListScreen
import com.rightguard.app.ui.incident.IncidentSummaryScreen
import com.rightguard.app.ui.onboarding.LegalStatusScreen
import com.rightguard.app.ui.onboarding.OnboardingWelcomeScreen
import com.rightguard.app.ui.onboarding.PermissionsSetupScreen
import com.rightguard.app.ui.onboarding.PinSetupScreen
import com.rightguard.app.ui.onboarding.ProfileSetupScreen
import com.rightguard.app.ui.onboarding.TrustedContactsScreen
import com.rightguard.app.ui.panic.PanicLockScreen
import com.rightguard.app.ui.safeguard.SafeguardScreen
import com.rightguard.app.ui.settings.SettingsScreen

@Composable
fun RightGuardNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding
        composable(Screen.OnboardingWelcome.route) {
            OnboardingWelcomeScreen(
                onNext = { navController.navigate(Screen.ProfileSetup.route) }
            )
        }
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                onNext = { navController.navigate(Screen.LegalStatus.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LegalStatus.route) {
            LegalStatusScreen(
                onNext = { navController.navigate(Screen.TrustedContacts.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TrustedContacts.route) {
            TrustedContactsScreen(
                onNext = { navController.navigate(Screen.PinSetup.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PinSetup.route) {
            PinSetupScreen(
                onNext = { navController.navigate(Screen.PermissionsSetup.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PermissionsSetup.route) {
            PermissionsSetupScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Main
        composable(Screen.Home.route) {
            HomeScreen(
                onActivateSafeguard = { navController.navigate(Screen.Safeguard.route) },
                onActivatePanic = { navController.navigate(Screen.PanicLock.route) },
                onOpenIncidents = { navController.navigate(Screen.IncidentList.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Safeguard.route) {
            SafeguardScreen(
                onEndEncounter = { incidentId ->
                    navController.navigate(Screen.IncidentSummary.createRoute(incidentId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onPanic = { navController.navigate(Screen.PanicLock.route) }
            )
        }
        composable(Screen.PanicLock.route) {
            PanicLockScreen(
                onUnlocked = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Incidents
        composable(Screen.IncidentList.route) {
            IncidentListScreen(
                onIncidentClick = { id ->
                    navController.navigate(Screen.IncidentDetail.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.IncidentDetail.route,
            arguments = listOf(navArgument("incidentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val incidentId = backStackEntry.arguments?.getLong("incidentId") ?: return@composable
            IncidentDetailScreen(
                incidentId = incidentId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.IncidentSummary.route,
            arguments = listOf(navArgument("incidentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val incidentId = backStackEntry.arguments?.getLong("incidentId") ?: return@composable
            IncidentSummaryScreen(
                incidentId = incidentId,
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
