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

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.Locale;

import top.qwq2333.nullgram.utils.MessageUtils;

public class DatacenterActivity extends BaseActivity implements NotificationCenter.NotificationCenterDelegate {
    private final int dcToHighlight;

    private int headerRow;
    private int header2Row;

    private int datacentersRow;
    private int datacenters2Row;

    public DatacenterActivity(int dcToHighlight) {
        this.dcToHighlight = dcToHighlight;
    }

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
        listView.setOnItemLongClickListener((view, position, x, y) -> false);

        if (dcToHighlight != 0) {
            listView.highlightRow(() -> {
                layoutManager.scrollToPositionWithOffset(datacentersRow + dcToHighlight, AndroidUtilities.dp(60));
                return datacentersRow + dcToHighlight;
            });
        }
        return fragmentView;
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        updateRows();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxyCheckDone);
    }

    protected void onItemClick(View view, int position, float x, float y) {
        if (position > datacentersRow && position < datacenters2Row) {
            var datacenterInfo = MessageUtils.datacenterInfos.get(position - datacentersRow - 1);
            if (datacenterInfo.checking) {
                return;
            }
            checkDatacenter(datacenterInfo, true);
        }
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        // ignore
        return false;
    }

    @Override
    protected String getKey() {
        return "datacenter";
    }

    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    protected void updateRows() {
        rowCount = 0;
        rowMap.clear();

        headerRow = rowCount++;
        header2Row = rowCount++;

        datacentersRow = rowCount++;
        rowCount += 5;
        datacenters2Row = rowCount++;

        for (var datacenterInfo : MessageUtils.datacenterInfos) {
            checkDatacenter(datacenterInfo, false);
        }
    }

    private void checkDatacenter(MessageUtils.DatacenterInfo datacenterInfo, boolean force) {
        if (datacenterInfo.checking) {
            return;
        }
        if (!force && SystemClock.elapsedRealtime() - datacenterInfo.availableCheckTime < 2 * 60 * 1000) {
            return;
        }
        datacenterInfo.checking = true;
        int position = datacentersRow + MessageUtils.datacenterInfos.indexOf(datacenterInfo) + 1;
        if (force) {
            listAdapter.notifyItemChanged(position);
        }
        datacenterInfo.pingId = ConnectionsManager.getInstance(currentAccount).checkProxy("ping-test", datacenterInfo.id, null, null, null, time -> AndroidUtilities.runOnUIThread(() -> {
            datacenterInfo.availableCheckTime = SystemClock.elapsedRealtime();
            datacenterInfo.checking = false;
            if (time == -1) {
                datacenterInfo.available = false;
                datacenterInfo.ping = 0;
            } else {
                datacenterInfo.ping = time;
                datacenterInfo.available = true;
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, null, datacenterInfo.id);
        }));
    }

    protected String getActionBarTitle() {
        return LocaleController.getString("DatacenterStatus", R.string.DatacenterStatus);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.proxyCheckDone) {
            if (listAdapter != null && args.length > 1) {
                listAdapter.notifyItemChanged(datacentersRow + ((int) args[1]));
            }
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    if (position == datacenters2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 4: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    cell.setText(LocaleController.getString("DatacenterStatus", R.string.DatacenterStatus));
                    break;
                }
                case Integer.MAX_VALUE: {
                    DatacenterCell cell = (DatacenterCell) holder.itemView;
                    cell.setDC(MessageUtils.datacenterInfos.get(position - datacentersRow - 1), position + 1 != datacenters2Row);
                    break;
                }
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == Integer.MAX_VALUE - 1) {
                var headerCell = new DatacenterHeaderCell(mContext);
                headerCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                headerCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(headerCell);
            } else if (viewType == Integer.MAX_VALUE) {
                var dcCell = new DatacenterCell(mContext);
                dcCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                dcCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(dcCell);
            } else {
                return super.onCreateViewHolder(parent, viewType);
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            var position = holder.getAdapterPosition();
            if (position > datacentersRow && position < datacenters2Row) {
                var datacenterInfo = MessageUtils.datacenterInfos.get(position - datacentersRow - 1);
                return !datacenterInfo.checking;
            } else {
                return super.isEnabled(holder);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == header2Row || position == datacenters2Row) {
                return 1;
            } else if (position == datacentersRow) {
                return 4;
            } else if (position == headerRow) {
                return Integer.MAX_VALUE - 1;
            }
            return Integer.MAX_VALUE;
        }
    }

    public ActionBar createActionBar(Context context) {
        ActionBar actionBar;
        actionBar = super.createActionBar(context);
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


    private class DatacenterHeaderCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

        BackupImageView imageView;
        TextView textView;

        public DatacenterHeaderCell(@NonNull Context context) {
            super(context);
            imageView = new BackupImageView(context);
            addView(imageView, LayoutHelper.createFrame(120, 120, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 0));

            imageView.setOnClickListener(view -> {
                if (imageView.getImageReceiver().getLottieAnimation() != null && !imageView.getImageReceiver().getLottieAnimation().isRunning()) {
                    imageView.getImageReceiver().getLottieAnimation().setCurrentFrame(0, false);
                    imageView.getImageReceiver().getLottieAnimation().restart();
                }
            });
            imageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

            textView = new TextView(context);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 36, 152, 36, 0));
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
            textView.setHighlightColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkSelection));
            textView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
            textView.setText(getSpannedString("DatacenterStatusAbout", R.string.DatacenterStatusAbout, "https://core.telegram.org/api/datacenter"));

            setSticker();
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
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(196), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            setSticker();
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.diceStickersDidLoad);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.diceStickersDidLoad);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.diceStickersDidLoad) {
                String name = (String) args[0];
                if (AndroidUtilities.STICKERS_PLACEHOLDER_PACK_NAME.equals(name)) {
                    setSticker();
                }
            }
        }

        private void setSticker() {
            TLRPC.Document document = null;
            TLRPC.TL_messages_stickerSet set;

            set = MediaDataController.getInstance(currentAccount).getStickerSetByName(AndroidUtilities.STICKERS_PLACEHOLDER_PACK_NAME);
            if (set == null) {
                set = MediaDataController.getInstance(currentAccount).getStickerSetByEmojiOrName(AndroidUtilities.STICKERS_PLACEHOLDER_PACK_NAME);
            }
            if (set != null && set.documents.size() >= 3) {
                document = set.documents.get(2);
            }

            SvgHelper.SvgDrawable svgThumb = null;
            if (document != null) {
                svgThumb = DocumentObject.getSvgThumb(document.thumbs, Theme.key_emptyListPlaceholder, 0.2f);
            }
            if (svgThumb != null) {
                svgThumb.overrideWidthAndHeight(512, 512);
            }

            if (document != null) {
                ImageLocation imageLocation = ImageLocation.getForDocument(document);
                imageView.setImage(imageLocation, "130_130", "tgs", svgThumb, set);
                imageView.getImageReceiver().setAutoRepeat(2);
            } else {
                MediaDataController.getInstance(currentAccount).loadStickersByEmojiOrName(AndroidUtilities.STICKERS_PLACEHOLDER_PACK_NAME, false, set == null);
            }
        }
    }

    public static class DatacenterCell extends FrameLayout {

        private final TextView textView;
        private final TextView valueTextView;
        private MessageUtils.DatacenterInfo currentInfo;

        private boolean needDivider;

        public DatacenterCell(Context context) {
            super(context);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, (LocaleController.isRTL ? 56 : 21), 10, (LocaleController.isRTL ? 21 : 56), 0));

            valueTextView = new TextView(context);
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            valueTextView.setLines(1);
            valueTextView.setMaxLines(1);
            valueTextView.setSingleLine(true);
            valueTextView.setCompoundDrawablePadding(AndroidUtilities.dp(6));
            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
            valueTextView.setPadding(0, 0, 0, 0);
            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, (LocaleController.isRTL ? 56 : 21), 35, (LocaleController.isRTL ? 21 : 56), 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        }

        public void setDC(MessageUtils.DatacenterInfo info, boolean divider) {
            textView.setText(String.format(Locale.US, "DC%d %s, %s", info.id, MessageUtils.getDCName(info.id), MessageUtils.getDCLocation(info.id)));
            currentInfo = info;
            needDivider = divider;
            setWillNotDraw(!needDivider);
            updateStatus();
        }

        public void updateStatus() {
            int colorKey;
            if (currentInfo.checking) {
                valueTextView.setText(LocaleController.getString("Checking", R.string.Checking));
                colorKey = Theme.key_windowBackgroundWhiteGrayText2;
            } else if (currentInfo.available) {
                if (currentInfo.ping >= 1000) {
                    valueTextView.setText(String.format("%s, %s", LocaleController.getString("SpeedSlow", R.string.SpeedSlow), LocaleController.formatString("Ping", R.string.Ping, currentInfo.ping)));
                    colorKey = Theme.key_text_RedRegular;
                } else if (currentInfo.ping != 0) {
                    valueTextView.setText(String.format("%s, %s", LocaleController.getString("Available", R.string.Available), LocaleController.formatString("Ping", R.string.Ping, currentInfo.ping)));
                    colorKey = Theme.key_windowBackgroundWhiteGreenText;
                } else {
                    valueTextView.setText(LocaleController.getString("Available", R.string.Available));
                    colorKey = Theme.key_windowBackgroundWhiteGreenText;
                }
            } else {
                valueTextView.setText(LocaleController.getString("Unavailable", R.string.Unavailable));
                colorKey = Theme.key_text_RedRegular;
            }
            valueTextView.setTag(colorKey);
            valueTextView.setTextColor(Theme.getColor(colorKey));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider) {
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }


}
