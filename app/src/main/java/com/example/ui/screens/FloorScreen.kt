package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloorZone
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.localization.LocalAppStrings
import com.example.ui.components.HeroSection
import com.example.ui.components.SelectedPlaceBanner
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusNewBg
import com.example.ui.theme.StatusNewText
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloorScreen(
    uiState: LakemarkUiState,
    onSelectZone: (FloorZone) -> Unit,
    onSelectPlace: (FloorZone, String) -> Unit,
    onChangePlaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val zone = uiState.selectedZone
    val orders = uiState.orders

    fun liveOrderFor(z: FloorZone, loc: String): Order? =
        orders.firstOrNull { it.typeEnum == z.orderType && it.location == loc && !it.statusEnum.isTerminal }

    fun occupiedInZone(z: FloorZone): Int =
        z.locations.count { liveOrderFor(z, it) != null }

    val zoneTags = remember(orders, s) {
        FloorZone.entries.map { "${s.floorZoneTitle(it)} ${occupiedInZone(it)}/${it.capacity}" }
    }
    val subtitle = when (zone) {
        FloorZone.HOTEL -> s.get("floor_sub_hotel", "rooms in service")
        FloorZone.DINE -> s.get("floor_sub_dine", "tables in use")
        FloorZone.REC -> s.get("floor_sub_rec", "desk orders")
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = navBottom + 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroSection(
                pillText = if (zone == FloorZone.HOTEL) {
                    s.get("floor_tap_room", "Tap a room to start an order")
                } else {
                    s.get("floor_tap_table", "Tap a table to start an order")
                },
                bigNumber = occupiedInZone(zone),
                unitLabel = s.format("home_of_cap", "cap" to zone.capacity).trim(),
                subtitle = subtitle,
                tags = zoneTags,
            )
        }

        // Zone Segmented Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.alt)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FloorZone.entries.forEach { z ->
                    val selected = z == zone
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) palette.plum else Color.Transparent)
                            .clickable { onSelectZone(z) }
                            .testTag("zone_tab_${z.key}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = s.floorZoneTitle(z),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = if (selected) palette.onPlum else palette.mut,
                        )
                    }
                }
            }
        }

        // Legend Row
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LegendBadge(s.get("floor_legend_free", "Free"), palette.surface, palette.text)
                LegendBadge(s.get("floor_legend_new", "New"), StatusNewBg, StatusNewText)
                LegendBadge(s.get("floor_legend_cooking", "Cooking"), palette.plum2, palette.onPlum)
                LegendBadge(s.get("floor_legend_ready", "Ready"), palette.acc, palette.onAcc)
            }
        }

        if (uiState.selectedPlace != null) {
            item {
                SelectedPlaceBanner(
                    selectedPlace = uiState.selectedPlace,
                    onChangeClick = onChangePlaceClick,
                )
            }
        }

        if (zone == FloorZone.HOTEL) {
            val firstFloor = zone.locations.subList(0, 16)
            val secondFloor = zone.locations.subList(16, 32)
            item {
                DashedFloorCard(
                    title = s.get("floor_first_floor", "First floor · 101–116"),
                    sideLeft = s.get("floor_stairs", "Stairs"),
                    sideRight = s.get("floor_lift", "Lift"),
                ) {
                    RoomGrid(
                        rooms = firstFloor.subList(0, 8),
                        uiState = uiState,
                        liveOrder = { liveOrderFor(FloorZone.HOTEL, it) },
                        onTap = { onSelectPlace(FloorZone.HOTEL, it) },
                    )
                    CorridorBar()
                    RoomGrid(
                        rooms = firstFloor.subList(8, 16),
                        uiState = uiState,
                        liveOrder = { liveOrderFor(FloorZone.HOTEL, it) },
                        onTap = { onSelectPlace(FloorZone.HOTEL, it) },
                    )
                }
            }
            item {
                DashedFloorCard(
                    title = s.get("floor_second_floor", "Second floor · 201–216"),
                    sideLeft = s.get("floor_stairs", "Stairs"),
                    sideRight = s.get("floor_lift", "Lift"),
                ) {
                    RoomGrid(
                        rooms = secondFloor.subList(0, 8),
                        uiState = uiState,
                        liveOrder = { liveOrderFor(FloorZone.HOTEL, it) },
                        onTap = { onSelectPlace(FloorZone.HOTEL, it) },
                    )
                    CorridorBar()
                    RoomGrid(
                        rooms = secondFloor.subList(8, 16),
                        uiState = uiState,
                        liveOrder = { liveOrderFor(FloorZone.HOTEL, it) },
                        onTap = { onSelectPlace(FloorZone.HOTEL, it) },
                    )
                }
            }
        } else {
            val topTitle = if (zone == FloorZone.DINE) {
                s.get("floor_indoor", "Indoor Area")
            } else {
                s.get("floor_lobby_lounge", "Lobby lounge")
            }
            val leftLabel = if (zone == FloorZone.DINE) {
                s.get("floor_patio", "Patio")
            } else {
                s.get("floor_entrance", "Entrance")
            }
            val rightLabel = if (zone == FloorZone.DINE) {
                s.get("floor_vip", "VIP Area")
            } else {
                s.get("floor_front_desk", "Front desk")
            }
            item {
                DashedFloorCard(
                    title = topTitle,
                    sideLeft = leftLabel,
                    sideRight = rightLabel,
                    footerTitle = topTitle,
                ) {
                    TableGrid(
                        zone = zone,
                        locations = zone.locations,
                        uiState = uiState,
                        liveOrder = { liveOrderFor(zone, it) },
                        onTap = { onSelectPlace(zone, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendBadge(text: String, bg: Color, fg: Color) {
    val font = lakemarkFont()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontFamily = font,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = fg,
        )
    }
}

@Composable
private fun CorridorBar() {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.alt)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = s.get("floor_corridor", "Corridor"),
            fontFamily = font,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = palette.mut,
        )
    }
}

@Composable
private fun DashedFloorCard(
    title: String,
    sideLeft: String,
    sideRight: String,
    footerTitle: String? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val font = lakemarkFont()
    val lineColor = palette.line
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(palette.surface)
            .drawBehind {
                drawRoundRect(
                    color = lineColor,
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f),
                    ),
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sideLeft,
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = palette.mut,
            )
            Text(
                text = title,
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = palette.mut,
            )
            Text(
                text = sideRight,
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = palette.mut,
            )
        }
        content()
        if (!footerTitle.isNullOrBlank()) {
            Text(
                text = footerTitle,
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = palette.mut,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun RoomGrid(
    rooms: List<String>,
    uiState: LakemarkUiState,
    liveOrder: (String) -> Order?,
    onTap: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rooms.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { loc ->
                    val order = liveOrder(loc)
                    val selected = uiState.selectedPlace?.type == FloorZone.HOTEL.orderType &&
                        uiState.selectedPlace.location == loc
                    FloorTileButton(
                        tileTitle = loc,
                        order = order,
                        selected = selected,
                        chairCount = 1,
                        showTopChairs = false,
                        showBottomChairs = false,
                        fullLabel = s.format("floor_room_prefix", "loc" to loc),
                        onClick = { onTap(loc) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TableGrid(
    zone: FloorZone,
    locations: List<String>,
    uiState: LakemarkUiState,
    liveOrder: (String) -> Order?,
    onTap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        locations.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { loc ->
                    val order = liveOrder(loc)
                    val selected = uiState.selectedPlace?.type == zone.orderType &&
                        uiState.selectedPlace.location == loc
                    FloorTileButton(
                        tileTitle = zone.shortTileLabel(loc),
                        order = order,
                        selected = selected,
                        chairCount = 3,
                        showTopChairs = true,
                        showBottomChairs = true,
                        fullLabel = zone.fullLabel(loc),
                        onClick = { onTap(loc) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FloorTileButton(
    tileTitle: String,
    order: Order?,
    selected: Boolean,
    chairCount: Int,
    showTopChairs: Boolean,
    showBottomChairs: Boolean,
    fullLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val status = order?.statusEnum
    val (bg, fg, chairColor) = when (status) {
        OrderStatus.NEW -> Triple(StatusNewBg, StatusNewText, StatusNewText.copy(alpha = 0.45f))
        OrderStatus.CONFIRMED, OrderStatus.PREPARING -> Triple(palette.plum2, palette.onPlum, palette.plum2)
        OrderStatus.READY -> Triple(palette.acc, palette.onAcc, palette.acc)
        else -> Triple(palette.alt, palette.plum, palette.line)
    }
    val mins = if (order != null) {
        val createdMs = order.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        max(1, ((System.currentTimeMillis() - createdMs) / 60_000L).toInt())
    } else {
        0
    }
    val a11yLabel = if (order != null) {
        "$fullLabel, #${order.orderNumber} ${s.statusLabel(order.statusEnum)}"
    } else {
        "$fullLabel, ${s.get("floor_legend_free", "Free")}"
    }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = a11yLabel }
            .testTag("floor_tile_${tileTitle.replace(" ", "_")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showTopChairs) {
            ChairBarRow(count = chairCount, color = chairColor)
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = bg,
            border = if (selected) BorderStroke(2.5.dp, palette.plum) else null,
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = tileTitle,
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = fg,
                    maxLines = 1,
                )
                if (order != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${mins}m",
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = fg.copy(alpha = 0.88f),
                        )
                        Text(
                            text = "#${order.orderNumber}",
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = fg.copy(alpha = 0.88f),
                        )
                    }
                }
            }
        }
        if (showBottomChairs) {
            ChairBarRow(count = chairCount, color = chairColor)
        }
    }
}

@Composable
private fun ChairBarRow(count: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .width(if (count == 1) 28.dp else 18.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color),
            )
        }
    }
}
