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

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

public class ProfileDateHelper {
    private static String JSON_FILE = "id_date.json";
    private static final ArrayList<ProfileDateData> profileDateDataList = new ArrayList<>();

    private static void loadData() {
        try {
            InputStream in = ApplicationLoader.applicationContext.getAssets().open(JSON_FILE);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[10240];
            int c;
            while ((c = in.read(buffer)) != -1) {
                bos.write(buffer, 0, c);
            }
            bos.close();
            in.close();
            String json = bos.toString("UTF-8");
            JSONObject object = new JSONObject(json);
            JSONArray data = object.getJSONArray("data");
            profileDateDataList.clear();
            for (int i = 0; i<data.length(); i++){
                JSONObject o = data.getJSONObject(i);
                profileDateDataList.add(new ProfileDateData(o.getLong("id"), o.getLong("date")));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static String getUserTime(String key, int stringRes, long date) {
        String st = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, LocaleController.getInstance().getFormatterYear().format(new Date(date)), LocaleController.getInstance().getFormatterDay().format(new Date(date)));
        return LocaleController.formatString(key, stringRes, st);
    }

    public static String getUserTime(Long userId) {
        if (profileDateDataList.isEmpty()) {
            loadData();
        }
        if (profileDateDataList.isEmpty()) {
            return "unknown";
        }
        for (int i = 1; i < profileDateDataList.size(); i++){
            ProfileDateData data1 = profileDateDataList.get(i - 1);
            ProfileDateData data2 = profileDateDataList.get(i);
            if (userId >= data1.getId() && userId <= data2.getId()) {
                long idx = userId - data1.getId();
                long idxRange = data2.getId() - data1.getId();
                double t = (double) idx / idxRange;
                long date1 = data1.getDate();
                long date2 = data2.getDate();
                double date = (date1 + t * (date2 - date1)) * 1000.0;
                long dateLong = Math.round(date);
                return getUserTime("RegistrationDateApproximately", R.string.RegistrationDateApproximately, dateLong);
            }
        }
        if (userId <= 1000000) {
            return getUserTime("RegistrationDateOlder", R.string.RegistrationDateOlder, 1380326400000L);
        }
        return getUserTime("RegistrationDateNewer", R.string.RegistrationDateNewer, 1711889200000L);
    }

    public static class ProfileDateData {
        private final long id;
        private final long date;

        public ProfileDateData(long id, long date) {
            this.id = id;
            this.date = date;
        }

        public long getId() {
            return id;
        }

        public long getDate() {
            return date;
        }
    }
}
