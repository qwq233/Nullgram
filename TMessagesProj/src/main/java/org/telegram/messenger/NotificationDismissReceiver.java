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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int currentAccount = intent.getIntExtra("currentAccount", UserConfig.selectedAccount);
        if (!UserConfig.isValidAccount(currentAccount)) {
            return;
        }
        long dialogId = intent.getLongExtra("dialogId", 0);
        int date = intent.getIntExtra("messageDate", 0);
        if (intent.hasExtra("story") && intent.getBooleanExtra("story", false)) {
            NotificationsController.getInstance(currentAccount).processIgnoreStories();
        } else if (intent.hasExtra("storyReaction") && intent.getBooleanExtra("storyReaction", false)) {
            NotificationsController.getInstance(currentAccount).processIgnoreStoryReactions();
        } else if (dialogId == 0) {
            FileLog.d("set dismissDate of global to " + date);
            MessagesController.getNotificationsSettings(currentAccount).edit().putInt("dismissDate", date).commit();
        } else {
            FileLog.d("set dismissDate of " + dialogId + " to " + date);
            MessagesController.getNotificationsSettings(currentAccount).edit().putInt("dismissDate" + dialogId, date).commit();
        }
    }
}
