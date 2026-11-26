"use strict";

const { cert, getApps, initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const {
  buildHybridFcmPayload,
  resolveTopicFallbackTargets,
  shouldDeliverTokenToRecipient,
  shouldIncludeTargetUserId,
  validQueryRoles,
} = require("./pushRouting");
const {
  extractBearerToken,
  hashPassword,
  isAllowedStaffRole,
  isCallerManager,
  isValidPassword,
  isValidUsername,
  normalizeUsername,
  resolveUpdatedStaffFields,
  verifyPassword,
} = require("./staffAuth");

const DATABASE_ID = "(default)";
const TRIGGER_REGION = "us-central1";

/**
 * Initializes Firebase Admin SDK once using Google Cloud default credentials
 * or an explicit service account JSON/base64 env var if provided.
 */
function initFirebaseAdmin() {
  if (getApps().length > 0) {
    return getApps()[0];
  }
  const rawJson = String(process.env.FIREBASE_SERVICE_ACCOUNT_JSON || "").trim();
  const rawBase64 = String(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64 || "").trim();
  if (rawJson) {
    const parsed = JSON.parse(rawJson);
    return initializeApp({ credential: cert(parsed), projectId: parsed.project_id });
  }
  if (rawBase64) {
    const decoded = Buffer.from(rawBase64, "base64").toString("utf8");
    const parsed = JSON.parse(decoded);
    return initializeApp({ credential: cert(parsed), projectId: parsed.project_id });
  }
  return initializeApp();
}

function hasAdminCredentials() {
  return Boolean(
    getApps().length > 0 ||
    String(process.env.FIREBASE_SERVICE_ACCOUNT_JSON || "").trim() ||
    String(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64 || "").trim() ||
    String(process.env.GOOGLE_APPLICATION_CREDENTIALS || "").trim() ||
    String(process.env.K_SERVICE || "").trim() ||
    String(process.env.FUNCTION_TARGET || "").trim()
  );
}

function getDb() {
  if (!hasAdminCredentials()) {
    const err = new Error("Backend Firebase credentials are not configured. Add FIREBASE_SERVICE_ACCOUNT_JSON in Render Environment Variables.");
    err.statusCode = 503;
    throw err;
  }
  initFirebaseAdmin();
  return getFirestore(DATABASE_ID);
}

/**
 * Claims a notification document atomically so concurrent HTTP + onSnapshot
 * triggers never send duplicate FCM pushes for the same notification.
 */
async function claimNotificationForDispatch(db, notifRef) {
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(notifRef);
    if (!snap.exists) {
      return { claimed: false, reason: "not_found", data: null };
    }
    const data = snap.data() || {};
    if (data.fcmDispatchedAt) {
      return { claimed: false, reason: "already_dispatched", data };
    }
    tx.update(notifRef, {
      fcmDispatchedAt: FieldValue.serverTimestamp(),
    });
    return { claimed: true, reason: "claimed", data };
  });
}

