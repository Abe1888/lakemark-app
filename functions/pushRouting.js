"use strict";

const CHANNEL_IDS = {
  kitchen: "lakemark_kitchen_alerts_v3",
  ready: "lakemark_ready_alerts_v3",
  broadcast: "lakemark_broadcast_alerts_v3",
};

function soundNameForKind(kind) {
  if (kind === "new") return "lakemark_new_order";
  if (kind === "ready") return "lakemark_order_ready";
  return "lakemark_ping";
}

const DEFAULT_BRANDS = {
  manager: { primaryHex: "#FAF3E6", secondaryHex: "#3D0F4F", accentHex: "#04C353", brandName: "Manager", mark: "" },
  reception: { primaryHex: "#D6EEEA", secondaryHex: "#124A47", accentHex: "#F2A31B", brandName: "Reception", mark: "" },
  kitchen: { primaryHex: "#FBE2CE", secondaryHex: "#7A2B0A", accentHex: "#F26B1D", brandName: "Kitchen", mark: "" },
  waiter: { primaryHex: "#DCE7FB", secondaryHex: "#1B3A86", accentHex: "#2BB3E8", brandName: "Waiter", mark: "" },
  cashier: { primaryHex: "#F5E9C4", secondaryHex: "#5A4208", accentHex: "#D9A21B", brandName: "Cashier", mark: "" },
  delivery: { primaryHex: "#F8DCE6", secondaryHex: "#7E1743", accentHex: "#F0436B", brandName: "Delivery", mark: "" },
  customer: { primaryHex: "#DBF2E2", secondaryHex: "#14502B", accentHex: "#1FB55A", brandName: "Customer", mark: "" },
};

function sanitizeTopicSegment(raw) {
  return String(raw || "")
    .trim()
    .replace(/[^a-zA-Z0-9-_.~%]/g, "_")
    .slice(0, 80);
}

function channelIdForKind(kind) {
  if (kind === "ready") return CHANNEL_IDS.ready;
  if (kind === "broadcast" || kind === "app_update") return CHANNEL_IDS.broadcast;
  return CHANNEL_IDS.kitchen;
}

function validQueryRoles(targetRoles, kind) {
  return (Array.isArray(targetRoles) ? targetRoles : [])
    .map((r) => String(r || "").trim().toLowerCase())
    .filter((r) => Boolean(r) && r !== "all" && (r !== "customer" || kind === "broadcast" || kind === "app_update"));
}

function shouldIncludeTargetUserId(targetUserId, senderUid, kind) {
  const cleanTarget = String(targetUserId || "").trim();
  const cleanSender = String(senderUid || "").trim();
  if (!cleanTarget) return false;
  // Customers placing an order write targetUserId = their own uid for Firestore read rules;
  // they must not receive a "New order" push back for the order they just created.
  if (kind === "new" && cleanTarget === cleanSender) return false;
  return true;
}

function shouldDeliverTokenToRecipient({
  uid,
  userData,
  tokenData,
  senderUid,
  kind,
  targetRoles,
  targetUserId,
  orderData,
}) {
  const token = String((tokenData && tokenData.token) || "").trim();
  if (!token) return false;
  if (tokenData && tokenData.notificationsEnabled === false) return false;
  if (!userData || userData.active !== true) return false;

  const cleanUid = String(uid || "").trim();
  const cleanSender = String(senderUid || "").trim();
  if (cleanSender && cleanUid === cleanSender) {
    return false;
  }

  const normalizedTargets = (Array.isArray(targetRoles) ? targetRoles : ["all"])
    .map((r) => String(r || "").trim().toLowerCase())
    .filter(Boolean);

  const effectiveRole = String(
    (tokenData && tokenData.role) || (userData && userData.role) || "customer"
  ).trim().toLowerCase();

  const cleanTargetUserId = String(targetUserId || "").trim();
  const isTargetUser = Boolean(cleanTargetUserId && cleanUid === cleanTargetUserId);
  const matchesAll = normalizedTargets.includes("all");

  if (effectiveRole === "customer") {
    if (kind === "broadcast" || kind === "app_update") {
      return isTargetUser || matchesAll || normalizedTargets.includes("customer");
    }
    return isTargetUser;
  }

  if (effectiveRole !== "manager") {
    const matchesRole = matchesAll || normalizedTargets.includes(effectiveRole);
    if (!matchesRole && !isTargetUser) {
      return false;
    }
  }

  if (kind === "ready" && orderData && typeof orderData === "object") {
    const orderType = String(orderData.orderType || "").trim().toLowerCase();
    const assignedWaiterId = String(orderData.assignedWaiterId || "").trim();
    if (effectiveRole === "delivery" && orderType !== "out") {
      return false;
    }
    if (effectiveRole === "waiter") {
      if (orderType === "out") return false;
      if (assignedWaiterId && assignedWaiterId !== cleanUid) return false;
    }
  }

  return true;
}

