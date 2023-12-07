package com.rockethat.ornaassistant

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CustomModalDrawer() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val items = listOf("Home", "Profile", "Settings")

    ModalDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(items = items, onItemClicked = { /* Handle click */ })
        }
    ) {
        MainContent(drawerState = drawerState)
    }
}

@Composable
fun DrawerContent(items: List<String>, onItemClicked: (String) -> Unit) {
    items.forEach { item ->
        Text(
            text = item,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.Black
        )
    }
}

@Composable
fun MainContent(drawerState: DrawerState) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Your main content goes here
        Text(text = "Main Content")
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
    CustomModalDrawer()
}