package com.example.fcm

import com.example.data.model.AppNotification
import com.example.data.model.AppTab
import com.example.data.model.BrandConfig
import com.example.data.model.OrderStatus
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import java.util.Date

enum class PushChannelCategory(val channelId: String) {
    KITCHEN_ALERTS("lakemark_kitchen_alerts_v3"),
    READY_ALERTS("lakemark_ready_alerts_v3"),
    BROADCAST_ALERTS("lakemark_broadcast_alerts_v3"),
}

data class PushDeepLink(
    val tab: AppTab = AppTab.HOME,
    val orderId: String = "",
    val notifId: String = "",
    val openNotificationsSheet: Boolean = false,
    val kind: String = "",
    val orderStatus: String = "",
    val apkUrl: String = "",
) {
    fun resolveTargetTab(activeRole: UserRole): AppTab {
        if (tab in activeRole.allowedTabs) return tab
        return when {
            orderId.isNotBlank() && AppTab.KITCHEN in activeRole.allowedTabs && activeRole == UserRole.KITCHEN -> AppTab.KITCHEN
            orderId.isNotBlank() && AppTab.ORDERS in activeRole.allowedTabs -> AppTab.ORDERS
            AppTab.HOME in activeRole.allowedTabs -> AppTab.HOME
            else -> activeRole.allowedTabs.firstOrNull() ?: AppTab.HOME
        }
    }

    fun toUriString(): String {
        val params = buildList {
            add("tab=${encodeQuery(tab.key)}")
            if (orderId.isNotBlank()) add("orderId=${encodeQuery(orderId)}")
            if (notifId.isNotBlank()) add("notifId=${encodeQuery(notifId)}")
            if (openNotificationsSheet) add("openSheet=1")
            if (kind.isNotBlank()) add("kind=${encodeQuery(kind)}")
            if (orderStatus.isNotBlank()) add("status=${encodeQuery(orderStatus)}")
            if (apkUrl.isNotBlank()) add("apkUrl=${encodeQuery(apkUrl)}")
        }
        return "lakemark://app/open?${params.joinToString("&")}"
    }

    companion object {
        const val EXTRA_TAB = "lakemark_extra_tab"
        const val EXTRA_ORDER_ID = "lakemark_extra_order_id"
        const val EXTRA_NOTIF_ID = "lakemark_extra_notif_id"
        const val EXTRA_OPEN_SHEET = "lakemark_extra_open_sheet"
        const val EXTRA_KIND = "lakemark_extra_kind"
        const val EXTRA_ORDER_STATUS = "lakemark_extra_order_status"
        const val EXTRA_APK_URL = "lakemark_extra_apk_url"

        fun fromQueryAndExtras(
            uriString: String?,
            extraTab: String?,
            extraOrderId: String?,
            extraNotifId: String?,
            extraOpenSheet: Boolean?,
            extraKind: String?,
            extraOrderStatus: String?,
            extraApkUrl: String? = null,
        ): PushDeepLink? {
            val queryParams = parseUriQuery(uriString)
            val rawTab = extraTab?.ifBlank { null } ?: queryParams["tab"]
            val rawOrderId = extraOrderId?.ifBlank { null } ?: queryParams["orderId"].orEmpty()
            val rawNotifId = extraNotifId?.ifBlank { null } ?: queryParams["notifId"].orEmpty()
            val openSheet = extraOpenSheet ?: (
                queryParams["openSheet"] == "1" ||
                    queryParams["openSheet"] == "true" ||
                    queryParams["openNotificationsSheet"] == "1" ||
                    queryParams["openNotificationsSheet"] == "true"
                )
            val rawKind = extraKind?.ifBlank { null } ?: queryParams["kind"].orEmpty()
            val rawStatus = extraOrderStatus?.ifBlank { null } ?: queryParams["status"].orEmpty()
            val rawApkUrl = extraApkUrl?.ifBlank { null } ?: queryParams["apkUrl"].orEmpty()

            if (rawTab.isNullOrBlank() && rawOrderId.isBlank() && rawNotifId.isBlank() && !openSheet && rawKind != "app_update") {
                return null
            }

            val parsedTab = AppTab.entries.firstOrNull { it.key.equals(rawTab, ignoreCase = true) }
                ?: if (rawOrderId.isNotBlank()) AppTab.ORDERS else AppTab.HOME

            return PushDeepLink(
                tab = parsedTab,
                orderId = rawOrderId,
                notifId = rawNotifId,
                openNotificationsSheet = openSheet,
                kind = rawKind,
                orderStatus = rawStatus,
                apkUrl = rawApkUrl,
            )
        }

        private fun encodeQuery(value: String): String =
            java.net.URLEncoder.encode(value, "UTF-8")

        private fun parseUriQuery(uriString: String?): Map<String, String> {
            if (uriString.isNullOrBlank()) return emptyMap()
            val idx = uriString.indexOf('?')
            val pathPart = if (idx >= 0) uriString.substring(0, idx) else uriString
            val map = mutableMapOf<String, String>()
            if (pathPart.endsWith("/notifications", ignoreCase = true)) {
                map["openSheet"] = "1"
            } else if (pathPart.endsWith("/kitchen", ignoreCase = true)) {
                map["tab"] = AppTab.KITCHEN.key
            } else if (pathPart.endsWith("/orders", ignoreCase = true)) {
                map["tab"] = AppTab.ORDERS.key
            }
            if (idx < 0 || idx + 1 >= uriString.length) return map
            val query = uriString.substring(idx + 1)
            query.split('&').forEach { pair ->
                val eq = pair.indexOf('=')
                if (eq > 0) {
                    val k = java.net.URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                    val v = java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                    map[k] = v
                }
            }
            return map
        }
    }
}

