package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.SubcomposeAsyncImage
import com.example.BuildConfig
import com.example.data.model.AppReleaseConfig
import com.example.data.model.UserRole
import com.example.localization.AppStrings
import com.example.localization.LocalAppStrings
import com.example.ui.theme.BrandPalette
import com.example.ui.theme.StatusCancelledText
import com.example.util.ApkDownloadState
import com.example.util.ApkUpdateDownloader
import com.example.util.ApkUpdateVerifier
import kotlinx.coroutines.launch
import com.example.ui.theme.LocalBrandPalette
import com.example.ui.theme.lakemarkFont
import com.example.util.MediaImageUploader

@Stable
class MediaPickerController(
    val openGallery: () -> Unit,
    val openCamera: () -> Unit,
)

@Composable
fun rememberMediaImagePicker(
    filePrefix: String = "lakemark",
    onImageSelected: (Uri) -> Unit,
    onCameraPermissionDenied: () -> Unit,
): MediaPickerController {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onImageSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val captured = pendingCameraUri
        pendingCameraUri = null
        if (saved && captured != null) {
            onImageSelected(captured)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = MediaImageUploader.createCameraCaptureUri(context, filePrefix)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            onCameraPermissionDenied()
        }
    }

    return remember(context, filePrefix) {
        MediaPickerController(
            openGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            openCamera = {
                val hasCameraPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCameraPerm) {
                    val uri = MediaImageUploader.createCameraCaptureUri(context, filePrefix)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
        )
    }
}

@Composable
fun UserAvatar(
    photoUrl: String,
    name: String,
    role: UserRole,
    palette: BrandPalette = LocalBrandPalette.current,
    size: Dp = 38.dp,
    isUploading: Boolean = false,
    borderColor: Color = palette.acc,
    modifier: Modifier = Modifier,
) {
    val cleanName = name.trim()
    val initials = cleanName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.plum)
            .border(1.5.dp, borderColor.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = cleanName.ifBlank { role.displayName },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    AvatarFallbackContent(initials = initials, role = role, palette = palette, size = size)
                },
                error = {
                    AvatarFallbackContent(initials = initials, role = role, palette = palette, size = size)
                },
            )
        } else {
            AvatarFallbackContent(initials = initials, role = role, palette = palette, size = size)
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = palette.acc,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(size * 0.45f),
                )
            }
        }
    }
}

