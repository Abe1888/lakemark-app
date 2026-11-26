package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AppTab
import com.example.data.model.FloorZone
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.UserRole
import com.example.localization.LocalAppStrings
import com.example.ui.components.CircularServedRing
import com.example.ui.components.HeroSection
import com.example.ui.components.MiniSevenDayBarChart
import com.example.ui.components.StatusPill
import com.example.ui.components.formatEtb
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class RoleHomeMetrics(
    val bigNumber: Int,
    val unitLabel: String,
    val subtitle: String,
    val tags: List<String>,
    val stackRows: List<Pair<String, Int>>,
)

@Composable
fun HomeScreen(
    uiState: LakemarkUiState,
    onNavigateTab: (AppTab) -> Unit,
    onOpenArea: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val orders = uiState.orders
    val role = uiState.activeRole

    val todayStartMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val nNew = orders.count { it.statusEnum == OrderStatus.NEW }
    val nConfirmed = orders.count { it.statusEnum == OrderStatus.CONFIRMED }
    val nPreparing = orders.count { it.statusEnum == OrderStatus.PREPARING }
    val nReady = orders.count { it.statusEnum == OrderStatus.READY }

    val todayOrders = remember(orders, todayStartMs) {
        orders.filter { (it.createdAt?.toDate()?.time ?: 0L) >= todayStartMs }
    }
    val nDoneToday = todayOrders.count { it.statusEnum == OrderStatus.DONE }
    val todayCount = todayOrders.size
    val servedPct = if (todayCount > 0) ((nDoneToday * 100f) / todayCount).toInt().coerceIn(0, 100) else 0

    val dayCounts = remember(orders, todayStartMs) {
        (6 downTo 0).map { daysAgo ->
            val start = todayStartMs - daysAgo * 86_400_000L
            val end = start + 86_400_000L
            orders.count {
                val ms = it.createdAt?.toDate()?.time ?: 0L
                ms in start until end
            }
        }
    }

    val activeCount = nNew + nConfirmed + nPreparing + nReady
    val cookingCount = nConfirmed + nPreparing
    val todaySales = todayOrders.filter { it.statusEnum != OrderStatus.CANCELLED }.sumOf { it.total }
    val salesStr = formatEtb(todaySales)

    val metrics = when (role) {
        UserRole.MANAGER -> RoleHomeMetrics(
            bigNumber = todayCount,
            unitLabel = s.get("home_unit_orders", "orders"),
            subtitle = s.get("home_sub_manager", "received by the kitchen today"),
            tags = listOf(
                s.format("home_tag_waiting_rec", "n" to nNew),
                s.format("home_tag_cooking", "n" to cookingCount),
                s.format("home_tag_sales", "amount" to salesStr),
            ),
            stackRows = listOf(
                s.get("home_row_received_today", "Received today") to todayCount,
                s.get("home_row_preparing", "Preparing") to nPreparing,
                s.get("home_row_ready_pickup", "Ready for pickup") to nReady,
            ),
        )
        UserRole.RECEPTION -> RoleHomeMetrics(
            bigNumber = nNew,
            unitLabel = s.get("home_unit_waiting", "waiting"),
            subtitle = s.get("home_sub_reception", "orders to send to the kitchen"),
            tags = listOf(
                s.format("home_tag_ready_handover", "n" to nReady),
                s.format("home_tag_cooking", "n" to cookingCount),
            ),
            stackRows = listOf(
                s.get("home_row_waiting_rec", "Waiting at reception") to nNew,
                s.get("home_row_cooking", "Cooking") to cookingCount,
                s.get("home_row_ready_pickup", "Ready for pickup") to nReady,
            ),
        )
        UserRole.WAITER -> RoleHomeMetrics(
            bigNumber = nReady,
            unitLabel = s.get("home_unit_ready", "ready"),
            subtitle = s.get("home_sub_waiter", "orders waiting to be served"),
            tags = listOf(
                s.format("home_tag_cooking", "n" to cookingCount),
                s.format("home_tag_at_reception", "n" to nNew),
            ),
            stackRows = listOf(
                s.get("home_row_ready_serve", "Ready to serve") to nReady,
                s.get("home_row_cooking", "Cooking") to cookingCount,
                s.get("home_row_served_today", "Served today") to nDoneToday,
            ),
        )
        UserRole.KITCHEN -> RoleHomeMetrics(
            bigNumber = cookingCount,
            unitLabel = s.get("home_unit_cooking", "cooking"),
            subtitle = s.get("home_sub_kitchen", "orders in the kitchen now"),
            tags = listOf(
                s.format("home_tag_to_start", "n" to nConfirmed),
                s.format("home_tag_ready", "n" to nReady),
            ),
            stackRows = listOf(
                s.get("home_row_received", "Received") to nConfirmed,
                s.get("home_row_preparing", "Preparing") to nPreparing,
                s.get("home_row_ready", "Ready") to nReady,
            ),
        )
        UserRole.CASHIER -> RoleHomeMetrics(
            bigNumber = nDoneToday,
            unitLabel = s.get("home_unit_settled", "settled"),
            subtitle = s.get("home_sub_cashier", "orders completed today"),
            tags = listOf(
                s.format("home_tag_sales", "amount" to salesStr),
                s.format("home_tag_still_open", "n" to activeCount),
            ),
            stackRows = listOf(
                s.get("home_row_completed_today", "Completed today") to nDoneToday,
                s.get("home_row_open_orders", "Open orders") to activeCount,
                s.get("home_row_ready_pickup", "Ready for pickup") to nReady,
            ),
        )
        UserRole.DELIVERY -> RoleHomeMetrics(
            bigNumber = activeCount,
            unitLabel = s.get("home_unit_active", "active"),
            subtitle = s.get("home_sub_delivery", "takeaway & delivery orders"),
            tags = listOf(
                s.format("home_tag_ready_go", "n" to nReady),
                s.format("home_tag_cooking", "n" to cookingCount),
            ),
            stackRows = listOf(
                s.get("home_row_active", "Active") to activeCount,
                s.get("home_row_preparing", "Preparing") to nPreparing,
                s.get("home_row_ready_go", "Ready to go") to nReady,
            ),
        )
        UserRole.CUSTOMER -> RoleHomeMetrics(
            bigNumber = activeCount,
            unitLabel = s.get("home_unit_active", "active"),
            subtitle = s.get("home_sub_customer", "your orders in progress"),
            tags = listOf(
                s.format("home_tag_ready", "n" to nReady),
                s.format("home_tag_done_today", "n" to nDoneToday),
            ),
            stackRows = listOf(
                s.get("home_row_active", "Active") to activeCount,
                s.get("home_row_preparing", "Preparing") to cookingCount,
                s.get("home_row_ready", "Ready") to nReady,
            ),
        )
    }

    val allowed = role.allowedTabs
    val primaryAction: Pair<String, () -> Unit>? = when {
        AppTab.MENU in allowed -> s.get("home_action_order_food", "Order food") to { onNavigateTab(AppTab.MENU) }
        AppTab.KITCHEN in allowed -> s.get("home_action_kitchen_board", "Kitchen board") to { onNavigateTab(AppTab.KITCHEN) }
        AppTab.ORDERS in allowed -> s.get("home_action_open_orders", "Open orders") to { onNavigateTab(AppTab.ORDERS) }
        else -> null
    }
    val secondaryAction: Pair<String, () -> Unit>? =
        if (AppTab.MENU in allowed && AppTab.KITCHEN in allowed) {
            "${s.get("home_action_kitchen_board", "Kitchen board")} ›" to { onNavigateTab(AppTab.KITCHEN) }
        } else {
            null
        }

    val waiterCoverageText = if (role == UserRole.WAITER) {
        val cov = uiState.currentUser?.userId?.let { uiState.waiterCoverage[it] }
        val areas = cov?.areaSet?.sortedBy { it.index }?.joinToString(", ") { s.waiterAreaLabel(it) }.orEmpty()
        if (areas.isNotBlank()) " · $areas" else ""
    } else {
        ""
    }
    val roleLabel = s.roleName(role)
    val datePill = remember(roleLabel, waiterCoverageText, s.language) {
        val df = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
        "${df.format(Date())} · $roleLabel$waiterCoverageText"
    }

    val latestOrders = remember(orders) {
        orders.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(3)
    }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = navBottom + 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroSection(
                pillText = datePill,
                bigNumber = metrics.bigNumber,
                unitLabel = metrics.unitLabel,
                subtitle = metrics.subtitle,
                tags = metrics.tags,
                primaryAction = primaryAction,
                secondaryAction = secondaryAction,
            )
        }

        // Analytics dark plum summary card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = palette.plum,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularServedRing(
                        percent = servedPct,
                        accentColor = palette.acc,
                        trackColor = palette.onPlum.copy(alpha = 0.14f),
                        centerColor = palette.plumDark,
                        onPlumColor = palette.onPlum,
                    )

                    VerticalDivider(
                        color = palette.onPlum.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxHeight(),
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = s.get("home_completed_today_multiline", "Completed\ntoday"),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            color = palette.onPlum.copy(alpha = 0.72f),
                        )
                        Text(
                            text = nDoneToday.toString(),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            color = palette.onPlum,
                        )
                    }

                    VerticalDivider(
                        color = palette.onPlum.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxHeight(),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = s.get("home_last_7_days", "Last 7 days"),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = palette.onPlum.copy(alpha = 0.72f),
                        )
                        MiniSevenDayBarChart(
                            dayCounts = dayCounts,
                            accentColor = palette.acc,
                            barColor = palette.onPlum.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }

        // Overlapping 3-card status stack
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                metrics.stackRows.forEachIndexed { idx, (label, count) ->
                    val bg = when (idx) {
                        0 -> palette.alt
                        1 -> palette.plum2
                        else -> palette.acc
                    }
                    val fg = when (idx) {
                        0 -> palette.text
                        1 -> palette.onPlum
                        else -> palette.onAcc
                    }
                    val bottomPad = if (idx < 2) 26.dp else 16.dp
                    val topOffset = when (idx) {
                        0 -> 0.dp
                        1 -> (-16).dp
                        else -> (-32).dp
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = topOffset)
                            .zIndex(idx.toFloat())
                            .shadow(6.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(bg)
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = bottomPad),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            letterSpacing = (-0.5).sp,
                            color = fg,
                        )
                        Text(
                            text = count.toString(),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = fg,
                        )
                    }
                }
            }
        }

        // 3-column Area Occupancy cards
        item {
            Box(modifier = Modifier.offset(y = (-16).dp)) {
                AreaOccupancyRow(
                    orders = orders,
                    onOpenArea = onOpenArea,
                )
            }
        }

        // Latest orders section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-8).dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.get("home_latest_orders", "Latest orders"),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.4).sp,
                        color = palette.text,
                    )
                    if (AppTab.ORDERS in allowed) {
                        Text(
                            text = s.get("home_see_all", "See all"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = palette.plum,
                            modifier = Modifier
                                .clickable { onNavigateTab(AppTab.ORDERS) }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .testTag("see_all_orders_button"),
                        )
                    }
                }

                when (val ordersState = uiState.ordersState) {
                    is UiState.Loading -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                color = palette.plum,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = s.get("home_connecting_orders", "Connecting to live orders…"),
                                fontFamily = font,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = palette.mut,
                            )
                        }
                    }
                    is UiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = StatusCancelledBg,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = ordersState.message,
                                fontFamily = font,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = StatusCancelledText,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    is UiState.Success -> if (latestOrders.isEmpty()) {
                        Text(
                            text = s.get("home_no_orders", "No orders yet."),
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = palette.mut,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    } else {
                        latestOrders.forEach { order ->
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
                                            text = "#${order.orderNumber} · ${formatEtb(order.total)}",
                                            fontFamily = font,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = palette.text,
                                        )
                                        Text(
                                            text = "${s.orderTypeLabel(order.typeEnum)} · ${order.location}",
                                            fontFamily = font,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = palette.mut,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    StatusPill(order.statusEnum)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AreaOccupancyRow(
    orders: List<Order>,
    onOpenArea: (String) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FloorZone.entries.forEach { zone ->
            val zoneLocations = zone.locations.toSet()
            val zoneActive = orders.filter {
                it.typeEnum == zone.orderType && it.location in zoneLocations && !it.statusEnum.isTerminal
            }
            val occupiedCount = zoneActive.map { it.location }.toSet().size
            val readyCount = zoneActive.count { it.statusEnum == OrderStatus.READY }
            val cookingCount = zoneActive.count {
                it.statusEnum == OrderStatus.CONFIRMED || it.statusEnum == OrderStatus.PREPARING
            }
            val ratio = (occupiedCount.toFloat() / zone.capacity.toFloat()).coerceIn(0f, 1f)

            Surface(
                onClick = { onOpenArea(zone.key) },
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("area_card_${zone.key}"),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = s.floorZoneTitle(zone),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (readyCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.acc)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = s.format("home_tag_ready", "n" to readyCount),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.5.sp,
                                    color = palette.onAcc,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.plum,
                                ),
                            ) {
                                append(occupiedCount.toString())
                            }
                            withStyle(
                                SpanStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.mut,
                                ),
                            ) {
                                append(s.format("home_of_cap", "cap" to zone.capacity))
                            }
                        },
                        fontFamily = font,
                        lineHeight = 32.sp,
                    )

                    Text(
                        text = if (zone == FloorZone.HOTEL) {
                            s.get("home_rooms_in_use", "rooms in use")
                        } else {
                            s.get("home_tables_in_use", "tables in use")
                        },
                        fontFamily = font,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = palette.mut,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette.alt),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceAtLeast(if (occupiedCount > 0) 0.08f else 0f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.acc),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.alt)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = s.format("home_n_active", "n" to zoneActive.size),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = palette.text,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.alt)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = s.format("home_tag_cooking", "n" to cookingCount),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = palette.text,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
