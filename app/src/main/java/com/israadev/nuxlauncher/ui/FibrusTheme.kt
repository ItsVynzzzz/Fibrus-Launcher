package com.israadev.nuxlauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Warna diambil dari tema Amber (gelap, aksen kuning amber). */
object FibrusColors {
    val Ink = Color(0xFF111111)
    val Amber = Color(0xFFFFD60A)
    val Text = Color(0xFFF4F3EE)
    val TextDim = Color(0xFFB2B0A6)
    val Line = Color(0x26FFFFFF)
    val Danger = Color(0xFFFF5A4D)
}

@Composable
fun FibrusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FibrusColors.Amber,
            onPrimary = FibrusColors.Ink,
            background = FibrusColors.Ink,
            onBackground = FibrusColors.Text,
            surface = FibrusColors.Ink,
            onSurface = FibrusColors.Text,
            error = FibrusColors.Danger
        ),
        content = content
    )
}
