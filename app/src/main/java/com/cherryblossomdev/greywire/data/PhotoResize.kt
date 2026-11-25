package com.cherryblossomdev.greywire.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** The longest side of an uploaded profile photo. The web shows it at 150px or less. */
private const val MAX_PHOTO_SIDE = 1024

/**
 * Reads the image at [uri] and returns it as a JPEG no bigger than
 * [MAX_PHOTO_SIDE] on its longest side, well under the backend's 5 MB limit
 * (phone camera photos often exceed it). Android 9+ applies the photo's EXIF
 * rotation; older versions upload it as stored. Run it off the main thread.
 */
fun resizePhotoForUpload(resolver: ContentResolver, uri: Uri): ByteArray {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val scale = scaleFor(info.size.width, info.size.height)
            decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("That file isn't an image the app can read.")
        val scale = scaleFor(decoded.width, decoded.height)
        if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).roundToInt(), (decoded.height * scale).roundToInt(), true)
        } else {
            decoded
        }
    }
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
}

private fun scaleFor(width: Int, height: Int): Float =
    (MAX_PHOTO_SIDE.toFloat() / max(width, height)).coerceAtMost(1f)
