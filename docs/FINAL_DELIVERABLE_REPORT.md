# Lakemark Kitchen Management — Final Deliverable Report

**Application ID:** `com.aistudio.applet.kphrxs`  
**App Version:** `3.4.7` (`versionCode = 21`)  
**Firebase Project:** `lakemark-app` (Project Number `1010314964245`, **Spark Free Plan**)  
**Firestore Database:** `(default)` (`nam5`)  
**Master Admin Email:** `abebaw4040@gmail.com` (sole allowlisted admin)  
**Auth & Push Architecture:** Direct Admin Login + Native Firebase Auth/Firestore (no Render/Railway)  
**Distribution Model:** Internal company APK distribution (`app/build/outputs/apk/release/lakemark-v3.4.7.apk`, Play Store–free)

---

## 1. Summary of Implemented & Migrated Requirements

### A. Firebase Project Migration & Admin Security Cleanup
- **Single Source of Truth (`lakemark-app`):** Migrated the Android app, Cloud Functions, security rules, and CI/CD workflow from the legacy AI Studio project (`gen-lang-client-0949106041`) and named database (`ai-studio-android-lakemark-8b626ca5-cd53-40bb-a0e9-c6e8894c407f`) to `lakemark-app` and `(default)` Firestore.
- **Registered Android App, SHA Fingerprints & Google Sign-In:** Registered `com.aistudio.applet.kphrxs` (`1:1010314964245:android:3ce0bd71b81cbf9a630d1c`) on `lakemark-app`, registered the production keystore **SHA-1** and **SHA-256** fingerprints (`8a2da36631a2012e`, `9aedeae096191ab1`), enabled Google Sign-In (`abebaw4040@gmail.com`), and refreshed `app/google-services.json` with both the Android (`client_type: 1`) and Web (`client_type: 3`) OAuth clients.
- **Sole Master Admin Allowlist:** Removed `bettyabe21@gmail.com` and `belewabebe157@gmail.com` end-to-end across `firestore.rules`, `storage.rules`, `functions/staffAuth.js`, `LakemarkRepository.kt`, `LakemarkViewModel.kt`, tests, and docs. Only `abebaw4040@gmail.com` (plus `/admins/{uid}`) is authorized as Master Admin.
- **Admin SDK & CI/CD Security Migration:** Backend uses `firebase-admin` (`^13.0.0`) and `firebase-functions` (`^6.0.0`). Removed deprecated `FIREBASE_TOKEN` usage from `.github/workflows/deploy-firebase.yml`, migrating exclusively to `google-github-actions/auth@v2` (`FIREBASE_SERVICE_ACCOUNT_LAKEMARK_APP`).
- **Direct Admin Login & Native Firebase Staff Auth (Spark Free Plan):** Removed the Render/Railway server setup. Manager (`abebaw4040@gmail.com`) signs in directly via Google Sign-In with full role switching; staff accounts are provisioned directly via Firebase Auth Email/Password (`@staff.lakemark.local` via secondary `FirebaseApp`) and Firestore (`users` + `staff_credentials`), and notifications are routed in real time via Firestore listeners + custom SOT-1 WAV sounds.

### B. Authentication, Localization, Profiles, Saved Dishes & Food Photos
- **Manager Google Setup & Staff Auth:** `AuthScreen` separates **Staff Sign In** (Firebase Auth Email/Password with `@staff.lakemark.local` + Firestore `/users/{uid}`) and **Manager Login** (Google Sign-In via `CredentialManager`). Unauthorized non-admin Google sign-ins during setup are cleanly rejected.
- **Complete EN & AM Localization:** Full English and Amharic translations via `AppStrings` / `TranslationCatalog`, `noto_sans_ethiopic.ttf` typography, DataStore persistence, and real-time Manager editing in `Manage → Language` (`/translations/{key}`).
- **User Profiles & Food Photos:** Camera/gallery upload via `MediaImageUploader` (MIME `image/jpeg`, `image/png`, `image/webp`, 5 MB cap, EXIF rotation) to Firebase Storage (`users/{uid}/profile/...` and `menu_items/{itemId}/...`).
- **Saved Dishes & Newly Registered Queue:** Per-user bookmarks in `/users/{uid}/saved_items/{menuItemId}` (`SavedScreen`) and newest-first registration queue in `Manage → Team → Newly Registered`.

