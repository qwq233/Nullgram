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
import androidx.annotation.RequiresApi
import org.telegram.messenger.ApplicationLoader
import org.telegram.ui.BasePermissionsActivity


object PermissionUtils {
    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun isPermissionGranted(permission: String?): Boolean {
        return ApplicationLoader.applicationContext.checkSelfPermission(permission!!) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @JvmStatic
    fun isImagesPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            isStoragePermissionGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @JvmStatic
    fun isImagesAndVideoPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isImagesPermissionGranted() && isVideoPermissionGranted()
        } else {
            isStoragePermissionGranted()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun requestImagesAndVideoPermission(activity: Activity?) {
        requestImagesAndVideoPermission(
            activity, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun requestImagesPermission(activity: Activity?) {
        requestImagesPermission(activity, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun requestImagesPermission(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(activity, requestCode, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            requestPermissions(activity, requestCode, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun requestImagesAndVideoPermission(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun requestAudioPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun requestStoragePermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @JvmStatic
    fun requestPermissions(activity: Activity?, requestCode: Int, vararg permissions: String?) {
        if (activity == null) {
            return
        }
        activity.requestPermissions(permissions, requestCode)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @JvmStatic
    fun isVideoPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            isStoragePermissionGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @JvmStatic
    fun isAudioPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            isStoragePermissionGranted()
        }
    }

    @JvmStatic
    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isImagesPermissionGranted() && isVideoPermissionGranted() && isAudioPermissionGranted()
            } else {
                isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            true
        }
    }

}
