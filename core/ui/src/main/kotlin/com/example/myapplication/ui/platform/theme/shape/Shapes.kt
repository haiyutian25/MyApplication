package com.example.myapplication.ui.platform.theme.shape

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The shapes for the app. Material 3 exposes five shape tokens that scale the roundness of the
 * components throughout the app.
 */
internal val MyApplicationShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(size = 4.dp),
    small = RoundedCornerShape(size = 8.dp),
    medium = RoundedCornerShape(size = 12.dp),
    large = RoundedCornerShape(size = 16.dp),
    extraLarge = RoundedCornerShape(size = 28.dp),
)
