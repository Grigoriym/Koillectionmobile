package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// M2.1 placeholder. M2.4 replaces the collection screen.

@Composable
fun CollectionScreen(collectionId: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Collection $collectionId")
    }
}
