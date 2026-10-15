package com.saeed.ideasmo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

// Material 3 defaults, with one correction: labelSmall is raised to 12sp
// because the design kit forbids any text below 12sp.
val AppTypography: Typography = Typography().let { base ->
    base.copy(
        labelSmall = base.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp)
    )
}