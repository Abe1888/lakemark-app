package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StaffUser
import com.example.data.model.TeamSubSection
import com.example.data.model.UserRole
import com.example.data.model.WaiterArea
import com.example.data.model.WaiterCoverage
import com.example.localization.LocalAppStrings
import com.example.ui.components.BrandMarkOrIcon
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandPalette
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledText
import com.example.ui.theme.StatusNewBg
import com.example.ui.theme.StatusNewText
import com.example.ui.theme.StatusReadyBg
import com.example.ui.theme.StatusReadyText
import com.example.ui.theme.deriveBrandPalette
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import java.text.SimpleDateFormat
import java.util.Locale

enum class AccountRoleFilter(val key: String, val label: String) {
    ALL("all", "All"),
    STAFF("staff", "Staff"),
    WAITERS("waiters", "Waiters"),
    GUESTS("guests", "Guests"),
}

internal fun LazyListScope.teamSectionItems(
    uiState: LakemarkUiState,
    teamSubSection: TeamSubSection,
    accountFilter: AccountRoleFilter,
    onSelectTeamSubSection: (TeamSubSection) -> Unit,
    onSelectAccountFilter: (AccountRoleFilter) -> Unit,
    onOpenBrandEditor: (UserRole) -> Unit,
    onToggleUserActive: (StaffUser) -> Unit,
    onApproveCustomer: (StaffUser) -> Unit,
    onOpenAddStaffDialog: () -> Unit,
    onOpenEditStaffDialog: (StaffUser) -> Unit,
    onOpenResetPasswordDialog: (StaffUser) -> Unit,
    onUpdateStaffRole: (StaffUser, UserRole) -> Unit,
    onToggleWaiterDuty: (String) -> Unit,
    onToggleWaiterArea: (String, WaiterArea) -> Unit,
) {
    item(key = "team_sub_switcher") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        val pendingCustomersCount = uiState.staffUsers.count { it.userRole == UserRole.CUSTOMER && !it.active }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface)
                .border(1.dp, palette.line.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TeamSubSection.entries.forEach { sub ->
                val selected = teamSubSection == sub
                val badgeText = when (sub) {
                    TeamSubSection.ACCOUNTS -> "${uiState.staffUsers.size}"
                    TeamSubSection.NEW_REGISTERED -> if (pendingCustomersCount > 0) "$pendingCustomersCount" else "${uiState.staffUsers.size}"
                    TeamSubSection.COVERAGE -> "${uiState.waiterUsers.size}"
                    TeamSubSection.BRANDS -> "${UserRole.entries.size}"
                }
                val icon = when (sub) {
                    TeamSubSection.ACCOUNTS -> Icons.Filled.People
                    TeamSubSection.NEW_REGISTERED -> Icons.Filled.NewReleases
                    TeamSubSection.COVERAGE -> Icons.Filled.RoomService
                    TeamSubSection.BRANDS -> Icons.Filled.Palette
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) palette.acc else Color.Transparent)
                        .clickable { onSelectTeamSubSection(sub) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("team_subtab_${sub.key}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) palette.onAcc else palette.mut,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "${s.teamSubSectionLabel(sub)} ($badgeText)",
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = if (selected) palette.onAcc else palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    when (teamSubSection) {
        TeamSubSection.ACCOUNTS -> teamAccountsSubItems(
            uiState = uiState,
            accountFilter = accountFilter,
            onSelectAccountFilter = onSelectAccountFilter,
            onToggleUserActive = onToggleUserActive,
            onApproveCustomer = onApproveCustomer,
            onOpenAddStaffDialog = onOpenAddStaffDialog,
            onOpenEditStaffDialog = onOpenEditStaffDialog,
            onOpenResetPasswordDialog = onOpenResetPasswordDialog,
            onUpdateStaffRole = onUpdateStaffRole,
        )
        TeamSubSection.NEW_REGISTERED -> teamNewlyRegisteredSubItems(
            uiState = uiState,
            onApproveCustomer = onApproveCustomer,
            onToggleUserActive = onToggleUserActive,
        )
        TeamSubSection.COVERAGE -> teamCoverageSubItems(
            uiState = uiState,
            onToggleWaiterDuty = onToggleWaiterDuty,
            onToggleWaiterArea = onToggleWaiterArea,
        )
        TeamSubSection.BRANDS -> teamBrandsSubItems(
            uiState = uiState,
            onOpenBrandEditor = onOpenBrandEditor,
        )
    }
}

private fun LazyListScope.teamNewlyRegisteredSubItems(
    uiState: LakemarkUiState,
    onApproveCustomer: (StaffUser) -> Unit,
    onToggleUserActive: (StaffUser) -> Unit,
) {
    val newestFirst = uiState.staffUsers.sortedByDescending { it.createdAt?.seconds ?: 0L }

    item(key = "new_reg_header") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("newly_registered_header"),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = s.get("manage_new_reg_title", "Newly registered users"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = palette.text,
                )
                Text(
                    text = s.get(
                        "manage_new_reg_desc",
                        "Recent sign-ups, registration status, and Manager activation controls.",
                    ),
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = palette.mut,
                )
            }
        }
    }

    if (newestFirst.isEmpty()) {
        item(key = "new_reg_empty") {
            val palette = LocalBrandPalette.current
            val s = LocalAppStrings.current
            val font = lakemarkFont()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = s.get("orders_empty", "No registrations recorded yet."),
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = palette.mut,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    } else {
        items(newestFirst, key = { "newreg_${it.userId.ifBlank { it.id }}" }) { user ->
            val palette = LocalBrandPalette.current
            val s = LocalAppStrings.current
            val font = lakemarkFont()
            val timeFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
            val regTime = user.createdAt?.toDate()?.let { timeFormat.format(it) } ?: "—"
            val role = user.userRole
            val isPendingCustomer = role == UserRole.CUSTOMER && !user.active
            val isPermCustomer = role == UserRole.CUSTOMER && (user.active && user.permanentlyActivated)
            val handle = if (user.username.isNotBlank()) "@${user.username}" else user.email

            val (statusLabel, statusBg, statusFg) = when {
                isPendingCustomer -> Triple(
                    s.get("manage_reg_status_pending", "Pending approval"),
                    StatusNewBg,
                    StatusNewText,
                )
                isPermCustomer -> Triple(
                    s.get("manage_reg_status_perm", "Permanently activated"),
                    StatusReadyBg,
                    StatusReadyText,
                )
                user.active -> Triple(
                    s.get("manage_reg_status_active", "Active"),
                    StatusReadyBg,
                    StatusReadyText,
                )
                else -> Triple(
                    s.get("manage_reg_status_disabled", "Disabled"),
                    StatusCancelledBg,
                    StatusCancelledText,
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_reg_card_${user.userId}"),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    UserAvatar(
                        photoUrl = user.photoUrl,
                        name = user.name.ifBlank { handle },
                        role = role,
                        palette = palette,
                        size = 44.dp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = user.name.ifBlank { handle.ifBlank { s.roleName(role) } },
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${s.roleName(role)}${if (handle.isNotBlank()) " · $handle" else ""}",
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = palette.plum,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = s.format("manage_reg_at", "time" to regTime),
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = palette.mut,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = statusLabel,
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = statusFg,
                            )
                        }
                    }

                    if (uiState.isAdminUser && isPendingCustomer) {
                        Button(
                            onClick = { onApproveCustomer(user) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.acc,
                                contentColor = palette.onAcc,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("new_reg_approve_${user.userId}"),
                        ) {
                            Text(
                                text = s.get("manage_approve_btn", "Approve"),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                            )
                        }
                    } else if (uiState.isAdminUser && role != UserRole.MANAGER && role != UserRole.CUSTOMER) {
                        Switch(
                            checked = user.active,
                            onCheckedChange = { onToggleUserActive(user) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.acc,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = palette.line,
                            ),
                            modifier = Modifier.testTag("new_reg_switch_${user.userId}"),
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.teamAccountsSubItems(
    uiState: LakemarkUiState,
    accountFilter: AccountRoleFilter,
    onSelectAccountFilter: (AccountRoleFilter) -> Unit,
    onToggleUserActive: (StaffUser) -> Unit,
    onApproveCustomer: (StaffUser) -> Unit,
    onOpenAddStaffDialog: () -> Unit,
    onOpenEditStaffDialog: (StaffUser) -> Unit,
    onOpenResetPasswordDialog: (StaffUser) -> Unit,
    onUpdateStaffRole: (StaffUser, UserRole) -> Unit,
) {
    val allUsers = uiState.staffUsers
    val filteredUsers = allUsers.filter { user ->
        when (accountFilter) {
            AccountRoleFilter.ALL -> true
            AccountRoleFilter.STAFF -> user.userRole.isStaff
            AccountRoleFilter.WAITERS -> user.userRole == UserRole.WAITER
            AccountRoleFilter.GUESTS -> user.userRole == UserRole.CUSTOMER
        }
    }

    item(key = "accounts_header") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        val activeCount = allUsers.count { it.active }
        val pendingCount = allUsers.count { it.userRole == UserRole.CUSTOMER && !it.active }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.get("team_sub_accounts", "Accounts"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = palette.text,
                )
                val pendingSuffix = if (pendingCount > 0) " · $pendingCount ${s.get("manage_reg_status_pending", "pending")}" else ""
                Text(
                    text = "$activeCount/${allUsers.size} ${s.get("manage_reg_status_active", "active")}$pendingSuffix",
                    fontFamily = font,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = palette.mut,
                )
            }
            Button(
                onClick = onOpenAddStaffDialog,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.plum,
                    contentColor = palette.onPlum,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .testTag("add_staff_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = s.get("manage_create_staff_btn", "Add staff"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                )
            }
        }
    }

    item(key = "accounts_filter_row") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountRoleFilter.entries.forEach { filter ->
                val selected = accountFilter == filter
                val filterLabel = when (filter) {
                    AccountRoleFilter.ALL -> s.get("cat_all", "All")
                    AccountRoleFilter.STAFF -> s.get("role_kitchen", "Staff")
                    AccountRoleFilter.WAITERS -> s.get("team_sub_waiters", "Waiters")
                    AccountRoleFilter.GUESTS -> s.get("role_customer", "Guests")
                }
                Surface(
                    onClick = { onSelectAccountFilter(filter) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) palette.plum else palette.alt,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("accounts_filter_${filter.key}"),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = filterLabel,
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = if (selected) palette.onPlum else palette.text,
                        )
                    }
                }
            }
        }
    }

    if (filteredUsers.isEmpty()) {
        item(key = "accounts_empty") {
            val palette = LocalBrandPalette.current
            val s = LocalAppStrings.current
            val font = lakemarkFont()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = s.get("orders_empty", "No accounts found."),
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = palette.mut,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    } else {
        items(filteredUsers, key = { "staff_${it.userId.ifBlank { it.id }}" }) { staff ->
            StaffAccountCard(
                staff = staff,
                uiState = uiState,
                onUpdateStaffRole = onUpdateStaffRole,
                onToggleUserActive = onToggleUserActive,
                onApproveCustomer = onApproveCustomer,
                onEditStaff = { onOpenEditStaffDialog(staff) },
                onResetPassword = { onOpenResetPasswordDialog(staff) },
            )
        }
    }
}

