package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class UserRole(
    val key: String,
    val displayName: String,
    val defaultStaffName: String,
    val primaryHex: String,
    val secondaryHex: String,
    val accentHex: String,
) {
    MANAGER("manager", "Manager", "Hotel Manager", "#FAF3E6", "#3D0F4F", "#04C353"),
    RECEPTION("reception", "Reception", "Reception Desk", "#D6EEEA", "#124A47", "#F2A31B"),
    KITCHEN("kitchen", "Kitchen", "Head Chef", "#FBE2CE", "#7A2B0A", "#F26B1D"),
    WAITER("waiter", "Waiter", "Waiter", "#DCE7FB", "#1B3A86", "#2BB3E8"),
    CASHIER("cashier", "Cashier", "Cashier", "#F5E9C4", "#5A4208", "#D9A21B"),
    DELIVERY("delivery", "Delivery", "Delivery Rider", "#F8DCE6", "#7E1743", "#F0436B"),
    CUSTOMER("customer", "Customer", "Guest", "#DBF2E2", "#14502B", "#1FB55A");

    val allowedTabs: List<AppTab>
        get() = when (this) {
            CUSTOMER, RECEPTION, WAITER -> listOf(AppTab.HOME, AppTab.FLOOR, AppTab.MENU, AppTab.SAVED, AppTab.ORDERS)
            CASHIER, DELIVERY -> listOf(AppTab.HOME, AppTab.SAVED, AppTab.ORDERS)
            KITCHEN -> listOf(AppTab.HOME, AppTab.KITCHEN, AppTab.SAVED)
            MANAGER -> listOf(AppTab.HOME, AppTab.FLOOR, AppTab.MENU, AppTab.SAVED, AppTab.ORDERS, AppTab.KITCHEN, AppTab.MANAGE)
        }

    val bottomBarTabs: List<AppTab>
        get() = when (this) {
            MANAGER -> listOf(AppTab.HOME, AppTab.MANAGE)
            KITCHEN -> listOf(AppTab.HOME, AppTab.KITCHEN)
            CASHIER, DELIVERY -> listOf(AppTab.HOME, AppTab.ORDERS)
            CUSTOMER, RECEPTION, WAITER -> listOf(AppTab.MENU, AppTab.ORDERS)
        }

    val directActions: List<OrderStatus>
        get() = when (this) {
            MANAGER -> listOf(
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.READY,
                OrderStatus.CANCELLED,
                OrderStatus.DONE,
            )
            KITCHEN -> listOf(OrderStatus.PREPARING, OrderStatus.READY)
            RECEPTION -> listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.DONE)
            WAITER, DELIVERY -> listOf(OrderStatus.DONE)
            CASHIER, CUSTOMER -> emptyList()
        }

    val isStaff: Boolean get() = this != CUSTOMER

    fun defaultBrandConfig(): BrandConfig = BrandConfig(
        roleKey = key,
        primaryHex = primaryHex,
        secondaryHex = secondaryHex,
        accentHex = accentHex,
        brandName = displayName,
        mark = "",
    )

    companion object {
        fun fromKey(raw: String?): UserRole =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class AppTab(val key: String, val label: String) {
    HOME("home", "Home"),
    FLOOR("floor", "Floor"),
    MENU("menu", "Menu"),
    SAVED("saved", "Saved"),
    ORDERS("orders", "Orders"),
    KITCHEN("kitchen", "Kitchen"),
    MANAGE("manage", "Manage"),
}

enum class ManageSection(val key: String, val label: String) {
    OVERVIEW("overview", "Overview"),
    MENU("menu", "Menu"),
    TEAM("team", "Team"),
    LANGUAGE("language", "Language"),
    ALERTS("alerts", "Alerts"),
}

enum class TeamSubSection(val key: String, val label: String) {
    ACCOUNTS("accounts", "Accounts"),
    NEW_REGISTERED("new_registered", "New Users"),
    COVERAGE("coverage", "Waiters"),
    BRANDS("brands", "Brands"),
}

enum class WaiterArea(val key: String, val label: String, val index: Int) {
    DIN("din", "Restaurant", 0),
    LOB("lob", "Lobby", 1),
    F1("f1", "Floor 1", 2),
    F2("f2", "Floor 2", 3);

    companion object {
        fun fromKey(raw: String?): WaiterArea? =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) }
    }
}

@Keep
@IgnoreExtraProperties
data class WaiterCoverage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val waiterName: String = "",
    val onDuty: Boolean = true,
    val areas: List<String> = listOf(WaiterArea.F1.key),
    val updatedAt: Timestamp? = null,
) {
    val areaSet: Set<WaiterArea>
        get() = areas.mapNotNull { WaiterArea.fromKey(it) }.toSet()

    companion object {
        fun create(
            userId: String,
            waiterName: String = "",
            onDuty: Boolean = true,
            areas: Set<WaiterArea> = setOf(WaiterArea.F1),
        ): WaiterCoverage = WaiterCoverage(
            id = userId,
            userId = userId,
            waiterName = waiterName,
            onDuty = onDuty,
            areas = areas.sortedBy { it.index }.map { it.key },
        )
    }
}

