# Lakemark Kitchen Management — Release Delivery & Installation Guide

**Application ID:** `com.aistudio.applet.kphrxs`  
**Current App Version:** `3.4.7` (`versionCode = 21`)  
**Firebase Project:** `lakemark-app` (`1010314964245`, **Spark Free Plan**)  
**Firestore Database:** `(default)`  
**Auth & Push Architecture:** Direct Admin Login + Native Firebase Auth/Firestore (no Render/Railway)  
**Build Type:** Release (`assembleRelease`, `minSdk = 24`, `targetSdk = 36`, APK Signature Scheme v2)  
**Distribution Model:** Internal company APK distribution (`app/build/outputs/apk/release/lakemark-release-v4.apk`, Play Store–free). **Do not upload to Google Play Store.**

---

## 1. Internal Release Delivery & Automatic In-App Updates

Lakemark distributes full, identically signed APKs for both fresh installations and seamless in-place upgrades (preserving all user accounts, Auth sessions, DataStore preferences, and local state):

1. **In-App Release Check & Reopen Detection (`/app_config/release`):** `LakemarkViewModel` monitors Firestore `/app_config/release` and checks automatically on startup and whenever the app is reopened from background (`ON_STOP` -> `ON_RESUME`) when `latestVersionCode > BuildConfig.VERSION_CODE`.
2. **Automatic Update Push Notification (`kind = "app_update"`):** When a Manager publishes a new release, `dispatchLakemarkNotification` sends high-priority FCM pushes to all active devices. `AlertAudioAndNotificationManager` attaches an **Update Now** notification action button, and tapping either the notification or **Update Now** opens `AppUpdateAvailableDialog`.
3. **Verified In-App APK Downloader & Installer (`ApkUpdateManager`):** Tapping **Update Now** downloads the APK over HTTPS (`apkDownloadUrl`), verifies the ZIP header, optional SHA-256 checksum (`apkSha256`), package ID (`com.aistudio.applet.kphrxs`), non-downgrade `versionCode`, and **matching signing certificate SHA-256**, checks `canRequestPackageInstalls()` (`REQUEST_INSTALL_PACKAGES`), and launches Android's package installer via `FileProvider` (`${applicationId}.fileprovider`).
4. **Manager Staff Seeding & Direct Staff Management:** When an active Manager signs in and no non-manager staff accounts exist in Firestore `/users`, the app automatically seeds 5 starter staff accounts (`@reception1`, `@kitchen1`, `@waiter1`, `@cashier1`, `@delivery1`) and supports creating, editing, and resetting staff passwords.

---

## 2. Signing Certificate Consistency

Every production APK **must** be signed with the same production release keystore (`KEYSTORE_PATH` / `my-upload-key.jks`, alias `upload`) so devices upgrade in place without uninstalling:

1. Extract the release keystore's **SHA-1** and **SHA-256** fingerprints and register them in **Firebase Console → Project Settings → Your apps (`com.aistudio.applet.kphrxs`)**.
2. Verify the built APK (`app/build/outputs/apk/release/lakemark-release-v4.apk`) with `apksigner`:
   ```powershell
   apksigner verify --verbose --print-certs app/build/outputs/apk/release/lakemark-release-v4.apk
   ```

---

## 3. Firebase Spark Setup (Direct Admin Login & Native Staff Auth)

**Already Deployed / Configured on `lakemark-app`:**
- **Firebase Auth:** Google Sign-In enabled (`supportEmail: abebaw4040@gmail.com`) and production keystore **SHA-1** (`c0:43:c4:03:d7:a8:f1:ba:22:6d:1b:fc:7c:9c:6a:ce:f0:48:81:3e`) & **SHA-256** (`2b:2d:58:5f:f1:94:0c:ba:0a:f6:85:e7:f0:8d:ca:e0:d3:e6:73:8b:be:b7:35:da:f0:5c:7d:3e:1f:e3:2e:ab`) registered.
- **Cloud Firestore (`(default)`):** Created in `nam5`; `firestore.rules` and `firestore.indexes.json` deployed (`firebase deploy --only firestore`).

