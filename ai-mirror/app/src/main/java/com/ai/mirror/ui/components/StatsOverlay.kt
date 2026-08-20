package com.ai.mirror.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.mirror.R
import com.ai.mirror.data.streaming.StreamMetrics
import com.ai.mirror.ui.theme.ErrorRed
import com.ai.mirror.ui.theme.SuccessGreen
import com.ai.mirror.ui.theme.WarningOrange

@Composable
fun StatsOverlay(
    metrics: StreamMetrics,
    modifier: Modifier = Modifier,
    isSender: Boolean = false
) {
    val fpsColor = when {
        metrics.fps >= 24f -> SuccessGreen
        metrics.fps >= 15f -> WarningOrange
        else -> ErrorRed
    }

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_fps, metrics.fps),
                color = fpsColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = stringResource(R.string.stats_bitrate, metrics.bitrateKbps / 8f),
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isSender && metrics.latencyMs > 0) {
                val latencyColor = when {
                    metrics.latencyMs < 100 -> SuccessGreen
                    metrics.latencyMs < 250 -> WarningOrange
                    else -> ErrorRed
                }
                Text(
                    text = stringResource(R.string.stats_latency, metrics.latencyMs),
                    color = latencyColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (metrics.width > 0 && metrics.height > 0) {
                Text(
                    text = stringResource(R.string.stats_resolution, metrics.width, metrics.height),
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
