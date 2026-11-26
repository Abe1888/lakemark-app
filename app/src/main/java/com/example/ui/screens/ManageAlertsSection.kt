package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.AppNotification
import com.example.data.model.UserRole
import com.example.localization.LocalAppStrings
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.deriveBrandPalette
import com.example.ui.theme.lakemarkFont
import com.example.ui.viewmodel.LakemarkUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActivityLogFilter(val key: String, val label: String) {
    ALL("all", "All"),
    BROADCASTS("broadcasts", "Broadcasts"),
    ORDERS("orders", "Order alerts"),
}

internal fun LazyListScope.alertsSectionItems(
    uiState: LakemarkUiState,
    broadcastTarget: String,
    onSelectBroadcastTarget: (String) -> Unit,
    broadcastMsg: String,
    onBroadcastMsgChange: (String) -> Unit,
    onSendBroadcast: () -> Unit,
    activityFilter: ActivityLogFilter,
    onSelectActivityFilter: (ActivityLogFilter) -> Unit,
    onOpenNotification: (AppNotification) -> Unit,
    onPublishReleaseUpdate: (versionCode: Long, versionName: String, apkDownloadUrl: String, releaseNotes: String, mandatory: Boolean, apkSha256: String, onDone: () -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
) {
    item(key = "send_to_staff_panel") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        val targets = remember(s) {
            listOf("all" to s.get("manage_alerts_everyone", "Everyone")) +
                UserRole.entries.map { it.key to s.roleName(it) }
        }
        val selectedLabel = targets.firstOrNull { it.first == broadcastTarget }?.second
            ?: s.get("manage_alerts_everyone", "Everyone")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(palette.gradTop, palette.gradBottom)))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = palette.acc,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = s.get("manage_alerts_send_title", "Send to staff"),
                            fontFamily = font,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = palette.onPlum,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = s.format("manage_alerts_to_prefix", "target" to selectedLabel),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = palette.onPlum,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    targets.chunked(3).forEach { rowTargets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowTargets.forEach { (key, label) ->
                                val selected = broadcastTarget == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) palette.acc else Color.White.copy(alpha = 0.12f))
                                        .clickable { onSelectBroadcastTarget(key) }
                                        .padding(horizontal = 6.dp)
                                        .testTag("broadcast_target_$key"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = font,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (selected) palette.onAcc else palette.onPlum.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            repeat(3 - rowTargets.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = broadcastMsg,
                    onValueChange = onBroadcastMsgChange,
                    placeholder = {
                        Text(
                            text = s.format("manage_alerts_hint", "target" to selectedLabel),
                            color = palette.onPlum.copy(alpha = 0.65f),
                            fontFamily = font,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.18f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                        focusedBorderColor = palette.acc,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("broadcast_message_input"),
                )

                Button(
                    onClick = onSendBroadcast,
                    enabled = broadcastMsg.isNotBlank(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.acc,
                        contentColor = palette.onAcc,
                        disabledContainerColor = palette.acc.copy(alpha = 0.35f),
                        disabledContentColor = palette.onAcc.copy(alpha = 0.6f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_broadcast_button"),
                ) {
                    Text(
                        text = s.get("manage_alerts_send_btn", "Send notification"),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }

    if (uiState.isAdminUser) {
        item(key = "release_update_panel") {
            ReleaseUpdatePanel(
                uiState = uiState,
                onPublishReleaseUpdate = onPublishReleaseUpdate,
            )
        }
    }

    val filteredAlerts = uiState.notifications
        .filter { notif ->
            when (activityFilter) {
                ActivityLogFilter.ALL -> true
                ActivityLogFilter.BROADCASTS -> notif.orderId.isBlank()
                ActivityLogFilter.ORDERS -> notif.orderId.isNotBlank()
            }
        }
        .take(20)

    item(key = "live_activity_header") {
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
                text = s.get("manage_alerts_activity", "Operational activity"),
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
                    text = "${filteredAlerts.size}",
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = palette.text,
                )
            }
        }
    }

    item(key = "live_activity_filters") {
        val palette = LocalBrandPalette.current
        val s = LocalAppStrings.current
        val font = lakemarkFont()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityLogFilter.entries.forEach { filter ->
                val selected = activityFilter == filter
                val filterLabel = when (filter) {
                    ActivityLogFilter.ALL -> s.get("cat_all", "All")
                    ActivityLogFilter.BROADCASTS -> s.get("manage_tab_alerts", "Broadcasts")
                    ActivityLogFilter.ORDERS -> s.get("nav_orders", "Order alerts")
                }
                Surface(
                    onClick = { onSelectActivityFilter(filter) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) palette.plum else palette.alt,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("alerts_filter_${filter.key}"),
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

    if (filteredAlerts.isEmpty()) {
        item(key = "live_activity_empty") {
            val palette = LocalBrandPalette.current
            val s = LocalAppStrings.current
            val font = lakemarkFont()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        tint = palette.mut,
                    )
                    Text(
                        text = s.get("notif_empty", "No notifications yet."),
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = palette.mut,
                    )
                }
            }
        }
    } else {
        items(filteredAlerts, key = { "notif_${it.id.ifBlank { "${it.orderId}_${it.message.hashCode()}" }}" }) { notif ->
            val s = LocalAppStrings.current
            val font = lakemarkFont()
            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            val senderRole = UserRole.fromKey(notif.senderRole)
            val senderPalette = uiState.pushBrandOverrides[notif.id]
                ?: uiState.brands[senderRole]
                ?: deriveBrandPalette(senderRole.defaultBrandConfig())
            val timeText = notif.createdAt?.toDate()?.let { timeFormat.format(it) } ?: timeFormat.format(Date())
            val canOpenOrder = notif.orderId.isNotBlank() || notif.kind == "app_update"
            val audienceTag = when {
                notif.kind == "app_update" -> s.get("notif_update_now", "Update Now")
                notif.orderId.isNotBlank() -> s.get("nav_orders", "Orders")
                else -> {
                    val target = notif.targetRoles.firstOrNull().orEmpty()
                    if (target.isBlank() || target == "all") {
                        s.get("manage_alerts_everyone", "Everyone")
                    } else {
                        s.roleName(UserRole.fromKey(target))
                    }
                }
            }

            Surface(
                onClick = { if (canOpenOrder) onOpenNotification(notif) },
                enabled = canOpenOrder,
                shape = RoundedCornerShape(20.dp),
                color = senderPalette.bg,
                border = BorderStroke(1.dp, senderPalette.plum.copy(alpha = 0.3f)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 38.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(senderPalette.plum),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = notif.senderBrandName.ifBlank { s.roleName(senderRole) },
                                fontFamily = font,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = senderPalette.text,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(senderPalette.acc.copy(alpha = 0.22f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = audienceTag,
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = senderPalette.plum,
                                )
                            }
                        }
                        Text(
                            text = notif.message,
                            fontFamily = font,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = senderPalette.mut,
                        )
                    }
                    Text(
                        text = timeText,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = senderPalette.mut,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseUpdatePanel(
    uiState: LakemarkUiState,
    onPublishReleaseUpdate: (versionCode: Long, versionName: String, apkDownloadUrl: String, releaseNotes: String, mandatory: Boolean, apkSha256: String, onDone: () -> Unit) -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val cfg = uiState.releaseConfig

    var versionName by remember(cfg?.latestVersionName) {
        mutableStateOf(cfg?.latestVersionName?.ifBlank { null } ?: BuildConfig.VERSION_NAME)
    }
    var versionCodeText by remember(cfg?.latestVersionCode) {
        mutableStateOf(
            ((cfg?.latestVersionCode ?: BuildConfig.VERSION_CODE.toLong()).coerceAtLeast(BuildConfig.VERSION_CODE.toLong())).toString(),
        )
    }
    var apkUrl by remember(cfg?.apkDownloadUrl) { mutableStateOf(cfg?.apkDownloadUrl.orEmpty()) }
    var apkSha256 by remember(cfg?.apkSha256) { mutableStateOf(cfg?.apkSha256.orEmpty()) }
    var releaseNotes by remember(cfg?.releaseNotes) { mutableStateOf(cfg?.releaseNotes.orEmpty()) }
    var mandatory by remember(cfg?.mandatory) { mutableStateOf(cfg?.mandatory ?: false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manage_release_update_panel"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = palette.plum,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.get("manage_release_title", "Release Update & Delivery"),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp,
                        color = palette.text,
                    )
                    Text(
                        text = s.get("manage_release_desc", "Notify active users when a new APK release is available."),
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = palette.mut,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it.take(24) },
                    label = { Text(s.get("manage_release_version_name", "Version name"), fontFamily = font) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = versionCodeText,
                    onValueChange = { raw -> versionCodeText = raw.filter { it.isDigit() }.take(6) },
                    label = { Text(s.get("manage_release_version_code", "Version code"), fontFamily = font) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = apkUrl,
                onValueChange = { apkUrl = it },
                label = { Text(s.get("manage_release_apk_url", "APK download URL"), fontFamily = font) },
                placeholder = { Text("https://...", fontFamily = font) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = apkSha256,
                onValueChange = { apkSha256 = it.take(128) },
                label = { Text(s.get("manage_release_sha256", "APK SHA-256 (optional)"), fontFamily = font) },
                placeholder = { Text("64-char hex checksum", fontFamily = font) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("release_sha256_input"),
            )

            OutlinedTextField(
                value = releaseNotes,
                onValueChange = { releaseNotes = it },
                label = { Text(s.get("manage_release_notes", "Release notes"), fontFamily = font) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = s.get("manage_release_mandatory", "Mandatory update prompt"),
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = palette.text,
                )
                Switch(
                    checked = mandatory,
                    onCheckedChange = { mandatory = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = palette.acc, checkedThumbColor = Color.White),
                )
            }

            val parsedCode = versionCodeText.toLongOrNull() ?: 0L
            Button(
                onClick = {
                    onPublishReleaseUpdate(
                        parsedCode,
                        versionName.trim(),
                        apkUrl.trim(),
                        releaseNotes.trim(),
                        mandatory,
                        apkSha256.trim(),
                        {},
                    )
                },
                enabled = parsedCode > 0L && versionName.isNotBlank(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.plum,
                    contentColor = palette.onPlum,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("publish_release_update_button"),
            ) {
                Text(
                    text = s.get("manage_release_publish_btn", "Publish & notify users"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                )
            }
        }
    }
}
