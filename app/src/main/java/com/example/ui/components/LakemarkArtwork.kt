package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.LimeHighlight
import com.example.ui.theme.ManropeFamily
import com.example.ui.theme.SplashBadgePlum
import kotlin.math.max

@Composable
fun LakemarkFlameEmblem(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 80.dp,
    badgeColor: Color = SplashBadgePlum,
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.lakemark_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun SplashPlateIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 320f
        val sy = size.height / 210f

        // Fresh cherry tomatoes behind plate
        drawCircle(Color(0xFFDC2626), radius = 14f * sx, center = Offset(220f * sx, 48f * sy))
        drawCircle(Color(0xFFEF4444), radius = 12f * sx, center = Offset(238f * sx, 58f * sy))
        drawCircle(Color(0xFFB91C1C), radius = 11f * sx, center = Offset(204f * sx, 60f * sy))

        // Fresh herb leaves behind plate
        drawPath(
            Path().apply {
                moveTo(140f * sx, 55f * sy)
                cubicTo(125f * sx, 25f * sy, 165f * sx, 15f * sy, 175f * sx, 45f * sy)
                close()
            },
            color = Color(0xFF16A34A),
        )
        drawPath(
            Path().apply {
                moveTo(165f * sx, 50f * sy)
                cubicTo(175f * sx, 18f * sy, 210f * sx, 22f * sy, 195f * sx, 52f * sy)
                close()
            },
            color = Color(0xFF22C55E),
        )
        drawPath(
            Path().apply {
                moveTo(95f * sx, 75f * sy)
                cubicTo(70f * sx, 55f * sy, 95f * sx, 30f * sy, 118f * sx, 60f * sy)
                close()
            },
            color = Color(0xFF15803D),
        )

        // Dark ceramic plate
        drawOval(Color(0xFF1C1917), topLeft = Offset(18f * sx, 60f * sy), size = Size(284f * sx, 124f * sy))
        drawOval(Color(0xFF292524), topLeft = Offset(28f * sx, 64f * sy), size = Size(264f * sx, 108f * sy))
        drawOval(
            Color(0xFF44403C),
            topLeft = Offset(28f * sx, 64f * sy),
            size = Size(264f * sx, 108f * sy),
            style = Stroke(width = 2f * sx),
        )
        drawOval(Color(0xFF1F1C1A), topLeft = Offset(55f * sx, 78f * sy), size = Size(210f * sx, 80f * sy))

        // Golden glazed sauce
        drawOval(
            Color(0xD99A3412),
            topLeft = Offset(68f * sx, 88f * sy),
            size = Size(184f * sx, 68f * sy),
        )

        // Roasted potatoes / sides
        rotate(-15f, pivot = Offset(95f * sx, 120f * sy)) {
            drawOval(Color(0xFFD97706), topLeft = Offset(73f * sx, 107f * sy), size = Size(44f * sx, 26f * sy))
        }
        rotate(10f, pivot = Offset(118f * sx, 134f * sy)) {
            drawOval(Color(0xFFF59E0B), topLeft = Offset(98f * sx, 122f * sy), size = Size(40f * sx, 24f * sy))
        }
        rotate(18f, pivot = Offset(222f * sx, 118f * sy)) {
            drawOval(Color(0xFFD97706), topLeft = Offset(198f * sx, 105f * sy), size = Size(48f * sx, 26f * sy))
        }
        rotate(-8f, pivot = Offset(202f * sx, 134f * sy)) {
            drawOval(Color(0xFFB45309), topLeft = Offset(183f * sx, 123f * sy), size = Size(38f * sx, 22f * sy))
        }

        // Main seared roast
        drawPath(
            Path().apply {
                moveTo(108f * sx, 102f * sy)
                cubicTo(108f * sx, 72f * sy, 206f * sx, 72f * sy, 208f * sx, 104f * sy)
                cubicTo(210f * sx, 128f * sy, 185f * sx, 138f * sy, 158f * sx, 138f * sy)
                cubicTo(128f * sx, 138f * sy, 108f * sx, 126f * sy, 108f * sx, 102f * sy)
                close()
            },
            color = Color(0xFFB45309),
        )
        drawPath(
            Path().apply {
                moveTo(114f * sx, 98f * sy)
                cubicTo(116f * sx, 74f * sy, 198f * sx, 74f * sy, 200f * sx, 98f * sy)
                cubicTo(202f * sx, 116f * sy, 180f * sx, 124f * sy, 158f * sx, 124f * sy)
                cubicTo(132f * sx, 124f * sy, 112f * sx, 114f * sy, 114f * sx, 98f * sy)
                close()
            },
            color = Color(0xFFD97706),
        )

        // Grill char marks
        val charStroke = 4.5f * sx
        listOf(
            Offset(132f * sx, 78f * sy) to Offset(124f * sx, 114f * sy),
            Offset(150f * sx, 76f * sy) to Offset(142f * sx, 116f * sy),
            Offset(168f * sx, 76f * sy) to Offset(160f * sx, 116f * sy),
            Offset(185f * sx, 80f * sy) to Offset(177f * sx, 114f * sy),
        ).forEach { (start, end) ->
            drawLine(Color(0xFF451A03), start = start, end = end, strokeWidth = charStroke, cap = StrokeCap.Round)
        }
        drawLine(Color(0xB378350F), Offset(122f * sx, 92f * sy), Offset(192f * sx, 98f * sy), strokeWidth = 2.5f * sx, cap = StrokeCap.Round)
        drawLine(Color(0xB378350F), Offset(120f * sx, 105f * sy), Offset(190f * sx, 110f * sy), strokeWidth = 2.5f * sx, cap = StrokeCap.Round)

        // Garnish leaf
        drawPath(
            Path().apply {
                moveTo(155f * sx, 78f * sy)
                cubicTo(148f * sx, 64f * sy, 168f * sx, 60f * sy, 166f * sx, 76f * sy)
                close()
            },
            color = Color(0xFF22C55E),
        )
    }
}

