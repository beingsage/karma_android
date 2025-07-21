package com.technource.android.module.miscModule.miscscreen.Notes.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = File(context.getExternalFilesDir(null), "images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = createImageFile(context)
            val outputStream = FileOutputStream(file)
            
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    fun copyFileToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "FILE_${timeStamp}"
            val storageDir = File(context.getExternalFilesDir(null), "documents")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val file = File(storageDir, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}