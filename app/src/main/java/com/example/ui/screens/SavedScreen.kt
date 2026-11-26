package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MenuItem
import com.example.data.model.SavedMenuItem
import com.example.data.model.UserRole
import com.example.localization.LocalAppStrings
import com.example.ui.components.FoodImageBox
import com.example.ui.components.HeroSection
import com.example.ui.components.formatEtb
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.lakemarkFont

@Composable
fun SavedScreen(
    savedItems: Map<String, SavedMenuItem>,
    liveMenuItems: List<MenuItem>,
    activeRole: UserRole,
    onAddToCart: (MenuItem) -> Unit,
    onRemoveSaved: (MenuItem) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val liveById = liveMenuItems.associateBy { it.id }
    val orderedSaved = savedItems.values.sortedByDescending { it.savedAt?.seconds ?: 0L }
    val canOrderFromMenu = com.example.data.model.AppTab.MENU in activeRole.allowedTabs

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("saved_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            HeroSection(
                pillText = s.get("saved_pill", "Saved"),
                bigNumber = orderedSaved.size,
                unitLabel = s.get("saved_unit", "saved"),
                subtitle = s.get("saved_subtitle", "your bookmarked Lakemark dishes"),
            )
        }

        if (orderedSaved.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_empty_card"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = palette.plum,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = s.get("saved_empty_title", "No saved dishes yet"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = palette.text,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = s.get("saved_empty_hint", "Tap the bookmark icon on any dish in Menu to save it here for quick ordering."),
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = palette.mut,
                            textAlign = TextAlign.Center,
                        )
                        if (canOrderFromMenu) {
                            Button(
                                onClick = onOpenMenu,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("saved_browse_menu_button"),
                            ) {
                                Text(
                                    text = s.get("saved_open_menu_btn", "Browse Menu"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            items(orderedSaved, key = { it.menuItemId.ifBlank { it.id } }) { saved ->
                val itemId = saved.menuItemId.ifBlank { saved.id }
                val liveItem = liveById[itemId]
                val displayItem = liveItem ?: MenuItem(
                    id = itemId,
                    name = saved.name,
                    category = saved.category,
                    price = saved.price,
                    available = true,
                    imageUrl = saved.imageUrl,
                )
                val isAvailable = liveItem?.available ?: true
                val stillOnMenu = liveById.isEmpty() || liveItem != null

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_item_$itemId"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FoodImageBox(
                            imageUrl = displayItem.imageUrl,
                            category = displayItem.category,
                            dishName = s.dishName(displayItem),
                            palette = palette,
                            size = 54.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.dishName(displayItem),
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${s.menuCategoryLabel(displayItem.category)} · ${s.format("currency_etb", "amount" to displayItem.price)}",
                                fontFamily = font,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = palette.mut,
                            )
                            if (!stillOnMenu) {
                                Text(
                                    text = s.get("saved_unavailable", "Currently off menu"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = StatusCancelledText,
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRemoveSaved(displayItem) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("saved_remove_$itemId"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = s.get("menu_remove_saved", "Remove saved"),
                                tint = palette.plum,
                            )
                        }
                        if (canOrderFromMenu && stillOnMenu) {
                            if (isAvailable) {
                                Button(
                                    onClick = { onAddToCart(displayItem) },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.acc,
                                        contentColor = palette.onAcc,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("saved_add_$itemId"),
                                ) {
                                    Text(
                                        text = s.get("menu_add", "+ Add"),
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = StatusCancelledBg,
                                    ) {
                                        Text(
                                            text = s.get("orders_off_duty", "Sold out"),
                                            fontFamily = font,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.5.sp,
                                            color = StatusCancelledText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
