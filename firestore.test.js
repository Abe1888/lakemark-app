const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "abebaw4040@gmail.com";
const STAFF_UID = "kitchen_uid_1";
const CUSTOMER_UID = "customer_uid_1";
const OTHER_CUSTOMER_UID = "customer_uid_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("menu_items").get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("brands").get());
  await assertFails(unauthDb.collection("notifications").get());
  await assertFails(unauthDb.collection("counters").doc("orders").get());
});

test("Users: self-registration forces customer active=false; privilege escalation, email spoofing & staff_credentials blocked", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, {
    email: "guest@example.com",
    email_verified: true,
  }).firestore();
  const now = pastDate();

  // Customer self-registration with active=true must fail (disabled by default rule)
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      email: "guest@example.com",
      name: "Guest User",
      role: "customer",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid customer self-registration (disabled by default until Manager permanently activates)
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      email: "guest@example.com",
      name: "Guest User",
      role: "customer",
      active: false,
      permanentlyActivated: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Pending customer can read own profile status, but cannot read orders/menu or self-activate
  await assertSucceeds(custDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(custDb.collection("menu_items").where("available", "==", true).get());
  await assertFails(custDb.collection("orders").where("userId", "==", CUSTOMER_UID).get());
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      active: true,
      permanentlyActivated: true,
      updatedAt: pastDate(1000),
    })
  );

  // Self-escalation to manager must fail
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      role: "manager",
      updatedAt: pastDate(1000),
    })
  );

  // Spoofed admin email with email_verified: false must fail
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("users").doc("spoof_uid").set({
      userId: "spoof_uid",
      email: ADMIN_EMAIL,
      name: "Spoofer",
      role: "manager",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Verified admin email succeeds as manager and can permanently activate customer
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      name: "Hotel Manager",
      role: "manager",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(CUSTOMER_UID).update({
      active: true,
      permanentlyActivated: true,
      updatedAt: pastDate(500),
    })
  );
  // Once permanently activated, customer cannot be disabled back to active=false while remaining customer
  await assertFails(
    adminDb.collection("users").doc(CUSTOMER_UID).update({
      active: false,
      updatedAt: pastDate(100),
    })
  );

  // staff_credentials collection is locked for all client SDKs (even admin client)
  await assertFails(adminDb.collection("staff_credentials").doc(CUSTOMER_UID).get());
  await assertFails(adminDb.collection("staff_credentials").doc("staff_1").set({ username: "chef" }));
});

