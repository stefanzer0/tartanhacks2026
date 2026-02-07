package com.rightguard.app.ui.navigation

sealed class Screen(val route: String) {
    // Onboarding
    data object OnboardingWelcome : Screen("onboarding_welcome")
    data object ProfileSetup : Screen("profile_setup")
    data object LegalStatus : Screen("legal_status")
    data object TrustedContacts : Screen("trusted_contacts")
    data object PinSetup : Screen("pin_setup")
    data object PermissionsSetup : Screen("permissions_setup")

    // Main
    data object Home : Screen("home")
    data object Safeguard : Screen("safeguard")
    data object PanicLock : Screen("panic_lock")

    // Incidents
    data object IncidentList : Screen("incident_list")
    data object IncidentDetail : Screen("incident_detail/{incidentId}") {
        fun createRoute(incidentId: Long) = "incident_detail/$incidentId"
    }
    data object IncidentSummary : Screen("incident_summary/{incidentId}") {
        fun createRoute(incidentId: Long) = "incident_summary/$incidentId"
    }

    // Settings
    data object Settings : Screen("settings")
}
