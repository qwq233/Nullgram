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

package top.qwq2333.nullgram.helpers

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.browser.Browser
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.BottomSheet
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.CameraScanActivity
import org.telegram.ui.CameraScanActivity.CameraScanActivityDelegate
import org.telegram.ui.Components.AlertsCreator
import org.telegram.ui.Components.Bulletin
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.LinkSpanDrawable.LinksTextView
import top.qwq2333.nullgram.utils.Log

object QrHelper {
    @JvmStatic
    fun openCameraScanActivity(fragment: BaseFragment) {
        CameraScanActivity.showAsSheet(fragment, true, CameraScanActivity.TYPE_QR, object : CameraScanActivityDelegate {
            override fun didFindQr(link: String) {
                Browser.openUrl(fragment.parentActivity, link, true, false)
            }

            override fun processQr(link: String, onLoadEnd: Runnable): Boolean {
                AndroidUtilities.runOnUIThread(onLoadEnd, 750)
                return true
            }
        })
    }

    @JvmOverloads
    @JvmStatic
    fun showQrDialog(fragment: BaseFragment?, resourcesProvider: Theme.ResourcesProvider?, qrResults: ArrayList<QrResult>, dark: Boolean = false) {
        if (fragment == null || qrResults.isEmpty()) {
            return
        }
        if (qrResults.size == 1) {
            val text = qrResults[0].text
            if (text!!.startsWith("http://") || text.startsWith("https://")) {
                AlertsCreator.showOpenUrlAlert(fragment, text, true, true, if (dark) null else resourcesProvider)
                return
            }
        }
        val context = fragment.parentActivity
        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.VERTICAL
        val dialog = AlertDialog.Builder(context, resourcesProvider)
            .setView(ll)
            .create()
        if (dark) {
            dialog.setBackgroundColor(-0xe3ddd7)
        }
        for (i in qrResults.indices) {
            val text = qrResults[i].text
            val username = Browser.extractUsername(text)
            val linkOrUsername = username != null || text!!.startsWith("http://") || text.startsWith("https://")
            val textView = LinksTextView(context, if (dark) null else resourcesProvider)
            textView.setDisablePaddingsOffsetY(true)
            textView.setTextColor(
                if (dark) if (linkOrUsername) -0x863b04 else -0x1 else if (linkOrUsername) Theme.getColor(Theme.key_dialogTextLink, resourcesProvider) else Theme.getColor(
                    Theme.key_dialogTextBlack, resourcesProvider
                )
            )
            textView.setLinkTextColor(if (dark) -0x863b04 else Theme.getColor(Theme.key_dialogTextLink, resourcesProvider))
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            textView.gravity = if (LocaleController.isRTL) Gravity.RIGHT else Gravity.LEFT
            textView.maxLines = 0
            textView.isSingleLine = false
            textView.setPadding(AndroidUtilities.dp(21f), AndroidUtilities.dp(10f), AndroidUtilities.dp(21f), AndroidUtilities.dp(10f))
            textView.background = if (dark) Theme.createSelectorDrawable(0x0fffffff, Theme.RIPPLE_MASK_ALL) else Theme.getSelectorDrawable(false)
            if (linkOrUsername) {
                val sb = SpannableStringBuilder(text)
                sb.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        if (text!!.startsWith("http://") || text.startsWith("https://")) {
                            AlertsCreator.showOpenUrlAlert(fragment, text, true, false, if (dark) null else resourcesProvider)
                        }
                    }
                }, 0, text!!.length, 0)
                textView.setOnLinkLongPressListener { textView.performLongClick() }
                textView.text = sb
            } else {
                textView.text = text
                textView.setOnClickListener {
                    AndroidUtilities.addToClipboard(text)
                    BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied))
                        .show()
                }
            }
            textView.setOnLongClickListener {
                val builder = BottomSheet.Builder(context, false, resourcesProvider)
                builder.setTitle(if (username != null) "@$username" else text)
                builder.setItems(
                    if (linkOrUsername) arrayOf<CharSequence>(
                        LocaleController.getString("Open", R.string.Open),
                        LocaleController.getString("ShareFile", R.string.ShareFile),
                        LocaleController.getString("Copy", R.string.Copy)
                    ) else arrayOf<CharSequence?>(null, null, null, LocaleController.getString("Copy", R.string.Copy))
                ) { _: DialogInterface?, which: Int ->
                    if (which == 0) {
                        AlertsCreator.showOpenUrlAlert(fragment, text, true, false, if (dark) null else resourcesProvider)
                    } else if (which == 1 || which == 2) {
                        var url1 = text
                        var tel = false
                        if (url1!!.startsWith("mailto:")) {
                            url1 = url1.substring(7)
                        } else if (url1.startsWith("tel:")) {
                            url1 = url1.substring(4)
                            tel = true
                        }
                        if (which == 2) {
                            AndroidUtilities.addToClipboard(url1)
                            val bulletinMessage: String = if (tel) {
                                LocaleController.getString("PhoneCopied", R.string.PhoneCopied)
                            } else if (url1.startsWith("#")) {
                                LocaleController.getString("HashtagCopied", R.string.HashtagCopied)
                            } else if (url1.startsWith("@")) {
                                LocaleController.getString("UsernameCopied", R.string.UsernameCopied)
                            } else {
                                LocaleController.getString("LinkCopied", R.string.LinkCopied)
                            }
                            if (AndroidUtilities.shouldShowClipboardToast()) {
                                BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.voip_invite, bulletinMessage).show()
                            }
                        } else {
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, url1)
                            val chooserIntent = Intent.createChooser(shareIntent, LocaleController.getString("ShareFile", R.string.ShareFile))
                            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            ApplicationLoader.applicationContext.startActivity(chooserIntent)
                        }
                    } else {
                        BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider)
                            .createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show()
                    }
                }
                val bottomSheet = builder.create()
                if (dark) {
                    bottomSheet.scrollNavBar = true
                    bottomSheet.show()
                    bottomSheet.setItemColor(0, -0x1, -0x1)
                    bottomSheet.setItemColor(1, -0x1, -0x1)
                    bottomSheet.setItemColor(2, -0x1, -0x1)
                    bottomSheet.setBackgroundColor(-0xe3ddd7)
                    bottomSheet.fixNavigationBar(-0xe3ddd7)
                    bottomSheet.setTitleColor(-0x757576)
                    bottomSheet.setCalcMandatoryInsets(true)
                    AndroidUtilities.setNavigationBarColor(bottomSheet.window, -0xe3ddd7, false)
                    AndroidUtilities.setLightNavigationBar(bottomSheet.window, false)
                    bottomSheet.scrollNavBar = true
                } else {
                    bottomSheet.show()
                }
                true
            }
            ll.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        }
        fragment.showDialog(dialog)
    }

    @JvmStatic
    fun readQr(bitmap: Bitmap?): ArrayList<QrResult> {
        if (bitmap == null || bitmap.isRecycled || (bitmap.width == 0) || (bitmap.height == 0)) {
            return ArrayList()
        }
        val results = ArrayList(readQrInternal(bitmap))
        var inverted: Bitmap? = null
        if (results.isEmpty()) {
            inverted = invert(bitmap)
            results.addAll(readQrInternal(inverted))
            AndroidUtilities.recycleBitmap(inverted)
        }
        if (results.isEmpty()) {
            val monochrome = monochrome(inverted)
            results.addAll(readQrInternal(monochrome))
            AndroidUtilities.recycleBitmap(monochrome)
        }
        return results
    }

    private var qrReader: QRCodeMultiReader = QRCodeMultiReader()
    private var visionQrReader: BarcodeDetector = BarcodeDetector.Builder(ApplicationLoader.applicationContext).setBarcodeFormats(Barcode.QR_CODE).build()
    private fun readQrInternal(bitmap: Bitmap): ArrayList<QrResult> {
        val results = ArrayList<QrResult>()
        try {
            if (visionQrReader.isOperational) {
                val frame = Frame.Builder().setBitmap(bitmap).build()
                val width = bitmap.width
                val height = bitmap.height
                val codes = visionQrReader.detect(frame)
                for (i in 0 until codes.size()) {
                    val code = codes.valueAt(i)
                    val text = code.rawValue
                    val bounds = RectF()
                    if (code.cornerPoints.isNotEmpty()) {
                        var minX = Float.MAX_VALUE
                        var maxX = Float.MIN_VALUE
                        var minY = Float.MAX_VALUE
                        var maxY = Float.MIN_VALUE
                        for (point in code.cornerPoints) {
                            minX = minX.coerceAtMost(point.x.toFloat())
                            maxX = maxX.coerceAtLeast(point.x.toFloat())
                            minY = minY.coerceAtMost(point.y.toFloat())
                            maxY = maxY.coerceAtLeast(point.y.toFloat())
                        }
                        bounds[minX, minY, maxX] = maxY
                    }
                    results.add(buildResult(text, bounds, width, height))
                }
            } else {
                val intArray = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                val width = bitmap.width
                val height = bitmap.width
                val codes: Array<Result>? = try {
                    qrReader.decodeMultiple(BinaryBitmap(GlobalHistogramBinarizer(source)))
                } catch (e: NotFoundException) {
                    null
                }
                if (codes != null) {
                    for (code in codes) {
                        val text = code.text
                        var bounds = RectF()
                        val resultPoints = code.resultPoints
                        if (resultPoints != null && resultPoints.isNotEmpty()) {
                            var minX = Float.MAX_VALUE
                            var maxX = Float.MIN_VALUE
                            var minY = Float.MAX_VALUE
                            var maxY = Float.MIN_VALUE
                            for (point in resultPoints) {
                                minX = minX.coerceAtMost(point.x)
                                maxX = maxX.coerceAtLeast(point.x)
                                minY = minY.coerceAtMost(point.y)
                                maxY = maxY.coerceAtLeast(point.y)
                            }
                            bounds = RectF()
                            bounds[minX, minY, maxX] = maxY
                        }
                        results.add(buildResult(text, bounds, width, height))
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(t)
        }
        return results
    }

    private fun buildResult(text: String, bounds: RectF, width: Int, height: Int): QrResult {
        val result = QrResult()
        if (!bounds.isEmpty) {
            val paddingX = AndroidUtilities.dp(25f)
            val paddingY = AndroidUtilities.dp(15f)
            bounds[bounds.left - paddingX, bounds.top - paddingY, bounds.right + paddingX] = bounds.bottom + paddingY
            bounds[bounds.left / width.toFloat(), bounds.top / height.toFloat(), bounds.right / width.toFloat()] = bounds.bottom / height.toFloat()
        }
        result.bounds = bounds
        result.text = text
        return result
    }

    private fun invert(bitmap: Bitmap): Bitmap {
        val height = bitmap.height
        val width = bitmap.width
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val paint = Paint()
        val matrixGrayscale = ColorMatrix()
        matrixGrayscale.setSaturation(0f)
        val matrixInvert = ColorMatrix()
        matrixInvert.set(
            floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
        matrixInvert.preConcat(matrixGrayscale)
        paint.colorFilter = ColorMatrixColorFilter(matrixInvert)
        try {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        } catch (e: Exception) {
            Log.e(e)
        }
        return newBitmap
    }

    private fun monochrome(bitmap: Bitmap?): Bitmap {
        val height = bitmap!!.height
        val width = bitmap.width
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(createThresholdMatrix(90))
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return newBitmap
    }

    @JvmStatic
    fun createThresholdMatrix(threshold: Int = 90) = ColorMatrix(
        floatArrayOf(
            85f, 85f, 85f, 0f, -255f * threshold,
            85f, 85f, 85f, 0f, -255f * threshold,
            85f, 85f, 85f, 0f, -255f * threshold,
            0f, 0f, 0f, 1f, 0f
        )
    )

    class QrResult {
        @JvmField
        var text: String? = null
        @JvmField
        var bounds: RectF? = null
    }
}