data class ParsedPushMessage(
    val notifId: String,
    val fcmMessageId: String,
    val orderId: String,
    val orderNumber: Long,
    val orderStatus: String,
    val senderUid: String,
    val senderRole: UserRole,
    val senderBrandName: String,
    val senderMark: String,
    val senderPrimaryHex: String,
    val senderSecondaryHex: String,
    val senderAccentHex: String,
    val targetRoles: List<String>,
    val targetUserId: String,
    val kind: String,
    val title: String,
    val body: String,
    val apkUrl: String = "",
    val createdAtMs: Long,
    val deepLink: PushDeepLink,
    val channelCategory: PushChannelCategory,
) {
    fun senderBrandConfig(): BrandConfig {
        val fallback = senderRole.defaultBrandConfig()
        return BrandConfig(
            roleKey = senderRole.key,
            primaryHex = senderPrimaryHex.ifBlank { fallback.primaryHex },
            secondaryHex = senderSecondaryHex.ifBlank { fallback.secondaryHex },
            accentHex = senderAccentHex.ifBlank { fallback.accentHex },
            brandName = senderBrandName.ifBlank { fallback.brandName }.take(18),
            mark = senderMark.take(2),
        )
    }

    fun toAppNotification(): AppNotification = AppNotification(
        id = notifId.ifBlank { fcmMessageId.ifBlank { "push_${createdAtMs}" } },
        senderUid = senderUid,
        senderRole = senderRole.key,
        senderBrandName = senderBrandName.ifBlank { senderRole.displayName },
        message = body,
        targetRoles = targetRoles,
        targetUserId = targetUserId,
        orderId = orderId,
        kind = kind,
        apkUrl = apkUrl,
        createdAt = Timestamp(Date(createdAtMs)),
    )
}

object FcmPayloadParser {
    private val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")

    fun parse(
        data: Map<String, String>,
        fallbackTitle: String? = null,
        fallbackBody: String? = null,
        fcmMessageId: String = "",
        sentTimeMs: Long = 0L,
    ): ParsedPushMessage? {
        val senderRole = UserRole.fromKey(data["senderRole"])
        val defaultBrand = senderRole.defaultBrandConfig()

        val notifId = data["notifId"].orEmpty().trim()
        val orderId = data["orderId"].orEmpty().trim()
        val orderNumber = data["orderNumber"]?.trim()?.toLongOrNull() ?: 0L
        val orderStatus = data["orderStatus"]?.trim()?.ifBlank { null }
            ?: data["status"].orEmpty().trim()
        val senderUid = data["senderUid"].orEmpty().trim()
        val senderBrandName = data["senderBrandName"]?.trim()?.take(30)?.ifBlank { null }
            ?: senderRole.displayName
        val senderMark = data["senderMark"]?.trim()?.take(2).orEmpty()

        val primaryHex = data["senderPrimaryHex"]?.trim()?.takeIf { hexRegex.matches(it) }
            ?: defaultBrand.primaryHex
        val secondaryHex = data["senderSecondaryHex"]?.trim()?.takeIf { hexRegex.matches(it) }
            ?: defaultBrand.secondaryHex
        val accentHex = data["senderAccentHex"]?.trim()?.takeIf { hexRegex.matches(it) }
            ?: defaultBrand.accentHex

        val rawTargets = data["targetRoles"].orEmpty()
        val targetRoles = rawTargets.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("all") }

