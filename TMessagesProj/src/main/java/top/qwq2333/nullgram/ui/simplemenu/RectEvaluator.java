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

package top.qwq2333.nullgram.ui.simplemenu;

import android.animation.TypeEvaluator;
import android.annotation.SuppressLint;
import android.graphics.Rect;

/**
 * This evaluator can be used to perform type interpolation between {@link Rect}.
 */

class RectEvaluator implements TypeEvaluator<Rect> {

    private final Rect mMax;
    private final Rect mTemp = new Rect();

    public RectEvaluator(Rect max) {
        mMax = max;
    }

    @SuppressLint("CheckResult")
    @Override
    public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
        mTemp.left = startValue.left + (int) ((endValue.left - startValue.left) * fraction);
        mTemp.top = startValue.top + (int) ((endValue.top - startValue.top) * fraction);
        mTemp.right = startValue.right + (int) ((endValue.right - startValue.right) * fraction);
        mTemp.bottom = startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction);
        mTemp.setIntersect(mMax, mTemp);
        return mTemp;
    }
}
