# Lakemark Kitchen Management — Product Requirements Document (PRD)

**Document Version:** 2.1 (Spark Free + Render Backend & `v3.4.5` Baseline)  
**Application Name:** Lakemark (Branch Context: Bahir Dar)  
**Android Package (`applicationId`):** `com.aistudio.applet.kphrxs` (`namespace = "com.example"`)  
**App Version:** `3.4.5` (`versionCode = 19`)  
**Platform & Stack:** Android (Kotlin, Jetpack Compose Material 3, Firebase Auth, Cloud Firestore, Firebase Storage, Firebase Crashlytics & Analytics, Cloud Functions v2 Node 20, FCM HTTP v1, Jetpack Credential Manager, DataStore Preferences)  
**Audit Basis:** Direct static audit of all source files, security rules, Cloud Functions, Gradle manifests, resources, and test suites in the repository.

---

## Implementation Status Legend

Throughout this document, every feature and requirement is explicitly tagged with its verified status in the current codebase:

- **[Implemented]**: Complete client and/or backend logic exists in source code.
- **[Implemented — Not Confirmed Deployed]**: Implemented in `/functions` and `storage.rules` locally, but requires Blaze plan / Storage initialization and deployment against the live Firebase project (`lakemark-app`) and physical devices.
- **[Partially Implemented]**: Core flow or backend support exists, but UI controls, edge-case validation, or downstream workflows are incomplete.
- **[Not Implemented]**: Absent from the current codebase (identified only in scope boundaries or Section 9).

---

## 1. Product Overview, Objectives & Scope

### 1.1 Product Vision & Purpose
**Lakemark Kitchen Management** is a multi-role Android hospitality and kitchen operations application tailored for **Lakemark (Bahir Dar)**—unifying hotel room service, dine-in restaurant service, reception/lobby lounge service, and takeaway/delivery order fulfillment into a single real-time operational hub.

It connects seven operational personas (**Manager**, **Reception**, **Kitchen**, **Waiter**, **Cashier**, **Delivery**, and **Customer**) over a shared Cloud Firestore database and Firebase Cloud Messaging (FCM) push infrastructure, allowing guests and floor staff to place orders, reception to dispatch tickets, kitchen staff to manage preparation stages, waiters/delivery riders to fulfill ready orders, and managers to oversee revenue, staff accounts, floor coverage, and role branding.

### 1.2 Core Business Objectives
1. **End-to-End Real-Time Order Orchestration:** Eliminate paper tickets and manual handoffs between rooms/tables, reception, kitchen, and service staff via live Firestore snapshot listeners and high-priority FCM push notifications.
2. **Strict Role-Based Access & Operational Focus:** Tailor navigation tabs, order visibility, action buttons, and modal alerts to each station's exact responsibility while isolating customers to their own orders.
3. **Mandatory Customer Access Governance:** Enforce that self-registered customer Google accounts remain **disabled by default** until a Manager explicitly and permanently activates them.
4. **Google-Free Staff Operations:** Enable Reception, Kitchen, Waiter, Cashier, and Delivery staff to authenticate with Manager-provisioned usernames and passwords (`staffSignIn`) without requiring individual Google accounts or Firebase Console access.
5. **Station-Specific Brand Identity:** Support customizable `60% / 30% / 10%` color palettes and sender marks per role so alerts, toasts, and UI surfaces immediately convey which station originated an action.

### 1.3 Product Scope

#### In Scope (Current Codebase)
- **Authentication & Account Governance [Implemented + Implemented — Not Confirmed Deployed]:**
  - Google Sign-In via Jetpack `CredentialManager` for Customers and pre-authorized Managers.
  - App-managed username/password sign-in, staff creation, and password reset via Cloud Functions (`staffSignIn`, `createStaffAccount`, `resetStaffPassword`) [requires live Cloud Functions deployment].
  - Default-disabled customer gating (`Approval Pending`) and irreversible permanent customer activation by Managers.
- **Floor & Zone Ordering [Implemented]:**
  - Visual floor map across 3 on-premise zones (**Rooms**: `101–116` & `201–216`; **Restaurant**: `Table 1–12`; **Reception**: `Reception T1–T20`) plus **Takeaway / delivery** (`OUT`).
  - Interactive menu browsing, search, category filtering, cart management, and checkout with automatic **15% VAT** calculation.
- **Kitchen Display & Order Pipeline [Implemented]:**
  - Sequential order numbering (`counters/orders`, starting `1007` when seeded by client or `>= 1001` in rules).
  - Status workflow: `new` → `confirmed` → `preparing` → `ready` → `done` (or `cancelled` from `new`/`confirmed` with required reason).
  - Blocking modal alerts (`KitchenOrderAlertDialog` and `PickupReadyAlertDialog`) for time-sensitive stations.
- **Waiter Duty & Smart Dispatch [Implemented]:**
  - 4 coverage areas (`din`, `lob`, `f1`, `f2`), self/manager duty toggles, and workload-balanced auto-assignment or manual reassignment.
- **Manager Console & Analytics [Implemented / Partially Implemented]:**
  - Today vs. 7-day revenue KPIs, daily breakdown chart, top 5 dishes, menu price & availability toggles, account/role/waiter management, role brand editor, and staff broadcast composer.
- **Real-Time Notifications & Audio/Vibration Alerts [Implemented — Not Confirmed Deployed]:**
  - Hybrid FCM HTTP v1 Cloud Function fan-out (`dispatchLakemarkNotification`), Firestore live alert observers, cross-path deduplication (`NotificationDeduplicator`), 3 Android v2 notification channels, synthesized `AudioTrack` chimes, and deep-link navigation.

#### Out of Scope / Not Implemented
- **Payment Processing, POS Billing & Receipts [Not Implemented]:** No payment methods, cash/card settlement, split bills, or thermal/PDF receipt printing.
- **Inventory / Recipe Stock Tracking [Not Implemented]:** Only a binary `available` (`On sale` / `Sold out`) toggle exists.
- **AI / Gemini / CameraX / Location Features [Not Implemented]:** `firebase-ai`, `camera-*`, `play-services-location`, `room`, and `retrofit` dependencies are commented out in `app/build.gradle.kts` (profile/menu photo capture uses `ActivityResultContracts.TakePicture` + `PickVisualMedia`).
- **Google Play Store Distribution [Out of Scope]:** Lakemark is an internal company app distributed via direct signed APKs and Play Store–free in-app updates (`ApkUpdateManager`).

### 1.4 Current Build & Deployment Snapshot
| Dimension | Audited State |
|---|---|
| **Android App Version** | `3.4.7` (`versionCode = 21`, `applicationId = "com.aistudio.applet.kphrxs"`, `minSdk = 24`, `targetSdk = 36`). Built and signed against `lakemark-app` (`app/build/outputs/apk/release/lakemark-v3.4.7.apk`). |
| **Distribution & Signing** | Internal Play Store–free APK replacement model using `ApkUpdateManager` (`REQUEST_INSTALL_PACKAGES` + `FileProvider` + SHA-256 & signing certificate verification). Production builds require `my-upload-key.jks` (`KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD`). |
| **Firebase Project & DB** | Project `lakemark-app` (`1010314964245`), `(default)` Firestore database (`nam5`), Auth Google Sign-In enabled (`abebaw4040@gmail.com`). |
| **Cloud Functions (`functions/index.js`)** | 5 Cloud Functions v2 implemented (`dispatchLakemarkNotification`, `staffSignIn`, `createStaffAccount`, `updateStaffAccount`, `resetStaffPassword`); **pending Blaze plan upgrade & live deployment to `lakemark-app`**. |
| **Firestore & Storage Rules** | Zero-trust `firestore.rules` and `firestore.indexes.json` **deployed to `lakemark-app`**; `storage.rules` authored and **pending Storage Console initialization**. |
| **Observability** | Firebase Crashlytics (`3.0.8`) and Analytics integrated (release-only collection in `LakemarkApplication`, non-fatal error reporting in `FirestoreErrorHandler`). |
| **Physical Device End-to-End Verification** | Requires external Android device testing across foreground, background, killed state, sign-out token pruning, in-place **Update Now** rollout, and live Cloud Functions logs. |

---

## 2. All Existing Features & Functionalities

