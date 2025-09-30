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

package org.telegram.messenger.wallpaper;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.util.List;

public class WallpaperBitmapHolder {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_PATTERN = 1;

    public final int mode;
    public final Bitmap bitmap;

    public final @Nullable List<WallpaperGiftPatternPosition> giftPatternPositions;
    
    public WallpaperBitmapHolder(Bitmap bitmap, int mode) {
        this(bitmap, mode, null);
    }
    
    public WallpaperBitmapHolder(
        Bitmap bitmap, 
        int mode, 
        @Nullable List<WallpaperGiftPatternPosition> giftPatternPositions
    ) {
        this.giftPatternPositions = giftPatternPositions;
        this.bitmap = bitmap;
        this.mode = mode;
    }
}
