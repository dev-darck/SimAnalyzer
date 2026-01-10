package com.project.analyzer.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.analyzer.navigation.api.EntryFactory

@Composable
fun App(providerFactory: EntryFactory) {

    val items = listOf(
        NavItem("live", "Live", Icons.Outlined.Star),
        NavItem("session", "Session", Icons.Outlined.AccountBox),
        NavItem("setup", "Setup", Icons.Outlined.Build),
    )

    val settings = NavItem("settings", "Settings", Icons.Outlined.Settings)

    var selected by remember { mutableStateOf("live") }

    Row(Modifier.fillMaxSize()) {
        AceLikeSidebar(
            topIcon = { Icon(Icons.Outlined.Edit, null, tint = Color(0xFF4D86FF)) },
            items = items,
            bottomItem = settings,
            selectedKey = selected,
            onSelect = { selected = it.key }
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "Selected: $selected",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
//    AppNavGraph(
//        navigationState = rememberNavigationState(),
//        providerFactory = providerFactory
//    )
}
