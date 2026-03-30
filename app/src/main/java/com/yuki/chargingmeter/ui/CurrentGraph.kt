package com.yuki.chargingmeter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun CurrentGraph(
    history: List<Long>, // µA values
    modifier: Modifier = Modifier
) {
    if (history.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val w = size.width
        val h = size.height
        val maxVal = history.maxOf { abs(it) }.coerceAtLeast(100_000L).toFloat()

        // Grid garis tengah
        drawLine(
            color = gridColor,
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.dp.toPx()
        )

        // Grafik
        val path = Path()
        history.forEachIndexed { index, value ->
            val x = index.toFloat() / (history.size - 1) * w
            // Normalize: positif (charging) ke atas, negatif ke bawah
            val normalized = value.toFloat() / maxVal // -1..1
            val y = h / 2f - normalized * (h / 2f - 4.dp.toPx())

            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
