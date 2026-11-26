package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale

enum class ImageValidationError {
    UNSUPPORTED_TYPE,
    TOO_LARGE,
    UNREADABLE,
}

sealed class ImageValidationResult {
    data class Valid(val mimeType: String) : ImageValidationResult()
    data class Invalid(val error: ImageValidationError) : ImageValidationResult()
}

class MediaUploadException(
    val code: ImageValidationError? = null,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class MediaImageUploader(
    private val context: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {

    companion object {
        const val MAX_UPLOAD_BYTES: Long = 5L * 1024L * 1024L
        const val TARGET_MAX_DIMENSION = 1024
        const val TARGET_SOFT_BYTES = 1_500_000
        private val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

        fun validateImageSpec(mimeType: String?, byteSize: Long?): ImageValidationResult {
            val normalizedMime = mimeType?.trim()?.lowercase(Locale.US).orEmpty()
            if (normalizedMime.isNotEmpty() && normalizedMime !in SUPPORTED_MIME_TYPES) {
                return ImageValidationResult.Invalid(ImageValidationError.UNSUPPORTED_TYPE)
            }
            if (byteSize != null && byteSize > MAX_UPLOAD_BYTES) {
                return ImageValidationResult.Invalid(ImageValidationError.TOO_LARGE)
            }
            val effectiveMime = if (normalizedMime.isEmpty()) "image/jpeg" else normalizedMime
            return ImageValidationResult.Valid(effectiveMime)
        }

        fun createCameraCaptureUri(context: Context, prefix: String = "photo"): Uri {
            val mediaDir = File(context.cacheDir, "media").apply { mkdirs() }
            val photoFile = File.createTempFile("${prefix}_${System.currentTimeMillis()}_", ".jpg", mediaDir)
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile,
            )
        }
    }

    suspend fun uploadProfilePhoto(
        uid: String,
        sourceUri: Uri,
        previousUrl: String = "",
    ): Result<String> {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) {
            return Result.failure(MediaUploadException(null, "Missing signed-in user ID"))
        }
        val path = "users/$cleanUid/profile/avatar_${System.currentTimeMillis()}.jpg"
        return uploadCompressedImage(sourceUri, path, previousUrl)
    }

    suspend fun uploadFoodImage(
        itemId: String,
        sourceUri: Uri,
        previousUrl: String = "",
    ): Result<String> {
        val cleanId = itemId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        if (cleanId.isBlank()) {
            return Result.failure(MediaUploadException(null, "Missing menu item ID"))
        }
        val path = "menu_items/$cleanId/dish_${System.currentTimeMillis()}.jpg"
        return uploadCompressedImage(sourceUri, path, previousUrl)
    }

    suspend fun deleteStorageUrlSafely(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        if (!trimmed.startsWith("https://firebasestorage.googleapis.com") &&
            !trimmed.startsWith("gs://")
        ) {
            return
        }
        withContext(Dispatchers.IO) {
            runCatching {
                storage.getReferenceFromUrl(trimmed).delete().await()
            }
        }
    }

    private suspend fun uploadCompressedImage(
        sourceUri: Uri,
        storagePath: String,
        previousUrl: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val detectedMime = resolveMimeType(resolver, sourceUri)
            val fileSize = resolveFileSize(resolver, sourceUri)
            when (val specCheck = validateImageSpec(detectedMime, fileSize)) {
                is ImageValidationResult.Invalid -> {
                    return@withContext Result.failure(
                        MediaUploadException(
                            code = specCheck.error,
                            message = when (specCheck.error) {
                                ImageValidationError.UNSUPPORTED_TYPE -> "Unsupported image type: $detectedMime"
                                ImageValidationError.TOO_LARGE -> "Image exceeds 5 MB limit"
                                ImageValidationError.UNREADABLE -> "Could not read image"
                            },
                        )
                    )
                }
                is ImageValidationResult.Valid -> Unit
            }

            val jpegBytes = decodeDownscaleAndCompress(resolver, sourceUri)
                ?: return@withContext Result.failure(
                    MediaUploadException(ImageValidationError.UNREADABLE, "Unable to decode selected image")
                )

            if (jpegBytes.size.toLong() > MAX_UPLOAD_BYTES) {
                return@withContext Result.failure(
                    MediaUploadException(ImageValidationError.TOO_LARGE, "Compressed image exceeds 5 MB")
                )
            }

            val ref = storage.reference.child(storagePath)
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            ref.putBytes(jpegBytes, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            if (previousUrl.isNotBlank() && previousUrl != downloadUrl) {
                deleteStorageUrlSafely(previousUrl)
            }
            Result.success(downloadUrl)
        } catch (e: MediaUploadException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                MediaUploadException(null, e.localizedMessage ?: "Upload failed", e)
            )
        }
    }

    private fun resolveMimeType(resolver: ContentResolver, uri: Uri): String? {
        val direct = resolver.getType(uri)
        if (!direct.isNullOrBlank()) return direct
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase(Locale.US)
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
    }

    private fun resolveFileSize(resolver: ContentResolver, uri: Uri): Long? {
        return runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val len = afd.length
                if (len >= 0L) len else null
            }
        }.getOrNull()
    }

    private fun decodeDownscaleAndCompress(resolver: ContentResolver, uri: Uri): ByteArray? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        } ?: return null

        val srcW = boundsOpts.outWidth
        val srcH = boundsOpts.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var sampleSize = 1
        while (srcW / sampleSize > TARGET_MAX_DIMENSION * 2 || srcH / sampleSize > TARGET_MAX_DIMENSION * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val oriented = applyExifOrientation(resolver, uri, decoded)
        val scaled = scaleDownIfNeeded(oriented, TARGET_MAX_DIMENSION)
        if (scaled !== oriented) oriented.recycle()
        if (oriented !== decoded) decoded.recycle()

        var quality = 88
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > TARGET_SOFT_BYTES && quality > 52) {
            out.reset()
            quality -= 10
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        scaled.recycle()
        return out.toByteArray()
    }

    private fun applyExifOrientation(resolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { input: InputStream ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrElse { bitmap }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w.toFloat(), maxDim.toFloat() / h.toFloat())
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
