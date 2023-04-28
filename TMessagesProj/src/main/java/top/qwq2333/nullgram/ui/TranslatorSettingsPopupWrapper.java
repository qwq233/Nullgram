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

package top.qwq2333.nullgram.ui;

import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import kotlin.Unit;
import top.qwq2333.nullgram.helpers.TranslateHelper;

public class TranslatorSettingsPopupWrapper {

    public ActionBarPopupWindow.ActionBarPopupWindowLayout windowLayout;

    public TranslatorSettingsPopupWrapper(BaseFragment fragment, PopupSwipeBackLayout swipeBackLayout, long dialogId, int topicId, Theme.ResourcesProvider resourcesProvider) {
        var context = fragment.getParentActivity();
        windowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, resourcesProvider, ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK);
        windowLayout.setFitItems(true);

        if (swipeBackLayout != null) {
            var backItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_arrow_back, LocaleController.getString("Back", R.string.Back), false, resourcesProvider);
            backItem.setOnClickListener(view -> swipeBackLayout.closeForeground());
        }

        var items = new String[]{
            LocaleController.getString("TranslatorType", R.string.TranslatorType),
            LocaleController.getString("TranslationTarget", R.string.TranslationTarget),
            LocaleController.getString("TranslationProvider", R.string.TranslationProvider),
        };
        for (int i = 0; i < items.length; i++) {
            var item = ActionBarMenuItem.addItem(windowLayout, 0, items[i], false, resourcesProvider);
            item.setTag(i);
            item.setOnClickListener(view -> {
                switch ((Integer) view.getTag()) {
                    case 0:
                        TranslateHelper.showTranslatorTypeSelector(context, null, resourcesProvider, () -> Unit.INSTANCE);
                        break;
                    case 1:
                        TranslateHelper.showTranslationTargetSelector(fragment, null, false, resourcesProvider, () -> Unit.INSTANCE);
                        break;
                    case 2:
                        TranslateHelper.showTranslationProviderSelector(context, null, resourcesProvider, ignore -> Unit.INSTANCE);
                        break;
                }
            });
        }
        var subSwipeBackLayout = windowLayout.getSwipeBack();
        if (subSwipeBackLayout != null) {
            subSwipeBackLayout.addOnSwipeBackProgressListener((layout, toProgress, progress) -> {
                if (swipeBackLayout != null) {
                    swipeBackLayout.setSwipeBackDisallowed(progress != 0);
                }
            });

            FrameLayout gap = new FrameLayout(context);
            gap.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuSeparator, resourcesProvider));
            View gapShadow = new View(context);
            gapShadow.setBackground(Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
            gap.addView(gapShadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            gap.setTag(R.id.fit_width_tag, 1);
            windowLayout.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));

            var autoTranslatePopupWrapper = new AutoTranslatePopupWrapper(fragment, windowLayout.getSwipeBack(), dialogId, topicId, resourcesProvider);
            int autoTranslateSwipeBackIndex = windowLayout.addViewToSwipeBack(autoTranslatePopupWrapper.windowLayout);
            var autoTranslateItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_translate, LocaleController.getString("AutoTranslate", R.string.AutoTranslate), true, resourcesProvider);
            autoTranslateItem.setRightIcon(R.drawable.msg_arrowright);
            autoTranslateItem.setOnClickListener(view -> subSwipeBackLayout.openForeground(autoTranslateSwipeBackIndex));
        }
    }
}

