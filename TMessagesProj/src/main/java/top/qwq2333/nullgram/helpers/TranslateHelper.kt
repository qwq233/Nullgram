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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.TextUtils
import android.text.style.URLSpan
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.util.Pair
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme.ResourcesProvider
import org.telegram.ui.Components.TranslateAlert
import top.qwq2333.gen.Config
import top.qwq2333.nullgram.activity.LanguageSelectActivity
import top.qwq2333.nullgram.config.ConfigManager
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.translate.providers.BaiduTranslator
import top.qwq2333.nullgram.translate.providers.DeepLTranslator
import top.qwq2333.nullgram.translate.providers.GoogleTranslator
import top.qwq2333.nullgram.translate.providers.LingoTranslator
import top.qwq2333.nullgram.translate.providers.MicrosoftTranslator
import top.qwq2333.nullgram.translate.providers.TelegramTranslator
import top.qwq2333.nullgram.translate.providers.YandexTranslator
import top.qwq2333.nullgram.ui.PopupBuilder
import top.qwq2333.nullgram.utils.Defines
import top.qwq2333.nullgram.utils.Log
import java.util.Locale

object TranslateHelper {
    @JvmStatic
    var currentTargetLanguage = ConfigManager.getStringOrDefault(Defines.targetLanguage, "app")!!
        set(value) {
            ConfigManager.putString(Defines.targetLanguage, value)
            field = value
        }

    @JvmStatic
    var restrictedLanguages: java.util.HashSet<String> = ConfigManager.getStringSetOrDefault(Defines.restrictedLanguages, java.util.HashSet()) as java.util.HashSet<String>
        set(value) {
            Log.i("TranslateHelper: set restrictedLanguages to $value")
            ConfigManager.putStringSet(Defines.restrictedLanguages, value)
            field = value
        }

    @JvmStatic
    var showOriginal = Config.showOriginal

    @JvmStatic
    fun toggleShowOriginal() {
        showOriginal = !showOriginal
        Config.toggleShowOriginal()
    }

    @JvmStatic
    var autoTranslate = Config.autoTranslate

    @JvmStatic
    fun toggleAutoTranslate() {
        autoTranslate = !autoTranslate
        Config.toggleAutoTranslate()
    }

    enum class ProviderType(val num: Int) {
        GoogleTranslator(1),
        TelegramTranslator(2),
        MicrosoftTranslator(3),
        LingoTranslator(4),
        BaiduTranslator(5),
        YandexTranslator(6),
        DeepLTranslator(7),
    }

    @JvmStatic
    var currentProviderType = when (ConfigManager.getIntOrDefault(Defines.translatorProvider, ProviderType.GoogleTranslator.num)) {
        ProviderType.GoogleTranslator.num -> ProviderType.GoogleTranslator
        ProviderType.YandexTranslator.num -> ProviderType.YandexTranslator
        ProviderType.TelegramTranslator.num -> ProviderType.TelegramTranslator
        ProviderType.MicrosoftTranslator.num -> ProviderType.MicrosoftTranslator
        ProviderType.DeepLTranslator.num -> ProviderType.DeepLTranslator
        ProviderType.LingoTranslator.num -> ProviderType.LingoTranslator
        ProviderType.BaiduTranslator.num -> ProviderType.BaiduTranslator
        else -> ProviderType.GoogleTranslator
    }
        set(value) {
            ConfigManager.putInt(Defines.translatorProvider, value.num)
            field = value
        }

    @JvmStatic
    fun getProvider(providerType: ProviderType) = when (providerType) {
        ProviderType.GoogleTranslator -> GoogleTranslator
        ProviderType.YandexTranslator -> YandexTranslator
        ProviderType.TelegramTranslator -> TelegramTranslator
        ProviderType.MicrosoftTranslator -> MicrosoftTranslator
        ProviderType.DeepLTranslator -> DeepLTranslator
        ProviderType.LingoTranslator -> LingoTranslator
        ProviderType.BaiduTranslator -> BaiduTranslator
    }

    @JvmStatic
    fun getCurrentProvider(): BaseTranslator = when (currentProviderType) {
        ProviderType.GoogleTranslator -> GoogleTranslator
        ProviderType.YandexTranslator -> YandexTranslator
        ProviderType.TelegramTranslator -> TelegramTranslator
        ProviderType.MicrosoftTranslator -> MicrosoftTranslator
        ProviderType.DeepLTranslator -> DeepLTranslator
        ProviderType.LingoTranslator -> LingoTranslator
        ProviderType.BaiduTranslator -> BaiduTranslator
    }

    @JvmStatic
    fun getProviderType(num: Int): ProviderType = when (num) {
        ProviderType.GoogleTranslator.num -> ProviderType.GoogleTranslator
        ProviderType.YandexTranslator.num -> ProviderType.YandexTranslator
        ProviderType.TelegramTranslator.num -> ProviderType.TelegramTranslator
        ProviderType.MicrosoftTranslator.num -> ProviderType.MicrosoftTranslator
        ProviderType.DeepLTranslator.num -> ProviderType.DeepLTranslator
        ProviderType.LingoTranslator.num -> ProviderType.LingoTranslator
        ProviderType.BaiduTranslator.num -> ProviderType.BaiduTranslator
        else -> ProviderType.GoogleTranslator
    }