@Composable
fun ChefClocheIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 240f
        val sy = size.height / 165f

        // Warm backdrop circles
        drawCircle(Color(0xFFF3E3C8), radius = 52f * sx, center = Offset(95f * sx, 95f * sy))
        drawCircle(Color(0xFFF6D9A8), radius = 32f * sx, center = Offset(178f * sx, 118f * sy))

        // Botanical leaves
        drawPath(
            Path().apply {
                moveTo(55f * sx, 135f * sy)
                cubicTo(30f * sx, 125f * sy, 25f * sx, 95f * sy, 48f * sx, 85f * sy)
                cubicTo(58f * sx, 105f * sy, 58f * sx, 122f * sy, 55f * sx, 135f * sy)
                close()
            },
            color = Color(0xFF84CC16),
        )
        drawPath(
            Path().apply {
                moveTo(62f * sx, 138f * sy)
                cubicTo(45f * sx, 132f * sy, 42f * sx, 112f * sy, 58f * sx, 104f * sy)
                cubicTo(65f * sx, 118f * sy, 65f * sx, 128f * sy, 62f * sx, 138f * sy)
                close()
            },
            color = Color(0xFF65A30D),
        )
        drawPath(
            Path().apply {
                moveTo(168f * sx, 105f * sy)
                cubicTo(182f * sx, 80f * sy, 205f * sx, 75f * sy, 212f * sx, 92f * sy)
                cubicTo(195f * sx, 102f * sy, 180f * sx, 105f * sy, 168f * sx, 105f * sy)
                close()
            },
            color = Color(0xFF84CC16),
        )
        drawPath(
            Path().apply {
                moveTo(172f * sx, 112f * sy)
                cubicTo(188f * sx, 98f * sy, 208f * sx, 98f * sy, 210f * sx, 112f * sy)
                cubicTo(195f * sx, 116f * sy, 182f * sx, 115f * sy, 172f * sx, 112f * sy)
                close()
            },
            color = Color(0xFFA3E635),
        )

        // Chef torso
        val torso = Path().apply {
            moveTo(72f * sx, 155f * sy)
            cubicTo(72f * sx, 122f * sy, 92f * sx, 112f * sy, 115f * sx, 112f * sy)
            cubicTo(138f * sx, 112f * sy, 156f * sx, 122f * sy, 156f * sx, 155f * sy)
            close()
        }
        drawPath(torso, Color.White)
        drawPath(torso, Color(0xFFE5E7EB), style = Stroke(width = 1.5f * sx))

        // Collar & buttons
        drawPath(
            Path().apply {
                moveTo(102f * sx, 112f * sy)
                lineTo(115f * sx, 126f * sy)
                lineTo(128f * sx, 112f * sy)
            },
            color = Color(0xFFCBD5E1),
            style = Stroke(width = 2f * sx),
        )
        listOf(
            Offset(108f * sx, 133f * sy),
            Offset(122f * sx, 133f * sy),
            Offset(108f * sx, 145f * sy),
            Offset(122f * sx, 145f * sy),
        ).forEach { drawCircle(Color(0xFF334155), radius = 2.2f * sx, center = it) }

        // Neck & face
        drawRoundRect(
            Color(0xFFE09F7D),
            topLeft = Offset(108f * sx, 96f * sy),
            size = Size(14f * sx, 18f * sy),
            cornerRadius = CornerRadius(6f * sx, 6f * sy),
        )
        drawPath(
            Path().apply {
                moveTo(96f * sx, 72f * sy)
                cubicTo(96f * sx, 55f * sy, 134f * sx, 55f * sy, 134f * sx, 72f * sy)
                cubicTo(134f * sx, 92f * sy, 126f * sx, 102f * sy, 115f * sx, 102f * sy)
                cubicTo(104f * sx, 102f * sy, 96f * sx, 92f * sy, 96f * sx, 72f * sy)
                close()
            },
            color = Color(0xFFECA986),
        )

        // Hair & eyebrows
        drawPath(
            Path().apply {
                moveTo(96f * sx, 68f * sy)
                cubicTo(96f * sx, 56f * sy, 134f * sx, 56f * sy, 134f * sx, 68f * sy)
                cubicTo(130f * sx, 62f * sy, 100f * sx, 62f * sy, 96f * sx, 68f * sy)
                close()
            },
            color = Color(0xFF3E2723),
        )
        drawPath(
            Path().apply {
                moveTo(103f * sx, 71f * sy)
                quadraticTo(107f * sx, 69f * sy, 111f * sx, 71f * sy)
            },
            color = Color(0xFF3E2723),
            style = Stroke(width = 2f * sx, cap = StrokeCap.Round),
        )
        drawPath(
            Path().apply {
                moveTo(119f * sx, 71f * sy)
                quadraticTo(123f * sx, 69f * sy, 127f * sx, 71f * sy)
            },
            color = Color(0xFF3E2723),
            style = Stroke(width = 2f * sx, cap = StrokeCap.Round),
        )

        // Eyes & smile
        drawCircle(Color(0xFF1E293B), radius = 2f * sx, center = Offset(107f * sx, 76f * sy))
        drawCircle(Color(0xFF1E293B), radius = 2f * sx, center = Offset(123f * sx, 76f * sy))
        drawPath(
            Path().apply {
                moveTo(109f * sx, 86f * sy)
                quadraticTo(115f * sx, 92f * sy, 121f * sx, 86f * sy)
            },
            color = Color(0xFF9A3412),
            style = Stroke(width = 2f * sx, cap = StrokeCap.Round),
        )

        // Puffy chef hat
        val hat = Path().apply {
            moveTo(95f * sx, 58f * sy)
            cubicTo(82f * sx, 54f * sy, 82f * sx, 34f * sy, 98f * sx, 32f * sy)
            cubicTo(102f * sx, 20f * sy, 128f * sx, 20f * sy, 132f * sx, 32f * sy)
            cubicTo(148f * sx, 34f * sy, 148f * sx, 54f * sy, 135f * sx, 58f * sy)
            close()
        }
        drawPath(hat, Color.White)
        drawPath(hat, Color(0xFFE2E8F0), style = Stroke(width = 1.5f * sx))
        drawRoundRect(
            Color(0xFFF8FAFC),
            topLeft = Offset(97f * sx, 53f * sy),
            size = Size(36f * sx, 9f * sy),
            cornerRadius = CornerRadius(3f * sx, 3f * sx),
        )

        // Hand & silver cloche
        drawPath(
            Path().apply {
                moveTo(150f * sx, 135f * sy)
                lineTo(168f * sx, 122f * sy)
                lineTo(176f * sx, 128f * sy)
                lineTo(156f * sx, 146f * sy)
                close()
            },
            color = Color(0xFFECA986),
        )
        drawOval(Color(0xFF64748B), topLeft = Offset(148f * sx, 117f * sy), size = Size(56f * sx, 8f * sy))
        drawPath(
            Path().apply {
                moveTo(152f * sx, 120f * sy)
                cubicTo(152f * sx, 100f * sy, 200f * sx, 100f * sy, 200f * sx, 120f * sy)
                close()
            },
            color = Color(0xFF94A3B8),
        )
        drawPath(
            Path().apply {
                moveTo(158f * sx, 117f * sy)
                cubicTo(160f * sx, 104f * sy, 174f * sx, 103f * sy, 176f * sx, 117f * sy)
                close()
            },
            color = Color(0x99CBD5E1),
        )
        drawCircle(Color(0xFF475569), radius = 3.5f * sx, center = Offset(176f * sx, 102f * sy))
    }
}

