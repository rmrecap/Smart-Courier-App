package com.smartcourier.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.smartcourier.core.domain.model.DailyEarnings
import com.smartcourier.core.ui.theme.ColorTokens
import com.smartcourier.core.ui.theme.Dimens

@Composable
fun EarningsChart(
    dailyEarnings: List<DailyEarnings>,
    modifier: Modifier = Modifier,
    todayDayOfWeek: Int = java.time.LocalDate.now().dayOfWeek.value
) {
    if (dailyEarnings.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = Dimens.grid_24, bottom = Dimens.grid_24)
    ) {
        val barCount = dailyEarnings.size
        val barSpacing = size.width / (barCount * 3f)
        val barWidth = barSpacing * 2f
        val labelHeight = 28.dp.toPx()
        val chartBottom = size.height - labelHeight
        val chartTop = 24.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val maxEarnings = dailyEarnings.maxOf { it.earnings + it.tipAmount }.coerceAtLeast(1.0)

        // Background grid lines
        val gridLineCount = 4
        val gridPaint = android.graphics.Paint().apply {
            color = ColorTokens.Divider.copy(alpha = 0.3f).toArgb()
            strokeWidth = 1.dp.toPx()
        }
        for (i in 0..gridLineCount) {
            val y = chartTop + chartHeight * i / gridLineCount
            drawContext.canvas.nativeCanvas.drawLine(
                0f, y, size.width, y, gridPaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                "AED ${(maxEarnings * (1f - i.toFloat() / gridLineCount)).toInt()}",
                size.width - 4.dp.toPx(), y - 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = ColorTokens.TextSecondary.toArgb()
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        dailyEarnings.forEachIndexed { index, day ->
            val barEarnings = day.earnings + day.tipAmount
            val barHeight = (barEarnings / maxEarnings * chartHeight).toFloat()
            val x = barSpacing + index * (barWidth + barSpacing)
            val y = chartBottom - barHeight
            val isToday = day.dayOfWeek == todayDayOfWeek

            val barColor = if (isToday) ColorTokens.DeliveryOrange
            else ColorTokens.DeliveryOrange.copy(alpha = 0.45f)

            val tipColor = ColorTokens.Success.copy(alpha = 0.7f)

            // Tip segment stacked on top of earnings
            val earningsHeight = (day.earnings / maxEarnings * chartHeight).toFloat()
            val tipHeight = (day.tipAmount / maxEarnings * chartHeight).toFloat()

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, chartBottom - earningsHeight),
                size = Size(barWidth, earningsHeight.coerceAtLeast(1f)),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            if (tipHeight > 0f) {
                drawRoundRect(
                    color = tipColor,
                    topLeft = Offset(x, chartBottom - earningsHeight - tipHeight),
                    size = Size(barWidth, tipHeight.coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Value label
            drawContext.canvas.nativeCanvas.drawText(
                "AED ${(day.earnings + day.tipAmount).toInt()}",
                x + barWidth / 2f,
                (chartBottom - barHeight - 6.dp.toPx()).coerceAtLeast(12.dp.toPx()),
                android.graphics.Paint().apply {
                    color = if (isToday) ColorTokens.DeliveryOrange.toArgb()
                    else ColorTokens.TextSecondary.toArgb()
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            // Day label
            drawContext.canvas.nativeCanvas.drawText(
                day.label,
                x + barWidth / 2f,
                size.height - 2.dp.toPx(),
                android.graphics.Paint().apply {
                    color = if (isToday) ColorTokens.DeliveryOrange.toArgb()
                    else ColorTokens.TextSecondary.toArgb()
                    textSize = 12.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = isToday
                }
            )

            // Today indicator underline
            if (isToday) {
                val underlineY = size.height - 2.dp.toPx() + 14.dp.toPx()
                drawRoundRect(
                    color = ColorTokens.DeliveryOrange,
                    topLeft = Offset(x + 4.dp.toPx(), underlineY),
                    size = Size(barWidth - 8.dp.toPx(), 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }
    }
}
