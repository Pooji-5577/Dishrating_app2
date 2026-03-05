package com.example.smackcheck2.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors

data class PhotoFilterState(
    val brightness: Float = 0f,    // -1f to 1f
    val contrast: Float = 1f,      // 0f to 2f
    val saturation: Float = 1f     // 0f to 2f
)

fun PhotoFilterState.toColorFilter(): ColorFilter {
    val brightnessMatrix = ColorMatrix().apply {
        val b = brightness * 255f
        set(0, 4, b)
        set(1, 4, b)
        set(2, 4, b)
    }

    val contrastMatrix = ColorMatrix().apply {
        val c = contrast
        val t = (1f - c) / 2f * 255f
        set(0, 0, c)
        set(1, 1, c)
        set(2, 2, c)
        set(0, 4, t)
        set(1, 4, t)
        set(2, 4, t)
    }

    val saturationMatrix = ColorMatrix().apply {
        setToSaturation(saturation)
    }

    // Combine matrices
    brightnessMatrix.timesAssign(contrastMatrix)
    brightnessMatrix.timesAssign(saturationMatrix)

    return ColorFilter.colorMatrix(brightnessMatrix)
}

@Composable
fun PhotoFilterControls(
    filterState: PhotoFilterState,
    onFilterChange: (PhotoFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Photo Adjustments",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Brightness
        FilterSlider(
            label = "Brightness",
            value = filterState.brightness,
            valueRange = -1f..1f,
            onValueChange = { onFilterChange(filterState.copy(brightness = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Contrast
        FilterSlider(
            label = "Contrast",
            value = filterState.contrast,
            valueRange = 0f..2f,
            onValueChange = { onFilterChange(filterState.copy(contrast = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Saturation
        FilterSlider(
            label = "Saturation",
            value = filterState.saturation,
            valueRange = 0f..2f,
            onValueChange = { onFilterChange(filterState.copy(saturation = it)) }
        )
    }
}

@Composable
private fun FilterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val colors = appColors()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = colors.TextSecondary,
            modifier = Modifier.width(80.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.Primary,
                activeTrackColor = colors.Primary
            )
        )

        Text(
            text = String.format("%.1f", value),
            fontSize = 12.sp,
            color = colors.TextSecondary,
            modifier = Modifier.width(36.dp)
        )
    }
}
