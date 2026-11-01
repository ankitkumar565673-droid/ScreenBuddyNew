package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * ScreenAccessInfoScreen - redirects to the full Level 2 ScreenAccessScreen.
 */
@Composable
fun ScreenAccessInfoScreen(
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenAccessScreen(
        onBackClick = onBackClick,
        modifier = modifier
    )
}

