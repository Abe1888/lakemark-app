"use strict";

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const {
  DATABASE_ID,
  TRIGGER_REGION,
  dispatchNotificationData,
  handleCreateStaffAccount,
  handleResetStaffPassword,
  handleStaffSignIn,
  handleUpdateStaffAccount,
  initFirebaseAdmin,
} = require("./handlers");

initFirebaseAdmin();

function handleCorsAndMethod(req, res) {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return false;
  }
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed." });
    return false;
  }
  return true;
}

exports.dispatchLakemarkNotification = onDocumentCreated(
  {
    document: "notifications/{notifId}",
    database: DATABASE_ID,
    region: TRIGGER_REGION,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    await dispatchNotificationData({
      notifId: event.params.notifId,
      notif: snap.data() || {},
      log: logger,
    });
  }
);

exports.staffSignIn = onRequest(
  { region: TRIGGER_REGION, invoker: "public" },
  async (req, res) => {
    if (!handleCorsAndMethod(req, res)) return;
    await handleStaffSignIn(req, res, { log: logger });
  }
);

exports.createStaffAccount = onRequest(
  { region: TRIGGER_REGION, invoker: "public" },
  async (req, res) => {
    if (!handleCorsAndMethod(req, res)) return;
    await handleCreateStaffAccount(req, res, { log: logger });
  }
);

exports.resetStaffPassword = onRequest(
  { region: TRIGGER_REGION, invoker: "public" },
  async (req, res) => {
    if (!handleCorsAndMethod(req, res)) return;
    await handleResetStaffPassword(req, res, { log: logger });
  }
);

exports.updateStaffAccount = onRequest(
  { region: TRIGGER_REGION, invoker: "public" },
  async (req, res) => {
    if (!handleCorsAndMethod(req, res)) return;
    await handleUpdateStaffAccount(req, res, { log: logger });
  }
);
