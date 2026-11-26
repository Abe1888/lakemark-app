package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.StaffAuthClient
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.authStateFlow
import com.example.data.auth.onGoogleSignInClicked
import com.example.data.auth.signOut
import com.example.data.model.AppTab
import com.example.data.model.OrderStatus
import com.example.data.model.UserRole
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.LakemarkRepository
import com.example.fcm.FcmTokenManager
import com.example.fcm.InAppPushCoordinator
import com.example.fcm.NotificationDeduplicator
import com.example.fcm.PushDeepLink
import com.example.fcm.PushHistoryStore
import com.example.localization.AppLanguage
import com.example.localization.LocalAppStrings
import com.example.localization.TranslationCatalog
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.BrandedToastBanner
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.KitchenOrderAlertDialog
import com.example.ui.components.LakemarkFlameEmblem
import com.example.ui.components.LakemarkTopBar
import com.example.ui.components.PickupReadyAlertDialog
import com.example.ui.components.WaiterAssignDialog
import com.example.ui.components.formatEtb
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BrandEditorSheet
import com.example.ui.screens.CartCheckoutSheet
import com.example.ui.screens.FloorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KitchenScreen
import com.example.ui.screens.ManageScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.SettingsSheet
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CreamBg
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.LakemarkTheme
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.deriveBrandPalette
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.AuthSession
import com.example.ui.viewmodel.LakemarkViewModel
import com.example.util.AlertAudioAndNotificationManager
import com.example.util.AlertSoundKind
import com.example.util.MediaImageUploader
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
        )
        handleNotificationIntent(intent)
        setContent {
            LakemarkRootApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val extraTab = intent.getStringExtra(PushDeepLink.EXTRA_TAB)
            ?: intent.getStringExtra("targetTab")
            ?: intent.getStringExtra("tab")
        val extraOrderId = intent.getStringExtra(PushDeepLink.EXTRA_ORDER_ID)
            ?: intent.getStringExtra("orderId")
        val extraNotifId = intent.getStringExtra(PushDeepLink.EXTRA_NOTIF_ID)
            ?: intent.getStringExtra("notifId")
        val extraKind = intent.getStringExtra(PushDeepLink.EXTRA_KIND)
            ?: intent.getStringExtra("kind")
        val extraOrderStatus = intent.getStringExtra(PushDeepLink.EXTRA_ORDER_STATUS)
            ?: intent.getStringExtra("orderStatus")
            ?: intent.getStringExtra("status")
        val isExternalBrowsable = intent.action == Intent.ACTION_VIEW && intent.data != null
        val extraApkUrl = if (isExternalBrowsable) {
            null
        } else {
            intent.getStringExtra(PushDeepLink.EXTRA_APK_URL)
                ?: intent.getStringExtra("apkUrl")
        }

        val extraOpenSheet: Boolean? = when {
            intent.hasExtra(PushDeepLink.EXTRA_OPEN_SHEET) ->
                intent.getBooleanExtra(PushDeepLink.EXTRA_OPEN_SHEET, false)
            intent.hasExtra("openNotificationsSheet") -> {
                val raw = intent.getStringExtra("openNotificationsSheet")
                if (raw != null) {
                    raw.equals("true", ignoreCase = true) || raw == "1"
                } else {
                    intent.getBooleanExtra("openNotificationsSheet", false)
                }
            }
            else -> null
        }

        val parsedDeepLink = PushDeepLink.fromQueryAndExtras(
            uriString = intent.dataString,
            extraTab = extraTab,
            extraOrderId = extraOrderId,
            extraNotifId = extraNotifId,
            extraOpenSheet = extraOpenSheet,
            extraKind = extraKind,
            extraOrderStatus = extraOrderStatus,
            extraApkUrl = extraApkUrl,
        )
        val deepLink = if (isExternalBrowsable && parsedDeepLink != null) {
            parsedDeepLink.copy(apkUrl = "")
        } else {
            parsedDeepLink
        }
        if (deepLink != null) {
            InAppPushCoordinator.enqueueDeepLink(deepLink)
        }
    }
}