        val targetUserId = data["targetUserId"].orEmpty().trim()
        val apkUrl = data["apkUrl"].orEmpty().trim().take(1000)
        val kind = data["kind"]?.trim()?.lowercase()?.takeIf {
            it in setOf("new", "ready", "ping", "broadcast", "app_update")
        } ?: when (orderStatus.lowercase()) {
            "new" -> "new"
            "ready" -> "ready"
            else -> if (orderId.isNotBlank()) "ping" else "broadcast"
        }

        val body = data["body"]?.trim()?.ifBlank { null }
            ?: data["message"]?.trim()?.ifBlank { null }
            ?: fallbackBody?.trim()?.ifBlank { null }
            ?: return null

        val title = data["title"]?.trim()?.ifBlank { null }
            ?: fallbackTitle?.trim()?.ifBlank { null }
            ?: defaultTitleFor(senderBrandName, kind, orderNumber)

        val rawTargetTab = data["targetTab"]?.trim()?.ifBlank { null }
        val resolvedTab = AppTab.entries.firstOrNull { it.key.equals(rawTargetTab, ignoreCase = true) }
            ?: when {
                kind == "new" && "kitchen" in targetRoles -> AppTab.KITCHEN
                orderId.isNotBlank() -> AppTab.ORDERS
                else -> AppTab.HOME
            }

        val openSheet = data["openNotificationsSheet"] == "true" ||
            data["openNotificationsSheet"] == "1" ||
            kind == "app_update" ||
            (kind == "broadcast" && orderId.isBlank())

        val createdAtMs = data["createdAtMs"]?.trim()?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: sentTimeMs.takeIf { it > 0L }
            ?: System.currentTimeMillis()

        val channelCategory = when (kind) {
            "ready" -> PushChannelCategory.READY_ALERTS
            "broadcast", "app_update" -> PushChannelCategory.BROADCAST_ALERTS
            else -> PushChannelCategory.KITCHEN_ALERTS
        }

        return ParsedPushMessage(
            notifId = notifId,
            fcmMessageId = fcmMessageId.ifBlank { data["google.message_id"].orEmpty() },
            orderId = orderId,
            orderNumber = orderNumber,
            orderStatus = orderStatus,
            senderUid = senderUid,
            senderRole = senderRole,
            senderBrandName = senderBrandName,
            senderMark = senderMark,
            senderPrimaryHex = primaryHex,
            senderSecondaryHex = secondaryHex,
            senderAccentHex = accentHex,
            targetRoles = targetRoles,
            targetUserId = targetUserId,
            kind = kind,
            title = title,
            body = body,
            apkUrl = apkUrl,
            createdAtMs = createdAtMs,
            deepLink = PushDeepLink(
                tab = resolvedTab,
                orderId = orderId,
                notifId = notifId,
                openNotificationsSheet = openSheet,
                kind = kind,
                orderStatus = orderStatus,
                apkUrl = apkUrl,
            ),
            channelCategory = channelCategory,
        )
    }

    private fun defaultTitleFor(senderBrandName: String, kind: String, orderNumber: Long): String {
        val numberSuffix = if (orderNumber > 0L) " · #$orderNumber" else ""
        return when (kind) {
            "new" -> "$senderBrandName · New order$numberSuffix"
            "ready" -> "$senderBrandName · Order ready$numberSuffix"
            "broadcast" -> "$senderBrandName · Announcement"
            "app_update" -> "$senderBrandName · App Update"
            else -> "$senderBrandName · Update$numberSuffix"
        }
    }
}

