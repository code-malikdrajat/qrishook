package com.qrishook.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val QRISHookShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(Radius.button),
    medium = RoundedCornerShape(Radius.card),
    large = RoundedCornerShape(Radius.fab),
    extraLarge = RoundedCornerShape(24.dp),
)
