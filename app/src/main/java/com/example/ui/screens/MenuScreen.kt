package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.DefaultMenuCatalog
import com.example.data.model.FloorZone
import com.example.data.model.MenuItem
import com.example.localization.LocalAppStrings
import com.example.ui.components.HeroSection
import com.example.ui.components.SelectedPlaceBanner
import com.example.ui.components.categoryIconVector
import com.example.ui.components.formatEtb
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.StatusReadyText
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import com.example.ui.viewmodel.UiState

@Composable
fun MenuScreen(
    uiState: LakemarkUiState,
    onStartArea: (String) -> Unit,
    onChangePlace: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onAddToCart: (MenuItem) -> Unit,
    onDecrementCart: (String) -> Unit,
    onToggleSaved: (MenuItem) -> Unit = {},
    onOpenSaved: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val place = uiState.selectedPlace ?: com.example.data.model.PlaceSelection(
        type = com.example.data.model.OrderType.DINE,
        location = "Table 1",
        label = "Table 1",
    )
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val categories = remember(uiState.menuItems) {
        val dynamic = uiState.menuItems.map { it.category }.distinct()
        listOf("All") + DefaultMenuCatalog.categories.drop(1).filter { it in dynamic } +
            dynamic.filter { it !in DefaultMenuCatalog.categories }
    }
    val filteredItems = remember(uiState.menuItems, uiState.selectedCategory, uiState.searchQuery, s) {
        val q = uiState.searchQuery.trim().lowercase()
        uiState.menuItems.filter { item ->
            val catOk = uiState.selectedCategory == "All" || item.category == uiState.selectedCategory
            val qOk = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                s.dishName(item).lowercase().contains(q) ||
                item.description.lowercase().contains(q) ||
                s.dishDescription(item).lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                s.menuCategoryLabel(item.category).lowercase().contains(q)
            catOk && qOk
        }
    }
    val cartMap = remember(uiState.cart) {
        uiState.cart.associate { it.menuItem.id to it.quantity }
    }

    BoxWithConstraints(modifier = modifier) {
        val columns = if (maxWidth >= 680.dp) 3 else 2

        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = navBottom + 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.get("menu_pill", "Menu"),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        letterSpacing = (-1.1).sp,
                        color = palette.plum,
                    )
                    Surface(
                        onClick = onOpenCart,
                        shape = RoundedCornerShape(999.dp),
                        color = if (uiState.cartItemCount > 0) palette.acc else palette.alt,
                        modifier = Modifier.testTag("menu_open_cart_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = s.get("cart_title", "Your Order"),
                                tint = if (uiState.cartItemCount > 0) palette.onAcc else palette.plum,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = uiState.cartItemCount.toString(),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (uiState.cartItemCount > 0) palette.onAcc else palette.plum,
                            )
                        }
                    }
                }
            }

            item {
                SelectedPlaceBanner(
                    selectedPlace = place,
                    onChangeClick = onChangePlace,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = s.get("menu_search_hint", "Search food or drink…"),
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
                        .testTag("menu_search_input"),
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { category ->
                        val selected = category == uiState.selectedCategory
                        Surface(
                            onClick = { onSelectCategory(category) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) palette.acc else palette.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(width = 88.dp, height = 68.dp)
                                .testTag("category_${category.lowercase().replace(" ", "_")}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = categoryIconVector(category),
                                    contentDescription = null,
                                    tint = if (selected) palette.onAcc else StatusReadyText,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = s.menuCategoryLabel(category),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (selected) palette.onAcc else palette.plum,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            when (val menuState = uiState.menuItemsState) {
                is UiState.Loading -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
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
                is UiState.Success -> {
                    val rows = filteredItems.chunked(columns)
                    if (rows.isEmpty()) {
                        item {
                            Text(
                                text = s.get("menu_empty", "No dishes match."),
                                fontFamily = font,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = palette.mut,
                                modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
                            )
                        }
                    } else {
                        items(rows) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowItems.forEach { item ->
                                    val qty = cartMap[item.id] ?: 0
                                    val isSaved = uiState.savedItems.containsKey(item.id)
                                    MenuDishCard(
                                        item = item,
                                        qty = qty,
                                        isSaved = isSaved,
                                        onAdd = { onAddToCart(item) },
                                        onDecrement = { onDecrementCart(item.id) },
                                        onToggleSaved = { onToggleSaved(item) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(Modifier.weight(1f))
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
private fun MenuDishCard(
    item: MenuItem,
    qty: Int,
    isSaved: Boolean,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val dishTitle = s.dishName(item)

    Surface(
        onClick = onAdd,
        enabled = item.available,
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = when {
            qty > 0 -> BorderStroke(2.dp, palette.acc)
            else -> BorderStroke(1.dp, palette.line.copy(alpha = 0.6f))
        },
        shadowElevation = 2.dp,
        modifier = modifier.testTag("menu_item_${item.id}"),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.alt),
            ) {
                if (item.imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = dishTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Icon(
                                imageVector = categoryIconVector(item.category),
                                contentDescription = dishTitle,
                                tint = palette.plum.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(28.dp),
                            )
                        },
                        error = {
                            Icon(
                                imageVector = categoryIconVector(item.category),
                                contentDescription = dishTitle,
                                tint = palette.plum.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(28.dp),
                            )
                        },
                    )
                }

                if (!item.available) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(StatusCancelledBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = s.get("saved_unavailable", "Sold out"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = StatusCancelledText,
                        )
                    }
                } else if (item.imageUrl.isBlank()) {
                    Icon(
                        imageVector = categoryIconVector(item.category),
                        contentDescription = dishTitle,
                        tint = palette.plum.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(24.dp),
                    )
                }

                // Bookmark button in top-right of dish image box (48dp touch target)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.surface.copy(alpha = 0.88f))
                        .clickable(onClick = onToggleSaved)
                        .semantics {
                            contentDescription = if (isSaved) {
                                s.get("menu_remove_saved", "Remove saved")
                            } else {
                                s.get("menu_save_item", "Save item")
                            }
                        }
                        .testTag("bookmark_item_${item.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isSaved) palette.plum else palette.mut,
                        modifier = Modifier.size(18.dp),
                    )
                }

                if (item.available) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.acc)
                            .clickable(onClick = onAdd)
                            .semantics { contentDescription = "Add $dishTitle" }
                            .testTag("menu_add_${item.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = palette.onAcc,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = dishTitle,
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatEtb(item.price),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = palette.plum,
                )
            }
        }
    }
}
