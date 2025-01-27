/*
 * Copyright (C) 2019-2025 qwq233 <qwq233@qwq2333.top>
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

package org.telegram.ui.Business;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.tl.TL_account;

import java.util.ArrayList;

public class BusinessChatbotController {

    private static volatile BusinessChatbotController[] Instance = new BusinessChatbotController[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];
    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }
    }
    public static BusinessChatbotController getInstance(int num) {
        BusinessChatbotController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (lockObjects[num]) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new BusinessChatbotController(num);
                }
            }
        }
        return localInstance;
    }

    private final int currentAccount;
    private BusinessChatbotController(int account) {
        this.currentAccount = account;
    }

    private long lastTime;
    private TL_account.connectedBots value;
    private ArrayList<Utilities.Callback<TL_account.connectedBots>> callbacks = new ArrayList<>();
    private boolean loading, loaded;

    public void load(Utilities.Callback<TL_account.connectedBots> callback) {
        callbacks.add(callback);
        if (loading) return;
        if (System.currentTimeMillis() - lastTime > 1000 * 60 || !loaded) {
            loading = true;
            ConnectionsManager.getInstance(currentAccount).sendRequest(new TL_account.getConnectedBots(), (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                loading = false;
                value = res instanceof TL_account.connectedBots ? (TL_account.connectedBots) res : null;
                if (value != null) {
                    MessagesController.getInstance(currentAccount).putUsers(value.users, false);
                }
                lastTime = System.currentTimeMillis();
                loaded = true;

                for (int i = 0; i < callbacks.size(); ++i) {
                    if (callbacks.get(i) != null) {
                        callbacks.get(i).run(value);
                    }
                }
                callbacks.clear();
            }));
        } else if (loaded) {
            for (int i = 0; i < callbacks.size(); ++i) {
                if (callbacks.get(i) != null) {
                    callbacks.get(i).run(value);
                }
            }
            callbacks.clear();
        }
    }

    public void invalidate(boolean reload) {
        loaded = false;
        if (reload) {
            load(null);
        }
    }
}
