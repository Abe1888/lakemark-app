package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold),
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
)

val ManropeFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
    Font(R.font.manrope, FontWeight.ExtraBold),
)

val EthiopicFamily = FontFamily(
    Font(R.font.noto_sans_ethiopic, FontWeight.Normal),
    Font(R.font.noto_sans_ethiopic, FontWeight.Medium),
    Font(R.font.noto_sans_ethiopic, FontWeight.SemiBold),
    Font(R.font.noto_sans_ethiopic, FontWeight.Bold),
    Font(R.font.noto_sans_ethiopic, FontWeight.ExtraBold),
)

fun buildLakemarkTypography(ethiopic: Boolean = false): Typography {
    fun pick(defaultFamily: FontFamily): FontFamily = if (ethiopic) EthiopicFamily else defaultFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (ethiopic) 54.sp else 68.sp,
            lineHeight = if (ethiopic) 62.sp else 70.sp,
            letterSpacing = if (ethiopic) (-0.8).sp else (-2.2).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = pick(PlusJakartaSansFamily),
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (ethiopic) 28.sp else 32.sp,
            lineHeight = if (ethiopic) 36.sp else 36.sp,
            letterSpacing = if (ethiopic) 0.sp else (-0.8).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (ethiopic) 25.sp else 28.sp,
            lineHeight = if (ethiopic) 32.sp else 31.sp,
            letterSpacing = if (ethiopic) 0.sp else (-1.1).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = pick(PlusJakartaSansFamily),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            lineHeight = 29.sp,
            letterSpacing = if (ethiopic) 0.sp else (-0.4).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            lineHeight = 25.sp,
            letterSpacing = if (ethiopic) 0.sp else (-0.4).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            letterSpacing = if (ethiopic) 0.sp else (-0.3).sp,
        ),
        titleSmall = TextStyle(
            fontFamily = pick(InterFamily),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = pick(InterFamily),
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = pick(ManropeFamily),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = pick(InterFamily),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.2.sp,
        ),
    )
}

val LakemarkTypography = buildLakemarkTypography(ethiopic = false)
val LakemarkEthiopicTypography = buildLakemarkTypography(ethiopic = true)

