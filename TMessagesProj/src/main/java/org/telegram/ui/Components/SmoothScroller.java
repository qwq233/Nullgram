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

import android.content.Context;
import android.graphics.PointF;
import android.view.View;
import android.view.animation.Interpolator;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;

public class SmoothScroller extends LinearSmoothScroller {

    private Interpolator interpolator = CubicBezierInterpolator.DEFAULT;

    private int offset;
    private float durationScale = 1f;

    public SmoothScroller(Context context) {
        super(context);
    }

    public SmoothScroller(Context context, Interpolator customInterpolator) {
        super(context);
        this.interpolator = customInterpolator;
    }

    protected void onEnd() {

    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setDurationScale(float scale) {
        this.durationScale = scale;
    }

    @Override
    protected void updateActionForInterimTarget(Action action) {
        // find an interim target position
        PointF scrollVector = computeScrollVectorForPosition(getTargetPosition());
        if (scrollVector == null || (scrollVector.x == 0 && scrollVector.y == 0)) {
            final int target = getTargetPosition();
            action.jumpTo(target);
            stop();
            return;
        }
        normalize(scrollVector);
        mTargetVector = scrollVector;

        mInterimTargetDx = (int) (TARGET_SEEK_SCROLL_DISTANCE_PX * scrollVector.x);
        mInterimTargetDy = (int) (TARGET_SEEK_SCROLL_DISTANCE_PX * scrollVector.y);
        final int time = calculateTimeForScrolling(TARGET_SEEK_SCROLL_DISTANCE_PX);
        // To avoid UI hiccups, trigger a smooth scroll to a distance little further than the
        // interim target. Since we track the distance travelled in onSeekTargetStep callback, it
        // won't actually scroll more than what we need.
        action.update((int) (mInterimTargetDx * TARGET_SEEK_EXTRA_SCROLL_RATIO),
                (int) (mInterimTargetDy * TARGET_SEEK_EXTRA_SCROLL_RATIO),
                (int) (time * TARGET_SEEK_EXTRA_SCROLL_RATIO), interpolator);
    }

    @Override
    protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
        final int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
        final int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
        final int distance = (int) Math.sqrt(dx * dx + dy * dy);
        final int time = calculateTimeForDeceleration(distance);
        if (time > 0) {
            action.update(-dx, -dy, time, interpolator);
        }
        AndroidUtilities.runOnUIThread(this::onEnd, Math.max(0, time));
    }

    @Override
    public int calculateDyToMakeVisible(View view, int snapPreference) {
        return super.calculateDyToMakeVisible(view, snapPreference) - offset;
    }

    protected int calculateTimeForDeceleration(int dx) {
        return Math.round(Math.min(super.calculateTimeForDeceleration(dx), 500) * durationScale);
    }
    protected int calculateTimeForScrolling(int dx) {
        return Math.round(Math.min(super.calculateTimeForScrolling(dx), 150) * durationScale);
    }
}