enum class OrderStatus(val key: String, val label: String) {
    NEW("new", "At reception"),
    CONFIRMED("confirmed", "Received by kitchen"),
    PREPARING("preparing", "Preparing"),
    READY("ready", "Ready"),
    DONE("done", "Completed"),
    CANCELLED("cancelled", "Cancelled");

    val nextStatuses: List<OrderStatus>
        get() = when (this) {
            NEW -> listOf(CONFIRMED, CANCELLED)
            CONFIRMED -> listOf(PREPARING)
            PREPARING -> listOf(READY)
            READY -> listOf(DONE)
            DONE, CANCELLED -> emptyList()
        }

    val actionLabel: String
        get() = when (this) {
            CONFIRMED -> "Send to kitchen"
            PREPARING -> "Start preparing"
            READY -> "Mark ready"
            DONE -> "Confirm pickup"
            CANCELLED -> "Cancel"
            NEW -> "New"
        }

    val progressFraction: Float
        get() = when (this) {
            NEW -> 0.12f
            CONFIRMED -> 0.25f
            PREPARING -> 0.55f
            READY -> 0.85f
            DONE -> 1.0f
            CANCELLED -> 0f
        }

    val isTerminal: Boolean get() = this == DONE || this == CANCELLED

    companion object {
        fun fromKey(raw: String?): OrderStatus =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: NEW
    }
}

enum class OrderType(val key: String, val label: String, val locationPrompt: String) {
    ROOM("room", "Room service", "Room number"),
    DINE("dine", "Dine-in", "Table number"),
    PICKUP("pickup", "Reception", "Reception table"),
    OUT("out", "Takeaway / delivery", "Phone number");

    companion object {
        fun fromKey(raw: String?): OrderType =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: DINE
    }
}

enum class FloorZone(val key: String, val title: String, val orderType: OrderType, val capacity: Int) {
    HOTEL("hotel", "Rooms", OrderType.ROOM, 32),
    DINE("dine", "Restaurant", OrderType.DINE, 12),
    REC("rec", "Reception", OrderType.PICKUP, 20);

    val locations: List<String>
        get() = when (this) {
            HOTEL -> (1..16).map { "${100 + it}" } + (1..16).map { "${200 + it}" }
            DINE -> (1..12).map { "Table $it" }
            REC -> (1..20).map { "Reception T$it" }
        }

    fun shortTileLabel(location: String): String = when (this) {
        HOTEL -> location
        DINE -> location.removePrefix("Table ")
        REC -> "T" + location.removePrefix("Reception T")
    }

    fun fullLabel(location: String): String = when (this) {
        HOTEL -> "Room $location"
        DINE -> location
        REC -> location
    }
}

data class PlaceSelection(
    val type: OrderType,
    val location: String,
    val label: String,
)

@Keep
@IgnoreExtraProperties
data class StaffUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val staffPassword: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val role: String = UserRole.CUSTOMER.key,
    val active: Boolean = false,
    val permanentlyActivated: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    val userRole: UserRole get() = UserRole.fromKey(role)
}

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "Classics",
    val description: String = "",
    val price: Long = 0L,
    val available: Boolean = true,
    val sortOrder: Long = 0L,
    val imageUrl: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedMenuItem(
    @DocumentId val id: String = "",
    val menuItemId: String = "",
    val name: String = "",
    val category: String = "",
    val price: Long = 0L,
    val imageUrl: String = "",
    val savedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val menuItemId: String = "",
    val name: String = "",
    val quantity: Long = 1L,
    val price: Long = 0L,
    val note: String = "",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "menuItemId" to menuItemId,
        "name" to name,
        "quantity" to quantity,
        "price" to price,
        "note" to note,
    )
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: Long = 0L,
    val userId: String = "",
    val createdByRole: String = UserRole.CUSTOMER.key,
    val lastUpdatedByRole: String = UserRole.CUSTOMER.key,
    val lastUpdatedByUid: String = "",
    val customerName: String = "",
    val orderType: String = OrderType.DINE.key,
    val location: String = "",
    val status: String = OrderStatus.NEW.key,
    val items: List<OrderLineItem> = emptyList(),
    val specialNote: String = "",
    val cancelReason: String = "",
    val assignedWaiterId: String = "",
    val assignedMethod: String = "",
    val subtotal: Long = 0L,
    val total: Long = 0L,
    val createdAt: Timestamp? = null,
    val confirmedAt: Timestamp? = null,
    val preparingAt: Timestamp? = null,
    val readyAt: Timestamp? = null,
    val doneAt: Timestamp? = null,
    val cancelledAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    val statusEnum: OrderStatus get() = OrderStatus.fromKey(status)
    val typeEnum: OrderType get() = OrderType.fromKey(orderType)
    val createdByRoleEnum: UserRole get() = UserRole.fromKey(createdByRole)
    val lastUpdatedByRoleEnum: UserRole get() = UserRole.fromKey(lastUpdatedByRole)

    val ticketToken: String
        get() = when (typeEnum) {
            OrderType.ROOM -> location
            OrderType.DINE -> "T" + location.filter { it.isDigit() }.ifBlank { location }
            OrderType.PICKUP -> "R" + location.filter { it.isDigit() }.ifBlank { location }
            OrderType.OUT -> "OUT"
        }

    fun areaForOrder(): WaiterArea? = when (typeEnum) {
        OrderType.ROOM -> if (location.trim().startsWith("1")) WaiterArea.F1 else WaiterArea.F2
        OrderType.DINE -> WaiterArea.DIN
        OrderType.PICKUP -> WaiterArea.LOB
        OrderType.OUT -> null
    }
}