@Composable
fun LakemarkRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val repository = remember(context) { LakemarkRepository(context.applicationContext) }
    val prefsRepo = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val alertManager = remember(context) { AlertAudioAndNotificationManager(context.applicationContext) }
    val pushHistoryStore = remember(context) { PushHistoryStore.getInstance(context.applicationContext) }
    val deduplicator = remember(context) { NotificationDeduplicator.getInstance(context.applicationContext) }
    val fcmTokenManager = remember(context) { FcmTokenManager(context.applicationContext, repository) }
    val staffAuthClient = remember { StaffAuthClient() }
    val mediaUploader = remember(context) { MediaImageUploader(context.applicationContext) }

    val rootPrefs by prefsRepo.preferences.collectAsStateWithLifecycle(
        initialValue = com.example.data.preferences.UserAlertPreferences(
            languageCode = prefsRepo.getQuickLanguageCode(),
        ),
    )
    val preAuthLang = AppLanguage.fromCode(rootPrefs.languageCode)
    val preAuthStrings = remember(preAuthLang, rootPrefs.cachedTranslationsJson) {
        TranslationCatalog.buildStrings(
            preAuthLang,
            TranslationCatalog.deserializeOverrides(rootPrefs.cachedTranslationsJson),
        )
    }

    val authUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var showAuthScreen by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isSigningIn = false
                    authError = null
                },
                onUnauthenticated = {
                    isSigningIn = false
                },
                scope = scope,
            )
        }
    }

    val activeSession: AuthSession? = authUser?.let { AuthSession(it) }

    if (activeSession == null) {
        LakemarkTheme(language = preAuthLang, strings = preAuthStrings) {
            if (showAuthScreen) {
                BackHandler { showAuthScreen = false }
                AuthScreen(
                    isLoading = isSigningIn,
                    errorMessage = authError,
                    onGoogleSignIn = {
                        isSigningIn = true
                        authError = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isSigningIn = false
                                authError = null
                            },
                            onAuthError = { msg ->
                                isSigningIn = false
                                authError = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isSigningIn = false
                            },
                        )
                    },
                    onStaffSignIn = { username, password ->
                        isSigningIn = true
                        authError = null
                        scope.launch {
                            staffAuthClient.signInStaff(username, password)
                                .onSuccess {
                                    isSigningIn = false
                                    authError = null
                                }
                                .onFailure { err ->
                                    isSigningIn = false
                                    authError = err.localizedMessage ?: preAuthStrings.get(
                                        "err_staff_signin_failed",
                                        "Staff sign-in failed",
                                    )
                                }
                        }
                    },
                    onClearError = { authError = null },
                    onBackToSplash = { showAuthScreen = false },
                )
            } else {
                SplashScreen(
                    onGetStarted = { showAuthScreen = true },
                    onSelectLanguage = { lang ->
                        scope.launch { prefsRepo.setLanguageCode(lang.code) }
                    },
                )
            }
        }
    } else {
        key(activeSession.sessionKey) {
            val viewModel: LakemarkViewModel = viewModel(
                factory = remember(activeSession.sessionKey) {
                    LakemarkViewModelFactory(
                        repository = repository,
                        prefsRepo = prefsRepo,
                        alertManager = alertManager,
                        pushHistoryStore = pushHistoryStore,
                        deduplicator = deduplicator,
                        fcmTokenManager = fcmTokenManager,
                        session = activeSession,
                        mediaUploader = mediaUploader,
                    )
                },
            )
            LakemarkMainScaffold(
                viewModel = viewModel,
                alertManager = alertManager,
                onSignOutClick = {
                    val fbUid = Firebase.auth.currentUser?.uid
                    scope.launch {
                        fcmTokenManager.unregisterOnSignOut(expectedFirebaseUid = fbUid)
                        signOut(
                            credentialManager = credentialManager,
                            scope = scope,
                            onSignOutComplete = {
                                showAuthScreen = true
                            },
                        )
                    }
                },
                onGoogleSetupRejected = { msg ->
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = {
                            isSigningIn = false
                            showAuthScreen = true
                            authError = msg
                        },
                    )
                },
            )
        }
    }
}

private class LakemarkViewModelFactory(
    private val repository: LakemarkRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val alertManager: AlertAudioAndNotificationManager,
    private val pushHistoryStore: PushHistoryStore,
    private val deduplicator: NotificationDeduplicator,
    private val fcmTokenManager: FcmTokenManager,
    private val session: AuthSession,
    private val mediaUploader: MediaImageUploader,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LakemarkViewModel(
            repository = repository,
            prefsRepo = prefsRepo,
            alertManager = alertManager,
            pushHistoryStore = pushHistoryStore,
            deduplicator = deduplicator,
            fcmTokenManager = fcmTokenManager,
            session = session,
            mediaUploader = mediaUploader,
        ) as T
    }
}