async function dispatchNotificationData({ notifId, notif, db = getDb(), messaging = getMessaging(), log = console }) {
  const senderUid = String(notif.senderUid || "");
  const senderRole = String(notif.senderRole || "manager").toLowerCase();
  const targetRoles = Array.isArray(notif.targetRoles) ? notif.targetRoles.map(String) : ["all"];
  const targetUserId = String(notif.targetUserId || "");
  const orderId = String(notif.orderId || "");
  const kind = String(notif.kind || "ping").toLowerCase();
  const message = String(notif.message || "").trim();
  if (!message) {
    log.warn("Skipped empty notification document", { notifId, database: DATABASE_ID });
    return { skipped: true, reason: "empty_message", matchedTokens: 0 };
  }

  const [brandSnap, orderSnap] = await Promise.all([
    db.collection("brands").doc(senderRole).get().catch((err) => {
      log.warn("Failed to load sender brand", { notifId, senderRole, error: err.message });
      return null;
    }),
    orderId
      ? db.collection("orders").doc(orderId).get().catch((err) => {
          log.warn("Failed to load order metadata", { notifId, orderId, error: err.message });
          return null;
        })
      : Promise.resolve(null),
  ]);

  const brandData = brandSnap && brandSnap.exists ? brandSnap.data() : null;
  const orderData = orderSnap && orderSnap.exists ? orderSnap.data() : null;

  const { channelId, fcmBase } = buildHybridFcmPayload({
    notifId,
    notif,
    brandData,
    orderData,
  });

  const recipientUsers = new Map();
  if (shouldIncludeTargetUserId(targetUserId, senderUid, kind)) {
    const uSnap = await db.collection("users").doc(targetUserId).get().catch(() => null);
    if (uSnap && uSnap.exists) {
      const uData = uSnap.data() || {};
      if (uData.active === true) recipientUsers.set(uSnap.id, uData);
    }
  }

  if (targetRoles.includes("all")) {
    const allActive = await db.collection("users").where("active", "==", true).get();
    allActive.forEach((doc) => recipientUsers.set(doc.id, doc.data()));
  } else {
    const queryRoles = validQueryRoles(targetRoles, kind);
    if (queryRoles.length > 0) {
      const roleSnap = await db.collection("users")
        .where("active", "==", true)
        .where("role", "in", queryRoles.slice(0, 10))
        .get();
      roleSnap.forEach((doc) => recipientUsers.set(doc.id, doc.data()));
    }
    if (!queryRoles.includes("manager")) {
      const mgrSnap = await db.collection("users")
        .where("active", "==", true)
        .where("role", "==", "manager")
        .get();
      mgrSnap.forEach((doc) => recipientUsers.set(doc.id, doc.data()));
    }
  }

  const tokenEntries = [];
  const seenTokens = new Set();
  let skippedDisabledOrFiltered = 0;

  await Promise.all(
    Array.from(recipientUsers.entries()).map(async ([uid, userData]) => {
      if (senderUid && uid === senderUid) return;
      const tokensSnap = await db.collection("users").doc(uid).collection("fcm_tokens").get();
      tokensSnap.forEach((tDoc) => {
        const tData = tDoc.data() || {};
        if (
          !shouldDeliverTokenToRecipient({
            uid,
            userData,
            tokenData: tData,
            senderUid,
            kind,
            targetRoles,
            targetUserId,
            orderData,
          })
        ) {
          skippedDisabledOrFiltered += 1;
          return;
        }
        const token = String(tData.token || "").trim();
        if (!token || seenTokens.has(token)) return;
        seenTokens.add(token);
        tokenEntries.push({ token, ref: tDoc.ref });
      });
    })
  );

  log.info("Dispatching Lakemark notification", {
    notifId,
    kind,
    orderId,
    channelId,
    database: DATABASE_ID,
    candidateUsers: recipientUsers.size,
    matchedTokens: tokenEntries.length,
    filteredTokens: skippedDisabledOrFiltered,
  });

  if (tokenEntries.length === 0) {
    const topicTargets = resolveTopicFallbackTargets({
      targetRoles,
      targetUserId,
      senderUid,
      kind,
    });
    await Promise.all(
      topicTargets.map((topic) =>
        messaging
          .send({ topic, ...fcmBase })
          .then((messageId) => {
            log.info("Topic fallback FCM sent", { notifId, topic, messageId });
          })
          .catch((err) => {
            log.warn("Topic fallback FCM failed", { notifId, topic, error: err.message });
          })
      )
    );
    return { skipped: false, topicFallbackCount: topicTargets.length, matchedTokens: 0 };
  }

  let totalSuccess = 0;
  let totalFailure = 0;
  for (let i = 0; i < tokenEntries.length; i += 500) {
    const batch = tokenEntries.slice(i, i + 500);
    const response = await messaging.sendEachForMulticast({
      tokens: batch.map((b) => b.token),
      ...fcmBase,
    });
    totalSuccess += response.successCount;
    totalFailure += response.failureCount;

    const deletions = [];
    response.responses.forEach((res, idx) => {
      if (!res.success && res.error) {
        const code = res.error.code || "unknown";
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          deletions.push(batch[idx].ref.delete().catch(() => null));
        }
      }
    });
    if (deletions.length > 0) {
      await Promise.all(deletions);
    }
  }

  return {
    skipped: false,
    matchedTokens: tokenEntries.length,
    successCount: totalSuccess,
    failureCount: totalFailure,
  };
}

