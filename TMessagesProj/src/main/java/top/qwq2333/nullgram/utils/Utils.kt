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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Base64
import android.view.View
import android.widget.Toast
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.SharedConfig
import org.telegram.tgnet.TLObject
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.AlertsCreator
import org.telegram.ui.Components.BulletinFactory
import top.qwq2333.gen.Config
import top.qwq2333.nullgram.activity.DatacenterActivity
import top.qwq2333.nullgram.remote.NicegramController
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Locale


object Utils {

    @JvmStatic
    fun showForwardDate(obj: MessageObject, orig: CharSequence): String = if (Config.dateOfForwardedMsg &&
        obj.messageOwner.fwd_from.date.toLong() != 0L
    ) {
        "$orig • ${LocaleController.formatDate(obj.messageOwner.fwd_from.date.toLong())}"
    } else {
        orig.toString()
    }

    @JvmStatic
    fun getBotIDFromUserID(originalID: Long, isChannel: Boolean): Long = if (isChannel) {
        -1000000000000L - originalID
    } else {
        -originalID
    }

    @JvmStatic
    fun getUserIDFromBotID(botID: Long, isChannel: Boolean) = if (isChannel) {
        -(botID + 1000000000000L)
    } else {
        -botID
    }

    @JvmStatic
    fun getSecurePassword(password: String, salt: String): String {
        lateinit var generatedPassword: String
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(Base64.decode(salt, Base64.DEFAULT))
            val bytes = md.digest(password.toByteArray())
            val sb = StringBuilder()
            for (i in bytes.indices) {
                sb.append(((bytes[i].toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
            generatedPassword = sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return generatedPassword
    }

    @JvmStatic
    fun getSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(32)
        random.nextBytes(salt)
        return salt
    }

    @JvmStatic
    var loadSystemEmojiFailed = false

    @JvmStatic
    private var systemEmojiTypeface: Typeface? = null

    private fun getSystemEmojiFontPath(): File? {
        try {
            BufferedReader(FileReader("/system/etc/fonts.xml")).use { br ->
                var line: String
                var ignored = false
                while (br.readLine().also { line = it } != null) {
                    val trimmed = line.trim { it <= ' ' }
                    if (trimmed.startsWith("<family") && trimmed.contains("ignore=\"true\"")) {
                        ignored = true
                    } else if (trimmed.startsWith("</family>")) {
                        ignored = false
                    } else if (trimmed.startsWith("<font") && !ignored) {
                        val start = trimmed.indexOf(">")
                        val end = trimmed.indexOf("<", 1)
                        if (start > 0 && end > 0) {
                            val font = trimmed.substring(start + 1, end)
                            if (font.lowercase(Locale.getDefault()).contains("emoji")) {
                                val file = File("/system/fonts/$font")
                                if (file.exists()) {
                                    Log.d("emoji font file fonts.xml = $font")
                                    return file
                                }
                            }
                        }
                    }
                }
                br.close()
                val fileAOSP = File("/system/fonts/" + Defines.aospEmojiFont)
                if (fileAOSP.exists()) {
                    return fileAOSP
                }
            }
        } catch (e: Exception) {
            Log.e(e)
        }
        return null
    }

    @JvmStatic
    fun getSystemEmojiTypeface(): Typeface? {
        if (!loadSystemEmojiFailed && systemEmojiTypeface == null) {
            val font: File? = getSystemEmojiFontPath()
            if (font != null) {
                systemEmojiTypeface = Typeface.createFromFile(font)
            }
            if (systemEmojiTypeface == null) {
                loadSystemEmojiFailed = true
            }
        }
        return systemEmojiTypeface
    }

    @JvmStatic
    fun isVPNEnabled(): Boolean {
        runCatching {
            val connectivityManager = ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities!!.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
        return false
    }

    @JvmStatic
    fun registerNetworkCallback() {
        val connectivityManager = ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback: ConnectivityManager.NetworkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return
                    val vpn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    if (!vpn) {
                        if (SharedConfig.currentProxy == null) {
                            if (!SharedConfig.proxyList.isEmpty()) {
                                SharedConfig.setCurrentProxy(SharedConfig.proxyList[0])
                            } else {
                                return
                            }
                        }
                    }
                    if ((SharedConfig.proxyEnabled && vpn) || (!SharedConfig.proxyEnabled && !vpn)) {
                        SharedConfig.setProxyEnable(!vpn)
                        UIUtil.runOnUIThread {
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged)
                        }
                    }
                }
            }

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (ignored: Exception) {
        }
    }

    /**
     * Right shift 32 bits and perform AND operation with 0xFFFFFFFF to get the Sticker Set Owner ID.
     *
     * Example:
     * ```
     * 0011 1001 1001 0111 1101 1101 0100 1110 0000 0000 0000 0000 0000 0000 0000 0001 // 4150028908722388993 Sticker Set ID
     * 0011 1001 1001 0111 1101 1101 0100 1110                                         // 966253902           Owner
     * ```
     */
    @JvmStatic
    fun getOwnerFromStickerSetId(stickerSetId: Long): Long {
        val j4 = stickerSetId shr 32
        val j5 = stickerSetId and 0xFFFF_FFFFL
        return j4 + j5 - j5.toInt()
    }

    @JvmStatic
    fun showIdPopup(fragment: BaseFragment, anchorView: View?, id: Long, dc: Int, user: Boolean, x: Float, y: Float) {
        val context: Context = fragment.parentActivity
        val popupLayout: ActionBarPopupWindowLayout = object : ActionBarPopupWindowLayout(context, R.drawable.popup_fixed_alert, fragment.resourceProvider) {
            val path = Path()
            override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
                canvas.save()
                path.rewind()
                AndroidUtilities.rectTmp[child.left.toFloat(), child.top.toFloat(), child.right.toFloat()] = child.bottom.toFloat()
                path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(6f).toFloat(), AndroidUtilities.dp(6f).toFloat(), Path.Direction.CW)
                canvas.clipPath(path)
                val draw = super.drawChild(canvas, child, drawingTime)
                canvas.restore()
                return draw
            }
        }
        popupLayout.setFitItems(true)
        val popupWindow = AlertsCreator.createSimplePopup(fragment, popupLayout, anchorView, x, y)
        if (id != 0L) {
            ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_copy, LocaleController.getString("CopyID", R.string.CopyID), false, fragment.resourceProvider)
                .setOnClickListener {
                    popupWindow.dismiss()
                    AndroidUtilities.addToClipboard(id.toString())
                    BulletinFactory.of(fragment).createCopyBulletin(LocaleController.formatString("TextCopied", R.string.TextCopied)).show()
                }
        }
        if (dc != 0) {
            val subItem = ActionBarMenuItem.addItem(
                popupLayout,
                R.drawable.msg_satellite,
                LocaleController.getString("DatacenterStatusShort", R.string.DatacenterStatusShort),
                false,
                fragment.resourceProvider
            )
            subItem.setSubtext(MessageUtils.formatDCString(dc))
            subItem.setOnClickListener {
                popupWindow.dismiss()
                fragment.presentFragment(DatacenterActivity(dc))
            }
        }
        if (id != 0L && user) {
            val subItem = ActionBarMenuItem.addItem(
                popupLayout,
                R.drawable.msg_calendar,
                LocaleController.getString("RegistrationDate", R.string.RegistrationDate),
                false,
                fragment.resourceProvider
            )
            subItem.setSubtext(LocaleController.getString("Loading", R.string.Loading))
            NicegramController.getRegDate(id, {
                subItem.setSubtext(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred))
            }) { dateType, date ->
                when (dateType) {
                    NicegramController.RegDateResponse.RegDateType.Approximately -> LocaleController.formatString("RegistrationDateApproximately", R.string.RegistrationDateApproximately, date)
                    NicegramController.RegDateResponse.RegDateType.NewerThan -> LocaleController.formatString("RegistrationDateNewer", R.string.RegistrationDateNewer, date)
                    NicegramController.RegDateResponse.RegDateType.OlderThan -> LocaleController.formatString("RegistrationDateOlder", R.string.RegistrationDateOlder, date)
                    else -> date
                }.let { subItem.setSubtext(it) }
            }
        }
        popupLayout.setParentWindow(popupWindow)
    }

    @JvmStatic
    fun showErrorToast(method: TLObject, text: String) {
        if (text == "FILE_REFERENCE_EXPIRED") {
            return
        }
        AndroidUtilities.runOnUIThread {
            Toast.makeText(ApplicationLoader.applicationContext, "${DatabaseUtils.getMethodName(method)}: $text", Toast.LENGTH_SHORT).show()
        }
    }

}

fun String.encodeUrl(): String = URLEncoder.encode(this, "UTF-8")
fun String.isNumber(): Boolean = try {
    this.toLong()
    true
} catch (e: NumberFormatException) {
    false
}

internal inline fun tryOrLog(block: () -> Unit) = runCatching {
    block()
}.onFailure {
    Log.e(it)
}
