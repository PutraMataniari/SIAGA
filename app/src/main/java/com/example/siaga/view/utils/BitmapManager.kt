package com.example.siaga.view.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

object BitmapManager {

    /**
     * Mengubah Base64 menjadi Bitmap
     */
    fun base64ToBitmap(base64: String): Bitmap {
        val decodedString = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    /**
     * Mengubah Bitmap menjadi Base64
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Proses gambar lengkap:
     * 1. Ambil foto dari URI
     * 2. Rotasi otomatis sesuai EXIF kamera
     * 3. Resize agar tidak memberatkan memori
     * 4. Konversi otomatis ke Base64
     */
    fun processImage(context: Context, uri: Uri): PhotoResult? {
        return try {
            val file = File(uri.path ?: return null)
            if (!file.exists()) return null

            // Decode foto asli
            var bitmap = BitmapFactory.decodeFile(file.absolutePath)

            // Rotasi sesuai EXIF kamera
            try {
                val exif = ExifInterface(file.absolutePath)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                bitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height, matrix, true
                )
            } catch (_: IOException) { }

            // Resize agar lebih ringan → maksimal lebar 800px
            val maxWidth = 800
            val ratio = maxWidth.toFloat() / bitmap.width
            val newHeight = (bitmap.height * ratio).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)

            // Konversi ke Base64
            val base64 = bitmapToBase64(scaledBitmap)

            // Kembalikan hasil lengkap
            PhotoResult(scaledBitmap, base64, file.absolutePath)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Data hasil pemrosesan foto
     */
    data class PhotoResult(
        val bitmap: Bitmap,
        val base64: String,
        val filePath: String
    )
}
