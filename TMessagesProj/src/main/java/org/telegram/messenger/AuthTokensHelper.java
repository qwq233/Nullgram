/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
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

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AuthTokensHelper {

    public static ArrayList<TLRPC.TL_auth_loggedOut> getSavedLogOutTokens() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("saved_tokens", Context.MODE_PRIVATE);
        int count = preferences.getInt("count", 0);

        if (count == 0) {
            return null;
        }

        ArrayList<TLRPC.TL_auth_loggedOut> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String value = preferences.getString("log_out_token_" + i, "");
            SerializedData serializedData = new SerializedData(Utilities.hexToBytes(value));
            TLRPC.TL_auth_loggedOut token = TLRPC.TL_auth_loggedOut.TLdeserialize(serializedData, serializedData.readInt32(true), true);
            if (token != null) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    public static void saveLogOutTokens(ArrayList<TLRPC.TL_auth_loggedOut> tokens) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("saved_tokens", Context.MODE_PRIVATE);
        ArrayList<TLRPC.TL_auth_loggedOut> activeTokens = new ArrayList<>();
        preferences.edit().clear().apply();
        int date = (int) (System.currentTimeMillis() / 1000L);
        for (int i = 0; i < Math.min(20, tokens.size()); i++) {
            activeTokens.add(tokens.get(i));
        }
        if (activeTokens.size() > 0) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("count", activeTokens.size());
            for (int i = 0; i < activeTokens.size(); i++) {
                SerializedData data = new SerializedData(activeTokens.get(i).getObjectSize());
                activeTokens.get(i).serializeToStream(data);
                editor.putString("log_out_token_" + i, Utilities.bytesToHex(data.toByteArray()));
            }
            editor.apply();
            //   BackupAgent.requestBackup(ApplicationLoader.applicationContext);
        }
    }

    public static ArrayList<TLRPC.TL_auth_authorization> getSavedLogInTokens() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("saved_tokens_login", Context.MODE_PRIVATE);
        int count = preferences.getInt("count", 0);

        if (count == 0) {
            return null;
        }

        ArrayList<TLRPC.TL_auth_authorization> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String value = preferences.getString("log_in_token_" + i, "");
            try {
                SerializedData serializedData = new SerializedData(Utilities.hexToBytes(value));
                TLRPC.auth_Authorization token = TLRPC.auth_Authorization.TLdeserialize(serializedData, serializedData.readInt32(true), true);
                if (token instanceof TLRPC.TL_auth_authorization) {
                    tokens.add((TLRPC.TL_auth_authorization) token);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        return tokens;
    }

    public static void saveLogInToken(TLRPC.TL_auth_authorization token) {
        if (BuildVars.DEBUG_VERSION) {
            FileLog.d("saveLogInToken " + new String(token.future_auth_token, StandardCharsets.UTF_8));
        }
        ArrayList<TLRPC.TL_auth_authorization> tokens = getSavedLogInTokens();
        if (tokens == null) {
            tokens = new ArrayList<>();
        }
        tokens.add(0, token);
        saveLogInTokens(tokens);
    }

    private static void saveLogInTokens(ArrayList<TLRPC.TL_auth_authorization> tokens) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("saved_tokens_login", Context.MODE_PRIVATE);
        ArrayList<TLRPC.TL_auth_authorization> activeTokens = new ArrayList<>();
        preferences.edit().clear().apply();
        for (int i = 0; i < Math.min(20, tokens.size()); i++) {
            activeTokens.add(tokens.get(i));
        }
        if (activeTokens.size() > 0) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("count", activeTokens.size());
            for (int i = 0; i < activeTokens.size(); i++) {
                SerializedData data = new SerializedData(activeTokens.get(i).getObjectSize());
                activeTokens.get(i).serializeToStream(data);
                editor.putString("log_in_token_" + i, Utilities.bytesToHex(data.toByteArray()));
            }
            editor.apply();
            BackupAgent.requestBackup(ApplicationLoader.applicationContext);
        }
    }

    public static void addLogOutToken(TLRPC.TL_auth_loggedOut response) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("saved_tokens", Context.MODE_PRIVATE);
        int count = preferences.getInt("count", 0);
        SerializedData data = new SerializedData(response.getObjectSize());
        response.serializeToStream(data);
        preferences.edit().putString("log_out_token_" + count, Utilities.bytesToHex(data.toByteArray())).putInt("count", count + 1).apply();
        BackupAgent.requestBackup(ApplicationLoader.applicationContext);
    }

    public static void clearLogInTokens() {
        ApplicationLoader.applicationContext.getSharedPreferences("saved_tokens_login", Context.MODE_PRIVATE).edit().clear().apply();
        ApplicationLoader.applicationContext.getSharedPreferences("saved_tokens", Context.MODE_PRIVATE).edit().clear().apply();
    }
}
