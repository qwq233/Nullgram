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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class ScaleStateListAnimator {

    public static void apply(View view) {
        apply(view, .1f, 1.5f);
    }

    public static void apply(View view, float scale, float tension) {
        if (view == null) {
            return;
        }

        AnimatorSet pressedAnimator = new AnimatorSet();
        pressedAnimator.playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 1f - scale),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f - scale)
        );
        pressedAnimator.setDuration(80);

        AnimatorSet defaultAnimator = new AnimatorSet();
        defaultAnimator.playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f)
        );
        defaultAnimator.setInterpolator(new OvershootInterpolator(tension));
        defaultAnimator.setDuration(350);

        StateListAnimator scaleStateListAnimator = new StateListAnimator();

        scaleStateListAnimator.addState(new int[]{android.R.attr.state_pressed}, pressedAnimator);
        scaleStateListAnimator.addState(new int[0], defaultAnimator);

        view.setStateListAnimator(scaleStateListAnimator);
    }

    public static void reset(View view) {
        view.setStateListAnimator(null);
    }

}