async function verifyAuthenticatedRequest(req, auth = getAuth(), log = console) {
  const token = extractBearerToken(req.headers && req.headers.authorization);
  if (!token) {
    return { ok: false, status: 401, error: "Authentication required." };
  }
  try {
    const decodedToken = await auth.verifyIdToken(token);
    return { ok: true, decodedToken };
  } catch (err) {
    log.warn("ID token verification failed", { error: err.message });
    return { ok: false, status: 401, error: "Invalid or expired session token." };
  }
}

async function verifyManagerRequest(req, db, auth = getAuth(), log = console) {
  const authCheck = await verifyAuthenticatedRequest(req, auth, log);
  if (!authCheck.ok) return authCheck;
  const callerSnap = await db.collection("users").doc(authCheck.decodedToken.uid).get().catch(() => null);
  const callerUserData = callerSnap && callerSnap.exists ? callerSnap.data() : null;
  if (!isCallerManager({ decodedToken: authCheck.decodedToken, callerUserData })) {
    return { ok: false, status: 403, error: "Only an active Lakemark Manager can perform this action." };
  }
  return { ok: true, decodedToken: authCheck.decodedToken };
}

async function handleStaffSignIn(req, res, deps = {}) {
  let db, auth;
  try {
    db = deps.db || getDb();
    auth = deps.auth || getAuth();
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
    return;
  }
  const log = deps.log || console;

  try {
    const body = req.body && typeof req.body === "object" ? req.body : {};
    const username = normalizeUsername(body.username);
    const password = typeof body.password === "string" ? body.password : "";

    if (!isValidUsername(username) || !isValidPassword(password)) {
      res.status(400).json({ error: "Enter a valid staff username and password." });
      return;
    }

    const credSnap = await db.collection("staff_credentials")
      .where("username", "==", username)
      .limit(1)
      .get();

    if (credSnap.empty) {
      res.status(401).json({ error: "Invalid username or password." });
      return;
    }

    const credDoc = credSnap.docs[0];
    const credData = credDoc.data() || {};
    if (!verifyPassword(password, credData.passwordSalt, credData.passwordHash, credData.hashAlgo)) {
      res.status(401).json({ error: "Invalid username or password." });
      return;
    }

    const staffUid = credDoc.id;
    const userSnap = await db.collection("users").doc(staffUid).get();
    if (!userSnap.exists) {
      res.status(403).json({ error: "Staff profile not found." });
      return;
    }
    const userData = userSnap.data() || {};
    if (userData.active !== true) {
      res.status(403).json({ error: "Staff account is disabled by management." });
      return;
    }
    const role = String(userData.role || "").toLowerCase();
    if (!role || role === "customer") {
      res.status(403).json({ error: "Account does not have staff station access." });
      return;
    }

    const customToken = await auth.createCustomToken(staffUid, {
      lakemarkRole: role,
      staffUsername: username,
    });
    log.info("Staff signed in without Google account", { staffUid, role, username });
    res.status(200).json({
      customToken,
      uid: staffUid,
      role,
      name: String(userData.name || ""),
      username,
    });
  } catch (err) {
    log.error("staffSignIn failed unexpectedly", { error: err.message });
    res.status(500).json({ error: "Unable to complete staff sign-in right now." });
  }
}

