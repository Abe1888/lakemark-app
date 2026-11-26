# Lakemark Kitchen Management — Architecture & Evidence-Based Audit Report

**Application ID:** `com.aistudio.applet.kphrxs` (`namespace = "com.example"`)  
**App Version:** `3.4.7` (`versionCode = 21`)  
**Firebase Project:** `lakemark-app` (Project Number `1010314964245`, **Spark Free Plan**)  
**Firestore Database:** `(default)` (`nam5`)  
**Storage Bucket:** `lakemark-app.firebasestorage.app`  
**Auth & Push Architecture:** Direct Admin Login + Native Firebase Auth/Firestore (no Render/Railway)  
**Master Admin Email:** `abebaw4040@gmail.com`  
**Distribution Model:** Internal company APK distribution (Play Store–free)

---

## 1. Reorganized Project Structure

```text
lakemark-app/
├── .github/workflows/
│   └── deploy-firebase.yml          # Admin SDK CI/CD (google-github-actions/auth@v2)
├── app/
│   ├── build.gradle.kts             # Android config, Crashlytics, Analytics
│   ├── google-services.json         # lakemark-app Android & Web OAuth client config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml  # REQUEST_INSTALL_PACKAGES, FileProvider, FCM service
│       │   ├── java/com/example/
│       │   │   ├── data/            # auth/, firebase/, model/, preferences/, repository/
│       │   │   ├── fcm/             # LakemarkFirebaseMessagingService, PushModelsAndRouting, etc.
│       │   │   ├── localization/    # AppStrings, TranslationCatalog (EN/AM)
│       │   │   ├── ui/              # components/, screens/, theme/, viewmodel/
│       │   │   └── util/            # ApkUpdateManager, AlertAudioAndNotificationManager, MediaImageUploader
│       │   └── res/                 # Updated LM vector/adaptive icons, splash, fonts, strings
│       └── test/                    # Unit & Robolectric test suites
├── docs/                            # Centralized project documentation
│   ├── ARCHITECTURE_AND_AUDIT_REPORT.md
│   ├── FINAL_DELIVERABLE_REPORT.md
│   ├── PRD.md
│   └── RELEASE_DELIVERY_AND_INSTALL.md
├── functions/                       # Cloud Functions v2 (Node 20, firebase-admin ^13.0.0)
│   ├── index.js
│   ├── index.test.js
│   ├── pushRouting.js
│   └── staffAuth.js
├── gradle/                          # libs.versions.toml & wrapper properties
├── public/
│   └── Lakemark_Android_Icon_Kit_LM/# Master brand SVGs, PNGs, and Android/PWA icon assets
├── .firebaserc                      # Default project: lakemark-app
├── firebase.json                    # Firestore (default), Storage, Functions, Auth (Google Sign-In)
├── firebase-blueprint.json          # Schema specification
├── firestore.rules                  # Zero-trust Firestore security rules
├── firestore.indexes.json           # Composite indexes
├── firestore.test.js                # Firestore rules emulator test suite
├── storage.rules                    # Zero-trust Firebase Storage rules
├── my-upload-key.jks                # Production release keystore (RSA 2048, alias: upload)
├── lakemark-release-v1.apk          # Signed production APK v3.4.4 (19.8 MB)
└── README.md                        # Repository entry point linking to /docs
```

### Reorganization & Cleanup Completed (Proven Unused)
- Moved `RELEASE_DELIVERY_AND_INSTALL.md` into `docs/RELEASE_DELIVERY_AND_INSTALL.md` and updated references in `README.md` and `docs/PRD.md`.
- Deleted `lakemark-release-v18.apk` (legacy build targeting old project `gen-lang-client-0949106041`).
- Deleted `public/assets/aistudio/` (18 unreferenced AI Studio prototype PNG/HTML files — verified 0 references across the codebase).
- Removed obsolete `com.aistudio.applet.svufua` client block from `app/google-services.json`.
- Removed duplicate/obsolete `mipmap-*/ic_launcher_{background,foreground,monochrome}.png`, `drawable-*/ic_stat_lakemark.png`, and `drawable-*/splash_icon.png` after installing `Lakemark_Android_Icon_Kit_LM` vector drawables.
- Cleaned `metadata.json` to remove unused `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`.
- Kept `ARCHIVE/` and `BACKUP/` strictly untouched.

