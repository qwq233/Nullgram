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

package top.qwq2333.nullgram.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public abstract class BaseActivity extends BaseFragment {
    protected BlurredRecyclerView listView;
    protected BaseListAdapter listAdapter;
    protected LinearLayoutManager layoutManager;
    protected Theme.ResourcesProvider resourcesProvider;

    protected int rowCount;
    protected static final Object PARTIAL = new Object();

    public static final int TYPE_SHADOW = 1;
    public static final int TYPE_SETTINGS = 2;
    public static final int TYPE_CHECK = 3;
    public static final int TYPE_HEADER = 4;
    public static final int TYPE_NOTIFICATION_CHECK = 5;
    public static final int TYPE_DETAIL_SETTINGS = 6;
    public static final int TYPE_INFO_PRIVACY = 7;
    public static final int TYPE_TEXT = 8;
    public static final int TYPE_CHECKBOX = 9;
    public static final int TYPE_RADIO = 10;
    public static final int TYPE_ACCOUNT = 11;
    public static final int TYPE_STICKER_SIZE = 12;
    protected HashMap<String, Integer> rowMap = new HashMap<>(20);
    protected HashMap<Integer, String> rowMapReverse = new HashMap<>(20);

    protected abstract BaseListAdapter createAdapter(Context context);

    protected abstract String getActionBarTitle();

    protected abstract void onItemClick(View view, int position, float x, float y);

    protected abstract boolean onItemLongClick(View view, int position, float x, float y);

    protected abstract String getKey();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected CharSequence getSpannedString(String key, int id, String url) {
        var text = LocaleController.getString(key, id);
        var builder = new SpannableStringBuilder(text);
        int index1 = text.indexOf("**");
        int index2 = text.lastIndexOf("**");
        if (index1 >= 0 && index2 >= 0 && index1 != index2) {
            builder.replace(index2, index2 + 2, "");
            builder.replace(index1, index1 + 2, "");
            builder.setSpan(new URLSpanNoUnderline(url), index1, index2 - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    @Override
    public View createView(Context context) {
        fragmentView = new BlurContentView(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        SizeNotifierFrameLayout frameLayout = (SizeNotifierFrameLayout) fragmentView;

        actionBar.setDrawBlurBackground(frameLayout);

        listView = new BlurredRecyclerView(context);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                frameLayout.invalidateBlur();
            }
        });
        listView.additionalClipBottom = AndroidUtilities.dp(200);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        //noinspection ConstantConditions
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listAdapter = createAdapter(context);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this::onItemClick);
        listView.setOnItemLongClickListener((view, position, x, y) -> {
            if (onItemLongClick(view, position, x, y)) {
                return true;
            }
            var holder = listView.findViewHolderForAdapterPosition(position);
            var key = getKey();
            if (key != null && holder != null && listAdapter.isEnabled(holder) && rowMapReverse.containsKey(position)) {
                showDialog(new AlertDialog.Builder(context).setItems(new CharSequence[]{LocaleController.getString("CopyLink", R.string.CopyLink)}, (dialogInterface, i) -> {
                    AndroidUtilities.addToClipboard(
                        String.format(Locale.getDefault(), "https://%s/nullsetting/%s?r=%s", getMessagesController().linkPrefix, getKey(), rowMapReverse.get(position)));
                    BulletinFactory.of(BaseActivity.this).createCopyLinkBulletin().show();
                }).create());
                return true;
            }
            return false;
        });
        return fragmentView;
    }

    @Override
    public void setParentLayout(INavigationLayout layout) {
        if (layout != null && layout.getLastFragment() != null && !hasWhiteActionBar()) {
            resourcesProvider = layout.getLastFragment().getResourceProvider();
        }
        super.setParentLayout(layout);
    }

    public void scrollToRow(String key, Runnable unknown) {
        if (rowMap.containsKey(key)) {
            listView.highlightRow(() -> {
                int position = rowMap.get(key);
                layoutManager.scrollToPositionWithOffset(position, AndroidUtilities.dp(60));
                return position;
            });
        } else {
            unknown.run();
        }
    }

    protected boolean hasWhiteActionBar() {
        return true;
    }


    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar;
        if (!hasWhiteActionBar()) {
            actionBar = super.createActionBar(context);
        } else {
            actionBar = new ActionBar(context);
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
            actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
            actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
            actionBar.setCastShadows(false);
        }
        actionBar.setTitle(getActionBarTitle());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        return actionBar;
    }

    protected int addRow() {
        return rowCount++;
    }

    protected int addRow(String... keys) {
        var row = rowCount++;
        for (var key : keys) {
            rowMap.put(key, row);
        }
        rowMapReverse.put(row, keys[0]);
        return row;
    }

    protected void updateRows() {
        rowCount = 0;
        rowMap.clear();
    }


    protected abstract class BaseListAdapter extends RecyclerListView.SelectionAdapter {

        protected final Context mContext;

        public BaseListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean payload) {
            throw new UnsupportedOperationException("Not implemented!");
        }

        public abstract int getItemViewType(int position);

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == TYPE_SETTINGS || type == TYPE_CHECK || type == TYPE_NOTIFICATION_CHECK || type == TYPE_DETAIL_SETTINGS || type == TYPE_TEXT | type == TYPE_CHECKBOX
                || type == TYPE_RADIO || type == TYPE_ACCOUNT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            var payload = holder.getPayload();
            onBindViewHolder(holder, position, PARTIAL.equals(payload));
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case TYPE_SHADOW:
                    view = new ShadowSectionCell(mContext, resourcesProvider);
                    break;
                case TYPE_SETTINGS:
                    view = new TextSettingsCell(mContext, resourcesProvider);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_CHECK:
                    view = new TextCheckCell(mContext, resourcesProvider);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_HEADER:
                    view = new HeaderCell(mContext, resourcesProvider);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_NOTIFICATION_CHECK:
                    view = new NotificationsCheckCell(mContext, resourcesProvider);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_DETAIL_SETTINGS:
                    view = new TextDetailSettingsCell(mContext, resourcesProvider);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_INFO_PRIVACY:
                    view = new TextInfoPrivacyCell(mContext, resourcesProvider);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case TYPE_TEXT:
                    view = new TextCell(mContext, resourcesProvider);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_CHECKBOX:
                    view = new TextCheckbox2Cell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_RADIO:
                    view = new TextRadioCell(mContext, resourcesProvider);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_ACCOUNT:
                    view = new PasscodeSettingActivity.AccountCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }

    class BlurContentView extends SizeNotifierFrameLayout {

        public BlurContentView(Context context) {
            super(context);
            needBlur = hasWhiteActionBar();
            blurBehindViews.add(this);
        }

        @Override
        protected void drawList(Canvas blurCanvas, boolean top) {
            for (int j = 0; j < listView.getChildCount(); j++) {
                View child = listView.getChildAt(j);
                if (child.getY() < listView.blurTopPadding + AndroidUtilities.dp(100)) {
                    int restore = blurCanvas.save();
                    blurCanvas.translate(getX() + child.getX(), getY() + listView.getY() + child.getY());
                    child.draw(blurCanvas);
                    blurCanvas.restoreToCount(restore);
                }
            }
        }

        public Paint blurScrimPaint = new Paint();
        Rect rectTmp = new Rect();

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (hasWhiteActionBar() && listView.canScrollVertically(-1)) {
                rectTmp.set(0, 0, getMeasuredWidth(), 1);
                blurScrimPaint.setColor(Theme.getColor(Theme.key_divider));
                drawBlurRect(canvas, getY(), rectTmp, blurScrimPaint, true);
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextDetailSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        return themeDescriptions;
    }


}