async function handleCreateStaffAccount(req, res, deps = {}) {
  let db, auth;
  try {
    db = deps.db || getDb();
    auth = deps.auth || getAuth();
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
    return;
  }
  const log = deps.log || console;

  let createdUid = null;
  try {
    const authCheck = await verifyManagerRequest(req, db, auth, log);
    if (!authCheck.ok) {
      res.status(authCheck.status).json({ error: authCheck.error });
      return;
    }

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const name = String(body.name || "").trim().slice(0, 60);
    const username = normalizeUsername(body.username);
    const role = String(body.role || "").trim().toLowerCase();
    const password = typeof body.password === "string" ? body.password : "";

    if (!name) {
      res.status(400).json({ error: "Enter the staff member's name." });
      return;
    }
    if (!isValidUsername(username)) {
      res.status(400).json({ error: "Username must be 3-32 characters (letters, numbers, ., _, -)." });
      return;
    }
    if (!isAllowedStaffRole(role)) {
      res.status(400).json({ error: "Choose a valid staff role." });
      return;
    }
    if (!isValidPassword(password)) {
      res.status(400).json({ error: "Password must be at least 6 characters." });
      return;
    }

    const existingCred = await db.collection("staff_credentials")
      .where("username", "==", username)
      .limit(1)
      .get();
    if (!existingCred.empty) {
      res.status(409).json({ error: "That username is already in use." });
      return;
    }

    const userRecord = await auth.createUser({ displayName: name });
    createdUid = userRecord.uid;
    const { saltHex, hashHex, hashAlgo } = hashPassword(password);
    const now = FieldValue.serverTimestamp();

    const batch = db.batch();
    batch.set(db.collection("users").doc(createdUid), {
      userId: createdUid,
      email: `${username}@staff.lakemark.local`,
      username,
      staffPassword: password,
      name,
      role,
      active: true,
      permanentlyActivated: true,
      createdAt: now,
      updatedAt: now,
    });
    batch.set(db.collection("staff_credentials").doc(createdUid), {
      userId: createdUid,
      username,
      passwordHash: hashHex,
      passwordSalt: saltHex,
      hashAlgo,
      role,
      createdAt: now,
      updatedAt: now,
    });
    if (role === "waiter") {
      batch.set(db.collection("waiter_coverage").doc(createdUid), {
        userId: createdUid,
        waiterName: name,
        onDuty: true,
        areas: ["f1"],
        updatedAt: now,
      });
    }
    await batch.commit();

    log.info("Manager created app-managed staff account", {
      managerUid: authCheck.decodedToken.uid,
      staffUid: createdUid,
      username,
      role,
    });
    res.status(201).json({ uid: createdUid, username, name, role });
  } catch (err) {
    if (createdUid) {
      await auth.deleteUser(createdUid).catch(() => null);
    }
    log.error("createStaffAccount failed unexpectedly", { error: err.message });
    res.status(500).json({ error: "Unable to create staff account right now." });
  }
}

async function handleResetStaffPassword(req, res, deps = {}) {
  let db, auth;
  try {
    db = deps.db || getDb();
    auth = deps.auth || getAuth();
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
    return;
  }
  const log = deps.log || console;

  try {
    const authCheck = await verifyManagerRequest(req, db, auth, log);
    if (!authCheck.ok) {
      res.status(authCheck.status).json({ error: authCheck.error });
      return;
    }

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const targetUserId = String(body.targetUserId || "").trim();
    const newPassword = typeof body.newPassword === "string" ? body.newPassword : "";

    if (!targetUserId || targetUserId.length > 128) {
      res.status(400).json({ error: "Valid staff user ID is required." });
      return;
    }
    if (!isValidPassword(newPassword)) {
      res.status(400).json({ error: "New password must be at least 6 characters." });
      return;
    }

    const userSnap = await db.collection("users").doc(targetUserId).get();
    if (!userSnap.exists) {
      res.status(404).json({ error: "Staff account not found." });
      return;
    }
    const userData = userSnap.data() || {};
    const role = String(userData.role || "").toLowerCase();
    const username = normalizeUsername(userData.username);
    if (role === "customer" || !username) {
      res.status(400).json({ error: "Password reset applies only to app-managed staff accounts." });
      return;
    }

    const { saltHex, hashHex, hashAlgo } = hashPassword(newPassword);
    const now = FieldValue.serverTimestamp();
    await Promise.all([
      db.collection("users").doc(targetUserId).set(
        {
          staffPassword: newPassword,
          updatedAt: now,
        },
        { merge: true }
      ),
      db.collection("staff_credentials").doc(targetUserId).set(
        {
          userId: targetUserId,
          username,
          passwordHash: hashHex,
          passwordSalt: saltHex,
          hashAlgo,
          role,
          updatedAt: now,
        },
        { merge: true }
      ),
    ]);

    log.info("Manager reset staff password", {
      managerUid: authCheck.decodedToken.uid,
      targetUserId,
      username,
    });
    res.status(200).json({ ok: true, uid: targetUserId });
  } catch (err) {
    log.error("resetStaffPassword failed unexpectedly", { error: err.message });
    res.status(500).json({ error: "Unable to reset staff password right now." });
  }
}

