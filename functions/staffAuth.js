"use strict";

const crypto = require("node:crypto");

const ADMIN_EMAILS = new Set([
  "abebaw4040@gmail.com",
]);

const ALLOWED_STAFF_ROLES = new Set([
  "reception",
  "kitchen",
  "waiter",
  "cashier",
  "delivery",
]);

function normalizeUsername(raw) {
  return String(raw || "")
    .trim()
    .toLowerCase()
    .replace(/^@+/, "");
}

function isValidUsername(username) {
  return /^[a-z0-9._-]{3,32}$/.test(String(username || ""));
}

function isValidPassword(password) {
  return typeof password === "string" && password.length >= 6 && password.length <= 128;
}

function isAllowedStaffRole(role) {
  return ALLOWED_STAFF_ROLES.has(String(role || "").trim().toLowerCase());
}

function hashPassword(password, saltHex = crypto.randomBytes(16).toString("hex")) {
  const hashHex = crypto.scryptSync(String(password), saltHex, 64).toString("hex");
  return { saltHex, hashHex, hashAlgo: "scrypt" };
}

function hashPasswordPbkdf2(password, saltHex = crypto.randomBytes(16).toString("hex")) {
  const hashHex = crypto.pbkdf2Sync(String(password), saltHex, 100000, 64, "sha256").toString("hex");
  return { saltHex, hashHex, hashAlgo: "pbkdf2_sha256" };
}

function verifyPassword(password, saltHex, expectedHashHex, hashAlgo = "scrypt") {
  if (
    typeof password !== "string" ||
    typeof saltHex !== "string" ||
    typeof expectedHashHex !== "string" ||
    !saltHex ||
    !expectedHashHex
  ) {
    return false;
  }
  const algo = String(hashAlgo || "scrypt").trim().toLowerCase();
  const actualBuf = algo === "pbkdf2_sha256"
    ? crypto.pbkdf2Sync(password, saltHex, 100000, 64, "sha256")
    : crypto.scryptSync(password, saltHex, 64);
  const expectedBuf = Buffer.from(expectedHashHex, "hex");
  if (actualBuf.length !== expectedBuf.length) {
    return false;
  }
  return crypto.timingSafeEqual(actualBuf, expectedBuf);
}

function isCallerManager({ decodedToken, callerUserData }) {
  if (!decodedToken || typeof decodedToken !== "object") {
    return false;
  }
  const email = String(decodedToken.email || "").trim().toLowerCase();
  if (decodedToken.email_verified === true && ADMIN_EMAILS.has(email)) {
    return true;
  }
  return Boolean(
    callerUserData &&
      callerUserData.active === true &&
      String(callerUserData.role || "").toLowerCase() === "manager"
  );
}

function extractBearerToken(authHeader) {
  const raw = String(authHeader || "").trim();
  if (!raw.toLowerCase().startsWith("bearer ")) {
    return "";
  }
  return raw.slice(7).trim();
}

function resolveUpdatedStaffFields({ existingUser, body }) {
  const existingRole = String((existingUser && existingUser.role) || "").trim().toLowerCase();
  const isManagerTarget = existingRole === "manager";
  const rawName = body && body.name !== undefined ? String(body.name || "").trim().slice(0, 60) : String((existingUser && existingUser.name) || "").trim().slice(0, 60);
  if (!rawName) {
    return { ok: false, status: 400, error: "Enter the staff member's name." };
  }

  const rawUsername = body && body.username !== undefined
    ? normalizeUsername(body.username)
    : normalizeUsername(existingUser && existingUser.username);
  const newPassword = body && typeof body.newPassword === "string" ? body.newPassword : "";

  if (!isManagerTarget && newPassword && !isValidPassword(newPassword)) {
    return { ok: false, status: 400, error: "New password must be at least 6 characters." };
  }
  if (!isManagerTarget && rawUsername && !isValidUsername(rawUsername)) {
    return { ok: false, status: 400, error: "Username must be 3-32 characters (letters, numbers, ., _, -)." };
  }
  if (!isManagerTarget && newPassword && !rawUsername) {
    return { ok: false, status: 400, error: "Enter a username before setting a staff password." };
  }

  let nextRole = existingRole;
  if (!isManagerTarget && body && body.role !== undefined) {
    const requestedRole = String(body.role || "").trim().toLowerCase();
    if (!isAllowedStaffRole(requestedRole)) {
      return { ok: false, status: 400, error: "Choose a valid staff role." };
    }
    nextRole = requestedRole;
  }

  const nextActive = isManagerTarget
    ? true
    : (body && typeof body.active === "boolean" ? body.active : Boolean(existingUser && existingUser.active));

  return {
    ok: true,
    isManagerTarget,
    name: rawName,
    username: isManagerTarget ? normalizeUsername(existingUser && existingUser.username) : rawUsername,
    role: nextRole,
    active: nextActive,
    newPassword: isManagerTarget ? "" : newPassword,
  };
}

module.exports = {
  ADMIN_EMAILS,
  ALLOWED_STAFF_ROLES,
  normalizeUsername,
  isValidUsername,
  isValidPassword,
  isAllowedStaffRole,
  hashPassword,
  hashPasswordPbkdf2,
  verifyPassword,
  isCallerManager,
  extractBearerToken,
  resolveUpdatedStaffFields,
};
