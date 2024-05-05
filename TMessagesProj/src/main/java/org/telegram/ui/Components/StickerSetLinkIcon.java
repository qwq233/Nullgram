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

package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class StickerSetLinkIcon extends Drawable {

    private final int N, count;
    private final AnimatedEmojiDrawable[] drawables;
    public int alpha = 0xFF;
    public final boolean out;

    public StickerSetLinkIcon(int currentAccount, boolean out, ArrayList<TLRPC.Document> documents, boolean text_color) {
        this.out = out;
        N = (int) Math.max(1, Math.sqrt(documents.size()));
        count = Math.min(N * N, documents.size());
        drawables = new AnimatedEmojiDrawable[count];
        final boolean emoji = !documents.isEmpty() && MessageObject.isAnimatedEmoji(documents.get(0));
        final int cacheType = N < 2 ? AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES_LARGE : AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES;
        for (int i = 0; i < count; ++i) {
            drawables[i] = AnimatedEmojiDrawable.make(currentAccount, cacheType, documents.get(i));
        }
    }

    public boolean equals(ArrayList<TLRPC.Document> documents) {
        if (documents == null)
            return drawables.length == 0;
        if (drawables.length != documents.size())
            return false;
        for (int i = 0; i < drawables.length; ++i) {
            TLRPC.Document d = drawables[i].getDocument();
            if ((d == null ? 0 : d.id) != documents.get(i).id) {
                return false;
            }
        }
        return true;
    }

    public void attach(View parentView) {
        for (int i = 0; i < count; ++i) {
            drawables[i].addView(parentView);
        }
    }

    public void detach(View parentView) {
        for (int i = 0; i < count; ++i) {
            drawables[i].removeView(parentView);
        }
    }

    private final RectF rect = new RectF();
    @Override
    public void draw(@NonNull Canvas canvas) {
        if (alpha <= 0) return;
        rect.set(getBounds());
        final float left = rect.centerX() - getIntrinsicWidth() / 2f;
        final float top = rect.centerY() - getIntrinsicHeight() / 2f;
        final float iw = getIntrinsicWidth() / N;
        final float ih = getIntrinsicHeight() / N;
        canvas.save();
        canvas.clipRect(left, top, left + getIntrinsicWidth(), top + getIntrinsicHeight());
        for (int y = 0; y < N; ++y) {
            for (int x = 0; x < N; ++x) {
                int i = x + y * N;
                if (i < 0 || i >= drawables.length) continue;
                if (drawables[i] == null) continue;
                drawables[i].setBounds(
                    (int) (left + iw * x),
                    (int) (top + ih * y),
                    (int) (left + iw * (x + 1)),
                    (int) (top + ih * (y + 1))
                );
                drawables[i].setAlpha(alpha);
                drawables[i].setColorFilter(out ? Theme.chat_outAnimatedEmojiTextColorFilter : Theme.chat_animatedEmojiTextColorFilter);
                drawables[i].draw(canvas);
            }
        }
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getIntrinsicHeight() {
        return dp(48);
    }

    @Override
    public int getIntrinsicWidth() {
        return dp(48);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    private boolean hit = false;
    public void readyToDie() {
        hit = true;
    }

    public void keepAlive() {
        hit = false;
    }

    public boolean die() {
        return hit;
    }
}
