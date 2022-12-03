package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScrollerCustom;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.FixedHeightEmptyCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CloseProgressDrawable2;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.DrawingInBackgroundThreadDrawable;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.EmojiPacksAlert;
import org.telegram.ui.Components.EmojiTabsStrip;
import org.telegram.ui.Components.EmojiView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumButtonView;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.Premium.PremiumLockIconView;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.Reactions.ReactionsUtils;
import org.telegram.ui.Components.RecyclerAnimationScrollHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class SelectAnimatedEmojiDialog extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    public final static int TYPE_EMOJI_STATUS = 0;
    public final static int TYPE_REACTIONS = 1;
    public final static int TYPE_SET_DEFAULT_REACTION = 2;
    public static final int TYPE_TOPIC_ICON = 3;

    private final int RECENT_MAX_LINES = 5;
    private final int EXPAND_MAX_LINES = 3;

    private int searchRow;
    private int recentReactionsStartRow;
    private int recentReactionsEndRow;
    private int topReactionsStartRow;
    private int topReactionsEndRow;
    private int recentReactionsSectionRow;
    private int popularSectionRow;
    private int longtapHintRow;
    private int defaultTopicIconRow;
    private int topicEmojiHeaderRow;
    
    private EmojiPackExpand recentExpandButton;

    public onLongPressedListener bigReactionListener;
    public SelectAnimatedEmojiDialog.onRecentClearedListener onRecentClearedListener;
    private boolean isAttached;
    HashSet<ReactionsLayoutInBubble.VisibleReaction> selectedReactions = new HashSet<>();
    HashSet<Long> selectedDocumentIds = new HashSet();
    public Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public Paint selectorAccentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Drawable forumIconDrawable;
    private ImageViewEmoji forumIconImage;
    private boolean animationsEnabled;

    public void putAnimatedEmojiToCache(AnimatedEmojiDrawable animatedEmojiDrawable) {
        emojiGridView.animatedEmojiDrawables.put(animatedEmojiDrawable.getDocumentId(), animatedEmojiDrawable);
    }

    public void setSelectedReactions(HashSet<ReactionsLayoutInBubble.VisibleReaction> selectedReactions) {
        this.selectedReactions = selectedReactions;
        selectedDocumentIds.clear();
        ArrayList<ReactionsLayoutInBubble.VisibleReaction> arrayList = new ArrayList<>(selectedReactions);
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).documentId != 0) {
                selectedDocumentIds.add(arrayList.get(i).documentId);
            }
        }
    }

    public static class SelectAnimatedEmojiDialogWindow extends PopupWindow {
        private static final Field superListenerField;
        private ViewTreeObserver.OnScrollChangedListener mSuperScrollListener;
        private ViewTreeObserver mViewTreeObserver;
        private static final ViewTreeObserver.OnScrollChangedListener NOP = () -> {
            /* do nothing */
        };

        static {
            Field f = null;
            try {
                f = PopupWindow.class.getDeclaredField("mOnScrollChangedListener");
                f.setAccessible(true);
            } catch (NoSuchFieldException e) {
                /* ignored */
            }
            superListenerField = f;
        }

        public SelectAnimatedEmojiDialogWindow(View anchor) {
            super(anchor);
            init();
        }

        public SelectAnimatedEmojiDialogWindow(View anchor, int width, int height) {
            super(anchor, width, height);
            init();
        }

        private void init() {
            setFocusable(true);
            setAnimationStyle(0);
            setOutsideTouchable(true);
            setClippingEnabled(true);
            setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_FROM_FOCUSABLE);
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            if (superListenerField != null) {
                try {
                    mSuperScrollListener = (ViewTreeObserver.OnScrollChangedListener) superListenerField.get(this);
                    superListenerField.set(this, NOP);
                } catch (Exception e) {
                    mSuperScrollListener = null;
                }
            }
        }

        private void unregisterListener() {
            if (mSuperScrollListener != null && mViewTreeObserver != null) {
                if (mViewTreeObserver.isAlive()) {
                    mViewTreeObserver.removeOnScrollChangedListener(mSuperScrollListener);
                }
                mViewTreeObserver = null;
            }
        }

        private void registerListener(View anchor) {
            if (getContentView() instanceof SelectAnimatedEmojiDialog) {
                ((SelectAnimatedEmojiDialog) getContentView()).onShow(this::dismiss);
            }
            if (mSuperScrollListener != null) {
                ViewTreeObserver vto = (anchor.getWindowToken() != null) ? anchor.getViewTreeObserver() : null;
                if (vto != mViewTreeObserver) {
                    if (mViewTreeObserver != null && mViewTreeObserver.isAlive()) {
                        mViewTreeObserver.removeOnScrollChangedListener(mSuperScrollListener);
                    }
                    if ((mViewTreeObserver = vto) != null) {
                        vto.addOnScrollChangedListener(mSuperScrollListener);
                    }
                }
            }
        }

        public void dimBehind() {
            View container = getContentView().getRootView();
            Context context = getContentView().getContext();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
            p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            p.dimAmount = 0.2f;
            wm.updateViewLayout(container, p);
        }

        private void dismissDim() {
            View container = getContentView().getRootView();
            Context context = getContentView().getContext();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            if (container.getLayoutParams() == null || !(container.getLayoutParams() instanceof WindowManager.LayoutParams)) {
                return;
            }
            WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
            try {
                if ((p.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0) {
                    p.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    p.dimAmount = 0.0f;
                    wm.updateViewLayout(container, p);
                }
            } catch (Exception ignore) {
            }
        }

        @Override
        public void showAsDropDown(View anchor) {
            super.showAsDropDown(anchor);
            registerListener(anchor);
        }

        @Override
        public void showAsDropDown(View anchor, int xoff, int yoff) {
            super.showAsDropDown(anchor, xoff, yoff);
            registerListener(anchor);
        }

        @Override
        public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
            super.showAsDropDown(anchor, xoff, yoff, gravity);
            registerListener(anchor);
        }

        @Override
        public void showAtLocation(View parent, int gravity, int x, int y) {
            super.showAtLocation(parent, gravity, x, y);
            unregisterListener();
        }

        @Override
        public void dismiss() {
            if (getContentView() instanceof SelectAnimatedEmojiDialog) {
                ((SelectAnimatedEmojiDialog) getContentView()).onDismiss(super::dismiss);
                dismissDim();
            } else {
                super.dismiss();
            }
        }
    }

    private int currentAccount = UserConfig.selectedAccount;
    private int type;

    private FrameLayout contentView;
    private View backgroundView;
    private EmojiTabsStrip emojiTabs;
    private View emojiTabsShadow;
    private SearchBox searchBox;
    public FrameLayout gridViewContainer;
    public EmojiListView emojiGridView;
    public EmojiListView emojiSearchGridView;
    public FrameLayout emojiSearchEmptyView;
    private BackupImageView emojiSearchEmptyViewImageView;
    private View bubble1View;
    private View bubble2View;
    private View topGradientView;
    private View bottomGradientView;
    private Adapter adapter;
    private SearchAdapter searchAdapter;
    private GridLayoutManager layoutManager;
    private GridLayoutManager searchLayoutManager;
    private RecyclerAnimationScrollHelper scrollHelper;
    private View contentViewForeground;

    private int totalCount;
    private ArrayList<Integer> rowHashCodes = new ArrayList<>();
    private SparseIntArray positionToSection = new SparseIntArray();
    private SparseIntArray sectionToPosition = new SparseIntArray();
    private SparseIntArray positionToExpand = new SparseIntArray();
    private SparseIntArray positionToButton = new SparseIntArray();
    private ArrayList<Long> expandedEmojiSets = new ArrayList<>();
    private ArrayList<Long> installedEmojiSets = new ArrayList<>();
    private boolean recentExpanded = false;
    private ArrayList<AnimatedEmojiSpan> recent = new ArrayList<>();
    private ArrayList<ReactionsLayoutInBubble.VisibleReaction> topReactions = new ArrayList<>();
    private ArrayList<ReactionsLayoutInBubble.VisibleReaction> recentReactions = new ArrayList<>();
    private ArrayList<AnimatedEmojiSpan> defaultStatuses = new ArrayList<>();
    private ArrayList<TLRPC.TL_messages_stickerSet> frozenEmojiPacks = new ArrayList<>();
    private ArrayList<EmojiView.EmojiPack> packs = new ArrayList<>();
    private boolean includeEmpty = false;
    private boolean includeHint = false;
    private Integer hintExpireDate;
    private boolean drawBackground = true;
    private List<ReactionsLayoutInBubble.VisibleReaction> recentReactionsToSet;
    ImageViewEmoji selectedReactionView;
    public boolean cancelPressed;
    float pressedProgress;
    ImageReceiver bigReactionImageReceiver = new ImageReceiver();
    AnimatedEmojiDrawable bigReactionAnimatedEmoji;
    private SelectStatusDurationDialog selectStatusDateDialog;

    private Integer emojiX;
    private Theme.ResourcesProvider resourcesProvider;

    private float scaleX, scaleY;
    private BaseFragment baseFragment;

    private int topMarginDp;
    DefaultItemAnimator emojiItemAnimator;

    public SelectAnimatedEmojiDialog(BaseFragment baseFragment, Context context, boolean includeEmpty, Theme.ResourcesProvider resourcesProvider) {
        this(baseFragment, context, includeEmpty, null, TYPE_EMOJI_STATUS, resourcesProvider);
    }

    public SelectAnimatedEmojiDialog(BaseFragment baseFragment, Context context, boolean includeEmpty, Integer emojiX, int type, Theme.ResourcesProvider resourcesProvider) {
        this(baseFragment, context, includeEmpty, emojiX, type, resourcesProvider, 16);
    }

    public SelectAnimatedEmojiDialog(BaseFragment baseFragment, Context context, boolean includeEmpty, Integer emojiX, int type, Theme.ResourcesProvider resourcesProvider, int topPaddingDp) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.type = type;
        this.includeEmpty = includeEmpty;
        this.baseFragment = baseFragment;
        this.includeHint = MessagesController.getGlobalMainSettings().getInt("emoji"+(type==TYPE_EMOJI_STATUS?"status":"reaction")+"usehint", 0) < 3;

        selectorPaint.setColor(Theme.getColor(Theme.key_listSelector, resourcesProvider));
        selectorAccentPaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider), 30));
        premiumStarColorFilter = new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider), PorterDuff.Mode.MULTIPLY);

        this.emojiX = emojiX;
        final Integer bubbleX = emojiX == null ? null : MathUtils.clamp(emojiX, AndroidUtilities.dp(26), AndroidUtilities.dp(340 - 48));
        boolean bubbleRight = bubbleX != null && bubbleX > AndroidUtilities.dp(170);


        setFocusableInTouchMode(true);
        if (type == TYPE_EMOJI_STATUS || type == TYPE_SET_DEFAULT_REACTION) {
            topMarginDp = topPaddingDp;
            setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            setOnTouchListener((v, e) -> {
                if (e.getAction() == MotionEvent.ACTION_DOWN && dismiss != null) {
                    dismiss.run();
                    return true;
                }
                return false;
            });
        }
        if (bubbleX != null) {
            bubble1View = new View(context);
            Drawable bubble1Drawable = getResources().getDrawable(R.drawable.shadowed_bubble1).mutate();
            bubble1Drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider), PorterDuff.Mode.MULTIPLY));
            bubble1View.setBackground(bubble1Drawable);
            addView(bubble1View, LayoutHelper.createFrame(10, 10, Gravity.TOP | Gravity.LEFT, bubbleX / AndroidUtilities.density + (bubbleRight ? -12 : 4), topMarginDp, 0, 0));
        }

//        backgroundView = new View(context) {
//
//            @Override
//            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//                setPivotY(getPaddingTop());
//                if (bubbleX != null) {
//                    setPivotX(bubbleX);
//                }
//            }
//        };
//        backgroundView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
//        addView(backgroundView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 12, 0, 0));

        contentView = new FrameLayout(context) {
            private Path path = new Path();
            private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (!drawBackground) {
                    super.dispatchDraw(canvas);
                    return;
                }
                canvas.save();
                paint.setShadowLayer(dp(2), 0, dp(-0.66f), 0x1e000000);
                paint.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));
                paint.setAlpha((int) (255 * getAlpha()));
                float px = (bubbleX == null ? getWidth() / 2f : bubbleX) + AndroidUtilities.dp(20);
                float w = getWidth() - getPaddingLeft() - getPaddingRight();
                float h = getHeight() - getPaddingBottom() - getPaddingTop();
                AndroidUtilities.rectTmp.set(
                        getPaddingLeft() + (px - px * scaleX),
                        getPaddingTop(),
                        getPaddingLeft() + px + (w - px) * scaleX,
                        getPaddingTop() + h * scaleY
                );
                path.rewind();
                path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(12), AndroidUtilities.dp(12), Path.Direction.CW);
                canvas.drawPath(path, paint);
//                if (showAnimator != null && showAnimator.isRunning()) {
                canvas.clipPath(path);
