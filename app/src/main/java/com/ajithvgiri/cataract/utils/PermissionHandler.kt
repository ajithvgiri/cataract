package com.ajithvgiri.cataract.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat


object PermissionHandler {

    val galleryPermission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun shouldAskPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    fun checkPermission(context: Context, permission: String, completion: PermissionCheckCompletion) {
        // If permission is not granted
        if (shouldAskPermission(context, permission)) {
            //If permission denied previously
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (context as Activity).shouldShowRequestPermissionRationale(permission)
                } else {
                    TODO("VERSION.SDK_INT < M")
                }
            ) {
                completion(CheckPermissionResult.PermissionPreviouslyDenied)
            } else {
                // Permission denied or first time requested
                completion(CheckPermissionResult.PermissionAsk)
//                if (PreferenceUtils.isFirstTimeAskingPermission(context, permission)) {
//                    PreferenceUtils.firstTimeAskingPermission(context, permission, false)
//                } else {
//                    // Handle the feature without permission or ask user to manually allow permission
//                    completion(CheckPermissionResult.PermissionDisabled)
//                }
            }
        } else {
            completion(CheckPermissionResult.PermissionGranted)
        }
    }

    fun checkPermission(context: Context, permission: Array<String>, completion: PermissionCheckCompletion) {
        var isAllPermissionGranted: Boolean
        var i = 0
        permission.forEach {
            i++
            // If permission is not granted
            if (shouldAskPermission(context, it)) {
                //If permission denied previously
                isAllPermissionGranted = if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        (context as Activity).shouldShowRequestPermissionRationale(it)
                    } else {
                        TODO("VERSION.SDK_INT < M")
                    }
                ) {
                    false
                    //completion(CheckPermissionResult.PermissionPreviouslyDenied)
                } else {
                    // Permission denied or first time requested
                    //completion(CheckPermissionResult.PermissionAsk)
                    false
                }
            } else {
                isAllPermissionGranted = true
            }

            if (i == permission.size) {
                if (isAllPermissionGranted) {
                    completion(CheckPermissionResult.PermissionGranted)
                } else {
                    completion(CheckPermissionResult.PermissionAsk)
                }
            }
        }
    }
}

enum class CheckPermissionResult {
    PermissionAsk,
    PermissionPreviouslyDenied,
    PermissionDisabled,
    PermissionGranted
}

typealias PermissionCheckCompletion = (CheckPermissionResult) -> Unit
