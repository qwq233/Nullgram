/*
 * Copyright (C) 2019-2025 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.telegram.ui.ActionBar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseIntArray;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatThemeController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.utils.tlutils.TlUtils;
import org.telegram.messenger.wallpaper.WallpaperBitmapHolder;
import org.telegram.messenger.wallpaper.WallpaperGiftBitmapDrawable;
import org.telegram.messenger.wallpaper.WallpaperGiftPatternPosition;
import org.telegram.tgnet.ResultCallback;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.theme.ITheme;
import org.telegram.ui.ActionBar.theme.ThemeKey;
import org.telegram.ui.Components.RLottieDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class EmojiThemes {

    public static final String REMOVED_EMOJI = "❌";

    public boolean showAsDefaultStub;
    public boolean showAsRemovedStub;
    public ThemeKey key;
    public TLRPC.ChatTheme chatTheme;
    public String emoji;
    public TLRPC.WallPaper wallpaper;
    int currentIndex = 0;
    public ArrayList<ThemeItem> items = new ArrayList<>();
    private final int currentAccount;

    private static final int[] previewColorKeys = new int[]{
            Theme.key_chat_inBubble,
            Theme.key_chat_outBubble,
            Theme.key_featuredStickers_addButton,
            Theme.key_chat_wallpaper,
            Theme.key_chat_wallpaper_gradient_to1,
            Theme.key_chat_wallpaper_gradient_to2,
            Theme.key_chat_wallpaper_gradient_to3,
            Theme.key_chat_wallpaper_gradient_rotation
    };

    public EmojiThemes(int currentAccount) {
        this.currentAccount = currentAccount;
    }

    public EmojiThemes(int currentAccount, TLRPC.TL_theme chatThemeObject, boolean isDefault) {
        this.currentAccount = currentAccount;
        this.showAsDefaultStub = isDefault;
        this.emoji = chatThemeObject.emoticon;
        this.key = ThemeKey.of(chatThemeObject);
        this.chatTheme = TLRPC.ChatTheme.ofEmoticon(chatThemeObject.emoticon);
        if (!isDefault) {
            ThemeItem lightTheme = new ThemeItem();
            lightTheme.tlTheme = chatThemeObject;
            lightTheme.settingsIndex = 0;
            items.add(lightTheme);

            ThemeItem darkTheme = new ThemeItem();
            darkTheme.tlTheme = chatThemeObject;
            darkTheme.settingsIndex = 1;
            items.add(darkTheme);
        }
    }

    public EmojiThemes(int currentAccount, TLRPC.TL_chatThemeUniqueGift chatThemeObject) {
        this.currentAccount = currentAccount;
        this.showAsDefaultStub = false;
        this.emoji = chatThemeObject.gift.slug;
        this.key = ThemeKey.of(chatThemeObject);
        this.chatTheme = chatThemeObject;


        ThemeItem lightTheme = new ThemeItem();
        lightTheme.tlChatThemeGift = chatThemeObject;
        lightTheme.settingsIndex = 0;
        items.add(lightTheme);

        ThemeItem darkTheme = new ThemeItem();
        darkTheme.tlChatThemeGift = chatThemeObject;
        darkTheme.settingsIndex = 1;
        items.add(darkTheme);
    }

    public boolean isAnyStub() {
        return showAsDefaultStub || showAsRemovedStub;
    }

    public boolean isGiftTheme() {
        return key != null && !TextUtils.isEmpty(key.giftSlug);
    }

    public static EmojiThemes createPreviewFullTheme(int currentAccount, TLRPC.TL_theme tl_theme) {
        EmojiThemes chatTheme = new EmojiThemes(currentAccount);
        chatTheme.emoji = tl_theme.emoticon;
        chatTheme.key = ThemeKey.of(tl_theme);
        chatTheme.chatTheme = TLRPC.ChatTheme.ofEmoticon(tl_theme.emoticon);

        for (int i = 0; i < tl_theme.settings.size(); i++) {
            ThemeItem theme = new ThemeItem();
            theme.tlTheme = tl_theme;
            theme.settingsIndex = i;
            chatTheme.items.add(theme);
        }
        return chatTheme;
    }


    public static EmojiThemes createChatThemesDefault(int currentAccount) {

        EmojiThemes themeItem = new EmojiThemes(currentAccount);
        themeItem.emoji = REMOVED_EMOJI;
        themeItem.key = ThemeKey.ofEmoticon(REMOVED_EMOJI);
        themeItem.chatTheme = TLRPC.ChatTheme.ofEmoticon(REMOVED_EMOJI);
        themeItem.showAsDefaultStub = true;

        ThemeItem lightTheme = new ThemeItem();
        lightTheme.themeInfo = getDefaultThemeInfo(true);
        themeItem.items.add(lightTheme);

        ThemeItem darkTheme = new ThemeItem();
        darkTheme.themeInfo = getDefaultThemeInfo(false);
        themeItem.items.add(darkTheme);

        return themeItem;
    }

    public static EmojiThemes createChatThemesRemoved(int currentAccount) {

        EmojiThemes themeItem = new EmojiThemes(currentAccount);
        themeItem.emoji = REMOVED_EMOJI;
        themeItem.key = ThemeKey.ofEmoticon(REMOVED_EMOJI);
        themeItem.chatTheme = TLRPC.ChatTheme.ofEmoticon(REMOVED_EMOJI);
        themeItem.showAsRemovedStub = true;

        ThemeItem lightTheme = new ThemeItem();
        lightTheme.themeInfo = getDefaultThemeInfo(true);
        themeItem.items.add(lightTheme);

        ThemeItem darkTheme = new ThemeItem();
        darkTheme.themeInfo = getDefaultThemeInfo(false);
        themeItem.items.add(darkTheme);

        return themeItem;
    }

    public static EmojiThemes createPreviewCustom(int currentAccount) {
        EmojiThemes themeItem = new EmojiThemes(currentAccount);
        themeItem.emoji = "\uD83C\uDFA8";
        themeItem.key = ThemeKey.ofEmoticon(themeItem.emoji);
        themeItem.chatTheme = TLRPC.ChatTheme.ofEmoticon(themeItem.emoji);

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE);
        String lastDayCustomTheme = preferences.getString("lastDayCustomTheme", null);
        int dayAccentId = preferences.getInt("lastDayCustomThemeAccentId", -1);
        if (lastDayCustomTheme == null || Theme.getTheme(lastDayCustomTheme) == null) {
            lastDayCustomTheme = preferences.getString("lastDayTheme", "Blue");
            Theme.ThemeInfo themeInfo = Theme.getTheme(lastDayCustomTheme);
            if (themeInfo == null) {
                lastDayCustomTheme = "Blue";
                dayAccentId = 99;
            } else {
                dayAccentId = themeInfo.currentAccentId;
            }
            preferences.edit().putString("lastDayCustomTheme", lastDayCustomTheme).apply();
        } else {
            if (dayAccentId == -1) {
                dayAccentId = Theme.getTheme(lastDayCustomTheme).lastAccentId;
            }
        }

        if (dayAccentId == -1) {
            lastDayCustomTheme = "Blue";
            dayAccentId = 99;
        }

        String lastDarkCustomTheme = preferences.getString("lastDarkCustomTheme", null);
        int darkAccentId = preferences.getInt("lastDarkCustomThemeAccentId", -1);
        if (lastDarkCustomTheme == null || Theme.getTheme(lastDarkCustomTheme) == null) {
            lastDarkCustomTheme = preferences.getString("lastDarkTheme", "Dark Blue");
            Theme.ThemeInfo themeInfo = Theme.getTheme(lastDarkCustomTheme);
            if (themeInfo == null) {
                lastDarkCustomTheme = "Dark Blue";
                darkAccentId = 0;
            } else {
                darkAccentId = themeInfo.currentAccentId;
            }
            preferences.edit().putString("lastDarkCustomTheme", lastDarkCustomTheme).apply();
        } else {
            if (darkAccentId == -1) {
                darkAccentId = Theme.getTheme(lastDayCustomTheme).lastAccentId;
            }
        }

        if (darkAccentId == -1) {
            lastDarkCustomTheme = "Dark Blue";
            darkAccentId = 0;
        }

        ThemeItem lightTheme = new ThemeItem();
        lightTheme.themeInfo = Theme.getTheme(lastDayCustomTheme);
        lightTheme.accentId = dayAccentId;
        themeItem.items.add(lightTheme);
        themeItem.items.add(null);

        ThemeItem darkTheme = new ThemeItem();
        darkTheme.themeInfo = Theme.getTheme(lastDarkCustomTheme);
        darkTheme.accentId = darkAccentId;
        themeItem.items.add(darkTheme);
        themeItem.items.add(null);

        return themeItem;
    }

    public static EmojiThemes createHomePreviewTheme(int currentAccount) {
        EmojiThemes themeItem = new EmojiThemes(currentAccount);
        themeItem.emoji = "\uD83C\uDFE0";
        themeItem.key = ThemeKey.ofEmoticon(themeItem.emoji);
        themeItem.chatTheme = TLRPC.ChatTheme.ofEmoticon(themeItem.emoji);

        ThemeItem blue = new ThemeItem();
        blue.themeInfo = Theme.getTheme("Blue");
        blue.accentId = 99;
        themeItem.items.add(blue);

        ThemeItem day = new ThemeItem();
        day.themeInfo = Theme.getTheme("Day");
        day.accentId = 9;
        themeItem.items.add(day);

        ThemeItem night = new ThemeItem();
        night.themeInfo = Theme.getTheme("Night");
        night.accentId = 0;
        themeItem.items.add(night);

        ThemeItem nightBlue = new ThemeItem();
        nightBlue.themeInfo = Theme.getTheme("Dark Blue");
        nightBlue.accentId = 0;
        themeItem.items.add(nightBlue);
        return themeItem;
    }

    public static EmojiThemes createHomeQrTheme(int currentAccount) {
        EmojiThemes themeItem = new EmojiThemes(currentAccount);
        themeItem.emoji = "\uD83C\uDFE0";
        themeItem.key = ThemeKey.ofEmoticon(themeItem.emoji);
        themeItem.chatTheme = TLRPC.ChatTheme.ofEmoticon(themeItem.emoji);

        ThemeItem blue = new ThemeItem();
        blue.themeInfo = Theme.getTheme("Blue");
        blue.accentId = 99;
        themeItem.items.add(blue);

        ThemeItem nightBlue = new ThemeItem();
        nightBlue.themeInfo = Theme.getTheme("Dark Blue");
        nightBlue.accentId = 0;
        themeItem.items.add(nightBlue);

        return themeItem;
    }

    public void initColors() {
        getPreviewColors(0, 0);
        getPreviewColors(0, 1);
    }

    @Deprecated
    public String getEmoticon() {
        return emoji;
    }

    public String getEmoticonOrSlug() {
        if (key == null) {
            return null;
        }

        if (key.giftSlug != null) {
            return key.giftSlug;
        }
        return key.emoticon;
    }


    public TLRPC.TL_theme getTlTheme(int index) {
        return items.get(index).tlTheme;
    }

    public ThemeKey getThemeKey() {
        return key;
    }

    public TLRPC.ChatTheme getChatTheme() {
        return chatTheme;
    }

    public ITheme getITheme(int index) {
        return items.get(index);
    }

    public long getThemeId(int index) {
        final ThemeItem item = items.get(index);
        return item.getThemeId();
    }

    public TLRPC.WallPaper getWallpaper(int index) {
        final ThemeItem item = items.get(index);
        final int settingsIndex = item.settingsIndex;
        return item.getThemeWallPaper(settingsIndex);
    }

    public String getWallpaperLink(int index) {
        return items.get(index).wallpaperLink;
    }

    public int getSettingsIndex(int index) {
        return items.get(index).settingsIndex;
    }

    public SparseIntArray getPreviewColors(int currentAccount, int index) {
        SparseIntArray currentColors = items.get(index).currentPreviewColors;
        if (currentColors != null) {
            return currentColors;
        }

        Theme.ThemeInfo themeInfo = getThemeInfo(index);
        Theme.ThemeAccent accent = null;
        if (themeInfo == null) {
            int settingsIndex = getSettingsIndex(index);
            final ITheme iTheme = getITheme(index);
            final TLRPC.TL_theme tlTheme = getTlTheme(index);
            Theme.ThemeInfo baseTheme;
            if (iTheme != null) {
                baseTheme = Theme.getTheme(Theme.getBaseThemeKey(iTheme.getThemeSettings(settingsIndex)));
            } else {
                baseTheme = Theme.getTheme("Blue");
            }
            if (baseTheme != null) {
                themeInfo = new Theme.ThemeInfo(baseTheme);
                if (iTheme != null) {
                    accent = themeInfo.createNewAccent(
                        iTheme.getThemeId(),
                        iTheme.getThemeSettings(settingsIndex),
                        tlTheme,
                        currentAccount,
                        true
                    );
                }
                if (accent != null) {
                    themeInfo.setCurrentAccentId(accent.id);
                }
            }
        } else {
            if (themeInfo.themeAccentsMap != null) {
                accent = themeInfo.themeAccentsMap.get(items.get(index).accentId);
            }
        }

        if (themeInfo == null) {
            return currentColors;
        }

        SparseIntArray currentColorsNoAccent;
        String[] wallpaperLink = new String[1];
        if (themeInfo.pathToFile != null) {
            currentColorsNoAccent = Theme.getThemeFileValues(new File(themeInfo.pathToFile), null, wallpaperLink);
        } else if (themeInfo.assetName != null) {
            currentColorsNoAccent = Theme.getThemeFileValues(null, themeInfo.assetName, wallpaperLink);
        } else {
            currentColorsNoAccent = new SparseIntArray();
        }

        items.get(index).wallpaperLink = wallpaperLink[0];

        if (accent != null) {
            currentColors = currentColorsNoAccent.clone();
            accent.fillAccentColors(currentColorsNoAccent, currentColors);
            if (isGiftTheme() && accent.parentTheme != null && accent.parentTheme.isLight()) {
                accent.resetAccentColorsForMyMessagesGiftThemeLight(currentColors);
            }
        } else {
            currentColors = currentColorsNoAccent;
        }

        SparseIntArray fallbackKeys = Theme.getFallbackKeys();
        SparseIntArray array = new SparseIntArray();
        items.get(index).currentPreviewColors = array;
        try {
            for (int i = 0; i < previewColorKeys.length; i++) {
                int key = previewColorKeys[i];
                int colorIndex = currentColors.indexOfKey(key);
                if (colorIndex >= 0) {
                    array.put(key, currentColors.valueAt(colorIndex));
                } else {
                    int fallbackKey = fallbackKeys.get(key, -1);
                    if (fallbackKey >= 0) {
                        int fallbackIndex = currentColors.indexOfKey(fallbackKey);
                        if (fallbackIndex >= 0) {
                            array.put(key, currentColors.valueAt(fallbackIndex));
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return array;
    }

    public SparseIntArray createColors(int currentAccount, int index) {
        SparseIntArray currentColors;

        Theme.ThemeInfo themeInfo = getThemeInfo(index);
        Theme.ThemeAccent accent = null;
        if (themeInfo == null) {
            int settingsIndex = getSettingsIndex(index);

            final ITheme iTheme = getITheme(index);
            final TLRPC.ThemeSettings settings = iTheme.getThemeSettings(settingsIndex);

            TLRPC.TL_theme tlTheme = getTlTheme(index);
            Theme.ThemeInfo baseTheme = Theme.getTheme(Theme.getBaseThemeKey(settings));
            themeInfo = new Theme.ThemeInfo(baseTheme);
            accent = themeInfo.createNewAccent(
                iTheme.getThemeId(),
                settings,
                tlTheme,
                currentAccount,
                true
            );
            themeInfo.setCurrentAccentId(accent.id);
        } else {
            if (themeInfo.themeAccentsMap != null) {
                accent = themeInfo.themeAccentsMap.get(items.get(index).accentId);
            }
        }

        SparseIntArray currentColorsNoAccent;
        String[] wallpaperLink = new String[1];
        if (themeInfo.pathToFile != null) {
            currentColorsNoAccent = Theme.getThemeFileValues(new File(themeInfo.pathToFile), null, wallpaperLink);
        } else if (themeInfo.assetName != null) {
            currentColorsNoAccent = Theme.getThemeFileValues(null, themeInfo.assetName, wallpaperLink);
        } else {
            currentColorsNoAccent = new SparseIntArray();
        }

        items.get(index).wallpaperLink = wallpaperLink[0];

        if (accent != null) {
            currentColors = currentColorsNoAccent.clone();
            accent.fillAccentColors(currentColorsNoAccent, currentColors);
            if (isGiftTheme() && accent.parentTheme != null && accent.parentTheme.isLight()) {
                accent.resetAccentColorsForMyMessagesGiftThemeLight(currentColors);
            }
        } else {
            currentColors = currentColorsNoAccent;
        }

        SparseIntArray fallbackKeys = Theme.getFallbackKeys();
        for (int i = 0; i < fallbackKeys.size(); i++) {
            int colorKey = fallbackKeys.keyAt(i);
            int fallbackKey = fallbackKeys.valueAt(i);
            if (currentColors.indexOfKey(colorKey) < 0) {
                int fallbackIndex = currentColors.indexOfKey(fallbackKey);
                if (fallbackIndex >= 0) {
                    currentColors.put(colorKey, currentColors.valueAt(fallbackIndex));
                }
            }
        }
        int[] defaultColors = Theme.getDefaultColors();
        for (int i = 0; i < defaultColors.length; i++) {
            if (currentColors.indexOfKey(i) < 0) {
                currentColors.put(i, defaultColors[i]);
            }
        }
        return currentColors;
    }

    public Theme.ThemeInfo getThemeInfo(int index) {
        return items.get(index).themeInfo;
    }

    public void loadWallpaper(int index, ResultCallback<Pair<Long, WallpaperBitmapHolder>> callback) {
        final TLRPC.WallPaper wallPaper = getWallpaper(index);
        if (wallPaper == null) {
            if (callback != null) {
                callback.onComplete(null);
            }
            return;
        }

        long themeId = getThemeId(index);
        loadWallpaperImage(currentAccount, wallPaper.id, wallPaper, wallpaper -> {
            if (callback != null) {
                callback.onComplete(new Pair<>(themeId, wallpaper));
            }
        });
    }

    public static void loadWallpaperImage(int currentAccount, long hash, TLRPC.WallPaper wallPaper, Utilities.Callback<WallpaperBitmapHolder> callback) {
        final int mode = wallPaper.pattern ?
            WallpaperBitmapHolder.MODE_PATTERN:
            WallpaperBitmapHolder.MODE_DEFAULT;

        ChatThemeController.getInstance(currentAccount).loadWallpaperBitmap(hash, mode, (cachedWallpaperBitmapHolder) -> {
            if (cachedWallpaperBitmapHolder != null && callback != null) {
                callback.run(cachedWallpaperBitmapHolder);
                return;
            }
            ImageLocation imageLocation = ImageLocation.getForDocument(wallPaper.document);
            ImageReceiver imageReceiver = new ImageReceiver();
            imageReceiver.setAllowLoadingOnAttachedOnly(false);

            String imageFilter;
            int w = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
            int h = Math.max(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
            imageFilter = (w / AndroidUtilities.density) + "_" + (h / AndroidUtilities.density) + "_f";

            imageReceiver.setImage(imageLocation, imageFilter, null, ".jpg", wallPaper, 1);
            imageReceiver.setDelegate((receiver, set, thumb, memCache) -> {
                ImageReceiver.BitmapHolder holder = receiver.getBitmapSafe();
                ImageReceiver.BitmapHolder dHolder = receiver.getDrawableSafe();
                if (!set || holder == null) {
                    return;
                }
                List<WallpaperGiftPatternPosition> patternPositions = null;
                if (dHolder != null && dHolder.drawable instanceof WallpaperGiftBitmapDrawable) {
                    patternPositions = ((WallpaperGiftBitmapDrawable) dHolder.drawable).patternPositions;
                }

                Bitmap bitmap = holder.bitmap;
                if (bitmap == null && (holder.drawable instanceof BitmapDrawable)) {
                    bitmap = ((BitmapDrawable) holder.drawable).getBitmap();
                }

                final WallpaperBitmapHolder wallpaperBitmapHolder = new WallpaperBitmapHolder(bitmap, mode, patternPositions);
                if (callback != null) {
                    callback.run(wallpaperBitmapHolder);
                }
                ChatThemeController.getInstance(currentAccount).saveWallpaperBitmap(wallpaperBitmapHolder, hash);
            });
            ImageLoader.getInstance().loadImageForImageReceiver(imageReceiver);
        });
    }

    public void loadWallpaperThumb(int index, ResultCallback<Pair<Long, Bitmap>> callback) {
        final TLRPC.WallPaper wallpaper = getWallpaper(index);
        if (wallpaper == null) {
            if (callback != null) {
                callback.onComplete(null);
            }
            return;
        }

        long themeId = getThemeId(index);
        if (themeId == 0) {
            if (callback != null) {
                callback.onComplete(null);
            }
            return;
        }
        Bitmap bitmap = ChatThemeController.getInstance(currentAccount).getWallpaperThumbBitmap(themeId);
        File file = getWallpaperThumbFile(themeId);
        if (bitmap == null && file.exists() && file.length() > 0) {
            try {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (bitmap != null) {
            if (callback != null) {
                callback.onComplete(new Pair<>(themeId, bitmap));
            }
            return;
        }

        if (wallpaper.document == null) {
            if (callback != null) {
                callback.onComplete(new Pair<>(themeId, null));
            }
            return;
        }
        final TLRPC.PhotoSize thumbSize = FileLoader.getClosestPhotoSizeWithSize(wallpaper.document.thumbs, 140);
        ImageLocation imageLocation = ImageLocation.getForDocument(thumbSize, wallpaper.document);
        ImageReceiver imageReceiver = new ImageReceiver();
        imageReceiver.setAllowLoadingOnAttachedOnly(false);
        imageReceiver.setImage(imageLocation, "120_140", null, null, null, 1);
        imageReceiver.setDelegate((receiver, set, thumb, memCache) -> {
            ImageReceiver.BitmapHolder holder = receiver.getBitmapSafe();
            if (!set || holder == null || holder.bitmap.isRecycled()) {
                return;
            }
            Bitmap resultBitmap = holder.bitmap;
            if (resultBitmap == null && (holder.drawable instanceof BitmapDrawable)) {
                resultBitmap = ((BitmapDrawable) holder.drawable).getBitmap();
            }
            if (resultBitmap != null) {
                if (callback != null) {
                    callback.onComplete(new Pair<>(themeId, resultBitmap));
                }
                final Bitmap saveBitmap = resultBitmap;
                Utilities.globalQueue.postRunnable(() -> {
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        saveBitmap.compress(Bitmap.CompressFormat.PNG, 87, outputStream);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                });
            } else {
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
        ImageLoader.getInstance().loadImageForImageReceiver(imageReceiver);
    }

    public void preloadWallpaper() {
        loadWallpaperThumb(0, null);
        loadWallpaperThumb(1, null);
        loadWallpaper(0, null);
        loadWallpaper(1, null);
    }

    private File getWallpaperThumbFile(long themeId) {
        return new File(ApplicationLoader.getFilesDirFixed(), "wallpaper_thumb_" + themeId + ".png");
    }

    public static Theme.ThemeInfo getDefaultThemeInfo(boolean isDark) {
        Theme.ThemeInfo themeInfo = isDark ? Theme.getCurrentNightTheme() : Theme.getCurrentTheme();
        if (isDark != themeInfo.isDark()) {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE);
            String lastThemeName = isDark
                    ? preferences.getString("lastDarkTheme", "Dark Blue")
                    : preferences.getString("lastDayTheme", "Blue");
            themeInfo = Theme.getTheme(lastThemeName);
            if (themeInfo == null) {
                themeInfo = Theme.getTheme(isDark ? "Dark Blue" : "Blue");
            }
        }
        return new Theme.ThemeInfo(themeInfo);
    }

    public static void fillTlTheme(Theme.ThemeInfo themeInfo) {
        if (themeInfo.info == null) {
            themeInfo.info = new TLRPC.TL_theme();
        }
    }

    public static SparseIntArray getPreviewColors(Theme.ThemeInfo themeInfo) {
        SparseIntArray currentColorsNoAccent;
        if (themeInfo.pathToFile != null) {
            currentColorsNoAccent = Theme.getThemeFileValues(new File(themeInfo.pathToFile), null, null);
        } else if (themeInfo.assetName != null) {
            currentColorsNoAccent = Theme.getThemeFileValues(null, themeInfo.assetName, null);
        } else {
            currentColorsNoAccent = new SparseIntArray();
        }
        SparseIntArray currentColors = currentColorsNoAccent.clone();
        Theme.ThemeAccent themeAccent = themeInfo.getAccent(false);
        if (themeAccent != null) {
            themeAccent.fillAccentColors(currentColorsNoAccent, currentColors);
        }
        return currentColors;
    }

    public int getAccentId(int themeIndex) {
        return items.get(themeIndex).accentId;
    }

    public void loadPreviewColors(int currentAccount) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == null) {
                continue;
            }
            SparseIntArray colorsMap = getPreviewColors(currentAccount, i);
            items.get(i).inBubbleColor = getOrDefault(colorsMap, Theme.key_chat_inBubble);
            items.get(i).outBubbleColor = getOrDefault(colorsMap, Theme.key_chat_outBubble);
            items.get(i).outLineColor = getOrDefault(colorsMap, Theme.key_featuredStickers_addButton);
            items.get(i).patternBgColor = colorsMap.get(Theme.key_chat_wallpaper, 0);
            items.get(i).patternBgGradientColor1 = colorsMap.get(Theme.key_chat_wallpaper_gradient_to1, 0);
            items.get(i).patternBgGradientColor2 = colorsMap.get(Theme.key_chat_wallpaper_gradient_to2, 0);
            items.get(i).patternBgGradientColor3 = colorsMap.get(Theme.key_chat_wallpaper_gradient_to3, 0);
            items.get(i).patternBgRotation = colorsMap.get(Theme.key_chat_wallpaper_gradient_rotation, 0);

            if (items.get(i).themeInfo != null && items.get(i).themeInfo.getKey().equals("Blue")) {
                int accentId = items.get(i).accentId >= 0 ? items.get(i).accentId : items.get(i).themeInfo.currentAccentId;
                if (accentId == 99) {
                    items.get(i).patternBgColor = 0xffdbddbb;
                    items.get(i).patternBgGradientColor1 = 0xff6ba587;
                    items.get(i).patternBgGradientColor2 = 0xffd5d88d;
                    items.get(i).patternBgGradientColor3 = 0xff88b884;
                }
            }
        }
    }

    private int getOrDefault(SparseIntArray colorsMap, int key) {
        if (colorsMap == null) return Theme.getDefaultColor(key);
        try {
            int index = colorsMap.indexOfKey(key);
            if (index >= 0) {
                return colorsMap.valueAt(index);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return Theme.getDefaultColor(key);
    }

    public ThemeItem getThemeItem(int index) {
        return items.get(index);
    }

    public static void saveCustomTheme(Theme.ThemeInfo themeInfo, int accentId) {
        if (themeInfo == null) {
            return;
        }
        if (accentId >= 0 && themeInfo.themeAccentsMap != null) {
            Theme.ThemeAccent accent = themeInfo.themeAccentsMap.get(accentId);
            if (accent == null || accent.isDefault) {
                return;
            }
        }
        if (themeInfo.getKey().equals("Blue") && accentId == 99) {
            return;
        }
        if (themeInfo.getKey().equals("Day") && accentId == 9) {
            return;
        }
        if (themeInfo.getKey().equals("Night") && accentId == 0) {
            return;
        }
        if (themeInfo.getKey().equals("Dark Blue") && accentId == 0) {
            return;
        }

        boolean dark = themeInfo.isDark();
        String key = dark ? "lastDarkCustomTheme" : "lastDayCustomTheme";
        String accentKey = dark ? "lastDarkCustomThemeAccentId" : "lastDayCustomThemeAccentId";
        ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE).edit()
                .putString(key, themeInfo.getKey())
                .putInt(accentKey, accentId)
                .apply();
    }

    public static class ThemeItem implements ITheme {

        public Theme.ThemeInfo themeInfo;
        TLRPC.TL_theme tlTheme;
        TLRPC.TL_chatThemeUniqueGift tlChatThemeGift;
        int settingsIndex;
        public int accentId = -1;
        public SparseIntArray currentPreviewColors;
        private String wallpaperLink;

        public int inBubbleColor;
        public int outBubbleColor;
        public int outLineColor;
        public int patternBgColor;
        public int patternBgGradientColor1;
        public int patternBgGradientColor2;
        public int patternBgGradientColor3;
        public int patternBgRotation;


        @Override
        public long getThemeId() {
            if (tlTheme != null) {
                return tlTheme.id;
            } else if (tlChatThemeGift != null) {
                return tlChatThemeGift.gift.gift_id;
            }
            return 0;
        }

        @Override
        public TLRPC.ThemeSettings getThemeSettings(int settingsIndex) {
            ArrayList<TLRPC.ThemeSettings> settings;
            if (tlTheme != null) {
                settings = tlTheme.settings;
            } else if (tlChatThemeGift != null) {
                settings = tlChatThemeGift.theme_settings;
            } else {
                return null;
            }

            if (settings != null && settingsIndex >= 0 && settings.size() > settingsIndex) {
                return settings.get(settingsIndex);
            }

            return null;
        }

        @Override
        public TLRPC.WallPaper getThemeWallPaper(int settingsIndex) {
            final TLRPC.ThemeSettings settings = getThemeSettings(settingsIndex);
            return settings != null ? settings.wallpaper : null;
        }
    }

    public TL_stars.TL_starGiftUnique getThemeGift() {
        if (chatTheme instanceof TLRPC.TL_chatThemeUniqueGift) {
            TL_stars.StarGift gift = ((TLRPC.TL_chatThemeUniqueGift) chatTheme).gift;
            if (gift instanceof TL_stars.TL_starGiftUnique)
            return (TL_stars.TL_starGiftUnique) gift;
        }

        return null;
    }

    public long getBusyByUserId() {
        if (chatTheme instanceof TLRPC.TL_chatThemeUniqueGift) {
            return ChatThemeController.getInstance(currentAccount)
                .getGiftThemeUser(((TLRPC.TL_chatThemeUniqueGift) chatTheme).gift.slug);
        }
        return 0;
    }

    public TLRPC.Document getEmojiAnimatedSticker() {
        if (chatTheme instanceof TLRPC.TL_chatThemeUniqueGift) {
            return TlUtils.getGiftDocument(((TLRPC.TL_chatThemeUniqueGift) chatTheme).gift);
        } else if (chatTheme instanceof TLRPC.TL_chatTheme) {
            return MediaDataController.getInstance(currentAccount)
                    .getEmojiAnimatedSticker(((TLRPC.TL_chatTheme) chatTheme).emoticon);
        }
        return null;
    }

    public void loadWallpaperGiftPattern(int index, ResultCallback<Pair<Long, Bitmap>> callback) {
        final ThemeItem item = getThemeItem(index);
        if (item != null && item.tlChatThemeGift != null) {
            long themeId = getThemeId(index);
            loadWallpaperGiftPattern(currentAccount, themeId, item.tlChatThemeGift.gift, callback);
        }
    }

    public static void loadWallpaperGiftPattern(int currentAccount, long hash, TL_stars.StarGift gift, ResultCallback<Pair<Long, Bitmap>> callback) {
        //ChatThemeController.getInstance(currentAccount).getWallpaperBitmap(hash, cachedBitmap -> {
            /*if (cachedBitmap != null && callback != null) {
                callback.onComplete(new Pair<>(hash, cachedBitmap));
                return;
            }*/

        TLRPC.Document document = TlUtils.getGiftDocumentPattern(gift);
        ImageLocation imageLocation = ImageLocation.getForDocument(document);
        ImageReceiver imageReceiver = new ImageReceiver();
        imageReceiver.setAllowLoadingOnAttachedOnly(false);

        imageReceiver.setImage(imageLocation, "40_40_firstframe", null, ".jpg", document, 1);
        imageReceiver.setDelegate((receiver, set, thumb, memCache) -> {
            ImageReceiver.BitmapHolder holder = receiver.getBitmapSafe();
            if (!set || holder == null) {
                return;
            }
            Bitmap bitmap = holder.bitmap;
            if (bitmap == null && (holder.drawable instanceof BitmapDrawable)) {
                bitmap = ((BitmapDrawable) holder.drawable).getBitmap();
            }
            if (callback != null) {
                callback.onComplete(new Pair<>(hash, bitmap));
            }
            // ChatThemeController.getInstance(currentAccount).saveWallpaperBitmap(bitmap, hash);
        });
        ImageLoader.getInstance().loadImageForImageReceiver(imageReceiver);
        //});
    }

}
