package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.LocalAppStrings
import com.example.ui.components.ChefClocheIllustration
import com.example.ui.components.LoginWaveAccent
import com.example.ui.theme.CreamAlt
import com.example.ui.theme.CreamBg
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.WarmLine
import com.example.ui.theme.lakemarkFont

private enum class AuthMode(val key: String, val stringKey: String, val fallbackLabel: String) {
    STAFF("staff", "auth_tab_staff", "Staff Sign In"),
    GOOGLE("google", "auth_tab_google", "Manager Login"),
}

@Composable
fun AuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit,
    onStaffSignIn: (username: String, password: String) -> Unit = { _, _ -> },
    onClearError: () -> Unit = {},
    onBackToSplash: () -> Unit = {},
) {
    var mode by remember { mutableStateOf(AuthMode.STAFF) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val s = LocalAppStrings.current
    val font = lakemarkFont()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    ),
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(width = 210.dp, height = 136.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ChefClocheIllustration(
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, CreamBg),
                                ),
                            ),
                    )
                    Image(
                        painter = painterResource(R.drawable.lakemark_logo),
                        contentDescription = s.get("app_brand_name", "LAKEMARK"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-10).dp, y = (-4).dp)
                            .size(44.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .border(2.dp, CreamBg, CircleShape),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = s.get("auth_welcome", "Welcome to Lakemark"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 27.sp,
                    letterSpacing = (-0.6).sp,
                    color = DeepPlum,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (mode == AuthMode.STAFF) {
                        s.get(
                            "auth_desc_staff",
                            "Staff sign in with internal Lakemark credentials created by your Manager — no Google account required.",
                        )
                    } else {
                        s.get(
                            "auth_desc_google",
                            "Only the Manager / Admin account uses Google authentication to configure Lakemark. All other staff accounts are created internally without Google login.",
                        )
                    },
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF6E4D7B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CreamAlt)
                        .border(1.dp, WarmLine, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AuthMode.entries.forEach { tab ->
                        val selected = mode == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) DeepPlum else Color.Transparent)
                                .clickable(enabled = !isLoading) {
                                    mode = tab
                                    onClearError()
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("auth_mode_${tab.key}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = s.get(tab.stringKey, tab.fallbackLabel),
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selected) Color.White else DeepPlum,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFFFDF9),
                    border = BorderStroke(1.dp, WarmLine),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (mode == AuthMode.STAFF) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    onClearError()
                                },
                                label = { Text(s.get("auth_username_label", "Staff username"), fontFamily = font) },
                                placeholder = { Text(s.get("auth_username_hint", "e.g. kitchen1 or waiter_abel"), fontFamily = font) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = DeepPlum)
                                },
                                singleLine = true,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Next,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_username_input"),
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    onClearError()
                                },
                                label = { Text(s.get("auth_password_label", "Password"), fontFamily = font) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = DeepPlum)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showPassword) {
                                                s.get("auth_hide_password", "Hide password")
                                            } else {
                                                s.get("auth_show_password", "Show password")
                                            },
                                            tint = Color(0xFF6E4D7B),
                                        )
                                    }
                                },
                                singleLine = true,
                                enabled = !isLoading,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (!isLoading) onStaffSignIn(username, password)
                                    },
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_password_input"),
                            )

                            Button(
                                onClick = { onStaffSignIn(username, password) },
                                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldAccent,
                                    contentColor = DeepPlum,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("staff_sign_in_button"),
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = DeepPlum,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Badge,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = s.get("auth_staff_signin_btn", "Sign in to station"),
                                            fontFamily = font,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                        )
                                    }
                                }
                            }

                            Text(
                                text = s.get(
                                    "auth_staff_roles_hint",
                                    "For Reception, Kitchen, Waiter, Cashier & Delivery staff — created under Manage → Team.",
                                ),
                                fontFamily = font,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color(0xFF7D6687),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Button(
                                onClick = onGoogleSignIn,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White,
                                ),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .shadow(8.dp, RoundedCornerShape(999.dp))
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF561370), Color(0xFF3B0C4D)),
                                        ),
                                    )
                                    .testTag("google_sign_in_button"),
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "G",
                                                fontFamily = font,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = DeepPlum,
                                            )
                                        }
                                        Text(
                                            text = s.get("auth_google_btn", "Continue with Google"),
                                            fontFamily = font,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }

                            Text(
                                text = s.get(
                                    "auth_manager_google_note",
                                    "All other staff accounts are created and managed internally without Google login.",
                                ),
                                fontFamily = font,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color(0xFF7D6687),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StatusCancelledBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_error_banner"),
                    ) {
                        Text(
                            text = errorMessage,
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = StatusCancelledText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                TextButton(
                    onClick = onBackToSplash,
                    modifier = Modifier.testTag("auth_back_to_splash"),
                ) {
                    Text(
                        text = s.get("btn_back", "Back"),
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF6E4D7B),
                    )
                }
            }

            LoginWaveAccent(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        ),
                    )
                    .height(38.dp),
            )
        }
    }
}
