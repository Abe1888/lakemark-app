package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppTab
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PlaceSelection
import com.example.data.model.StaffUser
import com.example.data.model.UserRole
import com.example.data.model.WaiterCoverage
import com.example.localization.LocalAppStrings
import com.example.ui.theme.BadgeCoral
import com.example.ui.theme.BadgeCoralText
import com.example.ui.theme.BrandPalette
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.ManropeFamily
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.StatusNewBg
import com.example.ui.theme.StatusNewText
import com.example.ui.theme.StatusReadyBg
import com.example.ui.theme.StatusReadyText
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.ToastBanner
import java.text.NumberFormat
import java.util.Locale

fun formatEtb(amount: Long): String {
    val nf = NumberFormat.getIntegerInstance(Locale.US)
    return "ETB ${nf.format(amount)}"
}

fun roleIconVector(role: UserRole): ImageVector = when (role) {
    UserRole.MANAGER -> Icons.Filled.Shield
    UserRole.RECEPTION -> Icons.Filled.Notifications
    UserRole.KITCHEN -> Icons.Filled.LocalFireDepartment
    UserRole.WAITER -> Icons.Filled.LocalCafe
    UserRole.CASHIER -> Icons.Filled.AccountBalanceWallet
    UserRole.DELIVERY -> Icons.Filled.TwoWheeler
    UserRole.CUSTOMER -> Icons.Filled.Person
}

fun tabIconVector(tab: AppTab): ImageVector = when (tab) {
    AppTab.HOME -> Icons.Filled.Home
    AppTab.FLOOR -> Icons.Filled.GridView
    AppTab.MENU -> Icons.Filled.Menu
    AppTab.SAVED -> Icons.Filled.Bookmark
    AppTab.ORDERS -> Icons.AutoMirrored.Filled.FormatListBulleted
    AppTab.KITCHEN -> Icons.Filled.LocalFireDepartment
    AppTab.MANAGE -> Icons.Filled.Group
}

fun categoryIconVector(category: String): ImageVector = when (category) {
    "All" -> Icons.Filled.GridView
    "Saved" -> Icons.Filled.Bookmark
    "Classics" -> Icons.Filled.RoomService
    "Lake Tana" -> Icons.Filled.SetMeal
    "Grill" -> Icons.Filled.LocalFireDepartment
    "Breakfast" -> Icons.Filled.WbSunny
    "Drinks" -> Icons.Filled.LocalCafe
    else -> Icons.Filled.Menu
}

