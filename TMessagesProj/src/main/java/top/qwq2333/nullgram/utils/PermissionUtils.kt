/*
 * Copyright (C) 2019-2023 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package top.qwq2333.nullgram.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import org.telegram.messenger.ApplicationLoader
import org.telegram.ui.BasePermissionsActivity


object PermissionUtils {
    @JvmStatic
    fun isPermissionGranted(permission: String): Boolean {
        return ApplicationLoader.applicationContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun isImagesPermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES) && isPermissionGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        isStoragePermissionGranted()
    }


    @JvmStatic
    fun isVideoPermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO) && isPermissionGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        isStoragePermissionGranted()
    }

    @JvmStatic
    fun isAudioPermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        isPermissionGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) && isPermissionGranted(Manifest.permission.READ_MEDIA_AUDIO)
    } else  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isPermissionGranted(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        isStoragePermissionGranted()
    }

    @JvmStatic
    fun isRecordAudioPermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        isPermissionGranted(Manifest.permission.RECORD_AUDIO) && isPermissionGranted(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
    } else {
        isPermissionGranted(Manifest.permission.RECORD_AUDIO)
    }

    @JvmStatic
    fun isCameraPermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        isPermissionGranted(Manifest.permission.CAMERA) && isPermissionGranted(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
    } else {
        isPermissionGranted(Manifest.permission.CAMERA)
    }

    @JvmStatic
    fun isStoragePermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isImagesPermissionGranted() && isVideoPermissionGranted() && isAudioPermissionGranted()
    } else {
        isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @JvmStatic
    fun isImagesAndVideoPermissionGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isImagesPermissionGranted() && isVideoPermissionGranted()
    } else {
        isStoragePermissionGranted()
    }

    @JvmStatic
    fun isPhoneCallStatePermissionGranted(): Boolean = isPermissionGranted(Manifest.permission.READ_PHONE_STATE)

    @JvmStatic
    fun requestImagesAndVideoPermission(activity: Activity?) {
        requestImagesAndVideoPermission(
            activity, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE
        )
    }

    @JvmStatic
    fun requestImagesPermission(activity: Activity?) {
        requestImagesPermission(activity, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE)
    }

    @JvmStatic
    fun requestImagesPermission(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(
                activity,
                requestCode,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(activity, requestCode, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            requestPermissions(activity, requestCode, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @JvmStatic
    fun requestImagesAndVideoPermission(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(
                activity,
                requestCode,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                activity,
                requestCode,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            requestPermissions(activity, requestCode, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @JvmStatic
    fun requestAudioPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    @JvmStatic
    fun requestRecordAudioPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_CALLS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
            )
        } else {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_CALLS,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    @JvmStatic
    fun requestCameraPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_VIDEO_MESSAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.FOREGROUND_SERVICE_CAMERA,
            )
        } else {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_VIDEO_MESSAGE,
                Manifest.permission.CAMERA
            )
        }
    }

    @JvmStatic
    fun requestStoragePermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            requestPermissions(
                activity,
                BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    @JvmStatic
    fun requestPhoneCallStatePermission(activity: Activity?) = requestPermissions(activity, BasePermissionsActivity.REQUEST_CODE_CALLS, Manifest.permission.READ_PHONE_STATE)

    @JvmStatic
    fun requestPermissions(activity: Activity?, requestCode: Int, vararg permissions: String?) {
        if (activity == null) {
            return
        }
        activity.requestPermissions(permissions, requestCode)
    }

}
