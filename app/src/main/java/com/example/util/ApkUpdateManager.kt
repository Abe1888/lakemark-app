package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

enum class ApkVerifyFailure {
    INVALID_PACKAGE,
    PACKAGE_MISMATCH,
    VERSION_DOWNGRADE,
    SIGNING_MISMATCH,
    CHECKSUM_MISMATCH,
}

sealed interface ApkArchiveCheckResult {
    data object Valid : ApkArchiveCheckResult
    data class Invalid(val reason: ApkVerifyFailure) : ApkArchiveCheckResult
}

sealed interface ApkDownloadState {
    data object Idle : ApkDownloadState
    data class Downloading(val progressFraction: Float?, val bytesRead: Long, val totalBytes: Long) : ApkDownloadState
    data object Verifying : ApkDownloadState
    data class ReadyToInstall(val apkFile: File, val sha256: String) : ApkDownloadState
    data class NeedsInstallPermission(val apkFile: File, val sha256: String) : ApkDownloadState
    data class WebpageFallback(val url: String) : ApkDownloadState
    data class Failed(val errorMessage: String) : ApkDownloadState
}

object ApkUpdateVerifier {
    private val sha256HexRegex = Regex("^[a-f0-9]{64}$")

    fun isValidHttpsUrl(rawUrl: String): Boolean {
        val clean = rawUrl.trim()
        if (clean.isEmpty()) return false
        return try {
            val uri = URI(clean)
            uri.scheme?.equals("https", ignoreCase = true) == true &&
                !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeSha256(raw: String): String =
        raw.trim().lowercase().replace(":", "").replace(" ", "")

    fun isValidSha256Hex(raw: String): Boolean =
        sha256HexRegex.matches(normalizeSha256(raw))

    fun verifyChecksum(actualSha256: String, expectedSha256: String): Boolean {
        val normalizedExpected = normalizeSha256(expectedSha256)
        if (normalizedExpected.isEmpty()) return true
        if (!sha256HexRegex.matches(normalizedExpected)) return false
        return normalizeSha256(actualSha256) == normalizedExpected
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computeSha256Hex(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        FileInputStream(file).use { input ->
            var read = input.read(buffer)
            while (read != -1) {
                md.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyArchiveCompatibility(
        archivePackageName: String?,
        archiveVersionCode: Long,
        archiveCertSha256s: Set<String>,
        expectedPackageName: String,
        minVersionCode: Long,
        installedCertSha256s: Set<String>,
    ): ApkArchiveCheckResult {
        if (archivePackageName.isNullOrBlank()) {
            return ApkArchiveCheckResult.Invalid(ApkVerifyFailure.INVALID_PACKAGE)
        }
        if (archivePackageName != expectedPackageName) {
            return ApkArchiveCheckResult.Invalid(ApkVerifyFailure.PACKAGE_MISMATCH)
        }
        if (archiveVersionCode < minVersionCode) {
            return ApkArchiveCheckResult.Invalid(ApkVerifyFailure.VERSION_DOWNGRADE)
        }
        if (installedCertSha256s.isNotEmpty() && archiveCertSha256s.none { it in installedCertSha256s }) {
            return ApkArchiveCheckResult.Invalid(ApkVerifyFailure.SIGNING_MISMATCH)
        }
        return ApkArchiveCheckResult.Valid
    }
}

object ApkUpdateDownloader {
    private const val MAX_APK_BYTES: Long = 200L * 1024L * 1024L

    fun canRequestPackageInstalls(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun openUnknownSourcesSettings(context: Context): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val packageIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val startedPackage = runCatching { context.startActivity(packageIntent) }.isSuccess
            if (!startedPackage) {
                val appDetailsIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appDetailsIntent)
            }
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Result<Unit> = runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    suspend fun downloadAndVerify(
        context: Context,
        apkUrl: String,
        expectedSha256: String,
        expectedMinVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
        onState: (ApkDownloadState) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val cleanUrl = apkUrl.trim()
        if (!ApkUpdateVerifier.isValidHttpsUrl(cleanUrl)) {
            onState(ApkDownloadState.Failed("Update download requires a valid HTTPS link."))
            return@withContext
        }
        val cleanExpectedSha = ApkUpdateVerifier.normalizeSha256(expectedSha256)
        if (cleanExpectedSha.isNotEmpty() && !ApkUpdateVerifier.isValidSha256Hex(cleanExpectedSha)) {
            onState(ApkDownloadState.Failed("Invalid SHA-256 checksum configured for this release."))
            return@withContext
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val tempFile = File(updatesDir, "lakemark-update.apk.tmp")
        val finalFile = File(updatesDir, "lakemark-update.apk")
        tempFile.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = openHttpsConnectionFollowingRedirects(cleanUrl)
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                onState(ApkDownloadState.Failed("Download failed (HTTP $responseCode)."))
                return@withContext
            }

            val contentType = connection.contentType.orEmpty().lowercase()
            if (contentType.startsWith("text/html")) {
                onState(ApkDownloadState.WebpageFallback(cleanUrl))
                return@withContext
            }

            val totalBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connection.contentLengthLong
            } else {
                connection.contentLength.toLong()
            }
            if (totalBytes > MAX_APK_BYTES) {
                onState(ApkDownloadState.Failed("Update file exceeds maximum allowed size (200 MB)."))
                return@withContext
            }

            onState(ApkDownloadState.Downloading(progressFraction = null, bytesRead = 0L, totalBytes = totalBytes))

            var bytesReadTotal = 0L
            var checkedHeader = false
            var isZipHeader = false
            val buffer = ByteArray(16 * 1024)

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        if (!checkedHeader && read >= 2) {
                            checkedHeader = true
                            isZipHeader = buffer[0] == 0x50.toByte() && buffer[1] == 0x4B.toByte()
                            if (!isZipHeader) {
                                tempFile.delete()
                                onState(ApkDownloadState.WebpageFallback(cleanUrl))
                                return@withContext
                            }
                        }
                        bytesReadTotal += read
                        if (bytesReadTotal > MAX_APK_BYTES) {
                            tempFile.delete()
                            onState(ApkDownloadState.Failed("Update file exceeds maximum allowed size (200 MB)."))
                            return@withContext
                        }
                        output.write(buffer, 0, read)
                        val fraction = if (totalBytes > 0L) {
                            (bytesReadTotal.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            null
                        }
                        onState(ApkDownloadState.Downloading(fraction, bytesReadTotal, totalBytes))
                    }
                    output.flush()
                }
            }

            if (bytesReadTotal <= 0L || !isZipHeader) {
                tempFile.delete()
                onState(ApkDownloadState.Failed("Downloaded file is not a valid Android APK package."))
                return@withContext
            }

            onState(ApkDownloadState.Verifying)
            val actualSha256 = ApkUpdateVerifier.computeSha256Hex(tempFile)
            if (!ApkUpdateVerifier.verifyChecksum(actualSha256, cleanExpectedSha)) {
                tempFile.delete()
                onState(ApkDownloadState.Failed("APK integrity check failed (SHA-256 mismatch)."))
                return@withContext
            }

            val minCode = maxOf(BuildConfig.VERSION_CODE.toLong(), expectedMinVersionCode)
            when (val check = verifyArchiveWithPackageManager(context, tempFile, minCode)) {
                ApkArchiveCheckResult.Valid -> Unit
                is ApkArchiveCheckResult.Invalid -> {
                    tempFile.delete()
                    val reason = when (check.reason) {
                        ApkVerifyFailure.INVALID_PACKAGE -> "Downloaded APK archive could not be parsed."
                        ApkVerifyFailure.PACKAGE_MISMATCH -> "APK package ID does not match Lakemark."
                        ApkVerifyFailure.VERSION_DOWNGRADE -> "Downloaded APK version is older than required."
                        ApkVerifyFailure.SIGNING_MISMATCH -> "APK signing certificate does not match this app."
                        ApkVerifyFailure.CHECKSUM_MISMATCH -> "APK checksum verification failed."
                    }
                    onState(ApkDownloadState.Failed(reason))
                    return@withContext
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            if (canRequestPackageInstalls(context)) {
                onState(ApkDownloadState.ReadyToInstall(finalFile, actualSha256))
            } else {
                onState(ApkDownloadState.NeedsInstallPermission(finalFile, actualSha256))
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            onState(ApkDownloadState.Failed(e.localizedMessage ?: "Failed to download APK update."))
        } finally {
            connection?.disconnect()
        }
    }

    private fun openHttpsConnectionFollowingRedirects(startUrl: String, maxRedirects: Int = 5): HttpURLConnection {
        var currentUrl = startUrl
        repeat(maxRedirects + 1) {
            if (!ApkUpdateVerifier.isValidHttpsUrl(currentUrl)) {
                throw SecurityException("Blocked non-HTTPS update redirect.")
            }
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 45_000
                setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            }
            val code = conn.responseCode
            if (code in setOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308,
                )
            ) {
                val location = conn.getHeaderField("Location").orEmpty().trim()
                conn.disconnect()
                if (location.isBlank()) {
                    throw IllegalStateException("Redirect missing Location header.")
                }
                currentUrl = URI(currentUrl).resolve(location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many redirects while downloading APK.")
    }

    private fun verifyArchiveWithPackageManager(
        context: Context,
        apkFile: File,
        minVersionCode: Long,
    ): ApkArchiveCheckResult {
        val pm = context.packageManager
        val archiveInfo = getArchivePackageInfo(pm, apkFile.absolutePath)
            ?: return ApkArchiveCheckResult.Invalid(ApkVerifyFailure.INVALID_PACKAGE)
        val installedInfo = getInstalledPackageInfo(pm, context.packageName)

        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        val archiveCerts = extractCertSha256s(archiveInfo)
        val installedCerts = installedInfo?.let(::extractCertSha256s).orEmpty()

        return ApkUpdateVerifier.verifyArchiveCompatibility(
            archivePackageName = archiveInfo.packageName,
            archiveVersionCode = archiveVersionCode,
            archiveCertSha256s = archiveCerts,
            expectedPackageName = context.packageName,
            minVersionCode = minVersionCode,
            installedCertSha256s = installedCerts,
        )
    }

    @Suppress("DEPRECATION")
    private fun getArchivePackageInfo(pm: PackageManager, path: String): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                path,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfo(pm: PackageManager, packageName: String): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun extractCertSha256s(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo
            if (signingInfo != null) {
                signingInfo.apkContentsSigners
            } else {
                info.signatures
            }
        } else {
            info.signatures
        }
        return signatures?.map { ApkUpdateVerifier.sha256Hex(it.toByteArray()) }?.toSet() ?: emptySet()
    }
}