### C. Play Store–Free Automatic In-App Update Delivery
- **Push + Deep-Link Routing:** `kind = "app_update"` notifications carry `apkUrl` and attach an **Update Now** action button in `AlertAudioAndNotificationManager`, opening `AppUpdateAvailableDialog` via `MainActivity` + `LakemarkViewModel` (reconciled safely via reactive `combine()` without cold-start timeout drops).
- **Verified In-Place Installer (`ApkUpdateManager`):** Downloads over HTTPS, validates ZIP header, optional SHA-256 checksum, package ID, non-downgrade `versionCode`, and matching signing certificate, then installs in place via `FileProvider` + `REQUEST_INSTALL_PACKAGES` without uninstalling or losing data.

### D. Crashlytics & Analytics Integration
- Added `firebase-crashlytics` (`3.0.8` Gradle plugin) and `firebase-analytics` via Firebase BOM.
- Configured release-only collection in `LakemarkApplication.onCreate()` and non-fatal Firestore error reporting with custom keys in `FirestoreErrorHandler.kt`.

---

## 2. Verification & Test Evidence

| Verification Check | Command / Tool | Result |
|---|---|---|
| Firebase Auth & Firestore Deploy | `npx -y firebase-tools@latest deploy --only auth,firestore --project lakemark-app` | **PASSED** (Google Sign-In enabled; `(default)` rules & indexes deployed) |
| Android SHA-1 & SHA-256 Registration | `firebase apps:android:sha:create` + `apps:sdkconfig ANDROID` | **PASSED** (`SHA_1` + `SHA_256` registered; `google-services.json` includes `client_type: 1` & `3`) |
| Cloud Functions Test Suite | `node --test functions\index.test.js` | **PASSED** (Push payload routing & channel validation) |
| Signed Production APK (`v4`) | `gradle :app:assembleRelease` + `apksigner verify` | **PASSED** (`lakemark-v3.4.7.apk`, v3.4.7, versionCode 21, APK Signature Scheme v2 verified) |
| Codebase Cleanup Scan | PowerShell `Select-String` across active files | **PASSED** (0 references to Render/Railway, removed emails, `svufua`, `FIREBASE_TOKEN`, or old project ID) |
| Cloud Functions Deploy (`lakemark-app`) | `firebase deploy --only functions --project lakemark-app` | **DEFERRED** (Spark Plan — Direct Admin Login & native Firebase Auth/Firestore used instead) |
| Storage Rules Deploy (`lakemark-app`) | `firebase deploy --only storage --project lakemark-app` | **BLOCKED** (Requires Storage bucket initialization in Console; may require Blaze on new projects) |
| Real-Device End-to-End Test | Physical Android devices | **NOT VERIFIED** (No ADB device connected here; requires testing `lakemark-v3.4.7.apk` on phone) |

---

## 3. Remaining Manual / Console Steps

1. **Enable Email/Password Sign-In in Firebase Console (Free on Spark):** Open **Firebase Console → Authentication → Sign-in method**, enable **Email/Password**, and save.
2. **Test `lakemark-v3.4.7.apk` on Real Devices:** Install `lakemark-v3.4.7.apk`, sign in as Manager (`abebaw4040@gmail.com`) to auto-seed/sync staff accounts, verify usernames & passwords in **Manage → Team**, test Staff Sign-In (`@reception1`, `@kitchen1`, etc.), Firestore operations, and custom SOT-1 notification sounds.
3. **Initialize Firebase Storage (or Upgrade to Blaze When Ready):** Visit `https://console.firebase.google.com/project/lakemark-app/storage` (note: new projects may require Blaze for Storage bucket creation) and deploy `storage.rules` + `functions` when ready for production Blaze cutover.
4. **Configure GitHub Actions Secret:** Add `FIREBASE_SERVICE_ACCOUNT_LAKEMARK_APP` in GitHub repository secrets for automated CI deployments.