    enum class Status(val num: Int) {
        Popup(2),
        InMessage(3),
        External(4),
    }

    @JvmStatic
    var currentStatus = when (ConfigManager.getIntOrDefault(Defines.translatorStatus, Status.InMessage.num)) {
        Status.Popup.num -> Status.Popup
        Status.InMessage.num -> Status.InMessage
        Status.External.num -> Status.External
        else -> Status.InMessage
    }
        set(value) {
            ConfigManager.putInt(Defines.translatorStatus, value.num)
            field = value
        }

    @JvmStatic
    fun stripLanguageCode(language: String): String {

        return if (language.contains("-")) {
            language.substring(0, language.indexOf("-"))
        } else language
    }

    @JvmStatic
    fun translate(obj: Any, from: String, onSuccess: (Any, String, String) -> Unit, onError: (Exception) -> Unit) {
        val translator = getCurrentProvider()
        val language = translator.getCurrentTargetLanguage()
        if (!translator.supportLanguage(language)) {
            onError(UnsupportedTargetLanguageException())
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    translator.translate(obj, from, language)
                }
                if (result.error != null) {
                    if (result.error == HttpStatusCode.TooManyRequests) {
                        onError(TooManyRequestException())
                    } else {
                        onError(Exception(result.error.toString()))
                    }
                } else {
                    onSuccess(result.result!!, translator.convertLanguageCode(result.from, true), language)
                }
            }
        }
    }


    @JvmStatic
    fun isLanguageRestricted(lang: String?): Boolean {
        var lang = lang
        if (lang == null || lang == "und") {
            return false
        }
        val toLang: String = stripLanguageCode(getCurrentProvider().getCurrentTargetLanguage())
        lang = stripLanguageCode(lang)
        if (lang == toLang) {
            return true
        }
        var restricted = false
        restrictedLanguages.forEach {
            val language = if (it.contains("-")) {
                it.substring(0, it.indexOf("_"))
            } else it
            if (lang == language) {
                restricted = true
                return@forEach
            }
        }
        return restricted
    }

    @JvmStatic
    fun getProviders(): Pair<ArrayList<String>, ArrayList<ProviderType>> {
        val names = ArrayList<String>()
        val types = ArrayList<ProviderType>()
        names.add(LocaleController.getString("ProviderGoogleTranslate", R.string.ProviderGoogleTranslate))
        types.add(ProviderType.GoogleTranslator)
        names.add(LocaleController.getString("ProviderYandexTranslate", R.string.ProviderYandexTranslate))
        types.add(ProviderType.YandexTranslator)
        names.add(LocaleController.getString("ProviderTelegramTranslate", R.string.ProviderTelegramTranslate))
        types.add(ProviderType.TelegramTranslator)
        names.add(LocaleController.getString("ProviderMicrosoftTranslate", R.string.ProviderMicrosoftTranslate))
        types.add(ProviderType.MicrosoftTranslator)
        names.add(LocaleController.getString("ProviderDeepLTranslator", R.string.ProviderDeepLTranslate))
        types.add(ProviderType.DeepLTranslator)
        names.add(LocaleController.getString("ProviderLingoTranslate", R.string.ProviderLingoTranslate))
        types.add(ProviderType.LingoTranslator)
        names.add(LocaleController.getString("ProviderBaiduTranslate", R.string.ProviderBaiduTranslate))
        types.add(ProviderType.BaiduTranslator)
        return Pair(names, types)
    }


    @JvmStatic
    fun showTranslationProviderSelector(
        context: Context, view: View?, resourcesProvider: ResourcesProvider? = null, result: (param: Boolean) -> Unit
    ) {
        val providers = getProviders()
        val names = providers.first
        val types = providers.second
        PopupBuilder.show(
            names, LocaleController.getString("TranslationProvider", R.string.TranslationProvider), types.indexOf(currentProviderType), context, view, resourcesProvider
        ) { i ->
            val translator: BaseTranslator = getProvider(types[i])
            val targetLanguage = translator.getTargetLanguage(currentTargetLanguage)
            if (translator.supportLanguage(targetLanguage)) {
                currentProviderType = types[i]
                result(true)
            } else {
                val builder = AlertDialog.Builder(context, resourcesProvider).setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported))
                if ("app" == currentTargetLanguage) {
                    builder.setPositiveButton(LocaleController.getString("UseGoogleTranslate", R.string.UseGoogleTranslate)) { _: DialogInterface?, _: Int ->
                        currentProviderType = ProviderType.GoogleTranslator
                        result(true)
                    }
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                } else if (translator.supportLanguage(translator.getCurrentAppLanguage())) {
                    builder.setPositiveButton(LocaleController.getString("ResetLanguage", R.string.ResetLanguage)) { _: DialogInterface?, _: Int ->
                        currentProviderType = types[i]
                        currentTargetLanguage = "app"
                        result(false)
                    }
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                } else {
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null)
                }
                builder.show()
            }
        }

    }


    @JvmStatic
    @JvmOverloads
    fun showTranslationTargetSelector(
        fragment: BaseFragment, view: View?, whiteActionBar: Boolean = true, resourcesProvider: ResourcesProvider? = null, callback: () -> Unit
    ) {
        if (getCurrentProvider().getTargetLanguages().size <= 30) {
            val targetLanguages = ArrayList<String>(getCurrentProvider().getTargetLanguages())
            val names = arrayListOf<CharSequence>()
            for (language in targetLanguages) {
                val locale = Locale.forLanguageTag(language)
                if (!TextUtils.isEmpty(locale.script)) {
                    names.add(HtmlCompat.fromHtml(String.format("%s - %s", locale.displayScript, locale.getDisplayScript(locale)), HtmlCompat.FROM_HTML_MODE_LEGACY))
                } else {
                    names.add(String.format("%s - %s", locale.displayName, locale.getDisplayName(locale)))
                }
            }
            targetLanguages.add(0, "app")
            names.add(0, LocaleController.getString("TranslationTargetApp", R.string.TranslationTargetApp))
            PopupBuilder.show(
                names,
                LocaleController.getString("TranslationTarget", R.string.TranslationTarget),
                targetLanguages.indexOf(currentTargetLanguage),
                fragment.parentActivity,
                view,
                resourcesProvider
            ) { i ->
                currentTargetLanguage = targetLanguages[i]
                callback.invoke()
            }
        } else {
            fragment.presentFragment(LanguageSelectActivity(LanguageSelectActivity.TYPE_TARGET, whiteActionBar))
        }
    }

    @JvmStatic
    @JvmOverloads
    fun showTranslatorTypeSelector(context: Context?, view: View?, resourcesProvider: ResourcesProvider? = null, callback: () -> Unit) {
        val arrayList = arrayListOf<String>()
        val types = arrayListOf<Status>()
        arrayList.add(LocaleController.getString("TranslatorTypeInMessage", R.string.TranslatorTypeInMessage))
        types.add(Status.InMessage)
        arrayList.add(LocaleController.getString("TranslatorTypePopup", R.string.TranslatorTypePopup))
        types.add(Status.Popup)
        arrayList.add(LocaleController.getString("TranslatorTypeExternal", R.string.TranslatorTypeExternal))
        types.add(Status.External)
        PopupBuilder.show(
            arrayList, LocaleController.getString("TranslatorType", R.string.TranslatorType), types.indexOf(currentStatus), context, view, resourcesProvider
        ) { i ->
            currentStatus = types[i]
            callback.invoke()
        }
    }

    @JvmStatic
    fun showTranslateDialog(
        context: Context, query: String, fragment: BaseFragment?, sourceLanguage: String?, onLinkPress: ((URLSpan) -> Boolean)?
    ) {
        if (currentStatus == Status.External) {
            startExternalTranslator(context, query)
        } else {
            TranslateAlert.showAlert(context, fragment, sourceLanguage, currentTargetLanguage, query, false, onLinkPress, null)
        }
    }

    @JvmStatic
    fun startExternalTranslator(context: Context, text: String) {
        @SuppressLint("InlinedApi") val intent = Intent(Intent.ACTION_TRANSLATE)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AlertDialog.Builder(context).setTitle(LocaleController.getString("AppName", R.string.AppName))
                .setMessage(LocaleController.getString("NoTranslatorAppInstalled", R.string.NoTranslatorAppInstalled)).show()
        }
    }

    @JvmStatic
    fun handleTranslationError(context: Context?, e: Exception?, onRetry: Runnable?, resourcesProvider: ResourcesProvider?) {
        if (context == null) {
            return
        }
        val builder = AlertDialog.Builder(context, resourcesProvider)
        if (e is UnsupportedTargetLanguageException) {
            builder.setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported))
            builder.setPositiveButton(
                LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort)
            ) { _: DialogInterface?, _: Int ->
                showTranslationProviderSelector(context, null, resourcesProvider) {}
            }
        } else {
            if (e is TooManyRequestException) {
                builder.setTitle(LocaleController.getString("TranslateFailed", R.string.TranslateFailed))
                builder.setMessage(LocaleController.getString("FloodWait", R.string.FloodWait))
            } else if (e != null) {
                builder.setTitle(LocaleController.getString("TranslateFailed", R.string.TranslateFailed))
                builder.setMessage(e.message)
            } else {
                builder.setMessage(LocaleController.getString("TranslateFailed", R.string.TranslateFailed))
            }
            if (onRetry != null) {
                builder.setPositiveButton(
                    LocaleController.getString("Retry", R.string.Retry)
                ) { _: DialogInterface?, _: Int -> onRetry.run() }
            }
            builder.setNeutralButton(
                LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort)
            ) { _: DialogInterface?, _: Int ->
                showTranslationProviderSelector(context, null, resourcesProvider) {}
            }
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
        builder.show()
    }

    private class UnsupportedTargetLanguageException : IllegalArgumentException()
    private class TooManyRequestException : IllegalStateException()

}