@Composable
private fun LakemarkMainScaffold(
    viewModel: LakemarkViewModel,
    alertManager: AlertAudioAndNotificationManager,
    onSignOutClick: () -> Unit,
    onGoogleSetupRejected: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scaffoldContext = LocalContext.current
    var notifSystemStatus by remember { mutableStateOf(alertManager.getNotificationSystemStatus()) }
    var hasNotifPermission by remember { mutableStateOf(notifSystemStatus.hasPermission) }
    var promptedNotifPermission by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.rejectedGoogleSetupMessage) {
        val msg = uiState.rejectedGoogleSetupMessage
        if (!msg.isNullOrBlank()) {
            onGoogleSetupRejected(msg)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notifSystemStatus = alertManager.getNotificationSystemStatus()
        hasNotifPermission = granted || notifSystemStatus.hasPermission
        viewModel.refreshFcmRegistration()
    }

    var wasBackgrounded by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, alertManager, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    wasBackgrounded = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    val freshStatus = alertManager.getNotificationSystemStatus()
                    notifSystemStatus = freshStatus
                    val nowAllowed = freshStatus.hasPermission
                    if (nowAllowed != hasNotifPermission) {
                        hasNotifPermission = nowAllowed
                        viewModel.refreshFcmRegistration()
                    }
                    if (wasBackgrounded) {
                        wasBackgrounded = false
                        viewModel.onAppReopened()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.activeRole) {
        if (!hasNotifPermission && !promptedNotifPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            promptedNotifPermission = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LakemarkTheme(
        brandPalette = uiState.activePalette,
        language = uiState.language,
        strings = uiState.strings,
    ) {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()

        if (uiState.isVerifyingAccount || !uiState.rejectedGoogleSetupMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CreamBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = palette.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.widthIn(max = 420.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LakemarkFlameEmblem(badgeSize = 58.dp)
                        CircularProgressIndicator(
                            color = DeepPlum,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = s.get("auth_verifying_status", "Verifying account status…"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = DeepPlum,
                        )
                    }
                }
            }
            return@LakemarkTheme
        }

        if (uiState.accountDisabled) {
            val isPendingCustomer = uiState.currentUser?.userRole == UserRole.CUSTOMER
            val accountIdentifier = uiState.currentUser?.email?.takeIf { it.isNotBlank() }
                ?: uiState.currentUser?.name.orEmpty()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CreamBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = palette.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.widthIn(max = 420.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LakemarkFlameEmblem(badgeSize = 64.dp)
                        Text(
                            text = if (isPendingCustomer) {
                                s.get("auth_approval_pending", "Approval Pending")
                            } else {
                                s.get("auth_account_disabled", "Account Disabled")
                            },
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = DeepPlum,
                        )
                        if (accountIdentifier.isNotBlank()) {
                            Text(
                                text = accountIdentifier,
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = palette.plum,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            text = if (isPendingCustomer) {
                                s.get(
                                    "auth_pending_desc",
                                    "Customer accounts remain disabled by default until a Lakemark Manager explicitly approves and permanently activates your account. Access unlocks automatically once approved.",
                                )
                            } else {
                                s.get(
                                    "auth_disabled_desc",
                                    "Your staff account is currently disabled by management. Please sign out or contact a Lakemark manager.",
                                )
                            },
                            fontFamily = font,
                            fontSize = 13.5.sp,
                            color = palette.mut,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onSignOutClick,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusCancelledBg,
                                contentColor = StatusCancelledText,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("disabled_sign_out_button"),
                        ) {
                            Text(s.get("btn_sign_out", "Sign out"), fontFamily = font, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            return@LakemarkTheme
        }

        BackHandler(enabled = uiState.currentTab != AppTab.HOME) {
            viewModel.selectTab(AppTab.HOME)
        }

        val bottomBarTabs = uiState.activeRole.bottomBarTabs
        val newOrdersCount = uiState.orders.count { it.statusEnum == OrderStatus.NEW }
        val confirmedKitchenCount = uiState.orders.count { it.statusEnum == OrderStatus.CONFIRMED }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
        ) {
            Scaffold(
                containerColor = palette.bg,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    LakemarkTopBar(
                        unreadCount = uiState.unreadNotificationCount,
                        activeRole = uiState.activeRole,
                        onNotificationsClick = viewModel::openNotificationsSheet,
                        onSettingsClick = viewModel::openSettingsSheet,
                        currentUser = uiState.currentUser,
                        assignedRole = uiState.assignedRole,
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                    ) {
                        when (uiState.currentTab) {
                            AppTab.HOME -> HomeScreen(
                                uiState = uiState,
                                onNavigateTab = viewModel::selectTab,
                                onOpenArea = viewModel::startFromAreaCard,
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppTab.FLOOR -> FloorScreen(
                                uiState = uiState,
                                onSelectZone = viewModel::selectZone,
                                onSelectPlace = viewModel::selectFloorTile,
                                onChangePlaceClick = { viewModel.selectTab(AppTab.FLOOR) },
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppTab.MENU -> MenuScreen(
                                uiState = uiState,
                                onStartArea = viewModel::startFromAreaCard,
                                onChangePlace = { viewModel.selectTab(AppTab.FLOOR) },
                                onSelectCategory = viewModel::selectCategory,
                                onSearchChange = viewModel::updateSearchQuery,
                                onAddToCart = viewModel::addToCartAndOpenOrder,
                                onDecrementCart = viewModel::decrementCartItem,
                                onToggleSaved = viewModel::toggleSavedItem,
                                onOpenSaved = { viewModel.selectTab(AppTab.SAVED) },
                                onOpenCart = viewModel::openCartSheet,
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppTab.SAVED -> SavedScreen(
                                savedItems = uiState.savedItems,
                                liveMenuItems = uiState.menuItems,
                                activeRole = uiState.activeRole,
                                onAddToCart = viewModel::addToCart,
                                onRemoveSaved = viewModel::toggleSavedItem,
                                onOpenMenu = { viewModel.selectTab(AppTab.MENU) },
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppTab.ORDERS -> OrdersScreen(
                                uiState = uiState,
                                onSelectFilter = viewModel::setOrdersFilter,
                                onSelectZoneFilter = viewModel::setOrdersZoneFilter,
                                onAdvanceOrder = { order, target -> viewModel.advanceOrder(order, target) },
                                onCancelRequest = viewModel::openCancelDialog,
                                onAssignWaiterClick = viewModel::openAssignWaiterDialog,
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppTab.KITCHEN -> KitchenScreen(
                                uiState = uiState,
                                onSelectStage = viewModel::setKitchenStageFilter,
                                onAdvanceOrder = { order, target -> viewModel.advanceOrder(order, target) },
                                onCancelRequest = viewModel::openCancelDialog,
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppTab.MANAGE -> ManageScreen(
                                uiState = uiState,
                                onSelectSection = viewModel::selectManageSection,
                                onSelectTeamSubSection = viewModel::selectTeamSubSection,
                                onUpdateMenuPrice = viewModel::updateMenuPrice,
                                onToggleMenuAvailable = viewModel::toggleMenuAvailable,
                                onUploadFoodImage = viewModel::uploadFoodImage,
                                onRemoveFoodImage = viewModel::removeFoodImage,
                                onOpenSaved = { viewModel.selectTab(AppTab.SAVED) },
                                onToggleWaiterDuty = viewModel::toggleWaiterDuty,
                                onToggleWaiterArea = viewModel::toggleWaiterArea,
                                onSendBroadcast = viewModel::sendStaffBroadcast,
                                onOpenBrandEditor = viewModel::openBrandEditor,
                                onToggleUserActive = viewModel::toggleStaffAccountActive,
                                onApproveCustomer = viewModel::approveCustomerPermanently,
                                onCreateStaffAccount = viewModel::createStaffAccount,
                                onEditStaffAccount = viewModel::editStaffAccount,
                                onResetStaffPassword = viewModel::resetStaffPassword,
                                onUpdateStaffRole = viewModel::updateStaffRole,
                                onOpenNotification = viewModel::openNotificationItem,
                                onSaveTranslation = viewModel::saveTranslationOverride,
                                onRestoreTranslation = viewModel::restoreDefaultTranslation,
                                onPublishReleaseUpdate = viewModel::publishReleaseUpdate,
                                onCameraPermissionDenied = {
                                    viewModel.showToast(
                                        s.get(
                                            "val_camera_denied",
                                            "Camera permission is needed to take a photo.",
                                        ),
                                    )
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // Bottom gradient scrim + solid nav-bar backing so scrolled content never collides with system nav bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.38f to palette.bg.copy(alpha = 0.92f),
                                0.72f to palette.bg,
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(palette.bg),
                )
            }

            FloatingPillNavBar(
                tabs = bottomBarTabs,
                selectedTab = uiState.currentTab,
                newOrdersCount = newOrdersCount,
                confirmedKitchenCount = confirmedKitchenCount,
                onSelectTab = viewModel::selectTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 600.dp),
            )

            if (uiState.showCartSheet) {
                CartCheckoutSheet(
                    uiState = uiState,
                    onClose = viewModel::closeCartSheet,
                    onAddItem = { viewModel.addToCart(it) },
                    onRemoveItem = viewModel::decrementCartItem,
                    onClearCart = viewModel::clearCart,
                    onChangePlace = {
                        viewModel.closeCartSheet()
                        viewModel.selectTab(AppTab.FLOOR)
                    },
                    onPlaceOrder = viewModel::placeOrder,
                )
            }

            if (uiState.showNotificationsSheet) {
                NotificationsSheet(
                    uiState = uiState,
                    onClose = viewModel::closeNotificationsSheet,
                    onNotificationClick = viewModel::openNotificationItem,
                )
            }

            if (uiState.showSettingsSheet) {
                SettingsSheet(
                    uiState = uiState,
                    hasNotificationPermission = hasNotifPermission,
                    notificationSystemStatus = notifSystemStatus,
                    onClose = viewModel::closeSettingsSheet,
                    onSignOut = onSignOutClick,
                    onSwitchAdminRole = viewModel::switchAdminWorkspaceRole,
                    onOpenMyBrand = { viewModel.openBrandEditor(uiState.activeRole) },
                    onToggleSound = viewModel::setSoundEnabled,
                    onToggleVibration = viewModel::setVibrationEnabled,
                    onVolumeChange = viewModel::setVolume,
                    onTestAlertSound = viewModel::playTestAlertSound,
                    onToggleWaiterDuty = viewModel::toggleWaiterDuty,
                    onRequestNotificationPermission = {
                        val act = scaffoldContext as? Activity
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val canPrompt = !promptedNotifPermission || (
                                act != null && ActivityCompat.shouldShowRequestPermissionRationale(
                                    act,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            )
                            if (canPrompt) {
                                promptedNotifPermission = true
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                AlertAudioAndNotificationManager.openAppNotificationSettings(scaffoldContext)
                            }
                        } else {
                            AlertAudioAndNotificationManager.openAppNotificationSettings(scaffoldContext)
                        }
                    },
                    onOpenNotificationSettings = {
                        AlertAudioAndNotificationManager.openAppNotificationSettings(scaffoldContext)
                    },
                    onSelectLanguage = viewModel::setLanguage,
                    onUploadProfilePhoto = viewModel::uploadProfilePhoto,
                    onRemoveProfilePhoto = viewModel::removeProfilePhoto,
                    onUpdateProfileName = viewModel::updateProfileName,
                    onOpenSaved = {
                        viewModel.closeSettingsSheet()
                        viewModel.selectTab(AppTab.SAVED)
                    },
                    onOpenUpdateDialog = {
                        viewModel.closeSettingsSheet()
                        viewModel.openAppUpdateDialog()
                    },
                    onCameraPermissionDenied = {
                        viewModel.showToast(
                            s.get(
                                "val_camera_denied",
                                "Camera permission is needed to take a photo.",
                            ),
                        )
                    },
                )
            }

            if (uiState.showAppUpdateDialog) {
                val updateApkFallback = uiState.notifications
                    .firstOrNull { it.kind == "app_update" && it.apkUrl.isNotBlank() }
                    ?.apkUrl
                    .orEmpty()
                AppUpdateAvailableDialog(
                    releaseConfig = uiState.releaseConfig,
                    fallbackApkUrl = updateApkFallback,
                    onDismiss = viewModel::dismissAppUpdateDialog,
                )
            }

            val editingRole = uiState.editingBrandForRole
            if (editingRole != null) {
                val currentCfg = uiState.brandConfigs[editingRole] ?: editingRole.defaultBrandConfig()
                BrandEditorSheet(
                    role = editingRole,
                    currentConfig = currentCfg,
                    onClose = viewModel::closeBrandEditor,
                    onReset = viewModel::resetBrand,
                    onSave = viewModel::saveBrand,
                )
            }

            val assigningOrder = uiState.assigningWaiterOrder
            if (assigningOrder != null) {
                WaiterAssignDialog(
                    order = assigningOrder,
                    waiters = uiState.waiterUsers,
                    waiterCoverage = uiState.waiterCoverage,
                    allOrders = uiState.orders,
                    assignedWaiterName = uiState.waiterName(assigningOrder.assignedWaiterId),
                    onDismiss = viewModel::dismissAssignWaiterDialog,
                    onAutoAssign = { viewModel.assignOrderWaiter(assigningOrder.id, null) },
                    onAssignWaiter = { waiterId -> viewModel.assignOrderWaiter(assigningOrder.id, waiterId) },
                )
            }

            val cancelling = uiState.cancellingOrder
            val pendingKitchen = uiState.pendingKitchenOrders
            val readyPickups = uiState.readyPickupOrders

            if (cancelling != null) {
                val senderRole = cancelling.createdByRoleEnum
                val senderPalette = uiState.brands[senderRole] ?: deriveBrandPalette(senderRole.defaultBrandConfig())
                KitchenOrderAlertDialog(
                    order = cancelling,
                    waitingCount = pendingKitchen.size.coerceAtLeast(1),
                    senderPalette = senderPalette,
                    isCancellingMode = true,
                    onOpenCancelMode = {},
                    onCancelBack = viewModel::dismissCancelDialog,
                    onConfirmCancel = { reason -> viewModel.advanceOrder(cancelling, OrderStatus.CANCELLED, reason) },
                    onAccept = { viewModel.advanceOrder(cancelling, OrderStatus.PREPARING) },
                )
            } else if (pendingKitchen.isNotEmpty()) {
                val firstOrder = pendingKitchen.first()
                LaunchedEffect(firstOrder.id, firstOrder.status) {
                    viewModel.notifyPopupCardVisible(firstOrder, AlertSoundKind.NEW_ORDER)
                    while (true) {
                        kotlinx.coroutines.delay(1800L)
                        viewModel.notifyPopupCardVisible(firstOrder, AlertSoundKind.NEW_ORDER, forceRepeat = true)
                    }
                }
                val senderRole = firstOrder.createdByRoleEnum
                val senderPalette = uiState.brands[senderRole] ?: deriveBrandPalette(senderRole.defaultBrandConfig())
                KitchenOrderAlertDialog(
                    order = firstOrder,
                    waitingCount = pendingKitchen.size,
                    senderPalette = senderPalette,
                    isCancellingMode = false,
                    onOpenCancelMode = { viewModel.openCancelDialog(firstOrder) },
                    onCancelBack = viewModel::dismissCancelDialog,
                    onConfirmCancel = { reason -> viewModel.advanceOrder(firstOrder, OrderStatus.CANCELLED, reason) },
                    onAccept = { viewModel.advanceOrder(firstOrder, OrderStatus.PREPARING) },
                )
            } else if (readyPickups.isNotEmpty()) {
                val firstReady = readyPickups.first()
                LaunchedEffect(firstReady.id, firstReady.status) {
                    viewModel.notifyPopupCardVisible(firstReady, AlertSoundKind.ORDER_READY)
                    while (true) {
                        kotlinx.coroutines.delay(4000L)
                        viewModel.notifyPopupCardVisible(firstReady, AlertSoundKind.ORDER_READY, forceRepeat = true)
                    }
                }
                val senderRole = firstReady.lastUpdatedByRoleEnum
                val senderPalette = uiState.brands[senderRole] ?: deriveBrandPalette(senderRole.defaultBrandConfig())
                PickupReadyAlertDialog(
                    order = firstReady,
                    readyCount = readyPickups.size,
                    activeRole = uiState.activeRole,
                    senderPalette = senderPalette,
                    onLater = { viewModel.dismissReadyAlertForLater(firstReady.id) },
                    onConfirmPickup = { viewModel.advanceOrder(firstReady, OrderStatus.DONE) },
                )
            }

            val toast = uiState.activeToast
            val toastPalette = toast?.let { uiState.brands[it.senderRole] ?: deriveBrandPalette(it.senderRole.defaultBrandConfig()) }
            BrandedToastBanner(
                toast = toast,
                senderPalette = toastPalette,
                onDismiss = viewModel::dismissToast,
                onOpenDeepLink = viewModel::handleDeepLink,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            )
        }
    }
}