function resolveTopicFallbackTargets({ targetRoles, targetUserId, senderUid, kind }) {
  const normalizedTargets = (Array.isArray(targetRoles) ? targetRoles : [])
    .map((r) => String(r || "").trim().toLowerCase())
    .filter(Boolean);

  const topics = [];
  if (normalizedTargets.includes("all")) {
    topics.push("lakemark_all");
  } else {
    validQueryRoles(normalizedTargets, kind).forEach((role) => {
      const seg = sanitizeTopicSegment(role);
      if (seg) topics.push(`lakemark_role_${seg}`);
    });
    if (shouldIncludeTargetUserId(targetUserId, senderUid, kind)) {
      const userSeg = sanitizeTopicSegment(targetUserId);
      if (userSeg) topics.push(`lakemark_user_${userSeg}`);
    }
  }
  return [...new Set(topics)];
}

function buildHybridFcmPayload({
  notifId,
  notif,
  brandData,
  orderData,
  nowMs = Date.now(),
}) {
  const senderUid = String((notif && notif.senderUid) || "");
  const senderRole = String((notif && notif.senderRole) || "manager").toLowerCase();
  const targetRoles = Array.isArray(notif && notif.targetRoles)
    ? notif.targetRoles.map((r) => String(r).trim().toLowerCase()).filter(Boolean)
    : ["all"];
  const targetUserId = String((notif && notif.targetUserId) || "");
  const orderId = String((notif && notif.orderId) || "");
  const kind = String((notif && notif.kind) || "ping").toLowerCase();
  const message = String((notif && notif.message) || "").trim();
  const apkUrl = String((notif && notif.apkUrl) || "").trim();

  const fallbackBrand = DEFAULT_BRANDS[senderRole] || DEFAULT_BRANDS.manager;
  const senderBrandName = String(
    (notif && notif.senderBrandName) ||
      (brandData && brandData.brandName) ||
      fallbackBrand.brandName
  ).slice(0, 30);

  const orderNumber = Number((orderData && orderData.orderNumber) || 0);
  const orderStatus = String(
    (orderData && orderData.status) ||
      (kind === "new" ? "new" : kind === "ready" ? "ready" : "")
  );
  const targetTab =
    kind === "new" && targetRoles.includes("kitchen")
      ? "kitchen"
      : orderId
        ? "orders"
        : "home";

  const title =
    kind === "new"
      ? `${senderBrandName} · New order${orderNumber ? ` #${orderNumber}` : ""}`
      : kind === "ready"
        ? `${senderBrandName} · Order ready${orderNumber ? ` #${orderNumber}` : ""}`
        : kind === "app_update"
          ? `${senderBrandName} · App update available`
          : kind === "broadcast"
            ? `${senderBrandName} · Announcement`
            : `${senderBrandName} · Order update${orderNumber ? ` #${orderNumber}` : ""}`;

  const channelId = channelIdForKind(kind);
  const secondaryHex = String((brandData && brandData.secondaryHex) || fallbackBrand.secondaryHex);

  const dataPayload = {
    notifId: String(notifId || ""),
    orderId,
    orderNumber: String(orderNumber),
    orderStatus,
    senderUid,
    senderRole,
    senderBrandName,
    senderMark: String((brandData && brandData.mark) || fallbackBrand.mark || ""),
    senderPrimaryHex: String((brandData && brandData.primaryHex) || fallbackBrand.primaryHex),
    senderSecondaryHex: secondaryHex,
    senderAccentHex: String((brandData && brandData.accentHex) || fallbackBrand.accentHex),
    targetRoles: targetRoles.join(","),
    targetUserId,
    targetTab,
    openNotificationsSheet: String((kind === "broadcast" || kind === "app_update") && !orderId),
    kind,
    apkUrl,
    title,
    body: message,
    channelId,
    createdAtMs: String(nowMs),
  };

  return {
    channelId,
    title,
    body: message,
    dataPayload,
    fcmBase: {
      notification: {
        title,
        body: message,
      },
      data: dataPayload,
      android: {
        priority: "high",
        ttl: 3600 * 1000,
        notification: {
          channelId,
          sound: soundNameForKind(kind),
          defaultSound: false,
          defaultVibrateTimings: false,
          vibrateTimingsMillis: kind === "new" ? [0, 300, 150, 300, 150, 300] : [0, 150],
          priority: "high",
          visibility: "public",
          color: secondaryHex,
          tag: orderId ? `order_${orderId}` : `notif_${String(notifId || nowMs)}`,
        },
      },
    },
  };
}

module.exports = {
  CHANNEL_IDS,
  DEFAULT_BRANDS,
  sanitizeTopicSegment,
  channelIdForKind,
  validQueryRoles,
  shouldIncludeTargetUserId,
  shouldDeliverTokenToRecipient,
  resolveTopicFallbackTargets,
  buildHybridFcmPayload,
};