| Domain | Feature | Status | Code Reference |
|---|---|---|---|
| **Onboarding & Auth** | Branded Splash screen with flame emblem and `"Get Started"` CTA | **[Implemented]** | `SplashScreen.kt` |
| **Onboarding & Auth** | Dual-mode Auth screen (`Staff Sign In` default tab; `Manager Login` Google tab) | **[Implemented]** | `AuthScreen.kt`, `MainActivity.kt` |
| **Onboarding & Auth** | Silent Google auto-sign-in attempt on app launch (`CredentialManager`) | **[Implemented]** | `GoogleAuthManager.kt`, `MainActivity.kt` |
| **Onboarding & Auth** | App-managed Staff Sign-In via Firebase Auth Email/Password (`@staff.lakemark.local`) + Firestore `/users/{uid}` | **[Implemented]** | `StaffAuthClient.kt`, `LakemarkRepository.kt` |
| **Onboarding & Auth** | Pre-authorized Admin email bootstrap (`abebaw4040@gmail.com`) | **[Implemented]** | `LakemarkRepository.kt`, `firestore.rules`, `functions/staffAuth.js` |
| **Onboarding & Auth** | Manager-only Google setup gate & Staff disabled gate (`Account Disabled`) | **[Implemented]** | `MainActivity.kt`, `LakemarkRepository.kt`, `firestore.rules` |
| **Home Dashboard** | Role-specific Hero KPI card & 3-row status stack | **[Implemented]** | `HomeScreen.kt` |
| **Home Dashboard** | Dark plum analytics card (7-day served % circular ring + 7-day order count bars) | **[Implemented]** | `HomeScreen.kt`, `LakemarkCommonUi.kt` |
| **Home Dashboard** | 3 Floor occupancy summary cards (`Rooms` /32, `Restaurant` /12, `Reception` /20) | **[Implemented]** | `HomeScreen.kt`, `LakemarkCommonUi.kt` |
| **Home Dashboard** | Latest 3 orders feed with `"See all"` shortcut | **[Implemented]** | `HomeScreen.kt` |
| **Floor Map** | Zone switcher (`Rooms`, `Restaurant`, `Reception`) & status color legend (`Free`, `New`, `Cooking`, `Ready`) | **[Implemented]** | `FloorScreen.kt` |
| **Floor Map** | Visual hotel map (`101–116` Floor 1 + Corridor + `201–216` Floor 2 + Lift/Stairs), Restaurant tables with chairs (`1–12`), Reception lounge tables (`T1–T20`) | **[Implemented]** | `FloorScreen.kt` |
| **Menu & Ordering** | Automatic default 12-dish catalog seeding when `menu_items` is empty (Manager session) | **[Implemented]** | `LakemarkRepository.kt`, `DefaultMenuCatalog` |
| **Menu & Ordering** | Free-space launcher (zone cards + `"Takeaway / delivery"` button) when no location is selected | **[Implemented]** | `MenuScreen.kt` |
| **Menu & Ordering** | Dish search (name + description), horizontal category filter (`All`, `Classics`, `Lake Tana`, `Grill`, `Breakfast`, `Drinks`), responsive 2/3-col grid | **[Implemented]** | `MenuScreen.kt` |
| **Menu & Ordering** | Inline cart stepper (`+` / `−`), `"Sold out"` badge & block, floating `"View order"` checkout FAB | **[Implemented]** | `MenuScreen.kt`, `MainActivity.kt` |
| **Menu & Ordering** | `CartCheckoutSheet` with guest name, phone validation for `OUT` orders, special requests note, 15% VAT calculation, atomic order counter & submission | **[Implemented]** | `Sheets.kt`, `LakemarkRepository.kt` |
| **Orders Management** | `Active` / `Finished` toggle, Zone filter pills (`All`, `Rooms`, `Restaurant`, `Reception`, `Takeaway`), day-grouped (`Today`, `Yesterday`, `EEE, d MMM`) list | **[Implemented]** | `OrdersScreen.kt` |
| **Orders Management** | Role-scoped order streams (`Customer` own; `Delivery` `out` only; `Waiter` non-`out` unassigned/assigned; all others full view) | **[Implemented]** | `LakemarkViewModel.kt` |
| **Orders Management** | Role-gated status progression buttons (`Send to kitchen`, `Start preparing`, `Mark ready`, `Confirm pickup`, `Cancel` with reason) | **[Implemented]** | `OrdersScreen.kt`, `LakemarkModels.kt`, `LakemarkRepository.kt` |
| **Orders Management** | Waiter auto-assignment on order creation & manual/auto reassignment modal (`WaiterAssignDialog`) | **[Implemented]** | `LakemarkViewModel.kt`, `LakemarkCommonUi.kt` |
| **Kitchen Display (KDS)** | 3 clickable pipeline stage counters (`Received`, `Preparing`, `Ready`) & detailed ticket cards with progress bar & dish breakdown | **[Implemented]** | `KitchenScreen.kt` |
| **Kitchen Display (KDS)** | Blocking `KitchenOrderAlertDialog` for incoming `NEW` / `CONFIRMED` tickets with quick-reason cancellation chips | **[Implemented]** | `MainActivity.kt`, `LakemarkCommonUi.kt` |
| **Pickup Alerts** | Blocking `PickupReadyAlertDialog` for `READY` orders (`Reception`, `Waiter`, `Cashier`, `Delivery`) | **[Implemented]** | `MainActivity.kt`, `LakemarkCommonUi.kt` |
| **Manager Console** | **Overview:** Today sales vs. 7-day completed revenue, Active/Completed/Cancelled 7d pipeline pills, 7-day sales bar chart, Top 5 dishes | **[Implemented]** | `ManageTabs.kt` |
| **Manager Console** | **Menu:** Category-grouped menu list with inline ETB price edit and `On sale` availability switch | **[Partially Implemented]** *(No dish create/delete/name/desc editing)* | `ManageTabs.kt` |
| **Manager Console** | **Team → Accounts:** Filterable directory (`All`, `Staff`, `Waiters`, `Guests`), `+ Add staff`, `Reset password`, role selector, permanent customer `Approve`, staff active switch | **[Implemented / Partially Implemented]** *(Role change doesn't sync `staff_credentials.role`)* | `ManageTeamSection.kt`, `ManageScreen.kt` |
| **Manager Console** | **Team → Waiters:** On-duty counter, uncovered-area warning banner (`din`, `lob`, `f1`, `f2`), per-waiter duty switch & multi-area matrix | **[Implemented]** | `ManageTeamSection.kt` |
| **Manager Console** | **Team → Brands & BrandEditorSheet:** Live `60/30/10` preview, luminance-validated hex/swatch pickers, brand name (`<= 18`), mark (`<= 2`), reset to default | **[Implemented]** | `ManageTeamSection.kt`, `Sheets.kt`, `BrandPalette.kt` |
| **Manager Console** | **Alerts:** Broadcast composer (`Everyone` or any single role, `<= 300` chars) + activity feed (`All`, `Broadcasts`, `Order alerts`, max 20) | **[Implemented]** | `ManageAlertsSection.kt` |
| **Settings & Alerts** | `NotificationsSheet` (max 40 merged Firestore + FCM notifications, tap-to-deep-link) & unread bell badge | **[Implemented]** | `Sheets.kt`, `LakemarkViewModel.kt` |
| **Settings & Alerts** | `SettingsSheet`: Profile photo/name editor + Sign out, Waiter `On duty` switch, Admin `Role workspace` switcher, `My brand` editor, Language (`EN`/`AM`) switcher, Sound/Vibration/Volume preferences, FCM status & permission trigger, Check for Updates | **[Implemented]** | `Sheets.kt`, `UserPreferencesRepository.kt` |
| **In-App Updates** | Automatic `/app_config/release` check, `kind = "app_update"` FCM push with **Update Now** action, `AppUpdateAvailableDialog`, `ApkUpdateManager` verified in-place installer | **[Implemented]** | `ApkUpdateManager.kt`, `AlertAudioAndNotificationManager.kt`, `LakemarkViewModel.kt` |
| **Localization & Media** | Full English (`EN`) & Amharic (`AM`) translations (`TranslationCatalog`, `noto_sans_ethiopic.ttf`), `Manage → Language`, Saved Dishes (`SavedScreen`), Profile & Food photo uploads (`MediaImageUploader` + Firebase Storage) | **[Implemented]** | `TranslationCatalog.kt`, `ManageLanguageSection.kt`, `SavedScreen.kt`, `MediaImageUploader.kt` |
| **Push & Audio Engine** | Cloud Functions v2 FCM HTTP v1 dispatcher + auto-pruning stale tokens + topic fallback + `ttl = 3600000` ms | **[Implemented — Not Confirmed Deployed]** | `functions/index.js`, `functions/pushRouting.js` |
| **Push & Audio Engine** | `LakemarkFirebaseMessagingService`, `FcmTokenManager`, `NotificationDeduplicator`, `PushHistoryStore`, `AlertAudioAndNotificationManager` | **[Implemented]** | `app/src/main/java/com/example/fcm/`, `util/` |
| **Cashier Billing / POS** | Payment collection, receipt generation, cash/card tender tracking | **[Not Implemented]** | — |

---

## 3. User Roles, Permissions & Workflows

### 3.1 Role Matrix (`UserRole` in `LakemarkModels.kt`)

| Role (`key`) | Auth Path | Tabs Allowed (`allowedTabs`) | Bottom Nav Tabs (`bottomBarTabs`) | Order Stream Scope | Allowed Direct Order Transitions (`directActions`) | Additional Permissions |
|---|---|---|---|---|---|---|
| **Manager** (`manager`) | Google Sign-In (pre-authorized admin email or `admins/{uid}`) | Home, Floor, Menu, Orders, Kitchen, Manage | Home, Floor, **Orders**, Kitchen, Manage *(Menu omitted from bar)* | All orders | `CONFIRMED`, `PREPARING`, `READY`, `CANCELLED`, `DONE` | Workspace role switcher; manage users, roles, passwords, permanent customer approval; waiter coverage; menu price/availability; all 7 role brands; staff broadcasts; assign/reassign waiters. |
| **Reception** (`reception`) | App-managed Staff Sign-In (or promoted Google user) | Home, Floor, Menu, Orders | Same 4 | All orders | `CONFIRMED`, `CANCELLED`, `DONE` | Place orders for any zone; sees `PickupReadyAlertDialog` (`Confirm` = `DONE`); assign/reassign waiters; edit Reception brand. |
| **Kitchen** (`kitchen`) | App-managed Staff Sign-In | Home, Kitchen | Same 2 | All orders | `PREPARING`, `READY` *(plus UI `Cancel` from `NEW`/`CONFIRMED`)* | Sees blocking `KitchenOrderAlertDialog` on `NEW`/`CONFIRMED` orders; edit Kitchen brand. |
| **Waiter** (`waiter`) | App-managed Staff Sign-In | Home, Floor, Menu, Orders | Same 4 | Non-`out` orders where `assignedWaiterId == ""` or `== uid` | `DONE` | Place orders; toggle own `On duty` status in `SettingsSheet`; sees `PickupReadyAlertDialog` for scoped `READY` orders; edit Waiter brand. |
| **Cashier** (`cashier`) | App-managed Staff Sign-In | Home, Orders | Same 2 | All orders | `emptyList()` *(ViewModel blocks `advanceOrder`)* | Read-only order monitoring; sees `PickupReadyAlertDialog` (`Got it` dismiss only); UI currently permits assigning waiters; edit Cashier brand. |
| **Delivery** (`delivery`) | App-managed Staff Sign-In | Home, Orders | Same 2 | Strictly `orderType == "out"` | `DONE` | Sees `PickupReadyAlertDialog` for ready `OUT` orders (`Confirm` = `DONE`); edit Delivery brand. |
| **Customer** (`customer`) | Google Sign-In (`CredentialManager`) — **requires Manager activation** | Home, Floor, Menu, Orders | Same 4 | Strictly `userId == uid` | `emptyList()` *(ViewModel blocks `advanceOrder`)* | Place orders once permanently activated; `"My brand"` customization hidden in `SettingsSheet` (`isGoogleCustomerSession`). |

### 3.2 Authentication & Account Lifecycle Workflows

#### Flow A: Staff Sign-In (No Google Account Required)
1. Employee opens app → taps **Get Started** on `SplashScreen` → lands on `AuthScreen` (`Staff Sign In` tab selected by default).
2. Enters Manager-issued `username` (normalized: trimmed, lowercased, strips leading `@`, regex `^[a-z0-9._-]{3,32}$`) and `password` (`6–128` chars).
3. `StaffAuthClient.signInStaff` authenticates directly via `Firebase.auth.signInWithEmailAndPassword("$cleanUser@staff.lakemark.local", password)` and verifies the `/users/{uid}` profile (`active == true` and `role != "customer"`).
4. Manager provisioning (`seedDefaultStaffIfEmpty`, `createStaffAccountDirectly`, `updateStaffAccountDirectly`, `resetStaffPasswordDirectly`) uses a secondary `FirebaseApp` (`staffProvisioner`) to create/update Firebase Auth Email/Password accounts without disturbing the Manager's session, and writes `/users/{uid}` + `/staff_credentials/{uid}`.
5. `authStateFlow()` triggers `LakemarkViewModel` creation and syncs FCM registration.

#### Flow B: Manager / Admin Google Sign-In & Setup Gate
1. On launch, `MainActivity` invokes silent `attemptAutoSignIn` (`GetGoogleIdOption(filterByAuthorizedAccounts = true, autoSelectEnabled = true)`).
2. On `AuthScreen` → **Manager Login** tab → **Continue with Google** triggers `CredentialManager` (`GetSignInWithGoogleOption`) → exchanges Google ID token for Firebase Auth credential.
3. `LakemarkViewModel.bootstrapSession()` calls `LakemarkRepository.ensureUserProfile(firebaseUser)`:
   - **Pre-authorized Manager** (`email_verified == true` and email == `"abebaw4040@gmail.com"`, or doc in `/admins/{uid}`): creates or upgrades `/users/{uid}` to `role = "manager"`, `active = true`, `permanentlyActivated = true`, and seeds default 12 menu items if `menu_items` is empty.
   - **Existing User Records:** Preserved as-is without altering existing activation or role states.
   - **Unauthorized New Google Accounts:** No customer profile is auto-created during initial app setup; the user is signed out cleanly and shown `auth_manager_only_error` on `AuthScreen`.
4. All operational staff accounts (Reception, Kitchen, Waiter, Cashier, Delivery) are created and managed internally by the Manager under **Manage → Team** without Google login.

#### Flow C: Manager Provisioning & Password Reset for Staff
1. Manager opens **Manage → Team → Accounts** and taps **`+ Add staff`**.
2. `AddStaffAccountDialog` collects Name (`1–60` chars), Username (`3–32` chars), Role (`Reception`, `Kitchen`, `Waiter`, `Cashier`, `Delivery` — `Manager` and `Customer` excluded), and Password (`>= 6` chars).
3. `StaffAuthClient.createStaffAccount` sends Bearer ID Token to `POST /createStaffAccount`.
4. Cloud Function verifies caller via `verifyManagerRequest`, checks username uniqueness, calls `getAuth().createUser()`, writes `/users/{uid}` (`email = "${username}@staff.lakemark.local"`, `active = true`, `permanentlyActivated = true`), writes `/staff_credentials/{uid}` (scrypt hash + 16-byte random salt), and if `role == "waiter"` initializes `/waiter_coverage/{uid}` (`onDuty = true`, `areas = ["f1"]`).
5. For any username-bearing non-customer user, Manager can tap **`Reset password`** (`ResetStaffPasswordDialog`) → `POST /resetStaffPassword`.

### 3.3 Core Operational Workflows
- **Order Placement:** User selects Zone (`FloorScreen` tile or `MenuScreen` free-space card / Takeaway CTA) → adds available dishes → opens `CartCheckoutSheet` → enters Guest name (pre-filled for Customer), Phone number (if `OUT`), optional Special note → submits. Waiter auto-assigned (unless `OUT`) → transactional order counter increments → `orders/{orderId}` (`status = "new"`) and `notifications/{notifId}` (`kind = "new"`) created.
- **Reception Triage & Kitchen Prep:** Reception (or Manager) clicks **Send to kitchen** (`new` → `confirmed`) or **Cancel** (with reason) on `OrdersScreen`. Kitchen receives `KitchenOrderAlertDialog` on `new`/`confirmed` orders, taps **Accept** (`preparing`) or **Cancel** (with reason). Once cooked, Kitchen taps **Mark ready** (`preparing` → `ready`) in `KitchenScreen`.
- **Service Pickup / Delivery Handoff:** `PickupReadyAlertDialog` pops up for Reception, assigned/unassigned Waiter, Cashier (dismiss-only), and Delivery (`out` only). Authorized staff taps **Confirm** (`ready` → `done`).

---

## 4. Complete Screen & Navigation Structure

### 4.1 Navigation Architecture
Lakemark uses **reactive state-driven navigation** in `MainActivity.kt` (`showAuthScreen` + `LakemarkViewModel._localUi.currentTab` + modal booleans) rather than `NavHost` (note: `androidx.navigation.compose` is declared in `app/build.gradle.kts` but unused).

- **Hardware Back Handling (`BackHandler`):**
  - On `AuthScreen`: `BackHandler { showAuthScreen = false }` returns to `SplashScreen`.
  - On any non-`HOME` main tab: `BackHandler(enabled = uiState.currentTab != AppTab.HOME) { viewModel.selectTab(AppTab.HOME) }` returns to `Home`.
- **Adaptive Layout Container:** `LakemarkMainScaffold` centers main tab content inside `Box(Modifier.widthIn(max = 720.dp))`; checkout FAB and `FloatingPillNavBar` are constrained to `widthIn(max = 600.dp)` with edge-to-edge `WindowInsets` (`enableEdgeToEdge()`).

### 4.2 Screen, Sheet & Dialog Inventory

| Screen / Component | File | Purpose & Key Elements | Key `testTag`s |
|---|---|---|---|
| **SplashScreen** | `SplashScreen.kt` | Plum gradient screen, flame emblem (`LakemarkFlameEmblem`), `"LAKEMARK"` + `"Kitchen Management"`, `LakemarkPlateIllustration`, `"Get Started"` CTA. | `get_started_button` |
| **AuthScreen** | `AuthScreen.kt` | Segmented toggle (`Staff Sign In` vs `Manager Login`), staff username/password inputs + visibility toggle, Manager Google Sign-In button, staff setup hint, error banner. | `auth_mode_staff`, `auth_mode_google`, `staff_username_input`, `staff_password_input`, `staff_sign_in_button`, `google_sign_in_button` |
| **Account Gates** | `MainActivity.kt` | `Verifying account status…` spinner; `Approval Pending` (customer) or `Account Disabled` (staff) card with account identifier & Sign out. | `disabled_sign_out_button` |
| **App Shell (`LakemarkMainScaffold`)** | `MainActivity.kt`, `LakemarkCommonUi.kt` | `LakemarkTopBar` (`"LAKEMARK Bahir Dar"`, unread bell badge capped `99`, role-colored profile button); `FloatingPillNavBar` (Orders badge = `NEW` count; Kitchen badge = `CONFIRMED` count); `View order` checkout FAB; `BrandedToastBanner` (3.2s auto-dismiss, tap deep-links). | `top_notifications_button`, `top_settings_button`, `nav_tab_{key}`, `view_order_fab` |
| **HomeScreen** | `HomeScreen.kt` | Date/role header (`"EEEE, d MMM · {Role}"` + waiter areas), role-specific Hero KPI + 3-row stack, dark plum analytics card (`CircularServedRing` + `MiniSevenDayBarChart`), 3 `AreaOccupancyRow` cards, top 3 `Latest orders` + `"See all"`. | `area_card_{hotel\|dine\|rec}`, `home_see_all_orders` |
| **FloorScreen** | `FloorScreen.kt` | Live occupancy hero, zone segmented toggle (`Rooms`, `Restaurant`, `Reception`), 4-color legend, `SelectedPlaceBanner`, `HotelsFloorGrid` (F1 `101–116`, Corridor, F2 `201–216`, Lift/Stairs), `RestaurantFloorGrid` (Tables `1–12` with chair bars, Patio/VIP), `ReceptionFloorGrid` (`T1–T20`, Entrance/Front desk). | `floor_zone_{key}`, `floor_tile_{location}` |
| **MenuScreen** | `MenuScreen.kt` | Free-space prompt (`AreaOccupancyRow` + `"Takeaway / delivery"` button) if no place selected; otherwise `SelectedPlaceBanner`, search bar, horizontal category pills, responsive 2-col (`<680dp`) / 3-col (`>=680dp`) `MenuDishCard` grid with category vector icons, `"Sold out"` state, inline `− qty +` stepper. | `menu_start_takeaway`, `menu_search_input`, `menu_cat_{category}`, `menu_add_{id}`, `menu_dec_{id}` |
| **OrdersScreen** | `OrdersScreen.kt` | Active counter hero, `Active`/`Finished` toggle, 5 zone filter pills, day-grouped `CompactOrderRow` list with ticket token (`214`, `T3`, `R5`, `OUT`), `StatusPill`, waiter chip, `Assign waiter`/`Reassign` chip, action buttons. | `orders_filter_{active\|finished}`, `orders_zone_{key}`, `order_row_{orderNumber}` |
| **KitchenScreen** | `KitchenScreen.kt` | Cooking count hero, 3 clickable stage cards (`Received`=`CONFIRMED`, `Preparing`=`PREPARING`, `Ready`=`READY`), `OrderTicketCard`s with progress bar (`LinearProgressIndicator`), line items, special note, `Start preparing` / `Mark ready` / `Cancel`. | `kitchen_stage_{key}`, `kitchen_ticket_{orderNumber}` |
| **ManageScreen** | `ManageScreen.kt`, `ManageTabs.kt`, `ManageTeamSection.kt`, `ManageAlertsSection.kt` | Role-gated (`isAdminUser`) 4-tab console: **Overview** (KPIs, pipeline pills, 7-day bars, Top 5 dishes), **Menu** (search, categories, inline price edit, `On sale` switch), **Team** (`Accounts`, `Waiters`, `Brands`), **Alerts** (broadcast composer + activity feed). | `manage_tab_{section}`, `overview_kpi_{today\|7d}`, `manage_reports_card`, `manage_menu_switch_{id}`, `team_subtab_{key}`, `add_staff_account_button`, `customer_approve_{uid}`, `account_switch_{uid}`, `waiter_duty_{uid}`, `waiter_area_{uid}_{area}`, `manage_brand_{role}`, `send_broadcast_button` |
| **CartCheckoutSheet** | `Sheets.kt` | Modal bottom sheet: selected place + `Change`, cart items with `Remove`, phone field (`OUT` only), Guest name, Special requests, Subtotal + **15% VAT** + Total, `"Place order"`. | `checkout_name_input`, `checkout_phone_input`, `checkout_note_input`, `place_order_button` |
| **NotificationsSheet** | `Sheets.kt` | Modal bottom sheet: up to 40 merged Firestore + FCM notifications styled with sender brand palette/mark, relative time, tap to deep-link. | — |
| **SettingsSheet** | `Sheets.kt` | Modal bottom sheet: user card + photo/name edit + `Sign out`, Waiter `On duty` switch, Admin `Role workspace` 7-role pills, `My brand` editor entry (non-customer), Language (`EN`/`AM`) switcher, Sound/Vibration toggles + Volume slider, Push notification status card + device ID + `Allow` permission CTA (API 33+), `Check for Updates`, footer `"Lakemark v3.4.4 · Bahir Dar"`. | `settings_sign_out_button`, `settings_role_chip_{key}`, `settings_my_brand_button`, `settings_sound_switch`, `settings_vibration_switch`, `settings_app_version` |
| **BrandEditorSheet** | `Sheets.kt` | Live `60/30/10` preview card, luminance-enforced swatches & hex inputs for Primary (`>= 0.55`), Secondary (`<= 0.22`), Accent, Brand name (`<= 18`), Mark (`<= 2`), `Reset`, `Save brand`. | `brand_editor_name`, `brand_editor_mark`, `brand_editor_save` |
| **Modal Dialogs** | `LakemarkCommonUi.kt`, `ManageTeamSection.kt` | `KitchenOrderAlertDialog` (non-dismissible outside; Accept / Cancel with 4 quick chips or custom reason); `PickupReadyAlertDialog` (`Later` / `Confirm` or `Got it`); `WaiterAssignDialog` (`Auto-assign nearest` + waiter list); `AddStaffAccountDialog`; `ResetStaffPasswordDialog`. | `kitchen_alert_accept`, `kitchen_alert_cancel`, `pickup_alert_later`, `pickup_alert_confirm`, `add_staff_*`, `reset_pw_*` |

### 4.3 Deep Linking & Intent Routing (`PushDeepLink`)
- **URI Scheme:** `lakemark://app/open?...`, `lakemark://app/notifications`, `lakemark://app/kitchen`, `lakemark://app/orders`.
- **Intent Extras (`MainActivity.handleNotificationIntent`):** `lakemark_extra_tab` / `targetTab` / `tab`, `lakemark_extra_order_id` / `orderId`, `lakemark_extra_notif_id` / `notifId`, `lakemark_extra_open_sheet` / `openNotificationsSheet`, `lakemark_extra_kind` / `kind`, `lakemark_extra_order_status` / `orderStatus` / `status`.
- **Fallback Resolution:** If `tab` is disallowed for `activeRole`, `PushDeepLink.resolveTargetTab` falls back to `KITCHEN` (for kitchen with `orderId`), `ORDERS` (if allowed with `orderId`), or `HOME`.

---

## 5. Business Rules & Operational Processes

### 5.1 Customer Account Activation & Staff Auth Rules
1. **Customer Default-Disabled Rule (Mandatory):** Every non-admin Google sign-in creates `/users/{uid}` with `role = "customer"`, `active = false`, `permanentlyActivated = false`. Enforced in client (`LakemarkRepository`), ViewModel (`isVerifyingAccount` / `accountDisabled` gates), and `firestore.rules` (`allow create` rejects `active: true` or `permanentlyActivated: true` unless `isAdmin()`).
2. **Irreversible Permanent Activation:** Once a Manager approves a customer (`active = true, permanentlyActivated = true`), `firestore.rules` explicitly forbids reverting `active` or `permanentlyActivated` back to `false` while `role == "customer"`. `LakemarkViewModel.toggleStaffAccountActive` also blocks toggling off permanently activated customers.
3. **Hardcoded Owner/Admin Bootstrap:** Verified Google email `abebaw4040@gmail.com` (plus documents in `/admins/{uid}`) bypasses customer gating and auto-provisions as active `manager`.
4. **Staff Credential Isolation:** Staff accounts use `${username}@staff.lakemark.local` placeholder emails, `scrypt` (`N=16384`, 64-byte key, 16-byte random salt) hashes in `/staff_credentials/{uid}` (`allow read, write: if false` for all client SDKs), and never require Google accounts or Firebase Console access.

### 5.2 Floor Zones, Locations & Occupancy Rules
- **3 On-Premise Zones (`FloorZone`):**
  - `HOTEL` (**Rooms**, `OrderType.ROOM`, capacity **32**): Floor 1 (`"101"`–`"116"`) and Floor 2 (`"201"`–`"216"`). Tile label = room number; full label = `"Room {n}"`; ticket token = `"{n}"`.
  - `DINE` (**Restaurant**, `OrderType.DINE`, capacity **12**): `"Table 1"`–`"Table 12"`. Tile label = `1..12`; ticket token = `"T{n}"`.
  - `REC` (**Reception**, `OrderType.PICKUP`, capacity **20**): `"Reception T1"`–`"Reception T20"`. Tile label = `"T1".."T20"`; ticket token = `"R{n}"`.
- **Off-Premise (`OrderType.OUT`):** `"Takeaway / delivery"`, ticket token = `"OUT"`, requires customer phone matching `^[0-9+ ()-]{7,16}$`.
- **Occupancy Semantics:** A tile is marked occupied by `liveOrderFor` using the first non-terminal (`!isTerminal`) matching order. Note: selecting an occupied tile displays a status toast (`"Room 214 has order #1001 (Preparing)"`) **and still allows placing an additional order** for that same location.

### 5.3 Menu, Cart & Pricing Rules
- **Categories (`DefaultMenuCatalog.categories`):** `All`, `Classics`, `Lake Tana`, `Grill`, `Breakfast`, `Drinks` (12 seeded dishes from `m0` `Doro Wat` at `420 ETB` to `m11` `Spris Juice` at `110 ETB`).
- **Cart Constraints:** 1 to 30 line items per order (`firestore.rules` enforces `items.size() in 1..30`); sold-out items (`available == false`) are blocked both on add-to-cart and checkout validation.
- **Pricing & Tax Formula:**
  - `subtotal = sum(item.price * item.quantity)` (integer ETB)
  - `vat = (subtotal * 0.15).roundToLong()` (**15% VAT**)
  - `total = subtotal + vat`
  - **Special Note Propagation:** Checkout `specialNote` (`<= 200` chars) is stored on `Order.specialNote` **and** copied onto every `OrderLineItem.note`.

### 5.4 Transactional Order Numbering & State Machine
- **Order Number Counter:** `createOrder` runs a Firestore transaction on `/counters/orders`. If absent, initializes `nextOrderNumber = 1008` and assigns **`1007`** (rules allow client creation `>= 1001`); otherwise assigns `current` and atomically increments by `+1` (rules enforce `incoming == existing + 1`).
- **Order Status Transitions (`OrderStatus`):**
  - `NEW` (`"new"`, progress `0.12`) → `CONFIRMED` or `CANCELLED`
  - `CONFIRMED` (`"confirmed"`, progress `0.25`) → `PREPARING` or `CANCELLED` *(note: `OrderStatus.nextStatuses` only lists `[PREPARING]`, while `LakemarkRepository` & `firestore.rules` allow cancelling from `NEW` or `CONFIRMED` with non-blank `cancelReason`)*
  - `PREPARING` (`"preparing"`, progress `0.55`) → `READY`
  - `READY` (`"ready"`, progress `0.85`) → `DONE`
  - `DONE` (`"done"`, progress `1.0`) & `CANCELLED` (`"cancelled"`, progress `0.0`): **terminal & immutable** for non-admin staff in `firestore.rules`.
- **Audit Timestamps:** `createdAt`, `confirmedAt`, `preparingAt` (backfills `confirmedAt` if transitioning `NEW → PREPARING`), `readyAt`, `doneAt`, `cancelledAt`, `updatedAt`, plus `lastUpdatedByUid` & `lastUpdatedByRole`.

### 5.5 Waiter Coverage & Auto-Assignment Algorithm
- **4 Waiter Areas (`WaiterArea`):** `din` (Restaurant), `lob` (Lobby/Reception), `f1` (Floor 1: rooms starting with `"1"`), `f2` (Floor 2: rooms not starting with `"1"`). `OUT` orders map to `null` (no waiter assigned).
- **`pickBestWaiter` Selection Logic (`LakemarkViewModel.kt`):**
  1. Filter `waiterUsers` to `active == true` and `coverage[uid]?.onDuty ?: true`.
  2. If any on-duty waiters cover `orderArea`, narrow candidates to that subset; otherwise **fall back to all on-duty waiters** across other areas.
  3. Select waiter with **minimum count of active (`!isTerminal`) assigned orders**, breaking ties alphabetically by `name.lowercase()`.
- **Assignment Methods:** `"auto"` on order placement or `"Auto-assign nearest"` in `WaiterAssignDialog`; `"manual"` when picked explicitly by Manager, Reception, or Cashier. Triggers a `"ping"` notification targeting `["waiter", "manager"]` and `targetUserId = chosenWaiterId`.

### 5.6 Analytics & KPI Calculations
- **Home Screen (`HomeScreen.kt`):**
  - `todayOrders`: orders with `createdAt >= local midnight`.
  - `todaySales`: sum of `order.total` (post-VAT) across `todayOrders` where `status != CANCELLED` (includes active orders).
  - `servedPct`: `(doneToday / todayOrders.size) * 100`.
- **Manage Overview (`ManageTabs.kt`):**
  - `Today's sales`: same `todaySales` definition above.
  - `7-day revenue`: sum of `order.total` for `DONE` orders in last 7 calendar days (`doneAt ?: updatedAt ?: createdAt >= todayStart - 6 days`).
  - `Top 5 dishes`: aggregates `line.quantity` and `line.quantity * line.price` (**pre-VAT**) across 7-day `DONE` orders, sorted descending by quantity.

### 5.7 Brand Palette `60 / 30 / 10` Luminance Rules
- `BrandEditorSheet` & `BrandPalette.kt` enforce:
  - **60% Primary (Background):** `relativeLuminance >= 0.55` (light/cream tone).
  - **30% Secondary (Plum/Chrome):** `relativeLuminance <= 0.22` (dark tone for contrast).
  - **10% Accent (CTA/Badges):** High-contrast accent (`onAcc` auto-selected between white and darkened secondary via WCAG contrast ratio).

---

## 6. Firebase Backend, Database & Integrations

### 6.1 Firebase Configuration
- **GCP / Firebase Project:** `lakemark-app` (Project Number `1010314964245`).
- **Firestore Database:** `(default)` database in `nam5` (configured in `firebase_applet_config.xml`, `firebase.json`, `functions/index.js`, `storage.rules`).
- **Firebase Storage Bucket:** `lakemark-app.firebasestorage.app` (configured in `google-services.json`, `storage.rules`).
- **Android OAuth Clients (`google-services.json`):** Active package `com.aistudio.applet.kphrxs` (`1:1010314964245:android:3ce0bd71b81cbf9a630d1c`) with Web OAuth Client (`default_web_client_id`, `client_type: 3`).
- **Auth & Notification Mode:** Option A (Direct Admin Login via Google Sign-In + native Firebase Auth Email/Password staff provisioning + real-time Firestore notification listeners; no Render/Railway or Cloud Functions required on Spark).

### 6.2 Firestore Data Model & Security Rules (`firestore.rules` & `firebase-blueprint.json`)

| Collection / Path | Purpose | Key Fields | Access Control (`firestore.rules`) |
|---|---|---|---|
| `/users/{userId}` | User accounts & roles | `userId`, `email`, `username`, `name`, `role`, `active`, `permanentlyActivated`, `createdAt`, `updatedAt` | **Read:** Self, Admin, Active Manager. **Create:** Self (`role=customer, active=false, permanentlyActivated=false`) or Admin. **Update:** Self (`name, email, updatedAt` only) or Admin/Active Manager (`name, role, active, permanentlyActivated, updatedAt`; cannot deactivate permanently activated customer). **Delete:** `false`. |
| `/users/{userId}/fcm_tokens/{deviceId}` | Per-device FCM tokens | `token`, `userId`, `deviceId`, `role`, `platform='android'`, `appVersion`, `notificationsEnabled`, `createdAt`, `updatedAt` | **Get/List/Delete:** Self. **Create/Update:** Activated account + Self (`userId`, `deviceId`, `platform`, `createdAt` immutable on update). |
| `/admins/{adminId}` | Optional extra admin UID list | *(existence check only)* | **Get:** Self (`uid == adminId`). **List/Write:** `false`. |
| `/staff_credentials/{staffId}` | Scrypt hashes for staff login | `userId`, `username`, `passwordHash`, `passwordSalt`, `role`, `createdAt`, `updatedAt` | **All Client SDK Reads & Writes:** `false` (Cloud Functions Admin SDK only). |
| `/menu_items/{itemId}` | Dish catalog | `name`, `category`, `description`, `price` (`1..100000`), `available`, `sortOrder` (`0..1000`), timestamps | **Read:** Activated accounts (`available == true`, or any if Admin/Active Staff). **Create/Update/Delete:** Admin or Active Manager. |
| `/orders/{orderId}` | Customer & staff orders | `orderNumber`, `userId`, `createdByRole`, `lastUpdatedByRole`, `lastUpdatedByUid`, `customerName`, `orderType`, `location`, `status`, `items` (1–30), `specialNote`, `cancelReason`, `assignedWaiterId`, `assignedMethod`, `subtotal`, `total`, stage timestamps | **Read:** Activated Owner, Admin, or Active Staff. **Create:** Activated user (`status='new'`, `userId==uid`). **Update:** Admin (unrestricted valid schema) or Active Staff (strict non-terminal state-machine transitions & allowed diff keys, or waiter assignment). **Delete:** `false`. |
| `/brands/{roleKey}` | Per-role 60/30/10 themes | `roleKey`, `primaryHex`, `secondaryHex`, `accentHex`, `brandName` (`<=18`), `mark` (`<=2`), `updatedBy`, `updatedAt` | **Read:** Activated accounts. **Create/Update/Delete:** Admin, Active Manager, or Active User matching `roleKey`. |
| `/notifications/{notifId}` | Order alerts, broadcasts & app updates | `senderUid`, `senderRole`, `senderBrandName`, `message` (`<=300`), `targetRoles`, `targetUserId`, `orderId`, `kind` (`new\|ready\|ping\|broadcast\|app_update`), `apkUrl`, `createdAt` | **Read:** Activated target user, sender, `all`/`customer` targets, Admin, or Active Staff. **Create:** Admin/Active Staff, or Customer ONLY for `kind=='new'` & `targetUserId==uid`. **Update/Delete:** `false`. |
| `/counters/{counterId}` | Sequential order counter | `nextOrderNumber` (`1001..9999999`), `updatedAt` (`counterId == 'orders'`) | **Get/Create/Update:** Activated accounts (`update` enforces `nextOrderNumber == existing + 1`). **List/Delete:** `false`. |
| `/waiter_coverage/{waiterId}` | Waiter duty & area list | `userId`, `waiterName`, `onDuty`, `areas` (`<=4`), `updatedAt` | **Read:** Admin or Active Staff. **Create/Update:** Admin, Active Manager, or Active Staff for own `waiterId`. **Delete:** Admin or Active Manager. |
| `/translations/{key}` | Live EN/AM UI overrides | `key`, `en`, `am`, `category`, `updatedBy`, `updatedAt` | **Read:** Signed-in users. **Create/Update/Delete:** Admin or Active Manager. |
| `/app_config/release` | In-app APK update metadata | `latestVersionCode`, `latestVersionName`, `apkDownloadUrl`, `apkSha256`, `releaseNotes`, `mandatory`, `updatedAt` | **Read:** Signed-in users. **Write:** Admin or Active Manager. |
| `/users/{userId}/saved_items/{menuItemId}` | User bookmarked dishes | `menuItemId`, `savedAt` | **Read/Write:** Self (`uid == userId`). |

### 6.3 Cloud Functions v2 (`functions/index.js`, Node 20, `us-central1`)

| Function | Trigger | Behavior | Status |
|---|---|---|---|
| `dispatchLakemarkNotification` | `onDocumentCreated` (`notifications/{notifId}`, `database: DATABASE_ID`, `region: us-central1`) | Loads sender brand & order metadata; resolves active recipients (suppressing `new` self-echo and inactive accounts); filters `/fcm_tokens` by `notificationsEnabled`, `effectiveRole`, and `delivery`/`waiter` order scope; sends FCM HTTP v1 hybrid `notification+data` payload (`priority: "high"`, `ttl: 3600000` ms) in batches of 500; deletes unregistered tokens; falls back to topics if 0 direct tokens matched. | **[Implemented — Not Confirmed Deployed]** |
| `staffSignIn` | HTTPS `onRequest` (`POST`) | Validates `username`/`password`, verifies `scrypt` + `timingSafeEqual` against `staff_credentials`, enforces `users/{uid}.active === true` and `role !== 'customer'`, mints Firebase custom token. | **[Implemented — Not Confirmed Deployed]** |
| `createStaffAccount` | HTTPS `onRequest` (`POST` + Bearer ID token) | Verifies caller is active Manager/Admin; creates Auth user + `/users/{uid}` + `/staff_credentials/{uid}` (+ `/waiter_coverage/{uid}` if waiter). Disallows creating `manager` or `customer`. | **[Implemented — Not Confirmed Deployed]** |
| `resetStaffPassword` | HTTPS `onRequest` (`POST` + Bearer ID token) | Verifies caller is active Manager/Admin; updates `passwordHash`/`passwordSalt` in `/staff_credentials/{uid}` for non-customer username accounts. | **[Implemented — Not Confirmed Deployed]** |

### 6.4 Client-Side Persistence & External Integrations
- **Jetpack DataStore (`lakemark_prefs`):** Stores `sound_enabled` (`true`), `vibration_enabled` (`true`), `alert_volume` (`0.8f`), `preferred_role` (`"manager"`), `last_notif_read_ms` (`0L`), `onboarding_seen` (`false` — unused).
- **`SharedPreferences` Stores:**
  - `lakemark_push_history`: Persists up to 40 recent FCM notifications, stable `deviceId` (`android_{16-hex-uuid}`), current FCM token, topic list, and last registration timestamp.
  - `lakemark_push_dedup`: Persists `DeduplicationCache` (up to 150 keys, 10-minute TTL) across process restarts.
- **Jetpack Credential Manager (`1.5.0`) + Google ID (`1.1.1`):** Google Sign-In & silent re-auth; sign-out calls `ClearCredentialStateRequest()`. (Manifest merger adds `android.permission.USE_BIOMETRIC` from Google Identity / Credential dependencies alongside `INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`, and `POST_NOTIFICATIONS`.)
- **Backup & Device-Transfer Rules (`backup_rules.xml`, `data_extraction_rules.xml`):** `android:allowBackup="true"` preserves app preferences across backups, while `lakemark_push_store.xml` and `lakemark_push_dedup.xml` are explicitly excluded from cloud backup and device transfer so device-specific `deviceId` / FCM token state does not leak to a different physical device.

---

## 7. Functional & Non-Functional Requirements

### 7.1 Functional Requirements

| ID | Requirement Description | Status |
|---|---|---|
| **FR-AUTH-01** | Customers and Managers shall authenticate via Google Sign-In (`CredentialManager`) with silent re-authentication on launch. | **[Implemented]** |
| **FR-AUTH-02** | Staff (`reception`, `kitchen`, `waiter`, `cashier`, `delivery`) shall authenticate via app-managed username and password without needing Google accounts. | **[Implemented — Not Confirmed Deployed]** |
| **FR-AUTH-03** | Newly signed-in non-admin Google accounts shall default to `role = "customer"`, `active = false`, `permanentlyActivated = false` and be blocked from operational screens/data until a Manager permanently activates them. | **[Implemented]** |
| **FR-AUTH-04** | Managers shall create staff accounts, reset staff passwords, change account roles, and toggle staff `active` status (except managers and permanently activated customers). | **[Implemented — Not Confirmed Deployed]** |
| **FR-MENU-01** | Managers shall seed the default 12-item catalog when empty and edit dish prices (`1..100,000 ETB`) and availability (`available`). | **[Implemented]** |
| **FR-MENU-02** | Managers shall create new dishes, delete dishes, and edit dish names, descriptions, categories, or images. | **[Not Implemented]** |
| **FR-FLOOR-01** | Authorized users shall browse 3 floor zones (32 Rooms, 12 Restaurant tables, 20 Reception tables) with live status tinting and initiate Takeaway/Delivery (`OUT`) orders. | **[Implemented]** |
| **FR-ORD-01** | Users shall build a cart (1–30 available items), compute 15% VAT (`roundToLong`), validate phone numbers for `OUT` orders, and submit orders with atomic sequential order numbers. | **[Implemented]** |
| **FR-ORD-02** | Order state transitions shall follow the zero-trust state machine (`NEW → CONFIRMED → PREPARING → READY → DONE`, or `CANCELLED` from `NEW`/`CONFIRMED` with mandatory reason) gated by user role. | **[Implemented]** |
| **FR-KDS-01** | Kitchen staff shall receive blocking modal alerts for `NEW`/`CONFIRMED` orders and track `CONFIRMED`/`PREPARING`/`READY` stages on `KitchenScreen`. | **[Implemented]** |
| **FR-WAITER-01** | Waiters shall toggle their duty state; Managers, Reception, and Cashiers shall auto-assign (area-first, least active orders) or manually assign waiters to non-`OUT` orders. | **[Implemented]** |
| **FR-POS-01** | Cashiers shall collect/record payments, tender types, and receipts for completed orders. | **[Not Implemented]** |
| **FR-ADMIN-01** | Managers shall view Today vs. 7-day sales analytics, customize 60/30/10 role brands, switch workspace role views, and send role/all staff broadcasts. | **[Implemented]** |
| **FR-NOTIF-01** | Order events and broadcasts shall trigger deduplicated real-time in-app alerts, sound/vibration, and high-priority FCM push notifications across foreground, background, and killed states, with sign-out token removal. | **[Implemented — Not Confirmed Deployed]** |

### 7.2 Non-Functional Requirements

| Category | Requirement | Audited Implementation |
|---|---|---|
| **Security & Zero Trust** | Fail-closed database access, server-side credential hashing, no embedded private keys. | `firestore.rules` denies by default (`/{document=**}`); `staff_credentials` client access completely denied; scrypt + `timingSafeEqual` in Cloud Functions; no private keys embedded. |
| **Performance & Concurrency** | Non-blocking async I/O and bounded recomposition. | Coroutines/Flow (`Dispatchers.IO` for HTTP & AudioTrack), `stateIn(..., WhileSubscribed(5000L))`, `remember`/`derivedStateOf`. *Note: unbounded collection snapshots exist (see Section 9).* |
| **Reliability & Deduplication** | Prevent duplicate alerts across dual delivery paths (Firestore + FCM). | Shared `NotificationDeduplicator` (150 keys, 10-min TTL in `SharedPreferences`), self-echo suppression (`senderUid != uid`), and 3-minute session watermark. |
| **Compatibility & Adaptive UI** | Support Android SDK 24–36, edge-to-edge insets, tablets/foldables. | `minSdk = 24`, `targetSdk = 36`, `enableEdgeToEdge()`, `widthIn(max = 720.dp / 600.dp)`, adaptive 2/3-col Menu grid at `680.dp`. |
| **Accessibility & Testability** | Touch targets `>= 48.dp`, content descriptions, Compose `testTag`s. | `testTag`s applied across primary controls; `Icon`s include `contentDescription`; `48.dp`+ touch targets on primary buttons/switches. |
| **Automated Test Coverage** | Unit, Compose/Robolectric, Cloud Functions, and Firestore Rules tests. | 5 Node tests (`functions/index.test.js`), 4 Firestore emulator tests (`firestore.test.js`), 9 FCM unit tests (`FcmPushTests.kt`), 8 Compose Robolectric tests (`LakemarkTests.kt`), plus template tests. |

---

## 8. Notifications, Localization & User Management

### 8.1 Dual-Path Notification Architecture
Lakemark combines two synchronized real-time notification channels reconciled through `NotificationDeduplicator`:
1. **Cloud Functions + FCM HTTP v1 (`dispatchLakemarkNotification` + `LakemarkFirebaseMessagingService`):**
   - Triggers on `notifications/{notifId}` creation in the named Firestore database (`us-central1`).
   - Sends **hybrid (`notification` + `data`)** Android high-priority payloads (`ttl = 3,600,000` ms / 1 hour, `sound: "default"`, `defaultSound: true`, `defaultVibrateTimings: true`).
   - **Killed / Background:** Android OS displays the system notification directly on the targeted `_v2` channel with system default sound & vibration; tapping opens `MainActivity` with intent extras/deep link.
   - **Foreground:** `onMessageReceived` parses `FcmPayloadParser`, filters via `PushRoutingPolicy`, records in `PushHistoryStore` (max 40), checks `NotificationDeduplicator`, and emits via `InAppPushCoordinator.foregroundEvents` to play the synthesized chime + show `BrandedToastBanner` (or posts a local notification if backgrounded).
2. **In-App Firestore Real-Time Observers (`LakemarkViewModel`):**
   - `observeRealTimeAlerts()` watches `orders` (suppresses initial snapshot and events older than `sessionStartEpochMs - 180_000L`) and dispatches foreground toast/audio or background local notifications via `AlertAudioAndNotificationManager`.
   - `observeIncomingNotifications()` watches `notifications` for `broadcast` and direct `ping` events.

### 8.2 Notification Channels, Audio & Lifecycle Cleanup
- **3 Channels (`AlertAudioAndNotificationManager` & `LakemarkApplication.onCreate`):**
  1. `lakemark_kitchen_alerts_v2` (`IMPORTANCE_HIGH`, `RingtoneManager.TYPE_NOTIFICATION`, pattern `[0, 220, 120, 220]`, public lockscreen)
  2. `lakemark_ready_alerts_v2` (`IMPORTANCE_HIGH`, pattern `[0, 180, 100, 180]`)
  3. `lakemark_broadcast_alerts_v2` (`IMPORTANCE_HIGH`, pattern `[0, 160]`)
  - Legacy v1 channels (`lakemark_kitchen_alerts`, `lakemark_ready_alerts`, `lakemark_broadcast_alerts`) are deleted on startup so upgraded installs inherit high-importance sound settings.
- **In-App Synthesized Chimes (`AudioTrack` `MODE_STATIC`, 44.1 kHz PCM):**
  - `NEW_ORDER`: 4-tone sequence (`880 / 660 / 880 / 660 Hz`)
  - `ORDER_READY`: C5 major arpeggio (`523.25 / 659.25 / 783.99 / 1046.5 Hz`)
  - `PING`: 2-tone chime (`660 / 880 Hz`)
  - Scaled by `prefs.volume` (`0f..1f`), serialized via `chimeMutex`, with `RingtoneManager` fallback if `AudioTrack` initialization fails.
- **Token Registration & Sign-Out Pruning:**
  - `FcmTokenManager` registers `/users/{uid}/fcm_tokens/{deviceId}` + subscribes to topics (`lakemark_role_{role}`, `lakemark_staff`, `lakemark_all`, `lakemark_user_{uid}`) only when `isAdmin || active == true`.
  - Signing out (or detecting inactive profile) calls `unregisterOnSignOut`, deleting the Firestore token doc, unsubscribing all topics, and clearing `FirebaseMessaging.deleteToken()`.

### 8.3 Localization & Typography
- **Localization State:** **Implemented (`EN` & `AM`).** English (`EN`) is the default language, with full Amharic (`AM`) coverage via `AppStrings` (`LocalAppStrings`), `AppLanguage`, and `TranslationCatalog`, persisted in DataStore (`app_language`, `cached_translations_json`) and editable live by Managers in `Manage → Language` (`/translations/{key}`).
- **Typography:** Bundled local `.ttf` fonts (`manrope.ttf`, `inter.ttf`, `plus_jakarta_sans.ttf`, and `noto_sans_ethiopic.ttf` in `res/font/`) wired via `Type.kt`.

---

## 9. Identified Gaps, Inconsistencies & Improvement Opportunities

### P0 — Critical (Deployment, Auth Integrity & Data Security)
1. **Unverified Live Cloud Functions Deployment & Physical Device Verification:**
   - `StaffAuthClient` explicitly guards against HTTP `404` (`"Staff authentication service is not deployed yet on Firebase Functions."`). All 4 Cloud Functions (`dispatchLakemarkNotification`, `staffSignIn`, `createStaffAccount`, `resetStaffPassword`), `firestore.rules`, and multi-device physical FCM/sound behavior require live deployment and on-device verification before production sign-off.
2. **Role Mutation Desynchronization (`ManageTeamSection` / `LakemarkRepository.updateStaffAccount`):**
   - Updating a username-based staff user's role in `/users/{uid}` does **not** update `/staff_credentials/{uid}.role` (and cannot delete `/users/{uid}` or Auth records). Also, the Team role dropdown lets a Manager change a user's role to `Manager` (hiding their disable `Switch` and labeling them `"Owner"` in `StaffAccountCard`) or `Customer` without revoking Auth custom claims.
3. **Non-Atomic Username Check & Missing Brute-Force Protection (`functions/index.js`):**
   - `createStaffAccount` checks username uniqueness with a non-transactional query before calling `getAuth().createUser()` + `batch.commit()`. Concurrent requests could create duplicate usernames or orphan Auth users if the batch write fails. `staffSignIn` also has no rate limiting or lockout policy.
4. **Role-Workspace Switcher vs. Admin Privilege Split-Brain (`LakemarkViewModel.kt`):**
   - When an Admin switches workspace role (e.g., to `Customer` or `Waiter`) in `SettingsSheet`, `activeRole` changes while `isAdminUser` remains `true` (`assignedRole == MANAGER`). Consequently, Firestore rules (`isAdmin()`), `ObserveOrders` (`isStaffOrAdmin`), `ObserveNotifications`, and `ManageTabs` still run with Admin privileges while `HomeScreen`/`OrdersScreen` render the switched role.
5. **Production Keystore & Store Distribution Architecture (`app/build.gradle.kts`):**
   - `signingConfigs.release` expects `KEYSTORE_PATH` (`my-upload-key.jks`, alias `upload`), `STORE_PASSWORD`, and `KEY_PASSWORD`, which are not provisioned in the current workspace. All existing builds (`v8`, `v9`, `v10`) share the `google-services.json`-registered keystore (`debug.keystore`, `SHA-1: d2a3421ac7271b0eba97d3311310e5e9d3f0459b`). While this guarantees seamless in-place upgrades for direct APK installs, Google Play Store distribution requires provisioning a dedicated non-debug upload keystore and registering its SHA-1 in Firebase Console. Furthermore, no Play In-App Updates or Firebase App Distribution SDK is integrated for automated/incremental delivery.

### P1 — High (Core Operational & Functional Gaps)
5. **Kitchen Screen Omits `NEW` Orders (`KitchenScreen.kt`):**
   - `KitchenScreen` only provides stage filter cards for `CONFIRMED`, `PREPARING`, and `READY`, even though `KitchenOrderAlertDialog` triggers on both `NEW` and `CONFIRMED` orders. If a kitchen user dismisses or navigates away before Reception confirms, `NEW` orders are invisible on `KitchenScreen`.
6. **Cashier Role Lacks Billing / POS Functionality & Has Misaligned Permissions:**
   - `UserRole.CASHIER` has `directActions = emptyList()`, no payment method/settlement/receipt features, and its Home card labels `DONE` orders `"settled"`. Meanwhile `OrdersScreen` and `LakemarkViewModel.assignOrderWaiter` grant `CASHIER` permission to assign waiters.
7. **Incomplete Menu Catalog CRUD & Price Validation Mismatch:**
   - `firestore.rules` permits Manager `create`, `update`, and `delete` on `menu_items`, but `ManageTabs.kt` only exposes price editing and `available` toggling—no UI to add/delete dishes or edit names/descriptions/categories/images (`coil-compose` is disabled).
   - `ManageTabs.kt` allows typing `0` (`parsedPrice >= 0L`), `LakemarkRepository.updateMenuItem` silently clamps `1L..100000L`, and `firestore.rules` enforces `price >= 1`. Also `MenuItem.description` is searchable in `MenuScreen` but never rendered on `MenuDishCard`.
8. **VAT Not Stored Explicitly or Enforced Server-Side (`firestore.rules` / `Order`):**
   - `Order` stores `subtotal` and `total`, not `vatAmount`; `firestore.rules` only validates `subtotal in 0..10000000` and `total in 0..11500000` without verifying `total == round(subtotal * 1.15)` or server-side price recalculation against `menu_items`.

### P2 — Medium (UX Consistency, Scalability & Data Reporting)
9. **KPI Metric & Currency Divergences (`HomeScreen.kt` vs. `ManageTabs.kt`):**
   - `HomeScreen` and `Manage` `"Today's sales"` sum **all non-cancelled orders** (including unpaid `NEW`/`CONFIRMED`/`PREPARING`/`READY` orders) using post-VAT `total`, whereas `"7-day revenue"` sums only `DONE` orders (post-VAT `total`), and `"Top dishes"` sums `line.quantity * line.price` (**pre-VAT**) with `TopDishSummary.category` hardcoded to `""`. No date-range selector or CSV/PDF export exists.
10. **Floor Occupancy Masking & Customer Privacy Visibility (`FloorScreen.kt`, `LakemarkViewModel.kt`):**
    - `selectFloorTile` permits creating multiple concurrent active orders on the same room/table, while `FloorScreen.liveOrderFor` shows only `firstOrNull()`.
    - For Customers, `FloorScreen` reads their personal `uiState.orders` (`userId == uid`), causing tables/rooms occupied by other guests to appear `"Free"`.
11. **Manager `Menu` Tab Omitted From Bottom Bar (`LakemarkModels.kt`):**
    - `UserRole.MANAGER` includes `AppTab.MENU` in `allowedTabs` (reachable via Floor tiles or Home area cards), but `bottomBarTabs` excludes `Menu`, making direct cart/takeaway access less discoverable for Managers.
12. **Unbounded Firestore Listeners & Duplicate Orders Listener (`LakemarkRepository.kt`, `LakemarkViewModel.kt`):**
    - Staff `observeOrders`, `observeAllUsers`, and `observeNotifications` listen to entire collections without `.limit()` or date filters (`.take(40)` occurs client-side after downloading all documents). Furthermore, `LakemarkViewModel` opens a *second* `observeOrders` stream inside `observeRealTimeAlerts()` instead of sharing `ordersFlow`.
13. **Client-Side-Only Read Tracking & Unused Preference (`UserPreferencesRepository.kt`):**
    - `lastNotifReadAtMs` clears all unread notification badges locally on one device without per-notification read flags or cross-device sync. `onboardingSeen` preference is defined but unused.
14. **Blocking I/O on FCM Service (`LakemarkFirebaseMessagingService.kt`) & No Staff Self-Password Change:**
    - FCM message handling reads DataStore via `runBlocking` on incoming push threads, and staff have no self-service password change flow (Manager reset only).

### P3 — Low (Metadata Drift, Unused Dependencies & Test Drift)
15. **Schema & Metadata Drift:**
    - `firebase-blueprint.json` omits `username` and `permanentlyActivated` on `StaffUser` and omits `/admins` and `/staff_credentials` collections.
    - `metadata.json` still lists `"name": "Remix Lakemark"` (vs. `"Lakemark"` in `strings.xml`) and retains `"MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"` despite zero AI usage.
16. **Unused Dependencies & Outdated Robolectric Test Assertions:**
    - `androidx.navigation.compose` and `.env.example` (`GEMINI_API_KEY` comment) are unused in the codebase.
    - `LakemarkTests.kt` still asserts `account_switch_local_guest` (line 325) for a customer account even though `ManageTeamSection.kt` replaced customer toggles with `customer_approve_{uid}` (`"Approve"` / `"Activated"`), and calls `AuthScreen` / `ManageScreen` via default lambdas that omit newer staff-auth parameters.
17. **Hardcoded English Copy & Branch/Currency Strings:**
    - Moving strings into `strings.xml` and parameterizing branch (`"Bahir Dar"`) and currency (`"ETB"`) is required before supporting Amharic/multi-property deployments.

---

### Prioritized Next-Step Roadmap
1. **Immediate (Production Readiness on `lakemark-app`):** Enable Email/Password Sign-In in Firebase Console, install `app/build/outputs/apk/release/lakemark-v3.4.7.apk` on Android devices, sign in as Manager (`abebaw4040@gmail.com`) to sync the 5 starter staff accounts, and verify Staff Sign-In (`@reception1`..`@delivery1`), live Firestore sync, and SOT-1 notification sounds. Upgrade to Blaze later when ready to deploy `functions` and initialize `storage`.
2. **Short-Term (Hardening & Workflow Fixes):** Sync `staff_credentials.role` on role changes (or block promoting/demoting username staff to `customer`/`manager`), use Firestore transaction/unique-username doc lock in `createStaffAccount`, display `NEW` orders or clear handoff UX on `KitchenScreen`, and align `0 ETB` validation.
3. **Medium-Term (Scale & Feature Completeness):** Add `.limit()` / time-window queries on `orders` and `notifications`, share `ordersFlow` with `observeRealTimeAlerts()`, implement Cashier settlement/receipt flows, and clarify Today vs. 7d revenue metrics.

---

## 10. Release Delivery Strategy, Signing, Upgrade Compatibility & Data Preservation

### 10.1 Internal Play Store–Free APK Distribution Architecture
- **Current Strategy:** Lakemark is an internal company app distributed via **direct signed APKs** (`REQUEST_INSTALL_PACKAGES` + `FileProvider` + `ApkUpdateManager`) without Google Play Store.
- **Automatic Update Flow:** Managers publish releases to `/app_config/release` + `kind = "app_update"` notifications; existing devices receive an **Update Now** push notification and in-app dialog that downloads, SHA-256/certificate-verifies, and installs the new APK in place without uninstalling.

### 10.2 Version Code Progression & Signing Continuity
- **Package (`applicationId`):** `com.aistudio.applet.kphrxs`
- **Current Baseline:** `versionName = "3.4.4"`, `versionCode = 18` (`minSdk = 24`, `targetSdk = 36`, APK Signature Scheme v2).
- **`versionCode` Monotonicity & Certificate Continuity:** Every internal release must increment `versionCode` monotonically and be signed with the **same** production release keystore (`my-upload-key.jks`, alias `upload`) registered in `lakemark-app` so Android performs seamless in-place upgrades and Google Sign-In continues working.

### 10.3 User Data Preservation Across Upgrades
1. **In-Place Replacement (`Update Now` / `adb install -r`):** Preserves `/data/data/com.aistudio.applet.kphrxs/`—retaining Firebase Auth tokens, DataStore `lakemark_prefs` (`app_language`, `cached_translations_json`, sound, vibration, volume, `preferredRoleKey`, `lastNotifReadAtMs`), `lakemark_push_store.xml` (`deviceId`, push history), and `lakemark_push_dedup.xml`.
2. **Post-Upgrade Startup Sync:** `LakemarkApplication.onCreate()` ensures all three `_v2` notification channels remain configured and deletes legacy v1 channels; `FcmTokenManager.syncSessionPushRegistration()` automatically re-syncs FCM topics and `/users/{uid}/fcm_tokens/{deviceId}`.
3. **Cloud Data Isolation:** All business records (orders, menu, accounts, waiter coverage, brands, translations, saved items, release metadata) reside in Cloud Firestore/Storage on `lakemark-app` and remain untouched by client upgrades.
4. **Full Installation Reference:** Step-by-step ADB upgrade, sideload update, fresh install, and release build reproduction instructions are documented in `/docs/RELEASE_DELIVERY_AND_INSTALL.md`.

