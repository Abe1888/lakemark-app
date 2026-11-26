package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.localization.LocalAppStrings
import com.example.ui.components.HeroSection
import com.example.ui.components.OrderTicketCard
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import com.example.ui.viewmodel.UiState

@Composable
fun KitchenScreen(
    uiState: LakemarkUiState,
    onSelectStage: (OrderStatus) -> Unit,
    onAdvanceOrder: (Order, OrderStatus) -> Unit,
    onCancelRequest: (Order) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val orders = uiState.orders

    val nConfirmed = orders.count { it.statusEnum == OrderStatus.CONFIRMED }
    val nPreparing = orders.count { it.statusEnum == OrderStatus.PREPARING }
    val nReady = orders.count { it.statusEnum == OrderStatus.READY }
    val cookingCount = nConfirmed + nPreparing

    val stages = remember(nConfirmed, nPreparing, nReady, s) {
        listOf(
            Triple(
                s.get("kitchen_stage_received", "Received"),
                OrderStatus.CONFIRMED,
                nConfirmed to s.get("kitchen_stage_received_sub", "new tickets"),
            ),
            Triple(
                s.get("kitchen_stage_preparing", "Preparing"),
                OrderStatus.PREPARING,
                nPreparing to s.get("kitchen_stage_preparing_sub", "on the stove"),
            ),
            Triple(
                s.get("kitchen_stage_ready", "Ready"),
                OrderStatus.READY,
                nReady to s.get("kitchen_stage_ready_sub", "to serve"),
            ),
        )
    }

    val filteredTickets = remember(orders, uiState.kitchenStageFilter) {
        orders.filter { it.statusEnum == uiState.kitchenStageFilter }
            .sortedBy { it.createdAt?.seconds ?: 0L }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = navBottom + 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroSection(
                pillText = s.get("nav_kitchen", "Kitchen"),
                bigNumber = cookingCount,
                unitLabel = s.get("home_unit_cooking", "cooking"),
                subtitle = s.get("home_sub_kitchen", "orders in the kitchen now"),
                tags = listOf(
                    s.format("home_tag_to_start", "n" to nConfirmed),
                    s.format("home_tag_cooking", "n" to nPreparing),
                    s.format("home_tag_ready", "n" to nReady),
                ),
            )
        }

        // 3 clickable stage cards matching SOT .tri
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stages.forEach { (title, status, countAndSub) ->
                    val selected = status == uiState.kitchenStageFilter
                    Surface(
                        onClick = { onSelectStage(status) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) palette.plum else palette.surface,
                        shadowElevation = if (selected) 8.dp else 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kitchen_stage_${status.key}"),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = title,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (selected) palette.onPlum else palette.text,
                                maxLines = 1,
                            )
                            Text(
                                text = countAndSub.first.toString(),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp,
                                lineHeight = 36.sp,
                                color = if (selected) palette.onPlum else palette.plum,
                            )
                            Text(
                                text = countAndSub.second,
                                fontFamily = font,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = if (selected) palette.onPlum.copy(alpha = 0.78f) else palette.mut,
                                maxLines = 1,
                            )
                        }
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
            is UiState.Success -> if (filteredTickets.isEmpty()) {
                item {
                    Text(
                        text = s.get("kitchen_nothing_here", "Nothing here."),
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
                items(filteredTickets, key = { it.id }) { order ->
                    OrderTicketCard(
                        order = order,
                        activeRole = uiState.activeRole,
                        isHighlighted = uiState.highlightedOrderId == order.id,
                        waiterName = uiState.waiterName(order.assignedWaiterId),
                        onAdvance = { target -> onAdvanceOrder(order, target) },
                        onCancelRequest = { onCancelRequest(order) },
                    )
                }
            }
        }
    }
}
