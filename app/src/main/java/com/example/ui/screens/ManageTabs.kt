package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultMenuCatalog
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.localization.LocalAppStrings
import com.example.ui.components.FoodImageBox
import com.example.ui.components.categoryIconVector
import com.example.ui.components.formatEtb
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.StatusNewBg
import com.example.ui.theme.StatusNewText
import com.example.ui.theme.StatusReadyBg
import com.example.ui.theme.StatusReadyText
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DailyReportRow(
    val label: String,
    val count: Int,
    val sales: Long,
)

private data class TopDishSummary(
    val id: String,
    val name: String,
    val qty: Long,
    val revenue: Long,
)

internal fun LazyListScope.overviewSectionItems(uiState: LakemarkUiState) {
    when (val ordersState = uiState.ordersState) {
        is UiState.Loading -> {
            item(key = "overview_loading") {
                val palette = LocalBrandPalette.current
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
            item(key = "overview_error") {
                val font = lakemarkFont()
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
        is UiState.Success -> {
            item(key = "overview_content") {
                OverviewSectionContent(orders = ordersState.data, uiState = uiState)
            }
        }
    }
}

@Composable
private fun OverviewSectionContent(orders: List<Order>, uiState: LakemarkUiState) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val todayStartMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val windowStartMs = remember(todayStartMs) { todayStartMs - 6 * 86_400_000L }

    val todayOrders = remember(orders, todayStartMs) {
        orders.filter { (it.createdAt?.toDate()?.time ?: 0L) >= todayStartMs }
    }
    val todaySales = remember(todayOrders) {
        todayOrders.filter { it.statusEnum != OrderStatus.CANCELLED }.sumOf { it.total }
    }
    val todayActiveCount = remember(todayOrders) {
        todayOrders.count { !it.statusEnum.isTerminal }
    }

    val completed7Days = remember(orders, windowStartMs) {
        orders.filter { order ->
            if (order.statusEnum != OrderStatus.DONE) return@filter false
            val ms = (order.doneAt ?: order.updatedAt ?: order.createdAt)?.toDate()?.time ?: 0L
            ms >= windowStartMs
        }
    }
    val cancelled7DaysCount = remember(orders, windowStartMs) {
        orders.count { order ->
            if (order.statusEnum != OrderStatus.CANCELLED) return@count false
            val ms = (order.cancelledAt ?: order.updatedAt ?: order.createdAt)?.toDate()?.time ?: 0L
            ms >= windowStartMs
        }
    }
    val inProgressCount = remember(orders) {
        orders.count { !it.statusEnum.isTerminal }
    }

    val todayLabel = s.get("orders_today", "Today")
    val dayBuckets = remember(completed7Days, todayStartMs, todayLabel) {
        val df = SimpleDateFormat("EEE", Locale.getDefault())
        (6 downTo 0).map { daysAgo ->
            val start = todayStartMs - daysAgo * 86_400_000L
            val end = start + 86_400_000L
            val dayOrders = completed7Days.filter { order ->
                val ms = (order.doneAt ?: order.updatedAt ?: order.createdAt)?.toDate()?.time ?: 0L
                ms in start until end
            }
            val label = if (daysAgo == 0) todayLabel else df.format(Date(start))
            DailyReportRow(
                label = label,
                count = dayOrders.size,
                sales = dayOrders.sumOf { it.total },
            )
        }
    }
    val total7DaysSales = remember(dayBuckets) { dayBuckets.sumOf { it.sales } }
    val avgTicket7Days = remember(completed7Days, total7DaysSales) {
        if (completed7Days.isEmpty()) 0L else total7DaysSales / completed7Days.size
    }
    val maxDaySales = remember(dayBuckets) { dayBuckets.maxOfOrNull { it.sales }?.coerceAtLeast(1L) ?: 1L }

    val menuById = remember(uiState.menuItems) { uiState.menuItems.associateBy { it.id } }
    val topDishes = remember(completed7Days) {
        val byDish = linkedMapOf<String, Triple<String, Long, Long>>()
        completed7Days.forEach { order ->
            order.items.forEach { line ->
                val key = line.menuItemId.ifBlank { line.name }
                val prev = byDish[key]
                val qty = (prev?.second ?: 0L) + line.quantity
                val rev = (prev?.third ?: 0L) + line.quantity * line.price
                byDish[key] = Triple(line.name, qty, rev)
            }
        }
        byDish.entries
            .map { (id, data) ->
                TopDishSummary(id = id, name = data.first, qty = data.second, revenue = data.third)
            }
            .sortedByDescending { it.qty }
            .take(5)
    }
    val topDishMaxQty = remember(topDishes) { topDishes.firstOrNull()?.qty?.coerceAtLeast(1L) ?: 1L }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Key Performance KPI Pair
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KpiSummaryCard(
                label = s.get("manage_kpi_sales", "Today sales"),
                value = formatEtb(todaySales),
                caption = "${todayOrders.size} ${s.get("manage_hero_unit", "orders")} · $todayActiveCount ${s.get("home_unit_active", "active")}",
                highlightColor = palette.plum,
                onHighlightColor = palette.onPlum,
                accentColor = palette.acc,
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_kpi_today"),
            )
            KpiSummaryCard(
                label = s.get("home_last_7_days", "Last 7 days"),
                value = formatEtb(total7DaysSales),
                caption = if (completed7Days.isNotEmpty()) {
                    "${completed7Days.size} ${s.get("status_done", "Completed")} · ${formatEtb(avgTicket7Days)}"
                } else {
                    s.get("home_no_orders", "No orders yet.")
                },
                highlightColor = palette.surface,
                onHighlightColor = palette.text,
                accentColor = palette.plum,
                isSecondary = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("overview_kpi_7d"),
            )
        }

        // 2. Order Pipeline Breakdown
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PipelinePill(
                    count = inProgressCount,
                    label = s.get("home_row_active", "Active"),
                    bg = StatusNewBg,
                    fg = StatusNewText,
                    modifier = Modifier.weight(1f),
                )
                PipelinePill(
                    count = completed7Days.size,
                    label = s.get("status_done", "Completed"),
                    bg = StatusReadyBg,
                    fg = StatusReadyText,
                    modifier = Modifier.weight(1f),
                )
                PipelinePill(
                    count = cancelled7DaysCount,
                    label = s.get("status_cancelled", "Cancelled"),
                    bg = StatusCancelledBg,
                    fg = StatusCancelledText,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 3. 7-Day Daily Sales Breakdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = s.get("manage_reports_title", "Reports & analytics"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = palette.text,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.alt)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "${completed7Days.size} ${s.get("home_unit_settled", "settled")}",
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = palette.plum,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manage_reports_card"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                dayBuckets.forEach { row ->
                    val fraction = (row.sales.toFloat() / maxDaySales.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = row.label,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = palette.text,
                            )
                            Text(
                                text = "${row.count} ${s.get("manage_hero_unit", "orders")} · ${formatEtb(row.sales)}",
                                fontFamily = font,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = palette.mut,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.alt),
                        ) {
                            if (row.sales > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction.coerceAtLeast(0.06f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.acc),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Top Performing Dishes
        Text(
            text = s.get("manage_top_dishes", "Top dishes"),
            fontFamily = font,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = palette.text,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (topDishes.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = palette.mut,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = s.get("home_no_orders", "No orders yet."),
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = palette.mut,
                        )
                    }
                } else {
                    topDishes.forEachIndexed { idx, dish ->
                        val dishFraction = (dish.qty.toFloat() / topDishMaxQty.toFloat()).coerceIn(0f, 1f)
                        val liveMenuItem = menuById[dish.id]
                        val displayName = if (liveMenuItem != null) s.dishName(liveMenuItem) else dish.name
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == 0) palette.acc else palette.alt),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = if (idx == 0) palette.onAcc else palette.text,
                                    )
                                }
                                Text(
                                    text = displayName,
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = palette.text,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${dish.qty}× · ${formatEtb(dish.revenue)}",
                                    fontFamily = font,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = palette.mut,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette.alt),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(dishFraction.coerceAtLeast(0.08f))
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (idx == 0) palette.acc else palette.plum.copy(alpha = 0.45f)),
                                )
                            }
                        }
                        if (idx < topDishes.lastIndex) {
                            HorizontalDivider(color = palette.line.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiSummaryCard(
    label: String,
    value: String,
    caption: String,
    highlightColor: Color,
    onHighlightColor: Color,
    accentColor: Color,
    isSecondary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val font = lakemarkFont()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = highlightColor,
        shadowElevation = if (isSecondary) 2.dp else 4.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSecondary) palette.mut else onHighlightColor.copy(alpha = 0.78f),
            )
            Text(
                text = value,
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = if (isSecondary) palette.plum else accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = caption,
                fontFamily = font,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = if (isSecondary) palette.mut else onHighlightColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PipelinePill(
    count: Int,
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    val font = lakemarkFont()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = count.toString(),
            fontFamily = font,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = fg,
        )
        Text(
            text = label,
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun LazyListScope.manageMenuSectionItems(
    uiState: LakemarkUiState,
    selectedCategory: String,
    searchQuery: String,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUpdateMenuPrice: (String, Long) -> Unit,
    onToggleMenuAvailable: (MenuItem) -> Unit,
    onPickFoodImage: (MenuItem) -> Unit = {},
    onRemoveFoodImage: (MenuItem) -> Unit = {},
    onOpenSaved: () -> Unit = {},
) {
    val allItems = uiState.menuItems
    val s = uiState.strings
    val categories = listOf("All") +
        DefaultMenuCatalog.categories.drop(1).filter { cat -> allItems.any { it.category == cat } } +
        allItems.map { it.category }.distinct().filter { it !in DefaultMenuCatalog.categories }

    val availableCount = allItems.count { it.available }
    val pausedCount = allItems.size - availableCount
    val query = searchQuery.trim().lowercase()
    val filteredItems = allItems.filter { item ->
        val catOk = selectedCategory == "All" || item.category == selectedCategory
        val qOk = query.isEmpty() ||
            item.name.lowercase().contains(query) ||
            s.dishName(item).lowercase().contains(query) ||
            item.category.lowercase().contains(query) ||
            s.menuCategoryLabel(item.category).lowercase().contains(query)
        catOk && qOk
    }

    item(key = "manage_menu_header") {
        val palette = LocalBrandPalette.current
        val str = LocalAppStrings.current
        val font = lakemarkFont()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = str.get("manage_tab_menu", "Menu"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = palette.text,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onOpenSaved,
                    shape = RoundedCornerShape(999.dp),
                    color = palette.plum.copy(alpha = 0.12f),
                    modifier = Modifier.testTag("manage_menu_open_saved_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = palette.plum,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = str.format("manage_menu_view_saved", "n" to uiState.savedItems.size),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = palette.plum,
                        )
                    }
                }
                if (pausedCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(StatusCancelledBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "$pausedCount",
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = StatusCancelledText,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.alt)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$availableCount/${allItems.size}",
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        color = palette.text,
                    )
                }
            }
        }
    }

    item(key = "manage_menu_search") {
        val palette = LocalBrandPalette.current
        val str = LocalAppStrings.current
        val font = lakemarkFont()
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = str.get("menu_search_hint", "Search food or drink…"),
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    color = palette.mut,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = palette.mut,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.alt,
                focusedBorderColor = palette.plum,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manage_menu_search_input"),
        )
    }

    item(key = "manage_menu_categories") {
        val palette = LocalBrandPalette.current
        val str = LocalAppStrings.current
        val font = lakemarkFont()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                val selected = category == selectedCategory
                Surface(
                    onClick = { onSelectCategory(category) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) palette.plum else palette.alt,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("manage_menu_cat_${category.lowercase().replace(" ", "_")}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = categoryIconVector(category),
                            contentDescription = null,
                            tint = if (selected) palette.onPlum else palette.plum,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = str.menuCategoryLabel(category),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = if (selected) palette.onPlum else palette.text,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    when (val menuState = uiState.menuItemsState) {
        is UiState.Loading -> {
            item(key = "manage_menu_loading") {
                val palette = LocalBrandPalette.current
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
            item(key = "manage_menu_error") {
                val font = lakemarkFont()
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StatusCancelledBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = menuState.message,
                        fontFamily = font,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = StatusCancelledText,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        is UiState.Success -> if (filteredItems.isEmpty()) {
            item(key = "manage_menu_empty") {
                val palette = LocalBrandPalette.current
                val str = LocalAppStrings.current
                val font = lakemarkFont()
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestaurantMenu,
                            contentDescription = null,
                            tint = palette.mut,
                        )
                        Text(
                            text = str.get("menu_empty", "No dishes match."),
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = palette.mut,
                        )
                    }
                }
            }
        } else {
            val grouped = if (selectedCategory == "All") {
                val orderedCategories = categories.drop(1)
                orderedCategories.mapNotNull { cat ->
                    val list = filteredItems.filter { it.category == cat }
                    if (list.isEmpty()) null else cat to list
                }
            } else {
                listOf(selectedCategory to filteredItems)
            }

            items(grouped, key = { (cat, _) -> "menu_group_$cat" }) { (categoryName, categoryItems) ->
                ManageMenuCategoryCard(
                    categoryName = categoryName,
                    menuItems = categoryItems,
                    uploadingItemId = uiState.uploadingMenuItemId,
                    onUpdateMenuPrice = onUpdateMenuPrice,
                    onToggleMenuAvailable = onToggleMenuAvailable,
                    onPickFoodImage = onPickFoodImage,
                    onRemoveFoodImage = onRemoveFoodImage,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageMenuCategoryCard(
    categoryName: String,
    menuItems: List<MenuItem>,
    uploadingItemId: String?,
    onUpdateMenuPrice: (String, Long) -> Unit,
    onToggleMenuAvailable: (MenuItem) -> Unit,
    onPickFoodImage: (MenuItem) -> Unit,
    onRemoveFoodImage: (MenuItem) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val focusManager = LocalFocusManager.current
    val liveInCat = menuItems.count { it.available }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manage_menu_card_${categoryName.lowercase().replace(" ", "_")}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = categoryIconVector(categoryName),
                        contentDescription = null,
                        tint = palette.plum,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = s.menuCategoryLabel(categoryName),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = palette.plum,
                    )
                }
                Text(
                    text = "$liveInCat/${menuItems.size}",
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = palette.mut,
                )
            }

            menuItems.forEachIndexed { idx, item ->
                var priceText by remember(item.id, item.price) {
                    mutableStateOf(item.price.toString())
                }
                val parsedPrice = priceText.toLongOrNull()
                val isDirty = parsedPrice != null && parsedPrice >= 0L && parsedPrice != item.price
                val isUploadingThis = uploadingItemId == item.id

                fun commitPriceIfValid() {
                    val parsed = priceText.toLongOrNull()
                    if (parsed != null && parsed >= 0L && parsed != item.price) {
                        onUpdateMenuPrice(item.id, parsed)
                    } else if (parsed == null) {
                        priceText = item.price.toString()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (item.available) 1f else 0.72f)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FoodImageBox(
                            imageUrl = item.imageUrl,
                            category = item.category,
                            dishName = s.dishName(item),
                            palette = palette,
                            size = 48.dp,
                            isUploading = isUploadingThis,
                            modifier = Modifier
                                .clickable(enabled = !isUploadingThis) { onPickFoodImage(item) }
                                .testTag("manage_food_thumb_${item.id}"),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = s.dishName(item),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!item.available) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(StatusCancelledBg)
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = s.get("saved_unavailable", "Sold out"),
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.5.sp,
                                        color = StatusCancelledText,
                                    )
                                }
                            } else {
                                Text(
                                    text = formatEtb(item.price),
                                    fontFamily = font,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = palette.mut,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { raw ->
                                priceText = raw.filter { it.isDigit() }.take(6)
                            },
                            singleLine = true,
                            trailingIcon = if (isDirty) {
                                {
                                    IconButton(
                                        onClick = {
                                            commitPriceIfValid()
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.testTag("manage_menu_price_save_${item.id}"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Save price for ${item.name}",
                                            tint = palette.acc,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    commitPriceIfValid()
                                    focusManager.clearFocus()
                                },
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.alt,
                                unfocusedContainerColor = palette.alt,
                                focusedBorderColor = palette.plum,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text,
                            ),
                            modifier = Modifier
                                .width(100.dp)
                                .heightIn(min = 48.dp)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && isDirty) {
                                        commitPriceIfValid()
                                    }
                                }
                                .testTag("manage_menu_price_${item.id}"),
                        )

                        Box(
                            modifier = Modifier.width(52.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Switch(
                                checked = item.available,
                                onCheckedChange = { onToggleMenuAvailable(item) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.acc,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = palette.line,
                                ),
                                modifier = Modifier.testTag("manage_menu_switch_${item.id}"),
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            onClick = { onPickFoodImage(item) },
                            enabled = !isUploadingThis,
                            shape = RoundedCornerShape(999.dp),
                            color = palette.alt,
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .testTag("food_photo_upload_${item.id}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddAPhoto,
                                    contentDescription = null,
                                    tint = palette.plum,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = if (item.imageUrl.isBlank()) {
                                        s.get("manage_menu_photo_add", "Photo")
                                    } else {
                                        s.get("manage_menu_photo_replace", "Replace photo")
                                    },
                                    fontFamily = font,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = palette.plum,
                                )
                            }
                        }

                        if (item.imageUrl.isNotBlank()) {
                            Surface(
                                onClick = { onRemoveFoodImage(item) },
                                enabled = !isUploadingThis,
                                shape = RoundedCornerShape(999.dp),
                                color = StatusCancelledBg,
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .testTag("food_photo_remove_${item.id}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = StatusCancelledText,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = s.get("manage_menu_photo_remove", "Remove photo"),
                                        fontFamily = font,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = StatusCancelledText,
                                    )
                                }
                            }
                        }
                    }
                }
                if (idx < menuItems.lastIndex) {
                    HorizontalDivider(color = palette.line.copy(alpha = 0.45f))
                }
            }
        }
    }
}