object PushRoutingPolicy {
    fun topicsForSession(uid: String, role: UserRole): Set<String> {
        val topics = mutableSetOf<String>()
        val cleanRole = sanitizeTopicSegment(role.key)
        if (cleanRole.isNotEmpty()) {
            topics.add("lakemark_role_$cleanRole")
        }
        if (role.isStaff) {
            topics.add("lakemark_staff")
        }
        topics.add("lakemark_all")
        val cleanUid = sanitizeTopicSegment(uid)
        if (cleanUid.isNotEmpty()) {
            topics.add("lakemark_user_$cleanUid")
        }
        return topics
    }

    fun shouldDeliverToSession(
        message: ParsedPushMessage,
        currentUid: String,
        currentRole: UserRole?,
    ): Boolean {
        val cleanUid = currentUid.trim()
        if (cleanUid.isBlank()) return false
        val role = currentRole ?: return false

        // Suppress direct self-echoes for actions initiated by this signed-in user
        if (message.senderUid.isNotBlank() && message.senderUid == cleanUid) {
            return false
        }

        return when {
            role == UserRole.MANAGER -> true
            role.isStaff -> {
                val matchesRole = message.targetRoles.contains("all") ||
                    message.targetRoles.contains(role.key)
                val matchesUser = message.targetUserId.isNotBlank() &&
                    message.targetUserId == cleanUid
                matchesRole || matchesUser
            }
            else -> {
                val matchesUser = message.targetUserId.isNotBlank() &&
                    message.targetUserId == cleanUid
                val isGlobalBroadcast = (message.kind == "broadcast" || message.kind == "app_update") &&
                    message.targetRoles.contains("all") &&
                    message.targetUserId.isBlank()
                val isUnaddressedCustomerPush = (message.kind == "broadcast" || message.kind == "app_update") &&
                    message.targetUserId.isBlank() &&
                    message.targetRoles.contains(UserRole.CUSTOMER.key)
                matchesUser || isGlobalBroadcast || isUnaddressedCustomerPush
            }
        }
    }

    fun sanitizeTopicSegment(raw: String): String =
        raw.trim().replace(Regex("[^a-zA-Z0-9-_.~%]"), "_").take(80)
}

class DeduplicationCache(
    private val maxEntries: Int = 150,
    private val ttlMs: Long = 10 * 60 * 1000L,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private val entries = LinkedHashMap<String, Long>(maxEntries, 0.75f, true)

    @Synchronized
    fun restore(snapshot: Map<String, Long>) {
        val now = clockMs()
        snapshot.entries
            .filter { now - it.value in 0..ttlMs }
            .sortedBy { it.value }
            .takeLast(maxEntries)
            .forEach { entries[it.key] = it.value }
    }

    @Synchronized
    fun snapshot(): Map<String, Long> {
        pruneLocked(clockMs())
        return LinkedHashMap(entries)
    }

    @Synchronized
    fun checkAndMark(keys: List<String>): Boolean {
        val validKeys = keys.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (validKeys.isEmpty()) return true
        val now = clockMs()
        pruneLocked(now)
        if (validKeys.any { entries.containsKey(it) }) {
            return false
        }
        validKeys.forEach { key ->
            entries[key] = now
        }
        while (entries.size > maxEntries) {
            val eldest = entries.entries.firstOrNull()?.key ?: break
            entries.remove(eldest)
        }
        return true
    }

    private fun pruneLocked(now: Long) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > ttlMs || now < entry.value) {
                iterator.remove()
            }
        }
    }

    companion object {
        fun keysForPush(message: ParsedPushMessage): List<String> = buildList {
            if (message.notifId.isNotBlank()) add("notif:${message.notifId}")
            if (message.fcmMessageId.isNotBlank()) add("fcm:${message.fcmMessageId}")
            val isWaiterAssignmentPing = message.kind == "ping" &&
                message.body.contains("assigned to", ignoreCase = true)
            if (!isWaiterAssignmentPing &&
                message.orderId.isNotBlank() &&
                message.orderStatus.isNotBlank()
            ) {
                add("order:${message.orderId}:${message.orderStatus.lowercase()}")
            }
            if (message.notifId.isBlank() && message.fcmMessageId.isBlank()) {
                add("msg:${message.senderRole.key}:${message.orderId}:${message.kind}:${message.body.hashCode()}")
            }
        }

        fun keysForOrderStatus(orderId: String, status: OrderStatus): List<String> =
            if (orderId.isNotBlank()) listOf("order:$orderId:${status.key}") else emptyList()
    }
}