---

## 2. Automatic Android Update Delivery Architecture

Lakemark is configured for **Play Store–free internal distribution** with zero Play Core / `app-update` dependencies:

1. **Release Trigger & Version Check:** `LakemarkViewModel` observes Firestore `/app_config/release` on launch and resume (`ON_STOP` -> `ON_RESUME`) and compares `latestVersionCode > BuildConfig.VERSION_CODE`.
2. **FCM Push Notification with "Update Now":** Publishing a release in **Manage → Alerts → Release Update & Delivery** creates a `kind = "app_update"` notification targeting `["all"]`. `AlertAudioAndNotificationManager` attaches an **Update Now** (`R.string.notif_action_update`) action button with a `PushDeepLink` containing `apkUrl`.
3. **In-Place Verified Installer (`ApkUpdateManager`):** Downloads the APK over HTTPS, validates the ZIP header, optional SHA-256 checksum, package ID (`com.aistudio.applet.kphrxs`), non-downgrade `versionCode`, and **matching signing certificate SHA-256**, then launches Android's installer via `FileProvider` (`${applicationId}.fileprovider`) and `REQUEST_INSTALL_PACKAGES`.
4. **Data & Account Preservation:** Installing over the existing app with the same `applicationId` and release keystore preserves Firebase Auth sessions, DataStore (`lakemark_prefs`), and local push stores (`lakemark_push_store.xml`, `lakemark_push_dedup.xml`).

---

## 3. Verification & Test Evidence

| Check | Command / Scope | Observed Result |
|---|---|---|
| Cloud Functions Push Routing Tests | `node --test functions\index.test.js` | **PASSED** (Push payload routing & `_v3` channel validation) |
| Firebase Auth & Firestore Deploy | `npx -y firebase-tools@latest deploy --only auth,firestore --project lakemark-app` | **PASSED** (Google Sign-In enabled; `(default)` rules & indexes deployed) |
| Firebase Android SHA & OAuth Config | `firebase apps:android:sha:create` + `apps:sdkconfig ANDROID` | **PASSED** — `SHA_1` (`8a2da36631a2012e`) & `SHA_256` (`9aedeae096191ab1`) registered; `app/google-services.json` includes Android `client_type: 1` & Web `client_type: 3` OAuth clients |
| Config JSON Validation | `app/google-services.json`, `.firebaserc`, `firebase.json` | **PASSED** (Valid JSON targeting `lakemark-app`) |
| Reference & Cleanup Scan | PowerShell `Select-String` across active files | **PASSED** (0 stale project IDs, Render/Railway configs, removed emails, `FIREBASE_TOKEN`, or broken doc paths) |
| Android Gradle Build & APK Signing | `gradle :app:assembleRelease` + `apksigner verify` | **PASSED** — `lakemark-release-v4.apk` (v3.4.7, versionCode 21) signed with production keystore (`CN=Lakemark App, O=Lakemark, L=Addis Ababa, C=ET`), APK Signature Scheme v2 verified; 3 SOT-1 `.wav` sound files packaged |
| Android Unit Tests | `gradle :app:testDebugUnitTest` | **PARTIAL** — `FcmPushTests` (11/11) & `ExampleUnitTest` (1/1) **PASSED**; 3 Robolectric `@Config(sdk = [36])` tests require Java 21 (JDK 17 installed here) |
| Signing Certificate SHA-256 | `apksigner verify --print-certs` | `2b:2d:58:5f:f1:94:0c:ba:0a:f6:85:e7:f0:8d:ca:e0:d3:e6:73:8b:be:b7:35:da:f0:5c:7d:3e:1f:e3:2e:ab` |
| Signing Certificate SHA-1 | `apksigner verify --print-certs` | `c0:43:c4:03:d7:a8:f1:ba:22:6d:1b:fc:7c:9c:6a:ce:f0:48:81:3e` |
| Cloud Functions Deploy (`lakemark-app`) | `firebase deploy --only functions --project lakemark-app` | **DEFERRED (Spark Plan)** — Direct Admin Login + native Firebase Auth/Firestore used instead until Blaze upgrade |
| Storage Rules Deploy (`lakemark-app`) | `firebase deploy --only storage --project lakemark-app` | **BLOCKED** — Firebase Storage bucket must be initialized in Console (new projects may require Blaze for Storage initialization) |
| Real-Device End-to-End Test | Physical Android devices (`adb devices` = none attached) | **NOT VERIFIED** — Requires installing `lakemark-release-v4.apk` on physical Android devices |

