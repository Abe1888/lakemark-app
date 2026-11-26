package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.localization.TranslationCatalog

@Composable
fun lakemarkFont(default: FontFamily = ManropeFamily): FontFamily =
    if (LocalAppLanguage.current == AppLanguage.AM) EthiopicFamily else default

@Composable
fun LakemarkTheme(
    brandPalette: BrandPalette = deriveBrandPalette(com.example.data.model.UserRole.MANAGER.defaultBrandConfig()),
    language: AppLanguage = AppLanguage.EN,
    strings: AppStrings = TranslationCatalog.defaultEnglishStrings(),
    content: @Composable () -> Unit,
) {
    val colorScheme = lightColorScheme(
        primary = brandPalette.plum,
        onPrimary = brandPalette.onPlum,
        primaryContainer = brandPalette.alt,
        onPrimaryContainer = brandPalette.text,
        secondary = brandPalette.acc,
        onSecondary = brandPalette.onAcc,
        secondaryContainer = brandPalette.acc.copy(alpha = 0.16f),
        onSecondaryContainer = brandPalette.text,
        tertiary = brandPalette.plum2,
        onTertiary = brandPalette.onPlum,
        background = brandPalette.bg,
        onBackground = brandPalette.text,
        surface = brandPalette.surface,
        onSurface = brandPalette.text,
        surfaceVariant = brandPalette.alt,
        onSurfaceVariant = brandPalette.mut,
        outline = brandPalette.line,
        error = StatusCancelledText,
        onError = Color.White,
        errorContainer = StatusCancelledBg,
        onErrorContainer = StatusCancelledText,
    )

    val typography = if (language == AppLanguage.AM) {
        LakemarkEthiopicTypography
    } else {
        LakemarkTypography
    }

    CompositionLocalProvider(
        LocalBrandPalette provides brandPalette,
        LocalAppLanguage provides language,
        LocalAppStrings provides strings,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

