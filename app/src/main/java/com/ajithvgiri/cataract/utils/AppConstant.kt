package com.ajithvgiri.cataract.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

class AppConstant {

}

fun Context.uriToImageFile(uri: Uri): File? {
    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val filePath = cursor.getString(columnIndex)
            cursor.close()
            return File(filePath)
        }
        cursor.close()
    }
    return null
}