@Composable
fun LoginWaveAccent(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 400f
        val sy = size.height / 60f

        drawPath(
            Path().apply {
                moveTo(0f, 10f * sy)
                cubicTo(120f * sx, 10f * sy, 160f * sx, 55f * sy, 260f * sx, 60f * sy)
                lineTo(0f, 60f * sy)
                close()
            },
            color = DeepPlum,
        )
        drawPath(
            Path().apply {
                moveTo(90f * sx, 38f * sy)
                cubicTo(150f * sx, 38f * sy, 185f * sx, 55f * sy, 240f * sx, 60f * sy)
                lineTo(90f * sx, 60f * sy)
                close()
            },
            color = EmeraldAccent,
        )
        drawCircle(Color(0xFFF3E2C5), radius = 45f * sy, center = Offset(350f * sx, 40f * sy))
    }
}

@Composable
fun CircularServedRing(
    percent: Int,
    accentColor: Color,
    trackColor: Color,
    centerColor: Color,
    onPlumColor: Color,
    modifier: Modifier = Modifier,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val sweep by animateFloatAsState(
        targetValue = if (started) (percent.coerceIn(0, 100) / 100f) * 360f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ringSweep",
    )

    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f
            drawCircle(color = centerColor, radius = (size.minDimension - strokeWidth) / 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                fontFamily = com.example.ui.theme.lakemarkFont(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color.White,
                lineHeight = 22.sp,
            )
            Text(
                text = com.example.localization.LocalAppStrings.current.get("home_served_ring_sub", "served"),
                fontFamily = com.example.ui.theme.lakemarkFont(),
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                color = onPlumColor.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
fun MiniSevenDayBarChart(
    dayCounts: List<Int>,
    accentColor: Color,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxVal = max(1, dayCounts.maxOrNull() ?: 1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        dayCounts.forEachIndexed { index, count ->
            val targetFraction = if (count <= 0) 0.12f else max(0.18f, count.toFloat() / maxVal.toFloat())
            val animatedFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 600 + index * 60, easing = FastOutSlowInEasing),
                label = "bar_$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedFraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index == dayCounts.lastIndex) accentColor else barColor),
            )
        }
    }
}
