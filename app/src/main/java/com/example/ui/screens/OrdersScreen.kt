package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.UserRole
import com.example.localization.LocalAppStrings
import com.example.ui.components.CompactOrderRow
import com.example.ui.components.HeroSection
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import com.example.ui.viewmodel.OrdersFilterTab
import com.example.ui.viewmodel.OrdersZoneFilter
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class OrderDayGroup(
    val dayKey: Long,
    val label: String,
    val orders: List<Order>,
)

@Composable
fun OrdersScreen(
    uiState: LakemarkUiState,
    onSelectFilter: (OrdersFilterTab) -> Unit,
    onSelectZoneFilter: (OrdersZoneFilter) -> Unit,
    onAdvanceOrder: (Order, OrderStatus) -> Unit,
    onCancelRequest: (Order) -> Unit,
    onAssignWaiterClick: (Order) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val allOrders = uiState.orders
    val isFinished = uiState.ordersFilter == OrdersFilterTab.FINISHED
    val canAssignWaiter = uiState.activeRole in listOf(UserRole.MANAGER, UserRole.RECEPTION, UserRole.CASHIER)

    val nNew = allOrders.count { it.statusEnum == OrderStatus.NEW }
    val nCook = allOrders.count { it.statusEnum == OrderStatus.CONFIRMED || it.statusEnum == OrderStatus.PREPARING }
    val nReady = allOrders.count { it.statusEnum == OrderStatus.READY }
    val activeTotal = nNew + nCook + nReady

    val filtered = remember(allOrders, isFinished, uiState.ordersZoneFilter) {
        allOrders.filter { order ->
            val statusMatch = if (isFinished) order.statusEnum.isTerminal else !order.statusEnum.isTerminal
            val zoneMatch = uiState.ordersZoneFilter == OrdersZoneFilter.ALL ||
                order.typeEnum.key == uiState.ordersZoneFilter.key
            statusMatch && zoneMatch
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    val todayLabel = s.get("orders_today", "Today")
    val yesterdayLabel = s.get("orders_yesterday", "Yesterday")
    val grouped = remember(filtered, todayLabel, yesterdayLabel) {
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val calYesterday = calToday - 86_400_000L
        val df = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())

        val map = linkedMapOf<Long, MutableList<Order>>()
        filtered.forEach { order ->
            val ms = order.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            val c = Calendar.getInstance().apply {
                timeInMillis = ms
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            map.getOrPut(c) { mutableListOf() }.add(order)
        }
        map.entries.sortedByDescending { it.key }.map { (dayKey, list) ->
            val label = when (dayKey) {
                calToday -> todayLabel
                calYesterday -> yesterdayLabel
                else -> df.format(Date(dayKey))
            }
            OrderDayGroup(dayKey = dayKey, label = label, orders = list)
        }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = navBottom + 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroSection(
                pillText = s.get("nav_orders", "Orders"),
                bigNumber = activeTotal,
                unitLabel = s.get("home_unit_active", "active"),
                subtitle = s.get("home_sub_customer", "orders in progress"),
                tags = listOf(
                    s.format("home_tag_at_reception", "n" to nNew),
                    s.format("home_tag_cooking", "n" to nCook),
                    s.format("home_tag_ready", "n" to nReady),
                ),
            )
        }

        // Active / Finished chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.alt)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OrdersFilterTab.entries.forEach { tab ->
                    val selected = tab == uiState.ordersFilter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) palette.plum else Color.Transparent)
                            .clickable { onSelectFilter(tab) }
                            .testTag("orders_filter_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (tab == OrdersFilterTab.ACTIVE) {
                                s.get("orders_filter_active", "Active")
                            } else {
                                s.get("orders_filter_finished", "Finished")
                            },
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (selected) palette.onPlum else palette.mut,
                        )
                    }
                }
            }
        }

        // Zone filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.alt)
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OrdersZoneFilter.entries.forEach { zf ->
                    val selected = zf == uiState.ordersZoneFilter
                    val zfLabel = when (zf) {
                        OrdersZoneFilter.ALL -> s.get("orders_zone_all", "All")
                        OrdersZoneFilter.ROOM -> s.get("zone_rooms", "Rooms")
                        OrdersZoneFilter.DINE -> s.get("zone_restaurant", "Restaurant")
                        OrdersZoneFilter.PICKUP -> s.get("zone_reception", "Reception")
                        OrdersZoneFilter.OUT -> s.get("order_type_out", "Takeaway")
                    }
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) palette.plum else Color.Transparent)
                            .clickable { onSelectZoneFilter(zf) }
                            .padding(horizontal = 14.dp)
                            .testTag("orders_zone_${zf.key}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = zfLabel,
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (selected) palette.onPlum else palette.mut,
                        )
                    }
                }
            }
        }

        when (val ordersState = uiState.ordersState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = palette.plum, strokeWidth = 2.5.dp)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StatusCancelledBg,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = ordersState.message,
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = StatusCancelledText,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            is UiState.Success -> if (grouped.isEmpty()) {
                item {
                    Text(
                        text = s.get("orders_empty", "No orders here."),
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = palette.mut,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            } else {
                grouped.forEach { group ->
                    item(key = "header_${group.dayKey}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = group.label,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.4).sp,
                                color = palette.text,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.alt)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = group.orders.size.toString(),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = palette.text,
                                )
                            }
                        }
                    }
                    items(group.orders, key = { it.id }) { order ->
                        CompactOrderRow(
                            order = order,
                            activeRole = uiState.activeRole,
                            isHighlighted = uiState.highlightedOrderId == order.id,
                            waiterName = uiState.waiterName(order.assignedWaiterId),
                            canAssignWaiter = canAssignWaiter,
                            onAssignWaiterClick = { onAssignWaiterClick(order) },
                            onAdvance = { target -> onAdvanceOrder(order, target) },
                            onCancelRequest = { onCancelRequest(order) },
                        )
                    }
                }
            }
        }
    }
}
