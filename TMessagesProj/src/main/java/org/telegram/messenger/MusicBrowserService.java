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

package org.telegram.messenger;

import android.annotation.TargetApi;
import android.media.browse.MediaBrowser;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.service.media.MediaBrowserService;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MusicBrowserService extends MediaBrowserService {

    private static final String MEDIA_ID_ROOT = "__ROOT__";

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
        TelegramMediaSession holder = TelegramMediaSession.getInstance(this);
        setSessionToken(holder.getFrameworkSessionToken());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        if (clientPackageName == null) {
            return null;
        }
        boolean isSelf = Process.SYSTEM_UID == clientUid || Process.myUid() == clientUid;
        if (!isSelf && !PackageValidator.isKnownCaller(this, clientPackageName, clientUid)) {
            return null;
        }
        if (TelegramMediaSession.getInstance(this).isPasscodeLocked()) {
            return null;
        }
        return new BrowserRoot(MEDIA_ID_ROOT, TelegramMediaSession.getInstance(this).buildRootHints());
    }

    @Override
    public void onLoadChildren(@NonNull String parentMediaId, @NonNull Result<List<MediaBrowser.MediaItem>> result) {
        TelegramMediaSession holder = TelegramMediaSession.getInstance(this);
        if (holder.isPasscodeLocked()) {
            Toast.makeText(getApplicationContext(), LocaleController.getString(R.string.EnterYourTelegramPasscode), Toast.LENGTH_LONG).show();
            stopSelf();
            result.detach();
            return;
        }
        result.detach();
        holder.loadBrowseChildren(parentMediaId, result::sendResult);
    }
}