@Composable
private fun AvatarFallbackContent(
    initials: String,
    role: UserRole,
    palette: BrandPalette,
    size: Dp,
) {
    if (initials.isNotBlank()) {
        Text(
            text = initials,
            fontFamily = lakemarkFont(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.36f).coerceIn(11f, 24f).sp,
            color = palette.onPlum,
            maxLines = 1,
        )
    } else {
        Icon(
            imageVector = roleIconVector(role),
            contentDescription = role.displayName,
            tint = palette.onPlum,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

@Composable
fun FoodImageBox(
    imageUrl: String,
    category: String,
    dishName: String,
    palette: BrandPalette = LocalBrandPalette.current,
    size: Dp = 56.dp,
    isUploading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(palette.alt)
            .border(1.dp, palette.line.copy(alpha = 0.6f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = dishName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Icon(
                        imageVector = categoryIconVector(category),
                        contentDescription = dishName,
                        tint = palette.plum,
                        modifier = Modifier.size(size * 0.44f),
                    )
                },
                error = {
                    Icon(
                        imageVector = categoryIconVector(category),
                        contentDescription = dishName,
                        tint = palette.plum,
                        modifier = Modifier.size(size * 0.44f),
                    )
                },
            )
        } else {
            Icon(
                imageVector = categoryIconVector(category),
                contentDescription = dishName,
                tint = palette.plum,
                modifier = Modifier.size(size * 0.44f),
            )
        }
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = palette.acc,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(size * 0.4f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignedRoleBadges(
    assignedRole: UserRole,
    activePreviewRole: UserRole,
    strings: AppStrings = LocalAppStrings.current,
    palette: BrandPalette = LocalBrandPalette.current,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.plum)
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .testTag("assigned_role_badge"),
        ) {
            Text(
                text = strings.roleName(assignedRole),
                fontFamily = lakemarkFont(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = palette.onPlum,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (assignedRole == UserRole.MANAGER && activePreviewRole != assignedRole) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.acc.copy(alpha = 0.18f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
                    .testTag("preview_role_badge"),
            ) {
                Text(
                    text = strings.format("profile_preview_badge", "role" to strings.roleName(activePreviewRole)),
                    fontFamily = lakemarkFont(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = palette.plum,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ImageSourcePickerDialog(
    title: String? = null,
    onDismiss: () -> Unit,
    onPickGallery: () -> Unit,
    onPickCamera: () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.bg,
            border = BorderStroke(2.dp, palette.plum),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .testTag("image_source_picker_dialog"),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title ?: s.get("media_picker_title", "Choose image source"),
                    fontFamily = font,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = palette.text,
                )
                Button(
                    onClick = {
                        onDismiss()
                        onPickGallery()
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.plum,
                        contentColor = palette.onPlum,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_gallery_button"),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "  ${s.get("media_picker_gallery", "Choose from gallery")}",
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                }
                Button(
                    onClick = {
                        onDismiss()
                        onPickCamera()
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.acc,
                        contentColor = palette.onAcc,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_camera_button"),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "  ${s.get("media_picker_camera", "Take a photo")}",
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = s.get("btn_cancel", "Cancel"),
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = palette.mut,
                    )
                }
            }
        }
    }
}

@Composable
fun AppUpdateAvailableDialog(
    releaseConfig: AppReleaseConfig?,
    fallbackApkUrl: String = "",
    onDismiss: () -> Unit,
) {
    val palette = LocalBrandPalette.current
    val s = LocalAppStrings.current
    val font = lakemarkFont()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val versionLabel = releaseConfig?.latestVersionName?.ifBlank { null } ?: BuildConfig.VERSION_NAME
    val apkUrl = releaseConfig?.apkDownloadUrl?.ifBlank { null } ?: fallbackApkUrl
    val expectedSha256 = releaseConfig?.apkSha256.orEmpty()
    val expectedMinVersion = (releaseConfig?.latestVersionCode ?: BuildConfig.VERSION_CODE.toLong())
        .coerceAtLeast(BuildConfig.VERSION_CODE.toLong())
    val notes = releaseConfig?.releaseNotes?.ifBlank { null }
        ?: s.get("notif_update_available", "App update available")

    var downloadState by remember(apkUrl, expectedSha256) {
        mutableStateOf<ApkDownloadState>(ApkDownloadState.Idle)
    }

    DisposableEffect(lifecycleOwner, downloadState) {
        val observer = LifecycleEventObserver { _, event ->
            val current = downloadState
            if (event == Lifecycle.Event.ON_RESUME &&
                current is ApkDownloadState.NeedsInstallPermission &&
                ApkUpdateDownloader.canRequestPackageInstalls(context)
            ) {
                downloadState = ApkDownloadState.ReadyToInstall(current.apkFile, current.sha256)
                ApkUpdateDownloader.launchApkInstaller(context, current.apkFile)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isBusy = downloadState is ApkDownloadState.Downloading || downloadState is ApkDownloadState.Verifying
    val isMandatory = releaseConfig?.mandatory == true

    fun startUpdateDownload() {
        if (isBusy) return
        scope.launch {
            ApkUpdateDownloader.downloadAndVerify(
                context = context,
                apkUrl = apkUrl,
                expectedSha256 = expectedSha256,
                expectedMinVersionCode = expectedMinVersion,
                onState = { next ->
                    downloadState = next
                    if (next is ApkDownloadState.ReadyToInstall) {
                        ApkUpdateDownloader.launchApkInstaller(context, next.apkFile)
                    }
                },
            )
        }
    }

    Dialog(
        onDismissRequest = { if (!isMandatory && !isBusy) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isMandatory && !isBusy,
            dismissOnClickOutside = !isMandatory && !isBusy,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.bg,
            border = BorderStroke(2.dp, palette.plum),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .widthIn(max = 460.dp)
                .testTag("app_update_dialog"),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.acc),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = palette.onAcc,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = s.format("notif_update_dialog_title", "ver" to versionLabel),
                        fontFamily = font,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = palette.text,
                    )
                }

                Text(
                    text = notes,
                    fontFamily = font,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = palette.mut,
                )

                when (val state = downloadState) {
                    is ApkDownloadState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = s.get("notif_update_downloading", "Downloading verified APK…"),
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = palette.plum,
                            )
                            val frac = state.progressFraction
                            if (frac != null) {
                                LinearProgressIndicator(
                                    progress = { frac },
                                    color = palette.acc,
                                    trackColor = palette.alt,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                LinearProgressIndicator(
                                    color = palette.acc,
                                    trackColor = palette.alt,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    ApkDownloadState.Verifying -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = s.get("notif_update_verifying", "Verifying APK integrity & signature…"),
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = palette.plum,
                            )
                            LinearProgressIndicator(
                                color = palette.acc,
                                trackColor = palette.alt,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    is ApkDownloadState.ReadyToInstall -> {
                        Text(
                            text = s.get("notif_update_install_ready", "Verified · Opening Android installer"),
                            fontFamily = font,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = palette.plum,
                        )
                    }
                    is ApkDownloadState.NeedsInstallPermission -> {
                        Text(
                            text = s.get(
                                "notif_update_allow_install",
                                "Allow Lakemark to install verified updates in Android Settings.",
                            ),
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = palette.plum,
                        )
                    }
                    is ApkDownloadState.WebpageFallback -> {
                        Text(
                            text = s.get("notif_update_open_browser", "Open download page"),
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = palette.plum,
                        )
                    }
                    is ApkDownloadState.Failed -> {
                        Text(
                            text = state.errorMessage,
                            fontFamily = font,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = StatusCancelledText,
                            modifier = Modifier.testTag("app_update_error_text"),
                        )
                    }
                    ApkDownloadState.Idle -> Unit
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!isMandatory && !isBusy) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.alt,
                                contentColor = palette.text,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("app_update_later_button"),
                        ) {
                            Text(s.get("btn_later", "Later"), fontFamily = font, fontWeight = FontWeight.Bold)
                        }
                    }

                    when (val state = downloadState) {
                        is ApkDownloadState.ReadyToInstall -> {
                            Button(
                                onClick = { ApkUpdateDownloader.launchApkInstaller(context, state.apkFile) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("app_update_download_button"),
                            ) {
                                Text(
                                    text = s.get("notif_update_now", "Update Now"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        is ApkDownloadState.NeedsInstallPermission -> {
                            Button(
                                onClick = { ApkUpdateDownloader.openUnknownSourcesSettings(context) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("app_update_download_button"),
                            ) {
                                Text(
                                    text = s.get("notif_update_allow_install", "Allow app installs"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        is ApkDownloadState.WebpageFallback -> {
                            Button(
                                onClick = {
                                    if (ApkUpdateVerifier.isValidHttpsUrl(state.url)) {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.url)))
                                        }
                                    }
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("app_update_download_button"),
                            ) {
                                Text(
                                    text = s.get("notif_update_open_browser", "Open download page"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        else -> if (apkUrl.isNotBlank()) {
                            Button(
                                onClick = { startUpdateDownload() },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("app_update_download_button"),
                            ) {
                                Text(
                                    text = s.get("notif_update_now", "Update Now"),
                                    fontFamily = font,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.acc,
                                    contentColor = palette.onAcc,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text(s.get("btn_got_it", "Got it"), fontFamily = font, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
