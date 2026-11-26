package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.AppTab
import com.example.data.model.BrandConfig
import com.example.data.model.DefaultMenuCatalog
import com.example.data.model.FloorZone
import com.example.data.model.ManageSection
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PlaceSelection
import com.example.data.model.StaffUser
import com.example.data.model.TeamSubSection
import com.example.data.model.UserRole
import com.example.data.model.WaiterArea
import com.example.data.model.WaiterCoverage
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FloorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KitchenScreen
import com.example.ui.screens.ManageScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.SettingsSheet
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.LakemarkTheme
import com.example.ui.theme.deriveBrandPalette
import com.example.ui.theme.relativeLuminance
import com.example.ui.viewmodel.LakemarkUiState
import com.example.ui.viewmodel.UiState
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class LakemarkTests {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleOrders(): List<Order> = listOf(
        Order(
            id = "o1001",
            orderNumber = 1001L,
            userId = "u1",
            createdByRole = "customer",
            customerName = "Meron K.",
            orderType = OrderType.ROOM.key,
            location = "214",
            status = OrderStatus.PREPARING.key,
            items = listOf(OrderLineItem("m1", "Kitfo", 1L, 480L)),
            subtotal = 480L,
            total = 552L,
            createdAt = Timestamp.now(),
        ),
        Order(
            id = "o1002",
            orderNumber = 1002L,
            userId = "u2",
            createdByRole = "waiter",
            customerName = "Dawit A.",
            orderType = OrderType.DINE.key,
            location = "Table 3",
            status = OrderStatus.READY.key,
            items = listOf(OrderLineItem("m4", "Fish Fillet", 2L, 380L)),
            subtotal = 760L,
            total = 874L,
            createdAt = Timestamp.now(),
        ),
    )

    private fun sampleState(): LakemarkUiState = LakemarkUiState(
        currentUser = StaffUser(id = "u1", userId = "u1", name = "Hotel Manager", email = "manager@lakemark.et", role = "manager"),
        activeRole = UserRole.MANAGER,
        isAdminUser = true,
        menuItemsState = UiState.Success(DefaultMenuCatalog.items),
        ordersState = UiState.Success(sampleOrders()),
        selectedPlace = PlaceSelection(OrderType.DINE, "Table 3", "Table 3"),
        staffUsers = listOf(
            StaffUser(id = "u1", userId = "u1", name = "Hotel Manager", email = "manager@lakemark.et", role = "manager", active = true),
            StaffUser(id = "local_waiter", userId = "local_waiter", name = "Waiter 1", email = "waiter1@lakemark.et", role = "waiter", active = true),
            StaffUser(id = "local_guest", userId = "local_guest", name = "Guest", email = "guest@lakemark.et", role = "customer", active = false),
        ),
        waiterUsers = listOf(
            StaffUser(id = "local_waiter", userId = "local_waiter", name = "Waiter 1", email = "waiter1@lakemark.et", role = "waiter", active = true),
        ),
        waiterCoverage = mapOf(
            "local_waiter" to WaiterCoverage.create("local_waiter", "Waiter 1", onDuty = true, areas = setOf(WaiterArea.F1, WaiterArea.DIN)),
        ),
    )

    @Test
    fun `brand palette derivation satisfies 60-30-10 luminance constraints for all roles`() {
        UserRole.entries.forEach { role ->
            val cfg = role.defaultBrandConfig()
            val palette = deriveBrandPalette(cfg)
            assertEquals(role, palette.roleKey)
            assertTrue("${role.key} primary must be light", relativeLuminance(palette.bg) >= 0.55)
            assertTrue("${role.key} secondary must be dark", relativeLuminance(palette.plum) <= 0.22)
        }
        val custom = BrandConfig(roleKey = "kitchen", primaryHex = "#FFF8EE", secondaryHex = "#24140B", accentHex = "#F26B1D", brandName = "Grill Chef", mark = "GC")
        val derived = deriveBrandPalette(custom)
        assertEquals("Grill Chef", derived.brandName)
        assertEquals("GC", derived.mark)
    }

    @Test
    fun `floor zones and order ticket tokens match Lakemark specification`() {
        assertEquals(32, FloorZone.HOTEL.locations.size)
        assertEquals(12, FloorZone.DINE.locations.size)
        assertEquals(20, FloorZone.REC.locations.size)
        assertEquals("101", FloorZone.HOTEL.locations.first())
        assertEquals("216", FloorZone.HOTEL.locations.last())

        val orders = sampleOrders()
        assertEquals("214", orders[0].ticketToken)
        assertEquals("T3", orders[1].ticketToken)
        assertEquals("OUT", Order(orderType = OrderType.OUT.key, location = "0911000000").ticketToken)
    }

    @Test
    fun `role permissions and order state transitions enforce zero-trust workflow`() {
        assertEquals(listOf(AppTab.HOME, AppTab.KITCHEN, AppTab.SAVED), UserRole.KITCHEN.allowedTabs)
        assertEquals(listOf(AppTab.HOME, AppTab.KITCHEN), UserRole.KITCHEN.bottomBarTabs)
        assertEquals(
            listOf(AppTab.HOME, AppTab.FLOOR, AppTab.MENU, AppTab.SAVED, AppTab.ORDERS, AppTab.KITCHEN, AppTab.MANAGE),
            UserRole.MANAGER.allowedTabs,
        )
        assertEquals(listOf(AppTab.HOME, AppTab.MANAGE), UserRole.MANAGER.bottomBarTabs)
        UserRole.entries.forEach { role ->
            assertEquals("${role.key} footer must stay 2 items", 2, role.bottomBarTabs.size)
        }
        assertEquals(listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED), OrderStatus.NEW.nextStatuses)
        assertEquals(emptyList<OrderStatus>(), OrderStatus.DONE.nextStatuses)
        assertTrue(OrderStatus.CANCELLED.isTerminal)
    }

    @Test
    fun `waiter coverage and order area mapping parse floor zones cleanly`() {
        val coverage = WaiterCoverage.create(
            userId = "w1",
            waiterName = "Waiter 1",
            onDuty = true,
            areas = setOf(WaiterArea.F1, WaiterArea.DIN),
        )
        assertEquals(setOf(WaiterArea.F1, WaiterArea.DIN), coverage.areaSet)
        assertEquals(WaiterArea.F2, Order(orderType = OrderType.ROOM.key, location = "214").areaForOrder())
        assertEquals(WaiterArea.F1, Order(orderType = OrderType.ROOM.key, location = "105").areaForOrder())
        assertEquals(WaiterArea.DIN, Order(orderType = OrderType.DINE.key, location = "Table 3").areaForOrder())
        assertEquals(WaiterArea.LOB, Order(orderType = OrderType.PICKUP.key, location = "Lounge 1").areaForOrder())
    }

    @Test
    fun `splash and auth screens render Google sign-in without demo accounts`() {
        var started by mutableStateOf(false)
        var googleSignInClicked by mutableStateOf(false)
        composeRule.setContent {
            LakemarkTheme {
                if (!started) {
                    SplashScreen(onGetStarted = { started = true })
                } else {
                    AuthScreen(
                        isLoading = false,
                        errorMessage = null,
                        onGoogleSignIn = { googleSignInClicked = true },
                        onBackToSplash = { started = false },
                    )
                }
            }
        }

        composeRule.onNodeWithText("LAKEMARK").assertIsDisplayed()
        composeRule.onNodeWithText("Kitchen Management").assertIsDisplayed()
        composeRule.onNodeWithTag("get_started_button").performClick()
        assertTrue(started)

        composeRule.onNodeWithText("Welcome to Lakemark").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_mode_google").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manager Login").assertIsDisplayed()
        composeRule.onAllNodesWithText("Customer & Manager").assertCountEquals(0)
        composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()
        composeRule.onAllNodesWithText("Demo accounts · tap to fill").assertCountEquals(0)

        composeRule.onNodeWithTag("google_sign_in_button").performClick()
        assertTrue(googleSignInClicked)
    }

    @Test
    fun `settings sheet hides test sound buttons and manual sync and displays v3_2_0 footer`() {
        val state = sampleState()
        composeRule.setContent {
            LakemarkTheme {
                SettingsSheet(
                    uiState = state,
                    hasNotificationPermission = true,
                    onClose = {},
                    onSignOut = {},
                    onSwitchAdminRole = {},
                    onOpenMyBrand = {},
                    onToggleSound = {},
                    onToggleVibration = {},
                    onVolumeChange = {},
                    onToggleWaiterDuty = {},
                    onRequestNotificationPermission = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Test new order").assertCountEquals(0)
        composeRule.onAllNodesWithText("Test ready").assertCountEquals(0)
        composeRule.onAllNodesWithText("Sync").assertCountEquals(0)
        composeRule.onNodeWithTag("settings_app_version").performScrollTo()
        composeRule.onNodeWithText("Lakemark v${BuildConfig.VERSION_NAME} · Bahir Dar").assertIsDisplayed()
    }

    @Test
    fun `home screen renders hero occupancy and latest orders`() {
        val state = sampleState()
        composeRule.setContent {
            LakemarkTheme {
                HomeScreen(uiState = state, onNavigateTab = {}, onOpenArea = {})
            }
        }
        composeRule.onNodeWithTag("area_card_hotel").assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Latest orders"))
        composeRule.onNodeWithText("Latest orders").assertIsDisplayed()
    }

    @Test
    fun `floor screen renders hotel room tiles across both floors`() {
        val state = sampleState()
        composeRule.setContent {
            LakemarkTheme {
                FloorScreen(uiState = state, onSelectZone = {}, onSelectPlace = { _, _ -> }, onChangePlaceClick = {})
            }
        }
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("floor_tile_214"))
        composeRule.onNodeWithTag("floor_tile_214").assertIsDisplayed()
    }

    @Test
    fun `menu orders kitchen and manage sections render cleanly`() {
        var currentTab by mutableStateOf(AppTab.MENU)
        var manageSection by mutableStateOf(ManageSection.OVERVIEW)
        var teamSubSection by mutableStateOf(TeamSubSection.ACCOUNTS)
        val baseState = sampleState()
        composeRule.setContent {
            LakemarkTheme {
                val state = baseState.copy(
                    manageSection = manageSection,
                    teamSubSection = teamSubSection,
                )
                when (currentTab) {
                    AppTab.MENU -> MenuScreen(
                        uiState = state,
                        onStartArea = {},
                        onChangePlace = {},
                        onSelectCategory = {},
                        onSearchChange = {},
                        onAddToCart = {},
                        onDecrementCart = {},
                    )
                    AppTab.ORDERS -> OrdersScreen(
                        uiState = state,
                        onSelectFilter = {},
                        onSelectZoneFilter = {},
                        onAdvanceOrder = { _, _ -> },
                        onCancelRequest = {},
                    )
                    AppTab.KITCHEN -> KitchenScreen(
                        uiState = state.copy(kitchenStageFilter = OrderStatus.PREPARING),
                        onSelectStage = {},
                        onAdvanceOrder = { _, _ -> },
                        onCancelRequest = {},
                    )
                    AppTab.MANAGE -> ManageScreen(
                        uiState = state,
                        onSelectSection = { manageSection = it },
                        onSelectTeamSubSection = { teamSubSection = it },
                        onSendBroadcast = { _, _, _ -> },
                        onOpenBrandEditor = {},
                        onToggleUserActive = {},
                        onUpdateStaffRole = { _, _ -> },
                    )
                    else -> Unit
                }
            }
        }

        composeRule.onNodeWithText("Doro Wat").assertIsDisplayed()
        composeRule.onNodeWithText("ETB 420").assertIsDisplayed()
        composeRule.onAllNodesWithText("Chicken in berbere, egg, injera").assertCountEquals(0)

        composeRule.runOnUiThread { currentTab = AppTab.ORDERS }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("order_row_1001").assertIsDisplayed()

        composeRule.runOnUiThread { currentTab = AppTab.KITCHEN }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("kitchen_ticket_1001").assertIsDisplayed()

        composeRule.runOnUiThread { currentTab = AppTab.MANAGE }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overview_kpi_today").assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("manage_reports_card"))
        composeRule.onNodeWithTag("manage_reports_card").assertIsDisplayed()

        composeRule.onNodeWithTag("manage_tab_menu").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("manage_menu_switch_m0").assertIsDisplayed()

        composeRule.onNodeWithTag("manage_tab_team").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("approve_customer_local_guest"))
        composeRule.onNodeWithTag("approve_customer_local_guest").assertIsDisplayed()

        composeRule.onNodeWithTag("team_subtab_coverage").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("waiter_duty_local_waiter").assertIsDisplayed()
        composeRule.onNodeWithTag("waiter_area_local_waiter_din").assertIsDisplayed()

        composeRule.onNodeWithTag("team_subtab_brands").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("manage_brand_manager").assertIsDisplayed()

        composeRule.onNodeWithTag("manage_tab_alerts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send to staff").assertIsDisplayed()
    }
}