//                }
                super.dispatchDraw(canvas);
                canvas.restore();
            }
        };
        if (type == TYPE_EMOJI_STATUS || type == TYPE_SET_DEFAULT_REACTION) {
            contentView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        }
        addView(contentView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, type == TYPE_EMOJI_STATUS || type == TYPE_SET_DEFAULT_REACTION ? 6 + topMarginDp : 0, 0, 0));

        if (bubbleX != null) {
            bubble2View = new View(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    setPivotX(getMeasuredWidth() / 2);
                    setPivotY(getMeasuredHeight());
                }
            };
            Drawable bubble2Drawable = getResources().getDrawable(R.drawable.shadowed_bubble2_half);
            bubble2Drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider), PorterDuff.Mode.MULTIPLY));
            bubble2View.setBackground(bubble2Drawable);
            addView(bubble2View, LayoutHelper.createFrame(17, 9, Gravity.TOP | Gravity.LEFT, bubbleX / AndroidUtilities.density + (bubbleRight ? -25 : 10), 6 + 8 - 9 + topMarginDp, 0, 0));
        }

        boolean showSettings = baseFragment != null && type != TYPE_TOPIC_ICON;
        emojiTabs = new EmojiTabsStrip(context, null, false, true, type, showSettings ? () -> {
            onSettings();
            baseFragment.presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, frozenEmojiPacks));
            if (dismiss != null) {
                dismiss.run();
            }
        } : null) {
            @Override
            protected boolean onTabClick(int index) {
                if (smoothScrolling) {
                    return false;
                }
                int position = searchRow == -1 ? 1 : 0;
                if (index > 0 && sectionToPosition.indexOfKey(index - 1) >= 0) {
                    position = sectionToPosition.get(index - 1);
                }
                scrollToPosition(position, AndroidUtilities.dp(-2));
                emojiTabs.select(index);
                emojiGridView.scrolledByUserOnce = true;
                return true;
            }

            @Override
            protected void onTabCreate(EmojiTabsStrip.EmojiTabButton button) {
                if (showAnimator == null || showAnimator.isRunning()) {
                    button.setScaleX(0);
                    button.setScaleY(0);
                }
            }
        };
        emojiTabs.recentTab.setOnLongClickListener(e -> {
            onRecentLongClick();
            try {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
            } catch (Exception ignore) {}
            return true;
        });
        emojiTabs.updateButtonDrawables = false;
        emojiTabs.setAnimatedEmojiCacheType(type == TYPE_EMOJI_STATUS || type == TYPE_SET_DEFAULT_REACTION ? AnimatedEmojiDrawable.CACHE_TYPE_TAB_STRIP : AnimatedEmojiDrawable.CACHE_TYPE_ALERT_PREVIEW_TAB_STRIP);
        emojiTabs.animateAppear = bubbleX == null;
        emojiTabs.setPaddingLeft(5);
        contentView.addView(emojiTabs, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36));

        emojiTabsShadow = new View(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                if (bubbleX != null) {
                    setPivotX(bubbleX);
                }
            }
        };
        emojiTabsShadow.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));
        contentView.addView(emojiTabsShadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1f / AndroidUtilities.density, Gravity.TOP, 0, 36, 0, 0));
        AndroidUtilities.updateViewVisibilityAnimated(emojiTabsShadow, true, 1f, false);
        emojiGridView = new EmojiListView(context) {
            @Override
            public void onScrolled(int dx, int dy) {
                super.onScrolled(dx, dy);
                checkScroll();
                if (!smoothScrolling) {
                    updateTabsPosition(layoutManager.findFirstCompletelyVisibleItemPosition());
                }
                updateSearchBox();
                AndroidUtilities.updateViewVisibilityAnimated(emojiTabsShadow, emojiGridView.computeVerticalScrollOffset() != 0, 1f, true);
            }

            @Override
            public void onScrollStateChanged(int state) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    smoothScrolling = false;
                    if (searchRow != -1 && searchBox.getVisibility() == View.VISIBLE && searchBox.getTranslationY() > -AndroidUtilities.dp(51)) {
                        SelectAnimatedEmojiDialog.this.scrollToPosition(searchBox.getTranslationY() > -AndroidUtilities.dp(16) ? 0 : 1, 0);
                    }
                }
                super.onScrollStateChanged(state);
            }
        };
        emojiItemAnimator = new DefaultItemAnimator() {
            @Override
            protected float animateByScale(View view) {
                return (view instanceof EmojiPackExpand ? .6f : 0f);
            }
        };
        emojiItemAnimator.setAddDuration(220);
        emojiItemAnimator.setMoveDuration(260);
        emojiItemAnimator.setChangeDuration(160);
        emojiItemAnimator.setSupportsChangeAnimations(false);
        emojiItemAnimator.setMoveInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        emojiItemAnimator.setDelayAnimations(false);
        emojiGridView.setItemAnimator(emojiItemAnimator);
        emojiGridView.setPadding(dp(5), dp(2), dp(5), dp(2 + 36));
        emojiGridView.setAdapter(adapter = new Adapter());
        emojiGridView.setLayoutManager(layoutManager = new GridLayoutManager(context, 8) {
            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                try {
                    LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(recyclerView.getContext(), LinearSmoothScrollerCustom.POSITION_TOP) {
                        @Override
                        public void onEnd() {
                            smoothScrolling = false;
                        }
                    };
                    linearSmoothScroller.setTargetPosition(position);
                    startSmoothScroll(linearSmoothScroller);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (positionToSection.indexOfKey(position) >= 0 || positionToButton.indexOfKey(position) >= 0 || position == recentReactionsSectionRow || position == popularSectionRow || position == longtapHintRow || position == searchRow || position == topicEmojiHeaderRow) ? layoutManager.getSpanCount() : 1;
            }
        });

        gridViewContainer = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) + AndroidUtilities.dp(36), MeasureSpec.EXACTLY));
            }
        };
        gridViewContainer.addView(emojiGridView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 0, 0, 0));

        emojiSearchGridView = new EmojiListView(context) {
            @Override
            public void onScrolled(int dx, int dy) {
                super.onScrolled(dx, dy);
                checkScroll();
            }
        };
        if (emojiSearchGridView.getItemAnimator() != null) {
            emojiSearchGridView.getItemAnimator().setDurations(180);
            emojiSearchGridView.getItemAnimator().setMoveInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        }
        TextView emptyViewText = new TextView(context);
        if (type == TYPE_EMOJI_STATUS) {
            emptyViewText.setText(LocaleController.getString("NoEmojiFound", R.string.NoEmojiFound));
        } else if (type == TYPE_REACTIONS || type == TYPE_SET_DEFAULT_REACTION) {
            emptyViewText.setText(LocaleController.getString("NoReactionsFound", R.string.NoReactionsFound));
        } else {
            emptyViewText.setText(LocaleController.getString("NoIconsFound", R.string.NoIconsFound));
        }

        emptyViewText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        emptyViewText.setTextColor(Theme.getColor(Theme.key_chat_emojiPanelEmptyText, resourcesProvider));
        emojiSearchEmptyViewImageView = new BackupImageView(context);
        emojiSearchEmptyView = new FrameLayout(context);
        emojiSearchEmptyView.addView(emojiSearchEmptyViewImageView, LayoutHelper.createFrame(36, 36, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 16, 0, 0));
        emojiSearchEmptyView.addView(emptyViewText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 16 + 36 + 8, 0, 0));
        emojiSearchEmptyView.setVisibility(View.GONE);
        emojiSearchEmptyView.setAlpha(0);
        gridViewContainer.addView(emojiSearchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));
        emojiSearchGridView.setPadding(dp(5), dp(52 + 2), dp(5), dp(2));
        emojiSearchGridView.setAdapter(searchAdapter = new SearchAdapter());
        emojiSearchGridView.setLayoutManager(searchLayoutManager = new GridLayoutManager(context, 8) {
            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                try {
                    LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(recyclerView.getContext(), LinearSmoothScrollerCustom.POSITION_TOP) {
                        @Override
                        public void onEnd() {
                            smoothScrolling = false;
                        }
                    };
                    linearSmoothScroller.setTargetPosition(position);
                    startSmoothScroll(linearSmoothScroller);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });
//        searchLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//            @Override
//            public int getSpanSize(int position) {
//                return position == 0 ? layoutManager.getSpanCount() : 1;
//            }
//        });

        emojiSearchGridView.setVisibility(View.GONE);
        gridViewContainer.addView(emojiSearchGridView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 0, 0, 0));
        contentView.addView(gridViewContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 36 + (1 / AndroidUtilities.density), 0, 0));

        scrollHelper = new RecyclerAnimationScrollHelper(emojiGridView, layoutManager);
        scrollHelper.setAnimationCallback(new RecyclerAnimationScrollHelper.AnimationCallback() {
            @Override
            public void onPreAnimation() {
                smoothScrolling = true;
            }

            @Override
            public void onEndAnimation() {
                smoothScrolling = false;
            }
        });

        RecyclerListView.OnItemLongClickListenerExtended onItemLongClick = new RecyclerListView.OnItemLongClickListenerExtended() {
            @Override
            public boolean onItemClick(View view, int position, float x, float y) {
                if (view instanceof ImageViewEmoji && type == TYPE_REACTIONS) {
                    incrementHintUse();
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    ImageViewEmoji imageViewEmoji = (ImageViewEmoji) view;
                    if (!imageViewEmoji.isDefaultReaction && !UserConfig.getInstance(currentAccount).isPremium()) {
                        TLRPC.Document document = imageViewEmoji.span.document;
                        if (document == null) {
                            document = AnimatedEmojiDrawable.findDocument(currentAccount, imageViewEmoji.span.documentId);
                        }
                        onEmojiSelected(imageViewEmoji, imageViewEmoji.span.documentId, document, null);
                        return true;
                    }
                    selectedReactionView = imageViewEmoji;
                    pressedProgress = 0f;
                    cancelPressed = false;
                    if (selectedReactionView.isDefaultReaction) {
                        TLRPC.TL_availableReaction reaction = MediaDataController.getInstance(currentAccount).getReactionsMap().get(selectedReactionView.reaction.emojicon);
                        if (reaction != null) {
                            bigReactionImageReceiver.setImage(ImageLocation.getForDocument(reaction.select_animation), ReactionsUtils.SELECT_ANIMATION_FILTER, null, null, null, 0, "tgs", selectedReactionView.reaction, 0);
                        }
                    } else {
                        setBigReactionAnimatedEmoji(new AnimatedEmojiDrawable(AnimatedEmojiDrawable.CACHE_TYPE_ALERT_PREVIEW_LARGE, currentAccount, selectedReactionView.span.documentId));
                    }
                    emojiGridView.invalidate();
                    return true;
                }
                if (view instanceof ImageViewEmoji && ((ImageViewEmoji) view).span != null && type == TYPE_EMOJI_STATUS) {
                    SelectStatusDurationDialog dialog = selectStatusDateDialog = new SelectStatusDurationDialog(context, dismiss, SelectAnimatedEmojiDialog.this, (ImageViewEmoji) view, resourcesProvider) {
                        @Override
                        protected boolean getOutBounds(Rect rect) {
                            if (scrimDrawable != null && emojiX != null) {
                                rect.set(drawableToBounds);
                                return true;
                            }
                            return false;
                        }

                        @Override
                        protected void onEndPartly(Integer date) {
                            incrementHintUse();
                            TLRPC.TL_emojiStatus status = new TLRPC.TL_emojiStatus();
                            status.document_id = ((ImageViewEmoji) view).span.documentId;
                            onEmojiSelected(view, status.document_id, ((ImageViewEmoji) view).span.document, date);
                            MediaDataController.getInstance(currentAccount).pushRecentEmojiStatus(status);
                        }

                        @Override
                        protected void onEnd(Integer date) {
                            if (date != null) {
                                if (SelectAnimatedEmojiDialog.this.dismiss != null) {
                                    SelectAnimatedEmojiDialog.this.dismiss.run();
                                }
                            }
                        }

                        @Override
                        public void dismiss() {
                            super.dismiss();
                            selectStatusDateDialog = null;
                        }
                    };
                    dialog.show();

                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    } catch (Exception ignore) {}
                    return true;
                }
                return false;
            }

            @Override
            public void onLongClickRelease() {
                if (selectedReactionView != null) {
                    cancelPressed = true;
                    ValueAnimator cancelProgressAnimator = ValueAnimator.ofFloat(pressedProgress, 0);
                    cancelProgressAnimator.addUpdateListener(animation -> pressedProgress = (float) animation.getAnimatedValue());
                    cancelProgressAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            selectedReactionView.bigReactionSelectedProgress = 0f;
                            selectedReactionView = null;
                            emojiGridView.invalidate();
                        }
                    });
                    cancelProgressAnimator.setDuration(150);
                    cancelProgressAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                    cancelProgressAnimator.start();
                }
            }
        };
        emojiGridView.setOnItemLongClickListener(onItemLongClick, (long) (ViewConfiguration.getLongPressTimeout() * 0.25f));
        emojiSearchGridView.setOnItemLongClickListener(onItemLongClick, (long) (ViewConfiguration.getLongPressTimeout() * 0.25f));
        RecyclerListView.OnItemClickListener onItemClick = (view, position) -> {
            if (view instanceof ImageViewEmoji) {
                ImageViewEmoji viewEmoji = (ImageViewEmoji) view;
                if (viewEmoji.isDefaultReaction) {
                    incrementHintUse();
                    onReactionClick(viewEmoji, viewEmoji.reaction);
                } else {
                    onEmojiClick(viewEmoji, viewEmoji.span);
                }
                if (type != TYPE_REACTIONS) {
                    try {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    } catch (Exception ignore) {}
                }
            } else if (view instanceof ImageView) {
                onEmojiClick(view, null);
                if (type != TYPE_REACTIONS) {
                    try {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    } catch (Exception ignore) {}
                }
            } else if (view instanceof EmojiPackExpand) {
                EmojiPackExpand button = (EmojiPackExpand) view;
                expand(position, button);
                if (type != TYPE_REACTIONS) {
                    try {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    } catch (Exception ignore) {}
                }
            } else if (view != null) {
                view.callOnClick();
            }
        };
        emojiGridView.setOnItemClickListener(onItemClick);
        emojiSearchGridView.setOnItemClickListener(onItemClick);

        searchBox = new SearchBox(context);
        searchBox.setTranslationY(-AndroidUtilities.dp(4 + 52));
        searchBox.setVisibility(View.INVISIBLE);
        gridViewContainer.addView(searchBox, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.TOP, 0, 0, 0, 0));

        topGradientView = new View(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                if (bubbleX != null) {
                    setPivotX(bubbleX);
                }
            }
        };
        Drawable topGradient = getResources().getDrawable(R.drawable.gradient_top);
        topGradient.setColorFilter(new PorterDuffColorFilter(AndroidUtilities.multiplyAlphaComponent(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider), .8f), PorterDuff.Mode.SRC_IN));
        topGradientView.setBackground(topGradient);
        topGradientView.setAlpha(0);
        contentView.addView(topGradientView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 36 + 1f / AndroidUtilities.density, 0, 0));

        bottomGradientView = new View(context);
        Drawable bottomGradient = getResources().getDrawable(R.drawable.gradient_bottom);
        bottomGradient.setColorFilter(new PorterDuffColorFilter(AndroidUtilities.multiplyAlphaComponent(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider), .8f), PorterDuff.Mode.SRC_IN));
        bottomGradientView.setBackground(bottomGradient);
        bottomGradientView.setAlpha(0);
        contentView.addView(bottomGradientView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));

        contentViewForeground = new View(context);
        contentViewForeground.setAlpha(0);
        contentViewForeground.setBackgroundColor(0xff000000);
        contentView.addView(contentViewForeground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        preload(type, currentAccount);

        bigReactionImageReceiver.setLayerNum(7);

        updateRows(true, false);
    }

    protected void onSettings() {

    }

    public void setExpireDateHint(int date) {
        includeHint = true;
        hintExpireDate = date;
        updateRows(true, false);
    }

    private void setBigReactionAnimatedEmoji(AnimatedEmojiDrawable animatedEmojiDrawable) {
        if (!isAttached) {
            return;
        }
        if (bigReactionAnimatedEmoji == animatedEmojiDrawable) {
            return;
        }
        if (bigReactionAnimatedEmoji != null) {
            bigReactionAnimatedEmoji.removeView(this);
        }
        this.bigReactionAnimatedEmoji = animatedEmojiDrawable;
        if (bigReactionAnimatedEmoji != null) {
            bigReactionAnimatedEmoji.addView(this);
        }
    }

    private void onRecentLongClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), null);
        builder.setTitle(LocaleController.getString("ClearRecentEmojiStatusesTitle", R.string.ClearRecentEmojiStatusesTitle));
        builder.setMessage(LocaleController.getString("ClearRecentEmojiStatusesText", R.string.ClearRecentEmojiStatusesText));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialogInterface, i) -> {
            ConnectionsManager.getInstance(currentAccount).sendRequest(new TLRPC.TL_account_clearRecentEmojiStatuses(), null);
            MediaDataController.getInstance(currentAccount).clearRecentEmojiStatuses();
            updateRows(false, true);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setDimEnabled(false);
        builder.setOnDismissListener(di -> {
            setDim(0, true);
        });
        builder.show();
        setDim(1f, true);
    }

    private ValueAnimator dimAnimator;
    private final float maxDim = .25f;

    private void setDim(float dim, boolean animated) {
        if (dimAnimator != null) {
            dimAnimator.cancel();
            dimAnimator = null;
        }
        if (animated) {
            dimAnimator = ValueAnimator.ofFloat(contentViewForeground.getAlpha(), dim * maxDim);
            dimAnimator.addUpdateListener(anm -> {
                contentViewForeground.setAlpha((float) anm.getAnimatedValue());
                final int bubbleColor = Theme.blendOver(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider), ColorUtils.setAlphaComponent(0xff000000, (int) (255 * (float) anm.getAnimatedValue())));
                if (bubble1View != null) {
                    bubble1View.getBackground().setColorFilter(new PorterDuffColorFilter(bubbleColor, PorterDuff.Mode.MULTIPLY));
                }
                if (bubble2View != null) {
                    bubble2View.getBackground().setColorFilter(new PorterDuffColorFilter(bubbleColor, PorterDuff.Mode.MULTIPLY));
                }
            });
            dimAnimator.setDuration(200);
            dimAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            dimAnimator.start();
        } else {
            contentViewForeground.setAlpha(dim * maxDim);
            final int bubbleColor = Theme.blendOver(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider), ColorUtils.setAlphaComponent(0xff000000, (int) (255 * dim * maxDim)));
            if (bubble1View != null) {
                bubble1View.getBackground().setColorFilter(new PorterDuffColorFilter(bubbleColor, PorterDuff.Mode.MULTIPLY));
            }
            if (bubble2View != null) {
                bubble2View.getBackground().setColorFilter(new PorterDuffColorFilter(bubbleColor, PorterDuff.Mode.MULTIPLY));
            }
        }
    }

    private void updateTabsPosition(int position) {
        if (position != RecyclerView.NO_POSITION) {
            final int recentmaxlen = layoutManager.getSpanCount() * RECENT_MAX_LINES;
            int recentSize = recent.size() > recentmaxlen && !recentExpanded ? recentmaxlen : recent.size() + (includeEmpty ? 1 : 0);
            if (position <= recentSize || position <= recentReactions.size()) {
                emojiTabs.select(0); // recent
            } else {
                final int maxlen = layoutManager.getSpanCount() * EXPAND_MAX_LINES;
                for (int i = 0; i < positionToSection.size(); ++i) {
                    int startPosition = positionToSection.keyAt(i);
                    int index = i - (defaultStatuses.isEmpty() ? 0 : 1);
                    EmojiView.EmojiPack pack = index >= 0 ? packs.get(index) : null;
                    if (pack == null) {
                        continue;
                    }
                    int count = pack.expanded ? pack.documents.size() : Math.min(maxlen, pack.documents.size());
                    if (position > startPosition && position <= startPosition + 1 + count) {
                        emojiTabs.select(1 + i);
                        return;
                    }
                }
            }
        }
    }

    private void updateSearchBox() {
        if (searchBox == null) {
            return;
        }
        if (searched) {
            searchBox.clearAnimation();
            searchBox.setVisibility(View.VISIBLE);
            searchBox.animate().translationY(-AndroidUtilities.dp(4)).start();
        } else {
            if (emojiGridView.getChildCount() > 0) {
                View first = emojiGridView.getChildAt(0);
                if (emojiGridView.getChildAdapterPosition(first) == searchRow && "searchbox".equals(first.getTag())) {
                    searchBox.setVisibility(View.VISIBLE);
                    searchBox.setTranslationY(first.getY() - AndroidUtilities.dp(4));
                } else {
//                    searchBox.setVisibility(View.INVISIBLE);
                    searchBox.setTranslationY(-AndroidUtilities.dp(4 + 52));
                }
            } else {
//                searchBox.setVisibility(View.INVISIBLE);
                searchBox.setTranslationY(-AndroidUtilities.dp(4 + 52));
            }
        }
    }

    private Drawable premiumStar;
    private ColorFilter premiumStarColorFilter;

    private Drawable getPremiumStar() {
        if (premiumStar == null) {
//            premiumStar = PremiumGradient.getInstance().premiumStarMenuDrawableGray;
            premiumStar = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.msg_settings_premium).mutate();
//            premiumStar.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_verifiedBackground, resourcesProvider), PorterDuff.Mode.MULTIPLY));
            premiumStar.setColorFilter(premiumStarColorFilter);
        }
        return premiumStar;
    }

    private float scrimAlpha = 1f;
    private int scrimColor;
    private AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable scrimDrawable;
    private Rect drawableToBounds;
    private View scrimDrawableParent;

    private float emojiSelectAlpha = 1f;
    private ImageViewEmoji emojiSelectView;
    private Rect emojiSelectRect;
    private OvershootInterpolator overshootInterpolator = new OvershootInterpolator(2f);

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (scrimDrawable != null && emojiX != null) {
//            Rect bounds = (scrimDrawableBounds == null ? scrimDrawableBounds = scrimDrawable.getBounds() : scrimDrawableBounds);
            Rect bounds = scrimDrawable.getBounds();
            float scale = scrimDrawableParent == null ? 1f : scrimDrawableParent.getScaleY();
            int wasAlpha = 255;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                wasAlpha = scrimDrawable.getAlpha();
            }
            int h = (scrimDrawableParent == null ? bounds.height() : scrimDrawableParent.getHeight());
            canvas.save();
            canvas.translate(0, -getTranslationY());
            scrimDrawable.setAlpha((int) (wasAlpha * Math.pow(contentView.getAlpha(), .25f) * scrimAlpha));
            if (drawableToBounds == null) {
                drawableToBounds = new Rect();
            }
            drawableToBounds.set(
                (int) (bounds.centerX() - bounds.width() / 2f * scale - bounds.centerX() + emojiX + (scale > 1f && scale < 1.5f ? 2 : 0)),
                (int) ((h - (h - bounds.bottom)) * scale - (scale > 1.5f ? (bounds.height() * .81f + 1) : 0) - bounds.top - bounds.height() / 2f + AndroidUtilities.dp(topMarginDp) - bounds.height() * scale),
                (int) (bounds.centerX() + bounds.width() / 2f * scale - bounds.centerX() + emojiX + (scale > 1f && scale < 1.5f ? 2 : 0)),
                (int) ((h - (h - bounds.bottom)) * scale - (scale > 1.5f ? bounds.height() * .81f + 1 : 0) - bounds.top - bounds.height() / 2f + AndroidUtilities.dp(topMarginDp))
            );
            scrimDrawable.setBounds(
                drawableToBounds.left,
                drawableToBounds.top,
                (int) (drawableToBounds.left + drawableToBounds.width() / scale),
                (int) (drawableToBounds.top + drawableToBounds.height() / scale)
            );
            canvas.scale(scale, scale, drawableToBounds.left, drawableToBounds.top);
            scrimDrawable.draw(canvas);
            scrimDrawable.setAlpha(wasAlpha);
            scrimDrawable.setBounds(bounds);
            canvas.restore();
        }
        super.dispatchDraw(canvas);
        if (emojiSelectView != null && emojiSelectRect != null && drawableToBounds != null && emojiSelectView.drawable != null) {
            canvas.save();
            canvas.translate(0, -getTranslationY());
            emojiSelectView.drawable.setAlpha((int) (255 * emojiSelectAlpha));
            emojiSelectView.drawable.setBounds(emojiSelectRect);
            emojiSelectView.drawable.setColorFilter(new PorterDuffColorFilter(ColorUtils.blendARGB(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider), scrimColor, 1f - scrimAlpha), PorterDuff.Mode.MULTIPLY));
            emojiSelectView.drawable.draw(canvas);
            canvas.restore();
        }
    }

    private ValueAnimator emojiSelectAnimator;
    public void animateEmojiSelect(ImageViewEmoji view, Runnable onDone) {
        if (emojiSelectAnimator != null || scrimDrawable == null) {
            onDone.run();
            return;
        }

        view.notDraw = true;
        final Rect from = new Rect();
        from.set(
            contentView.getLeft() + emojiGridView.getLeft() + view.getLeft(),
            contentView.getTop() + emojiGridView.getTop() + view.getTop(),
            contentView.getLeft() + emojiGridView.getLeft() + view.getRight(),
            contentView.getTop() + emojiGridView.getTop() + view.getBottom()
        );

        final AnimatedEmojiDrawable statusDrawable =
            view.drawable instanceof AnimatedEmojiDrawable ?
                AnimatedEmojiDrawable.make(currentAccount, AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS, ((AnimatedEmojiDrawable) view.drawable).getDocumentId()) :
                null;

        emojiSelectView = view;
        emojiSelectRect = new Rect();
        emojiSelectRect.set(from);

        boolean[] done = new boolean[1];
        emojiSelectAnimator = ValueAnimator.ofFloat(0, 1);
        emojiSelectAnimator.addUpdateListener(anm -> {
            float t = (float) anm.getAnimatedValue();
            scrimAlpha = 1f - t * t * t;
            emojiSelectAlpha = 1f - (float) Math.pow(t, 10);
            AndroidUtilities.lerp(from, drawableToBounds, t, emojiSelectRect);
            float scale = Math.max(1, overshootInterpolator.getInterpolation(MathUtils.clamp(3 * t - (3 - 1), 0, 1f))) * view.getScaleX();
            emojiSelectRect.set(
                (int) (emojiSelectRect.centerX() - emojiSelectRect.width() / 2f * scale),
                (int) (emojiSelectRect.centerY() - emojiSelectRect.height() / 2f * scale),
                (int) (emojiSelectRect.centerX() + emojiSelectRect.width() / 2f * scale),
                (int) (emojiSelectRect.centerY() + emojiSelectRect.height() / 2f * scale)
            );
            invalidate();

            if (t > .85f && !done[0]) {
                done[0] = true;
                onDone.run();
                if (statusDrawable != null && scrimDrawable != null) {
//                    scrimDrawable.set(statusDrawable, false);
                    scrimDrawable.play();
                }
            }
        });
        emojiSelectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                emojiSelectView = null;
                invalidate();
                if (!done[0]) {
                    done[0] = true;
                    onDone.run();
                }
            }
        });
        emojiSelectAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        emojiSelectAnimator.setDuration(260);
        emojiSelectAnimator.start();
    }

    private boolean topGradientShown = false;
    private boolean bottomGradientShown = false;

    private void checkScroll() {
        final boolean bottom = (gridSearch ? emojiSearchGridView : emojiGridView).canScrollVertically(1);
        if (bottom != bottomGradientShown) {
            bottomGradientShown = bottom;
            bottomGradientView.animate().alpha(bottom ? 1f : 0f).setDuration(200).start();
        }
    }

    private boolean smoothScrolling = false;

    private void scrollToPosition(int p, int offset) {
        View view = layoutManager.findViewByPosition(p);
        int firstPosition = layoutManager.findFirstVisibleItemPosition();
        if ((view == null && Math.abs(p - firstPosition) > layoutManager.getSpanCount() * 9f) || !SharedConfig.animationsEnabled()) {
            scrollHelper.setScrollDirection(layoutManager.findFirstVisibleItemPosition() < p ? RecyclerAnimationScrollHelper.SCROLL_DIRECTION_DOWN : RecyclerAnimationScrollHelper.SCROLL_DIRECTION_UP);
            scrollHelper.scrollToPosition(p, offset, false, true);
        } else {
//            ignoreStickersScroll = true;
            LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(emojiGridView.getContext(), LinearSmoothScrollerCustom.POSITION_TOP) {
                @Override
                public void onEnd() {
                    smoothScrolling = false;
                }

                @Override
                protected void onStart() {
                    smoothScrolling = true;
                }
            };
            linearSmoothScroller.setTargetPosition(p);
            linearSmoothScroller.setOffset(offset);
            layoutManager.startSmoothScroll(linearSmoothScroller);
        }
    }

    public boolean searching = false;
    public boolean searched = false;
    private String lastQuery;
    private ArrayList<ReactionsLayoutInBubble.VisibleReaction> searchResult;
    private ValueAnimator gridSwitchAnimator;

    private boolean gridSearch = false;
    public void switchGrids(boolean search) {
        if (gridSearch == search) {
            return;
        }
        gridSearch = search;
        emojiGridView.setVisibility(View.VISIBLE);
        emojiSearchGridView.setVisibility(View.VISIBLE);
        if (gridSwitchAnimator != null) {
            gridSwitchAnimator.cancel();
        }
        if (searchEmptyViewAnimator != null) {
            searchEmptyViewAnimator.cancel();
            searchEmptyViewAnimator = null;
        }
        gridSwitchAnimator = ValueAnimator.ofFloat(0, 1);
        gridSwitchAnimator.addUpdateListener(anm -> {
            float t = (float) anm.getAnimatedValue();
            if (!search) {
                t = 1f - t;
            }
            emojiGridView.setAlpha(1f - t);
            emojiSearchGridView.setAlpha(t);
            emojiSearchEmptyView.setAlpha(emojiSearchGridView.getAlpha() * t);
        });
        gridSwitchAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                emojiSearchGridView.setVisibility(search ? View.VISIBLE : View.GONE);
                emojiGridView.setVisibility(search ? View.GONE : View.VISIBLE);
                gridSwitchAnimator = null;
                if (!search && searchResult != null) {
                    searchResult.clear();
                    searchAdapter.updateRows(false);
                }
            }
        });
        gridSwitchAnimator.setDuration(280);
        gridSwitchAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        gridSwitchAnimator.start();
        ((View) emojiGridView.getParent()).animate()
            .translationY(gridSearch ? -AndroidUtilities.dp(36) : 0)
            .setInterpolator(CubicBezierInterpolator.DEFAULT)
            .setDuration(160)
            .start();
        checkScroll();
    }

    private ArrayList<String> emptyViewEmojis = new ArrayList<String>(4); {
        emptyViewEmojis.add("\uD83D\uDE16");
        emptyViewEmojis.add("\uD83D\uDE2B");
        emptyViewEmojis.add("\uD83E\uDEE0");
        emptyViewEmojis.add("\uD83D\uDE28");
        emptyViewEmojis.add("❓");
    };
    public void updateSearchEmptyViewImage() {
        if (emojiSearchEmptyViewImageView == null) {
            return;
        }

        TLRPC.Document emoji = null;
        ArrayList<TLRPC.StickerSetCovered> featuredSets = MediaDataController.getInstance(currentAccount).getFeaturedEmojiSets();
        List<TLRPC.StickerSetCovered> shuffledFeaturedSets = new ArrayList<>(featuredSets);
        Collections.shuffle(shuffledFeaturedSets);
        for (int i = 0; i < shuffledFeaturedSets.size(); ++i) {
            if (shuffledFeaturedSets.get(i) instanceof TLRPC.TL_stickerSetFullCovered && ((TLRPC.TL_stickerSetFullCovered) shuffledFeaturedSets.get(i)).documents != null) {
                List<TLRPC.Document> documents = new ArrayList<>(((TLRPC.TL_stickerSetFullCovered) shuffledFeaturedSets.get(i)).documents);
                Collections.shuffle(documents);
                for (int j = 0; j < documents.size(); ++j) {
                    TLRPC.Document document = documents.get(j);
                    if (document != null && emptyViewEmojis.contains(MessageObject.findAnimatedEmojiEmoticon(document, null))) {
                        emoji = document;
                        break;
                    }
                }
            }
            if (emoji != null) {
                break;
            }
        }
        if (emoji == null) {
            ArrayList<TLRPC.TL_messages_stickerSet> sets = MediaDataController.getInstance(currentAccount).getStickerSets(MediaDataController.TYPE_EMOJIPACKS);
            List<TLRPC.TL_messages_stickerSet> shuffledSets = new ArrayList<>(sets);
            Collections.shuffle(shuffledSets);
            for (int i = 0; i < shuffledSets.size(); ++i) {
                if (shuffledSets.get(i) != null && shuffledSets.get(i).documents != null) {
                    List<TLRPC.Document> documents = new ArrayList<>(shuffledSets.get(i).documents);
                    Collections.shuffle(documents);
                    for (int j = 0; j < documents.size(); ++j) {
                        TLRPC.Document document = documents.get(j);
                        if (document != null && emptyViewEmojis.contains(MessageObject.findAnimatedEmojiEmoticon(document, null))) {
                            emoji = document;
                            break;
                        }
                    }
                }
                if (emoji != null) {
                    break;
                }
            }
        }

        if (emoji != null) {
            TLRPC.Document document = emoji;
            String filter = "36_36";
            ImageLocation mediaLocation;
            String mediaFilter;
            SvgHelper.SvgDrawable thumbDrawable = DocumentObject.getSvgThumb(document.thumbs, Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            if ("video/webm".equals(document.mime_type)) {
                mediaLocation = ImageLocation.getForDocument(document);
                mediaFilter = filter + "_" + ImageLoader.AUTOPLAY_FILTER;
                if (thumbDrawable != null) {
                    thumbDrawable.overrideWidthAndHeight(512, 512);
                }
            } else {
                if (thumbDrawable != null && MessageObject.isAnimatedStickerDocument(document, false)) {
                    thumbDrawable.overrideWidthAndHeight(512, 512);
                }
                mediaLocation = ImageLocation.getForDocument(document);
                mediaFilter = filter;
            }
            emojiSearchEmptyViewImageView.setLayerNum(7);
            emojiSearchEmptyViewImageView.setRoundRadius(AndroidUtilities.dp(4));
            emojiSearchEmptyViewImageView.setImage(mediaLocation, mediaFilter, ImageLocation.getForDocument(thumb, document), "36_36", thumbDrawable, document);
        }
    }

    private boolean searchEmptyViewVisible = false;
    private ValueAnimator searchEmptyViewAnimator;
    public void switchSearchEmptyView(boolean empty) {
        if (searchEmptyViewVisible == empty) {
            return;
        }

        searchEmptyViewVisible = empty;
        if (searchEmptyViewAnimator != null) {
            searchEmptyViewAnimator.cancel();
        }
        searchEmptyViewAnimator = ValueAnimator.ofFloat(0, 1);
        searchEmptyViewAnimator.addUpdateListener(anm -> {
            float t = (float) anm.getAnimatedValue();
            if (!empty) {
                t = 1f - t;
            }
            emojiSearchEmptyView.setAlpha(emojiSearchGridView.getAlpha() * t);
        });
        searchEmptyViewAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                emojiSearchEmptyView.setVisibility(empty && emojiSearchGridView.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
                searchEmptyViewAnimator = null;
            }
        });
        searchEmptyViewAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        searchEmptyViewAnimator.setDuration(100);
        searchEmptyViewAnimator.start();

        if (empty) {
            updateSearchEmptyViewImage();
        }
    }

    private static String[] lastSearchKeyboardLanguage;
    private Runnable clearSearchRunnable;
    private Runnable searchRunnable;
    public void search(String query) {
        if (clearSearchRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(clearSearchRunnable);
            clearSearchRunnable = null;
        }
        if (searchRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(searchRunnable);
            searchRunnable = null;
        }
        if (query == null) {
            searching = false;
            searched = false;
            switchGrids(false);
            if (searchBox != null && searchBox.clearDrawable != null) {
                searchBox.clearDrawable.stopAnimation();
            }
            searchAdapter.updateRows(true);
            lastQuery = null;
        } else {
            boolean firstSearch = !searching;
            searching = true;
            searched = false;
            if (searchBox != null && searchBox.clearDrawable != null) {
                searchBox.clearDrawable.startAnimation();
            }
            if (firstSearch) {
                if (searchResult != null) {
                    searchResult.clear();
                }
                searchAdapter.updateRows(false);
            } else if (!query.equals(lastQuery)) {
                AndroidUtilities.runOnUIThread(clearSearchRunnable = () -> {
                    if (searchResult != null) {
                        searchResult.clear();
                    }
                    searchAdapter.updateRows(true);
                }, 120);
            }
            lastQuery = query;
            String[] newLanguage = AndroidUtilities.getCurrentKeyboardLanguage();
            if (!Arrays.equals(newLanguage, lastSearchKeyboardLanguage)) {
                MediaDataController.getInstance(currentAccount).fetchNewEmojiKeywords(newLanguage);
            }
            lastSearchKeyboardLanguage = newLanguage;
            AndroidUtilities.runOnUIThread(searchRunnable = () -> {
                MediaDataController.getInstance(currentAccount).getAnimatedEmojiByKeywords(query, _documentIds -> {
                    final ArrayList<Long> documentIds = _documentIds == null ? new ArrayList<>() : _documentIds;
                    final HashMap<String, TLRPC.TL_availableReaction> availableReactions = MediaDataController.getInstance(currentAccount).getReactionsMap();
                    if (Emoji.fullyConsistsOfEmojis(query)) {
                        ArrayList<TLRPC.TL_messages_stickerSet> stickerSets = MediaDataController.getInstance(currentAccount).getStickerSets(MediaDataController.TYPE_EMOJIPACKS);
                        String emoticon;

                        for (int i = 0; i < stickerSets.size(); ++i) {
                            if (stickerSets.get(i).documents != null) {
                                ArrayList<TLRPC.Document> documents = stickerSets.get(i).documents;
                                if (documents != null) {
                                    for (int j = 0; j < documents.size(); ++j) {
                                        emoticon = MessageObject.findAnimatedEmojiEmoticon(documents.get(j), null);
                                        long id = documents.get(j).id;
                                        if (emoticon != null && !documentIds.contains(id) && query.contains(emoticon.toLowerCase())) {
                                            documentIds.add(id);
                                        }
                                    }
                                }
                            }
                        }

                        ArrayList<TLRPC.StickerSetCovered> featuredStickerSets = MediaDataController.getInstance(currentAccount).getFeaturedEmojiSets();
                        for (int i = 0; i < featuredStickerSets.size(); ++i) {
                            if (featuredStickerSets.get(i) instanceof TLRPC.TL_stickerSetFullCovered &&
                                    ((TLRPC.TL_stickerSetFullCovered) featuredStickerSets.get(i)).keywords != null) {
                                ArrayList<TLRPC.Document> documents = ((TLRPC.TL_stickerSetFullCovered) featuredStickerSets.get(i)).documents;
                                if (documents != null) {
                                    for (int j = 0; j < documents.size(); ++j) {
                                        emoticon = MessageObject.findAnimatedEmojiEmoticon(documents.get(j), null);
                                        long id = documents.get(j).id;
                                        if (emoticon != null && !documentIds.contains(id) && query.contains(emoticon)) {
                                            documentIds.add(id);
                                        }
                                    }
                                }
                            }
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            if (clearSearchRunnable != null) {
                                AndroidUtilities.cancelRunOnUIThread(clearSearchRunnable);
                                clearSearchRunnable = null;
                            }

                            if (query != lastQuery) {
                                return;
                            }
                            searched = true;
                            switchGrids(true);
                            if (searchBox != null && searchBox.clearDrawable != null) {
                                searchBox.clearDrawable.stopAnimation();
                            }
                            if (searchResult == null) {
                                searchResult = new ArrayList<>();
                            } else {
                                searchResult.clear();
                            }
                            emojiSearchGridView.scrollToPosition(0);
                            searched = true;
                            if (type == TYPE_REACTIONS || type == TYPE_SET_DEFAULT_REACTION) {
                                TLRPC.TL_availableReaction reaction = availableReactions.get(query);
                                if (reaction != null) {
                                    searchResult.add(ReactionsLayoutInBubble.VisibleReaction.fromEmojicon(reaction));
                                }
                            }
                            for (int i = 0; i < documentIds.size(); ++i) {
                                searchResult.add(ReactionsLayoutInBubble.VisibleReaction.fromCustomEmoji(documentIds.get(i)));
                            }
                            searchAdapter.updateRows(!firstSearch);
                        });
                    } else {
                        MediaDataController.getInstance(currentAccount).getEmojiSuggestions(
                            lastSearchKeyboardLanguage,
                            query,
                            false,
                            (result, alias) -> {
                                if (clearSearchRunnable != null) {
                                    AndroidUtilities.cancelRunOnUIThread(clearSearchRunnable);
                                    clearSearchRunnable = null;
                                }
                                if (query != lastQuery) {
                                    return;
                                }
                                searched = true;
                                switchGrids(true);
                                if (searchBox != null && searchBox.clearDrawable != null) {
                                    searchBox.clearDrawable.stopAnimation();
                                }
                                if (searchResult == null) {
                                    searchResult = new ArrayList<>();
                                } else {
                                    searchResult.clear();
                                }
                                for (int i = 0; i < result.size(); ++i) {
                                    try {
                                        if (result.get(i).emoji.startsWith("animated_")) {
                                            documentIds.add(Long.parseLong(result.get(i).emoji.substring(9)));
                                        } else {
                                            if (type == TYPE_REACTIONS || type == TYPE_SET_DEFAULT_REACTION) {
                                                TLRPC.TL_availableReaction reaction = availableReactions.get(result.get(i).emoji);
                                                if (reaction != null) {
                                                    searchResult.add(ReactionsLayoutInBubble.VisibleReaction.fromEmojicon(reaction));
                                                }
                                            }
                                        }
                                    } catch (Exception ignore) {}
                                }
                                emojiSearchGridView.scrollToPosition(0);
                                searched = true;
                                for (int i = 0; i < documentIds.size(); ++i) {
                                    searchResult.add(ReactionsLayoutInBubble.VisibleReaction.fromCustomEmoji(documentIds.get(i)));
                                }
                                searchAdapter.updateRows(!firstSearch);
                            },
                            null,
                            true,
                            30
                        );
                    }
                });
            }, 425);
        }
        updateSearchBox();

        if (searchBox != null && searchBox.clear != null) {
            boolean showed = searchBox.clear.getAlpha() != 0;
            if (searching != showed) {
                searchBox.clear.animate()
                    .alpha(searching ? 1.0f : 0.0f)
                    .setDuration(150)
                    .scaleX(searching ? 1.0f : 0.1f)
                    .scaleY(searching ? 1.0f : 0.1f)
                    .start();
            }
        }
    }

    private class SearchAdapter extends RecyclerListView.SelectionAdapter {

        public int VIEW_TYPE_SEARCH = 7;
        public int VIEW_TYPE_EMOJI = 3;
        public int VIEW_TYPE_REACTION = 4;

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_TYPE_EMOJI || holder.getItemViewType() == VIEW_TYPE_REACTION;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_SEARCH) {
                view = new View(getContext()) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(52), MeasureSpec.EXACTLY));
                    }
                };
                view.setTag("searchbox");
            } else {
                view = new ImageViewEmoji(getContext());
            }
            if (showAnimator != null && showAnimator.isRunning()) {
                view.setScaleX(0);
                view.setScaleY(0);
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (searchResult == null || position < 0 || position >= searchResult.size() || searchResult.get(position).emojicon == null) {
                return VIEW_TYPE_EMOJI;
            } else {
                return VIEW_TYPE_REACTION;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_REACTION) {
                ImageViewEmoji imageView = (ImageViewEmoji) holder.itemView;
                imageView.position = position;
                if (searchResult == null || position < 0 || position >= searchResult.size()) {
                    return;
                }
                ReactionsLayoutInBubble.VisibleReaction currentReaction = searchResult.get(position);

                if (imageView.imageReceiver == null) {
                    imageView.imageReceiver = new ImageReceiver();
                    imageView.imageReceiver.setLayerNum(7);
                    imageView.imageReceiver.onAttachedToWindow();
                }
                imageView.imageReceiver.setParentView(emojiSearchGridView);
                imageView.reaction = currentReaction;
                imageView.setViewSelected(selectedReactions.contains(currentReaction), false);
                imageView.notDraw = false;
                imageView.invalidate();
                if (currentReaction.emojicon != null) {
                    imageView.isDefaultReaction = true;
                    TLRPC.TL_availableReaction reaction = MediaDataController.getInstance(currentAccount).getReactionsMap().get(currentReaction.emojicon);
                    if (reaction != null) {
                        SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(reaction.activate_animation, Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
                        imageView.imageReceiver.setImage(ImageLocation.getForDocument(reaction.select_animation), ReactionsUtils.SELECT_ANIMATION_FILTER, null, null, svgThumb, 0, "tgs", currentReaction, 0);
                    } else {
                        imageView.imageReceiver.clearImage();
                    }
                    imageView.span = null;
                    imageView.document = null;
                    imageView.setDrawable(null);
                    if (imageView.premiumLockIconView != null) {
                        imageView.premiumLockIconView.setVisibility(View.GONE);
                        imageView.premiumLockIconView.setImageReceiver(null);
                    }
                } else {
                    imageView.isDefaultReaction = false;
                    imageView.span = new AnimatedEmojiSpan(currentReaction.documentId, null);
                    imageView.document = null;
                    imageView.imageReceiver.clearImage();
                    AnimatedEmojiDrawable drawable = emojiSearchGridView.animatedEmojiDrawables.get(imageView.span.getDocumentId());
//
                    if (drawable == null) {
                        drawable = AnimatedEmojiDrawable.make(currentAccount, getCacheType(), imageView.span.getDocumentId());
                        drawable.addView(emojiSearchGridView);
                        emojiSearchGridView.animatedEmojiDrawables.put(imageView.span.getDocumentId(), drawable);
                    }
                    imageView.setDrawable(drawable);

                    if (!UserConfig.getInstance(currentAccount).isPremium()) {
                        if (imageView.premiumLockIconView == null) {
                            imageView.premiumLockIconView = new PremiumLockIconView(getContext(), PremiumLockIconView.TYPE_STICKERS_PREMIUM_LOCKED);
                            imageView.addView(imageView.premiumLockIconView, LayoutHelper.createFrame(12, 12, Gravity.RIGHT | Gravity.BOTTOM));
                        }
                        imageView.premiumLockIconView.setVisibility(View.VISIBLE);
                    }
                }
            } else if (holder.getItemViewType() == VIEW_TYPE_EMOJI) {
                ImageViewEmoji imageView = (ImageViewEmoji) holder.itemView;
                imageView.empty = false;
                imageView.position = position;
                imageView.setPadding(AndroidUtilities.dp(1), AndroidUtilities.dp(1), AndroidUtilities.dp(1), AndroidUtilities.dp(1));
                boolean selected = false;
                imageView.setDrawable(null);
                if (searchResult != null && position >= 0 && position < searchResult.size()) {
                    final long documentId = searchResult.get(position).documentId;
                    imageView.span = new AnimatedEmojiSpan(documentId, null);
                    imageView.document = imageView.span.document;
                    selected = selectedDocumentIds.contains(documentId);

                    AnimatedEmojiDrawable drawable = emojiSearchGridView.animatedEmojiDrawables.get(imageView.span.getDocumentId());
                    if (drawable == null) {
                        drawable = AnimatedEmojiDrawable.make(currentAccount, getCacheType(), imageView.span.getDocumentId());
                        drawable.addView(emojiSearchGridView);
                        emojiSearchGridView.animatedEmojiDrawables.put(imageView.span.getDocumentId(), drawable);
                    }
                    imageView.setDrawable(drawable);
                }
                imageView.setViewSelected(selected, false);
            }
        }

        private int count = 1;
        private ArrayList<Integer> rowHashCodes = new ArrayList<>();

        @Override
        public int getItemCount() {
            return count;
        }

        public void updateRows(boolean diff) {
            if (!isAttached) {
                diff = false;
            }
            ArrayList<Integer> prevRowHashCodes = new ArrayList<>(rowHashCodes);

            count = 0;
            rowHashCodes.clear();

//            count++;
//            rowHashCodes.add(132);

            if (searchResult != null) {
                for (int i = 0; i < searchResult.size(); ++i) {
                    count++;
                    rowHashCodes.add(Objects.hash(-4342, searchResult.get(i)));
                }
            }

            if (diff) {
                DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return prevRowHashCodes.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return rowHashCodes.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return prevRowHashCodes.get(oldItemPosition).equals(rowHashCodes.get(newItemPosition));
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return true;
                    }
                }, false).dispatchUpdatesTo(SearchAdapter.this);
            } else {
                this.notifyDataSetChanged();
            }

            switchSearchEmptyView(searched && count == 0);
        }
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {

        public int VIEW_TYPE_HEADER = 0;
        public int VIEW_TYPE_REACTION = 1;
        public int VIEW_TYPE_IMAGE = 2;
        public int VIEW_TYPE_EMOJI = 3;
        public int VIEW_TYPE_EXPAND = 4;
        public int VIEW_TYPE_BUTTON = 5;
        public int VIEW_TYPE_HINT = 6;
        public int VIEW_TYPE_SEARCH = 7;
        public int VIEW_TYPE_TOPIC_ICON = 8;

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int viewType = holder.getItemViewType();
            return (
                viewType == VIEW_TYPE_IMAGE ||
                viewType == VIEW_TYPE_REACTION ||
                viewType == VIEW_TYPE_EMOJI ||
                viewType == VIEW_TYPE_TOPIC_ICON
            );
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderView(getContext());
            } else if (viewType == VIEW_TYPE_IMAGE) {
                view = new ImageView(getContext());
            } else if (viewType == VIEW_TYPE_EMOJI || viewType == VIEW_TYPE_REACTION || viewType == VIEW_TYPE_TOPIC_ICON) {
                ImageViewEmoji imageView = new ImageViewEmoji(getContext());
                if (viewType == VIEW_TYPE_TOPIC_ICON) {
                    imageView.isStaticIcon = true;
                    imageView.imageReceiverToDraw = imageView.imageReceiver = new ImageReceiver(imageView) {
                        @Override
                        public boolean draw(Canvas canvas) {
                            return super.draw(canvas);
                        }
                    };

                    imageView.imageReceiver.setImageBitmap(forumIconDrawable);
                    forumIconImage = imageView;
                    imageView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
                }
                view = imageView;
            } else if (viewType == VIEW_TYPE_EXPAND) {
                view = new EmojiPackExpand(getContext(), null);
            } else if (viewType == VIEW_TYPE_BUTTON) {
                view = new EmojiPackButton(getContext());
            } else if (viewType == VIEW_TYPE_HINT) {
                TextView textView = new TextView(getContext()) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(AndroidUtilities.dp(26)), MeasureSpec.EXACTLY));
                    }
                };
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                if (type == TYPE_TOPIC_ICON) {
                    textView.setText(LocaleController.getString("SelectTopicIconHint", R.string.SelectTopicIconHint));
                } else if (type == TYPE_EMOJI_STATUS) {
                    textView.setText(LocaleController.getString("EmojiLongtapHint", R.string.EmojiLongtapHint));
                } else {
                    textView.setText(LocaleController.getString("ReactionsLongtapHint", R.string.ReactionsLongtapHint));
                }
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
                view = textView;
            } else if (viewType == VIEW_TYPE_SEARCH) {
                view = new FixedHeightEmptyCell(getContext(), 52);
                view.setTag("searchbox");
            } else {
                view = new ImageViewEmoji(getContext());
            }
            if (showAnimator != null && showAnimator.isRunning()) {
                view.setScaleX(0);
                view.setScaleY(0);
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == searchRow) {
                return VIEW_TYPE_SEARCH;
            } else if ((position >= recentReactionsStartRow && position < recentReactionsEndRow) || (position >= topReactionsStartRow && position < topReactionsEndRow)) {
                return VIEW_TYPE_REACTION;
            } else if (positionToExpand.indexOfKey(position) >= 0) {
                return VIEW_TYPE_EXPAND;
            } else if (positionToButton.indexOfKey(position) >= 0) {
                return VIEW_TYPE_BUTTON;
            } else if (position == longtapHintRow) {
                return VIEW_TYPE_HINT;
            } else if (positionToSection.indexOfKey(position) >= 0 || position == recentReactionsSectionRow || position == popularSectionRow || position == topicEmojiHeaderRow) {
                return VIEW_TYPE_HEADER;
            } if (position == defaultTopicIconRow) {
                return VIEW_TYPE_TOPIC_ICON;
            } else {
                return VIEW_TYPE_EMOJI;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int viewType = holder.getItemViewType();
            if (viewType == VIEW_TYPE_TOPIC_ICON) {
                ImageViewEmoji imageView = (ImageViewEmoji) holder.itemView;
                imageView.position = position;
                imageView.selected = selectedDocumentIds.contains(0L);
                return;
            }
            if (showAnimator == null || !showAnimator.isRunning()) {
                holder.itemView.setScaleX(1);
                holder.itemView.setScaleY(1);
            }
            if (viewType == VIEW_TYPE_HINT) {
                TextView textView = (TextView) holder.itemView;
                if (hintExpireDate != null) {
                    textView.setText(LocaleController.formatString("EmojiStatusExpireHint", R.string.EmojiStatusExpireHint, LocaleController.formatStatusExpireDateTime(hintExpireDate)));
                }
            } else if (viewType == VIEW_TYPE_HEADER) {
                HeaderView header = (HeaderView) holder.itemView;
                if (position == topicEmojiHeaderRow) {
                    header.setText(LocaleController.getString("SelectTopicIconHint", R.string.SelectTopicIconHint), false);
                    header.closeIcon.setVisibility(View.GONE);
                    return;
                }
                if (position == recentReactionsSectionRow) {
                    header.setText(LocaleController.getString("RecentlyUsed", R.string.RecentlyUsed), false);
                    header.closeIcon.setVisibility(View.VISIBLE);
                    header.closeIcon.setOnClickListener((view) -> {
                        clearRecent();
                    });
                    return;
                }
                header.closeIcon.setVisibility(View.GONE);
                if (position == popularSectionRow) {
                    header.setText(LocaleController.getString("PopularReactions", R.string.PopularReactions), false);
                    return;
                }

                int index = positionToSection.get(position);
                if (index >= 0) {
                    EmojiView.EmojiPack pack = packs.get(index);
                    header.setText(pack.set.title, !pack.free && !UserConfig.getInstance(currentAccount).isPremium());
                } else {
                    header.setText(null, false);
                }
            } else if (viewType == VIEW_TYPE_REACTION) {
                ImageViewEmoji imageView = (ImageViewEmoji) holder.itemView;
                imageView.position = position;
                ReactionsLayoutInBubble.VisibleReaction currentReaction;
                if ((position >= recentReactionsStartRow && position < recentReactionsEndRow)) {
                    int index = position - recentReactionsStartRow;
                    currentReaction = recentReactions.get(index);
                } else {
                    int index = position - topReactionsStartRow;
                    currentReaction = topReactions.get(index);
                }

                if (imageView.imageReceiver == null) {
                    imageView.imageReceiver = new ImageReceiver();
                    imageView.imageReceiver.setLayerNum(7);
                    imageView.imageReceiver.onAttachedToWindow();
                }
                imageView.reaction = currentReaction;
                imageView.setViewSelected(selectedReactions.contains(currentReaction), false);
                imageView.notDraw = false;
                if (currentReaction.emojicon != null) {
                    imageView.isDefaultReaction = true;
                    TLRPC.TL_availableReaction reaction = MediaDataController.getInstance(currentAccount).getReactionsMap().get(currentReaction.emojicon);
                    if (reaction != null) {
                        SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(reaction.activate_animation, Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
                        imageView.imageReceiver.setImage(ImageLocation.getForDocument(reaction.select_animation), ReactionsUtils.SELECT_ANIMATION_FILTER, null, null, svgThumb, 0, "tgs", currentReaction, 0);
                    } else {
                        imageView.imageReceiver.clearImage();
                    }
                    imageView.span = null;
                    imageView.document = null;
                    imageView.setDrawable(null);
                    if (imageView.premiumLockIconView != null) {
                        imageView.premiumLockIconView.setVisibility(View.GONE);
                        imageView.premiumLockIconView.setImageReceiver(null);
                    }
                } else {
                    imageView.isDefaultReaction = false;
                    imageView.span = new AnimatedEmojiSpan(currentReaction.documentId, null);
                    imageView.document = null;
                    imageView.imageReceiver.clearImage();
                    Drawable drawable = emojiGridView.animatedEmojiDrawables.get(imageView.span.getDocumentId());

                    if (drawable == null) {
                        drawable = AnimatedEmojiDrawable.make(currentAccount, getCacheType(), imageView.span.getDocumentId());
                        emojiGridView.animatedEmojiDrawables.put(imageView.span.getDocumentId(), (AnimatedEmojiDrawable) drawable);
                    }
                    imageView.setDrawable(drawable);

                    if (!UserConfig.getInstance(currentAccount).isPremium()) {
                        if (imageView.premiumLockIconView == null) {
                            imageView.premiumLockIconView = new PremiumLockIconView(getContext(), PremiumLockIconView.TYPE_STICKERS_PREMIUM_LOCKED);
                            imageView.addView(imageView.premiumLockIconView, LayoutHelper.createFrame(12, 12, Gravity.RIGHT | Gravity.BOTTOM));
                        }
                        imageView.premiumLockIconView.setVisibility(View.VISIBLE);
                    }
                }
            } else if (viewType == VIEW_TYPE_EXPAND) {
                EmojiPackExpand button = (EmojiPackExpand) holder.itemView;
                final int i = positionToExpand.get(position);
                EmojiView.EmojiPack pack = i >= 0 && i < packs.size() ? packs.get(i) : null;
                if (i == -1) {
                    recentExpandButton = button;
                    final int maxlen = layoutManager.getSpanCount() * RECENT_MAX_LINES;
                    button.textView.setText("+" + (recent.size() - maxlen + (includeEmpty ? 1 : 0) + 1));
                } else if (pack != null) {
                    if (recentExpandButton == button) {
                        recentExpandButton = null;
                    }
                    final int maxlen = layoutManager.getSpanCount() * EXPAND_MAX_LINES;
                    button.textView.setText("+" + (pack.documents.size() - maxlen + 1));
                } else {
                    if (recentExpandButton == button) {
                        recentExpandButton = null;
                    }
                }
            } else if (viewType == VIEW_TYPE_BUTTON) {
                EmojiPackButton button = (EmojiPackButton) holder.itemView;
                final int packIndex = positionToButton.get(position);
                if (packIndex >= 0 && packIndex < packs.size()) {
                    EmojiView.EmojiPack pack = packs.get(packIndex);
                    if (pack != null) {
                        button.set(pack.set.title, !pack.free && !UserConfig.getInstance(currentAccount).isPremium(), pack.installed, e -> {
                            if (!pack.free && !UserConfig.getInstance(currentAccount).isPremium()) {
                                new PremiumFeatureBottomSheet(baseFragment, getContext(), currentAccount, PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_EMOJI, false).show();
                                return;
                            }
                            Integer p = null;
                            View expandButton = null;
                            for (int i = 0; i < emojiGridView.getChildCount(); ++i) {
                                if (emojiGridView.getChildAt(i) instanceof EmojiPackExpand) {
                                    View child = emojiGridView.getChildAt(i);
                                    int j = emojiGridView.getChildAdapterPosition(child);
                                    if (j >= 0 && positionToExpand.get(j) == packIndex) {
                                        p = j;
                                        expandButton = child;
                                        break;
                                    }
                                }
                            }
                            if (p != null) {
                                expand(p, expandButton);
                            }

                            EmojiPacksAlert.installSet(null, pack.set, false);
                            installedEmojiSets.add(pack.set.id);
                            updateRows(true, true);
                        });
                    }
                }
            } else if (viewType == VIEW_TYPE_SEARCH) {

            } else {
                ImageViewEmoji imageView = (ImageViewEmoji) holder.itemView;
                imageView.empty = false;
                imageView.position = position;
                imageView.setPadding(AndroidUtilities.dp(1), AndroidUtilities.dp(1), AndroidUtilities.dp(1), AndroidUtilities.dp(1));
                final int recentmaxlen = layoutManager.getSpanCount() * RECENT_MAX_LINES;
                final int maxlen = layoutManager.getSpanCount() * EXPAND_MAX_LINES;
                int recentSize;
                if (type == TYPE_TOPIC_ICON) {
                    recentSize = recent.size();
                } else {
                    recentSize = recent.size() > recentmaxlen && !recentExpanded ? recentmaxlen : recent.size() + (includeEmpty ? 1 : 0);
                }
                boolean selected = false;
                imageView.setDrawable(null);
                if (includeEmpty && position == (searchRow != -1 ? 1 : 0) + (includeHint ? 1 : 0)) {
                    selected = selectedDocumentIds.contains(null);
                    imageView.empty = true;
                    imageView.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(5), AndroidUtilities.dp(5), AndroidUtilities.dp(5));
                    imageView.span = null;
                    imageView.document = null;
                } else if (position - (searchRow != -1 ? 1 : 0) - (includeHint ? 1 : 0) < recentSize) {
                    imageView.span = recent.get(position - (searchRow != -1 ? 1 : 0) - (includeHint ? 1 : 0) - (includeEmpty ? 1 : 0));
                    imageView.document = imageView.span == null ? null : imageView.span.document;
                    selected = imageView.span != null && selectedDocumentIds.contains(imageView.span.getDocumentId());
                } else if (!defaultStatuses.isEmpty() && position - (searchRow != -1 ? 1 : 0) - (includeHint ? 1 : 0) - recentSize - 1 >= 0 && position - (searchRow != -1 ? 1 : 0) - (includeHint ? 1 : 0) - recentSize - 1 < defaultStatuses.size()) {
                    int index = position - (searchRow != -1 ? 1 : 0) - (includeHint ? 1 : 0) - recentSize - 1;
                    imageView.span = defaultStatuses.get(index);
                    imageView.document = imageView.span == null ? null : imageView.span.document;
                    selected = imageView.span != null && selectedDocumentIds.contains(imageView.span.getDocumentId());
                } else {
                    for (int i = 0; i < positionToSection.size(); ++i) {
                        int startPosition = positionToSection.keyAt(i);
                        int index = i - (defaultStatuses.isEmpty() ? 0 : 1);
                        EmojiView.EmojiPack pack = index >= 0 ? packs.get(index) : null;
                        if (pack == null) {
                            continue;
                        }
                        int count = pack.expanded ? pack.documents.size() : Math.min(pack.documents.size(), maxlen);
                        if (position > startPosition && position <= startPosition + 1 + count) {
                            TLRPC.Document document = pack.documents.get(position - startPosition - 1);
                            if (document != null) {
                                imageView.span = new AnimatedEmojiSpan(document, null);
                                imageView.document = document;
                            }
                        }
                    }
                    selected = imageView.span != null && selectedDocumentIds.contains(imageView.span.getDocumentId());
                }

                if (imageView.span != null) {
                    AnimatedEmojiDrawable drawable = emojiGridView.animatedEmojiDrawables.get(imageView.span.getDocumentId());
                    if (drawable == null) {
                        drawable = AnimatedEmojiDrawable.make(currentAccount, getCacheType(), imageView.span.getDocumentId());
                        emojiGridView.animatedEmojiDrawables.put(imageView.span.getDocumentId(), drawable);
                    }
                    imageView.setDrawable(drawable);
                } else {
                    imageView.setDrawable(null);
                }
                imageView.setViewSelected(selected, false);
            }
        }

        @Override
        public int getItemCount() {
            return totalCount;
        }
    }

    private void clearRecent() {
        if (type == TYPE_REACTIONS && onRecentClearedListener != null) {
            onRecentClearedListener.onRecentCleared();
        }
    }

    private class HeaderView extends FrameLayout {
        private LinearLayout layoutView;
        private TextView textView;
        private RLottieImageView lockView;
        ImageView closeIcon;

        public HeaderView(Context context) {
            super(context);

            layoutView = new LinearLayout(context);
            layoutView.setOrientation(LinearLayout.HORIZONTAL);
            addView(layoutView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            lockView = new RLottieImageView(context);
            lockView.setAnimation(R.raw.unlock_icon, 20, 20);
            lockView.setColorFilter(Theme.getColor(Theme.key_chat_emojiPanelStickerSetName, resourcesProvider));
            layoutView.addView(lockView, LayoutHelper.createLinear(20, 20));

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_chat_emojiPanelStickerSetName, resourcesProvider));
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            layoutView.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            closeIcon = new ImageView(context);
            closeIcon.setImageResource(R.drawable.msg_close);
            closeIcon.setScaleType(ImageView.ScaleType.CENTER);
            closeIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_emojiPanelStickerSetNameIcon, resourcesProvider), PorterDuff.Mode.MULTIPLY));
            addView(closeIcon, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(30), MeasureSpec.EXACTLY));
        }

        public void setText(String text, boolean lock) {
            this.textView.setText(text);
            updateLock(lock, false);
        }

        private float lockT;
        private ValueAnimator lockAnimator;

        public void updateLock(boolean lock, boolean animated) {
            if (lockAnimator != null) {
                lockAnimator.cancel();
                lockAnimator = null;
            }

            if (animated) {
                lockAnimator = ValueAnimator.ofFloat(lockT, lock ? 1f : 0f);
                lockAnimator.addUpdateListener(anm -> {
                    lockT = (float) anm.getAnimatedValue();
                    lockView.setTranslationX(AndroidUtilities.dp(-8) * (1f - lockT));
                    textView.setTranslationX(AndroidUtilities.dp(-8) * (1f - lockT));
                    lockView.setAlpha(lockT);
                });
                lockAnimator.setDuration(200);
                lockAnimator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
                lockAnimator.start();
            } else {
                lockT = lock ? 1f : 0f;
                lockView.setTranslationX(AndroidUtilities.dp(-8) * (1f - lockT));
                textView.setTranslationX(AndroidUtilities.dp(-8) * (1f - lockT));
                lockView.setAlpha(lockT);
            }
        }
    }

    private class EmojiPackButton extends FrameLayout {

        FrameLayout addButtonView;
        AnimatedTextView addButtonTextView;
        PremiumButtonView premiumButtonView;

        public EmojiPackButton(Context context) {
            super(context);

            addButtonTextView = new AnimatedTextView(getContext());
            addButtonTextView.setAnimationProperties(.3f, 0, 250, CubicBezierInterpolator.EASE_OUT_QUINT);
            addButtonTextView.setTextSize(AndroidUtilities.dp(14));
            addButtonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            addButtonTextView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText, resourcesProvider));
            addButtonTextView.setGravity(Gravity.CENTER);

            addButtonView = new FrameLayout(getContext());
            addButtonView.setBackground(Theme.AdaptiveRipple.filledRect(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider), 8));
            addButtonView.addView(addButtonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
            addView(addButtonView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            premiumButtonView = new PremiumButtonView(getContext(), false);
            premiumButtonView.setIcon(R.raw.unlock_icon);
            addView(premiumButtonView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        private String lastTitle;

        public void set(String title, boolean unlock, boolean installed, OnClickListener onClickListener) {
            lastTitle = title;
            if (unlock) {
                addButtonView.setVisibility(View.GONE);
                premiumButtonView.setVisibility(View.VISIBLE);
                premiumButtonView.setButton(LocaleController.formatString("UnlockPremiumEmojiPack", R.string.UnlockPremiumEmojiPack, title), onClickListener);
            } else {
                premiumButtonView.setVisibility(View.GONE);
                addButtonView.setVisibility(View.VISIBLE);
                addButtonView.setOnClickListener(onClickListener);
            }

            updateInstall(installed, false);
            updateLock(unlock, false);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(8), AndroidUtilities.dp(5), AndroidUtilities.dp(8));
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44) + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY));
        }

        private ValueAnimator installFadeAway;

        public void updateInstall(boolean installed, boolean animated) {
            CharSequence text = installed ?
                    LocaleController.getString("Added", R.string.Added) :
                    LocaleController.formatString("AddStickersCount", R.string.AddStickersCount, lastTitle);
            addButtonTextView.setText(text, animated);
            if (installFadeAway != null) {
                installFadeAway.cancel();
                installFadeAway = null;
            }
            addButtonView.setEnabled(!installed);
            if (animated) {
                installFadeAway = ValueAnimator.ofFloat(addButtonView.getAlpha(), installed ? .6f : 1f);
                addButtonView.setAlpha(addButtonView.getAlpha());
                installFadeAway.addUpdateListener(anm -> {
                    addButtonView.setAlpha((float) anm.getAnimatedValue());
                });
                installFadeAway.setDuration(450);
                installFadeAway.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                installFadeAway.start();
            } else {
                addButtonView.setAlpha(installed ? .6f : 1f);
            }
        }

        private float lockT;
        private Boolean lockShow;
        private ValueAnimator lockAnimator;

        private void updateLock(boolean show, boolean animated) {
            if (lockAnimator != null) {
                lockAnimator.cancel();
                lockAnimator = null;
            }

            if (lockShow != null && lockShow == show) {
                return;
            }
            lockShow = show;

            if (animated) {
                premiumButtonView.setVisibility(View.VISIBLE);
                lockAnimator = ValueAnimator.ofFloat(lockT, show ? 1f : 0f);
                lockAnimator.addUpdateListener(anm -> {
                    lockT = (float) anm.getAnimatedValue();
                    addButtonView.setAlpha(1f - lockT);
                    premiumButtonView.setAlpha(lockT);
                });
                lockAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!show) {
                            premiumButtonView.setVisibility(View.GONE);
                        }
                    }
                });
                lockAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                lockAnimator.setDuration(350);
                lockAnimator.start();
            } else {
                lockT = lockShow ? 1 : 0;
                addButtonView.setAlpha(1f - lockT);
                premiumButtonView.setAlpha(lockT);
                premiumButtonView.setScaleX(lockT);
                premiumButtonView.setScaleY(lockT);
                premiumButtonView.setVisibility(lockShow ? View.VISIBLE : View.GONE);
            }
        }
    }

    public static class EmojiPackExpand extends FrameLayout {
        public TextView textView;

        public EmojiPackExpand(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            textView = new TextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            textView.setTextColor(0xffffffff);// Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
            textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(11), ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_chat_emojiPanelStickerSetName, resourcesProvider), 99)));
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            textView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(1.66f), AndroidUtilities.dp(4), AndroidUtilities.dp(2f));
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        }
    }

    private View animateExpandFromButton;
    private float animateExpandFromButtonTranslate;
    private int animateExpandFromPosition = -1, animateExpandToPosition = -1;
    private long animateExpandStartTime = -1;

    public long animateExpandDuration() {
        return animateExpandAppearDuration() + animateExpandCrossfadeDuration() + 16;
    }

    public long animateExpandAppearDuration() {
        int count = animateExpandToPosition - animateExpandFromPosition;
        return Math.max(450, Math.min(55, count) * 30L);
    }

    public long animateExpandCrossfadeDuration() {
        int count = animateExpandToPosition - animateExpandFromPosition;
        return Math.max(300, Math.min(45, count) * 25L);
    }

    public class ImageViewEmoji extends FrameLayout {
        public boolean empty = false;
        public boolean notDraw = false;
        public int position;
        public TLRPC.Document document;
        public AnimatedEmojiSpan span;
        public ImageReceiver.BackgroundThreadDrawHolder[] backgroundThreadDrawHolder = new ImageReceiver.BackgroundThreadDrawHolder[DrawingInBackgroundThreadDrawable.THREAD_COUNT];
        public ImageReceiver imageReceiver;
        public ImageReceiver imageReceiverToDraw;
        public boolean isDefaultReaction;
        public ReactionsLayoutInBubble.VisibleReaction reaction;
        public Drawable drawable;
        public Rect drawableBounds;
        public float bigReactionSelectedProgress;
        public boolean attached;
        ValueAnimator backAnimator;
        PremiumLockIconView premiumLockIconView;
        public boolean selected;
        private float pressedProgress;
        public float skewAlpha;
        public int skewIndex;
        public boolean isStaticIcon;
        private float selectedProgress;
        final AnimatedEmojiSpan.InvalidateHolder invalidateHolder = new AnimatedEmojiSpan.InvalidateHolder() {
            @Override
            public void invalidate() {
                if (emojiGridView != null) {
                    emojiGridView.invalidate();
                }
            }
        };

        public ImageViewEmoji(Context context) {
            super(context);
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY));
        }

        @Override
        public void setPressed(boolean pressed) {
            if (isPressed() != pressed) {
                super.setPressed(pressed);
                invalidate();
                if (pressed) {
                    if (backAnimator != null) {
                        backAnimator.removeAllListeners();
                        backAnimator.cancel();
                    }
                }
                if (!pressed && pressedProgress != 0) {
                    backAnimator = ValueAnimator.ofFloat(pressedProgress, 0);
                    backAnimator.addUpdateListener(animation -> {
                        pressedProgress = (float) animation.getAnimatedValue();
                        emojiGridView.invalidate();
                    });
                    backAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            backAnimator = null;
                        }
                    });
                    backAnimator.setInterpolator(new OvershootInterpolator(5.0f));
                    backAnimator.setDuration(350);
                    backAnimator.start();
                }
            }
        }

        public void updatePressedProgress() {
            if (isPressed() && pressedProgress != 1f) {
                pressedProgress = Utilities.clamp(pressedProgress + 16f / 100f, 1f, 0);
                invalidate();
            }
        }

        public void update(long time) {
            if (imageReceiverToDraw != null) {
                if (imageReceiverToDraw.getLottieAnimation() != null) {
                    imageReceiverToDraw.getLottieAnimation().updateCurrentFrame(time, true);
                }
                if (imageReceiverToDraw.getAnimation() != null) {
                    imageReceiverToDraw.getAnimation().updateCurrentFrame(time, true);
                }
            }
        }

        public void setViewSelected(boolean selected, boolean animated) {
            if (this.selected != selected) {
                this.selected = selected;
                if (!animated) {
                    selectedProgress = selected ? 1f : 0f;
                }
            }
        }

        public void drawSelected(Canvas canvas, View view) {
            if ((selected || selectedProgress > 0) && !notDraw) {
                if (selected && selectedProgress < 1f) {
                    selectedProgress += 16/ 300f;
                    view.invalidate();
                }
                if (!selected && selectedProgress > 0) {
                    selectedProgress -= 16/ 300f;
                    view.invalidate();
                }
                selectedProgress = Utilities.clamp(selectedProgress, 1f, 0f);

                AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                AndroidUtilities.rectTmp.inset(AndroidUtilities.dp(1), AndroidUtilities.dp(1));
                Paint paint = empty || drawable instanceof AnimatedEmojiDrawable && ((AnimatedEmojiDrawable) drawable).canOverrideColor() ? selectorAccentPaint : selectorPaint;
                int wasAlpha = paint.getAlpha();
                paint.setAlpha((int) (wasAlpha * getAlpha() * selectedProgress));
                canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(4), AndroidUtilities.dp(4), paint);
                paint.setAlpha(wasAlpha);
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attached = true;
            if (drawable instanceof AnimatedEmojiDrawable) {
                ((AnimatedEmojiDrawable) drawable).addView(invalidateHolder);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            attached = false;
            if (this.drawable instanceof AnimatedEmojiDrawable) {
                ((AnimatedEmojiDrawable) this.drawable).removeView(invalidateHolder);
            }
        }

        public void setDrawable(Drawable drawable) {
            if (this.drawable != drawable) {
                if (this.drawable != null && this.drawable instanceof AnimatedEmojiDrawable) {
                    ((AnimatedEmojiDrawable) this.drawable).removeView(invalidateHolder);
                }
                this.drawable = drawable;
                if (attached && drawable instanceof AnimatedEmojiDrawable) {
                    ((AnimatedEmojiDrawable) drawable).addView(invalidateHolder);
                }
            }

        }
    }

    public void onEmojiClick(View view, AnimatedEmojiSpan span) {
        incrementHintUse();
        if (span == null) {
            onEmojiSelected(view, null, null, null);
        } else {
            TLRPC.TL_emojiStatus status = new TLRPC.TL_emojiStatus();
            status.document_id = span.getDocumentId();

            TLRPC.Document document = span.document == null ? AnimatedEmojiDrawable.findDocument(currentAccount, span.documentId) : span.document;
            if (view instanceof ImageViewEmoji) {
                if (type == TYPE_EMOJI_STATUS) {
                    MediaDataController.getInstance(currentAccount).pushRecentEmojiStatus(status);
                }
                if (type == TYPE_EMOJI_STATUS || type == TYPE_SET_DEFAULT_REACTION) {
                    animateEmojiSelect((ImageViewEmoji) view, () -> {
                        onEmojiSelected(view, span.documentId, document, null);
                    });
                } else {
                    onEmojiSelected(view, span.documentId, document, null);
                }
            } else {
                onEmojiSelected(view, span.documentId, document, null);
            }
        }
    }

    private void incrementHintUse() {
        if (type == TYPE_SET_DEFAULT_REACTION) {
            return;
        }
        final String key = "emoji" + (type==TYPE_EMOJI_STATUS ? "status" : "reaction") + "usehint";
        final int value = MessagesController.getGlobalMainSettings().getInt(key, 0);
        if (value <= 3) {
            MessagesController.getGlobalMainSettings().edit().putInt(key, value + 1).apply();
        }
    }

    protected void onReactionClick(ImageViewEmoji emoji, ReactionsLayoutInBubble.VisibleReaction reaction) {

    }

    protected void onEmojiSelected(View view, Long documentId, TLRPC.Document document, Integer until) {

    }

    public void preload(int type, int account) {
        if (MediaDataController.getInstance(account) == null) {
            return;
        }
        MediaDataController.getInstance(account).checkStickers(MediaDataController.TYPE_EMOJIPACKS);
        if (type == TYPE_REACTIONS || type == TYPE_SET_DEFAULT_REACTION) {
            MediaDataController.getInstance(account).checkReactions();
        } else if (type == TYPE_EMOJI_STATUS) {
            MediaDataController.getInstance(account).fetchEmojiStatuses(0, true);
        } else if (type == TYPE_TOPIC_ICON) {
            MediaDataController.getInstance(account).checkDefaultTopicIcons();
        }
        MediaDataController.getInstance(account).getStickerSet(new TLRPC.TL_inputStickerSetEmojiDefaultStatuses(), false);
    }

    public static void preload(int account) {
        if (MediaDataController.getInstance(account) == null) {
            return;
        }
        MediaDataController.getInstance(account).checkStickers(MediaDataController.TYPE_EMOJIPACKS);
        MediaDataController.getInstance(account).fetchEmojiStatuses(0, true);
        MediaDataController.getInstance(account).checkReactions();
        MediaDataController.getInstance(account).getStickerSet(new TLRPC.TL_inputStickerSetEmojiDefaultStatuses(), false);
        MediaDataController.getInstance(account).checkDefaultTopicIcons();
    }

    private boolean defaultSetLoading = false;
    private void updateRows(boolean updateEmojipacks, boolean diff) {
        if (!animationsEnabled) {
            diff = false;
        }
        MediaDataController mediaDataController = MediaDataController.getInstance(UserConfig.selectedAccount);
        if (mediaDataController == null) {
            return;
        }

        if (updateEmojipacks || frozenEmojiPacks == null) {
            frozenEmojiPacks = new ArrayList<>(mediaDataController.getStickerSets(MediaDataController.TYPE_EMOJIPACKS));
        }
        ArrayList<TLRPC.TL_messages_stickerSet> installedEmojipacks = frozenEmojiPacks;
        ArrayList<TLRPC.StickerSetCovered> featuredEmojiPacks = new ArrayList<>(mediaDataController.getFeaturedEmojiSets());

        ArrayList<Integer> prevRowHashCodes = new ArrayList<>(rowHashCodes);
        totalCount = 0;
        recentReactionsSectionRow = -1;
        recentReactionsStartRow = -1;
        recentReactionsEndRow = -1;
        popularSectionRow = -1;
        longtapHintRow = -1;
        defaultTopicIconRow = -1;
        topicEmojiHeaderRow = -1;
        recent.clear();
        defaultStatuses.clear();
        topReactions.clear();
        recentReactions.clear();
        packs.clear();
        positionToSection.clear();
        sectionToPosition.clear();
        positionToExpand.clear();
        rowHashCodes.clear();
        positionToButton.clear();

        if (!installedEmojipacks.isEmpty()) {
            searchRow = totalCount++;
            rowHashCodes.add(9);
        } else {
            searchRow = -1;
        }

        if (type == TYPE_TOPIC_ICON) {
            topicEmojiHeaderRow = totalCount++;
            rowHashCodes.add(12);
            defaultTopicIconRow = totalCount++;
            rowHashCodes.add(7);

            String packName = UserConfig.getInstance(currentAccount).defaultTopicIcons;
            TLRPC.TL_messages_stickerSet defaultSet = null;
            if (packName != null) {
                defaultSet = MediaDataController.getInstance(currentAccount).getStickerSetByName(packName);
                if (defaultSet == null) {
                    defaultSet = MediaDataController.getInstance(currentAccount).getStickerSetByEmojiOrName(packName);
                }
            }

            if (defaultSet == null) {
                defaultSetLoading = true;
            } else {
                if (includeEmpty) {
                    totalCount++;
                    rowHashCodes.add(2);
                }
                if (defaultSet.documents != null && !defaultSet.documents.isEmpty()) {
                    for (int i = 0; i < defaultSet.documents.size(); ++i) {
                        recent.add(new AnimatedEmojiSpan(defaultSet.documents.get(i), null));
                    }
                }

                for (int i = 0; i < recent.size(); ++i) {
                    rowHashCodes.add(Objects.hash(43223, recent.get(i).getDocumentId()));
                    totalCount++;
                }
            }

        }

        if (includeHint && type != TYPE_SET_DEFAULT_REACTION && type != TYPE_TOPIC_ICON) {
            longtapHintRow = totalCount++;
            rowHashCodes.add(6);
        }

        if (recentReactionsToSet != null) {
            topReactionsStartRow = totalCount;
            ArrayList<ReactionsLayoutInBubble.VisibleReaction> tmp = new ArrayList<>();
            tmp.addAll(recentReactionsToSet);
            for (int i = 0; i < 16; i++) {
                if (!tmp.isEmpty()) {
                    topReactions.add(tmp.remove(0));
                }
            }
            for (int i = 0; i < topReactions.size(); ++i) {
                rowHashCodes.add(Objects.hash(-5632, topReactions.get(i).hashCode()));
            }
            totalCount += topReactions.size();
            topReactionsEndRow = totalCount;

            if (!tmp.isEmpty()) {
                boolean allRecentReactionsIsDefault = true;
                for (int i = 0; i < tmp.size(); i++) {
                    if (tmp.get(i).documentId != 0) {
                        allRecentReactionsIsDefault = false;
                        break;
                    }
                }
                if (allRecentReactionsIsDefault) {
                    if (UserConfig.getInstance(currentAccount).isPremium()) {
                        popularSectionRow = totalCount++;
                        rowHashCodes.add(5);
                    }
                } else {
                    recentReactionsSectionRow = totalCount++;
                    rowHashCodes.add(4);
                }

                recentReactionsStartRow = totalCount;
                recentReactions.addAll(tmp);
                for (int i = 0; i < recentReactions.size(); ++i) {
                    rowHashCodes.add(Objects.hash(allRecentReactionsIsDefault ? 4235 : -3142, recentReactions.get(i).hashCode()));
                }
                totalCount += recentReactions.size();
                recentReactionsEndRow = totalCount;
            }
        } else if (type == TYPE_EMOJI_STATUS) {
            ArrayList<TLRPC.EmojiStatus> recentEmojiStatuses = MediaDataController.getInstance(currentAccount).getRecentEmojiStatuses();
            TLRPC.TL_messages_stickerSet defaultSet = MediaDataController.getInstance(currentAccount).getStickerSet(new TLRPC.TL_inputStickerSetEmojiDefaultStatuses(), false);
            if (defaultSet == null) {
                defaultSetLoading = true;
            } else {
                if (includeEmpty) {
                    totalCount++;
                    rowHashCodes.add(2);
                }
                ArrayList<TLRPC.EmojiStatus> defaultEmojiStatuses = MediaDataController.getInstance(currentAccount).getDefaultEmojiStatuses();
                final int maxrecentlen = layoutManager.getSpanCount() * (RECENT_MAX_LINES + 8);
                if (defaultSet.documents != null && !defaultSet.documents.isEmpty()) {
                    for (int i = 0; i < Math.min(layoutManager.getSpanCount() - 1, defaultSet.documents.size()); ++i) {
                        recent.add(new AnimatedEmojiSpan(defaultSet.documents.get(i), null));
                        if (recent.size() + (includeEmpty ? 1 : 0) >= maxrecentlen) {
                            break;
                        }
                    }
                }
                if (recentEmojiStatuses != null && !recentEmojiStatuses.isEmpty()) {
                    for (TLRPC.EmojiStatus emojiStatus : recentEmojiStatuses) {
                        long did;
                        if (emojiStatus instanceof TLRPC.TL_emojiStatus) {
                            did = ((TLRPC.TL_emojiStatus) emojiStatus).document_id;
                        } else if (emojiStatus instanceof TLRPC.TL_emojiStatusUntil && ((TLRPC.TL_emojiStatusUntil) emojiStatus).until > (int) (System.currentTimeMillis() / 1000)) {
                            did = ((TLRPC.TL_emojiStatusUntil) emojiStatus).document_id;
                        } else {
                            continue;
                        }
                        boolean foundDuplicate = false;
                        for (int i = 0; i < recent.size(); ++i) {
                            if (recent.get(i).getDocumentId() == did) {
                                foundDuplicate = true;
                                break;
                            }
                        }
                        if (foundDuplicate)
                            continue;
                        recent.add(new AnimatedEmojiSpan(did, null));
                        if (recent.size() + (includeEmpty ? 1 : 0) >= maxrecentlen) {
                            break;
                        }
                    }
                }
                if (defaultEmojiStatuses != null && !defaultEmojiStatuses.isEmpty()) {
                    for (TLRPC.EmojiStatus emojiStatus : defaultEmojiStatuses) {
                        long did;
                        if (emojiStatus instanceof TLRPC.TL_emojiStatus) {
                            did = ((TLRPC.TL_emojiStatus) emojiStatus).document_id;
                        } else if (emojiStatus instanceof TLRPC.TL_emojiStatusUntil && ((TLRPC.TL_emojiStatusUntil) emojiStatus).until > (int) (System.currentTimeMillis() / 1000)) {
                            did = ((TLRPC.TL_emojiStatusUntil) emojiStatus).document_id;
                        } else {
                            continue;
                        }
                        boolean foundDuplicate = false;
                        for (int i = 0; i < recent.size(); ++i) {
                            if (recent.get(i).getDocumentId() == did) {
                                foundDuplicate = true;
                                break;
                            }
                        }
                        if (!foundDuplicate) {
                            recent.add(new AnimatedEmojiSpan(did, null));
                            if (recent.size() + (includeEmpty ? 1 : 0) >= maxrecentlen) {
                                break;
                            }
                        }
                    }
                }

                final int maxlen = layoutManager.getSpanCount() * RECENT_MAX_LINES;
                int len = maxlen - (includeEmpty ? 1 : 0);
                if (recent.size() > len && !recentExpanded) {
                    for (int i = 0; i < len - 1; ++i) {
                        rowHashCodes.add(Objects.hash(43223, recent.get(i).getDocumentId()));
                        totalCount++;
                    }
                    rowHashCodes.add(Objects.hash(-5531, -1, (recent.size() - maxlen + (includeEmpty ? 1 : 0) + 1)));
                    if (recentExpandButton != null) {
                        recentExpandButton.textView.setText("+" + (recent.size() - maxlen + (includeEmpty ? 1 : 0) + 1));
                    }
                    positionToExpand.put(totalCount, -1);
                    totalCount++;
                } else {
                    for (int i = 0; i < recent.size(); ++i) {
                        rowHashCodes.add(Objects.hash(43223, recent.get(i).getDocumentId()));
                        totalCount++;
                    }
                }
            }
        }
        if (installedEmojipacks != null) {
            for (int i = 0, j = 0; i < installedEmojipacks.size(); ++i) {
                TLRPC.TL_messages_stickerSet set = installedEmojipacks.get(i);
                if (set != null && set.set != null && set.set.emojis && !installedEmojiSets.contains(set.set.id)) {
                    positionToSection.put(totalCount, packs.size());
                    sectionToPosition.put(packs.size(), totalCount);
                    totalCount++;
                    rowHashCodes.add(Objects.hash(9211, set.set.id));

                    EmojiView.EmojiPack pack = new EmojiView.EmojiPack();
                    pack.installed = true;
                    pack.featured = false;
                    pack.expanded = true;
                    pack.free = !MessageObject.isPremiumEmojiPack(set);
                    pack.set = set.set;
                    pack.documents = set.documents;
                    pack.index = packs.size();
                    packs.add(pack);
                    totalCount += pack.documents.size();
                    for (int k = 0; k < pack.documents.size(); ++k) {
                        rowHashCodes.add(Objects.hash(3212, pack.documents.get(k).id));
                    }
                    j++;
                }
            }
        }
        if (featuredEmojiPacks != null) {
            final int maxlen = layoutManager.getSpanCount() * EXPAND_MAX_LINES;
            for (int i = 0; i < featuredEmojiPacks.size(); ++i) {
                TLRPC.StickerSetCovered set1 = featuredEmojiPacks.get(i);
                if (set1 instanceof TLRPC.TL_stickerSetFullCovered) {
                    TLRPC.TL_stickerSetFullCovered set = (TLRPC.TL_stickerSetFullCovered) set1;
                    boolean foundDuplicate = false;
                    for (int j = 0; j < packs.size(); ++j) {
                        if (packs.get(j).set.id == set.set.id) {
                            foundDuplicate = true;
                            break;
                        }
                    }

                    if (foundDuplicate) {
                        continue;
                    }

                    positionToSection.put(totalCount, packs.size());
                    sectionToPosition.put(packs.size(), totalCount);
                    totalCount++;
                    rowHashCodes.add(Objects.hash(9211, set.set.id));

                    EmojiView.EmojiPack pack = new EmojiView.EmojiPack();
                    pack.installed = installedEmojiSets.contains(set.set.id);
                    pack.featured = true;
                    pack.free = !MessageObject.isPremiumEmojiPack(set);
                    pack.set = set.set;
                    pack.documents = set.documents;
                    pack.index = packs.size();
                    pack.expanded = expandedEmojiSets.contains(pack.set.id);

                    if (pack.documents.size() > maxlen && !pack.expanded) {
                        totalCount += maxlen;
                        for (int k = 0; k < maxlen - 1; ++k) {
                            rowHashCodes.add(Objects.hash(3212, pack.documents.get(k).id));
                        }
                        rowHashCodes.add(Objects.hash(-5531, set.set.id, (pack.documents.size() - maxlen + 1)));
                        positionToExpand.put(totalCount - 1, packs.size());
                    } else {
                        totalCount += pack.documents.size();
                        for (int k = 0; k < pack.documents.size(); ++k) {
                            rowHashCodes.add(Objects.hash(3212, pack.documents.get(k).id));
                        }
                    }

                    if (!pack.installed) {
                        positionToButton.put(totalCount, packs.size());
                        totalCount++;
                        rowHashCodes.add(Objects.hash(3321, set.set.id));
                    }

                    packs.add(pack);
                }
            }
        }

        post(() -> {
            emojiTabs.updateEmojiPacks(packs);
        });

        if (diff) {
            emojiGridView.setItemAnimator(emojiItemAnimator);
        } else {
            emojiGridView.setItemAnimator(null);
        }
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return prevRowHashCodes.size();
            }

            @Override
            public int getNewListSize() {
                return rowHashCodes.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return prevRowHashCodes.get(oldItemPosition).equals(rowHashCodes.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return true;
            }
        }, false).dispatchUpdatesTo(adapter);

        if (!emojiGridView.scrolledByUserOnce) {
            emojiGridView.scrollToPosition(1);
        }
    }

    public void expand(int position, View expandButton) {
        int index = positionToExpand.get(position);
        Integer from = null, count = null;
        boolean last;
        int maxlen;
        int fromCount, start, toCount;
        animateExpandFromButtonTranslate = 0;
        if (index >= 0 && index < packs.size()) {
            maxlen = layoutManager.getSpanCount() * EXPAND_MAX_LINES;
            EmojiView.EmojiPack pack = packs.get(index);
            if (pack.expanded) {
                return;
            }
            last = index + 1 == packs.size();

            start = sectionToPosition.get(index);
            expandedEmojiSets.add(pack.set.id);

            fromCount = pack.expanded ? pack.documents.size() : Math.min(maxlen, pack.documents.size());
            if (pack.documents.size() > maxlen) {
                from = start + 1 + fromCount;
            }
            pack.expanded = true;
            toCount = pack.documents.size();
        } else if (index == -1) {
            maxlen = layoutManager.getSpanCount() * RECENT_MAX_LINES;
            if (recentExpanded) {
                return;
            }
            last = false;
            start = (searchRow != -1 ? 1 : 0) + (includeHint ? 1 : 0) + (includeEmpty ? 1 : 0);
            fromCount = recentExpanded ? recent.size() : Math.min(maxlen - (includeEmpty ? 1 : 0) - 2, recent.size());
            toCount = recent.size();
            recentExpanded = true;
            animateExpandFromButtonTranslate = AndroidUtilities.dp(8);
        } else {
            return;
        }
        if (toCount > fromCount) {
            from = start + 1 + fromCount;
            count = toCount - fromCount;
        }

        updateRows(false, true);

        if (from != null && count != null) {
            animateExpandFromButton = expandButton;
            animateExpandFromPosition = from;
            animateExpandToPosition = from + count;
            animateExpandStartTime = SystemClock.elapsedRealtime();

            if (last) {
                final int scrollTo = from;
                final float durationMultiplier = count > maxlen / 2 ? 1.5f : 3.5f;
                post(() -> {
                    try {
                        LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(emojiGridView.getContext(), LinearSmoothScrollerCustom.POSITION_MIDDLE, durationMultiplier);
                        linearSmoothScroller.setTargetPosition(scrollTo);
                        layoutManager.startSmoothScroll(linearSmoothScroller);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                });
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (drawBackground && type != TYPE_TOPIC_ICON) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec((int) Math.min(AndroidUtilities.dp(340 - 16), AndroidUtilities.displaySize.x * .95f), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((int) Math.min(AndroidUtilities.dp(410 - 16 - 64), AndroidUtilities.displaySize.y * .75f), MeasureSpec.AT_MOST)
            );
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private int getCacheType() {
        return type == TYPE_EMOJI_STATUS || type == TYPE_SET_DEFAULT_REACTION ? AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD : AnimatedEmojiDrawable.CACHE_TYPE_ALERT_PREVIEW;
    }

    public class EmojiListView extends RecyclerListView {
        public EmojiListView(Context context) {
            super(context);

            setDrawSelectorBehind(true);
            setClipToPadding(false);
            setSelectorRadius(AndroidUtilities.dp(4));
            setSelectorDrawableColor(Theme.getColor(Theme.key_listSelector, resourcesProvider));
        }

        SparseArray<ArrayList<ImageViewEmoji>> viewsGroupedByLines = new SparseArray<>();
        ArrayList<ArrayList<ImageViewEmoji>> unusedArrays = new ArrayList<>();
        ArrayList<DrawingInBackgroundLine> unusedLineDrawables = new ArrayList<>();
        ArrayList<EmojiListView.DrawingInBackgroundLine> lineDrawables = new ArrayList<>();
        ArrayList<EmojiListView.DrawingInBackgroundLine> lineDrawablesTmp = new ArrayList<>();

        private LongSparseArray<AnimatedEmojiDrawable> animatedEmojiDrawables = new LongSparseArray<>();

        private AnimatedEmojiSpan[] getAnimatedEmojiSpans() {
            AnimatedEmojiSpan[] spans = new AnimatedEmojiSpan[getChildCount()];
            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                if (child instanceof ImageViewEmoji) {
                    spans[i] = ((ImageViewEmoji) child).span;
                }
            }
            return spans;
        }

        public void updateEmojiDrawables() {
            animatedEmojiDrawables = AnimatedEmojiSpan.update(getCacheType(), this, getAnimatedEmojiSpans(), animatedEmojiDrawables);
        }

        @Override
        public boolean drawChild(Canvas canvas, View child, long drawingTime) {
//            if (child instanceof ImageViewEmoji) {
//                return false;
//            }
            return super.drawChild(canvas, child, drawingTime);
        }

        @Override
        protected boolean canHighlightChildAt(View child, float x, float y) {
            if (child instanceof ImageViewEmoji && (((ImageViewEmoji) child).empty || ((ImageViewEmoji) child).drawable instanceof AnimatedEmojiDrawable && ((AnimatedEmojiDrawable) ((ImageViewEmoji) child).drawable).canOverrideColor())) {
                setSelectorDrawableColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider), 30));
            } else {
                setSelectorDrawableColor(Theme.getColor(Theme.key_listSelector, resourcesProvider));
            }
            return super.canHighlightChildAt(child, x, y);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (showAnimator == null || !showAnimator.isRunning()) {
                updateEmojiDrawables();
                lastChildCount = getChildCount();
            }
        }

        private int lastChildCount = -1;

        @Override
        public void dispatchDraw(Canvas canvas) {
            if (getVisibility() != View.VISIBLE) {
                return;
            }

            int restoreTo = canvas.getSaveCount();

            if (lastChildCount != getChildCount() && showAnimator != null && !showAnimator.isRunning()) {
                updateEmojiDrawables();
                lastChildCount = getChildCount();
            }

            if (!selectorRect.isEmpty()) {
                selectorDrawable.setBounds(selectorRect);
                canvas.save();
                if (selectorTransformer != null) {
                    selectorTransformer.accept(canvas);
                }
                selectorDrawable.draw(canvas);
                canvas.restore();
            }

            for (int i = 0; i < viewsGroupedByLines.size(); i++) {
                ArrayList<ImageViewEmoji> arrayList = viewsGroupedByLines.valueAt(i);
                arrayList.clear();
                unusedArrays.add(arrayList);
            }
            viewsGroupedByLines.clear();
            final boolean animatedExpandIn = animateExpandStartTime > 0 && (SystemClock.elapsedRealtime() - animateExpandStartTime) < animateExpandDuration();
            final boolean drawButton = animatedExpandIn && animateExpandFromButton != null && animateExpandFromPosition >= 0;
            if (animatedEmojiDrawables != null) {
                for (int i = 0; i < getChildCount(); ++i) {
                    View child = getChildAt(i);
                    if (child instanceof ImageViewEmoji) {
                        ImageViewEmoji imageViewEmoji = (ImageViewEmoji) child;
                        imageViewEmoji.updatePressedProgress();
                        int top = smoothScrolling ? (int) child.getY() : child.getTop();
                        ArrayList<ImageViewEmoji> arrayList = viewsGroupedByLines.get(top);

                        canvas.save();
                        canvas.translate(imageViewEmoji.getX(), imageViewEmoji.getY());
                        imageViewEmoji.drawSelected(canvas, this);
                        canvas.restore();

                        if (imageViewEmoji.getBackground() != null) {
                            imageViewEmoji.getBackground().setBounds((int) imageViewEmoji.getX(), (int) imageViewEmoji.getY(), (int) imageViewEmoji.getX() + imageViewEmoji.getWidth(), (int) imageViewEmoji.getY() + imageViewEmoji.getHeight());
                            int wasAlpha = 255; // imageViewEmoji.getBackground().getAlpha();
                            imageViewEmoji.getBackground().setAlpha((int) (wasAlpha * imageViewEmoji.getAlpha()));
                            imageViewEmoji.getBackground().draw(canvas);
                            imageViewEmoji.getBackground().setAlpha(wasAlpha);
                        }

                        if (arrayList == null) {
                            if (!unusedArrays.isEmpty()) {
                                arrayList = unusedArrays.remove(unusedArrays.size() - 1);
                            } else {
                                arrayList = new ArrayList<>();
                            }
                            viewsGroupedByLines.put(top, arrayList);
                        }
                        arrayList.add(imageViewEmoji);
                        if (imageViewEmoji.premiumLockIconView != null && imageViewEmoji.premiumLockIconView.getVisibility() == View.VISIBLE) {
                            if (imageViewEmoji.premiumLockIconView.getImageReceiver() == null && imageViewEmoji.imageReceiverToDraw != null) {
                                imageViewEmoji.premiumLockIconView.setImageReceiver(imageViewEmoji.imageReceiverToDraw);
                            }
                        }
                    }
                    if (drawButton && child != null) {
                        int position = getChildAdapterPosition(child);
                        if (position == animateExpandFromPosition - (animateExpandFromButtonTranslate > 0 ? 0 : 1)) {
                            float t = CubicBezierInterpolator.EASE_OUT.getInterpolation(MathUtils.clamp((SystemClock.elapsedRealtime() - animateExpandStartTime) / 200f, 0, 1));
                            if (t < 1) {
                                canvas.saveLayerAlpha(child.getLeft(), child.getTop(), child.getRight(), child.getBottom(), (int) (255 * (1f - t)), Canvas.ALL_SAVE_FLAG);
                                canvas.translate(child.getLeft(), child.getTop() + animateExpandFromButtonTranslate);
                                final float scale = .5f + .5f * (1f - t);
                                canvas.scale(scale, scale, child.getWidth() / 2f, child.getHeight() / 2f);
                                animateExpandFromButton.draw(canvas);
                                canvas.restore();
                            }
                        }
                    }
                }
            }

            lineDrawablesTmp.clear();
            lineDrawablesTmp.addAll(lineDrawables);
            lineDrawables.clear();

            long time = System.currentTimeMillis();
            for (int i = 0; i < viewsGroupedByLines.size(); i++) {
                ArrayList<ImageViewEmoji> arrayList = viewsGroupedByLines.valueAt(i);
                ImageViewEmoji firstView = arrayList.get(0);
                int position = getChildAdapterPosition(firstView);
                DrawingInBackgroundLine drawable = null;
                for (int k = 0; k < lineDrawablesTmp.size(); k++) {
                    if (lineDrawablesTmp.get(k).position == position) {
                        drawable = lineDrawablesTmp.get(k);
                        lineDrawablesTmp.remove(k);
                        break;
                    }
                }
                if (drawable == null) {
                    if (!unusedLineDrawables.isEmpty()) {
                        drawable = unusedLineDrawables.remove(unusedLineDrawables.size() - 1);
                    } else {
                        drawable = new DrawingInBackgroundLine();
                    }
                    drawable.position = position;
                    drawable.onAttachToWindow();
                }
                lineDrawables.add(drawable);
                drawable.imageViewEmojis = arrayList;
                canvas.save();
                canvas.translate(firstView.getLeft(), firstView.getY()/* + firstView.getPaddingTop()*/);
                drawable.startOffset = firstView.getLeft();
                int w = getMeasuredWidth() - firstView.getLeft() * 2;
                int h = firstView.getMeasuredHeight();
                if (w > 0 && h > 0) {
                    drawable.draw(canvas, time, w, h, 1f);
                }
                canvas.restore();
            }

            for (int i = 0; i < lineDrawablesTmp.size(); i++) {
                if (unusedLineDrawables.size() < 3) {
                    unusedLineDrawables.add(lineDrawablesTmp.get(i));
                    lineDrawablesTmp.get(i).imageViewEmojis = null;
                    lineDrawablesTmp.get(i).reset();
                } else {
                    lineDrawablesTmp.get(i).onDetachFromWindow();
                }
            }
            lineDrawablesTmp.clear();

            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                if (child instanceof ImageViewEmoji) {
                    ImageViewEmoji imageViewEmoji = (ImageViewEmoji) child;
                    if (imageViewEmoji.premiumLockIconView != null) {
                        canvas.save();
                        canvas.translate(
                            (int) (imageViewEmoji.getX() + imageViewEmoji.premiumLockIconView.getX()),
                            (int) (imageViewEmoji.getY() + imageViewEmoji.premiumLockIconView.getY())
                        );
                        imageViewEmoji.premiumLockIconView.draw(canvas);
                        canvas.restore();
                    }
                } else if (child != null && child != animateExpandFromButton) {
                    canvas.save();
                    canvas.translate((int) child.getX(), (int) child.getY());
                    child.draw(canvas);
                    canvas.restore();
                }
            }

            canvas.restoreToCount(restoreTo);
        }

        public class DrawingInBackgroundLine extends DrawingInBackgroundThreadDrawable {

            public int position;
            public int startOffset;
            ArrayList<ImageViewEmoji> imageViewEmojis;
            ArrayList<ImageViewEmoji> drawInBackgroundViews = new ArrayList<>();
            float skewAlpha = 1f;
            boolean skewBelow = false;

            @Override
            public void draw(Canvas canvas, long time, int w, int h, float alpha) {
                if (imageViewEmojis == null) {
                    return;
                }
                skewAlpha = 1f;
                skewBelow = false;
                if (!imageViewEmojis.isEmpty()) {
                    View firstView = imageViewEmojis.get(0);
                    if (firstView.getY() > getHeight() - getPaddingBottom() - firstView.getHeight()) {
                        skewAlpha = MathUtils.clamp(-(firstView.getY() - getHeight() + getPaddingBottom()) / firstView.getHeight(), 0, 1);
                        skewAlpha = .25f + .75f * skewAlpha;
                    }
                }
                boolean drawInUi = skewAlpha < 1 || isAnimating() || imageViewEmojis.size() <= 4 || SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW || showAnimator != null && showAnimator.isRunning();
                if (!drawInUi) {
                    boolean animatedExpandIn = animateExpandStartTime > 0 && (SystemClock.elapsedRealtime() - animateExpandStartTime) < animateExpandDuration();
                    for (int i = 0; i < imageViewEmojis.size(); i++) {
                        ImageViewEmoji img = imageViewEmojis.get(i);
                        if (img.pressedProgress != 0 || img.backAnimator != null || img.getTranslationX() != 0 || img.getTranslationY() != 0 || img.getAlpha() != 1 || (animatedExpandIn && img.position > animateExpandFromPosition && img.position < animateExpandToPosition) || img.isStaticIcon) {
                            drawInUi = true;
                            break;
                        }
                    }
                }
//                canvas.drawRect(0,0,w,h,Theme.DEBUG_RED);
                if (drawInUi) {
                    prepareDraw(System.currentTimeMillis());
                    drawInUiThread(canvas, alpha);
                    reset();
                } else {
                    super.draw(canvas, time, w, h, alpha);
                }
            }

            float[] verts = new float[16];

            @Override
            public void drawBitmap(Canvas canvas, Bitmap bitmap, Paint paint) {
//                if (skewAlpha < 1) {
//                    final float w = bitmap.getWidth();
//                    final float h = bitmap.getHeight();
//                    final float skew = .85f + .15f * skewAlpha;
///*
//                    verts[0] = hw + w * (0     - .5f) * (skewBelow ? skew : 1f); // x
//                    verts[2] = hw + w * (0.33f - .5f) * (skewBelow ? skew : 1f); // x
//                    verts[4] = hw + w * (0.66f - .5f) * (skewBelow ? skew : 1f); // x
//                    verts[6] = hw + w * (1     - .5f) * (skewBelow ? skew : 1f); // x
//                    verts[1] = verts[3] = verts[5] = verts[7] = (skewBelow ? 1f - skewAlpha : 0) * h; // y
//
//                    verts[8] =  hw + w * (0     - .5f) * (skewBelow ? 1f : skew); // x
//                    verts[10] = hw + w * (0.33f - .5f) * (skewBelow ? 1f : skew); // x
//                    verts[12] = hw + w * (0.66f - .5f) * (skewBelow ? 1f : skew); // x
//                    verts[14] = hw + w * (1     - .5f) * (skewBelow ? 1f : skew); // x
//                    verts[9] = verts[11] = verts[13] = verts[15] = (skewBelow ? 1f : skewAlpha) * h; // y
// */
//                    verts[0] = (skewBelow ? w * (.5f - .5f * skew) : 0);
//                    verts[2] = w * (skewBelow ? (.5f - .166667f * skew) : .333333f);
//                    verts[4] = w * (skewBelow ? (.5f + .166667f * skew) : .666666f);
//                    verts[6] = (skewBelow ? w * (.5f + .5f * skew) : w);
//                    verts[1] = verts[3] = verts[5] = verts[7] = (skewBelow ? h * (1f - skewAlpha) : 0); // y
//
//                    verts[8] = (skewBelow ? 0 : w * (.5f - .5f * skew));
//                    verts[10] = w * (skewBelow ? .333333f : (.5f - .166667f * skew));
//                    verts[12] = w * (skewBelow ? .666666f : (.5f + .166667f * skew));
//                    verts[14] = (skewBelow ? w : w * (.5f + .5f * skew));
//                    verts[9] = verts[11] = verts[13] = verts[15] = (skewBelow ? h : h * skewAlpha); // y
//
//                    canvas.drawBitmapMesh(bitmap, 3, 1, verts, 0, null, 0, paint);
//                } else {
                    canvas.drawBitmap(bitmap, 0, 0, paint);
//                }
            }

            @Override
            public void prepareDraw(long time) {
                drawInBackgroundViews.clear();
                for (int i = 0; i < imageViewEmojis.size(); i++) {
                    ImageViewEmoji imageView = imageViewEmojis.get(i);
                    if (imageView.notDraw) {
                        continue;
                    }
                    ImageReceiver imageReceiver;
                    if (imageView.empty) {
                        Drawable drawable = getPremiumStar();
                        float scale = 1f;
                        if (imageView.pressedProgress != 0 || imageView.selected) {
                            scale *= 0.8f + 0.2f * (1f - (imageView.selected ? .7f : imageView.pressedProgress));
                        }
                        if (drawable == null) {
                            continue;
                        }
                        drawable.setAlpha(255);
                        int topOffset = 0; // (int) (imageView.getHeight() * .03f);
                        int w = imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();
                        int h = imageView.getHeight() - imageView.getPaddingTop() - imageView.getPaddingBottom();
                        AndroidUtilities.rectTmp2.set(
                                (int) (imageView.getWidth() / 2f - w / 2f * imageView.getScaleX() * scale),
                                (int) (imageView.getHeight() / 2f - h / 2f * imageView.getScaleY() * scale),
                                (int) (imageView.getWidth() / 2f + w / 2f * imageView.getScaleX() * scale),
                                (int) (imageView.getHeight() / 2f + h / 2f * imageView.getScaleY() * scale)
                        );
                        AndroidUtilities.rectTmp2.offset(imageView.getLeft() - startOffset, topOffset);
                        if (imageView.drawableBounds == null) {
                            imageView.drawableBounds = new Rect();
                        }
                        imageView.drawableBounds.set(AndroidUtilities.rectTmp2);
                        imageView.drawable = drawable;
                        drawInBackgroundViews.add(imageView);
                    } else {
                        float scale = 1, alpha = 1;
                        if (imageView.pressedProgress != 0 || imageView.selected) {
                            scale *= 0.8f + 0.2f * (1f - (imageView.selected ? .7f : imageView.pressedProgress));
                        }
                        boolean animatedExpandIn = animateExpandStartTime > 0 && (SystemClock.elapsedRealtime() - animateExpandStartTime) < animateExpandDuration();
                        if (animatedExpandIn && animateExpandFromPosition >= 0 && animateExpandToPosition >= 0 && animateExpandStartTime > 0) {
                            int position = getChildAdapterPosition(imageView);
                            final int pos = position - animateExpandFromPosition;
                            final int count = animateExpandToPosition - animateExpandFromPosition;
                            if (pos >= 0 && pos < count) {
                                final float appearDuration = animateExpandAppearDuration();
                                final float AppearT = (MathUtils.clamp((SystemClock.elapsedRealtime() - animateExpandStartTime) / appearDuration, 0, 1));
                                final float alphaT = AndroidUtilities.cascade(AppearT, pos, count, count / 4f);
                                final float scaleT = AndroidUtilities.cascade(AppearT, pos, count, count / 4f);
                                scale *= .5f + appearScaleInterpolator.getInterpolation(scaleT) * .5f;
                                alpha *= alphaT;
                            }
                        } else {
                            alpha = imageView.getAlpha();
                        }

                        if (!imageView.isDefaultReaction && !imageView.isStaticIcon) {
                            AnimatedEmojiSpan span = imageView.span;
                            if (span == null) {
                                continue;
                            }
                            AnimatedEmojiDrawable drawable = null;
                            if (imageView.drawable instanceof AnimatedEmojiDrawable) {
                                drawable = (AnimatedEmojiDrawable) imageView.drawable;
                            }

                            if (drawable == null || drawable.getImageReceiver() == null) {
                                continue;
                            }
                            imageReceiver = drawable.getImageReceiver();
                            drawable.setAlpha((int) (255 * alpha));
                            imageView.setDrawable(drawable);
                            imageView.drawable.setColorFilter(premiumStarColorFilter);
                        } else {
                            imageReceiver = imageView.imageReceiver;
                            imageReceiver.setAlpha(alpha);
                        }
                        if (imageReceiver == null) {
                            continue;
                        }

                        if (imageView.selected) {
                            imageReceiver.setRoundRadius(AndroidUtilities.dp(4));
                        } else {
                            imageReceiver.setRoundRadius(0);
                        }
                        imageView.backgroundThreadDrawHolder[threadIndex] = imageReceiver.setDrawInBackgroundThread(imageView.backgroundThreadDrawHolder[threadIndex], threadIndex);
                        imageView.backgroundThreadDrawHolder[threadIndex].time = time;
                        imageView.imageReceiverToDraw = imageReceiver;

                        imageView.update(time);

                        int topOffset = 0; // (int) (imageView.getHeight() * .03f);
                        int w = imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();
                        int h = imageView.getHeight() - imageView.getPaddingTop() - imageView.getPaddingBottom();
                        AndroidUtilities.rectTmp2.set(imageView.getPaddingLeft(), imageView.getPaddingTop(), imageView.getWidth() - imageView.getPaddingRight(), imageView.getHeight() - imageView.getPaddingBottom());
                        if (imageView.selected && type != TYPE_TOPIC_ICON) {
                            AndroidUtilities.rectTmp2.set(
                                (int) Math.round(AndroidUtilities.rectTmp2.centerX() - AndroidUtilities.rectTmp2.width() / 2f * 0.86f),
                                (int) Math.round(AndroidUtilities.rectTmp2.centerY() - AndroidUtilities.rectTmp2.height() / 2f * 0.86f),
                                (int) Math.round(AndroidUtilities.rectTmp2.centerX() + AndroidUtilities.rectTmp2.width() / 2f * 0.86f),
                                (int) Math.round(AndroidUtilities.rectTmp2.centerY() + AndroidUtilities.rectTmp2.height() / 2f * 0.86f)
                            );
                        }
                        AndroidUtilities.rectTmp2.offset(imageView.getLeft() + (int) imageView.getTranslationX() - startOffset, topOffset);
                        imageView.backgroundThreadDrawHolder[threadIndex].setBounds(AndroidUtilities.rectTmp2);

                        imageView.skewAlpha = 1f;
                        imageView.skewIndex = i;

                        drawInBackgroundViews.add(imageView);
                    }
                }
            }

            @Override
            public void drawInBackground(Canvas canvas) {
                for (int i = 0; i < drawInBackgroundViews.size(); i++) {
                    ImageViewEmoji imageView = drawInBackgroundViews.get(i);
                    if (!imageView.notDraw) {
                        if (imageView.empty) {
                            imageView.drawable.setBounds(imageView.drawableBounds);
                            if (imageView.drawable instanceof AnimatedEmojiDrawable) {
                                ((AnimatedEmojiDrawable) imageView.drawable).draw(canvas, false);
                            } else {
                                imageView.drawable.draw(canvas);
                            }
                        } else if (imageView.imageReceiverToDraw != null) {
//                            imageView.drawable.setColorFilter(premiumStarColorFilter);
                            imageView.imageReceiverToDraw.draw(canvas, imageView.backgroundThreadDrawHolder[threadIndex]);
                        }
                    }
                }
            }

            private OvershootInterpolator appearScaleInterpolator = new OvershootInterpolator(3f);

            @Override
            protected void drawInUiThread(Canvas canvas, float alpha) {
                if (imageViewEmojis != null) {
                    canvas.save();
                    canvas.translate(-startOffset, 0);
                    for (int i = 0; i < imageViewEmojis.size(); i++) {
                        ImageViewEmoji imageView = imageViewEmojis.get(i);
                        if (imageView.notDraw) {
                            continue;
                        }

                        float scale = imageView.getScaleX();
                        if (imageView.pressedProgress != 0 || imageView.selected) {
                            scale *= 0.8f + 0.2f * (1f - ((imageView.selected && type != TYPE_TOPIC_ICON) ? 0.7f : imageView.pressedProgress));
                        }
                        boolean animatedExpandIn = animateExpandStartTime > 0 && (SystemClock.elapsedRealtime() - animateExpandStartTime) < animateExpandDuration();
                        boolean animatedExpandInLocal = animatedExpandIn && animateExpandFromPosition >= 0 && animateExpandToPosition >= 0 && animateExpandStartTime > 0;
                        if (animatedExpandInLocal) {
                            int position = getChildAdapterPosition(imageView);
                            final int pos = position - animateExpandFromPosition;
                            final int count = animateExpandToPosition - animateExpandFromPosition;
                            if (pos >= 0 && pos < count) {
                                final float appearDuration = animateExpandAppearDuration();
                                final float AppearT = (MathUtils.clamp((SystemClock.elapsedRealtime() - animateExpandStartTime) / appearDuration, 0, 1));
                                final float alphaT = AndroidUtilities.cascade(AppearT, pos, count, count / 4f);
                                final float scaleT = AndroidUtilities.cascade(AppearT, pos, count, count / 4f);
                                scale *= .5f + appearScaleInterpolator.getInterpolation(scaleT) * .5f;
                                alpha = alphaT;
                            }
                        } else {
                            alpha = imageView.getAlpha();
                        }

                        AndroidUtilities.rectTmp2.set((int) imageView.getX() + imageView.getPaddingLeft(), imageView.getPaddingTop(), (int) imageView.getX() + imageView.getWidth() - imageView.getPaddingRight(), imageView.getHeight() - imageView.getPaddingBottom());
                        if (!smoothScrolling && !animatedExpandIn) {
                            AndroidUtilities.rectTmp2.offset(0, (int) imageView.getTranslationY());
                        }
                        Drawable drawable = null;
                        if (imageView.empty) {
                            drawable = getPremiumStar();
                            drawable.setBounds(AndroidUtilities.rectTmp2);
                            drawable.setAlpha(255);
                        } else if (!imageView.isDefaultReaction && !imageView.isStaticIcon) {
                            AnimatedEmojiSpan span = imageView.span;
                            if (span == null || imageView.notDraw) {
                                continue;
                            }
                            drawable = imageView.drawable;
                            if (drawable == null) {
                                continue;
                            }
                            drawable.setAlpha(255);
                            drawable.setBounds(AndroidUtilities.rectTmp2);
                        } else if (imageView.imageReceiver != null) {
                            imageView.imageReceiver.setImageCoords(AndroidUtilities.rectTmp2);
                        }
                        if (imageView.drawable instanceof AnimatedEmojiDrawable) {
                            imageView.drawable.setColorFilter(premiumStarColorFilter);
                        }
                        imageView.skewAlpha = skewAlpha;
                        imageView.skewIndex = i;
                        if (scale != 1 || skewAlpha < 1) {
                            canvas.save();
                            canvas.scale(scale, scale, AndroidUtilities.rectTmp2.centerX(), AndroidUtilities.rectTmp2.centerY());
                            skew(canvas, i, imageView.getHeight());
                            drawImage(canvas, drawable, imageView, alpha);
                            canvas.restore();
                        } else {
                            drawImage(canvas, drawable, imageView, alpha);
                        }
                    }
                    canvas.restore();
                }
            }

            private void skew(Canvas canvas, int i, int h) {
                if (skewAlpha < 1) {
                    if (skewBelow) {
                        canvas.translate(0, h);
                        canvas.skew((1f - 2f * i / imageViewEmojis.size()) * -(1f - skewAlpha), 0);
                        canvas.translate(0, -h);
                    } else {
                        canvas.scale(1f, skewAlpha, 0, 0);
                        canvas.skew((1f - 2f * i / imageViewEmojis.size()) * (1f - skewAlpha), 0);
                    }
                }
            }

            private void drawImage(Canvas canvas, Drawable drawable, ImageViewEmoji imageView, float alpha) {
                if (drawable != null) {
                    drawable.setColorFilter(premiumStarColorFilter);
                    drawable.setAlpha((int) (255 * alpha));
                    if (drawable instanceof AnimatedEmojiDrawable) {
                        ((AnimatedEmojiDrawable) drawable).draw(canvas, false);
                    } else {
                        drawable.draw(canvas);
                    }
                    if (imageView.premiumLockIconView != null) {

                    }
                } else if ((imageView.isDefaultReaction || imageView.isStaticIcon) && imageView.imageReceiver != null) {
                    imageView.imageReceiver.setAlpha(alpha);
                    imageView.imageReceiver.draw(canvas);
                }
            }

            @Override
            public void onFrameReady() {
                super.onFrameReady();
                for (int i = 0; i < drawInBackgroundViews.size(); i++) {
                    ImageViewEmoji imageView = drawInBackgroundViews.get(i);
                    if (imageView.backgroundThreadDrawHolder[threadIndex] != null) {
                        imageView.backgroundThreadDrawHolder[threadIndex].release();
                    }
                }
                emojiGridView.invalidate();
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (this == emojiGridView) {
                bigReactionImageReceiver.onAttachedToWindow();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (this == emojiGridView) {
                bigReactionImageReceiver.onDetachedFromWindow();
            }
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttached = true;
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.featuredEmojiDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.stickersDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.recentEmojiStatusesUpdate);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.groupStickersDidLoad);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setBigReactionAnimatedEmoji(null);
        isAttached = false;
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.featuredEmojiDidLoad);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.stickersDidLoad);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.recentEmojiStatusesUpdate);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.groupStickersDidLoad);

        if (scrimDrawable instanceof AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable) {
            ((AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable) scrimDrawable).removeParentView(this);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.stickersDidLoad) {
            if (((int) args[0]) == MediaDataController.TYPE_EMOJIPACKS) {
                updateRows(true, true);
            }
        } else if (id == NotificationCenter.featuredEmojiDidLoad) {
            updateRows(false, true);
        } else if (id == NotificationCenter.recentEmojiStatusesUpdate) {
            updateRows(false, true);
        } else if (id == NotificationCenter.groupStickersDidLoad) {
            if (defaultSetLoading) {
                updateRows(true, true);
                defaultSetLoading = false;
            }
        }
    }

    private Runnable dismiss;
    final float durationScale = 1f;
    final long showDuration = (long) (800 * durationScale);
    private ValueAnimator showAnimator;
    private ValueAnimator hideAnimator;

    public void onShow(Runnable dismiss) {
        if (listStateId != null) {
            Parcelable state = listStates.get(listStateId);
            if (state != null) {
//                layoutManager.onRestoreInstanceState(state);
//                updateTabsPosition(layoutManager.findFirstCompletelyVisibleItemPosition());
            }
        }
        this.dismiss = dismiss;
        if (!drawBackground) {
            checkScroll();
            for (int i = 0; i < emojiGridView.getChildCount(); ++i) {
                View child = emojiGridView.getChildAt(i);
                child.setScaleX(1);
                child.setScaleY(1);
            }
            return;
        }
        if (showAnimator != null) {
            showAnimator.cancel();
            showAnimator = null;
        }
        if (hideAnimator != null) {
            hideAnimator.cancel();
            hideAnimator = null;
        }
        boolean animated = type != TYPE_TOPIC_ICON;

        if (animated) {
            showAnimator = ValueAnimator.ofFloat(0, 1);
            showAnimator.addUpdateListener(anm -> {
                final float t = (float) anm.getAnimatedValue();
                updateShow(t);
            });
            showAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    checkScroll();
                    updateShow(1);
                    for (int i = 0; i < emojiGridView.getChildCount(); ++i) {
                        View child = emojiGridView.getChildAt(i);
                        child.setScaleX(1);
                        child.setScaleY(1);
                    }
                    for (int i = 0; i < emojiTabs.contentView.getChildCount(); ++i) {
                        View child = emojiTabs.contentView.getChildAt(i);
                        child.setScaleX(1);
                        child.setScaleY(1);
                    }
                    emojiTabs.contentView.invalidate();

                    emojiGridView.updateEmojiDrawables();
                }
            });
            updateShow(0);
            showAnimator.setDuration(showDuration);
            showAnimator.start();
        } else {
            checkScroll();
            updateShow(1);
        }
    }

    private class SearchBox extends FrameLayout {
        private FrameLayout box;
        private ImageView search;
        private ImageView clear;
        private CloseProgressDrawable2 clearDrawable;
        private EditTextCaption input;
        public SearchBox(Context context) {
            super(context);

            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));

            box = new FrameLayout(context);
            box.setBackground(Theme.createRoundRectDrawable(dp(18), Theme.getColor(Theme.key_chat_emojiPanelBackground, resourcesProvider)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                box.setClipToOutline(true);
                box.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), (int) dp(18));
                    }
                });
            }
            addView(box, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.FILL_HORIZONTAL, 8, 4 + 8, 8, 8));

            search = new ImageView(context);
            search.setScaleType(ImageView.ScaleType.CENTER);
            search.setImageResource(R.drawable.smiles_inputsearch);
            search.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_emojiSearchIcon, resourcesProvider), PorterDuff.Mode.MULTIPLY));
            box.addView(search, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.TOP));

            input = new EditTextCaption(context, resourcesProvider) {

                @Override
                protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
                    if (focused) {
                        onInputFocus();
                        AndroidUtilities.runOnUIThread(() -> {
                            AndroidUtilities.showKeyboard(input);
                        }, 200);

                    }
                    super.onFocusChanged(focused, direction, previouslyFocusedRect);
                }
            };
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    search(input.getText() == null || AndroidUtilities.trim(input.getText(), null).length() == 0 ? null : input.getText().toString());
                }
            });
            input.setBackground(null);
            input.setPadding(0, 0, AndroidUtilities.dp(4), 0);
            input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            if (type == TYPE_EMOJI_STATUS) {
                input.setHint(LocaleController.getString(R.string.SearchEmojiHint));
            } else if (type == TYPE_REACTIONS || type == TYPE_SET_DEFAULT_REACTION) {
                input.setHint(LocaleController.getString(R.string.SearchReactionsHint));
            } else {
                input.setHint(LocaleController.getString(R.string.SearchIconsHint));
            }

            input.setHintTextColor(Theme.getColor(Theme.key_chat_emojiSearchIcon, resourcesProvider));
            input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            input.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            input.setCursorColor(Theme.getColor(Theme.key_featuredStickers_addedIcon, resourcesProvider));
            input.setCursorSize(dp(20));
            input.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            input.setCursorWidth(1.5f);
            input.setMaxLines(1);
            input.setSingleLine(true);
            input.setLines(1);
            box.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 36,  -1, 32, 0));

            clear = new ImageView(context);
            clear.setScaleType(ImageView.ScaleType.CENTER);
            clear.setImageDrawable(clearDrawable = new CloseProgressDrawable2(1.25f) {
                @Override
                protected int getCurrentColor() {
                    return Theme.getColor(Theme.key_chat_emojiSearchIcon, resourcesProvider);
                }
            });
            clearDrawable.setSide(AndroidUtilities.dp(7));
            clear.setScaleX(0.1f);
            clear.setScaleY(0.1f);
            clear.setAlpha(0.0f);
            box.addView(clear, LayoutHelper.createFrame(36, 36, Gravity.RIGHT | Gravity.TOP));
            clear.setOnClickListener(v -> {
                input.setText("");
                search(null);
            });

            setOnClickListener(e -> {
                onInputFocus();
                input.requestFocus();
                scrollToPosition(0, 0);
            });
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(4 + 8+36+8), MeasureSpec.EXACTLY));
        }
    }

    protected void onInputFocus() {

    }

    private void updateShow(float t) {
        if (bubble1View != null) {
            float bubble1t = MathUtils.clamp((t * showDuration - 0) / 120 / durationScale, 0, 1);
            bubble1t = CubicBezierInterpolator.EASE_OUT.getInterpolation(bubble1t);
            bubble1View.setAlpha(bubble1t);
            bubble1View.setScaleX(bubble1t);
            bubble1View.setScaleY(bubble1t);
        }

        if (bubble2View != null) {
            float bubble2t = MathUtils.clamp((t * showDuration - 30) / 120 / durationScale, 0, 1);
//            bubble2t = CubicBezierInterpolator.EASE_OUT.getInterpolation(bubble2t);
            bubble2View.setAlpha(bubble2t);
            bubble2View.setScaleX(bubble2t);
            bubble2View.setScaleY(bubble2t);
        }

        float containerx = MathUtils.clamp((t * showDuration - 40) / 700, 0, 1);
        float containery = MathUtils.clamp((t * showDuration - 80) / 700, 0, 1);
        float containeritemst = MathUtils.clamp((t * showDuration - 40) / 750, 0, 1);
        float containeralphat = MathUtils.clamp((t * showDuration - 30) / 120, 0, 1);
        containerx = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(containerx);
        containery = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(containery);
//        containeritemst = endslow.getInterpolation(containeritemst);
//        containeralphat = CubicBezierInterpolator.EASE_OUT.getInterpolation(containeralphat);
        contentView.setAlpha(containeralphat);
        if (scrimDrawable != null) {
            invalidate();
        }
        contentView.setTranslationY(AndroidUtilities.dp(-5) * (1f - containeralphat));
        if (bubble2View != null) {
            bubble2View.setTranslationY(AndroidUtilities.dp(-5) * (1f - containeralphat));
        }
        this.scaleX = .15f + .85f * containerx;
        this.scaleY = .075f + .925f * containery;
        if (bubble2View != null) {
            bubble2View.setAlpha(containeralphat);
        }
        contentView.invalidate();
        emojiTabsShadow.setAlpha(containeralphat);
        emojiTabsShadow.setScaleX(Math.min(scaleX, 1));

        final float px = emojiTabsShadow.getPivotX(), py = 0;
        final float fullr = (float) Math.sqrt(Math.max(
            px * px + Math.pow(contentView.getHeight(), 2),
            Math.pow(contentView.getWidth() - px, 2) + Math.pow(contentView.getHeight(), 2)
        ));
        for (int i = 0; i < emojiTabs.contentView.getChildCount(); ++i) {
            View child = emojiTabs.contentView.getChildAt(i);
            float ccx = child.getLeft() + child.getWidth() / 2f, ccy = child.getTop() + child.getHeight() / 2f;
            float distance = (float) Math.sqrt((ccx - px) * (ccx - px) + ccy * ccy * .4f);
            float scale = AndroidUtilities.cascade(containeritemst, distance, fullr, child.getHeight() * 1.75f);
            if (Float.isNaN(scale)) {
                scale = 0;
            }
            child.setScaleX(scale);
            child.setScaleY(scale);
        }
        emojiTabs.contentView.invalidate();
        for (int i = 0; i < emojiGridView.getChildCount(); ++i) {
            View child = emojiGridView.getChildAt(i);
            float cx = child.getLeft() + child.getWidth() / 2f, cy = child.getTop() + child.getHeight() / 2f;
            float distance = (float) Math.sqrt((cx - px) * (cx - px) + cy * cy * .2f);
            float scale = AndroidUtilities.cascade(containeritemst, distance, fullr, child.getHeight() * 1.75f);
            if (Float.isNaN(scale))
                scale = 0;
            child.setScaleX(scale);
            child.setScaleY(scale);
        }
        emojiGridView.invalidate();
    }

    public void onDismiss(Runnable dismiss) {
        if (listStateId != null) {
            listStates.put(listStateId, layoutManager.onSaveInstanceState());
        }

        if (hideAnimator != null) {
            hideAnimator.cancel();
            hideAnimator = null;
        }
        hideAnimator = ValueAnimator.ofFloat(0, 1);
        hideAnimator.addUpdateListener(anm -> {
            float t = 1f - (float) anm.getAnimatedValue();
            setTranslationY(AndroidUtilities.dp(8) * (1f - t));
            if (bubble1View != null) {
                bubble1View.setAlpha(t);
            }
            if (bubble2View != null) {
                bubble2View.setAlpha(t * t);
            }
            contentView.setAlpha(t);
            contentView.invalidate();
            invalidate();
        });
        hideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dismiss.run();
                if (selectStatusDateDialog != null) {
                    selectStatusDateDialog.dismiss();
                    selectStatusDateDialog = null;
                }
            }
        });
        hideAnimator.setDuration(200);
        hideAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        hideAnimator.start();

        if (searchBox != null) {
            AndroidUtilities.hideKeyboard(searchBox.input);
        }
    }

    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    public void setRecentReactions(List<ReactionsLayoutInBubble.VisibleReaction> reactions) {
        recentReactionsToSet = reactions;
        updateRows(false, true);
    }

    public void resetBackgroundBitmaps() {
        for (int i = 0; i < emojiGridView.lineDrawables.size(); i++) {
            EmojiListView.DrawingInBackgroundLine line = emojiGridView.lineDrawables.get(i);
            for (int j = 0; j < line.imageViewEmojis.size(); j++) {
                if (line.imageViewEmojis.get(j).notDraw) {
                    line.imageViewEmojis.get(j).notDraw = false;
                    line.imageViewEmojis.get(j).invalidate();
                    line.reset();
                }
            }
        }
        emojiGridView.invalidate();
        for (int i = 0; i < emojiSearchGridView.lineDrawables.size(); i++) {
            EmojiListView.DrawingInBackgroundLine line = emojiSearchGridView.lineDrawables.get(i);
            for (int j = 0; j < line.imageViewEmojis.size(); j++) {
                if (line.imageViewEmojis.get(j).notDraw) {
                    line.imageViewEmojis.get(j).notDraw = false;
                    line.imageViewEmojis.get(j).invalidate();
                    line.reset();
                }
            }
        }
        emojiSearchGridView.invalidate();
    }

    public void setSelected(Long documentId) {
        selectedDocumentIds.clear();
        selectedDocumentIds.add(documentId);
        if (emojiGridView != null) {
            for (int i = 0; i < emojiGridView.getChildCount(); i++) {
                if (emojiGridView.getChildAt(i) instanceof ImageViewEmoji) {
                    ImageViewEmoji imageViewEmoji = (ImageViewEmoji) emojiGridView.getChildAt(i);
                    if (imageViewEmoji.span != null) {
                        imageViewEmoji.setViewSelected(selectedDocumentIds.contains(imageViewEmoji.span.getDocumentId()), true);
                    } else {
                        imageViewEmoji.setViewSelected(selectedDocumentIds.contains(0L), true);
                    }
                }
            }
            emojiGridView.invalidate();
        }
    }

    public void setScrimDrawable(AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable scrimDrawable, View drawableParent) {
        this.scrimColor = scrimDrawable == null ? 0 : scrimDrawable.getColor();
        this.scrimDrawable = scrimDrawable;
        this.scrimDrawableParent = drawableParent;
        if (scrimDrawable != null) {
            scrimDrawable.addParentView(this);
        }
        invalidate();
    }

    Paint paint = new Paint();

    public void drawBigReaction(Canvas canvas, View view) {
        if (selectedReactionView == null) {
            return;
        }
        bigReactionImageReceiver.setParentView(view);
        if (selectedReactionView != null) {
            if (pressedProgress != 1f && !cancelPressed) {
                pressedProgress += 16f / 1500f;
                if (pressedProgress >= 1f) {
                    pressedProgress = 1f;
                    if (bigReactionListener != null) {
                        bigReactionListener.onLongPressed(selectedReactionView);
                    }
                }
                selectedReactionView.bigReactionSelectedProgress = pressedProgress;
            }

            float pressedViewScale = 1 + 2 * pressedProgress;

            canvas.save();
            canvas.translate(emojiGridView.getX() + selectedReactionView.getX(), gridViewContainer.getY() + emojiGridView.getY() + selectedReactionView.getY());
            paint.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));
            canvas.drawRect(0, 0, selectedReactionView.getMeasuredWidth(), selectedReactionView.getMeasuredHeight(), paint);
            canvas.scale(pressedViewScale, pressedViewScale, selectedReactionView.getMeasuredWidth() / 2f, selectedReactionView.getMeasuredHeight());
            ImageReceiver imageReceiver = selectedReactionView.isDefaultReaction ? bigReactionImageReceiver : selectedReactionView.imageReceiverToDraw;
            if (bigReactionAnimatedEmoji != null && bigReactionAnimatedEmoji.getImageReceiver() != null && bigReactionAnimatedEmoji.getImageReceiver().hasBitmapImage()) {
                imageReceiver = bigReactionAnimatedEmoji.getImageReceiver();
            }
            if (imageReceiver != null) {
                imageReceiver.setImageCoords(0, 0, selectedReactionView.getMeasuredWidth(), selectedReactionView.getMeasuredHeight());
                imageReceiver.draw(canvas);
            }
            canvas.restore();
            view.invalidate();
        }
    }

    private static HashMap<Integer, Parcelable> listStates = new HashMap<Integer, Parcelable>();
    private Integer listStateId;

    public void setSaveState(int saveId) {
        listStateId = saveId;
    }

    public static void clearState(int saveId) {
        listStates.remove(saveId);
    }

    public void setOnLongPressedListener(onLongPressedListener l) {
        bigReactionListener = l;
    }

    public void setOnRecentClearedListener(SelectAnimatedEmojiDialog.onRecentClearedListener onRecentClearedListener) {
        this.onRecentClearedListener = onRecentClearedListener;
    }

    public interface onLongPressedListener {
        void onLongPressed(ImageViewEmoji view);
    }

    public interface onRecentClearedListener {
        void onRecentCleared();
    }

    private class SelectStatusDurationDialog extends Dialog {
        private ImageViewEmoji imageViewEmoji;
        private ImageReceiver imageReceiver;
        private Rect from = new Rect(), to = new Rect(), current = new Rect();
        private Theme.ResourcesProvider resourcesProvider;
        private Runnable parentDialogDismiss;
        private View parentDialogView;

        private int blurBitmapWidth, blurBitmapHeight;
        private Bitmap blurBitmap;
        private Paint blurBitmapPaint;

        private WindowInsets lastInsets;
        private ContentView contentView;

        private LinearLayout linearLayoutView;
        private View emojiPreviewView;
        private ActionBarPopupWindow.ActionBarPopupWindowLayout menuView;

        private BottomSheet dateBottomSheet;
        private boolean changeToScrimColor;

        private int parentDialogX, parentDialogY;
        private int clipBottom;

        private int[] tempLocation = new int[2];

        private class ContentView extends FrameLayout {
            public ContentView(Context context) {
                super(context);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY)
                );
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (blurBitmap != null && blurBitmapPaint != null) {
                    canvas.save();
                    canvas.scale(12f, 12f);
                    blurBitmapPaint.setAlpha((int) (255 * showT));
                    canvas.drawBitmap(blurBitmap, 0, 0, blurBitmapPaint);
                    canvas.restore();
                }
                super.dispatchDraw(canvas);
                if (imageViewEmoji != null) {
                    Drawable drawable = imageViewEmoji.drawable;
                    if (drawable != null) {
                        if (changeToScrimColor) {
                            drawable.setColorFilter(new PorterDuffColorFilter(ColorUtils.blendARGB(scrimColor, Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider), showT), PorterDuff.Mode.MULTIPLY));
                        } else {
                            drawable.setColorFilter(premiumStarColorFilter);
                        }
                        drawable.setAlpha((int) (255 * (1f - showT)));
                        AndroidUtilities.rectTmp.set(current);
                        float scale = 1f;
                        if (imageViewEmoji.pressedProgress != 0 || imageViewEmoji.selected) {
                            scale *= 0.8f + 0.2f * (1f - (imageViewEmoji.selected ? .7f : imageViewEmoji.pressedProgress));
                        }
                        AndroidUtilities.rectTmp2.set(
                            (int) (AndroidUtilities.rectTmp.centerX() - AndroidUtilities.rectTmp.width() / 2 * scale),
                            (int) (AndroidUtilities.rectTmp.centerY() - AndroidUtilities.rectTmp.height() / 2 * scale),
                            (int) (AndroidUtilities.rectTmp.centerX() + AndroidUtilities.rectTmp.width() / 2 * scale),
                            (int) (AndroidUtilities.rectTmp.centerY() + AndroidUtilities.rectTmp.height() / 2 * scale)
                        );
                        float skew = 1f - (1f - imageViewEmoji.skewAlpha) * (1f - showT);
                        canvas.save();
                        if (skew < 1) {
                            canvas.translate(AndroidUtilities.rectTmp2.left, AndroidUtilities.rectTmp2.top);
                            canvas.scale(1f, skew, 0, 0);
                            canvas.skew((1f - 2f * imageViewEmoji.skewIndex / layoutManager.getSpanCount()) * (1f - skew), 0);
                            canvas.translate(-AndroidUtilities.rectTmp2.left, -AndroidUtilities.rectTmp2.top);
                        }
                        canvas.clipRect(0, 0, getWidth(), clipBottom + showT * AndroidUtilities.dp(45));
                        drawable.setBounds(AndroidUtilities.rectTmp2);
                        drawable.draw(canvas);
                        canvas.restore();

                        if (imageViewEmoji.skewIndex == 0) {
                            AndroidUtilities.rectTmp2.offset(AndroidUtilities.dp(8 * skew), 0);
                        } else if (imageViewEmoji.skewIndex == 1) {
                            AndroidUtilities.rectTmp2.offset(AndroidUtilities.dp(4 * skew), 0);
                        } else if (imageViewEmoji.skewIndex == layoutManager.getSpanCount() - 2) {
                            AndroidUtilities.rectTmp2.offset(-AndroidUtilities.dp(-4 * skew), 0);
                        } else if (imageViewEmoji.skewIndex == layoutManager.getSpanCount() - 1) {
                            AndroidUtilities.rectTmp2.offset(AndroidUtilities.dp(-8 * skew), 0);
                        }
                        canvas.saveLayerAlpha(AndroidUtilities.rectTmp2.left, AndroidUtilities.rectTmp2.top, AndroidUtilities.rectTmp2.right, AndroidUtilities.rectTmp2.bottom, (int) (255 * (1f - showT)), Canvas.ALL_SAVE_FLAG);
                        canvas.clipRect(AndroidUtilities.rectTmp2);
                        canvas.translate((int) (bottomGradientView.getX() + SelectAnimatedEmojiDialog.this.contentView.getX() + parentDialogX), (int) bottomGradientView.getY() + SelectAnimatedEmojiDialog.this.contentView.getY() + parentDialogY);
                        bottomGradientView.draw(canvas);
                        canvas.restore();

                    } else if (imageViewEmoji.isDefaultReaction && imageViewEmoji.imageReceiver != null) {
                        imageViewEmoji.imageReceiver.setAlpha(1f - showT);
                        imageViewEmoji.imageReceiver.setImageCoords(current);
                        imageViewEmoji.imageReceiver.draw(canvas);
                    }
                }
                if (imageReceiver != null) {
                    imageReceiver.setAlpha(showT);
                    imageReceiver.setImageCoords(current);
                    imageReceiver.draw(canvas);
                }
            }

            @Override
            protected void onConfigurationChanged(Configuration newConfig) {
                lastInsets = null;
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                if (imageReceiver != null) {
                    imageReceiver.onAttachedToWindow();
                }
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                if (imageReceiver != null) {
                    imageReceiver.onDetachedFromWindow();
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);

                Activity parentActivity = getParentActivity();
                if (parentActivity == null) {
                    return;
                }
                View parentView = parentActivity.getWindow().getDecorView();
                if (blurBitmap == null || blurBitmap.getWidth() != parentView.getMeasuredWidth() || blurBitmap.getHeight() != parentView.getMeasuredHeight()) {
                    prepareBlurBitmap();
                }
            }
        }

        public SelectStatusDurationDialog(Context context, Runnable parentDialogDismiss, View parentDialogView, ImageViewEmoji imageViewEmoji, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.imageViewEmoji = imageViewEmoji;
            this.resourcesProvider = resourcesProvider;
            this.parentDialogDismiss = parentDialogDismiss;
            this.parentDialogView = parentDialogView;

            setContentView(
                this.contentView = new ContentView(context),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );

            linearLayoutView = new LinearLayout(context);
            linearLayoutView.setOrientation(LinearLayout.VERTICAL);

            emojiPreviewView = new View(context) {
                @Override
                protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                    super.onLayout(changed, left, top, right, bottom);
                    getLocationOnScreen(tempLocation);
                    to.set(
                        tempLocation[0],
                        tempLocation[1],
                        tempLocation[0] + getWidth(),
                        tempLocation[1] + getHeight()
                    );
                    AndroidUtilities.lerp(from, to, showT, current);
                }
            };
            linearLayoutView.addView(emojiPreviewView, LayoutHelper.createLinear(160, 160, Gravity.CENTER, 0, 0, 0, 16));

            menuView = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, R.drawable.popup_fixed_alert2, resourcesProvider);
            linearLayoutView.addView(menuView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 0));


            ActionBarMenuItem.addItem(true, false, menuView, 0, LocaleController.getString("SetEmojiStatusUntil1Hour", R.string.SetEmojiStatusUntil1Hour), false, resourcesProvider)
                .setOnClickListener(e -> done((int) (System.currentTimeMillis() / 1000 + 60 * 60)));
            ActionBarMenuItem.addItem(false, false, menuView, 0, LocaleController.getString("SetEmojiStatusUntil2Hours", R.string.SetEmojiStatusUntil2Hours), false, resourcesProvider)
                .setOnClickListener(e -> done((int) (System.currentTimeMillis() / 1000 + 2 * 60 * 60)));
            ActionBarMenuItem.addItem(false, false, menuView, 0, LocaleController.getString("SetEmojiStatusUntil8Hours", R.string.SetEmojiStatusUntil8Hours), false, resourcesProvider)
                .setOnClickListener(e -> done((int) (System.currentTimeMillis() / 1000 + 8 * 60 * 60)));
            ActionBarMenuItem.addItem(false, false, menuView, 0, LocaleController.getString("SetEmojiStatusUntil2Days", R.string.SetEmojiStatusUntil2Days), false, resourcesProvider)
                .setOnClickListener(e -> done((int) (System.currentTimeMillis() / 1000 + 2 * 24 * 60 * 60)));
            ActionBarMenuItem.addItem(false, true, menuView, 0, LocaleController.getString("SetEmojiStatusUntilOther", R.string.SetEmojiStatusUntilOther), false, resourcesProvider)
                .setOnClickListener(e -> {
                    if (dateBottomSheet != null) {
                        return;
                    }
                    boolean[] selected = new boolean[1];
                    BottomSheet.Builder builder = AlertsCreator.createStatusUntilDatePickerDialog(context, System.currentTimeMillis() / 1000, date -> {
                        selected[0] = true;
                        done(date);
                    });
                    builder.setOnPreDismissListener(di -> {
                        if (!selected[0]) {
                            animateMenuShow(true, null);
                        }
                        dateBottomSheet = null;
                    });
                    dateBottomSheet = builder.show();
                    animateMenuShow(false, null);
                });

            contentView.addView(linearLayoutView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            Window window = getWindow();
            if (window != null) {
                window.setWindowAnimations(R.style.DialogNoAnimation);
                window.setBackgroundDrawable(null);

                WindowManager.LayoutParams params = window.getAttributes();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.gravity = Gravity.TOP | Gravity.LEFT;
                params.dimAmount = 0;
                params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                params.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
                if (Build.VERSION.SDK_INT >= 21) {
                    params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
                    contentView.setOnApplyWindowInsetsListener((v, insets) -> {
                        lastInsets = insets;
                        v.requestLayout();
                        return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
                    });
                }
                params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                contentView.setFitsSystemWindows(true);
                contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                if (Build.VERSION.SDK_INT >= 28) {
                    params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                }
                window.setAttributes(params);
            }

            if (imageViewEmoji != null) {
                imageViewEmoji.notDraw = true;
            }
            prepareBlurBitmap();

            imageReceiver = new ImageReceiver();
            imageReceiver.setParentView(contentView);
            imageReceiver.setLayerNum(7);
            TLRPC.Document document = imageViewEmoji.document;
            if (document == null && imageViewEmoji != null && imageViewEmoji.drawable instanceof AnimatedEmojiDrawable) {
                document = ((AnimatedEmojiDrawable) imageViewEmoji.drawable).getDocument();
            }
            if (document != null) {
                String filter = "160_160";
                ImageLocation mediaLocation;
                String mediaFilter;
                SvgHelper.SvgDrawable thumbDrawable = DocumentObject.getSvgThumb(document.thumbs, Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
                if ("video/webm".equals(document.mime_type)) {
                    mediaLocation = ImageLocation.getForDocument(document);
                    mediaFilter = filter + "_" + ImageLoader.AUTOPLAY_FILTER;
                    if (thumbDrawable != null) {
                        thumbDrawable.overrideWidthAndHeight(512, 512);
                    }
                } else {
                    if (thumbDrawable != null && MessageObject.isAnimatedStickerDocument(document, false)) {
                        thumbDrawable.overrideWidthAndHeight(512, 512);
                    }
                    mediaLocation = ImageLocation.getForDocument(document);
                    mediaFilter = filter;
                }
                imageReceiver.setImage(mediaLocation, mediaFilter, ImageLocation.getForDocument(thumb, document), filter, null, null, thumbDrawable, document.size, null, document, 1);
                if (imageViewEmoji.drawable instanceof AnimatedEmojiDrawable && ((AnimatedEmojiDrawable) imageViewEmoji.drawable).canOverrideColor()) {
                    imageReceiver.setColorFilter(premiumStarColorFilter);
                }
            }

            imageViewEmoji.getLocationOnScreen(tempLocation);
            from.left = tempLocation[0] + imageViewEmoji.getPaddingLeft();
            from.top = tempLocation[1] + imageViewEmoji.getPaddingTop();
            from.right = tempLocation[0] + imageViewEmoji.getWidth() - imageViewEmoji.getPaddingRight();
            from.bottom = tempLocation[1] + imageViewEmoji.getHeight() - imageViewEmoji.getPaddingBottom();
            AndroidUtilities.lerp(from, to, showT, current);

            parentDialogView.getLocationOnScreen(tempLocation);
            parentDialogX = tempLocation[0];
            clipBottom = (parentDialogY = tempLocation[1]) + parentDialogView.getHeight();
        }

        private boolean done = false;
        private void done(Integer date) {
            if (done) {
                return;
            }
            done = true;
            boolean showback;
            if (showback = changeToScrimColor = date != null && getOutBounds(from)) {
                parentDialogView.getLocationOnScreen(tempLocation);
                from.offset(tempLocation[0], tempLocation[1]);
            } else {
                imageViewEmoji.getLocationOnScreen(tempLocation);
                from.left = tempLocation[0] + imageViewEmoji.getPaddingLeft();
                from.top = tempLocation[1] + imageViewEmoji.getPaddingTop();
                from.right = tempLocation[0] + imageViewEmoji.getWidth() - imageViewEmoji.getPaddingRight();
                from.bottom = tempLocation[1] + imageViewEmoji.getHeight() - imageViewEmoji.getPaddingBottom();
            }
            if (date != null && parentDialogDismiss != null) {
                parentDialogDismiss.run();
            }
            animateShow(false, () -> {
                onEnd(date);
                super.dismiss();
            }, () -> {
                if (date != null) {
                    try {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    } catch (Exception ignore) {}
                    onEndPartly(date);
                }
            }, !showback);
            animateMenuShow(false, null);
        }

        protected boolean getOutBounds(Rect rect) {
            return false;
        }

        protected void onEnd(Integer date) {

        }

        protected void onEndPartly(Integer date) {

        }

        private Activity getParentActivity() {
            Context currentContext = getContext();
            while (currentContext instanceof ContextWrapper) {
                if (currentContext instanceof Activity)
                    return (Activity) currentContext;
                currentContext = ((ContextWrapper) currentContext).getBaseContext();
            }
            return null;
        }

        private void prepareBlurBitmap() {
            Activity parentActivity = getParentActivity();
            if (parentActivity == null) {
                return;
            }
            View parentView = parentActivity.getWindow().getDecorView();
            int w = (int) (parentView.getMeasuredWidth() / 12.0f);
            int h = (int) (parentView.getMeasuredHeight() / 12.0f);
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(1.0f / 12.0f, 1.0f / 12.0f);
            canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            parentView.draw(canvas);
            if (parentActivity instanceof LaunchActivity && ((LaunchActivity) parentActivity).getActionBarLayout().getLastFragment().getVisibleDialog() != null) {
                ((LaunchActivity) parentActivity).getActionBarLayout().getLastFragment().getVisibleDialog().getWindow().getDecorView().draw(canvas);
            }
            if (parentDialogView != null) {
                parentDialogView.getLocationOnScreen(tempLocation);
                canvas.save();
                canvas.translate(tempLocation[0], tempLocation[1]);
                parentDialogView.draw(canvas);
                canvas.restore();
            }
            Utilities.stackBlurBitmap(bitmap, Math.max(10, Math.max(w, h) / 180));
            blurBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            blurBitmap = bitmap;
        }

        private float showT;
        private boolean showing;
        private ValueAnimator showAnimator;
        private void animateShow(boolean show, Runnable onDone, Runnable onPartly, boolean showback) {
            if (imageViewEmoji == null) {
                if (onDone != null) {
                    onDone.run();
                }
                return;
            }
            if (showAnimator != null) {
                if (showing == show) {
                    return;
                }
                showAnimator.cancel();
            }
            showing = show;
            if (show) {
                imageViewEmoji.notDraw = true;
            }
            final boolean[] partlydone = new boolean[1];
            showAnimator = ValueAnimator.ofFloat(showT, show ? 1f : 0);
            showAnimator.addUpdateListener(anm -> {
                showT = (float) anm.getAnimatedValue();
                AndroidUtilities.lerp(from, to, showT, current);
                contentView.invalidate();

                if (!show) {
                    menuView.setAlpha(showT);
                }

                if (showT < 0.025f && !show) {
                    if (showback) {
                        imageViewEmoji.notDraw = false;
                        emojiGridView.invalidate();
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.startAllHeavyOperations, 4);
                }

                if (showT < .5f && !show && onPartly != null && !partlydone[0]) {
                    partlydone[0] = true;
                    onPartly.run();
                }
            });
            showAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    showT = show ? 1f : 0;
                    AndroidUtilities.lerp(from, to, showT, current);
                    contentView.invalidate();
                    if (!show) {
                        menuView.setAlpha(showT);
                    }
                    if (showT < .5f && !show && onPartly != null && !partlydone[0]) {
                        partlydone[0] = true;
                        onPartly.run();
                    }

                    if (!show) {
                        if (showback) {
                            imageViewEmoji.notDraw = false;
                            emojiGridView.invalidate();
                        }
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.startAllHeavyOperations, 4);
                    }
                    showAnimator = null;
                    contentView.invalidate();
                    if (onDone != null) {
                        onDone.run();
                    }
                }
            });
            showAnimator.setDuration(420);
            showAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            showAnimator.start();
        }

        private float showMenuT;
        private boolean showingMenu;
        private ValueAnimator showMenuAnimator;
        private void animateMenuShow(boolean show, Runnable onDone) {
            if (showMenuAnimator != null) {
                if (showingMenu == show) {
                    return;
                }
                showMenuAnimator.cancel();
            }
            showingMenu = show;
//            imageViewEmoji.notDraw = true;
            showMenuAnimator = ValueAnimator.ofFloat(showMenuT, show ? 1f : 0);
            showMenuAnimator.addUpdateListener(anm -> {
                showMenuT = (float) anm.getAnimatedValue();

                menuView.setBackScaleY(showMenuT);
                menuView.setAlpha(CubicBezierInterpolator.EASE_OUT.getInterpolation(showMenuT));
                final int count = menuView.getItemsCount();
                for (int i = 0; i < count; ++i) {
                    final float at = AndroidUtilities.cascade(showMenuT, i, count, 4);
                    menuView.getItemAt(i).setTranslationY((1f - at) * AndroidUtilities.dp(-12));
                    menuView.getItemAt(i).setAlpha(at);
                }
            });
            showMenuAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    showMenuT = show ? 1f : 0;

                    menuView.setBackScaleY(showMenuT);
                    menuView.setAlpha(CubicBezierInterpolator.EASE_OUT.getInterpolation(showMenuT));
                    final int count = menuView.getItemsCount();
                    for (int i = 0; i < count; ++i) {
                        final float at = AndroidUtilities.cascade(showMenuT, i, count, 4);
                        menuView.getItemAt(i).setTranslationY((1f - at) * AndroidUtilities.dp(-12));
                        menuView.getItemAt(i).setAlpha(at);
                    }

                    showMenuAnimator = null;
                    if (onDone != null) {
                        onDone.run();
                    }
                }
            });
            if (show) {
                showMenuAnimator.setDuration(360);
                showMenuAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            } else {
                showMenuAnimator.setDuration(240);
                showMenuAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
            }
            showMenuAnimator.start();
        }

        @Override
        public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
            boolean res = super.dispatchTouchEvent(ev);
            if (!res && ev.getAction() == MotionEvent.ACTION_DOWN) {
                dismiss();
                return false;
            }
            return res;
        }

        @Override
        public void show() {
            super.show();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopAllHeavyOperations, 4);
            animateShow(true, null, null,true);
            animateMenuShow(true, null);
        }

        private boolean dismissed = false;
        @Override
        public void dismiss() {
            if (dismissed) {
                return;
            }
            done(null);
            dismissed = true;
        }
    }

    public void setForumIconDrawable(Drawable drawable) {
        forumIconDrawable = drawable;
        if (forumIconImage != null) {
            forumIconImage.imageReceiver.setImageBitmap(forumIconDrawable);
        }
    }

    @Override
    public void setPressed(boolean pressed) {
       return;
    }

    void setAnimationsEnabled(boolean aniationsEnabled) {
        this.animationsEnabled = aniationsEnabled;
    }
}
