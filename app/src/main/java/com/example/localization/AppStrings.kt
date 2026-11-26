package com.example.localization

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppTab
import com.example.data.model.FloorZone
import com.example.data.model.ManageSection
import com.example.data.model.MenuItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.TeamSubSection
import com.example.data.model.UserRole
import com.example.data.model.WaiterArea
import com.google.firebase.Timestamp

enum class AppLanguage(val code: String, val nativeName: String, val shortCode: String) {
    EN("en", "English", "EN"),
    AM("am", "አማርኛ", "አማ");

    companion object {
        fun fromCode(raw: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(raw?.trim(), ignoreCase = true) } ?: EN
    }
}

enum class TranslationCategory(val key: String, val labelEn: String, val labelAm: String) {
    BRAND("Brand & App", "Brand & App", "ብራንድ እና መተግበሪያ"),
    NAV("Navigation", "Navigation", "ዳሰሳ"),
    AUTH("Auth & Access", "Auth & Access", "መግቢያ እና ፍቃድ"),
    HOME("Home & Floor", "Home & Floor", "መነሻ እና ወለል"),
    MENU("Menu & Saved", "Menu & Saved", "ምናሌ እና የተቀመጡ"),
    ORDERS("Orders & Kitchen", "Orders & Kitchen", "ትዕዛዞች እና ወጥ ቤት"),
    MANAGE("Manage & Team", "Manage & Team", "አስተዳደር እና ቡድን"),
    PROFILE("Profile & Settings", "Profile & Settings", "መገለጫ እና ቅንብሮች"),
    STATUS("Roles & Statuses", "Roles & Statuses", "ሚናዎች እና ሁኔታዎች"),
    VALIDATION("Alerts & Validation", "Alerts & Validation", "ማሳወቂያ እና ማረጋገጫ"),
}

enum class TranslationStatus(val key: String) {
    TRANSLATED("translated"),
    UNTRANSLATED("untranslated"),
    OUTDATED("outdated"),
    MISSING("missing"),
}

data class TranslationItemSpec(
    val key: String,
    val category: TranslationCategory,
    val en: String,
    val am: String,
    val enVersion: Int = 1,
)

data class RemoteTranslation(
    val key: String = "",
    val category: String = "",
    val en: String = "",
    val am: String = "",
    val enVersion: Int = 1,
    val updatedBy: String = "",
    val updatedAt: Timestamp? = null,
)

@Immutable
data class TranslationRecord(
    val key: String,
    val category: TranslationCategory,
    val en: String,
    val am: String,
    val defaultEn: String,
    val defaultAm: String,
    val enVersion: Int,
    val currentEnVersion: Int,
    val isCustomized: Boolean,
    val status: TranslationStatus,
    val updatedAt: Timestamp? = null,
)

