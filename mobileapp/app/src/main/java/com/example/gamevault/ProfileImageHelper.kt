package com.example.gamevault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ProfileImageHelper {

    private const val PROFILE_FOLDER = "images/perfil"

    fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            // 1. Crear el directorio si no existe (Punto 1: Subcarpetas)
            val directory = File(context.filesDir, PROFILE_FOLDER)
            if (!directory.exists()) directory.mkdirs()

            // 2. Crear nombre único para el archivo
            val fileName = "profile_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)

            // 3. Leer la imagen desde el Uri
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // 4. Compresión (Punto 4: Estrategia de Rendimiento)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()

            // Devolver la ruta absoluta (Punto 2: Gestión de Referencias)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
