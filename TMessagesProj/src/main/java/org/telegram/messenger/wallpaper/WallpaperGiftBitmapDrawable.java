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
import android.graphics.drawable.BitmapDrawable;

import java.util.List;

public class WallpaperGiftBitmapDrawable extends BitmapDrawable {
    public final List<WallpaperGiftPatternPosition> patternPositions;

    public WallpaperGiftBitmapDrawable(Bitmap bitmap, List<WallpaperGiftPatternPosition> positions) {
        super(bitmap);
        this.patternPositions = positions;
    }

    public static BitmapDrawable create(Bitmap bitmap, List<WallpaperGiftPatternPosition> positions) {
        if (bitmap == null) {
            return null;
        }

        if (positions == null || positions.isEmpty()) {
            return new BitmapDrawable(bitmap);
        } else {
            return new WallpaperGiftBitmapDrawable(bitmap, positions);
        }
    }
}
