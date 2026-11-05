package com.ultron.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ultron.android.chat.ChatViewModel
import com.ultron.android.data.SettingsStore
import com.ultron.android.settings.OnboardingScreen
import com.ultron.android.settings.PermissionsScreen
import com.ultron.android.settings.SettingsHomeScreen
import com.ultron.android.settings.SettingsSection
import com.ultron.android.ui.home.HomeScreen
import com.ultron.android.ui.menu.MenuScreen
import com.ultron.android.ui.tools.ToolsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val CHAT = "chat"
    const val MENU = "menu"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val SETTINGS_SECTION = "settings/{section}"
    const val PERMISSIONS = "permissions"

    fun settingsSection(section: String) = "settings/$section"
}

/**
 * App ka navigation. Pehli baar app khulne par ONBOARDING (permissions) dikhta hai;
 * "Aage badho" ke baad onboardingDone save hota hai, aur agli baar seedha CHAT khulta hai.
 */
@Composable
fun Nav(chatViewModel: ChatViewModel, navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val settings = remember(context) { SettingsStore(context.applicationContext) }
    val start = if (settings.onboardingDone) Routes.CHAT else Routes.ONBOARDING

    fun back() {
        navController.popBackStack()
    }

    fun openPermissions() {
        navController.navigate(Routes.PERMISSIONS)
    }

    fun openSection(id: String) {
        navController.navigate(Routes.settingsSection(id))
    }

    val onMenuAction: (String) -> Unit = { id ->
        when {
            id.startsWith("section:") -> openSection(id.removePrefix("section:"))
            id == "tools" -> navController.navigate(Routes.TOOLS)
            id == "perm" -> openPermissions()
            id == "hub" -> navController.navigate(Routes.SETTINGS)
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                settings.onboardingDone = true
                navController.navigate(Routes.CHAT) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.CHAT) {
            HomeScreen(
                viewModel = chatViewModel,
                onOpenMenu = { navController.navigate(Routes.MENU) }
            )
        }
        composable(Routes.MENU) {
            MenuScreen(onBack = { back() }, onAction = onMenuAction)
        }
        composable(Routes.TOOLS) {
            ToolsScreen(onBack = { back() })
        }
        composable(Routes.SETTINGS) {
            SettingsHomeScreen(
                onBack = { back() },
                onOpenSection = { section -> openSection(section) },
                onOpenPermissions = { openPermissions() }
            )
        }
        composable(Routes.SETTINGS_SECTION) { backStackEntry ->
            val sectionName = backStackEntry.arguments?.getString("section") ?: ""
            SettingsSection(
                section = sectionName,
                onBack = { back() },
                onOpenPermissions = { openPermissions() }
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onBack = { back() })
        }
    }
}
