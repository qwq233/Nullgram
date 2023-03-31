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

package top.qwq2333.nullgram.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import top.qwq2333.nullgram.helpers.TranslateHelper;

public class DialogConfig {
    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("dialogconfig", Context.MODE_PRIVATE);

    public static boolean isAutoTranslateEnable(long dialog_id, int topicId) {
        return preferences.getBoolean("autoTranslate_" + dialog_id + (topicId != 0 ? "_" + topicId : ""), TranslateHelper.getAutoTranslate());
    }

    public static boolean hasAutoTranslateConfig(long dialog_id, int topicId) {
        return preferences.contains("autoTranslate_" + dialog_id + (topicId != 0 ? "_" + topicId : ""));
    }

    public static void setAutoTranslateEnable(long dialog_id, int topicId, boolean enable) {
        preferences.edit().putBoolean("autoTranslate_" + dialog_id + (topicId != 0 ? "_" + topicId : ""), enable).apply();
    }

    public static void removeAutoTranslateConfig(long dialog_id, int topicId) {
        preferences.edit().remove("autoTranslate_" + dialog_id + (topicId != 0 ? "_" + topicId : "")).apply();
    }

}
