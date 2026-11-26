package com.example

import com.example.data.model.AppTab
import com.example.data.model.OrderStatus
import com.example.data.model.UserRole
import com.example.fcm.DeduplicationCache
import com.example.fcm.FcmPayloadParser
import com.example.fcm.PushChannelCategory
import com.example.fcm.PushDeepLink
import com.example.fcm.PushRoutingPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmPushTests {

    @Test
    fun `payload parser parses branded data message and falls back on malformed hex`() {
        val parsed = FcmPayloadParser.parse(
            data = mapOf(
                "notifId" to "notif_101",
                "orderId" to "ord_1001",
                "orderNumber" to "1001",
                "orderStatus" to "ready",
                "senderUid" to "uid_chef",
                "senderRole" to "kitchen",
                "senderBrandName" to "Grill Chef",
                "senderMark" to "GC",
                "senderPrimaryHex" to "#FFF8EE",
                "senderSecondaryHex" to "#24140B",
                "senderAccentHex" to "not-a-hex",
                "targetRoles" to "waiter,reception,customer,manager",
                "targetUserId" to "uid_cust_1",
                "kind" to "ready",
                "body" to "Order #1001 is ready · Room 214",
                "createdAtMs" to "1700000000000",
            ),
            fcmMessageId = "fcm_abc",
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals("notif_101", parsed.notifId)
        assertEquals("ord_1001", parsed.orderId)
        assertEquals(1001L, parsed.orderNumber)
        assertEquals("ready", parsed.orderStatus)
        assertEquals(UserRole.KITCHEN, parsed.senderRole)
        assertEquals("Grill Chef", parsed.senderBrandName)
        assertEquals("GC", parsed.senderMark)
        assertEquals("#FFF8EE", parsed.senderPrimaryHex)
        assertEquals("#24140B", parsed.senderSecondaryHex)
        // Invalid accent hex falls back cleanly to Kitchen's default accent hex
        assertEquals(UserRole.KITCHEN.accentHex, parsed.senderAccentHex)
        assertEquals(PushChannelCategory.READY_ALERTS, parsed.channelCategory)
        assertEquals(AppTab.ORDERS, parsed.deepLink.tab)
        assertEquals("ord_1001", parsed.deepLink.orderId)

        val brandCfg = parsed.senderBrandConfig()
        assertEquals("Grill Chef", brandCfg.brandName)
        assertEquals("GC", brandCfg.mark)
    }

    @Test
    fun `payload parser returns null when body and fallback are empty`() {
        val parsed = FcmPayloadParser.parse(
            data = mapOf("senderRole" to "manager", "kind" to "ping"),
            fallbackTitle = "Lakemark",
            fallbackBody = "   ",
        )
        assertNull(parsed)
    }

    @Test
    fun `role routing delivers order updates only to authorized roles and owning customer`() {
        val readyPush = FcmPayloadParser.parse(
            data = mapOf(
                "notifId" to "n_ready",
                "orderId" to "ord_22",
                "orderStatus" to "ready",
                "senderUid" to "uid_kitchen",
                "senderRole" to "kitchen",
                "targetRoles" to "reception,waiter,cashier,delivery,customer,manager",
                "targetUserId" to "cust_A",
                "kind" to "ready",
                "body" to "Order #1022 ready",
            ),
        )!!

        // Customer A owns this order -> delivered
        assertTrue(PushRoutingPolicy.shouldDeliverToSession(readyPush, "cust_A", UserRole.CUSTOMER))
        // Customer B does NOT own this order -> blocked
        assertFalse(PushRoutingPolicy.shouldDeliverToSession(readyPush, "cust_B", UserRole.CUSTOMER))
        // Waiter and Reception match targetRoles -> delivered
        assertTrue(PushRoutingPolicy.shouldDeliverToSession(readyPush, "local_waiter", UserRole.WAITER))
        assertTrue(PushRoutingPolicy.shouldDeliverToSession(readyPush, "local_recep", UserRole.RECEPTION))
        // Manager always receives staff/station activity -> delivered
        assertTrue(PushRoutingPolicy.shouldDeliverToSession(readyPush, "local_mgr", UserRole.MANAGER))
        // Kitchen did not request to receive READY alerts -> blocked for kitchen station
        assertFalse(PushRoutingPolicy.shouldDeliverToSession(readyPush, "local_kitchen", UserRole.KITCHEN))
    }

    @Test
    fun `broadcast routing suppresses self echo and isolates role specific broadcasts from customers`() {
        val kitchenBroadcast = FcmPayloadParser.parse(
            data = mapOf(
                "notifId" to "n_bcast_1",
                "senderUid" to "uid_mgr",
                "senderRole" to "manager",
                "targetRoles" to "kitchen,manager",
                "kind" to "broadcast",
                "body" to "Prep extra injera for banquet",
            ),
        )!!

        assertFalse(PushRoutingPolicy.shouldDeliverToSession(kitchenBroadcast, "uid_mgr", UserRole.MANAGER))
        assertTrue(PushRoutingPolicy.shouldDeliverToSession(kitchenBroadcast, "uid_chef", UserRole.KITCHEN))
        assertFalse(PushRoutingPolicy.shouldDeliverToSession(kitchenBroadcast, "uid_waiter", UserRole.WAITER))
        assertFalse(PushRoutingPolicy.shouldDeliverToSession(kitchenBroadcast, "uid_guest", UserRole.CUSTOMER))
    }

    @Test
    fun `deduplication blocks duplicate notifId fcmId and cross source order transitions`() {
        var now = 1_000_000L
        val cache = DeduplicationCache(maxEntries = 4, ttlMs = 60_000L, clockMs = { now })

        val pushMsg = FcmPayloadParser.parse(
            data = mapOf(
                "notifId" to "notif_77",
                "orderId" to "ord_77",
                "orderStatus" to "ready",
                "kind" to "ready",
                "body" to "Order #1077 ready",
            ),
            fcmMessageId = "fcm_msg_77",
        )!!

        // First delivery via FCM push succeeds
        assertTrue(cache.checkAndMark(DeduplicationCache.keysForPush(pushMsg)))
        // Duplicate FCM push with same notifId or fcmMessageId is rejected
        assertFalse(cache.checkAndMark(DeduplicationCache.keysForPush(pushMsg)))
        // Firestore snapshot listener firing for the exact same order:status transition is also deduplicated
        assertFalse(cache.checkAndMark(DeduplicationCache.keysForOrderStatus("ord_77", OrderStatus.READY)))

        // Next status transition on the same order succeeds
        assertTrue(cache.checkAndMark(DeduplicationCache.keysForOrderStatus("ord_77", OrderStatus.DONE)))

        // After TTL expires, key is eligible again
        now += 61_000L
        assertTrue(cache.checkAndMark(DeduplicationCache.keysForOrderStatus("ord_77", OrderStatus.READY)))
    }

    @Test
    fun `deep link parsing and role tab clamping handle intents uris and restricted tabs`() {
        val kitchenPush = PushDeepLink(
            tab = AppTab.KITCHEN,
            orderId = "ord_55",
            notifId = "n_55",
            kind = "new",
            orderStatus = "confirmed",
        )
        val uri = kitchenPush.toUriString()
        val parsed = PushDeepLink.fromQueryAndExtras(
            uriString = uri,
            extraTab = null,
            extraOrderId = null,
            extraNotifId = null,
            extraOpenSheet = null,
            extraKind = null,
            extraOrderStatus = null,
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals(AppTab.KITCHEN, parsed.tab)
        assertEquals("ord_55", parsed.orderId)
        assertEquals("n_55", parsed.notifId)

        // Kitchen sees Kitchen tab; Reception/Customer don't have Kitchen tab so clamp safely to Orders
        assertEquals(AppTab.KITCHEN, parsed.resolveTargetTab(UserRole.KITCHEN))
        assertEquals(AppTab.ORDERS, parsed.resolveTargetTab(UserRole.RECEPTION))
        assertEquals(AppTab.ORDERS, parsed.resolveTargetTab(UserRole.CUSTOMER))
    }

    @Test
    fun `topic matrix switches cleanly across roles and multiple users`() {
        val kitchenTopics = PushRoutingPolicy.topicsForSession("user_1", UserRole.KITCHEN)
        assertTrue("lakemark_role_kitchen" in kitchenTopics)
        assertTrue("lakemark_staff" in kitchenTopics)
        assertTrue("lakemark_user_user_1" in kitchenTopics)
        assertTrue("lakemark_all" in kitchenTopics)

        val customerTopics = PushRoutingPolicy.topicsForSession("user_2", UserRole.CUSTOMER)
        assertTrue("lakemark_role_customer" in customerTopics)
        assertFalse("lakemark_staff" in customerTopics)
        assertTrue("lakemark_user_user_2" in customerTopics)
    }

    @Test
    fun `channel categories use v2 IDs and signed out sessions block delivery`() {
        assertEquals("lakemark_kitchen_alerts_v3", PushChannelCategory.KITCHEN_ALERTS.channelId)
        assertEquals("lakemark_ready_alerts_v3", PushChannelCategory.READY_ALERTS.channelId)
        assertEquals("lakemark_broadcast_alerts_v3", PushChannelCategory.BROADCAST_ALERTS.channelId)

        val broadcast = FcmPayloadParser.parse(
            data = mapOf(
                "notifId" to "n_bcast_auth",
                "senderUid" to "uid_mgr",
                "senderRole" to "manager",
                "targetRoles" to "all",
                "kind" to "broadcast",
                "body" to "Shift briefing",
            ),
        )!!

        assertFalse(PushRoutingPolicy.shouldDeliverToSession(broadcast, "", UserRole.MANAGER))
        assertFalse(PushRoutingPolicy.shouldDeliverToSession(broadcast, "uid_staff", null))
        assertTrue(PushRoutingPolicy.shouldDeliverToSession(broadcast, "uid_staff", UserRole.WAITER))
    }

    @Test
    fun `waiter assignment ping is not dropped by prior order status deduplication key`() {
        val cache = DeduplicationCache()
        assertTrue(cache.checkAndMark(DeduplicationCache.keysForOrderStatus("ord_99", OrderStatus.CONFIRMED)))

        val waiterAssignPush = FcmPayloadParser.parse(
            data = mapOf(
                "notifId" to "notif_assign_99",
                "orderId" to "ord_99",
                "orderStatus" to "confirmed",
                "senderUid" to "uid_recep",
                "senderRole" to "reception",
                "targetRoles" to "waiter,manager",
                "targetUserId" to "waiter_1",
                "kind" to "ping",
                "body" to "Order #1099 assigned to Waiter 1 · Room 108",
            ),
        )!!

        assertTrue(cache.checkAndMark(DeduplicationCache.keysForPush(waiterAssignPush)))
        assertFalse(cache.checkAndMark(DeduplicationCache.keysForPush(waiterAssignPush)))
    }

    @Test
    fun `deep link parser handles raw system tray FCM notification extras`() {
        val fromTray = PushDeepLink.fromQueryAndExtras(
            uriString = null,
            extraTab = "kitchen",
            extraOrderId = "ord_301",
            extraNotifId = "notif_301",
            extraOpenSheet = false,
            extraKind = "new",
            extraOrderStatus = "confirmed",
        )
        assertNotNull(fromTray)
        fromTray!!
        assertEquals(AppTab.KITCHEN, fromTray.tab)
        assertEquals("ord_301", fromTray.orderId)
        assertEquals("notif_301", fromTray.notifId)
        assertEquals("confirmed", fromTray.orderStatus)
    }

    @Test
    fun `app_update push parses apkUrl and routes update deep link cleanly`() {
        val apkUrl = "https://github.com/lakemark/releases/download/v3.2.0/lakemark-release-v12.apk"
        val parsed = FcmPayloadParser.parse(
            data = mapOf(
                "notifId" to "notif_release_12",
                "senderUid" to "uid_mgr",
                "senderRole" to "manager",
                "targetRoles" to "all",
                "kind" to "app_update",
                "apkUrl" to apkUrl,
                "body" to "Version 3.2.0 is ready to install.",
            ),
        )
        assertNotNull(parsed)
        parsed!!
        assertEquals("app_update", parsed.kind)
        assertEquals(apkUrl, parsed.apkUrl)
        assertEquals(apkUrl, parsed.deepLink.apkUrl)
        assertEquals("app_update", parsed.deepLink.kind)

        val uri = parsed.deepLink.toUriString()
        val roundTripped = PushDeepLink.fromQueryAndExtras(
            uriString = uri,
            extraTab = null,
            extraOrderId = null,
            extraNotifId = null,
            extraOpenSheet = null,
            extraKind = null,
            extraOrderStatus = null,
        )
        assertNotNull(roundTripped)
        assertEquals("app_update", roundTripped!!.kind)
        assertEquals(apkUrl, roundTripped.apkUrl)
    }
}
