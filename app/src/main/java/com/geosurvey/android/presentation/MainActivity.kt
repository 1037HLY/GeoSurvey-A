package com.geosurvey.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geosurvey.android.presentation.theme.GeoSurveyTheme
import com.geosurvey.android.presentation.ui.*
import com.geosurvey.android.presentation.ui.analysis.RoseDiagramScreen
import com.geosurvey.android.presentation.ui.analysis.StereographicScreen
import com.geosurvey.android.presentation.viewmodel.AttitudeViewModel
import com.geosurvey.android.presentation.viewmodel.TrackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoSurveyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as com.geosurvey.android.GeoSurveyApplication
    val trackViewModel = remember { TrackViewModel.getInstance(application) }
    val attitudeViewModel = remember { AttitudeViewModel.getInstance(application) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White.copy(alpha = 0.8f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("首页") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("🛣️", fontSize = 20.sp) },
                    label = { Text("轨迹") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate("track") {
                            popUpTo("track") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("🔬", fontSize = 20.sp) },
                    label = { Text("产状") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        navController.navigate("attitude") {
                            popUpTo("attitude") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("分析") },
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        navController.navigate("analysis") {
                            popUpTo("analysis") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Text("📷", fontSize = 20.sp) },
                    label = { Text("相机") },
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        navController.navigate("camera") {
                            popUpTo("camera") { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen()
            }
            composable("track") {
                TrackScreen(navController = navController)
            }
            composable("attitude") {
                AttitudeScreen()
            }
            composable("analysis") {
                AnalysisScreen(navController = navController)
            }
            composable("stereographic") {
                val records = attitudeViewModel.state.value.records
                StereographicScreen(
                    records = records,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("rose") {
                val records = attitudeViewModel.state.value.records
                RoseDiagramScreen(
                    records = records,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("camera") {
                CameraScreen()
            }
            composable("gallery") {
                PhotoGalleryScreen()
            }
            // ⭐ 导航页面路由
            composable("navigation") {
                NavigationScreen()
            }
        }
    }
}
