package com.rockethat.ornaassistant

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CustomModalDrawer(context: Context) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope() // Create a coroutine scope
    val items = listOf("Dungeon Visits", "Kingdom", "Orna hub", "Orna Guide", "Settings")

    ModalDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(items = items) { selectedItem ->
                handleNavigation(selectedItem, context)
                coroutineScope.launch {
                    drawerState.close() // Use the coroutine scope here
                }
            }
        }
    ) {
        MainContent(drawerState = drawerState)
    }
}

private fun handleNavigation(item: String, context: Context) {
    when (item) {
        "Dungeon Visits" -> {
            // If MainActivity is already open, you might not need to do anything
        }
        "Kingdom" -> {
            context.startActivity(Intent(context, KingdomActivity::class.java))

        }
        "Orna hub" -> {
            context.startActivity(Intent(context, OrnaHubActivity::class.java))
        }
        "Orna Guide" -> {
        context.startActivity(Intent(context, OrnaGuideActivity::class.java))
        }
        "Settings" -> {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}

@Composable
fun DrawerContent(items: List<String>, onItemClicked: (String) -> Unit) {
    items.forEach { item ->
        Text(
            text = item,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onItemClicked(item) },
            color = Color.Black
        )
    }
}

@Composable
fun MainContent(drawerState: DrawerState) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Your main content goes here
        Text(text = "")
        IconButton(onClick = {
            coroutineScope.launch {
                drawerState.open()
            }
        }) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CustomModalDrawer(LocalContext.current)
}