---

## 4. Build Artifacts & Signing Details

| Property | Value |
|---|---|
| **APK File** | `lakemark-release-v4.apk` (project root; supersedes `v1`–`v3`) |
| **App Version** | `3.4.7` (versionCode `21`) |
| **Application ID** | `com.aistudio.applet.kphrxs` |
| **Auth & Notification Mode** | Option A: Direct Admin Login + Native Firebase Auth (`@staff.lakemark.local`) + Firestore Real-Time Listeners |
| **Signing Scheme** | APK Signature Scheme v2 |
| **Signer DN** | `CN=Lakemark App, OU=Engineering, O=Lakemark, L=Addis Ababa, ST=Addis Ababa, C=ET` |
| **Key Algorithm** | RSA 2048-bit |
| **Certificate Validity** | Nov 25, 2026 → Apr 12, 2054 (10,000 days) |
| **Certificate SHA-256** | `2b:2d:58:5f:f1:94:0c:ba:0a:f6:85:e7:f0:8d:ca:e0:d3:e6:73:8b:be:b7:35:da:f0:5c:7d:3e:1f:e3:2e:ab` |
| **Certificate SHA-1** | `c0:43:c4:03:d7:a8:f1:ba:22:6d:1b:fc:7c:9c:6a:ce:f0:48:81:3e` |
| **Keystore File** | `my-upload-key.jks` (alias: `upload`) |
| **Build Tool** | Gradle 9.3.1, AGP 9.1.1, Kotlin 2.4.21, JDK 17.0.21.1 |

---

## 5. Required Next Steps (Cannot Be Automated Here)

1. **Enable Email/Password Sign-In in Firebase Console (Free on Spark):** Open **Firebase Console → Authentication → Sign-in method**, enable **Email/Password**, and save.
2. **Install `lakemark-release-v4.apk` on Real Android Devices:** Sign in as Manager (`abebaw4040@gmail.com`) to auto-seed/sync starter staff accounts, view usernames & passwords in **Manage → Team**, test Staff Sign-In (`@reception1`, `@kitchen1`, etc.), Firestore operations, and custom SOT-1 notification sounds.
3. **Firebase Storage (Profile/Food Photos & APK Hosting):** Initialize **Firebase Storage** (`https://console.firebase.google.com/project/lakemark-app/storage`) and run `npx -y firebase-tools@latest deploy --only storage --project lakemark-app` (Note: if Google Console requires Blaze to provision the default Storage bucket on `lakemark-app`, image uploads and Firebase Storage APK hosting remain deferred until Blaze upgrade; APKs can be hosted on any HTTPS host such as GitHub Releases during Spark testing).
4. **Final Production Cutover (When Ready for Blaze):** Upgrade `lakemark-app` to **Blaze**, and run `npx -y firebase-tools@latest deploy --only functions,storage --project lakemark-app`.
