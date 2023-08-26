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

import top.qwq2333.nullgram.BooleanConfig
import top.qwq2333.nullgram.IntConfig
import top.qwq2333.nullgram.StringConfig

/**
 * ConfigManager用到的Key都塞这 统一管理比较方便些
 */
object Defines {
    // Function
    @BooleanConfig
    const val showBotAPIID = "showBotAPIID"

    @BooleanConfig
    const val ignoreBlockedUser = "ignoreBlockedUser"

    @BooleanConfig
    const val blockSponsorAds = "blockSponsorMessages"
    const val hideGroupSticker = "hideGroupSticker"
    const val allowScreenshotOnNoForwardChat = "allowScreenshotOnNoForwardChat"
    const val autoDisableBuiltInProxy = "autoDisableBuiltInProxy"
    const val labelChannelUser = "labelChannelUser"
    const val displaySpoilerMsgDirectly = "displaySpoilerMsgDirectly"
    const val disableGreetingSticker = "disableGreetingSticker"
    const val codeSyntaxHighlight = "codeSyntaxHighlight"
    const val channelAlias = "aliasChannel" // ignore typo
    const val channelAliasPrefix = "aliasChannelName_" // ignore typo
    const val linkedUser = "linkedUser"
    const val linkedUserPrefix = "linkedUser_"
    const val overrideChannelAlias = "overrideChannelAlias"
    const val hidePhone = "hidePhone"
    const val openArchiveOnPull = "openArchiveOnPull"
    const val disableJumpToNextChannel = "disableJumpToNextChannel"
    const val verifyLinkTip = "verifyLinkTip"
    const val showExactNumber = "showExactNumber"
    const val disableTrendingSticker = "disableTrendingSticker"
    const val disablePremiumSticker = "disablePremiumSticker"
    const val disableInstantCamera = "disableInstantCamera"
    const val showHiddenSettings = "showHiddenSettings"
    const val confirmToSendMediaMessages = "confirmToSendMediaMessages"
    const val disableUndo = "disableUndo"
    const val skipOpenLinkConfirm = "skipOpenLinkConfirm"
    const val maxRecentSticker = "maxRecentSticker"
    const val unreadBadgeOnBackButton = "unreadBadgeOnBackButton"
    const val disableSendTyping = "disableSendTyping"
    const val ignoreReactionMention = "ignoreReactionMention"
    const val stickerSize = "customStickerSize"
    const val keepCopyFormatting = "keepCopyFormatting"
    const val dateOfForwardedMsg = "dateOfForwardedMsg"
    const val enchantAudio = "enchantAudio"
    const val avatarAsDrawerBackground = "avatarAsDrawerBackground"
    const val avatarBackgroundBlur = "avatarBackgroundBlur"
    const val avatarBackgroundDarken = "avatarBackgroundDarken"
    const val hideTimeForSticker = "hideTimeForSticker"
    const val showMessageID = "showMessageID"
    const val hideQuickSendMediaBottom = "hideQuickSendMediaButtom"
    const val largeAvatarAsBackground = "largeAvatarAsBackground"
    const val useSystemEmoji = "useSystemEmoji"
    const val customQuickMessage = "customQuickCommand"
    const val customQuickMessageEnabled = "customQuickMessageEnabled"
    const val customQuickMessageDisplayName = "customQuickCommandDisplayName"
    const val customQuickMessageRow = 92
    const val customQuickMsgSAR = "customQuickMessageSendAsReply"
    const val scrollableChatPreview = "scrollableChatPreview"
    const val disableVibration = "disableVibration"
    const val aospEmojiFont = "NotoColorEmoji.ttf"
    const val hidePremiumStickerAnim = "hidePremiumStickerAnim"
    const val fastSpeedUpload = "fastSpeedUpload"
    const val showTabsOnForward = "showTabsOnForward"
    const val disableStickersAutoReorder = "disableStickersAutoReorder"
    const val modifyDownloadSpeed = "modifyDownloadSpeed"
    const val disablePreviewVideoSoundShortcut = "disablePreviewVideoSoundShortcut"
    const val quickToggleAnonymous = "quickToggleAnonymous"
    const val hideProxySponsorChannel = "hideProxySponsorChannel"
    const val hideAllTab = "hideAllTab"
    const val ignoreMutedCount = "ignoreMutedCount"
    const val alwaysSendWithoutSound = "alwaysSendWithoutSound"
    const val markdownDisabled = "markdownEnable"
    const val markdownParseLinks = "markdownParseLinks"
    const val newMarkdownParser = "newMarkdownParser"
    const val showRPCError = "showRPCError"
    const val enablePanguOnSending = "enablePanguOnSending"
    const val enablePanguOnReceiving = "enablePanguOnReceiving"
    const val showExactTime = "showExactTime"