**Enable Email/Password Sign-In for Staff Accounts (Free on Spark):**
1. Open [Firebase Console → Authentication → Sign-in method](https://console.firebase.google.com/project/lakemark-app/authentication/providers).
2. Enable **Email/Password** (leave Email link disabled) and click **Save**.
3. Sign in once as the Manager (`abebaw4040@gmail.com`) in the Android app so `seedDefaultStaffIfEmpty()` and `syncExistingStaffAuthAccounts()` provision all starter staff accounts (`@reception1`, `@kitchen1`, `@waiter1`, `@cashier1`, `@delivery1`) in Firebase Auth and Firestore.

**Remaining Steps When Ready for Blaze Cutover:**
1. Upgrade `lakemark-app` to **Blaze** (`https://console.firebase.google.com/project/lakemark-app/usage/details`) and deploy Cloud Functions (`npx -y firebase-tools@latest deploy --only functions --project lakemark-app`).
2. Initialize **Firebase Storage** (`https://console.firebase.google.com/project/lakemark-app/storage`) and deploy Storage rules (`npx -y firebase-tools@latest deploy --only storage --project lakemark-app`).
3. **GitHub Actions CI/CD (`.github/workflows/deploy-firebase.yml`):** Add repository secret `FIREBASE_SERVICE_ACCOUNT_LAKEMARK_APP`.

---

## 4. Publishing an In-App Update (Free on Spark via GitHub Releases)

While on the Firebase **Spark (Free)** plan (before Firebase Storage is initialized), use **GitHub Releases** as the zero-HTML-redirect HTTPS APK host:

1. Build and sign the new APK (`versionCode` incremented, signed with the **same** release keystore) → output at `app/build/outputs/apk/release/lakemark-release-v4.apk`.
2. In your GitHub repository, go to **Releases → Draft a new release**, create a tag (e.g. `v3.4.7`), attach `lakemark-release-v4.apk`, and click **Publish release**.
3. Right-click the attached `.apk` asset under **Assets** and copy its direct HTTPS URL:
   `https://github.com/<username>/lakemark-app/releases/download/v3.4.7/lakemark-release-v4.apk`
4. Open Lakemark as the authorized Manager (`abebaw4040@gmail.com`) → **Manage → Alerts → Release Update & Delivery** and enter:
   - **Version name** and **Version code**
   - **APK download URL** (`https://github.com/.../releases/download/.../lakemark-release-v4.apk`)
   - **APK SHA-256 (optional)**
   - **Release notes**
5. Tap **Publish & notify users** to update `/app_config/release` and broadcast the `app_update` notification (with **Update Now**) to active devices.

*(Once upgraded to Blaze with Firebase Storage initialized, you can alternatively upload to `lakemark-app.firebasestorage.app` under `releases/<apk-filename>.apk`.)*

---

## 5. User Data Preservation During Upgrades

Upgrading in place (via **Update Now** or `adb install -r`) preserves all user and operational state:

1. **Firebase Auth Session:** Persisted auth tokens remain intact; signed-in Managers, Staff, and permanently activated Customers stay logged in.
2. **User Preferences (`lakemark_prefs` DataStore):** Selected language (`EN`/`AM`), cached `/translations` JSON overrides, sound/vibration toggles, alert volume, Manager `preferredRoleKey`, and `lastNotifReadAtMs` remain intact.
3. **Push & Deduplication Stores (`lakemark_push_store.xml`, `lakemark_push_dedup.xml`):** Retained locally during in-place upgrades while excluded from cross-device cloud backups.
4. **Cloud Firestore & Storage Data:** Users, profile photos (`users/{uid}/profile/...`), saved items (`/users/{uid}/saved_items`), `/translations/{key}`, menus (`/menu_items/{itemId}`), orders, brands, and release metadata reside in Firebase.

---

## 6. Installation & Verification Procedures

### A. In-Place Upgrade via ADB
```powershell
adb devices
adb shell dumpsys package com.aistudio.applet.kphrxs | Select-String "versionCode|versionName"
adb install -r app/build/outputs/apk/release/lakemark-release-v4.apk
adb shell dumpsys package com.aistudio.applet.kphrxs | Select-String "versionCode|versionName"
```
> **Do NOT run `adb uninstall com.aistudio.applet.kphrxs`** before upgrading, as uninstalling deletes local auth sessions, DataStore preferences, and `deviceId`.

### B. In-Place Upgrade Directly on Device
1. Tap **Update Now** on the push notification or inside **Settings → Check for Updates** (or open the downloaded APK in **Files**).
2. Allow **Install unknown apps** for Lakemark if prompted on Android 8+.
3. Confirm **Update** (or **Install** on clean devices).
4. Launch **Lakemark** and verify the version in **Settings** (`top_settings_button` → footer).

### C. Reproducing a Production Release Build
```powershell
$env:KEYSTORE_PATH = "C:\path\to\my-upload-key.jks"
$env:STORE_PASSWORD = "<your-store-password>"
$env:KEY_PASSWORD = "<your-key-password>"
gradle :app:assembleRelease
```
