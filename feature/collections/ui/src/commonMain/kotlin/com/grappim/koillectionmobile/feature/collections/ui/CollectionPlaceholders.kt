package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// M2.1 placeholders. M2.3 replaces the list, M2.4 the collection screen.

@Composable
fun CollectionListScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Collections")
    }
}

@Composable
fun CollectionScreen(collectionId: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Collection $collectionId")
    }
}
