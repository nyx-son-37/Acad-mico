package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.example.ui.theme.academicColors
import kotlin.random.Random

private data class PaperGrainSpec(
    val xFraction: Float,
    val yFraction: Float,
    val radiusPx: Float,
    val alpha: Float,
    val isFiber: Boolean,
    val angleOffset: Float,
)

/**
 * Provides a natural, organic parchment or dark-library walnut backdrop
 * using subtle seeded paper-grain speckles and ambient warm vignette.
 */
@Composable
fun OxfordLibraryBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val academic = MaterialTheme.academicColors
    val bgColor = MaterialTheme.colorScheme.background
    val grainColor = academic.paperGrain
    val ambientGlow = MaterialTheme.colorScheme.tertiary.copy(
        alpha = if (academic.isDark) 0.08f else 0.06f
    )
    val edgeShade = if (academic.isDark) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        academic.paperGrain.copy(alpha = 0.05f)
    }

    val grains = remember {
        val rng = Random(1386)
        List(160) { index ->
            PaperGrainSpec(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat(),
                radiusPx = 0.8f + rng.nextFloat() * 1.6f,
                alpha = 0.02f + rng.nextFloat() * 0.045f,
                isFiber = index % 5 == 0,
                angleOffset = (rng.nextFloat() - 0.5f) * 14f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (academic.paperTextureEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Soft warm reading-lamp radial wash from upper edge
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(ambientGlow, Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.10f),
                        radius = size.maxDimension * 0.72f
                    )
                )
                // Gentle perimeter depth (aged page edge / library shadow)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, edgeShade),
                        startY = size.height * 0.65f,
                        endY = size.height
                    )
                )
                // Organic paper grain & cellulose fibers
                grains.forEach { g ->
                    val x = g.xFraction * size.width
                    val y = g.yFraction * size.height
                    val c = grainColor.copy(alpha = g.alpha)
                    if (g.isFiber) {
                        drawLine(
                            color = c,
                            start = Offset(x, y),
                            end = Offset(x + 8f + g.angleOffset, y + 3f),
                            strokeWidth = 1f
                        )
                    } else {
                        drawCircle(
                            color = c,
                            radius = g.radiusPx,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Editorial folio divider with central brass diamond ornament.
 */
@Composable
fun OxfordFolioRule(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    ornamentColor: Color = MaterialTheme.academicColors.brassAccent
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = color
        )
        Canvas(modifier = Modifier.size(10.dp)) {
            val midX = size.width / 2f
            val midY = size.height / 2f
            val path = Path().apply {
                moveTo(midX, 0f)
                lineTo(size.width, midY)
                lineTo(midX, size.height)
                lineTo(0f, midY)
                close()
            }
            drawPath(path = path, color = ornamentColor.copy(alpha = 0.78f))
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = color
        )
    }
}

@Composable
fun oxfordCardBorder(emphasized: Boolean = false): BorderStroke {
    val color = if (emphasized) {
        MaterialTheme.academicColors.brassAccent.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.62f)
    }
    return BorderStroke(1.dp, color)
}

val OxfordCardShape = RoundedCornerShape(12.dp)
val OxfordChipShape = RoundedCornerShape(8.dp)