test("Orders: customer isolation, staff workflow transitions, terminal locking & shadow fields", async () => {
  const now = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      email: "c1@example.com",
      name: "Activated Guest",
      role: "customer",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      email: "chef@lakemark.com",
      name: "Head Chef",
      role: "kitchen",
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("orders").doc("order_other").set({
      orderNumber: 1001,
      userId: OTHER_CUSTOMER_UID,
      createdByRole: "customer",
      lastUpdatedByRole: "customer",
      lastUpdatedByUid: OTHER_CUSTOMER_UID,
      customerName: "Other Guest",
      orderType: "room",
      location: "101",
      status: "new",
      items: [{ menuItemId: "m0", name: "Doro Wat", quantity: 1, price: 420 }],
      specialNote: "",
      cancelReason: "",
      subtotal: 420,
      total: 483,
      createdAt: now,
      updatedAt: now,
    });
  });

  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, { email: "c1@example.com", email_verified: true }).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID, { email: "chef@lakemark.com", email_verified: true }).firestore();

  // Customer unfiltered list must fail; filtered list succeeds
  await assertFails(custDb.collection("orders").get());
  await assertFails(custDb.collection("orders").doc("order_other").get());
  await assertSucceeds(custDb.collection("orders").where("userId", "==", CUSTOMER_UID).get());

  // Customer creates valid order (NO @DocumentId id field)
  await assertSucceeds(
    custDb.collection("orders").doc("order_1002").set({
      orderNumber: 1002,
      userId: CUSTOMER_UID,
      createdByRole: "customer",
      lastUpdatedByRole: "customer",
      lastUpdatedByUid: CUSTOMER_UID,
      customerName: "Abel",
      orderType: "dine",
      location: "Table 3",
      status: "new",
      items: [{ menuItemId: "m1", name: "Kitfo", quantity: 1, price: 480 }],
      specialNote: "Extra injera",
      cancelReason: "",
      subtotal: 480,
      total: 552,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Shadow field `id` must fail
  await assertFails(
    custDb.collection("orders").doc("order_shadow").set({
      id: "order_shadow",
      orderNumber: 1003,
      userId: CUSTOMER_UID,
      createdByRole: "customer",
      lastUpdatedByRole: "customer",
      lastUpdatedByUid: CUSTOMER_UID,
      customerName: "Abel",
      orderType: "dine",
      location: "Table 4",
      status: "new",
      items: [{ menuItemId: "m1", name: "Kitfo", quantity: 1, price: 480 }],
      specialNote: "",
      cancelReason: "",
      subtotal: 480,
      total: 552,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot self-advance status to ready
  await assertFails(
    custDb.collection("orders").doc("order_1002").update({
      status: "ready",
      readyAt: pastDate(1000),
      updatedAt: pastDate(1000),
      lastUpdatedByRole: "customer",
      lastUpdatedByUid: CUSTOMER_UID,
    })
  );

  // Staff cannot skip directly from new -> ready
  await assertFails(
    staffDb.collection("orders").doc("order_1002").update({
      status: "ready",
      readyAt: pastDate(1000),
      updatedAt: pastDate(1000),
      lastUpdatedByRole: "kitchen",
      lastUpdatedByUid: STAFF_UID,
    })
  );

  // Staff valid transition: new -> preparing -> ready -> done
  await assertSucceeds(
    staffDb.collection("orders").doc("order_1002").update({
      status: "preparing",
      confirmedAt: pastDate(3000),
      preparingAt: pastDate(3000),
      updatedAt: pastDate(3000),
      lastUpdatedByRole: "kitchen",
      lastUpdatedByUid: STAFF_UID,
    })
  );
  await assertSucceeds(
    staffDb.collection("orders").doc("order_1002").update({
      status: "ready",
      readyAt: pastDate(2000),
      updatedAt: pastDate(2000),
      lastUpdatedByRole: "kitchen",
      lastUpdatedByUid: STAFF_UID,
    })
  );
  await assertSucceeds(
    staffDb.collection("orders").doc("order_1002").update({
      status: "done",
      doneAt: pastDate(1000),
      updatedAt: pastDate(1000),
      lastUpdatedByRole: "kitchen",
      lastUpdatedByUid: STAFF_UID,
    })
  );

  // Terminal state lock: staff cannot mutate a 'done' order
  await assertFails(
    staffDb.collection("orders").doc("order_1002").update({
      status: "cancelled",
      cancelReason: "Too late",
      cancelledAt: pastDate(500),
      updatedAt: pastDate(500),
      lastUpdatedByRole: "kitchen",
      lastUpdatedByUid: STAFF_UID,
    })
  );
});

test("Menu, Brands, Notifications, and Counters: parity and authorization", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      email: "c1@example.com",
      name: "Activated Guest",
      role: "customer",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
  });
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, { email: "c1@example.com", email_verified: true }).firestore();

  // Admin creates menu item; customer can query available=true, cannot create
  await assertSucceeds(
    adminDb.collection("menu_items").doc("m0").set({
      name: "Doro Wat",
      category: "Classics",
      description: "Chicken in berbere, egg, injera",
      price: 420,
      available: true,
      sortOrder: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(custDb.collection("menu_items").where("available", "==", true).get());
  await assertFails(
    custDb.collection("menu_items").doc("m99").set({
      name: "Hack Dish",
      category: "Classics",
      description: "Free",
      price: 1,
      available: true,
      sortOrder: 99,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Counter atomic increment
  await assertSucceeds(
    custDb.collection("counters").doc("orders").set({
      nextOrderNumber: 1007,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    custDb.collection("counters").doc("orders").update({
      nextOrderNumber: 1008,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    custDb.collection("counters").doc("orders").update({
      nextOrderNumber: 9999,
      updatedAt: pastDate(500),
    })
  );

  // Brands: Admin can save brand for kitchen; customer cannot overwrite kitchen brand
  await assertSucceeds(
    adminDb.collection("brands").doc("kitchen").set({
      roleKey: "kitchen",
      primaryHex: "#FBE2CE",
      secondaryHex: "#7A2B0A",
      accentHex: "#F26B1D",
      brandName: "Kitchen",
      mark: "",
      updatedBy: ADMIN_UID,
      updatedAt: now,
    })
  );
  await assertFails(
    custDb.collection("brands").doc("kitchen").update({
      brandName: "Hijacked",
      updatedBy: CUSTOMER_UID,
      updatedAt: pastDate(500),
    })
  );

  // Notifications: Admin broadcast succeeds; customer can read targetUserId and customer/all broadcasts, blocked from staff-only
  await assertSucceeds(
    adminDb.collection("notifications").doc("n1").set({
      senderUid: ADMIN_UID,
      senderRole: "manager",
      senderBrandName: "Manager",
      message: "Dinner service starts in 15 mins",
      targetRoles: ["all"],
      targetUserId: "",
      orderId: "",
      kind: "broadcast",
      createdAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("notifications").doc("n_staff_only").set({
      senderUid: ADMIN_UID,
      senderRole: "manager",
      senderBrandName: "Manager",
      message: "Kitchen prep check",
      targetRoles: ["kitchen", "manager"],
      targetUserId: "",
      orderId: "",
      kind: "broadcast",
      createdAt: now,
    })
  );
  await assertSucceeds(custDb.collection("notifications").where("targetUserId", "==", CUSTOMER_UID).get());
  const custBroadcastSnap = await assertSucceeds(
    custDb.collection("notifications").where("targetRoles", "array-contains-any", ["all", "customer"]).get()
  );
  if (custBroadcastSnap.docs.some((d) => d.id === "n_staff_only")) {
    throw new Error("Customer should not see staff-only broadcasts");
  }
  await assertFails(custDb.collection("notifications").doc("n_staff_only").get());

  // Waiter coverage: staff/admin can read, manager & owning waiter can write, customer denied
  await assertSucceeds(
    testEnv.withSecurityRulesDisabled(async (ctx) => {
      await ctx.firestore().collection("users").doc(STAFF_UID).set({
        userId: STAFF_UID,
        email: "chef@lakemark.com",
        name: "Chef",
        role: "kitchen",
        active: true,
        createdAt: now,
        updatedAt: now,
      });
      await ctx.firestore().collection("users").doc("waiter_1").set({
        userId: "waiter_1",
        email: "waiter1@lakemark.com",
        name: "Waiter 1",
        role: "waiter",
        active: true,
        createdAt: now,
        updatedAt: now,
      });
    })
  );
  const waiterDb = testEnv.authenticatedContext("waiter_1", { email: "waiter1@lakemark.com", email_verified: true }).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID, { email: "chef@lakemark.com", email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("waiter_coverage").doc("waiter_1").set({
      userId: "waiter_1",
      waiterName: "Waiter 1",
      onDuty: true,
      areas: ["f1", "din"],
      updatedAt: now,
    })
  );
  await assertSucceeds(waiterDb.collection("waiter_coverage").get());
  await assertSucceeds(
    waiterDb.collection("waiter_coverage").doc("waiter_1").set({
      userId: "waiter_1",
      waiterName: "Waiter 1",
      onDuty: false,
      areas: ["f1", "din"],
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(custDb.collection("waiter_coverage").get());
  await assertFails(
    staffDb.collection("waiter_coverage").doc("waiter_1").set({
      userId: "waiter_1",
      waiterName: "Waiter 1",
      onDuty: false,
      areas: ["lob"],
      updatedAt: pastDate(500),
    })
  );
});

test("FCM device tokens: owner-only CRUD, strict schema, immutable metadata, and cross-user isolation", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      email: "c1@example.com",
      name: "Activated Guest",
      role: "customer",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
    await ctx.firestore().collection("users").doc("pending_cust").set({
      userId: "pending_cust",
      email: "pending@example.com",
      name: "Pending Guest",
      role: "customer",
      active: false,
      permanentlyActivated: false,
      createdAt: now,
      updatedAt: now,
    });
  });
  const pendingDb = testEnv.authenticatedContext("pending_cust", { email: "pending@example.com", email_verified: true }).firestore();
  await assertFails(
    pendingDb.collection("users").doc("pending_cust").collection("fcm_tokens").doc("dev_pending").set({
      token: "fcm_token_pending",
      userId: "pending_cust",
      deviceId: "dev_pending",
      role: "customer",
      platform: "android",
      appVersion: "3.1.0",
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, { email: "c1@example.com", email_verified: true }).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_CUSTOMER_UID, { email: "c2@example.com", email_verified: true }).firestore();
  const removedAdminDb = testEnv.authenticatedContext("unauthorized_1", { email: "unauthorized@example.com", email_verified: true }).firestore();

  const tokenRef = custDb.collection("users").doc(CUSTOMER_UID).collection("fcm_tokens").doc("dev_android_1");

  // Owner registers FCM device token
  await assertSucceeds(
    tokenRef.set({
      token: "fcm_token_sample_abc123",
      userId: CUSTOMER_UID,
      deviceId: "dev_android_1",
      role: "customer",
      platform: "android",
      appVersion: "3.1.0",
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner can list & read own tokens
  await assertSucceeds(custDb.collection("users").doc(CUSTOMER_UID).collection("fcm_tokens").get());

  // Cross-user read/write/delete must fail
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).collection("fcm_tokens").doc("dev_android_1").get());
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).collection("fcm_tokens").doc("dev_android_1").delete());
  await assertFails(
    otherDb.collection("users").doc(CUSTOMER_UID).collection("fcm_tokens").doc("dev_hijack").set({
      token: "evil_token",
      userId: OTHER_CUSTOMER_UID,
      deviceId: "dev_hijack",
      role: "manager",
      platform: "android",
      appVersion: "3.1.0",
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner valid token refresh / role update
  await assertSucceeds(
    tokenRef.update({
      token: "fcm_token_refreshed_xyz789",
      role: "waiter",
      appVersion: "3.1.0",
      notificationsEnabled: false,
      updatedAt: pastDate(1000),
    })
  );

  // Immutable createdAt or deviceId tamper must fail
  await assertFails(
    tokenRef.update({
      deviceId: "tampered_id",
      updatedAt: pastDate(500),
    })
  );

  // Oversized token must fail
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).collection("fcm_tokens").doc("dev_oversized").set({
      token: "x".repeat(600),
      userId: CUSTOMER_UID,
      deviceId: "dev_oversized",
      role: "customer",
      platform: "android",
      appVersion: "3.1.0",
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner deletes token on sign-out
  await assertSucceeds(tokenRef.delete());

  // Non-allowlisted email cannot self-provision a manager profile
  await assertFails(
    removedAdminDb.collection("users").doc("unauthorized_1").set({
      userId: "unauthorized_1",
      email: "unauthorized@example.com",
      name: "Unauthorized User",
      role: "manager",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Profiles, Menu Images, Saved Items, Translations, Release Config & Update Notifications", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      name: "Hotel Manager",
      role: "manager",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      email: "c1@example.com",
      name: "Activated Guest",
      role: "customer",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(OTHER_CUSTOMER_UID).set({
      userId: OTHER_CUSTOMER_UID,
      email: "c2@example.com",
      name: "Other Guest",
      role: "customer",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      email: "chef@lakemark.local",
      name: "Head Chef",
      role: "kitchen",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, { email: "c1@example.com", email_verified: true }).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_CUSTOMER_UID, { email: "c2@example.com", email_verified: true }).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID, { email: "chef@lakemark.local", email_verified: true }).firestore();

  // User updates own photoUrl and name
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      photoUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/profile.jpg",
      name: "Activated Guest Updated",
      updatedAt: pastDate(1000),
    })
  );
  // Cannot modify another user's photoUrl unless Manager
  await assertFails(
    otherDb.collection("users").doc(CUSTOMER_UID).update({
      photoUrl: "https://evil.example/a.jpg",
      updatedAt: pastDate(900),
    })
  );

  // Manager adds menu item with imageUrl
  await assertSucceeds(
    adminDb.collection("menu_items").doc("m_img_1").set({
      name: "Tibs",
      category: "Grill",
      description: "Sauteed beef strips",
      price: 450,
      available: true,
      sortOrder: 2,
      imageUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/tibs.jpg",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Saved items: activated user can bookmark/remove own, cross-user denied
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("saved_items").doc("m_img_1").set({
      menuItemId: "m_img_1",
      name: "Tibs",
      category: "Grill",
      price: 450,
      imageUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/tibs.jpg",
      savedAt: now,
    })
  );
  await assertSucceeds(custDb.collection("users").doc(CUSTOMER_UID).collection("saved_items").get());
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).collection("saved_items").get());
  await assertFails(
    otherDb.collection("users").doc(CUSTOMER_UID).collection("saved_items").doc("m_img_1").delete()
  );
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("saved_items").doc("m_img_1").delete()
  );

  // Translations: Manager writes, staff & customers read, non-managers cannot write
  await assertSucceeds(
    adminDb.collection("translations").doc("nav_home").set({
      key: "nav_home",
      category: "Navigation",
      en: "Home",
      am: "መነሻ",
      enVersion: 1,
      updatedBy: ADMIN_UID,
      updatedAt: now,
    })
  );
  await assertSucceeds(staffDb.collection("translations").get());
  await assertSucceeds(custDb.collection("translations").doc("nav_home").get());
  await assertFails(
    staffDb.collection("translations").doc("nav_home").update({
      am: "Hack",
      updatedBy: STAFF_UID,
      updatedAt: pastDate(500),
    })
  );

  // Release config & app_update notifications: Manager-only writes
  await assertSucceeds(
    adminDb.collection("app_config").doc("release").set({
      latestVersionCode: 14,
      latestVersionName: "3.4.0",
      apkDownloadUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/lakemark-v3.4.0.apk",
      apkSha256: "a".repeat(64),
      releaseNotes: "EN/AM localization & verified release delivery.",
      mandatory: false,
      updatedBy: ADMIN_UID,
      updatedAt: now,
    })
  );
  await assertSucceeds(custDb.collection("app_config").doc("release").get());
  await assertFails(
    staffDb.collection("app_config").doc("release").set({
      latestVersionCode: 99,
      latestVersionName: "9.9.9",
      apkDownloadUrl: "https://evil.example/app.apk",
      releaseNotes: "Hack",
      mandatory: true,
      updatedBy: STAFF_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("notifications").doc("n_upd_1").set({
      senderUid: ADMIN_UID,
      senderRole: "manager",
      senderBrandName: "Manager",
      message: "Lakemark v3.2.0 is ready to install.",
      targetRoles: ["all"],
      targetUserId: "",
      orderId: "",
      kind: "app_update",
      apkUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/lakemark-v3.2.0.apk",
      createdAt: now,
    })
  );
  await assertFails(
    staffDb.collection("notifications").doc("n_upd_hack").set({
      senderUid: STAFF_UID,
      senderRole: "kitchen",
      senderBrandName: "Kitchen",
      message: "Fake update",
      targetRoles: ["all"],
      targetUserId: "",
      orderId: "",
      kind: "app_update",
      apkUrl: "https://evil.example/fake.apk",
      createdAt: now,
    })
  );
});

test("Manager can provision, list, and edit staff accounts + write-only credentials; non-managers blocked", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      name: "Hotel Manager",
      role: "manager",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      email: "chef@lakemark.local",
      username: "kitchen1",
      name: "Chef Dawit",
      role: "kitchen",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID, { email: "chef@lakemark.local", email_verified: true }).firestore();

  const newStaffId = "staff_reception1";
  const validHash = "a".repeat(128);
  const validSalt = "b".repeat(32);

  // Manager creates staff user + write-only credentials
  await assertSucceeds(
    adminDb.collection("users").doc(newStaffId).set({
      userId: newStaffId,
      email: "reception1@staff.lakemark.local",
      username: "reception1",
      name: "Hanna Tadesse",
      role: "reception",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("staff_credentials").doc(newStaffId).set({
      userId: newStaffId,
      username: "reception1",
      passwordHash: validHash,
      passwordSalt: validSalt,
      hashAlgo: "pbkdf2_sha256",
      role: "reception",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Even Manager cannot read staff_credentials
  await assertFails(adminDb.collection("staff_credentials").doc(newStaffId).get());
  await assertFails(adminDb.collection("staff_credentials").get());

  // Manager lists staff and edits username, role, name, active, and credentials
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(newStaffId).update({
      name: "Hanna Senior",
      username: "reception_lead",
      role: "cashier",
      active: false,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("staff_credentials").doc(newStaffId).update({
      username: "reception_lead",
      passwordHash: "c".repeat(128),
      passwordSalt: "d".repeat(32),
      hashAlgo: "pbkdf2_sha256",
      role: "cashier",
      updatedAt: pastDate(1000),
    })
  );

  // Non-manager staff cannot create another staff member or modify credentials
  await assertFails(
    staffDb.collection("users").doc("rogue_staff").set({
      userId: "rogue_staff",
      email: "rogue@staff.lakemark.local",
      username: "rogue1",
      name: "Rogue",
      role: "waiter",
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    staffDb.collection("staff_credentials").doc(newStaffId).update({
      passwordHash: "e".repeat(128),
      updatedAt: pastDate(500),
    })
  );
});



