package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.ui.components.LakemarkFlameEmblem
import com.example.ui.components.SplashPlateIllustration
import com.example.ui.theme.CreamBg
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.PlumGradBottom
import com.example.ui.theme.PlumGradTop
import com.example.ui.theme.lakemarkFont

@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    onSelectLanguage: ((AppLanguage) -> Unit)? = null,
) {
    val ctaInk = Color(0xFF2A1226)
    val s = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val font = lakemarkFont()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient Decorative Circles matching HTML SOT
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-48).dp, y = 96.dp)
                .size(128.dp)
                .clip(CircleShape)
                .background(Color(0x99F3E4CA)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 32.dp)
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0xB3F3E4CA)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = 256.dp)
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0x99F3E4CA)),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp),
        ) {
            val isCompactHeight = maxHeight < 640.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (onSelectLanguage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFF3E4CA),
                        ) {
                            Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                AppLanguage.entries.forEach { lang ->
                                    val selected = lang == currentLang
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (selected) DeepPlum else Color.Transparent)
                                            .clickable { onSelectLanguage(lang) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("splash_lang_${lang.code}"),
                                    ) {
                                        Text(
                                            text = lang.shortCode,
                                            fontFamily = font,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.5.sp,
                                            color = if (selected) Color.White else DeepPlum,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Centered Top Brand Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    LakemarkFlameEmblem(
                        badgeSize = if (isCompactHeight) 78.dp else 88.dp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = s.get("app_brand_name", "LAKEMARK"),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        letterSpacing = 1.6.sp,
                        color = DeepPlum,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = s.get("app_subtitle", "Kitchen Management"),
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFF5B2A6E),
                    )
                }

                // Bottom Curved Hero Food Illustration & CTA
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp)
                            .clip(RoundedCornerShape(topStart = 150.dp, topEnd = 150.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PlumGradTop, PlumGradBottom),
                                ),
                            )
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                            .padding(horizontal = 24.dp)
                            .padding(top = 92.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(
                            onClick = onGetStarted,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = ctaInk,
                            ),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(10.dp, RoundedCornerShape(999.dp))
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF07CD59), EmeraldDark),
                                    ),
                                )
                                .testTag("get_started_button"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = s.get("btn_get_started", "Get Started"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.5.sp,
                                    color = ctaInk,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = ctaInk,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // Gourmet plate overlapping the top of the purple curve
                    SplashPlateIllustration(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 256.dp, height = 160.dp),
                    )
                }
            }
        }
    }
}