    /**
     * 0 default 1 online 2 offline
     */
    const val keepOnlineStatusAs = "keepOnlineStatusAs"

    // Custom API
    @IntConfig(0)
    const val customAPI = "customAPI"
    const val customAppId = "customAppId"
    @StringConfig("It shouldn't be happened")
    const val customAppHash = "customAppHash"
    const val disableCustomAPI = 0
    const val useTelegramAPI = 1
    const val useCustomAPI = 2
    const val telegramID = 4
    const val telegramHash = "014b35b6184100b085b0d0572f9b5103"

    // Menu Display
    const val showDeleteDownloadFiles = "showDeleteDownloadFiles"
    const val showNoQuoteForward = "showNoQuoteForward"
    const val showMessagesDetail = "showMessagesDetail"
    const val showSaveMessages = "showSaveMessages"
    const val showViewHistory = "showViewHistory"
    const val showRepeat = "showRepeat"
    const val showCopyPhoto = "showCopyPhoto"

    // custom double tap
    @IntConfig(doubleTabReaction)
    const val doubleTab = "doubleTab"
    const val doubleTabNone = 0
    const val doubleTabReaction = 1
    const val doubleTabReply = 2
    const val doubleTabSaveMessages = 3
    const val doubleTabRepeat = 4
    const val doubleTabEdit = 5
    const val doubleTabTranslate = 6

    // Auto Update
    const val ignoredUpdateTag = "skipUpdate"
    const val lastCheckUpdateTime = "lastCheckUpdateTime"
    const val updateChannel = "updateChannel"
    const val stableChannel = 1
    const val disableAutoUpdate = 0
    const val ciChannel = 2
    const val updateChannelSkip = "updateChannelSkip"

    // Tab Menu
    const val hasUpdateDialogFilterDatabase = "hasUpdateDialogFilterDatabaseFix"
    const val tabMenu = "tabMenu"
    const val tabMenuText = 0
    const val tabMenuMix = 1
    const val tabMenuIcon = 2

    // Override device
    const val devicePerformance = "devicePerformance"
    const val devicePerformanceAuto = -1
    const val devicePerformanceLow = 0
    const val devicePerformanceMedium = 1
    const val devicePerformanceHigh = 2

    // WebSocket Proxy
    const val wsEnableTLS = "wsEnableTLS"
    const val wsServerHost = "wsServerHost"
    const val wsBuiltInProxyBackend = "wsBuiltInProxyBackend"

    // Translate
    const val showOriginal = "showOriginal"
    const val translatorProvider = "translatorProvider"
    const val deepLFormality = "deepLFormality"
    const val translatorStatus = "translatorStatus"
    const val targetLanguage = "targetLanguage"
    const val restrictedLanguages = "restrictedLanguagesFix"
    const val autoTranslate = "autoTranslate"

    // Misc
    @JvmField
    val officialID = longArrayOf(
        966253902,  // Developer
        1668888324,  // Channel
        1578562490,  // Developer Channel
        1645976613,  // Update Channel
        1714986438,  // CI Channel
        1477185964,  // Discussion Group
        1068402676 // Kitsune
    )

    /**
     * 数组中元素未找到的下标，值为-1
     */
    const val indexNotFound = -1

    /**
     * 数组中是否包含元素
     *
     * @param array 数组
     * @param value 被检查的元素
     * @return 是否包含
     */
    @JvmStatic
    fun contains(array: LongArray?, value: Long): Boolean {
        return indexOf(array, value) > indexNotFound
    }

    /**
     * 返回数组中指定元素所在位置，未找到返回[.indexNotFound]
     *
     * @param array 数组
     * @param value 被检查的元素
     * @return 数组中指定元素所在位置，未找到返回[.indexNotFound]
     */
    @JvmStatic
    fun indexOf(array: LongArray?, value: Long): Int {
        if (null != array) {
            for (i in array.indices) {
                if (value == array[i]) {
                    return i
                }
            }
        }
        return indexNotFound
    }
}