@Composable
fun BrandMarkOrIcon(
    palette: BrandPalette,
    role: UserRole = palette.roleKey,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    tint: Color = palette.onPlum,
) {
    if (palette.mark.isNotBlank()) {
        Text(
            text = palette.mark.uppercase(),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (iconSize.value * 0.78f).sp,
            color = tint,
            letterSpacing = (-0.5).sp,
        )
    } else {
        Icon(
            imageVector = roleIconVector(role),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun LakemarkTopBar(
    unreadCount: Int,
    activeRole: UserRole,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    currentUser: StaffUser? = null,
    assignedRole: UserRole = activeRole,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val displayName = currentUser?.name?.trim()?.ifBlank { null } ?: s.roleName(assignedRole)
    val displayRole = s.roleName(assignedRole)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .height(58.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Image(
                    painter = painterResource(R.drawable.lakemark_logo),
                    contentDescription = s.get("app_brand_name", "LAKEMARK"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                )
                Column {
                    Text(
                        text = s.get("app_brand_name", "LAKEMARK"),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.6.sp,
                        color = palette.text,
                        maxLines = 1,
                    )
                    Text(
                        text = s.get("brand_location", "Bahir Dar"),
                        fontFamily = font,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        color = palette.mut,
                        maxLines = 1,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        onClick = onNotificationsClick,
                        shape = CircleShape,
                        color = palette.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .testTag("notifications_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = s.get("notif_sheet_title", "Notifications"),
                                tint = palette.text,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(BadgeCoral),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = unreadCount.coerceAtMost(99).toString(),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = BadgeCoralText,
                            )
                        }
                    }
                }

                Surface(
                    onClick = onSettingsClick,
                    shape = RoundedCornerShape(999.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.line),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .height(44.dp)
                        .widthIn(max = 175.dp)
                        .testTag("settings_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 5.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        UserAvatar(
                            photoUrl = currentUser?.photoUrl.orEmpty(),
                            name = displayName,
                            role = assignedRole,
                            palette = palette,
                            size = 32.dp,
                        )
                        Column {
                            Text(
                                text = displayName,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                lineHeight = 13.sp,
                                color = palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = displayRole,
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = palette.plum,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = palette.line.copy(alpha = 0.55f),
        )
    }
}

@Composable
fun FloatingPillNavBar(
    tabs: List<AppTab>,
    selectedTab: AppTab,
    newOrdersCount: Int,
    confirmedKitchenCount: Int,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .height(64.dp),
        shape = RoundedCornerShape(999.dp),
        color = palette.plum,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val label = s.tabLabel(tab)
                val badge = when (tab) {
                    AppTab.ORDERS -> newOrdersCount
                    AppTab.KITCHEN -> confirmedKitchenCount
                    else -> 0
                }
                val itemWeight = if (isSelected) 1.18f else 1f
                Box(
                    modifier = Modifier
                        .weight(itemWeight)
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) palette.acc else Color.Transparent)
                        .clickable { onSelectTab(tab) }
                        .testTag("nav_${tab.key}")
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            imageVector = tabIconVector(tab),
                            contentDescription = null,
                            tint = if (isSelected) palette.onAcc else palette.onPlum.copy(alpha = 0.78f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            color = if (isSelected) palette.onAcc else palette.onPlum.copy(alpha = 0.78f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!isSelected && badge > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 6.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(BadgeCoral),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = badge.coerceAtMost(99).toString(),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = BadgeCoralText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroSection(
    pillText: String,
    bigNumber: Int,
    unitLabel: String,
    subtitle: String,
    tags: List<String> = emptyList(),
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
) {
    val palette = LocalBrandPalette.current
    val font = lakemarkFont()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.plum)
                .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            Text(
                text = pillText,
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = palette.onPlum,
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = bigNumber.toString(),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 68.sp,
                lineHeight = 68.sp,
                letterSpacing = (-2.8).sp,
                color = palette.plum,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = unitLabel,
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = palette.plum,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        Text(
            text = subtitle,
            fontFamily = font,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = palette.text,
            textAlign = TextAlign.Center,
        )

        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(palette.alt)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = tag,
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = palette.text,
                        )
                    }
                }
            }
        }

        if (primaryAction != null || secondaryAction != null) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                primaryAction?.let { (label, onClick) ->
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.acc,
                            contentColor = palette.onAcc,
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hero_primary_cta"),
                    ) {
                        Text(
                            text = label,
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                    }
                }
                secondaryAction?.let { (label, onClick) ->
                    Text(
                        text = label,
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = palette.plum,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = onClick)
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                            .testTag("hero_secondary_cta"),
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: OrderStatus) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val (bg, fg) = when (status) {
        OrderStatus.NEW -> StatusNewBg to StatusNewText
        OrderStatus.CONFIRMED, OrderStatus.PREPARING -> palette.alt to palette.text
        OrderStatus.READY, OrderStatus.DONE -> StatusReadyBg to StatusReadyText
        OrderStatus.CANCELLED -> StatusCancelledBg to StatusCancelledText
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = s.statusLabel(status),
            fontFamily = font,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.5.sp,
            color = fg,
        )
    }
}

@Composable
fun SelectedPlaceBanner(
    selectedPlace: PlaceSelection?,
    onChangeClick: () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.plum)
            .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selectedPlace != null) {
                "${s.get("place_selected_for", "Selected place")}: ${selectedPlace.label}"
            } else {
                s.get("menu_choose_spot", "Choose a room or table to begin")
            },
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = palette.onPlum,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(999.dp))
                .background(palette.onPlum.copy(alpha = 0.14f))
                .clickable(onClick = onChangeClick)
                .padding(horizontal = 16.dp, vertical = 9.dp)
                .testTag("change_place_button"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (selectedPlace != null) s.get("place_change", "Change") else s.get("nav_floor", "Floor"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                color = palette.onPlum,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactOrderRow(
    order: Order,
    activeRole: UserRole,
    isHighlighted: Boolean,
    waiterName: String = "",
    canAssignWaiter: Boolean = false,
    onAssignWaiterClick: () -> Unit = {},
    onAdvance: (OrderStatus) -> Unit,
    onCancelRequest: () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val status = order.statusEnum
    val (tokenBg, tokenFg) = when (status) {
        OrderStatus.NEW -> StatusNewBg to StatusNewText
        OrderStatus.CONFIRMED, OrderStatus.PREPARING -> palette.plum2 to palette.onPlum
        OrderStatus.READY -> palette.acc to palette.onAcc
        OrderStatus.DONE -> palette.alt to palette.plum
        OrderStatus.CANCELLED -> StatusCancelledBg to StatusCancelledText
    }
    val actions = if (activeRole == UserRole.KITCHEN && (status == OrderStatus.NEW || status == OrderStatus.CONFIRMED)) {
        listOf(OrderStatus.CANCELLED, OrderStatus.PREPARING)
    } else {
        status.nextStatuses.filter { it in activeRole.directActions }
    }
    val hasWaiterArea = order.areaForOrder() != null
    val showAssignButton = canAssignWaiter && hasWaiterArea && !status.isTerminal
    val waiterSuffix = when {
        waiterName.isNotBlank() -> " · $waiterName"
        hasWaiterArea && !status.isTerminal -> ""
        else -> ""
    }
    val reasonSuffix = if (order.cancelReason.isNotBlank()) " · ${s.statusLabel(OrderStatus.CANCELLED)}: ${order.cancelReason}" else ""

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        shadowElevation = 2.dp,
        border = if (isHighlighted) BorderStroke(2.dp, palette.acc) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_row_${order.orderNumber}"),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tokenBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = order.ticketToken,
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = tokenFg,
                        maxLines = 1,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${order.orderNumber} · ${order.customerName}",
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = order.items.joinToString(", ") { "${it.quantity}× ${it.name}" } + reasonSuffix + waiterSuffix,
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = palette.mut,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = formatEtb(order.total),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = palette.text,
                    )
                    StatusPill(status)
                }
            }

            if (actions.isNotEmpty() || showAssignButton) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    actions.forEach { target ->
                        val isCancel = target == OrderStatus.CANCELLED
                        val label = if (activeRole == UserRole.KITCHEN && target == OrderStatus.PREPARING) {
                            s.get("dialog_accept", "Accept")
                        } else {
                            s.statusActionLabel(target)
                        }
                        Button(
                            onClick = {
                                if (isCancel) onCancelRequest() else onAdvance(target)
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCancel) StatusCancelledBg else palette.acc,
                                contentColor = if (isCancel) StatusCancelledText else palette.onAcc,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Text(
                                text = label,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    if (showAssignButton) {
                        Button(
                            onClick = onAssignWaiterClick,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.alt,
                                contentColor = palette.text,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("assign_waiter_${order.orderNumber}"),
                        ) {
                            Text(
                                text = s.get("orders_assign_waiter", "Assign"),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderTicketCard(
    order: Order,
    activeRole: UserRole,
    isHighlighted: Boolean,
    waiterName: String = "",
    onAdvance: (OrderStatus) -> Unit,
    onCancelRequest: () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val status = order.statusEnum
    val actions = if (activeRole == UserRole.KITCHEN && (status == OrderStatus.NEW || status == OrderStatus.CONFIRMED)) {
        listOf(OrderStatus.CANCELLED, OrderStatus.PREPARING)
    } else {
        status.nextStatuses.filter { it in activeRole.directActions }
    }
    val waiterSuffix = if (waiterName.isNotBlank()) " · $waiterName" else ""

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        shadowElevation = 2.dp,
        border = if (isHighlighted) BorderStroke(2.dp, palette.acc) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kitchen_ticket_${order.orderNumber}"),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#${order.orderNumber} · ${s.orderTypeLabel(order.typeEnum)}",
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = palette.text,
                )
                StatusPill(status)
            }
            Text(
                text = "${order.location} · ${order.customerName}$waiterSuffix",
                fontFamily = font,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = palette.mut,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                order.items.forEach { item ->
                    val noteSuffix = if (item.note.isNotBlank()) " — ${item.note}" else ""
                    Text(
                        text = "• ${item.quantity}× ${item.name}$noteSuffix",
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = palette.text,
                    )
                }
            }
            if (status != OrderStatus.CANCELLED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(palette.alt),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(status.progressFraction.coerceAtLeast(0.08f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette.acc),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatEtb(order.total),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = palette.text,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    actions.forEach { target ->
                        val isCancel = target == OrderStatus.CANCELLED
                        val label = if (activeRole == UserRole.KITCHEN && target == OrderStatus.PREPARING) {
                            s.get("dialog_accept", "Accept")
                        } else {
                            s.statusActionLabel(target)
                        }
                        Button(
                            onClick = { if (isCancel) onCancelRequest() else onAdvance(target) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCancel) StatusCancelledBg else palette.acc,
                                contentColor = if (isCancel) StatusCancelledText else palette.onAcc,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Text(label, fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrandedToastBanner(
    toast: ToastBanner?,
    senderPalette: BrandPalette?,
    onDismiss: () -> Unit,
    onOpenDeepLink: ((com.example.fcm.PushDeepLink) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        if (toast != null && senderPalette != null) {
            Surface(
                onClick = {
                    val link = toast.deepLink
                    onDismiss()
                    if (link != null && onOpenDeepLink != null) {
                        onOpenDeepLink(link)
                    }
                },
                shape = RoundedCornerShape(22.dp),
                color = senderPalette.bg,
                border = BorderStroke(1.5.dp, senderPalette.plum),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("toast_banner"),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
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
                        BrandMarkOrIcon(senderPalette, toast.senderRole, 18.dp, senderPalette.onPlum)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = s.get("app_brand_name", "LAKEMARK"),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.sp,
                                color = senderPalette.plum.copy(alpha = 0.78f),
                            )
                            Text(
                                text = if (toast.deepLink != null) "▸" else "•",
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = senderPalette.plum.copy(alpha = 0.78f),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(senderPalette.acc),
                            )
                            Text(
                                text = toast.brandName,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = senderPalette.text,
                            )
                        }
                        Text(
                            text = toast.message,
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = senderPalette.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitchenOrderAlertDialog(
    order: Order,
    waitingCount: Int,
    senderPalette: BrandPalette,
    isCancellingMode: Boolean,
    onOpenCancelMode: () -> Unit,
    onCancelBack: () -> Unit,
    onConfirmCancel: (String) -> Unit,
    onAccept: () -> Unit,
) {
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    var cancelReason by remember(order.id) { mutableStateOf("") }
    var cancelError by remember(order.id) { mutableStateOf("") }
    val reasons = listOf(
        s.get("cancel_reason_out_of_stock", "Out of stock"),
        s.get("cancel_reason_kitchen_busy", "Kitchen at capacity"),
        s.get("cancel_reason_guest_cancelled", "Guest cancelled"),
    )
    val notes = order.items.mapNotNull { it.note.takeIf { n -> n.isNotBlank() } }.distinct()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = senderPalette.bg,
            border = BorderStroke(3.dp, senderPalette.plum),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 520.dp),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(senderPalette.plum)
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(senderPalette.acc),
                            contentAlignment = Alignment.Center,
                        ) {
                            BrandMarkOrIcon(senderPalette, order.createdByRoleEnum, 16.dp, senderPalette.onAcc)
                        }
                        Text(
                            text = if (isCancellingMode) {
                                "${s.get("action_cancel", "Cancel")} #${order.orderNumber}"
                            } else {
                                s.format("dialog_new_order_header", "brand" to senderPalette.brandName)
                            },
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = senderPalette.onPlum,
                        )
                    }
                    Text(
                        text = if (waitingCount > 1) {
                            s.format("dialog_waiting_more", "n" to (waitingCount - 1))
                        } else {
                            s.orderTypeLabel(order.typeEnum)
                        },
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = senderPalette.onPlum.copy(alpha = 0.85f),
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isCancellingMode) {
                        Text(
                            text = s.get("dialog_cancel_reason_prompt", "Choose or type a reason."),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = senderPalette.text,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            reasons.forEach { reason ->
                                Button(
                                    onClick = {
                                        cancelReason = reason
                                        cancelError = ""
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = senderPalette.alt,
                                        contentColor = senderPalette.text,
                                    ),
                                ) {
                                    Text(reason, fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = {
                                cancelReason = it
                                cancelError = ""
                            },
                            placeholder = { Text(s.get("dialog_cancel_reason_hint", "Reason for cancellation")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = senderPalette.plum,
                                unfocusedBorderColor = senderPalette.line,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (cancelError.isNotEmpty()) {
                            Text(cancelError, color = StatusCancelledText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "#${order.orderNumber}",
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 72.sp,
                            color = senderPalette.plum,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${order.location} · ${order.customerName}",
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = senderPalette.mut,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        order.items.forEach { item ->
                            Text(
                                text = "${item.quantity}× ${item.name}",
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = senderPalette.text,
                            )
                        }
                        notes.forEach { note ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(StatusNewBg)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = s.format("dialog_note_prefix", "note" to note),
                                    fontFamily = font,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = StatusNewText,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isCancellingMode) {
                        Button(
                            onClick = onCancelBack,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = senderPalette.alt,
                                contentColor = senderPalette.text,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                        ) {
                            Text(s.get("btn_back", "Back"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Button(
                            onClick = {
                                if (cancelReason.trim().isEmpty()) {
                                    cancelError = s.get("dialog_cancel_reason_prompt", "Choose or type a reason.")
                                } else {
                                    onConfirmCancel(cancelReason.trim())
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusCancelledBg,
                                contentColor = StatusCancelledText,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                        ) {
                            Text(s.get("btn_confirm", "Confirm"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    } else {
                        Button(
                            onClick = onOpenCancelMode,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusCancelledBg,
                                contentColor = StatusCancelledText,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp),
                        ) {
                            Text(s.get("btn_cancel", "Cancel"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                        Button(
                            onClick = onAccept,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = senderPalette.acc,
                                contentColor = senderPalette.onAcc,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp),
                        ) {
                            Text(s.get("dialog_accept", "Accept"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickupReadyAlertDialog(
    order: Order,
    readyCount: Int,
    activeRole: UserRole,
    senderPalette: BrandPalette,
    onLater: () -> Unit,
    onConfirmPickup: () -> Unit,
) {
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = senderPalette.bg,
            border = BorderStroke(3.dp, senderPalette.plum),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 520.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(senderPalette.acc)
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(senderPalette.plum),
                            contentAlignment = Alignment.Center,
                        ) {
                            BrandMarkOrIcon(senderPalette, order.lastUpdatedByRoleEnum, 16.dp, senderPalette.onPlum)
                        }
                        Text(
                            text = s.format("dialog_order_ready_header", "brand" to senderPalette.brandName),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = senderPalette.onAcc,
                        )
                    }
                    Text(
                        text = if (readyCount > 1) {
                            s.format("dialog_ready_more", "n" to (readyCount - 1))
                        } else {
                            s.orderTypeLabel(order.typeEnum)
                        },
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = senderPalette.onAcc.copy(alpha = 0.85f),
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "#${order.orderNumber}",
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 72.sp,
                        color = senderPalette.plum,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${order.location} · ${order.customerName}",
                        fontFamily = font,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = senderPalette.mut,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    order.items.forEach { item ->
                        Text(
                            text = "${item.quantity}× ${item.name}",
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = senderPalette.text,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onLater,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = senderPalette.alt,
                            contentColor = senderPalette.text,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                    ) {
                        Text(s.get("btn_later", "Later"), fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    Button(
                        onClick = if (activeRole == UserRole.CASHIER) onLater else onConfirmPickup,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = senderPalette.acc,
                            contentColor = senderPalette.onAcc,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                    ) {
                        Text(
                            text = if (activeRole == UserRole.CASHIER) s.get("btn_got_it", "Got it") else s.get("btn_confirm", "Confirm"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaiterAssignDialog(
    order: Order,
    waiters: List<StaffUser>,
    waiterCoverage: Map<String, WaiterCoverage>,
    allOrders: List<Order>,
    assignedWaiterName: String,
    onDismiss: () -> Unit,
    onAutoAssign: () -> Unit,
    onAssignWaiter: (String) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val areaLabel = order.areaForOrder()?.let { s.waiterAreaLabel(it) }.orEmpty()
    val subtitleSuffix = if (assignedWaiterName.isNotBlank()) " · $assignedWaiterName" else ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.bg,
            border = BorderStroke(2.dp, palette.plum),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 520.dp),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.plum)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.format("orders_assign_title", "num" to order.orderNumber),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = palette.onPlum,
                    )
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = s.get("btn_close", "Close"),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            color = palette.onPlum,
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${order.location} · ${order.customerName} · $areaLabel$subtitleSuffix",
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = palette.mut,
                    )

                    Button(
                        onClick = onAutoAssign,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.acc,
                            contentColor = palette.onAcc,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auto_assign_waiter_button"),
                    ) {
                        Text(
                            text = s.get("orders_auto_assign", "Auto-assign nearest"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                    }

                    waiters.forEach { waiter ->
                        val cov = waiterCoverage[waiter.userId] ?: WaiterCoverage(waiter.userId)
                        val isLive = waiter.active && cov.onDuty
                        val areasText = cov.areaSet.sortedBy { it.index }.joinToString(", ") { s.waiterAreaLabel(it) }
                            .ifBlank { s.get("orders_no_area", "No area") }
                        val activeLoad = allOrders.count {
                            it.assignedWaiterId == waiter.userId && !it.statusEnum.isTerminal
                        }
                        val isAssigned = order.assignedWaiterId == waiter.userId

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                UserAvatar(
                                    photoUrl = waiter.photoUrl,
                                    name = waiter.name,
                                    role = waiter.userRole,
                                    palette = palette,
                                    size = 36.dp,
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp),
                                ) {
                                    Text(
                                        text = "${waiter.name} · ${s.roleName(waiter.userRole)}",
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = palette.text,
                                    )
                                    Text(
                                        text = "$areasText · ${if (isLive) s.get("orders_on_duty", "On duty") else s.get("orders_off_duty", "Off duty")} · ${s.format("orders_active_load", "n" to activeLoad)}",
                                        fontFamily = font,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = palette.mut,
                                    )
                                }
                                when {
                                    isAssigned -> {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(palette.alt)
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                        ) {
                                            Text(
                                                text = s.get("orders_assigned_badge", "Assigned"),
                                                fontFamily = font,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.5.sp,
                                                color = palette.text,
                                            )
                                        }
                                    }
                                    isLive -> {
                                        Button(
                                            onClick = { onAssignWaiter(waiter.userId) },
                                            shape = RoundedCornerShape(999.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = palette.acc,
                                                contentColor = palette.onAcc,
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .height(38.dp)
                                                .testTag("assign_to_${waiter.userId}"),
                                        ) {
                                            Text(
                                                text = s.get("orders_assign_waiter", "Assign"),
                                                fontFamily = font,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.5.sp,
                                            )
                                        }
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(palette.alt)
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                        ) {
                                            Text(
                                                text = s.get("orders_off_duty", "Off duty"),
                                                fontFamily = font,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = palette.mut,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
