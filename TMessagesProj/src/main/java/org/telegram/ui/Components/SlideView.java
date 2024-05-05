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
import android.os.Bundle;
import android.widget.LinearLayout;

public class SlideView extends LinearLayout {

    public SlideView(Context context) {
        super(context);
    }

    public String getHeaderName() {
        return "";
    }

    public void setParams(Bundle params, boolean restore) {

    }

    public boolean onBackPressed(boolean force) {
        return true;
    }

    public void onShow() {}

    public void onHide() {}

    public void updateColors() {}

    public boolean hasCustomKeyboard() {
        return false;
    }

    public void onDestroyActivity() {

    }

    public void onNextPressed(String code) {

    }

    public void onCancelPressed() {

    }

    public void saveStateParams(Bundle bundle) {

    }

    public void restoreStateParams(Bundle bundle) {

    }

    public boolean needBackButton() {
        return false;
    }

    public void onResume() {

    }
}