async function handleUpdateStaffAccount(req, res, deps = {}) {
  let db, auth;
  try {
    db = deps.db || getDb();
    auth = deps.auth || getAuth();
  } catch (err) {
    res.status(err.statusCode || 500).json({ error: err.message });
    return;
  }
  const log = deps.log || console;

  try {
    const authCheck = await verifyManagerRequest(req, db, auth, log);
    if (!authCheck.ok) {
      res.status(authCheck.status).json({ error: authCheck.error });
      return;
    }

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const targetUserId = String(body.targetUserId || "").trim();
    if (!targetUserId || targetUserId.length > 128) {
      res.status(400).json({ error: "Valid staff user ID is required." });
      return;
    }

    const userRef = db.collection("users").doc(targetUserId);
    const userSnap = await userRef.get();
    if (!userSnap.exists) {
      res.status(404).json({ error: "Staff account not found." });
      return;
    }

    const existingUser = userSnap.data() || {};
    if (String(existingUser.role || "").toLowerCase() === "customer") {
      res.status(400).json({ error: "Use customer approval controls for guest accounts." });
      return;
    }

    const resolved = resolveUpdatedStaffFields({ existingUser, body });
    if (!resolved.ok) {
      res.status(resolved.status).json({ error: resolved.error });
      return;
    }

    if (!resolved.isManagerTarget && resolved.username) {
      const dupSnap = await db.collection("staff_credentials")
        .where("username", "==", resolved.username)
        .limit(2)
        .get();
      const takenByOther = dupSnap.docs.some((doc) => doc.id !== targetUserId);
      if (takenByOther) {
        res.status(409).json({ error: "That username is already in use." });
        return;
      }
    }

    await auth.updateUser(targetUserId, {
      displayName: resolved.name,
      disabled: !resolved.active,
    }).catch(async (err) => {
      if (err && err.code === "auth/user-not-found") {
        await auth.createUser({
          uid: targetUserId,
          displayName: resolved.name,
          disabled: !resolved.active,
        }).catch((createErr) => {
          log.warn("Auth createUser fallback warning", { targetUserId, error: createErr.message });
        });
      } else {
        log.warn("Auth updateUser warning", { targetUserId, error: err.message });
      }
    });

    const now = FieldValue.serverTimestamp();
    const batch = db.batch();
    const userUpdates = {
      name: resolved.name,
      role: resolved.role,
      active: resolved.active,
      updatedAt: now,
    };
    if (resolved.active) {
      userUpdates.permanentlyActivated = true;
    }
    if (!resolved.isManagerTarget && resolved.username) {
      userUpdates.username = resolved.username;
    }
    if (!resolved.isManagerTarget && resolved.newPassword) {
      userUpdates.staffPassword = resolved.newPassword;
    }
    batch.set(userRef, userUpdates, { merge: true });

    if (!resolved.isManagerTarget && (resolved.username || resolved.newPassword)) {
      const credRef = db.collection("staff_credentials").doc(targetUserId);
      const existingCredSnap = await credRef.get();
      const existingCred = existingCredSnap.exists ? (existingCredSnap.data() || {}) : {};
      const credUpdates = {
        userId: targetUserId,
        username: resolved.username,
        role: resolved.role,
        updatedAt: now,
      };
      if (resolved.newPassword) {
        const { saltHex, hashHex, hashAlgo } = hashPassword(resolved.newPassword);
        credUpdates.passwordHash = hashHex;
        credUpdates.passwordSalt = saltHex;
        credUpdates.hashAlgo = hashAlgo;
      } else if (!existingCred.passwordHash) {
        res.status(400).json({ error: "Enter a password when assigning a username to this staff account." });
        return;
      }
      if (!existingCredSnap.exists) {
        credUpdates.createdAt = now;
      }
      batch.set(credRef, credUpdates, { merge: true });
    }

    const covRef = db.collection("waiter_coverage").doc(targetUserId);
    if (resolved.role === "waiter") {
      const covSnap = await covRef.get();
      if (covSnap.exists) {
        batch.set(covRef, { waiterName: resolved.name, onDuty: resolved.active, updatedAt: now }, { merge: true });
      } else {
        batch.set(covRef, {
          userId: targetUserId,
          waiterName: resolved.name,
          onDuty: resolved.active,
          areas: ["f1"],
          updatedAt: now,
        });
      }
    } else {
      batch.delete(covRef);
    }

    await batch.commit();

    log.info("Manager updated staff account", {
      managerUid: authCheck.decodedToken.uid,
      targetUserId,
      username: resolved.username,
      role: resolved.role,
      active: resolved.active,
      passwordUpdated: Boolean(resolved.newPassword),
    });
    res.status(200).json({
      uid: targetUserId,
      name: resolved.name,
      username: resolved.username,
      role: resolved.role,
      active: resolved.active,
    });
  } catch (err) {
    log.error("updateStaffAccount failed unexpectedly", { error: err.message });
    res.status(500).json({ error: "Unable to update staff account right now." });
  }
}

