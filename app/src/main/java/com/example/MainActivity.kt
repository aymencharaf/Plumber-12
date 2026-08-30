package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.PlumberTheme
import com.example.ui.viewmodel.PlumberViewModel

sealed class Screen(val route: String, val titleAr: String, val icon: ImageVector) {
    object Home : Screen("home", "الرئيسية", Icons.Default.Home)
    object Projects : Screen("projects", "المشاريع", Icons.Default.Folder)
    object Catalog : Screen("catalog", "المواد", Icons.Default.Category)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
    object Invoices : Screen("invoices", "الفواتير", Icons.Default.ReceiptLong)
    object CurrentProject : Screen("current_project", "مواد المشروع", Icons.Default.ListAlt)
    object Team : Screen("team", "الفريق", Icons.Default.Group)
    object Estimator : Screen("estimator", "الحاسبة", Icons.Default.Calculate)
    object Appointment : Screen("appointments", "طلب موعد", Icons.Default.Event)
    object Welcome : Screen("welcome", "الترحيب", Icons.Default.Home)
    object Login : Screen("login", "تسجيل الدخول", Icons.Default.Lock)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlumberTheme {
                PlumberApp()
            }
        }
    }
}

@Composable
fun PlumberApp() {
    val navController = rememberNavController()
    val viewModel: PlumberViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

    LaunchedEffect(isUserLoggedIn) {
        if (!isUserLoggedIn && currentRoute != Screen.Welcome.route) {
            navController.navigate(Screen.Welcome.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomBarVisibleRoutes = listOf(
        Screen.Home.route,
        Screen.Projects.route,
        Screen.Catalog.route,
        Screen.Settings.route,
        Screen.Invoices.route,
        Screen.Team.route
    )

    val showBottomBar = currentRoute in bottomBarVisibleRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    tonalElevation = 12.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. الرئيسية (Home)
                        BottomNavItemView(
                            screen = Screen.Home,
                            selected = currentRoute == Screen.Home.route,
                            onClick = {
                                if (currentRoute != Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )

                        // 2. المشاريع (Projects)
                        BottomNavItemView(
                            screen = Screen.Projects,
                            selected = currentRoute == Screen.Projects.route,
                            onClick = {
                                if (currentRoute != Screen.Projects.route) {
                                    navController.navigate(Screen.Projects.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )

                        // 3. Center FAB (+) Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .offset(y = (-10).dp)
                                .size(54.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable {
                                    navController.navigate("new_project")
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "مشروع جديد",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // 4. المواد (Materials)
                        BottomNavItemView(
                            screen = Screen.Catalog,
                            selected = currentRoute == Screen.Catalog.route,
                            onClick = {
                                if (currentRoute != Screen.Catalog.route) {
                                    navController.navigate(Screen.Catalog.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )

                        // 5. الإعدادات (Settings)
                        BottomNavItemView(
                            screen = Screen.Settings,
                            selected = currentRoute == Screen.Settings.route,
                            onClick = {
                                if (currentRoute != Screen.Settings.route) {
                                    navController.navigate(Screen.Settings.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNewProject = { workType ->
                        val route = if (workType != null) "new_project?workType=$workType" else "new_project"
                        navController.navigate(route)
                    },
                    onNavigateToProjectDetails = {
                        navController.navigate("work_type_materials")
                    },
                    onNavigateToEstimator = {
                        navController.navigate(Screen.Estimator.route)
                    },
                    onNavigateToTeam = {
                        navController.navigate(Screen.Team.route)
                    },
                    onNavigateToInvoices = {
                        navController.navigate(Screen.Invoices.route)
                    },
                    onNavigateToProjects = {
                        navController.navigate(Screen.Projects.route)
                    },
                    onNavigateToCatalog = {
                        navController.navigate(Screen.Catalog.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToAppointment = {
                        navController.navigate(Screen.Appointment.route)
                    }
                )
            }

            composable(Screen.Appointment.route) {
                AppointmentScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    viewModel = viewModel,
                    onLoginClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onRegisterClick = {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    initialTab = 1, // Open on "تسجيل عامل جديد" tab
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Invoices.route) {
                InvoicesManagementScreen(
                    viewModel = viewModel,
                    onBack = null
                )
            }

            composable(Screen.Team.route) {
                TeamManagementScreen(
                    viewModel = viewModel,
                    onBack = null,
                    onNavigateToProjectDetails = {
                        navController.navigate("work_type_materials")
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Projects.route) {
                ProjectsListScreen(
                    viewModel = viewModel,
                    onNavigateToNewProject = { navController.navigate("new_project") },
                    onNavigateToProjectDetails = {
                        navController.navigate("work_type_materials")
                    }
                )
            }

            composable(Screen.CurrentProject.route) {
                ProjectMaterialsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToAddMore = { navController.navigate("work_type_materials") }
                )
            }

            composable(Screen.Catalog.route) {
                CatalogScreen(
                    viewModel = viewModel,
                    onNavigateToProject = { navController.navigate(Screen.CurrentProject.route) }
                )
            }

            composable(Screen.Estimator.route) {
                PipeCalculatorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToProjectMaterials = { navController.navigate(Screen.CurrentProject.route) }
                )
            }

            composable(
                route = "new_project?workType={workType}",
                arguments = listOf(navArgument("workType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val workType = backStackEntry.arguments?.getString("workType")
                NewProjectScreen(
                    viewModel = viewModel,
                    initialWorkTypeKey = workType,
                    onBack = { navController.popBackStack() },
                    onProjectCreated = {
                        navController.navigate("work_type_materials") {
                            popUpTo("new_project") { inclusive = true }
                        }
                    }
                )
            }

            composable("work_type_materials") {
                WorkTypeMaterialsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToProjectMaterials = {
                        navController.navigate(Screen.CurrentProject.route)
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavItemView(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.titleAr,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = screen.titleAr,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
