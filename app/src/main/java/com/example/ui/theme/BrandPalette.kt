package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.BrandConfig
import com.example.data.model.UserRole
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Immutable
data class BrandPalette(
    val roleKey: UserRole,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val brandName: String,
    val mark: String,
    val bg: Color,
    val surface: Color,
    val alt: Color,
    val line: Color,
    val plum: Color,
    val plum2: Color,
    val acc: Color,
    val acc2: Color,
    val onAcc: Color,
    val onPlum: Color,
    val text: Color,
    val mut: Color,
    val sub: Color,
    val plumDark: Color,
    val gradTop: Color,
    val gradBottom: Color,
)

val LocalBrandPalette = staticCompositionLocalOf { deriveBrandPalette(UserRole.MANAGER.defaultBrandConfig()) }

fun parseHexColor(hex: String, fallback: Color = CreamBg): Color {
    val clean = hex.trim().removePrefix("#")
    return try {
        val full = if (clean.length == 3) {
            clean.map { "$it$it" }.joinToString("")
        } else {
            clean
        }
        Color(android.graphics.Color.parseColor("#$full"))
    } catch (_: Exception) {
        fallback
    }
}

fun Color.toHexString(): String {
    val argb = toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

fun relativeLuminance(color: Color): Double {
    fun channel(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

fun contrastRatio(a: Color, b: Color): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
}

fun mixColors(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = ((a.red + (b.red - a.red) * clamped) * 255f).roundToInt().coerceIn(0, 255),
        green = ((a.green + (b.green - a.green) * clamped) * 255f).roundToInt().coerceIn(0, 255),
        blue = ((a.blue + (b.blue - a.blue) * clamped) * 255f).roundToInt().coerceIn(0, 255),
        alpha = 255,
    )
}

fun deriveBrandPalette(config: BrandConfig): BrandPalette {
    val role = UserRole.fromKey(config.roleKey)
    val p = parseHexColor(config.primaryHex, CreamBg)
    val s = parseHexColor(config.secondaryHex, DeepPlum)
    val a = parseHexColor(config.accentHex, EmeraldAccent)
    val isDefaultManager = role == UserRole.MANAGER &&
        config.primaryHex.equals("#FAF3E6", ignoreCase = true) &&
        config.secondaryHex.equals("#3D0F4F", ignoreCase = true) &&
        config.accentHex.equals("#04C353", ignoreCase = true)

    if (isDefaultManager) {
        return BrandPalette(
            roleKey = role,
            primary = p,
            secondary = s,
            accent = a,
            brandName = config.brandName.ifBlank { role.displayName },
            mark = config.mark,
            bg = CreamBg,
            surface = CreamSurface,
            alt = CreamAlt,
            line = WarmLine,
            plum = DeepPlum,
            plum2 = PlumMid,
            acc = EmeraldAccent,
            acc2 = EmeraldDark,
            onAcc = OnCreamText,
            onPlum = OnPlumCream,
            text = OnCreamText,
            mut = MutedPlum,
            sub = SubPlum,
            plumDark = PlumDark,
            gradTop = PlumGradTop,
            gradBottom = PlumGradBottom,
        )
    }

    val onPlum = if (relativeLuminance(s) < 0.4) Color(0xFFFBF5E3) else Color(0xFF1D0F1A)
    val darkOnAcc = mixColors(s, Color.Black, 0.55f)
    val onAcc = if (contrastRatio(a, Color.White) >= contrastRatio(a, darkOnAcc)) Color.White else darkOnAcc
    val text = mixColors(s, Color.Black, 0.50f)

    return BrandPalette(
        roleKey = role,
        primary = p,
        secondary = s,
        accent = a,
        brandName = config.brandName.ifBlank { role.displayName },
        mark = config.mark,
        bg = p,
        surface = mixColors(p, Color.White, 0.60f),
        alt = mixColors(p, s, 0.08f),
        line = mixColors(p, s, 0.17f),
        plum = s,
        plum2 = mixColors(s, Color.White, 0.14f),
        acc = a,
        acc2 = mixColors(a, Color.Black, 0.16f),
        onAcc = onAcc,
        onPlum = onPlum,
        text = text,
        mut = mixColors(text, p, 0.42f),
        sub = mixColors(text, p, 0.35f),
        plumDark = mixColors(s, Color.Black, 0.55f),
        gradTop = mixColors(s, Color.White, 0.10f),
        gradBottom = mixColors(s, Color.Black, 0.28f),
    )
}