@Immutable
data class AppStrings(
    val language: AppLanguage,
    private val resolved: Map<String, String>,
) {
    fun get(key: String, fallback: String = key): String =
        resolved[key]?.takeIf { it.isNotBlank() } ?: fallback

    fun format(key: String, vararg args: Pair<String, Any?>): String {
        var text = get(key, key)
        args.forEach { (placeholder, value) ->
            text = text.replace("{$placeholder}", value?.toString().orEmpty())
        }
        return text
    }

    fun roleName(role: UserRole): String = when (role) {
        UserRole.MANAGER -> get("role_manager", role.displayName)
        UserRole.RECEPTION -> get("role_reception", role.displayName)
        UserRole.KITCHEN -> get("role_kitchen", "Kitchen Staff")
        UserRole.WAITER -> get("role_waiter", role.displayName)
        UserRole.CASHIER -> get("role_cashier", role.displayName)
        UserRole.DELIVERY -> get("role_delivery", role.displayName)
        UserRole.CUSTOMER -> get("role_customer", role.displayName)
    }

    fun tabLabel(tab: AppTab): String = when (tab) {
        AppTab.HOME -> get("nav_home", tab.label)
        AppTab.FLOOR -> get("nav_floor", tab.label)
        AppTab.MENU -> get("nav_menu", tab.label)
        AppTab.SAVED -> get("nav_saved", "Saved")
        AppTab.ORDERS -> get("nav_orders", tab.label)
        AppTab.KITCHEN -> get("nav_kitchen", tab.label)
        AppTab.MANAGE -> get("nav_manage", tab.label)
    }

    fun manageSectionLabel(section: ManageSection): String = when (section) {
        ManageSection.OVERVIEW -> get("manage_tab_overview", section.label)
        ManageSection.MENU -> get("manage_tab_menu", section.label)
        ManageSection.TEAM -> get("manage_tab_team", section.label)
        ManageSection.LANGUAGE -> get("manage_tab_language", "Language")
        ManageSection.ALERTS -> get("manage_tab_alerts", section.label)
    }

    fun teamSubSectionLabel(sub: TeamSubSection): String = when (sub) {
        TeamSubSection.ACCOUNTS -> get("team_sub_accounts", sub.label)
        TeamSubSection.NEW_REGISTERED -> get("team_sub_new_registered", "New Users")
        TeamSubSection.COVERAGE -> get("team_sub_waiters", sub.label)
        TeamSubSection.BRANDS -> get("team_sub_brands", sub.label)
    }

    fun statusLabel(status: OrderStatus): String = when (status) {
        OrderStatus.NEW -> get("status_new", status.label)
        OrderStatus.CONFIRMED -> get("status_confirmed", status.label)
        OrderStatus.PREPARING -> get("status_preparing", status.label)
        OrderStatus.READY -> get("status_ready", status.label)
        OrderStatus.DONE -> get("status_done", status.label)
        OrderStatus.CANCELLED -> get("status_cancelled", status.label)
    }

    fun statusActionLabel(status: OrderStatus): String = when (status) {
        OrderStatus.CONFIRMED -> get("action_send_to_kitchen", status.actionLabel)
        OrderStatus.PREPARING -> get("action_start_preparing", status.actionLabel)
        OrderStatus.READY -> get("action_mark_ready", status.actionLabel)
        OrderStatus.DONE -> get("action_confirm_pickup", status.actionLabel)
        OrderStatus.CANCELLED -> get("action_cancel", status.actionLabel)
        OrderStatus.NEW -> get("status_new_short", status.actionLabel)
    }

    fun orderTypeLabel(type: OrderType): String = when (type) {
        OrderType.ROOM -> get("order_type_room", type.label)
        OrderType.DINE -> get("order_type_dine", type.label)
        OrderType.PICKUP -> get("order_type_pickup", type.label)
        OrderType.OUT -> get("order_type_out", type.label)
    }

    fun orderTypePrompt(type: OrderType): String = when (type) {
        OrderType.ROOM -> get("prompt_room_number", type.locationPrompt)
        OrderType.DINE -> get("prompt_table_number", type.locationPrompt)
        OrderType.PICKUP -> get("prompt_reception_table", type.locationPrompt)
        OrderType.OUT -> get("prompt_phone_number", type.locationPrompt)
    }

    fun floorZoneTitle(zone: FloorZone): String = when (zone) {
        FloorZone.HOTEL -> get("zone_rooms", zone.title)
        FloorZone.DINE -> get("zone_restaurant", zone.title)
        FloorZone.REC -> get("zone_reception", zone.title)
    }

    fun waiterAreaLabel(area: WaiterArea): String = when (area) {
        WaiterArea.DIN -> get("area_restaurant", area.label)
        WaiterArea.LOB -> get("area_lobby", area.label)
        WaiterArea.F1 -> get("area_floor_1", area.label)
        WaiterArea.F2 -> get("area_floor_2", area.label)
    }

    fun menuCategoryLabel(category: String): String = when (category) {
        "All" -> get("cat_all", "All")
        "Saved" -> get("cat_saved", "Saved")
        "Classics" -> get("cat_classics", "Classics")
        "Lake Tana" -> get("cat_lake_tana", "Lake Tana")
        "Grill" -> get("cat_grill", "Grill")
        "Breakfast" -> get("cat_breakfast", "Breakfast")
        "Drinks" -> get("cat_drinks", "Drinks")
        else -> category
    }

    fun dishName(item: MenuItem): String = get("dish_name_${item.id}", item.name)

    fun dishDescription(item: MenuItem): String = get("dish_desc_${item.id}", item.description)

    fun translationStatusLabel(status: TranslationStatus): String = when (status) {
        TranslationStatus.TRANSLATED -> get("lang_status_translated", "Translated")
        TranslationStatus.UNTRANSLATED -> get("lang_status_untranslated", "Untranslated")
        TranslationStatus.OUTDATED -> get("lang_status_outdated", "Outdated")
        TranslationStatus.MISSING -> get("lang_status_missing", "Missing")
    }

    fun translationCategoryLabel(cat: TranslationCategory): String =
        if (language == AppLanguage.AM) cat.labelAm else cat.labelEn

    fun orderStatusMessage(
        orderNumber: Long,
        location: String,
        status: OrderStatus,
        reason: String = "",
    ): String = when (status) {
        OrderStatus.NEW -> format("notif_order_new", "num" to orderNumber, "loc" to location)
        OrderStatus.CONFIRMED -> format("notif_order_confirmed", "num" to orderNumber, "loc" to location)
        OrderStatus.PREPARING -> format("notif_order_preparing", "num" to orderNumber, "loc" to location)
        OrderStatus.READY -> format("notif_order_ready", "num" to orderNumber, "loc" to location)
        OrderStatus.DONE -> format("notif_order_done", "num" to orderNumber, "loc" to location)
        OrderStatus.CANCELLED -> if (reason.isNotBlank()) {
            format("notif_order_cancelled_reason", "num" to orderNumber, "reason" to reason)
        } else {
            format("notif_order_cancelled", "num" to orderNumber, "loc" to location)
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.EN }
val LocalAppStrings = staticCompositionLocalOf { TranslationCatalog.defaultEnglishStrings() }
