"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  CHANNEL_IDS,
  buildHybridFcmPayload,
  channelIdForKind,
  resolveTopicFallbackTargets,
  shouldDeliverTokenToRecipient,
  shouldIncludeTargetUserId,
  validQueryRoles,
} = require("./pushRouting");

test("builds hybrid notification + data payload with _v2 sound channel and deep link fields", () => {
  const { channelId, fcmBase, dataPayload } = buildHybridFcmPayload({
    notifId: "n_1001",
    notif: {
      senderUid: "uid_chef",
      senderRole: "kitchen",
      senderBrandName: "Grill Chef",
      targetRoles: ["reception", "waiter", "customer", "manager"],
      targetUserId: "uid_cust",
      orderId: "ord_1001",
      kind: "ready",
      message: "Order #1001 is ready · Room 204",
    },
    brandData: {
      brandName: "Grill Chef",
      mark: "GC",
      primaryHex: "#FFF4E6",
      secondaryHex: "#7A2B0A",
      accentHex: "#F26B1D",
    },
    orderData: {
      orderNumber: 1001,
      status: "ready",
    },
    nowMs: 1700000000000,
  });

  assert.equal(channelId, CHANNEL_IDS.ready);
  assert.equal(channelId, "lakemark_ready_alerts_v3");
  assert.equal(fcmBase.notification.title, "Grill Chef · Order ready #1001");
  assert.equal(fcmBase.notification.body, "Order #1001 is ready · Room 204");
  assert.equal(fcmBase.android.priority, "high");
  assert.equal(fcmBase.android.ttl, 3600000);
  assert.equal(fcmBase.android.notification.channelId, "lakemark_ready_alerts_v3");
  assert.equal(fcmBase.android.notification.sound, "lakemark_order_ready");
  assert.equal(fcmBase.android.notification.defaultSound, false);
  assert.equal(fcmBase.android.notification.priority, "high");
  assert.equal(fcmBase.android.notification.visibility, "public");
  assert.equal(dataPayload.targetTab, "orders");
  assert.equal(dataPayload.openNotificationsSheet, "false");
});

test("maps new/ping/ready/broadcast/app_update to expected _v3 Android channels", () => {
  assert.equal(channelIdForKind("new"), "lakemark_kitchen_alerts_v3");
  assert.equal(channelIdForKind("ping"), "lakemark_kitchen_alerts_v3");
  assert.equal(channelIdForKind("ready"), "lakemark_ready_alerts_v3");
  assert.equal(channelIdForKind("broadcast"), "lakemark_broadcast_alerts_v3");
  assert.equal(channelIdForKind("app_update"), "lakemark_broadcast_alerts_v3");

  const updatePush = buildHybridFcmPayload({
    notifId: "upd_320",
    notif: {
      senderUid: "uid_mgr",
      senderRole: "manager",
      senderBrandName: "Manager",
      targetRoles: ["all"],
      targetUserId: "",
      orderId: "",
      kind: "app_update",
      apkUrl: "https://storage.googleapis.com/lakemark-app/lakemark-v3.2.0.apk",
      message: "Lakemark v3.2.0 is ready with Amharic support & profile photos.",
    },
  });
  assert.equal(updatePush.channelId, "lakemark_broadcast_alerts_v3");
  assert.equal(updatePush.dataPayload.kind, "app_update");
  assert.equal(updatePush.dataPayload.apkUrl, "https://storage.googleapis.com/lakemark-app/lakemark-v3.2.0.apk");
  assert.equal(updatePush.dataPayload.openNotificationsSheet, "true");
});

test("suppresses creator self-echo on kind=new orders and allows customer broadcasts", () => {
  assert.equal(shouldIncludeTargetUserId("cust_1", "cust_1", "new"), false);
  assert.equal(shouldIncludeTargetUserId("cust_1", "chef_1", "ready"), true);

  assert.deepEqual(validQueryRoles(["reception", "customer", "manager"], "new"), ["reception", "manager"]);
  assert.deepEqual(validQueryRoles(["customer", "manager"], "broadcast"), ["customer", "manager"]);

  const newOrderTopics = resolveTopicFallbackTargets({
    targetRoles: ["reception", "kitchen", "manager"],
    targetUserId: "cust_1",
    senderUid: "cust_1",
    kind: "new",
  });
  assert.equal(newOrderTopics.includes("lakemark_user_cust_1"), false);
  assert.ok(newOrderTopics.includes("lakemark_role_reception"));
  assert.ok(newOrderTopics.includes("lakemark_role_kitchen"));
});

