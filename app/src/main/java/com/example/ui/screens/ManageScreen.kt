package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.ManageSection
import com.example.data.model.MenuItem
import com.example.data.model.StaffUser
import com.example.data.model.TeamSubSection
import com.example.data.model.UserRole
import com.example.data.model.WaiterArea
import com.example.localization.LocalAppStrings
import com.example.localization.TranslationCatalog
import com.example.localization.TranslationCategory
import com.example.localization.TranslationRecord
import com.example.localization.TranslationStatus
import com.example.ui.components.ImageSourcePickerDialog
import com.example.ui.components.rememberMediaImagePicker
import com.example.ui.theme.BrandPalette
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState

@Composable
fun ManageScreen(
    uiState: LakemarkUiState,
    onSelectSection: (ManageSection) -> Unit = {},
    onSelectTeamSubSection: (TeamSubSection) -> Unit = {},
    onUpdateMenuPrice: (String, Long) -> Unit = { _, _ -> },
    onToggleMenuAvailable: (MenuItem) -> Unit = {},
    onUploadFoodImage: (MenuItem, Uri) -> Unit = { _, _ -> },
    onRemoveFoodImage: (MenuItem) -> Unit = {},
    onOpenSaved: () -> Unit = {},
    onToggleWaiterDuty: (String) -> Unit = {},
    onToggleWaiterArea: (String, WaiterArea) -> Unit = { _, _ -> },
    onSendBroadcast: (String, String, () -> Unit) -> Unit,
    onOpenBrandEditor: (UserRole) -> Unit,
    onToggleUserActive: (StaffUser) -> Unit,
    onApproveCustomer: (StaffUser) -> Unit = {},
    onCreateStaffAccount: (String, String, UserRole, String, () -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onEditStaffAccount: (StaffUser, String, String, UserRole, Boolean, String, () -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onResetStaffPassword: (StaffUser, String, () -> Unit, (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onUpdateStaffRole: (StaffUser, UserRole) -> Unit,
    onOpenNotification: (AppNotification) -> Unit = {},
    onSaveTranslation: (key: String, category: TranslationCategory, en: String, am: String, enVersion: Int, onDone: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onRestoreTranslation: (key: String, onDone: () -> Unit) -> Unit = { _, _ -> },
    onPublishReleaseUpdate: (versionCode: Long, versionName: String, apkDownloadUrl: String, releaseNotes: String, mandatory: Boolean, apkSha256: String, onDone: () -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onCameraPermissionDenied: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()

    var menuCategory by remember { mutableStateOf("All") }
    var menuSearchQuery by remember { mutableStateOf("") }
    var foodPhotoTarget by remember { mutableStateOf<MenuItem?>(null) }
    var showFoodImageSourceDialog by remember { mutableStateOf<MenuItem?>(null) }

    var accountFilter by remember { mutableStateOf(AccountRoleFilter.ALL) }
    var activityFilter by remember { mutableStateOf(ActivityLogFilter.ALL) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var editingStaffUser by remember { mutableStateOf<StaffUser?>(null) }
    var resetPasswordUser by remember { mutableStateOf<StaffUser?>(null) }

    var broadcastTarget by remember { mutableStateOf("all") }
    var broadcastMsg by remember { mutableStateOf("") }

    var langSearchQuery by remember { mutableStateOf("") }
    var langCategoryFilter by remember { mutableStateOf<TranslationCategory?>(null) }
    var langStatusFilter by remember { mutableStateOf<TranslationStatus?>(null) }
    var editingTranslation by remember { mutableStateOf<TranslationRecord?>(null) }

    val foodMediaPicker = rememberMediaImagePicker(
        filePrefix = "lakemark_food",
        onImageSelected = { uri ->
            val target = foodPhotoTarget
            foodPhotoTarget = null
            if (target != null) {
                onUploadFoodImage(target, uri)
            }
        },
        onCameraPermissionDenied = onCameraPermissionDenied,
    )

    val translationRecords = remember(uiState.remoteTranslations) {
        TranslationCatalog.buildRecords(uiState.remoteTranslations)
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier.imePadding(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = navBottom + 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "manage_title") {
            Text(
                text = s.get("manage_pill", "Manage"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = (-1.1).sp,
                color = palette.plum,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        item(key = "manage_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.alt)
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ManageSection.entries.forEach { section ->
                    val selected = uiState.manageSection == section
                    val icon = when (section) {
                        ManageSection.OVERVIEW -> Icons.Filled.BarChart
                        ManageSection.MENU -> Icons.Filled.RestaurantMenu
                        ManageSection.TEAM -> Icons.Filled.Group
                        ManageSection.LANGUAGE -> Icons.Filled.Translate
                        ManageSection.ALERTS -> Icons.Filled.Campaign
                    }
                    Box(
                        modifier = Modifier
                            .widthIn(min = 72.dp)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) palette.plum else Color.Transparent)
                            .clickable { onSelectSection(section) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("manage_tab_${section.key}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) palette.acc else palette.mut,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = s.manageSectionLabel(section),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                color = if (selected) palette.onPlum else palette.mut,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        when (uiState.manageSection) {
            ManageSection.OVERVIEW -> overviewSectionItems(uiState)
            ManageSection.MENU -> manageMenuSectionItems(
                uiState = uiState,
                selectedCategory = menuCategory,
                searchQuery = menuSearchQuery,
                onSelectCategory = { menuCategory = it },
                onSearchQueryChange = { menuSearchQuery = it },
                onUpdateMenuPrice = onUpdateMenuPrice,
                onToggleMenuAvailable = onToggleMenuAvailable,
                onPickFoodImage = { item -> showFoodImageSourceDialog = item },
                onRemoveFoodImage = onRemoveFoodImage,
                onOpenSaved = onOpenSaved,
            )
            ManageSection.TEAM -> teamSectionItems(
                uiState = uiState,
                teamSubSection = uiState.teamSubSection,
                accountFilter = accountFilter,
                onSelectTeamSubSection = onSelectTeamSubSection,
                onSelectAccountFilter = { accountFilter = it },
                onOpenBrandEditor = onOpenBrandEditor,
                onToggleUserActive = onToggleUserActive,
                onApproveCustomer = onApproveCustomer,
                onOpenAddStaffDialog = { showAddStaffDialog = true },
                onOpenEditStaffDialog = { editingStaffUser = it },
                onOpenResetPasswordDialog = { resetPasswordUser = it },
                onUpdateStaffRole = onUpdateStaffRole,
                onToggleWaiterDuty = onToggleWaiterDuty,
                onToggleWaiterArea = onToggleWaiterArea,
            )
            ManageSection.LANGUAGE -> manageLanguageSectionItems(
                records = translationRecords,
                searchQuery = langSearchQuery,
                selectedCategory = langCategoryFilter,
                selectedStatus = langStatusFilter,
                onSearchChange = { langSearchQuery = it },
                onSelectCategory = { langCategoryFilter = it },
                onSelectStatus = { langStatusFilter = it },
                onEditRecord = { editingTranslation = it },
                palette = palette,
                s = s,
            )
            ManageSection.ALERTS -> alertsSectionItems(
                uiState = uiState,
                broadcastTarget = broadcastTarget,
                onSelectBroadcastTarget = { broadcastTarget = it },
                broadcastMsg = broadcastMsg,
                onBroadcastMsgChange = { broadcastMsg = it },
                onSendBroadcast = {
                    onSendBroadcast(broadcastTarget, broadcastMsg) {
                        broadcastMsg = ""
                    }
                },
                activityFilter = activityFilter,
                onSelectActivityFilter = { activityFilter = it },
                onOpenNotification = onOpenNotification,
                onPublishReleaseUpdate = onPublishReleaseUpdate,
            )
        }
    }

    val pendingFoodItem = showFoodImageSourceDialog
    if (pendingFoodItem != null) {
        ImageSourcePickerDialog(
            title = s.dishName(pendingFoodItem),
            onDismiss = { showFoodImageSourceDialog = null },
            onPickGallery = {
                foodPhotoTarget = pendingFoodItem
                foodMediaPicker.openGallery()
            },
            onPickCamera = {
                foodPhotoTarget = pendingFoodItem
                foodMediaPicker.openCamera()
            },
        )
    }

    val editingRec = editingTranslation
    if (editingRec != null) {
        TranslationEditorDialog(
            record = editingRec,
            onDismiss = { editingTranslation = null },
            onSave = { en, am ->
                onSaveTranslation(
                    editingRec.key,
                    editingRec.category,
                    en,
                    am,
                    editingRec.currentEnVersion,
                    { editingTranslation = null },
                    { /* Toast / error shown via repository/snackbar if needed */ },
                )
            },
            onRestoreDefault = {
                onRestoreTranslation(editingRec.key) {
                    editingTranslation = null
                }
            },
        )
    }

    if (showAddStaffDialog) {
        AddStaffAccountDialog(
            onDismiss = { showAddStaffDialog = false },
            onCreateStaff = onCreateStaffAccount,
        )
    }

    val targetEditUser = editingStaffUser
    if (targetEditUser != null) {
        EditStaffAccountDialog(
            staff = targetEditUser,
            isSelf = targetEditUser.userId == uiState.currentUser?.userId,
            onDismiss = { editingStaffUser = null },
            onSaveStaff = onEditStaffAccount,
        )
    }

    val targetResetUser = resetPasswordUser
    if (targetResetUser != null) {
        ResetStaffPasswordDialog(
            staff = targetResetUser,
            onDismiss = { resetPasswordUser = null },
            onResetPassword = onResetStaffPassword,
        )
    }
}

@Composable
fun SwatchDotsRow(palette: BrandPalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(palette.primary, palette.secondary, palette.accent).forEach { color ->
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
            )
        }
    }
}
