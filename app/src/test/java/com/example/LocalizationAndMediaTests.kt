package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AppNotification
import com.example.data.model.AppReleaseConfig
import com.example.data.model.DefaultMenuCatalog
import com.example.data.model.ManageSection
import com.example.data.model.OrderType
import com.example.data.model.PlaceSelection
import com.example.data.model.SavedMenuItem
import com.example.data.model.StaffUser
import com.example.data.model.TeamSubSection
import com.example.data.model.UserRole
import com.example.localization.AppLanguage
import com.example.localization.RemoteTranslation
import com.example.localization.TranslationCatalog
import com.example.localization.TranslationCategory
import com.example.localization.TranslationStatus
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.LakemarkTopBar
import com.example.ui.screens.ManageScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.SettingsSheet
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.LakemarkTheme
import com.example.ui.viewmodel.LakemarkUiState
import com.example.ui.viewmodel.UiState
import com.example.util.ApkArchiveCheckResult
import com.example.util.ApkUpdateVerifier
import com.example.util.ApkVerifyFailure
import com.example.util.ImageValidationError
import com.example.util.ImageValidationResult
import com.example.util.MediaImageUploader
import com.google.firebase.Timestamp
import androidx.core.content.ContextCompat
import com.example.ui.screens.AuthScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class LocalizationAndMediaTests {

    @get:Rule
    val composeRule = createComposeRule()

    private fun verticalLazyListMatcher(): SemanticsMatcher =
        hasScrollToNodeAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    @Test
    fun `translation catalog resolves English default, Amharic translations, status flags, and English fallback`() {
        val enStrings = TranslationCatalog.buildStrings(AppLanguage.EN, emptyMap())
        assertEquals("Manager", enStrings.roleName(UserRole.MANAGER))
        assertEquals("Kitchen Staff", enStrings.roleName(UserRole.KITCHEN))
        assertEquals("Receptionist", enStrings.roleName(UserRole.RECEPTION))
        assertEquals("Manager Login", enStrings.get("auth_tab_google"))
        assertTrue(enStrings.get("auth_manager_only_error").contains("Manager / Admin"))

        val amStrings = TranslationCatalog.buildStrings(AppLanguage.AM, emptyMap())
        assertEquals("ሌክማርክ", amStrings.get("app_brand_name"))
        assertEquals("አስተዳዳሪ", amStrings.roleName(UserRole.MANAGER))
        assertEquals("ዶሮ ወጥ", amStrings.dishName(DefaultMenuCatalog.items.first()))
        assertEquals("የአስተዳዳሪ መግቢያ", amStrings.get("auth_tab_google"))

        // Missing Amharic override falls back to English cleanly
        val overridesWithBlankAm = mapOf(
            "nav_home" to RemoteTranslation(
                key = "nav_home",
                category = TranslationCategory.NAV.name,
                en = "Home Custom",
                am = "",
                enVersion = 1,
            ),
            "nav_floor" to RemoteTranslation(
                key = "nav_floor",
                category = TranslationCategory.NAV.name,
                en = "Floor",
                am = "ወለል 2",
                enVersion = 0, // older than catalog enVersion=1 => OUTDATED
            ),
            "custom_dynamic_key" to RemoteTranslation(
                key = "custom_dynamic_key",
                category = TranslationCategory.MANAGE.name,
                en = "Dynamic English Fallback",
                am = "",
                enVersion = 1,
            ),
        )
        val mergedAm = TranslationCatalog.buildStrings(AppLanguage.AM, overridesWithBlankAm)
        assertEquals("Dynamic English Fallback", mergedAm.get("custom_dynamic_key"))
        assertEquals("ወለል 2", mergedAm.get("nav_floor"))

        val records = TranslationCatalog.buildRecords(overridesWithBlankAm)
        assertEquals(TranslationStatus.MISSING, records.first { it.key == "nav_home" }.status)
        assertEquals(TranslationStatus.OUTDATED, records.first { it.key == "nav_floor" }.status)
        assertEquals(TranslationStatus.TRANSLATED, records.first { it.key == "nav_menu" }.status)

        // JSON serialization round-trip preserves remote overrides offline
        val json = TranslationCatalog.serializeOverrides(overridesWithBlankAm)
        val decoded = TranslationCatalog.deserializeOverrides(json)
        assertEquals("ወለል 2", decoded["nav_floor"]?.am)
        assertEquals("Home Custom", decoded["nav_home"]?.en)
    }

    @Test
    fun `image upload validation enforces type and 5MB size limit`() {
        assertTrue(MediaImageUploader.validateImageSpec("image/jpeg", 512_000L) is ImageValidationResult.Valid)
        assertTrue(MediaImageUploader.validateImageSpec("image/png", 1_024_000L) is ImageValidationResult.Valid)
        assertTrue(MediaImageUploader.validateImageSpec("image/webp", 2_000_000L) is ImageValidationResult.Valid)

        val badMime = MediaImageUploader.validateImageSpec("application/pdf", 120_000L)
        assertEquals(ImageValidationResult.Invalid(ImageValidationError.UNSUPPORTED_TYPE), badMime)

        val tooBig = MediaImageUploader.validateImageSpec("image/jpeg", MediaImageUploader.MAX_UPLOAD_BYTES + 1L)
        assertEquals(ImageValidationResult.Invalid(ImageValidationError.TOO_LARGE), tooBig)
    }

    @Test
    fun `splash and settings switch EN to AM immediately across UI`() {
        var lang by mutableStateOf(AppLanguage.EN)

        composeRule.setContent {
            val strings = TranslationCatalog.buildStrings(lang, emptyMap())
            LakemarkTheme(language = lang, strings = strings) {
                SplashScreen(
                    onGetStarted = {},
                    onSelectLanguage = { lang = it },
                )
            }
        }

        composeRule.onNodeWithText("LAKEMARK").assertIsDisplayed()
        composeRule.onNodeWithText("Kitchen Management").assertIsDisplayed()

        composeRule.onNodeWithTag("splash_lang_am").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ሌክማርክ").assertIsDisplayed()
        composeRule.onNodeWithText("የወጥ ቤት አስተዳደር").assertIsDisplayed()
    }

    @Test
    fun `settings and topbar display true assigned role even while manager previews another workspace`() {
        val managerUser = StaffUser(
            id = "mgr_1",
            userId = "mgr_1",
            name = "Abebaw",
            email = "abebaw4040@gmail.com",
            role = UserRole.MANAGER.key,
            active = true,
        )
        val previewState = LakemarkUiState(
            currentUser = managerUser,
            assignedRole = UserRole.MANAGER,
            activeRole = UserRole.KITCHEN,
            isAdminUser = true,
            releaseConfig = AppReleaseConfig(
                latestVersionCode = 99L,
                latestVersionName = "3.3.0",
                releaseNotes = "Amharic sync update",
                apkDownloadUrl = "https://example.com/app.apk",
            ),
        )

        var selectedLang: AppLanguage? = null
        var savedName = ""
        var updateDialogOpened = false

        composeRule.setContent {
            LakemarkTheme(language = AppLanguage.EN, strings = TranslationCatalog.defaultEnglishStrings()) {
                androidx.compose.foundation.layout.Column {
                    LakemarkTopBar(
                        unreadCount = 1,
                        activeRole = previewState.activeRole,
                        onNotificationsClick = {},
                        onSettingsClick = {},
                        currentUser = previewState.currentUser,
                        assignedRole = previewState.assignedRole,
                    )
                    SettingsSheet(
                        uiState = previewState,
                        hasNotificationPermission = true,
                        onClose = {},
                        onSignOut = {},
                        onSwitchAdminRole = {},
                        onOpenMyBrand = {},
                        onToggleSound = {},
                        onToggleVibration = {},
                        onVolumeChange = {},
                        onRequestNotificationPermission = {},
                        onSelectLanguage = { selectedLang = it },
                        onUpdateProfileName = { name, _ -> savedName = name },
                        onOpenUpdateDialog = { updateDialogOpened = true },
                    )
                }
            }
        }

        // Assigned designation badge stays Manager; preview badge clarifies Kitchen Staff view
        composeRule.onNodeWithTag("assigned_role_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("preview_role_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_photo_edit_button").assertIsDisplayed()

        // Language switch & profile name edit
        composeRule.onNodeWithTag("settings_lang_am").performScrollTo().performClick()
        assertEquals(AppLanguage.AM, selectedLang)

        composeRule.onNodeWithTag("profile_name_input").performTextReplacement("Betty A.")
        composeRule.onNodeWithTag("profile_name_save_button").performClick()
        assertEquals("Betty A.", savedName)

        composeRule.onNodeWithTag("settings_update_button").performScrollTo().performClick()
        assertTrue(updateDialogOpened)
    }

    @Test
    fun `manage language table filters searches and opens editor dialog`() {
        var editedKey = ""
        var editedAm = ""
        val state = LakemarkUiState(
            currentUser = StaffUser(id = "m1", userId = "m1", name = "Manager", role = "manager", active = true),
            assignedRole = UserRole.MANAGER,
            activeRole = UserRole.MANAGER,
            isAdminUser = true,
            manageSection = ManageSection.LANGUAGE,
            menuItemsState = UiState.Success(DefaultMenuCatalog.items),
            ordersState = UiState.Success(emptyList()),
        )

        composeRule.setContent {
            LakemarkTheme {
                ManageScreen(
                    uiState = state,
                    onSendBroadcast = { _, _, _ -> },
                    onOpenBrandEditor = {},
                    onToggleUserActive = {},
                    onUpdateStaffRole = { _, _ -> },
                    onSaveTranslation = { key, _, _, am, _, done, _ ->
                        editedKey = key
                        editedAm = am
                        done()
                    },
                )
            }
        }

        composeRule.onNodeWithTag("language_management_header").assertIsDisplayed()
        composeRule.onNodeWithTag("language_search_input").performTextInput("Doro Wat")
        composeRule.waitForIdle()
        composeRule.onNode(verticalLazyListMatcher()).performScrollToNode(hasTestTag("lang_row_dish_name_m0"))
        composeRule.onNodeWithTag("lang_row_dish_name_m0").assertIsDisplayed()
        composeRule.onNodeWithTag("lang_row_dish_name_m0").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("translation_editor_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("translation_am_input").performTextReplacement("ዶሮ ወጥ (ልዩ)")
        composeRule.onNodeWithTag("translation_save_button").performClick()
        assertEquals("dish_name_m0", editedKey)
        assertEquals("ዶሮ ወጥ (ልዩ)", editedAm)
    }

    @Test
    fun `saved foods, menu bookmarks, newly registered users, food photo controls, and release update render`() {
        val sampleItem = DefaultMenuCatalog.items.first()
        var bookmarkToggledId = ""
        var approvedUid = ""
        var pickedFoodId = ""
        var updateClicked = false

        val pendingGuest = StaffUser(
            id = "guest_new",
            userId = "guest_new",
            name = "Abebe Kebede",
            email = "abebe@example.com",
            role = UserRole.CUSTOMER.key,
            active = false,
            permanentlyActivated = false,
            createdAt = Timestamp(Date(1760000000000L)),
        )
        val approvedGuest = StaffUser(
            id = "guest_perm",
            userId = "guest_perm",
            name = "Selam T.",
            email = "selam@example.com",
            role = UserRole.CUSTOMER.key,
            active = true,
            permanentlyActivated = true,
            createdAt = Timestamp(Date(1759000000000L)),
        )

        var mode by mutableStateOf("menu")
        val baseState = LakemarkUiState(
            currentUser = StaffUser(id = "m1", userId = "m1", name = "Manager", role = "manager", active = true),
            assignedRole = UserRole.MANAGER,
            activeRole = UserRole.MANAGER,
            isAdminUser = true,
            selectedPlace = PlaceSelection(OrderType.DINE, "Table 3", "Table 3"),
            menuItemsState = UiState.Success(DefaultMenuCatalog.items),
            ordersState = UiState.Success(emptyList()),
            savedItems = mapOf(
                sampleItem.id to SavedMenuItem(
                    id = sampleItem.id,
                    menuItemId = sampleItem.id,
                    name = sampleItem.name,
                    category = sampleItem.category,
                    price = sampleItem.price,
                    imageUrl = sampleItem.imageUrl,
                    savedAt = Timestamp(Date()),
                ),
            ),
            staffUsers = listOf(pendingGuest, approvedGuest),
            manageSection = ManageSection.TEAM,
            teamSubSection = TeamSubSection.NEW_REGISTERED,
            notifications = listOf(
                AppNotification(
                    id = "notif_upd_1",
                    senderRole = UserRole.MANAGER.key,
                    senderBrandName = "Lakemark",
                    message = "New release v3.2.0 available",
                    kind = "app_update",
                    apkUrl = "https://example.com/lakemark.apk",
                    targetRoles = listOf("all"),
                ),
            ),
        )

        composeRule.setContent {
            LakemarkTheme {
                when (mode) {
                    "menu" -> MenuScreen(
                        uiState = baseState,
                        onStartArea = {},
                        onChangePlace = {},
                        onSelectCategory = {},
                        onSearchChange = {},
                        onAddToCart = {},
                        onDecrementCart = {},
                        onToggleSaved = { bookmarkToggledId = it.id },
                        onOpenSaved = { mode = "saved" },
                    )
                    "saved" -> SavedScreen(
                        savedItems = baseState.savedItems,
                        liveMenuItems = DefaultMenuCatalog.items,
                        activeRole = UserRole.CUSTOMER,
                        onAddToCart = {},
                        onRemoveSaved = { bookmarkToggledId = it.id },
                        onOpenMenu = { mode = "menu" },
                    )
                    "new_reg" -> ManageScreen(
                        uiState = baseState,
                        onSendBroadcast = { _, _, _ -> },
                        onOpenBrandEditor = {},
                        onToggleUserActive = {},
                        onApproveCustomer = { approvedUid = it.userId },
                        onUpdateStaffRole = { _, _ -> },
                    )
                    "manage_menu" -> ManageScreen(
                        uiState = baseState.copy(manageSection = ManageSection.MENU),
                        onUploadFoodImage = { item, _ -> pickedFoodId = item.id },
                        onSendBroadcast = { _, _, _ -> },
                        onOpenBrandEditor = {},
                        onToggleUserActive = {},
                        onUpdateStaffRole = { _, _ -> },
                    )
                    "notifs" -> NotificationsSheet(
                        uiState = baseState,
                        onClose = {},
                        onNotificationClick = { if (it.kind == "app_update") updateClicked = true },
                    )
                }
            }
        }

        // Menu bookmark + Saved shortcut
        composeRule.onNode(verticalLazyListMatcher()).performScrollToNode(hasTestTag("bookmark_item_${sampleItem.id}"))
        composeRule.onNodeWithTag("bookmark_item_${sampleItem.id}").performClick()
        assertEquals(sampleItem.id, bookmarkToggledId)
        composeRule.onNode(verticalLazyListMatcher()).performScrollToNode(hasTestTag("menu_open_saved_button"))
        composeRule.onNodeWithTag("menu_open_saved_button").performClick()
        composeRule.waitForIdle()

        // Saved screen
        composeRule.onNodeWithTag("saved_item_${sampleItem.id}").assertIsDisplayed()
        composeRule.onAllNodesWithText("No saved dishes yet").let { }

        // Newly Registered users + Manager approval
        composeRule.runOnUiThread { mode = "new_reg" }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("newly_registered_header").assertIsDisplayed()
        composeRule.onNode(verticalLazyListMatcher()).performScrollToNode(hasTestTag("new_reg_approve_${pendingGuest.userId}"))
        composeRule.onNodeWithTag("new_reg_approve_${pendingGuest.userId}").performClick()
        assertEquals(pendingGuest.userId, approvedUid)

        // Manage -> Menu food photo control
        composeRule.runOnUiThread { mode = "manage_menu" }
        composeRule.waitForIdle()
        composeRule.onNode(verticalLazyListMatcher()).performScrollToNode(hasTestTag("food_photo_upload_${sampleItem.id}"))
        composeRule.onNodeWithTag("food_photo_upload_${sampleItem.id}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("image_source_picker_dialog").assertIsDisplayed()
        assertTrue(pickedFoodId.isEmpty()) // source picker opens cleanly before gallery/camera URI selection

        // Notifications Update CTA
        composeRule.runOnUiThread { mode = "notifs" }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_update_notif_upd_1").assertIsDisplayed()
        composeRule.onNodeWithText("Update Now").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_update_notif_upd_1").performClick()
        assertTrue(updateClicked)
    }

    @Test
    fun `apk update verifier validates https urls checksums and archive compatibility`() {
        assertTrue(ApkUpdateVerifier.isValidHttpsUrl("https://firebasestorage.googleapis.com/v0/b/app/lakemark-release-v14.apk"))
        assertFalse(ApkUpdateVerifier.isValidHttpsUrl("http://example.com/lakemark-release-v14.apk"))
        assertFalse(ApkUpdateVerifier.isValidHttpsUrl("file:///sdcard/lakemark.apk"))
        assertFalse(ApkUpdateVerifier.isValidHttpsUrl("   "))

        val validSha = "53cd3777e0c4bc73ec8985e409cbe8f689eca8a06ec4fbe51e6b191bf029b831"
        val formattedSha = "53:CD:37:77:E0:C4:BC:73:EC:89:85:E4:09:CB:E8:F6:89:EC:A8:A0:6E:C4:FB:E5:1E:6B:19:1B:F0:29:B8:31"
        val wrongSha = "0000000000000000000000000000000000000000000000000000000000000000"

        assertTrue(ApkUpdateVerifier.verifyChecksum(validSha, ""))
        assertTrue(ApkUpdateVerifier.verifyChecksum(validSha, formattedSha))
        assertFalse(ApkUpdateVerifier.verifyChecksum(validSha, wrongSha))
        assertFalse(ApkUpdateVerifier.verifyChecksum(validSha, "not-hex"))

        val expectedPkg = "com.aistudio.applet.kphrxs"
        val certSet = setOf(validSha)

        assertEquals(
            ApkArchiveCheckResult.Invalid(ApkVerifyFailure.INVALID_PACKAGE),
            ApkUpdateVerifier.verifyArchiveCompatibility(null, 14L, certSet, expectedPkg, 14L, certSet),
        )
        assertEquals(
            ApkArchiveCheckResult.Invalid(ApkVerifyFailure.PACKAGE_MISMATCH),
            ApkUpdateVerifier.verifyArchiveCompatibility("com.other.app", 14L, certSet, expectedPkg, 14L, certSet),
        )
        assertEquals(
            ApkArchiveCheckResult.Invalid(ApkVerifyFailure.VERSION_DOWNGRADE),
            ApkUpdateVerifier.verifyArchiveCompatibility(expectedPkg, 13L, certSet, expectedPkg, 14L, certSet),
        )
        assertEquals(
            ApkArchiveCheckResult.Invalid(ApkVerifyFailure.SIGNING_MISMATCH),
            ApkUpdateVerifier.verifyArchiveCompatibility(expectedPkg, 14L, setOf(wrongSha), expectedPkg, 14L, certSet),
        )
        assertEquals(
            ApkArchiveCheckResult.Valid,
            ApkUpdateVerifier.verifyArchiveCompatibility(expectedPkg, 14L, certSet, expectedPkg, 14L, certSet),
        )
    }

    @Test
    fun `app update dialog shows Update Now and rejects insecure http urls`() {
        val insecureRelease = AppReleaseConfig(
            latestVersionCode = 99L,
            latestVersionName = "3.4.0",
            releaseNotes = "Production fixes and v14 update delivery",
            apkDownloadUrl = "http://insecure.example.com/lakemark-release-v14.apk",
            apkSha256 = "",
            mandatory = false,
        )

        composeRule.setContent {
            LakemarkTheme {
                AppUpdateAvailableDialog(
                    releaseConfig = insecureRelease,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("app_update_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Production fixes and v14 update delivery").assertIsDisplayed()
        composeRule.onNodeWithTag("app_update_download_button").assertIsDisplayed()
        composeRule.onNodeWithText("Update Now").assertIsDisplayed()

        composeRule.onNodeWithTag("app_update_download_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("app_update_error_text").assertIsDisplayed()
        composeRule.onNodeWithText("Update download requires a valid HTTPS link.").assertIsDisplayed()
    }

    @Test
    fun `settings Update Now CTA and manage alerts sha256 release publisher work cleanly`() {
        var publishedSha = ""
        val sampleSha = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        var showSettings by mutableStateOf(true)
        val mgr = StaffUser(id = "m1", userId = "m1", name = "Manager", role = "manager", active = true)
        val state = LakemarkUiState(
            currentUser = mgr,
            assignedRole = UserRole.MANAGER,
            activeRole = UserRole.MANAGER,
            isAdminUser = true,
            manageSection = ManageSection.ALERTS,
            releaseConfig = AppReleaseConfig(
                latestVersionCode = 99L,
                latestVersionName = "3.4.0",
                releaseNotes = "v14 security and staff auth fix",
                apkDownloadUrl = "https://example.com/lakemark-release-v14.apk",
            ),
            menuItemsState = UiState.Success(DefaultMenuCatalog.items),
            ordersState = UiState.Success(emptyList()),
        )

        composeRule.setContent {
            LakemarkTheme {
                if (showSettings) {
                    SettingsSheet(
                        uiState = state,
                        hasNotificationPermission = true,
                        onClose = {},
                        onSignOut = {},
                        onSwitchAdminRole = {},
                        onOpenMyBrand = {},
                        onToggleSound = {},
                        onToggleVibration = {},
                        onVolumeChange = {},
                        onRequestNotificationPermission = {},
                        onOpenUpdateDialog = {},
                    )
                } else {
                    ManageScreen(
                        uiState = state,
                        onSendBroadcast = { _, _, _ -> },
                        onOpenBrandEditor = {},
                        onToggleUserActive = {},
                        onUpdateStaffRole = { _, _ -> },
                        onPublishReleaseUpdate = { _, _, _, _, _, sha, done ->
                            publishedSha = sha
                            done()
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("settings_update_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Update Now").assertIsDisplayed()

        composeRule.runOnUiThread { showSettings = false }
        composeRule.waitForIdle()
        composeRule.onNode(verticalLazyListMatcher()).performScrollToNode(hasTestTag("manage_release_update_panel"))
        composeRule.onNodeWithTag("release_sha256_input").performScrollTo().performTextReplacement(sampleSha)
        composeRule.onNodeWithTag("publish_release_update_button").performScrollTo().performClick()
        assertEquals(sampleSha, publishedSha)
    }

    @Test
    fun `alert audio usage foreground tracker settings test sound and staff auth diagnostics work cleanly`() {
        assertEquals(
            android.media.AudioAttributes.USAGE_MEDIA,
            com.example.util.AlertAudioAndNotificationManager.selectAlertAudioUsage(
                isForeground = true,
                mediaVolume = 8,
                notificationVolume = 0,
                ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE,
            ),
        )
        assertEquals(
            android.media.AudioAttributes.USAGE_NOTIFICATION,
            com.example.util.AlertAudioAndNotificationManager.selectAlertAudioUsage(
                isForeground = false,
                mediaVolume = 8,
                notificationVolume = 6,
                ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL,
            ),
        )

        com.example.fcm.AppForegroundTracker.resetForTest()
        com.example.fcm.AppForegroundTracker.onActivityStarted()
        com.example.fcm.AppForegroundTracker.onActivityResumed()
        assertTrue(com.example.fcm.AppForegroundTracker.isAppInForeground())
        com.example.fcm.AppForegroundTracker.onActivityPaused()
        assertTrue(com.example.fcm.AppForegroundTracker.isAppInForeground())
        com.example.fcm.AppForegroundTracker.onActivityStopped()
        assertFalse(com.example.fcm.AppForegroundTracker.isAppInForeground())

        assertEquals(
            "kitchen1@staff.lakemark.local",
            com.example.data.auth.StaffAuthClient.staffEmailFor(" @Kitchen1 "),
        )
        assertTrue(
            com.example.data.auth.StaffAuthClient.mapFirebaseAuthException(
                IllegalStateException("PASSWORD_LOGIN_DISABLED"),
            ).contains("Enable Email/Password sign-in", ignoreCase = true),
        )

        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val alertMgr = com.example.util.AlertAudioAndNotificationManager(ctx)
        val status = alertMgr.getNotificationSystemStatus()
        assertTrue(status.allChannelsSoundEnabled)

        var testedSound = false
        var openedNotifSettings = false
        val state = LakemarkUiState(
            currentUser = StaffUser(id = "m1", userId = "m1", name = "Manager", role = "manager", active = true),
            assignedRole = UserRole.MANAGER,
            activeRole = UserRole.MANAGER,
            isAdminUser = true,
        )

        composeRule.setContent {
            LakemarkTheme {
                SettingsSheet(
                    uiState = state,
                    hasNotificationPermission = true,
                    notificationSystemStatus = status.copy(deviceMediaMuted = true),
                    onClose = {},
                    onSignOut = {},
                    onSwitchAdminRole = {},
                    onOpenMyBrand = {},
                    onToggleSound = {},
                    onToggleVibration = {},
                    onVolumeChange = {},
                    onTestAlertSound = { testedSound = true },
                    onRequestNotificationPermission = {},
                    onOpenNotificationSettings = { openedNotifSettings = true },
                )
            }
        }

        composeRule.onNodeWithTag("settings_muted_volume_warning").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_test_sound_button").performScrollTo().performClick()
        assertTrue(testedSound)
        composeRule.onNodeWithTag("settings_open_system_notif_button").performScrollTo().performClick()
        assertTrue(openedNotifSettings)
    }

    @Test
    fun `manager can open edit staff dialog and update name username role active and password`() {
        var editedName = ""
        var editedUsername = ""
        var editedRole = UserRole.CUSTOMER
        var editedActive = true
        var editedPassword = ""

        val mgr = StaffUser(id = "m1", userId = "m1", name = "Manager", role = "manager", active = true)
        val staff = StaffUser(
            id = "k1",
            userId = "k1",
            name = "Abe",
            username = "abe",
            role = "kitchen",
            active = true,
        )
        val state = LakemarkUiState(
            currentUser = mgr,
            assignedRole = UserRole.MANAGER,
            activeRole = UserRole.MANAGER,
            isAdminUser = true,
            manageSection = ManageSection.TEAM,
            teamSubSection = TeamSubSection.ACCOUNTS,
            staffUsers = listOf(mgr, staff),
            menuItemsState = UiState.Success(DefaultMenuCatalog.items),
            ordersState = UiState.Success(emptyList()),
        )

        composeRule.setContent {
            LakemarkTheme {
                ManageScreen(
                    uiState = state,
                    onSendBroadcast = { _, _, _ -> },
                    onOpenBrandEditor = {},
                    onToggleUserActive = {},
                    onUpdateStaffRole = { _, _ -> },
                    onEditStaffAccount = { _, name, username, role, active, password, done, _ ->
                        editedName = name
                        editedUsername = username
                        editedRole = role
                        editedActive = active
                        editedPassword = password
                        done()
                    },
                )
            }
        }

        composeRule.onNode(verticalLazyListMatcher()).performScrollToNode(hasTestTag("edit_staff_k1"))
        composeRule.onNodeWithTag("edit_staff_k1").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edit_staff_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_staff_name_input").performTextReplacement("Abe Chef")
        composeRule.onNodeWithTag("edit_staff_username_input").performTextReplacement("abe_waiter")
        composeRule.onNodeWithTag("edit_staff_role_waiter").performClick()
        composeRule.onNodeWithTag("edit_staff_active_switch").performClick()
        composeRule.onNodeWithTag("edit_staff_password_input").performTextReplacement("Pass#2026")
        composeRule.onNodeWithTag("edit_staff_confirm_button").performClick()
        composeRule.waitForIdle()

        assertEquals("Abe Chef", editedName)
        assertEquals("abe_waiter", editedUsername)
        assertEquals(UserRole.WAITER, editedRole)
        assertFalse(editedActive)
        assertEquals("Pass#2026", editedPassword)
    }

    @Test
    fun `lakemark icon kit drawables mipmaps brand colors and splash theme resolve cleanly`() {
        val context = RuntimeEnvironment.getApplication()
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_stat_lakemark))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.lakemark_badge))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.lakemark_emblem))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.lakemark_logo_horizontal))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.splash_icon))
        assertEquals(0xFF310645.toInt(), ContextCompat.getColor(context, R.color.lakemark_purple))
        assertEquals(0xFFFDF6E9.toInt(), ContextCompat.getColor(context, R.color.lakemark_cream))
        assertTrue(R.style.Theme_Lakemark_Starting != 0)

        composeRule.setContent {
            LakemarkTheme {
                AuthScreen(
                    isLoading = false,
                    errorMessage = null,
                    onGoogleSignIn = {},
                )
            }
        }
        composeRule.onNodeWithText("Welcome to Lakemark").assertIsDisplayed()
    }

    @Test
    fun `staff password hasher matches node pbkdf2 vector, default roster covers all 5 roles, and unavailable detection classifies errors`() {
        val salt = "00112233445566778899aabbccddeeff"
        val hashed = com.example.data.auth.StaffPasswordHasher.hashPassword("Lakemark#101", salt)
        assertEquals(salt, hashed.saltHex)
        assertEquals(com.example.data.auth.StaffPasswordHasher.ALGO_PBKDF2_SHA256, hashed.hashAlgo)
        assertEquals(
            "44756a0e0ead2e9be05ae5d2132fa35d4c9524fb1acff95d5706438a9523f6bf95f32be991b37122dea6ba7ac9a0b75a5f7fd2d4f06838754f566328c278d226",
            hashed.hashHex,
        )

        val roster = com.example.data.auth.DefaultStaffRoster.accounts
        assertEquals(5, roster.size)
        assertEquals(
            setOf(
                UserRole.RECEPTION,
                UserRole.KITCHEN,
                UserRole.WAITER,
                UserRole.CASHIER,
                UserRole.DELIVERY,
            ),
            roster.map { it.role }.toSet(),
        )
        assertTrue(roster.all { it.initialPassword.length >= 6 && it.username.matches(Regex("^[a-z0-9._-]{3,32}$")) })

        assertEquals(
            "reception1@staff.lakemark.local",
            com.example.data.auth.StaffAuthClient.staffEmailFor("reception1"),
        )
        assertTrue(
            com.example.data.auth.StaffAuthClient.mapFirebaseAuthException(
                IllegalStateException("That username is already in use."),
            ).contains("already in use"),
        )
    }
}