@Keep
@IgnoreExtraProperties
data class BrandConfig(
    @DocumentId val id: String = "",
    val roleKey: String = UserRole.MANAGER.key,
    val primaryHex: String = "#FAF3E6",
    val secondaryHex: String = "#3D0F4F",
    val accentHex: String = "#04C353",
    val brandName: String = "Manager",
    val mark: String = "",
    val updatedBy: String = "",
    val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(uid: String): Map<String, Any> = mapOf(
        "roleKey" to roleKey,
        "primaryHex" to primaryHex,
        "secondaryHex" to secondaryHex,
        "accentHex" to accentHex,
        "brandName" to brandName.take(18),
        "mark" to mark.take(2),
        "updatedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderRole: String = UserRole.MANAGER.key,
    val senderBrandName: String = "Manager",
    val message: String = "",
    val targetRoles: List<String> = listOf("all"),
    val targetUserId: String = "",
    val orderId: String = "",
    val kind: String = "ping",
    val apkUrl: String = "",
    val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AppReleaseConfig(
    @DocumentId val id: String = "release",
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val apkDownloadUrl: String = "",
    val apkSha256: String = "",
    val releaseNotes: String = "",
    val mandatory: Boolean = false,
    val updatedBy: String = "",
    val updatedAt: Timestamp? = null,
)

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int,
)

object DefaultMenuCatalog {
    val items: List<MenuItem> = listOf(
        MenuItem("m0", "Doro Wat", "Classics", "Chicken in berbere, egg, injera", 420L, true, 0L),
        MenuItem("m1", "Kitfo", "Classics", "Minced beef, mitmita, niter kibbeh", 480L, true, 1L),
        MenuItem("m2", "Siga Tibs", "Classics", "Sautéed beef, onion, rosemary", 450L, true, 2L),
        MenuItem("m3", "Shiro Wat", "Classics", "Chickpea stew, injera (fasting)", 260L, true, 3L),
        MenuItem("m4", "Fish Fillet", "Lake Tana", "Fresh Lake Tana fillet, lemon, garlic", 380L, true, 4L),
        MenuItem("m5", "Fish Cutlet", "Lake Tana", "Crisp cutlets, spiced tartare", 320L, true, 5L),
        MenuItem("m6", "Club Sandwich", "Grill", "Chicken, egg, tomato, chips", 300L, true, 6L),
        MenuItem("m7", "Beef Burger", "Grill", "Patty, cheese, house sauce, chips", 350L, true, 7L),
        MenuItem("m8", "Fuul", "Breakfast", "Fava beans, tomato, chilli, bread", 180L, true, 8L),
        MenuItem("m9", "Firfir", "Breakfast", "Shredded injera, spiced sauce", 220L, true, 9L),
        MenuItem("m10", "Buna", "Drinks", "Ethiopian coffee in a jebena", 70L, true, 10L),
        MenuItem("m11", "Spris Juice", "Drinks", "Avocado, mango, papaya", 110L, true, 11L),
    )

    val categories: List<String> = listOf("All", "Classics", "Lake Tana", "Grill", "Breakfast", "Drinks")
}

object AlertTargets {
    fun recipientsForStatus(status: OrderStatus): List<String> = when (status) {
        OrderStatus.CONFIRMED -> listOf("kitchen", "customer", "manager")
        OrderStatus.PREPARING -> listOf("customer", "reception", "manager")
        OrderStatus.READY -> listOf("reception", "waiter", "cashier", "delivery", "customer", "manager")
        OrderStatus.DONE -> listOf("kitchen", "customer", "reception", "waiter", "cashier", "delivery", "manager")
        OrderStatus.CANCELLED -> listOf("reception", "customer", "manager")
        OrderStatus.NEW -> listOf("reception", "kitchen", "manager")
    }

    fun messageForStatus(orderNumber: Long, location: String, status: OrderStatus, reason: String = ""): String =
        when (status) {
            OrderStatus.NEW -> "New order #$orderNumber placed · $location"
            OrderStatus.CONFIRMED -> "Order #$orderNumber received by the kitchen · $location"
            OrderStatus.PREPARING -> "Order #$orderNumber is being prepared · $location"
            OrderStatus.READY -> "Order #$orderNumber is ready · $location"
            OrderStatus.DONE -> "Order #$orderNumber completed · $location"
            OrderStatus.CANCELLED -> if (reason.isNotBlank()) {
                "Order #$orderNumber cancelled by kitchen: $reason"
            } else {
                "Order #$orderNumber cancelled · $location"
            }
        }
}