test("filters disabled tokens, self-echoes, workspace role switches, and delivery/waiter order scope", () => {
  const baseOrder = { orderType: "room", assignedWaiterId: "waiter_2" };

  // Disabled notification permission on device token -> blocked
  assert.equal(
    shouldDeliverTokenToRecipient({
      uid: "waiter_2",
      userData: { role: "waiter", active: true },
      tokenData: { token: "tok_1", role: "waiter", notificationsEnabled: false },
      senderUid: "chef_1",
      kind: "ready",
      targetRoles: ["waiter", "reception", "manager"],
      targetUserId: "cust_1",
      orderData: baseOrder,
    }),
    false
  );

  // Assigned waiter receives room order ready push
  assert.equal(
    shouldDeliverTokenToRecipient({
      uid: "waiter_2",
      userData: { role: "waiter", active: true },
      tokenData: { token: "tok_1", role: "waiter", notificationsEnabled: true },
      senderUid: "chef_1",
      kind: "ready",
      targetRoles: ["waiter", "reception", "manager"],
      targetUserId: "cust_1",
      orderData: baseOrder,
    }),
    true
  );

  // Unassigned waiter_1 does NOT receive room order assigned to waiter_2
  assert.equal(
    shouldDeliverTokenToRecipient({
      uid: "waiter_1",
      userData: { role: "waiter", active: true },
      tokenData: { token: "tok_2", role: "waiter", notificationsEnabled: true },
      senderUid: "chef_1",
      kind: "ready",
      targetRoles: ["waiter", "reception", "manager"],
      targetUserId: "cust_1",
      orderData: baseOrder,
    }),
    false
  );

  // Delivery does NOT receive room order ready push
  assert.equal(
    shouldDeliverTokenToRecipient({
      uid: "deliv_1",
      userData: { role: "delivery", active: true },
      tokenData: { token: "tok_3", role: "delivery", notificationsEnabled: true },
      senderUid: "chef_1",
      kind: "ready",
      targetRoles: ["waiter", "delivery", "manager"],
      targetUserId: "cust_1",
      orderData: baseOrder,
    }),
    false
  );

  // Admin who switched workspace to customer on this device does not receive another customer's order ready push
  assert.equal(
    shouldDeliverTokenToRecipient({
      uid: "mgr_1",
      userData: { role: "manager", active: true },
      tokenData: { token: "tok_4", role: "customer", notificationsEnabled: true },
      senderUid: "chef_1",
      kind: "ready",
      targetRoles: ["waiter", "customer", "manager"],
      targetUserId: "cust_1",
      orderData: baseOrder,
    }),
    false
  );

  // Unapproved/disabled customer must NOT receive notifications
  assert.equal(
    shouldDeliverTokenToRecipient({
      uid: "cust_pending",
      userData: { role: "customer", active: false },
      tokenData: { token: "tok_5", role: "customer", notificationsEnabled: true },
      senderUid: "chef_1",
      kind: "broadcast",
      targetRoles: ["all", "customer"],
      targetUserId: "cust_pending",
      orderData: baseOrder,
    }),
    false
  );
});

const {
  hashPassword,
  hashPasswordPbkdf2,
  verifyPassword,
  normalizeUsername,
  isValidUsername,
  isValidPassword,
  isAllowedStaffRole,
  isCallerManager,
  resolveUpdatedStaffFields,
} = require("./staffAuth");

test("staffAuth scrypt & pbkdf2_sha256 hashing, timing-safe verification, input validation, and manager authorization", () => {
  const { saltHex, hashHex, hashAlgo } = hashPassword("Kitchen#2026");
  assert.equal(hashAlgo, "scrypt");
  assert.equal(verifyPassword("Kitchen#2026", saltHex, hashHex, hashAlgo), true);
  assert.equal(verifyPassword("wrong-pass", saltHex, hashHex, hashAlgo), false);
  assert.equal(verifyPassword("Kitchen#2026", "00".repeat(16), hashHex, hashAlgo), false);

  const pbkdf2Known = hashPasswordPbkdf2("Lakemark#101", "00112233445566778899aabbccddeeff");
  assert.equal(pbkdf2Known.hashAlgo, "pbkdf2_sha256");
  assert.equal(pbkdf2Known.hashHex.length, 128);
  assert.equal(verifyPassword("Lakemark#101", pbkdf2Known.saltHex, pbkdf2Known.hashHex, "pbkdf2_sha256"), true);
  assert.equal(verifyPassword("wrong-pass", pbkdf2Known.saltHex, pbkdf2Known.hashHex, "pbkdf2_sha256"), false);

  assert.equal(normalizeUsername("  @Chef_Abel "), "chef_abel");
  assert.equal(isValidUsername(normalizeUsername("@Chef_Abel")), true);
  assert.equal(isValidUsername("ab"), false);
  assert.equal(isValidUsername("bad user!"), false);
  assert.equal(isValidPassword("12345"), false);
  assert.equal(isValidPassword("123456"), true);
  assert.equal(isAllowedStaffRole("kitchen"), true);
  assert.equal(isAllowedStaffRole("waiter"), true);
  assert.equal(isAllowedStaffRole("customer"), false);
  assert.equal(isAllowedStaffRole("manager"), false);

  assert.equal(
    isCallerManager({
      decodedToken: { email: "abebaw4040@gmail.com", email_verified: true },
      callerUserData: null,
    }),
    true
  );
  assert.equal(
    isCallerManager({
      decodedToken: { email: "abebaw4040@gmail.com", email_verified: false },
      callerUserData: null,
    }),
    false
  );
  assert.equal(
    isCallerManager({
      decodedToken: { email: "unauthorized@example.com", email_verified: true },
      callerUserData: null,
    }),
    false
  );
  assert.equal(
    isCallerManager({
      decodedToken: { email: "staff@example.com", email_verified: true },
      callerUserData: { role: "manager", active: true },
    }),
    true
  );
  assert.equal(
    isCallerManager({
      decodedToken: { email: "cust@example.com", email_verified: true },
      callerUserData: { role: "customer", active: true },
    }),
    false
  );

  const edited = resolveUpdatedStaffFields({
    existingUser: { name: "Abe", username: "abe", role: "kitchen", active: true },
    body: { name: "Abe Chef", username: "@abe_chef", role: "waiter", active: false, newPassword: "NewPass#99" },
  });
  assert.equal(edited.ok, true);
  assert.equal(edited.name, "Abe Chef");
  assert.equal(edited.username, "abe_chef");
  assert.equal(edited.role, "waiter");
  assert.equal(edited.active, false);
  assert.equal(edited.newPassword, "NewPass#99");

  const missingUserForPw = resolveUpdatedStaffFields({
    existingUser: { name: "NoUser", username: "", role: "cashier", active: true },
    body: { newPassword: "NewPass#99" },
  });
  assert.equal(missingUserForPw.ok, false);
});
