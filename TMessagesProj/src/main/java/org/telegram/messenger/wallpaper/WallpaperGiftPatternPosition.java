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

import android.graphics.Matrix;
import android.graphics.RectF;

import androidx.annotation.Nullable;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.InputSerializedData;
import org.telegram.tgnet.OutputSerializedData;
import org.xml.sax.Attributes;

public class WallpaperGiftPatternPosition {
    public final RectF rect;
    public final Matrix matrix;

    private WallpaperGiftPatternPosition(RectF rect, Matrix matrix) {
        this.rect = rect;
        this.matrix = matrix;
    }

    @Nullable
    public static WallpaperGiftPatternPosition create(Attributes attributes, float scale) {
        try {
            final float x = Float.parseFloat(attributes.getValue("x"));
            final float y = Float.parseFloat(attributes.getValue("y"));
            final float w = Float.parseFloat(attributes.getValue("width"));
            final float h = Float.parseFloat(attributes.getValue("height"));
            final RectF rect = new RectF(x, y, x + w, y + h);

            final Matrix transform = SvgHelper.parseTransform(attributes.getValue("transform"));
            transform.postScale(scale, scale);

            return new WallpaperGiftPatternPosition(rect, transform);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }


    public static final int SERIALIZED_BUFFER_SIZE = (4 + 9) * 4;

    public void serialize(OutputSerializedData data) {
        data.writeFloat(rect.left);
        data.writeFloat(rect.top);
        data.writeFloat(rect.width());
        data.writeFloat(rect.height());

        final float[] m = new float[9];
        matrix.getValues(m);
        for (float v : m) {
            data.writeFloat(v);
        }
    }

    public static WallpaperGiftPatternPosition deserialize(InputSerializedData data) {
        final float rx = data.readFloat(true);
        final float ry = data.readFloat(true);
        final float rw = data.readFloat(true);
        final float rh = data.readFloat(true);

        final float[] m = new float[] {
            data.readFloat(true),
            data.readFloat(true),
            data.readFloat(true),
            data.readFloat(true),
            data.readFloat(true),
            data.readFloat(true),
            data.readFloat(true),
            data.readFloat(true),
            data.readFloat(true)
        };

        final RectF rectF = new RectF(rx, ry, rx + rw, ry + rh);
        final Matrix matrix = new Matrix();
        matrix.setValues(m);

        return new WallpaperGiftPatternPosition(rectF, matrix);
    }
}
