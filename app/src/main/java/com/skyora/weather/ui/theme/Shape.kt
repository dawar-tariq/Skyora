package com.skyora.weather.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SkyoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object SkyoraRadius {
    val chip = 50.dp
    val button = 50.dp
    val field = 18.dp
    val card = 24.dp
    val hero = 30.dp
    val sheet = 32.dp
}
