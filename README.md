# Lakemark Kitchen Management (Android + Firebase)

Internal Android hospitality and kitchen operations application for **Lakemark (Bahir Dar)**, built with Kotlin, Jetpack Compose (Material 3), and Firebase.

- **Package (`applicationId`):** `com.aistudio.applet.kphrxs` (`namespace = "com.example"`)
- **Current App Version:** `3.4.7` (`versionCode = 21`, `minSdk = 24`, `targetSdk = 36`)
- **Firebase Project:** `lakemark-app` (Project Number `1010314964245`, **Spark Free Plan**)
- **Firestore Database:** `(default)` (multi-region `nam5`)
- **Master Admin Email:** `abebaw4040@gmail.com`
- **Auth & Push Architecture (Spark Free Plan):** **Option A (Direct Admin Login + Pure Firebase Auth/Firestore)** — zero external servers (no Render/Railway). Manager signs in directly via Google Sign-In (`abebaw4040@gmail.com`) and can switch across all roles; Staff accounts are provisioned directly via Firebase Auth Email/Password (`@staff.lakemark.local`) + Firestore (`users` & `staff_credentials`).
- **Distribution Model:** Internal company APK distribution (`app/build/outputs/apk/release/lakemark-v3.4.7.apk`) with Play Store–independent in-app updates (`REQUEST_INSTALL_PACKAGES` + `ApkUpdateManager`). **Do not publish to Google Play Store.**

---

## Architecture Overview

- **Android App (`/app`):**
  - **Auth:** Direct Google Sign-In (`CredentialManager` + Firebase Auth) for the Master Admin (`abebaw4040@gmail.com`, with full role switching across Reception, Kitchen, Waiter, Cashier, Delivery, Manager, Customer); Firebase Auth Email/Password (`@staff.lakemark.local`) + Firestore for Reception, Kitchen, Waiter, Cashier, and Delivery staff provisioned by the Manager.
  - **Database & Storage:** Cloud Firestore (`(default)`) for real-time orders, menus, floor occupancy, staff/waiter coverage, translations (`EN`/`AM`), role brands, and release config (`/app_config/release`). On the Spark Free plan, signed release APKs (`app/build/outputs/apk/release/lakemark-v3.4.7.apk`) are hosted via **GitHub Releases** (direct HTTPS binary download with zero HTML redirects); Firebase Storage (`lakemark-app.firebasestorage.app`) is configured in `storage.rules` for future Blaze cutover.
  - **Notifications:** Real-time Firestore `notifications` & `orders` listeners (`LakemarkViewModel` + `AlertAudioAndNotificationManager`) plus FCM (`LakemarkFirebaseMessagingService`) with 3 `_v3` custom SOT-1 WAV sound channels (`lakemark_new_order.wav`, `lakemark_order_ready.wav`, `lakemark_ping.wav`), SOT-1 alert ring loops, foreground chimes, background system notifications, and **Update Now** action buttons for `kind = "app_update"`.
  - **Observability:** Firebase Crashlytics & Analytics (`LakemarkApplication`, `FirestoreErrorHandler`, release-only collection).
- **Cloud Functions (`/functions`, Node 20 — Optional for Future Blaze Upgrade):**
  - `functions/index.js` + `functions/handlers.js` (`firebase-admin ^13.0.0`, `firebase-functions ^6.0.0`) remain available for future Blaze-plan FCM push dispatch (`dispatchLakemarkNotification`) when upgrading from Spark.
- **Security Rules:** Zero-trust [firestore.rules](firestore.rules) and [storage.rules](storage.rules) with `abebaw4040@gmail.com` as the sole allowlisted Master Admin email.

---

## Directory Structure

- `app/` — Android Jetpack Compose app (`src/main`, `src/test`, `build/outputs/apk/release/`).
- `build/` — Root Gradle reports and configuration cache outputs.
- `functions/` — Optional Node.js 20 Cloud Functions backend (`handlers.js`, `index.js`, `pushRouting.js`, `staffAuth.js`, `index.test.js`), kept separate from Android build outputs.
- `docs/` — Project documentation (`PRD.md`, `ARCHITECTURE_AND_AUDIT_REPORT.md`, `FINAL_DELIVERABLE_REPORT.md`, `RELEASE_DELIVERY_AND_INSTALL.md`).
- `public/Lakemark_Android_Icon_Kit_LM/` — Master brand SVGs, PNGs, and Android/PWA icon assets.
- `gradle/` & `.github/workflows/` — Version catalog (`libs.versions.toml`), wrapper properties, and Firebase CI/CD workflow.
- Root Firebase configs — `firebase.json`, `.firebaserc`, `firestore.rules`, `firestore.indexes.json`, `storage.rules`, `firebase-blueprint.json`.

---

## Local Setup & Build

**Prerequisites:** Android Studio (or JDK 17+ & Android SDK 36) and Node.js 20+.

1. Open `lakemark-app` in **Android Studio** and sync Gradle.
2. Verify [app/google-services.json](app/google-services.json) targets project `lakemark-app`.
3. Configure production signing environment variables before building a release APK:
   ```powershell
   $env:KEYSTORE_PATH = "C:\path\to\my-upload-key.jks"
   $env:STORE_PASSWORD = "<store-password>"
   $env:KEY_PASSWORD = "<key-password>"
   gradle :app:assembleRelease
   ```
4. Run Cloud Functions unit tests locally:
   ```powershell
   node --test functions\index.test.js
   ```
5. See [docs/RELEASE_DELIVERY_AND_INSTALL.md](docs/RELEASE_DELIVERY_AND_INSTALL.md) for Firebase deployment, in-app update publishing, and internal APK distribution instructions.
6. See [docs/ARCHITECTURE_AND_AUDIT_REPORT.md](docs/ARCHITECTURE_AND_AUDIT_REPORT.md) for the complete architecture, reorganization summary, and verification audit.
