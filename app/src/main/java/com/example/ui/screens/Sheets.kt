package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.AppNotification
import com.example.data.model.BrandConfig
import com.example.data.model.OrderType
import com.example.data.model.UserRole
import com.example.fcm.FcmRegistrationState
import com.example.localization.AppLanguage
import com.example.localization.LocalAppStrings
import com.example.ui.components.AssignedRoleBadges
import com.example.ui.components.BrandMarkOrIcon
import com.example.ui.components.ImageSourcePickerDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatEtb
import com.example.ui.components.rememberMediaImagePicker
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.deriveBrandPalette
import com.example.ui.theme.lakemarkFont
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.relativeLuminance
import com.example.ui.viewmodel.LakemarkUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
private fun FullScreenSheetScaffold(
    title: String,
    onClose: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .testTag("sheet_close_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s.get("btn_back", "Back"),
                            tint = palette.text,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.6).sp,
                    color = palette.plum,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (trailingAction != null) {
                    trailingAction()
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }
        }
    }
}

@Composable
fun CartCheckoutSheet(
    uiState: LakemarkUiState,
    onClose: () -> Unit,
    onAddItem: (com.example.data.model.MenuItem) -> Unit = {},
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit = {},
    onChangePlace: () -> Unit,
    onPlaceOrder: (customerName: String, phoneOrLocation: String, note: String, onError: (String) -> Unit) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val place = uiState.selectedPlace ?: com.example.data.model.PlaceSelection(
        type = OrderType.DINE,
        location = "Table 1",
        label = "Table 1",
    )
    var phoneOrLocation by remember(place.location) { mutableStateOf("") }
    var customerName by remember(uiState.activeRole, uiState.currentUser) {
        mutableStateOf(
            if (uiState.activeRole == UserRole.CUSTOMER) uiState.currentUser?.name.orEmpty() else ""
        )
    }
    var specialNote by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }

    FullScreenSheetScaffold(
        title = s.get("cart_title", "Your Order"),
        onClose = onClose,
        trailingAction = {
            if (uiState.cart.isNotEmpty()) {
                Surface(
                    onClick = onClearCart,
                    shape = RoundedCornerShape(999.dp),
                    color = palette.alt,
                    modifier = Modifier.testTag("cart_clear_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = palette.text,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (s.language == AppLanguage.AM) "አጽዳ" else "Clear",
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = palette.text,
                        )
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.cart.forEach { cartItem ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.dishName(cartItem.menuItem),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = palette.text,
                            )
                            Text(
                                text = formatEtb(cartItem.menuItem.price * cartItem.quantity.toLong()),
                                fontFamily = font,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = palette.mut,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.alt)
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(palette.surface)
                                    .clickable { onRemoveItem(cartItem.menuItem.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "−",
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = palette.text,
                                )
                            }
                            Text(
                                text = cartItem.quantity.toString(),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = palette.text,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(palette.acc)
                                    .clickable { onAddItem(cartItem.menuItem) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+",
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = palette.onAcc,
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.label,
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = palette.text,
                        )
                        Text(
                            text = s.orderTypeLabel(place.type),
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = palette.mut,
                        )
                    }
                    Button(
                        onClick = onChangePlace,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.alt,
                            contentColor = palette.text,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp),
                    ) {
                        Text(
                            text = s.get("place_change", "Change"),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }

            if (place.type == OrderType.OUT) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = s.get("cart_takeaway_phone", "Phone number"),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = palette.text,
                    )
                    OutlinedTextField(
                        value = phoneOrLocation,
                        onValueChange = {
                            phoneOrLocation = it
                            validationError = ""
                        },
                        placeholder = { Text("0911 000 000", fontFamily = font) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.surface,
                            unfocusedContainerColor = palette.alt,
                            focusedBorderColor = palette.plum,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_input"),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s.get("cart_your_name", "Your name"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = palette.text,
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        validationError = ""
                    },
                    placeholder = { Text(s.get("cart_your_name", "Guest name"), fontFamily = font) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.alt,
                        focusedBorderColor = palette.plum,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_name_input"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s.get("cart_special_note", "Special requests (optional)"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = palette.text,
                )
                OutlinedTextField(
                    value = specialNote,
                    onValueChange = { specialNote = it },
                    placeholder = { Text(s.get("cart_special_note", "No onions, extra injera…"), fontFamily = font) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.alt,
                        focusedBorderColor = palette.plum,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_note_input"),
                )
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.get("cart_subtotal", "Subtotal"), fontFamily = font, color = palette.mut, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatEtb(uiState.cartSubtotal), fontFamily = font, color = palette.mut, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.get("cart_vat", "VAT (15%)"), fontFamily = font, color = palette.mut, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatEtb(uiState.cartVat), fontFamily = font, color = palette.mut, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.get("cart_total", "Total"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = palette.text)
                        Text(formatEtb(uiState.cartTotal), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = palette.text)
                    }
                }
            }

            if (validationError.isNotEmpty()) {
                Text(
                    text = validationError,
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = StatusCancelledText,
                )
            }

            Button(
                onClick = {
                    onPlaceOrder(customerName, phoneOrLocation, specialNote) { err ->
                        validationError = err
                    }
                },
                enabled = !uiState.isSubmittingOrder,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.acc,
                    contentColor = palette.onAcc,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("place_order_submit_button"),
            ) {
                if (uiState.isSubmittingOrder) {
                    CircularProgressIndicator(color = palette.onAcc, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(s.get("cart_place_order", "Place order"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun NotificationsSheet(
    uiState: LakemarkUiState,
    onClose: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit = {},
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    FullScreenSheetScaffold(title = s.get("notif_sheet_title", "Notifications"), onClose = onClose) {
        if (uiState.notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = s.get("notif_empty", "No notifications yet."),
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = palette.mut,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.notifications, key = { it.id.ifBlank { "${it.orderId}_${it.kind}_${it.message.hashCode()}" } }) { notif ->
                    val senderRole = UserRole.fromKey(notif.senderRole)
                    val senderPalette = uiState.pushBrandOverrides[notif.id]
                        ?: uiState.brands[senderRole]
                        ?: deriveBrandPalette(senderRole.defaultBrandConfig())
                    val timeText = notif.createdAt?.toDate()?.let { timeFormat.format(it) } ?: timeFormat.format(Date())
                    val isAppUpdate = notif.kind == "app_update"
                    val isDeepLinkable = isAppUpdate || notif.orderId.isNotBlank()

                    Surface(
                        onClick = { if (isDeepLinkable) onNotificationClick(notif) },
                        enabled = isDeepLinkable,
                        shape = RoundedCornerShape(20.dp),
                        color = senderPalette.bg,
                        border = BorderStroke(1.5.dp, senderPalette.plum),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_card_${notif.id.ifBlank { notif.orderId.ifBlank { notif.kind } }}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, senderPalette.acc, RoundedCornerShape(10.dp))
                                    .background(senderPalette.plum),
                                contentAlignment = Alignment.Center,
                            ) {
                                BrandMarkOrIcon(senderPalette, senderRole, 18.dp, senderPalette.onPlum)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notif.senderBrandName.ifBlank { s.roleName(senderRole) },
                                            fontFamily = font,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.5.sp,
                                            color = senderPalette.plum.copy(alpha = 0.85f),
                                        )
                                        Text(
                                            text = s.roleName(senderRole),
                                            fontFamily = font,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                            color = senderPalette.mut,
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (isAppUpdate) {
                                            Button(
                                                onClick = { onNotificationClick(notif) },
                                                shape = RoundedCornerShape(999.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = senderPalette.acc,
                                                    contentColor = senderPalette.onAcc,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .testTag("notification_update_${notif.id.ifBlank { "update" }}"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.SystemUpdate,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = s.get("notif_update_now", "Update Now"),
                                                    fontFamily = font,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                )
                                            }
                                        } else if (notif.orderId.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(senderPalette.acc.copy(alpha = 0.22f))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    text = s.get("home_action_open_orders", "View order"),
                                                    fontFamily = font,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp,
                                                    color = senderPalette.plum,
                                                )
                                            }
                                        }
                                        Text(
                                            text = timeText,
                                            fontFamily = font,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = senderPalette.plum.copy(alpha = 0.72f),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = notif.message,
                                    fontFamily = font,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = senderPalette.text,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    uiState: LakemarkUiState,
    hasNotificationPermission: Boolean,
    notificationSystemStatus: com.example.util.NotificationSystemStatus = com.example.util.NotificationSystemStatus(
        hasPermission = hasNotificationPermission,
        notificationsEnabledInSystem = hasNotificationPermission,
        kitchenChannelSoundEnabled = hasNotificationPermission,
        readyChannelSoundEnabled = hasNotificationPermission,
        broadcastChannelSoundEnabled = hasNotificationPermission,
    ),
    onClose: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchAdminRole: (UserRole) -> Unit,
    onOpenMyBrand: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTestAlertSound: () -> Unit = {},
    onToggleWaiterDuty: (String) -> Unit = {},
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onUploadProfilePhoto: (Uri) -> Unit = {},
    onRemoveProfilePhoto: () -> Unit = {},
    onUpdateProfileName: (String, () -> Unit) -> Unit = { _, _ -> },
    onOpenSaved: () -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onCameraPermissionDenied: () -> Unit = {},
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val prefs = uiState.preferences

    val currentUser = uiState.currentUser
    val photoUrl = currentUser?.photoUrl.orEmpty()
    val displayName = currentUser?.name?.ifBlank { null }
        ?: currentUser?.email?.substringBefore("@")?.ifBlank { null }
        ?: s.roleName(uiState.assignedRole)

    var editedName by remember(currentUser?.userId, currentUser?.name) {
        mutableStateOf(currentUser?.name.orEmpty())
    }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val mediaPicker = rememberMediaImagePicker(
        filePrefix = "lakemark_profile",
        onImageSelected = onUploadProfilePhoto,
        onCameraPermissionDenied = onCameraPermissionDenied,
    )

    if (showPhotoSourceDialog) {
        ImageSourcePickerDialog(
            title = s.get("profile_photo_title", "Profile Photo"),
            onDismiss = { showPhotoSourceDialog = false },
            onPickGallery = mediaPicker.openGallery,
            onPickCamera = mediaPicker.openCamera,
        )
    }

    FullScreenSheetScaffold(title = s.get("settings_title", "Profile & Settings"), onClose = onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Profile Identity & Photo Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(palette.gradTop, palette.gradBottom)))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        UserAvatar(
                            photoUrl = photoUrl,
                            name = displayName,
                            role = uiState.assignedRole,
                            palette = palette,
                            size = 64.dp,
                            isUploading = uiState.isUploadingMedia,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = displayName,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = palette.onPlum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val handleText = when {
                                currentUser?.username?.isNotBlank() == true -> "@${currentUser.username}"
                                currentUser?.email?.isNotBlank() == true -> currentUser.email
                                else -> ""
                            }
                            if (handleText.isNotBlank()) {
                                Text(
                                    text = handleText,
                                    fontFamily = font,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = palette.onPlum.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            AssignedRoleBadges(
                                assignedRole = uiState.assignedRole,
                                activePreviewRole = uiState.activeRole,
                                strings = s,
                                palette = palette.copy(
                                    plum = Color.White.copy(alpha = 0.2f),
                                    onPlum = palette.onPlum,
                                ),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { showPhotoSourceDialog = true },
                            enabled = !uiState.isUploadingMedia,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = palette.onPlum,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("profile_photo_edit_button"),
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (photoUrl.isNotBlank()) {
                                    s.get("profile_photo_change", "Change photo")
                                } else {
                                    s.get("profile_photo_add", "Add photo")
                                },
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (photoUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = onRemoveProfilePhoto,
                                enabled = !uiState.isUploadingMedia,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.onPlum),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("profile_photo_remove_button"),
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = s.get("profile_photo_remove", "Remove photo"), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = s.get("profile_photo_remove", "Remove photo"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    // Editable Full Name (synced across devices)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text(s.get("profile_name_label", "Full name"), fontFamily = font, fontSize = 11.5.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = palette.alt.copy(alpha = 0.5f),
                                    unfocusedContainerColor = palette.alt.copy(alpha = 0.5f),
                                    focusedBorderColor = palette.plum,
                                    unfocusedBorderColor = Color.Transparent,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_name_input"),
                            )
                            Button(
                                onClick = { onUpdateProfileName(editedName.trim()) {} },
                                enabled = editedName.trim().isNotEmpty() &&
                                    editedName.trim() != currentUser?.name.orEmpty().trim(),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("profile_name_save_button"),
                            ) {
                                Text(
                                    text = s.get("btn_save", "Save"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusCancelledBg,
                            contentColor = StatusCancelledText,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("sign_out_button"),
                    ) {
                        Text(s.get("btn_sign_out", "Sign out"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
                    }
                }
            }

            // App Release Update Banner when newer version is published
            if (uiState.hasUpdateAvailable) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.surface,
                    border = BorderStroke(1.5.dp, palette.acc),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.format(
                                    "notif_update_dialog_title",
                                    "ver" to (uiState.releaseConfig?.latestVersionName?.ifBlank { null } ?: "3.2.0"),
                                ),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = palette.text,
                            )
                            Text(
                                text = uiState.releaseConfig?.releaseNotes?.ifBlank { null }
                                    ?: s.get("notif_update_available", "App update available"),
                                fontFamily = font,
                                fontSize = 12.sp,
                                color = palette.mut,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onOpenUpdateDialog,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.acc, contentColor = palette.onAcc),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("settings_update_button"),
                        ) {
                            Text(s.get("notif_update_now", "Update Now"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                        }
                    }
                }
            }

            // Language Selector Card (EN / AM - Immediate & Persistent)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column {
                        Text(
                            text = s.get("settings_language_title", "Language / ቋንቋ"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = palette.text,
                        )
                        Text(
                            text = s.get("settings_language_sub", "Applies immediately across the app"),
                            fontFamily = font,
                            fontSize = 12.5.sp,
                            color = palette.mut,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            AppLanguage.EN to "EN · English",
                            AppLanguage.AM to "አማ · አማርኛ",
                        ).forEach { (lang, label) ->
                            val selected = uiState.language == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) palette.plum else palette.alt)
                                    .clickable { onSelectLanguage(lang) }
                                    .testTag("settings_lang_${lang.code}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = if (selected) palette.onPlum else palette.text,
                                )
                            }
                        }
                    }
                }
            }

            // Saved Dishes Shortcut Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.alt),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = palette.plum,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                text = s.format("settings_saved_shortcut", "n" to uiState.savedItems.size),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = palette.text,
                            )
                            Text(
                                text = s.get("saved_subtitle", "your bookmarked Lakemark dishes"),
                                fontFamily = font,
                                fontSize = 12.sp,
                                color = palette.mut,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Button(
                        onClick = onOpenSaved,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.alt, contentColor = palette.text),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("settings_open_saved_button"),
                    ) {
                        Text(s.get("saved_pill", "Saved"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                    }
                }
            }

            if (uiState.activeRole == UserRole.WAITER && uiState.currentUser != null) {
                val waiterId = uiState.currentUser.userId
                val cov = uiState.waiterCoverage[waiterId]
                val onDuty = cov?.onDuty ?: true
                val areasText = cov?.areaSet?.sortedBy { it.index }?.joinToString(", ") { s.waiterAreaLabel(it) }.orEmpty()
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.get("orders_on_duty", "On duty"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = palette.text)
                            Text(
                                text = if (areasText.isNotBlank()) areasText else s.get("home_sub_waiter", "Receive assigned floor orders"),
                                fontFamily = font,
                                fontSize = 12.5.sp,
                                color = palette.mut,
                            )
                        }
                        Switch(
                            checked = onDuty,
                            onCheckedChange = { onToggleWaiterDuty(waiterId) },
                            colors = SwitchDefaults.colors(checkedTrackColor = palette.acc, checkedThumbColor = Color.White),
                            modifier = Modifier.testTag("settings_waiter_duty_switch"),
                        )
                    }
                }
            }

            if (uiState.isAdminUser) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(s.get("settings_workspace_title", "Role workspace preview"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = palette.text)
                        Text(
                            text = s.get("settings_workspace_note", "Your assigned designation remains Manager."),
                            fontFamily = font,
                            fontSize = 12.sp,
                            color = palette.mut,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            UserRole.entries.forEach { role ->
                                val selected = role == uiState.activeRole
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (selected) palette.plum else palette.alt)
                                        .clickable { onSwitchAdminRole(role) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("switch_role_${role.key}"),
                                ) {
                                    Text(
                                        text = s.roleName(role),
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (selected) palette.onPlum else palette.text,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!uiState.isGoogleCustomerSession) {
                // My brand card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(s.get("settings_my_brand", "My brand"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = palette.text)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("60% · 30% · 10%", fontFamily = font, fontSize = 12.5.sp, color = palette.mut)
                                SwatchDotsRow(palette)
                            }
                        }
                        Button(
                            onClick = onOpenMyBrand,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.alt, contentColor = palette.text),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("customize_my_brand_button"),
                        ) {
                            Text(
                                text = if (s.language == AppLanguage.AM) "አስተካክል" else "Customize",
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // Sound, Vibration & Volume card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.get("settings_sound_title", "Alert sound"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = palette.text)
                        }
                        Switch(
                            checked = prefs.soundEnabled,
                            onCheckedChange = onToggleSound,
                            colors = SwitchDefaults.colors(checkedTrackColor = palette.acc, checkedThumbColor = Color.White),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.get("settings_vibration_title", "Vibration"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = palette.text)
                        }
                        Switch(
                            checked = prefs.vibrationEnabled,
                            onCheckedChange = onToggleVibration,
                            colors = SwitchDefaults.colors(checkedTrackColor = palette.acc, checkedThumbColor = Color.White),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.get("settings_volume_title", "Alert volume"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = palette.text)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("${(prefs.volume * 100).roundToInt()}%", fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.mut)
                                OutlinedButton(
                                    onClick = onTestAlertSound,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, palette.plum.copy(alpha = 0.45f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("settings_test_sound_button"),
                                ) {
                                    Text(
                                        text = s.get("settings_test_sound", "Test sound"),
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = palette.plum,
                                    )
                                }
                            }
                        }
                        Slider(
                            value = prefs.volume,
                            onValueChange = onVolumeChange,
                            colors = SliderDefaults.colors(thumbColor = palette.plum, activeTrackColor = palette.plum),
                        )
                        if (notificationSystemStatus.deviceMediaMuted) {
                            Text(
                                text = s.get(
                                    "settings_device_vol_muted",
                                    "Phone media volume is muted (0%) — raise device volume to hear alerts.",
                                ),
                                fontFamily = font,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = StatusCancelledText,
                                modifier = Modifier.testTag("settings_muted_volume_warning"),
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val fcm = uiState.fcmRegistrationInfo
                val fcmStatusLabel = when (val st = fcm.state) {
                    is FcmRegistrationState.Active ->
                        if (st.isFirestoreSynced) "FCM · Cloud synced" else "FCM · Role topics"
                    FcmRegistrationState.Syncing -> "Syncing FCM…"
                    FcmRegistrationState.PermissionDenied -> "Permission required"
                    is FcmRegistrationState.Error -> st.message
                    FcmRegistrationState.Idle -> "Connecting…"
                }
                val channelLine = if (notificationSystemStatus.allChannelsSoundEnabled) {
                    s.get("settings_notif_channels_ok", "All alert channels active with sound")
                } else {
                    s.get("settings_notif_channels_muted", "Channel sound muted in Android settings")
                }
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.get("settings_notif_perm_title", "System notifications"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = palette.text)
                            Text(
                                text = if (hasNotificationPermission) {
                                    "${s.get("settings_notif_enabled", "Enabled")} · $fcmStatusLabel"
                                } else {
                                    fcmStatusLabel
                                },
                                fontFamily = font,
                                fontSize = 12.5.sp,
                                color = palette.mut,
                            )
                            Text(
                                text = channelLine,
                                fontFamily = font,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = if (notificationSystemStatus.allChannelsSoundEnabled) palette.mut else StatusCancelledText,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (!hasNotificationPermission) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.acc, contentColor = palette.onAcc),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("settings_push_permission_button"),
                            ) {
                                Text(
                                    text = s.get("settings_notif_enable_btn", "Enable"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onOpenNotificationSettings,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, palette.plum.copy(alpha = 0.35f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("settings_open_system_notif_button"),
                            ) {
                                Text(
                                    text = s.get("settings_notif_open_sys", "App settings"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = palette.plum,
                                )
                            }
                        }
                    }
                    Text(
                        text = "lakemark_role_${uiState.activeRole.key} · ${fcm.deviceId.ifBlank { "pending" }}",
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = palette.mut,
                    )
                }
            }

            Text(
                text = s.format("app_version_footer", "ver" to BuildConfig.VERSION_NAME),
                fontFamily = font,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = palette.mut,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("settings_app_version"),
            )
        }
    }
}

@Composable
fun BrandEditorSheet(
    role: UserRole,
    currentConfig: BrandConfig,
    onClose: () -> Unit,
    onReset: (UserRole) -> Unit,
    onSave: (BrandConfig, (String) -> Unit) -> Unit,
) {
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    var primaryHex by remember(role, currentConfig) { mutableStateOf(currentConfig.primaryHex) }
    var secondaryHex by remember(role, currentConfig) { mutableStateOf(currentConfig.secondaryHex) }
    var accentHex by remember(role, currentConfig) { mutableStateOf(currentConfig.accentHex) }
    var brandName by remember(role, currentConfig) { mutableStateOf(currentConfig.brandName) }
    var mark by remember(role, currentConfig) { mutableStateOf(currentConfig.mark) }
    var errorMsg by remember { mutableStateOf("") }

    val previewConfig = BrandConfig(
        roleKey = role.key,
        primaryHex = primaryHex,
        secondaryHex = secondaryHex,
        accentHex = accentHex,
        brandName = brandName.ifBlank { s.roleName(role) }.take(18),
        mark = mark.take(2),
    )
    val preview = deriveBrandPalette(previewConfig)

    val lightSwatches = listOf("#FAF3E6", "#D6EEEA", "#FBE2CE", "#DCE7FB", "#F5E9C4", "#F8DCE6", "#DBF2E2")
    val darkSwatches = listOf("#3D0F4F", "#124A47", "#7A2B0A", "#1B3A86", "#5A4208", "#7E1743", "#14502B")
    val accentSwatches = listOf("#04C353", "#F2A31B", "#F26B1D", "#2BB3E8", "#D9A21B", "#F0436B", "#1FB55A")

    FullScreenSheetScaffold(title = "${s.get("team_sub_brands", "Brand")} · ${preview.brandName}", onClose = onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Live 60/30/10 Preview Card (.bpv)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = preview.bg,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                        Box(modifier = Modifier.weight(6f).height(14.dp).background(preview.bg))
                        Box(modifier = Modifier.weight(3f).height(14.dp).background(preview.plum))
                        Box(modifier = Modifier.weight(1f).height(14.dp).background(preview.acc))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(preview.plum)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(preview.brandName, fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = preview.onPlum)
                        Text("30%", fontFamily = font, fontSize = 12.5.sp, color = preview.onPlum.copy(alpha = 0.8f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("60%", fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = preview.text)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(preview.acc)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("10%", fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = preview.onAcc)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = preview.bg,
                        border = BorderStroke(1.5.dp, preview.plum),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 14.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, preview.acc, RoundedCornerShape(10.dp))
                                    .background(preview.plum),
                                contentAlignment = Alignment.Center,
                            ) {
                                BrandMarkOrIcon(preview, role, 17.dp, preview.onPlum)
                            }
                            Column {
                                Text(preview.brandName, fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = preview.text)
                                Text(
                                    text = s.format("notif_order_ready", "num" to 1005, "loc" to "Table 6"),
                                    fontFamily = font,
                                    fontSize = 12.5.sp,
                                    color = preview.text,
                                )
                            }
                        }
                    }
                }
            }

            // Color & Text Controls
            val activePalette = LocalBrandPalette.current
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = activePalette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrandColorField(
                        title = "Primary · 60%",
                        subtitle = "60%",
                        hexValue = primaryHex,
                        swatches = lightSwatches,
                        onHexChange = {
                            primaryHex = it
                            errorMsg = ""
                        },
                    )
                    BrandColorField(
                        title = "Secondary · 30%",
                        subtitle = "30%",
                        hexValue = secondaryHex,
                        swatches = darkSwatches,
                        onHexChange = {
                            secondaryHex = it
                            errorMsg = ""
                        },
                    )
                    BrandColorField(
                        title = "Accent · 10%",
                        subtitle = "10%",
                        hexValue = accentHex,
                        swatches = accentSwatches,
                        onHexChange = {
                            accentHex = it
                            errorMsg = ""
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Brand name", fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = activePalette.text)
                            Text("1–18 chars", fontFamily = font, fontSize = 12.sp, color = activePalette.mut)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = brandName,
                            onValueChange = { if (it.length <= 18) brandName = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(150.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mark", fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = activePalette.text)
                            Text("1–2 chars", fontFamily = font, fontSize = 12.sp, color = activePalette.mut)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = mark,
                            onValueChange = { if (it.length <= 2) mark = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(110.dp),
                        )
                    }
                }
            }

            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = StatusCancelledText)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onReset(role) },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activePalette.alt, contentColor = activePalette.text),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("brand_reset_button"),
                ) {
                    Text(s.get("btn_reset", "Reset"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        val pColor = parseHexColor(primaryHex, Color.Black)
                        val sColor = parseHexColor(secondaryHex, Color.White)
                        when {
                            relativeLuminance(pColor) < 0.55 ->
                                errorMsg = "Primary must be a light colour so text stays readable."
                            relativeLuminance(sColor) > 0.22 ->
                                errorMsg = "Secondary must be a dark colour."
                            brandName.trim().isEmpty() ->
                                errorMsg = "Brand name cannot be empty."
                            else -> onSave(previewConfig) { err -> errorMsg = err }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activePalette.acc, contentColor = activePalette.onAcc),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("brand_save_button"),
                ) {
                    Text(s.get("btn_save", "Save brand"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BrandColorField(
    title: String,
    subtitle: String,
    hexValue: String,
    swatches: List<String>,
    onHexChange: (String) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val font = lakemarkFont()
    val parsedColor = parseHexColor(hexValue, palette.alt)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = palette.text)
                Text(subtitle, fontFamily = font, fontSize = 12.sp, color = palette.mut)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(parsedColor)
                        .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                )
                OutlinedTextField(
                    value = hexValue,
                    onValueChange = { if (it.length <= 7) onHexChange(it) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(106.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            swatches.forEach { hex ->
                val c = parseHexColor(hex)
                val selected = hex.equals(hexValue, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) palette.plum else Color.Black.copy(alpha = 0.18f),
                            shape = CircleShape,
                        )
                        .clickable { onHexChange(hex) },
                )
            }
        }
    }
}