/**
 * HTTP trigger for dispatching an existing Firestore notification document via FCM.
 * Used by the Render backend (Spark-free mode) when Android creates notifications/{notifId}.
 */
async function handleDispatchNotificationHttp(req, res, deps = {}) {
  const db = deps.db || getDb();
  const auth = deps.auth || getAuth();
  const messaging = deps.messaging || getMessaging();
  const log = deps.log || console;

  try {
    const authCheck = await verifyAuthenticatedRequest(req, auth, log);
    if (!authCheck.ok) {
      res.status(authCheck.status).json({ error: authCheck.error });
      return;
    }

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const notifId = String(body.notifId || "").trim();
    if (!notifId || notifId.length > 128 || notifId.includes("/")) {
      res.status(400).json({ error: "Valid notification ID is required." });
      return;
    }

    const notifRef = db.collection("notifications").doc(notifId);
    const claim = await claimNotificationForDispatch(db, notifRef);
    if (!claim.claimed) {
      if (claim.reason === "not_found") {
        res.status(404).json({ error: "Notification document not found." });
        return;
      }
      res.status(200).json({ ok: true, notifId, deduplicated: true });
      return;
    }

    const result = await dispatchNotificationData({
      notifId,
      notif: claim.data,
      db,
      messaging,
      log,
    });
    res.status(200).json({ ok: true, notifId, ...result });
  } catch (err) {
    log.error("dispatchLakemarkNotification HTTP failed", { error: err.message });
    res.status(500).json({ error: "Unable to dispatch notification right now." });
  }
}

module.exports = {
  DATABASE_ID,
  TRIGGER_REGION,
  claimNotificationForDispatch,
  dispatchNotificationData,
  handleCreateStaffAccount,
  handleDispatchNotificationHttp,
  handleResetStaffPassword,
  handleStaffSignIn,
  handleUpdateStaffAccount,
  initFirebaseAdmin,
  verifyAuthenticatedRequest,
  verifyManagerRequest,
};
