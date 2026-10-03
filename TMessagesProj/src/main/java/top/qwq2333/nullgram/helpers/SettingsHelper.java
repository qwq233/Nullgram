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

package top.qwq2333.nullgram.helpers;

import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.Map;

import top.qwq2333.nullgram.activity.BaseActivity;
import top.qwq2333.nullgram.activity.ChatSettingActivity;
import top.qwq2333.nullgram.activity.ExperimentSettingActivity;
import top.qwq2333.nullgram.activity.GeneralSettingActivity;
import top.qwq2333.nullgram.activity.MainSettingActivity;
import top.qwq2333.nullgram.activity.PasscodeSettingActivity;

public class SettingsHelper {

    public static void processDeepLink(Uri uri, Callback callback, Runnable unknown) {
        if (uri == null) {
            unknown.run();
            return;
        }
        var segments = uri.getPathSegments();
        if (segments.isEmpty() || segments.size() > 2 || !"nullsetting".equals(segments.get(0))) {
            unknown.run();
            return;
        }
        BaseActivity fragment;
        if (segments.size() == 1) {
            fragment = new MainSettingActivity();
        } else if (PasscodeHelper.getSettingsKey().equals(segments.get(1))) {
            fragment = new PasscodeSettingActivity();
        } else {
            switch (segments.get(1)) {
                case "chat":
                case "chats":
                case "c":
                    fragment = new ChatSettingActivity();
                    break;
                case "experimental":
                case "e":
                    fragment = new ExperimentSettingActivity(false, false);
                    break;
                case "general":
                case "g":
                    fragment = new GeneralSettingActivity();
                    break;
                default:
                    unknown.run();
                    return;
            }
        }
        callback.presentFragment(fragment);
        var row = uri.getQueryParameter("r");
        if (TextUtils.isEmpty(row)) {
            row = uri.getQueryParameter("row");
        }
        if (!TextUtils.isEmpty(row)) {
            var rowFinal = row;
            AndroidUtilities.runOnUIThread(() -> fragment.scrollToRow(rowFinal, unknown));
        }

    }

    public interface Callback {
        void presentFragment(BaseFragment fragment);
    }

    public static ArrayList<SettingsSearchResult> onCreateSearchArray(Callback callback) {
        ArrayList<SettingsSearchResult> items = new ArrayList<>();
        ArrayList<BaseActivity> fragments = new ArrayList<>();
        fragments.add(new GeneralSettingActivity());
        fragments.add(new ChatSettingActivity());
        fragments.add(new ExperimentSettingActivity(false, false));
        String n_title = LocaleController.getString(R.string.NullSettings);
        for (BaseActivity fragment: fragments) {
            int uid = fragment.getBaseGuid();
            int drawable = fragment.getDrawable();
            String f_title = fragment.getTitle();
            for (Map.Entry<Integer, String> entry : fragment.getRowMapReverse().entrySet()) {
                Integer i = entry.getKey();
                String key = entry.getValue();
                if (key.equals(String.valueOf(i))) {
                    continue;
                }
                int guid = uid + i;
                String key1 = key.substring(0, 1).toUpperCase() + key.substring(1);
                String key2 = key.substring(0, 1).toLowerCase() + key.substring(1);
                String title1 = LocaleController.getString(key1);
                String title2 = LocaleController.getString(key2);
                String title = (title1 != null && !title1.isEmpty()) ? title1 : (title2 != null && !title2.isEmpty()) ? title2 : null;
                if (title == null) {
                    continue;
                }
                Runnable open = () -> {
                    callback.presentFragment(fragment);
                    AndroidUtilities.runOnUIThread(() -> fragment.scrollToRow(key, null));
                };
                SettingsSearchResult result = new SettingsSearchResult(
                    guid, title, n_title, f_title, drawable, open
                );
                items.add(result);
            }
        }
        return items;
    }
}