@Composable
private fun StaffAccountCard(
    staff: StaffUser,
    uiState: LakemarkUiState,
    onUpdateStaffRole: (StaffUser, UserRole) -> Unit,
    onToggleUserActive: (StaffUser) -> Unit,
    onApproveCustomer: (StaffUser) -> Unit,
    onEditStaff: () -> Unit,
    onResetPassword: () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val staffRole = staff.userRole
    val staffPalette = uiState.brands[staffRole] ?: deriveBrandPalette(staffRole.defaultBrandConfig())
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var showCredentialsPassword by remember { mutableStateOf(false) }

    val customBrandLabel = staffPalette.brandName.takeIf {
        it.isNotBlank() && !it.equals(staffRole.displayName, ignoreCase = true)
    }
    val handleText = if (staff.username.isNotBlank()) "@${staff.username}" else staff.email
    val isPendingCustomer = staffRole == UserRole.CUSTOMER && !staff.active
    val isPermCustomer = staffRole == UserRole.CUSTOMER && staff.active

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (staff.active || isPendingCustomer) 1f else 0.62f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UserAvatar(
                    photoUrl = staff.photoUrl,
                    name = staff.name.ifBlank { handleText },
                    role = staffRole,
                    palette = staffPalette,
                    size = 44.dp,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = staff.name.ifBlank { handleText },
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val subtitle = listOfNotNull(
                            handleText.takeIf { it.isNotBlank() },
                            customBrandLabel,
                        ).joinToString(" · ")
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                fontFamily = font,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = palette.mut,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        SwatchDotsRow(staffPalette)
                    }
                }

                Box {
                    Button(
                        onClick = { roleMenuExpanded = true },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.alt, contentColor = palette.text),
                        contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        modifier = Modifier.heightIn(min = 38.dp),
                    ) {
                        Text(
                            text = s.roleName(staffRole),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Change role",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false },
                    ) {
                        UserRole.entries.forEach { targetRole ->
                            DropdownMenuItem(
                                text = { Text(s.roleName(targetRole), fontFamily = font) },
                                onClick = {
                                    roleMenuExpanded = false
                                    onUpdateStaffRole(staff, targetRole)
                                },
                            )
                        }
                    }
                }

                when {
                    staffRole == UserRole.MANAGER -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.alt)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = s.roleName(UserRole.MANAGER),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                color = palette.text,
                            )
                        }
                    }
                    staffRole == UserRole.CUSTOMER -> {
                        if (isPendingCustomer) {
                            Button(
                                onClick = { onApproveCustomer(staff) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("approve_customer_${staff.userId}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = s.get("manage_approve_btn", "Approve"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                )
                            }
                        } else if (isPermCustomer) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(StatusReadyBg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("customer_activated_badge_${staff.userId}"),
                            ) {
                                Text(
                                    text = s.get("manage_approved_perm_badge", "Activated"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    color = StatusReadyText,
                                )
                            }
                        }
                    }
                    else -> {
                        Switch(
                            checked = staff.active,
                            onCheckedChange = { onToggleUserActive(staff) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.acc,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = palette.line,
                            ),
                            modifier = Modifier.testTag("account_switch_${staff.userId}"),
                        )
                    }
                }
            }

            if (isPendingCustomer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(StatusNewBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = s.get("manage_reg_status_pending", "Disabled · Pending manager approval"),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = StatusNewText,
                        )
                    }
                }
            } else if (staffRole != UserRole.CUSTOMER) {
                if (staffRole != UserRole.MANAGER && (staff.username.isNotBlank() || staff.staffPassword.isNotBlank())) {
                    val displayedPassword = when {
                        staff.staffPassword.isBlank() -> s.get("manage_pw_not_stored", "Tap Reset password to set")
                        showCredentialsPassword -> staff.staffPassword
                        else -> "••••••••"
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.alt,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (staff.username.isNotBlank()) {
                                    Text(
                                        text = "${s.get("manage_create_staff_user", "Username")}: @${staff.username}",
                                        fontFamily = font,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = palette.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = "${s.get("auth_password_label", "Password")}: $displayedPassword",
                                    fontFamily = font,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = palette.mut,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (staff.staffPassword.isNotBlank()) {
                                IconButton(
                                    onClick = { showCredentialsPassword = !showCredentialsPassword },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = if (showCredentialsPassword) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = if (showCredentialsPassword) {
                                            s.get("auth_hide_password", "Hide password")
                                        } else {
                                            s.get("auth_show_password", "Show password")
                                        },
                                        tint = palette.plum,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (staff.username.isNotBlank() && staffRole != UserRole.MANAGER) {
                        TextButton(
                            onClick = onResetPassword,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("reset_password_${staff.userId}"),
                        ) {
                            Text(
                                text = s.get("manage_reset_pw_btn", "Reset password"),
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = palette.mut,
                            )
                        }
                    }
                    TextButton(
                        onClick = onEditStaff,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("edit_staff_${staff.userId}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = palette.plum,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = s.get("manage_edit_staff_btn", "Edit"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = palette.plum,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddStaffAccountDialog(
    onDismiss: () -> Unit,
    onCreateStaff: (name: String, username: String, role: UserRole, password: String, onDone: () -> Unit, onError: (String) -> Unit) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.KITCHEN) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val staffRoles = remember {
        listOf(UserRole.RECEPTION, UserRole.KITCHEN, UserRole.WAITER, UserRole.CASHIER, UserRole.DELIVERY)
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = palette.surface,
        title = {
            Text(
                text = s.get("manage_create_staff_title", "Create staff account"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = palette.text,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = s.get("auth_desc_staff", "Staff sign in inside Lakemark with username and password."),
                    fontFamily = font,
                    fontSize = 12.sp,
                    color = palette.mut,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(60)
                        errorMsg = ""
                    },
                    label = { Text(s.get("manage_create_staff_name", "Full name"), fontFamily = font) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_staff_name_input"),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.take(32)
                        errorMsg = ""
                    },
                    label = { Text(s.get("manage_create_staff_user", "Username"), fontFamily = font) },
                    placeholder = { Text("e.g. kitchen1", fontFamily = font) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_staff_username_input"),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = s.get("role_kitchen", "Role"),
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = palette.text,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        staffRoles.forEach { role ->
                            val selected = selectedRole == role
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) palette.plum else palette.alt)
                                    .clickable(enabled = !isSubmitting) { selectedRole = role }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("add_staff_role_${role.key}"),
                            ) {
                                Text(
                                    text = s.roleName(role),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (selected) palette.onPlum else palette.text,
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it.take(64)
                        errorMsg = ""
                    },
                    label = { Text(s.get("manage_create_staff_pass", "Initial password"), fontFamily = font) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) {
                                    s.get("auth_hide_password", "Hide password")
                                } else {
                                    s.get("auth_show_password", "Show password")
                                },
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_staff_password_input"),
                )
                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StatusCancelledText,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    errorMsg = ""
                    onCreateStaff(
                        name,
                        username,
                        selectedRole,
                        password,
                        {
                            isSubmitting = false
                            onDismiss()
                        },
                        { err ->
                            isSubmitting = false
                            errorMsg = err
                        },
                    )
                },
                enabled = !isSubmitting && name.isNotBlank() && username.isNotBlank() && password.length >= 6,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.acc, contentColor = palette.onAcc),
                modifier = Modifier.testTag("add_staff_confirm_button"),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = palette.onAcc,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(s.get("manage_create_staff_btn", "Create account"), fontFamily = font, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(s.get("btn_cancel", "Cancel"), fontFamily = font, color = palette.mut)
            }
        },
    )
}

@Composable
fun ResetStaffPasswordDialog(
    staff: StaffUser,
    onDismiss: () -> Unit,
    onResetPassword: (StaffUser, String, () -> Unit, (String) -> Unit) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    var newPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = palette.surface,
        title = {
            Text(
                text = s.get("manage_reset_pw_btn", "Reset password"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = palette.text,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${staff.name.ifBlank { staff.username }} (@${staff.username})",
                    fontFamily = font,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = palette.mut,
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it.take(64)
                        errorMsg = ""
                    },
                    label = { Text(s.get("auth_password_label", "Password"), fontFamily = font) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) {
                                    s.get("auth_hide_password", "Hide password")
                                } else {
                                    s.get("auth_show_password", "Show password")
                                },
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_staff_password_input"),
                )
                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StatusCancelledText,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    errorMsg = ""
                    onResetPassword(
                        staff,
                        newPassword,
                        {
                            isSubmitting = false
                            onDismiss()
                        },
                        { err ->
                            isSubmitting = false
                            errorMsg = err
                        },
                    )
                },
                enabled = !isSubmitting && newPassword.length >= 6,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.plum, contentColor = palette.onPlum),
                modifier = Modifier.testTag("reset_staff_password_confirm"),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = palette.onPlum,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(s.get("btn_save", "Save"), fontFamily = font, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(s.get("btn_cancel", "Cancel"), fontFamily = font, color = palette.mut)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditStaffAccountDialog(
    staff: StaffUser,
    isSelf: Boolean,
    onDismiss: () -> Unit,
    onSaveStaff: (
        staff: StaffUser,
        name: String,
        username: String,
        role: UserRole,
        active: Boolean,
        newPassword: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val isManagerAccount = staff.userRole == UserRole.MANAGER
    var name by remember(staff.userId) { mutableStateOf(staff.name) }
    var username by remember(staff.userId) { mutableStateOf(staff.username) }
    var selectedRole by remember(staff.userId) { mutableStateOf(staff.userRole) }
    var active by remember(staff.userId) { mutableStateOf(staff.active) }
    var newPassword by remember(staff.userId) { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val staffRoles = remember {
        listOf(UserRole.RECEPTION, UserRole.KITCHEN, UserRole.WAITER, UserRole.CASHIER, UserRole.DELIVERY)
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .widthIn(max = 480.dp)
                .testTag("edit_staff_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = s.get("manage_edit_staff_title", "Edit staff account"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = palette.text,
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(60)
                        errorMsg = ""
                    },
                    label = { Text(s.get("manage_create_staff_name", "Full name"), fontFamily = font) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_staff_name_input"),
                )

                if (!isManagerAccount) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.take(32)
                            errorMsg = ""
                        },
                        label = { Text(s.get("manage_create_staff_user", "Username"), fontFamily = font) },
                        placeholder = { Text("e.g. kitchen1", fontFamily = font) },
                        singleLine = true,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_staff_username_input"),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = s.roleName(selectedRole),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = palette.text,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            staffRoles.forEach { role ->
                                val selected = selectedRole == role
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (selected) palette.plum else palette.alt)
                                        .clickable(enabled = !isSubmitting) { selectedRole = role }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("edit_staff_role_${role.key}"),
                                ) {
                                    Text(
                                        text = s.roleName(role),
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (selected) palette.onPlum else palette.text,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = s.get("manage_account_active_label", "Account active"),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.text,
                        )
                        Switch(
                            checked = active,
                            onCheckedChange = { if (!isSelf && !isSubmitting) active = it },
                            enabled = !isSelf && !isSubmitting,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.acc,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = palette.line,
                            ),
                            modifier = Modifier.testTag("edit_staff_active_switch"),
                        )
                    }

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it.take(64)
                            errorMsg = ""
                        },
                        label = {
                            Text(
                                s.get("manage_new_password_optional", "New password (optional)"),
                                fontFamily = font,
                            )
                        },
                        singleLine = true,
                        enabled = !isSubmitting,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) {
                                        s.get("auth_hide_password", "Hide password")
                                    } else {
                                        s.get("auth_show_password", "Show password")
                                    },
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_staff_password_input"),
                    )
                }

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StatusCancelledText,
                        modifier = Modifier.testTag("edit_staff_error_text"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text(s.get("btn_cancel", "Cancel"), fontFamily = font, color = palette.mut)
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            isSubmitting = true
                            errorMsg = ""
                            onSaveStaff(
                                staff,
                                name,
                                username,
                                selectedRole,
                                active,
                                newPassword,
                                {
                                    isSubmitting = false
                                    onDismiss()
                                },
                                { err ->
                                    isSubmitting = false
                                    errorMsg = err
                                },
                            )
                        },
                        enabled = !isSubmitting &&
                            name.isNotBlank() &&
                            (newPassword.isEmpty() || newPassword.length >= 6),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.plum, contentColor = palette.onPlum),
                        modifier = Modifier.testTag("edit_staff_confirm_button"),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = palette.onPlum,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(s.get("btn_save", "Save"), fontFamily = font, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.teamCoverageSubItems(
    uiState: LakemarkUiState,
    onToggleWaiterDuty: (String) -> Unit,
    onToggleWaiterArea: (String, WaiterArea) -> Unit,
) {
    val waiters = uiState.waiterUsers
    val coverageWritable = uiState.isAdminUser
    val onDutyWaiters = waiters.filter { w ->
        val cov = uiState.waiterCoverage[w.userId]
        w.active && (cov?.onDuty ?: true)
    }

    item(key = "waiters_header") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = s.get("team_sub_waiters", "Waiters"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = palette.text,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.alt)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "${onDutyWaiters.size}/${waiters.size} ${s.get("orders_on_duty", "On duty")}",
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = palette.text,
                )
            }
        }
    }

    if (waiters.isNotEmpty() && coverageWritable) {
        item(key = "waiters_area_summary") {
            val palette = LocalBrandPalette.current
            val s = LocalAppStrings.current
            val font = lakemarkFont()
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    WaiterArea.entries.forEach { area ->
                        val count = onDutyWaiters.count { w ->
                            val cov = uiState.waiterCoverage[w.userId] ?: WaiterCoverage(w.userId)
                            area in cov.areaSet
                        }
                        val uncovered = count == 0
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (uncovered) StatusCancelledBg else palette.alt)
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = count.toString(),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = if (uncovered) StatusCancelledText else palette.plum,
                            )
                            Text(
                                text = s.waiterAreaLabel(area),
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = if (uncovered) StatusCancelledText else palette.mut,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (waiters.isEmpty()) {
        item(key = "waiters_empty") {
            val palette = LocalBrandPalette.current
            val s = LocalAppStrings.current
            val font = lakemarkFont()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = s.get("orders_empty", "No waiter accounts found."),
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = palette.mut,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    } else {
        items(waiters, key = { "waiter_${it.userId.ifBlank { it.id }}" }) { waiter ->
            WaiterCoverageCard(
                waiter = waiter,
                coverage = uiState.waiterCoverage[waiter.userId] ?: WaiterCoverage(waiter.userId),
                activeLoad = uiState.orders.count {
                    it.assignedWaiterId == waiter.userId && !it.statusEnum.isTerminal
                },
                waiterPalette = uiState.brands[UserRole.WAITER] ?: deriveBrandPalette(UserRole.WAITER.defaultBrandConfig()),
                editable = coverageWritable,
                onToggleDuty = { onToggleWaiterDuty(waiter.userId) },
                onToggleArea = { area -> onToggleWaiterArea(waiter.userId, area) },
            )
        }
    }
}

@Composable
private fun WaiterCoverageCard(
    waiter: StaffUser,
    coverage: WaiterCoverage,
    activeLoad: Int,
    waiterPalette: BrandPalette,
    editable: Boolean,
    onToggleDuty: () -> Unit,
    onToggleArea: (WaiterArea) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val isLive = waiter.active && coverage.onDuty
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLive) 1f else 0.68f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UserAvatar(
                    photoUrl = waiter.photoUrl,
                    name = waiter.name.ifBlank { s.roleName(UserRole.WAITER) },
                    role = waiter.userRole,
                    palette = waiterPalette,
                    size = 40.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = waiter.name.ifBlank { waiter.email.substringBefore("@").ifBlank { s.roleName(UserRole.WAITER) } },
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isLive) StatusReadyBg else StatusCancelledBg)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = when {
                                    isLive -> s.get("orders_on_duty", "On duty")
                                    !waiter.active -> s.get("manage_reg_status_disabled", "Disabled")
                                    else -> s.get("orders_off_duty", "Off duty")
                                },
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = if (isLive) StatusReadyText else StatusCancelledText,
                            )
                        }
                        Text(
                            text = s.format("orders_active_load", "n" to activeLoad),
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = palette.mut,
                        )
                    }
                }
                Switch(
                    checked = isLive,
                    enabled = editable && waiter.active,
                    onCheckedChange = { onToggleDuty() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.acc,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = palette.line,
                    ),
                    modifier = Modifier.testTag("waiter_duty_${waiter.userId}"),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.alt)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WaiterArea.entries.forEach { area ->
                    val selected = area in coverage.areaSet
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) palette.plum else Color.Transparent)
                            .clickable(enabled = editable && waiter.active) { onToggleArea(area) }
                            .padding(horizontal = 4.dp)
                            .testTag("waiter_area_${waiter.userId}_${area.key}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = s.waiterAreaLabel(area),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = if (selected) palette.onPlum else palette.text,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.teamBrandsSubItems(
    uiState: LakemarkUiState,
    onOpenBrandEditor: (UserRole) -> Unit,
) {
    item(key = "role_brands_header") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = s.get("team_sub_brands", "Brands"),
                fontFamily = font,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = palette.text,
            )
            Text(
                text = s.get("settings_my_brand", "Customize role theme"),
                fontFamily = font,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = palette.mut,
            )
        }
    }

    val rolePairs = UserRole.entries.chunked(2)
    items(rolePairs, key = { pair -> "brand_row_${pair.joinToString("_") { it.key }}" }) { pair ->
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            pair.forEach { role ->
                val rolePalette = uiState.brands[role] ?: deriveBrandPalette(role.defaultBrandConfig())
                val customized = !rolePalette.brandName.equals(role.displayName, ignoreCase = true)
                Surface(
                    onClick = { onOpenBrandEditor(role) },
                    shape = RoundedCornerShape(20.dp),
                    color = rolePalette.bg,
                    border = BorderStroke(1.5.dp, rolePalette.plum.copy(alpha = 0.42f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manage_brand_${role.key}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(rolePalette.plum)
                                .border(1.5.dp, rolePalette.acc, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            BrandMarkOrIcon(rolePalette, role, 17.dp, rolePalette.onPlum)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = if (customized) rolePalette.brandName else s.roleName(role),
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = rolePalette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (customized) {
                                Text(
                                    text = s.roleName(role),
                                    fontFamily = font,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = rolePalette.mut,
                                    maxLines = 1,
                                )
                            }
                            SwatchDotsRow(rolePalette)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Customize ${role.displayName}",
                            tint = rolePalette.plum.copy(alpha = 0.72f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (pair.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
