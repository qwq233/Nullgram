/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ArchiveHintCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.DialogMeUrlCell;
import org.telegram.ui.Cells.DialogsEmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PullForegroundDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class DialogsAdapter extends RecyclerListView.SelectionAdapter implements DialogCell.DialogCellDelegate {
    public final static int VIEW_TYPE_DIALOG = 0,
        VIEW_TYPE_FLICKER = 1,
        VIEW_TYPE_RECENTLY_VIEWED = 2,
        VIEW_TYPE_DIVIDER = 3,
        VIEW_TYPE_ME_URL = 4,
        VIEW_TYPE_EMPTY = 5,
        VIEW_TYPE_USER = 6,
        VIEW_TYPE_HEADER = 7,
        VIEW_TYPE_SHADOW = 8,
        VIEW_TYPE_ARCHIVE = 9,
        VIEW_TYPE_LAST_EMPTY = 10,
        VIEW_TYPE_NEW_CHAT_HINT = 11,
        VIEW_TYPE_TEXT = 12,
        VIEW_TYPE_CONTACTS_FLICKER = 13,
        VIEW_TYPE_HEADER_2 = 14;

    private Context mContext;
    private ArchiveHintCell archiveHintCell;
    private ArrayList<TLRPC.TL_contact> onlineContacts;
    private boolean forceUpdatingContacts;
    private int dialogsCount;
    private int prevContactsCount;
    private int prevDialogsCount;
    private int dialogsType;
    private int folderId;
    private long openedDialogId;
    private int currentCount;
    private boolean isOnlySelect;
    private ArrayList<Long> selectedDialogs;
    private boolean hasHints;
    private int currentAccount;
    private boolean dialogsListFrozen;
    private boolean showArchiveHint;
    private boolean isReordering;
    private long lastSortTime;
    private PullForegroundDrawable pullForegroundDrawable;

    private Drawable arrowDrawable;

    private DialogsPreloader preloader;
    private boolean forceShowEmptyCell;

    private DialogsActivity parentFragment;

    public DialogsAdapter(DialogsActivity fragment, Context context, int type, int folder, boolean onlySelect, ArrayList<Long> selected, int account) {
        mContext = context;
        parentFragment = fragment;
        dialogsType = type;
        folderId = folder;
        isOnlySelect = onlySelect;
        hasHints = folder == 0 && type == 0 && !onlySelect;
        selectedDialogs = selected;
        currentAccount = account;
        if (folderId == 1) {
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            showArchiveHint = preferences.getBoolean("archivehint", true);
            preferences.edit().putBoolean("archivehint", false).commit();
        }
        if (folder == 0) {
            this.preloader = new DialogsPreloader();
        }
    }

    public void setOpenedDialogId(long id) {
        openedDialogId = id;
    }

    public void onReorderStateChanged(boolean reordering) {
        isReordering = reordering;
    }

    public int fixPosition(int position) {
        if (hasHints) {
            position -= 2 + MessagesController.getInstance(currentAccount).hintDialogs.size();
        }
        if (showArchiveHint) {
            position -= 2;
        } else if (dialogsType == 11 || dialogsType == 13) {
            position -= 2;
        } else if (dialogsType == 12) {
            position -= 1;
        }
        return position;
    }

    public boolean isDataSetChanged() {
        int current = currentCount;
        return current != getItemCount() || current == 1;
    }

    public void setDialogsType(int type) {
        dialogsType = type;
        notifyDataSetChanged();
    }

    public int getDialogsType() {
        return dialogsType;
    }

    public int getDialogsCount() {
        return dialogsCount;
    }

    @Override
    public int getItemCount() {
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        ArrayList<TLRPC.Dialog> array = parentFragment.getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen);
        dialogsCount = array.size();
        if (!forceUpdatingContacts && !forceShowEmptyCell && dialogsType != 7 && dialogsType != 8 && dialogsType != 11 && dialogsCount == 0 && (folderId != 0 || messagesController.isLoadingDialogs(folderId) || !MessagesController.getInstance(currentAccount).isDialogsEndReached(folderId))) {
            onlineContacts = null;
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("DialogsAdapter dialogsCount=" + dialogsCount + " dialogsType=" + dialogsType + " isLoadingDialogs=" + messagesController.isLoadingDialogs(folderId) + " isDialogsEndReached=" + MessagesController.getInstance(currentAccount).isDialogsEndReached(folderId));
            }
            if (folderId == 1 && showArchiveHint) {
                return (currentCount = 2);
            }
            return (currentCount = 0);
        }
        if (dialogsCount == 0 && messagesController.isLoadingDialogs(folderId)) {
            return (currentCount = 0);
        }
        int count = dialogsCount;
        if (dialogsType == 7 || dialogsType == 8) {
            if (dialogsCount == 0) {
                count++;
            }
        } else {
            if (!messagesController.isDialogsEndReached(folderId) || dialogsCount == 0) {
                count++;
            }
        }
        boolean hasContacts = false;
        if (hasHints) {
            count += 2 + messagesController.hintDialogs.size();
        } else if (dialogsType == 0 && folderId == 0 && messagesController.isDialogsEndReached(folderId)) {
            if (ContactsController.getInstance(currentAccount).contacts.isEmpty() && !ContactsController.getInstance(currentAccount).doneLoadingContacts && !forceUpdatingContacts) {
                onlineContacts = null;
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("DialogsAdapter loadingContacts=" + (ContactsController.getInstance(currentAccount).contacts.isEmpty() && !ContactsController.getInstance(currentAccount).doneLoadingContacts) + "dialogsCount=" + dialogsCount + " dialogsType=" + dialogsType);
                }
                return (currentCount = 0);
            }

            if (messagesController.getAllFoldersDialogsCount() <= 10 && ContactsController.getInstance(currentAccount).doneLoadingContacts && !ContactsController.getInstance(currentAccount).contacts.isEmpty()) {
                if (onlineContacts == null || prevDialogsCount != dialogsCount || prevContactsCount != ContactsController.getInstance(currentAccount).contacts.size()) {
                    onlineContacts = new ArrayList<>(ContactsController.getInstance(currentAccount).contacts);
                    prevContactsCount = onlineContacts.size();
                    prevDialogsCount = messagesController.dialogs_dict.size();
                    long selfId = UserConfig.getInstance(currentAccount).clientUserId;
                    for (int a = 0, N = onlineContacts.size(); a < N; a++) {
                        long userId = onlineContacts.get(a).user_id;
                        if (userId == selfId || messagesController.dialogs_dict.get(userId) != null) {
                            onlineContacts.remove(a);
                            a--;
                            N--;
                        }
                    }
                    if (onlineContacts.isEmpty()) {
                        onlineContacts = null;
                    }
                    sortOnlineContacts(false);

                    if (parentFragment.getContactsAlpha() == 0f) {
                        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                            @Override
                            public void onChanged() {
                                parentFragment.setContactsAlpha(0f);
                                parentFragment.animateContactsAlpha(1f);

                                unregisterAdapterDataObserver(this);
                            }
                        });
                    }
                }
                if (onlineContacts != null) {
                    count += onlineContacts.size() + 2;
                    hasContacts = true;
                }
            }
        }
        if (folderId == 0 && !hasContacts && dialogsCount == 0 && forceUpdatingContacts) {
            count += 3;
        }
        if (folderId == 0 && onlineContacts != null) {
            if (!hasContacts) {
                onlineContacts = null;
            }
        }
        if (folderId == 1 && showArchiveHint) {
            count += 2;
        }
        if (folderId == 0 && dialogsCount != 0) {
            count++;
            if (dialogsCount > 10 && dialogsType == 0) {
                count++;
            }
        }
        if (dialogsType == 11 || dialogsType == 13) {
            count += 2;
        } else if (dialogsType == 12) {
            count += 1;
        }
        currentCount = count;

        return count;
    }

    public TLObject getItem(int i) {
        if (onlineContacts != null && (dialogsCount == 0 || i >= dialogsCount)) {
            if (dialogsCount == 0) {
                i -= 3;
            } else {
                i -= dialogsCount + 2;
            }
            if (i < 0 || i >= onlineContacts.size()) {
                return null;
            }
            return MessagesController.getInstance(currentAccount).getUser(onlineContacts.get(i).user_id);
        }
        if (showArchiveHint) {
            i -= 2;
        } else if (dialogsType == 11 || dialogsType == 13) {
            i -= 2;
        } else if (dialogsType == 12) {
            i -= 1;
        }
        ArrayList<TLRPC.Dialog> arrayList = parentFragment.getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen);
        if (hasHints) {
            int count = MessagesController.getInstance(currentAccount).hintDialogs.size();
            if (i < 2 + count) {
                return MessagesController.getInstance(currentAccount).hintDialogs.get(i - 1);
            } else {
                i -= count + 2;
            }
        }
        if (i < 0 || i >= arrayList.size()) {
            return null;
        }
        return arrayList.get(i);
    }

    public void sortOnlineContacts(boolean notify) {
        if (onlineContacts == null || notify && (SystemClock.elapsedRealtime() - lastSortTime) < 2000) {
            return;
        }
        lastSortTime = SystemClock.elapsedRealtime();
        try {
            int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            MessagesController messagesController = MessagesController.getInstance(currentAccount);
            Collections.sort(onlineContacts, (o1, o2) -> {
                TLRPC.User user1 = messagesController.getUser(o2.user_id);
                TLRPC.User user2 = messagesController.getUser(o1.user_id);
                int status1 = 0;
                int status2 = 0;
                if (user1 != null) {
                    if (user1.self) {
                        status1 = currentTime + 50000;
                    } else if (user1.status != null) {
                        status1 = user1.status.expires;
                    }
                }
                if (user2 != null) {
                    if (user2.self) {
                        status2 = currentTime + 50000;
                    } else if (user2.status != null) {
                        status2 = user2.status.expires;
                    }
                }
                if (status1 > 0 && status2 > 0) {
                    if (status1 > status2) {
                        return 1;
                    } else if (status1 < status2) {
                        return -1;
                    }
                    return 0;
                } else if (status1 < 0 && status2 < 0) {
                    if (status1 > status2) {
                        return 1;
                    } else if (status1 < status2) {
                        return -1;
                    }
                    return 0;
                } else if (status1 < 0 && status2 > 0 || status1 == 0 && status2 != 0) {
                    return -1;
                } else if (status2 < 0 || status1 != 0) {
                    return 1;
                }
                return 0;
            });
            if (notify) {
                notifyDataSetChanged();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void setDialogsListFrozen(boolean frozen) {
        dialogsListFrozen = frozen;
    }

    public ViewPager getArchiveHintCellPager() {
        return archiveHintCell != null ? archiveHintCell.getViewPager() : null;
    }

    public void updateHasHints() {
        hasHints = folderId == 0 && dialogsType == 0 && !isOnlySelect && !MessagesController.getInstance(currentAccount).hintDialogs.isEmpty();
    }

    @Override
    public void notifyDataSetChanged() {
        updateHasHints();
        super.notifyDataSetChanged();
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        if (holder.itemView instanceof DialogCell) {
            DialogCell dialogCell = (DialogCell) holder.itemView;
            dialogCell.onReorderStateChanged(isReordering, false);
            int position = fixPosition(holder.getAdapterPosition());
            dialogCell.setDialogIndex(position);
            dialogCell.checkCurrentDialogIndex(dialogsListFrozen);
            dialogCell.setChecked(selectedDialogs.contains(dialogCell.getDialogId()), false);
        }
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int viewType = holder.getItemViewType();
        return viewType != VIEW_TYPE_FLICKER && viewType != VIEW_TYPE_EMPTY && viewType != VIEW_TYPE_DIVIDER &&
                viewType != VIEW_TYPE_SHADOW && viewType != VIEW_TYPE_HEADER && viewType != VIEW_TYPE_ARCHIVE &&
                viewType != VIEW_TYPE_LAST_EMPTY && viewType != VIEW_TYPE_NEW_CHAT_HINT && viewType != VIEW_TYPE_CONTACTS_FLICKER;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_DIALOG:
                if (dialogsType == 2) {
                    view = new ProfileSearchCell(mContext);
                } else {
                    DialogCell dialogCell = new DialogCell(parentFragment, mContext, true, false, currentAccount, null);
                    dialogCell.setArchivedPullAnimation(pullForegroundDrawable);
                    dialogCell.setPreloader(preloader);
                    dialogCell.setDialogCellDelegate(this);
                    view = dialogCell;
                }
                break;
            case VIEW_TYPE_FLICKER:
            case VIEW_TYPE_CONTACTS_FLICKER:
                FlickerLoadingView flickerLoadingView = new FlickerLoadingView(mContext);
                flickerLoadingView.setIsSingleCell(true);
                int flickerType = viewType == VIEW_TYPE_CONTACTS_FLICKER ? FlickerLoadingView.CONTACT_TYPE : FlickerLoadingView.DIALOG_CELL_TYPE;
                flickerLoadingView.setViewType(flickerType);
                if (flickerType == FlickerLoadingView.CONTACT_TYPE) {
                    flickerLoadingView.setIgnoreHeightCheck(true);
                }
                if (viewType == VIEW_TYPE_CONTACTS_FLICKER) {
                    flickerLoadingView.setItemsCount((int) (AndroidUtilities.displaySize.y * 0.5f / AndroidUtilities.dp(64)));
                }
                view = flickerLoadingView;
                break;
            case VIEW_TYPE_RECENTLY_VIEWED: {
                HeaderCell headerCell = new HeaderCell(mContext);
                headerCell.setText(LocaleController.getString("RecentlyViewed", R.string.RecentlyViewed));

                TextView textView = new TextView(mContext);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                textView.setText(LocaleController.getString("RecentlyViewedHide", R.string.RecentlyViewedHide));
                textView.setGravity((LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
                headerCell.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, 17, 15, 17, 0));
                textView.setOnClickListener(view1 -> {
                    MessagesController.getInstance(currentAccount).hintDialogs.clear();
                    SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                    preferences.edit().remove("installReferer").commit();
                    notifyDataSetChanged();
                });

                view = headerCell;
                break;
            }
            case VIEW_TYPE_DIVIDER:
                FrameLayout frameLayout = new FrameLayout(mContext) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(12), MeasureSpec.EXACTLY));
                    }
                };
                frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
                View v = new View(mContext);
                v.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                frameLayout.addView(v, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                view = frameLayout;
                break;
            case VIEW_TYPE_ME_URL:
                view = new DialogMeUrlCell(mContext);
                break;
            case VIEW_TYPE_EMPTY:
                view = new DialogsEmptyCell(mContext);
                break;
            case VIEW_TYPE_USER:
                view = new UserCell(mContext, 8, 0, false);
                break;
            case VIEW_TYPE_HEADER:
                view = new HeaderCell(mContext);
                view.setPadding(0, 0, 0, AndroidUtilities.dp(12));
                break;
            case VIEW_TYPE_HEADER_2:
                HeaderCell cell = new HeaderCell(mContext, Theme.key_graySectionText, 16, 0, false);
                cell.setHeight(32);
                view = cell;
                view.setClickable(false);
                break;
            case VIEW_TYPE_SHADOW: {
                view = new ShadowSectionCell(mContext);
                Drawable drawable = Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
                CombinedDrawable combinedDrawable = new CombinedDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)), drawable);
                combinedDrawable.setFullsize(true);
                view.setBackgroundDrawable(combinedDrawable);
                break;
            }
            case VIEW_TYPE_ARCHIVE:
                archiveHintCell = new ArchiveHintCell(mContext);
                view = archiveHintCell;
                break;
            case VIEW_TYPE_LAST_EMPTY: {
                view = new LastEmptyView(mContext);
                break;
            }
            case VIEW_TYPE_NEW_CHAT_HINT: {
                view = new TextInfoPrivacyCell(mContext) {

                    private int movement;
                    private float moveProgress;
                    private long lastUpdateTime;
                    private int originalX;
                    private int originalY;

                    @Override
                    protected void afterTextDraw() {
                        if (arrowDrawable != null) {
                            Rect bounds = arrowDrawable.getBounds();
                            arrowDrawable.setBounds(originalX, originalY, originalX + bounds.width(), originalY + bounds.height());
                        }
                    }

                    @Override
                    protected void onTextDraw() {
                        if (arrowDrawable != null) {
                            Rect bounds = arrowDrawable.getBounds();
                            int dx = (int) (moveProgress * AndroidUtilities.dp(3));
                            originalX = bounds.left;
                            originalY = bounds.top;
                            arrowDrawable.setBounds(originalX + dx, originalY + AndroidUtilities.dp(1), originalX + dx + bounds.width(), originalY + AndroidUtilities.dp(1) + bounds.height());

                            long newUpdateTime = SystemClock.elapsedRealtime();
                            long dt = newUpdateTime - lastUpdateTime;
                            if (dt > 17) {
                                dt = 17;
                            }
                            lastUpdateTime = newUpdateTime;
                            if (movement == 0) {
                                moveProgress += dt / 664.0f;
                                if (moveProgress >= 1.0f) {
                                    movement = 1;
                                    moveProgress = 1.0f;
                                }
                            } else {
                                moveProgress -= dt / 664.0f;
                                if (moveProgress <= 0.0f) {
                                    movement = 0;
                                    moveProgress = 0.0f;
                                }
                            }
                            getTextView().invalidate();
                        }
                    }
                };
                Drawable drawable = Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
                CombinedDrawable combinedDrawable = new CombinedDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)), drawable);
                combinedDrawable.setFullsize(true);
                view.setBackgroundDrawable(combinedDrawable);
                break;
            }
            case VIEW_TYPE_TEXT:
            default: {
                view = new TextCell(mContext);
            }
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, viewType == 5 ? RecyclerView.LayoutParams.MATCH_PARENT : RecyclerView.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    public int lastDialogsEmptyType = -1;
    public int dialogsEmptyType() {
        if (dialogsType == 7 || dialogsType == 8) {
            if (MessagesController.getInstance(currentAccount).isDialogsEndReached(folderId)) {
                return DialogsEmptyCell.TYPE_FILTER_NO_CHATS_TO_DISPLAY;
            } else {
                return DialogsEmptyCell.TYPE_FILTER_ADDING_CHATS;
            }
        } else {
            return onlineContacts != null ? DialogsEmptyCell.TYPE_WELCOME_WITH_CONTACTS : DialogsEmptyCell.TYPE_WELCOME_NO_CONTACTS;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_DIALOG: {
                TLRPC.Dialog dialog = (TLRPC.Dialog) getItem(i);
                TLRPC.Dialog nextDialog = (TLRPC.Dialog) getItem(i + 1);
                if (dialogsType == 2) {
                    ProfileSearchCell cell = (ProfileSearchCell) holder.itemView;
                    long oldDialogId = cell.getDialogId();

                    TLRPC.Chat chat = null;
                    CharSequence title = null;
                    CharSequence subtitle;
                    boolean isRecent = false;

                    if (dialog.id != 0) {
                        chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                        if (chat != null && chat.migrated_to != null) {
                            TLRPC.Chat chat2 = MessagesController.getInstance(currentAccount).getChat(chat.migrated_to.channel_id);
                            if (chat2 != null) {
                                chat = chat2;
                            }
                        }
                    }

                    if (chat != null) {
                        title = chat.title;
                        if (ChatObject.isChannel(chat) && !chat.megagroup) {
                            if (chat.participants_count != 0) {
                                subtitle = LocaleController.formatPluralStringComma("Subscribers", chat.participants_count);
                            } else {
                                if (!ChatObject.isPublic(chat)) {
                                    subtitle = LocaleController.getString("ChannelPrivate", R.string.ChannelPrivate).toLowerCase();
                                } else {
                                    subtitle = LocaleController.getString("ChannelPublic", R.string.ChannelPublic).toLowerCase();
                                }
                            }
                        } else {
                            if (chat.participants_count != 0) {
                                subtitle = LocaleController.formatPluralStringComma("Members", chat.participants_count);
                            } else {
                                if (chat.has_geo) {
                                    subtitle = LocaleController.getString("MegaLocation", R.string.MegaLocation);
                                } else if (!ChatObject.isPublic(chat)) {
                                    subtitle = LocaleController.getString("MegaPrivate", R.string.MegaPrivate).toLowerCase();
                                } else {
                                    subtitle = LocaleController.getString("MegaPublic", R.string.MegaPublic).toLowerCase();
                                }
                            }
                        }
                    } else {
                        subtitle = "";
                    }
                    cell.useSeparator = nextDialog != null;
                    cell.setData(chat, null, title, subtitle, isRecent, false);
                    cell.setChecked(selectedDialogs.contains(cell.getDialogId()), oldDialogId == cell.getDialogId());
                } else {
                    DialogCell cell = (DialogCell) holder.itemView;
                    cell.useSeparator = nextDialog != null;
                    cell.fullSeparator = dialog.pinned && nextDialog != null && !nextDialog.pinned;
                    if (dialogsType == 0) {
                        if (AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(dialog.id == openedDialogId);
                        }
                    }
                    cell.setChecked(selectedDialogs.contains(dialog.id), false);
                    cell.setDialog(dialog, dialogsType, folderId);
                    if (preloader != null && i < 10) {
                        preloader.add(dialog.id);
                    }
                }
                break;
            }
            case VIEW_TYPE_EMPTY: {
                DialogsEmptyCell cell = (DialogsEmptyCell) holder.itemView;
                int fromDialogsEmptyType = lastDialogsEmptyType;
                cell.setType(lastDialogsEmptyType = dialogsEmptyType());
                if (dialogsType != 7 && dialogsType != 8) {
                    cell.setOnUtyanAnimationEndListener(() -> parentFragment.setScrollDisabled(false));
                    cell.setOnUtyanAnimationUpdateListener(progress -> parentFragment.setContactsAlpha(progress));
                    if (!cell.isUtyanAnimationTriggered() && dialogsCount == 0) {
                        parentFragment.setContactsAlpha(0f);
                        parentFragment.setScrollDisabled(true);
                    }
                    if (onlineContacts != null && fromDialogsEmptyType == DialogsEmptyCell.TYPE_WELCOME_NO_CONTACTS) {
                        if (!cell.isUtyanAnimationTriggered()) {
                            cell.startUtyanCollapseAnimation(true);
                        }
                    } else if (forceUpdatingContacts) {
                        if (dialogsCount == 0) {
                            cell.startUtyanCollapseAnimation(false);
                        }
                    } else if (cell.isUtyanAnimationTriggered() && lastDialogsEmptyType == DialogsEmptyCell.TYPE_WELCOME_NO_CONTACTS) {
                        cell.startUtyanExpandAnimation();
                    }
                }
                break;
            }
            case VIEW_TYPE_ME_URL: {
                DialogMeUrlCell cell = (DialogMeUrlCell) holder.itemView;
                cell.setRecentMeUrl((TLRPC.RecentMeUrl) getItem(i));
                break;
            }
            case VIEW_TYPE_USER: {
                UserCell cell = (UserCell) holder.itemView;
                int position;
                if (dialogsCount == 0) {
                    position = i - 3;
                } else {
                    position = i - dialogsCount - 2;
                }
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(onlineContacts.get(position).user_id);
                cell.setData(user, null, null, 0);
                break;
            }
            case VIEW_TYPE_HEADER: {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if (dialogsType == 11 || dialogsType == 12 || dialogsType == 13) {
                    if (i == 0) {
                        cell.setText(LocaleController.getString("ImportHeader", R.string.ImportHeader));
                    } else {
                        cell.setText(LocaleController.getString("ImportHeaderContacts", R.string.ImportHeaderContacts));
                    }
                } else {
                    cell.setText(LocaleController.getString(dialogsCount == 0 && forceUpdatingContacts ? R.string.ConnectingYourContacts : R.string.YourContacts));
                }
                break;
            }
            case VIEW_TYPE_HEADER_2: {
                HeaderCell cell = (HeaderCell) holder.itemView;
                cell.setTextSize(14);
                cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                cell.setBackgroundColor(Theme.getColor(Theme.key_graySection));
                switch (((DialogsActivity.DialogsHeader) getItem(i)).headerType) {
                    case DialogsActivity.DialogsHeader.HEADER_TYPE_MY_CHANNELS:
                        cell.setText(LocaleController.getString("MyChannels", R.string.MyChannels));
                        break;
                    case DialogsActivity.DialogsHeader.HEADER_TYPE_MY_GROUPS:
                        cell.setText(LocaleController.getString("MyGroups", R.string.MyGroups));
                        break;
                    case DialogsActivity.DialogsHeader.HEADER_TYPE_GROUPS:
                        cell.setText(LocaleController.getString("FilterGroups", R.string.FilterGroups));
                        break;
                }
                break;
            }
            case VIEW_TYPE_NEW_CHAT_HINT: {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(LocaleController.getString("TapOnThePencil", R.string.TapOnThePencil));
                if (arrowDrawable == null) {
                    arrowDrawable = mContext.getResources().getDrawable(R.drawable.arrow_newchat);
                    arrowDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4), PorterDuff.Mode.MULTIPLY));
                }
                TextView textView = cell.getTextView();
                textView.setCompoundDrawablePadding(AndroidUtilities.dp(4));
                textView.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null);
                textView.getLayoutParams().width = LayoutHelper.WRAP_CONTENT;
                break;
            }
            case VIEW_TYPE_TEXT: {
                TextCell cell = (TextCell) holder.itemView;
                cell.setColors(Theme.key_windowBackgroundWhiteBlueText4, Theme.key_windowBackgroundWhiteBlueText4);
                cell.setTextAndIcon(LocaleController.getString("CreateGroupForImport", R.string.CreateGroupForImport), R.drawable.msg_groups_create, dialogsCount != 0);
                cell.setIsInDialogs();
                cell.setOffsetFromImage(75);
                break;
            }
        }
        if (i >= dialogsCount + 1) {
            holder.itemView.setAlpha(1f);
        }
    }

    public void setForceUpdatingContacts(boolean forceUpdatingContacts) {
        this.forceUpdatingContacts = forceUpdatingContacts;
    }

    @Override
    public int getItemViewType(int i) {
        if (dialogsCount == 0 && forceUpdatingContacts) {
            switch (i) {
                case 0:
                    return VIEW_TYPE_EMPTY;
                case 1:
                    return VIEW_TYPE_SHADOW;
                case 2:
                    return VIEW_TYPE_HEADER;
                case 3:
                    return VIEW_TYPE_CONTACTS_FLICKER;
            }
        } else if (onlineContacts != null) {
            if (dialogsCount == 0) {
                if (i == 0) {
                    return VIEW_TYPE_EMPTY;
                } else if (i == 1) {
                    return VIEW_TYPE_SHADOW;
                } else if (i == 2) {
                    return VIEW_TYPE_HEADER;
                }
            } else {
                if (i < dialogsCount) {
                    return VIEW_TYPE_DIALOG;
                } else if (i == dialogsCount) {
                    return VIEW_TYPE_SHADOW;
                } else if (i == dialogsCount + 1) {
                    return VIEW_TYPE_HEADER;
                } else if (i == currentCount - 1) {
                    return VIEW_TYPE_LAST_EMPTY;
                }
            }
            return VIEW_TYPE_USER;
        } else if (hasHints) {
            int count = MessagesController.getInstance(currentAccount).hintDialogs.size();
            if (i < 2 + count) {
                if (i == 0) {
                    return VIEW_TYPE_RECENTLY_VIEWED;
                } else if (i == 1 + count) {
                    return VIEW_TYPE_DIVIDER;
                }
                return VIEW_TYPE_ME_URL;
            } else {
                i -= 2 + count;
            }
        } else if (showArchiveHint) {
            if (i == 0) {
                return VIEW_TYPE_ARCHIVE;
            } else if (i == 1) {
                return VIEW_TYPE_SHADOW;
            } else {
                i -= 2;
            }
        } else if (dialogsType == 11 || dialogsType == 13) {
            if (i == 0) {
                return VIEW_TYPE_HEADER;
            } else if (i == 1) {
                return VIEW_TYPE_TEXT;
            } else {
                i -= 2;
            }
        } else if (dialogsType == 12) {
            if (i == 0) {
                return VIEW_TYPE_HEADER;
            } else {
                i -= 1;
            }
        }
        if (folderId == 0 && dialogsCount > 10 && i == currentCount - 2 && dialogsType == 0) {
            return VIEW_TYPE_NEW_CHAT_HINT;
        }
        int size = parentFragment.getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen).size();
        if (i == size) {
            if (!forceShowEmptyCell && dialogsType != 7 && dialogsType != 8 && !MessagesController.getInstance(currentAccount).isDialogsEndReached(folderId)) {
                return VIEW_TYPE_FLICKER;
            } else if (size == 0) {
                return VIEW_TYPE_EMPTY;
            } else {
                return VIEW_TYPE_LAST_EMPTY;
            }
        } else if (i > size) {
            return VIEW_TYPE_LAST_EMPTY;
        }
        if (dialogsType == 2 && getItem(i) instanceof DialogsActivity.DialogsHeader) {
            return VIEW_TYPE_HEADER_2;
        }
        return VIEW_TYPE_DIALOG;
    }

    @Override
    public void notifyItemMoved(int fromPosition, int toPosition) {
        ArrayList<TLRPC.Dialog> dialogs = parentFragment.getDialogsArray(currentAccount, dialogsType, folderId, false);
        int fromIndex = fixPosition(fromPosition);
        int toIndex = fixPosition(toPosition);
        TLRPC.Dialog fromDialog = dialogs.get(fromIndex);
        TLRPC.Dialog toDialog = dialogs.get(toIndex);
        if (dialogsType == 7 || dialogsType == 8) {
            MessagesController.DialogFilter filter = MessagesController.getInstance(currentAccount).selectedDialogFilter[dialogsType == 8 ? 1 : 0];
            int idx1 = filter.pinnedDialogs.get(fromDialog.id);
            int idx2 = filter.pinnedDialogs.get(toDialog.id);
            filter.pinnedDialogs.put(fromDialog.id, idx2);
            filter.pinnedDialogs.put(toDialog.id, idx1);
        } else {
            int oldNum = fromDialog.pinnedNum;
            fromDialog.pinnedNum = toDialog.pinnedNum;
            toDialog.pinnedNum = oldNum;
        }
        Collections.swap(dialogs, fromIndex, toIndex);
        super.notifyItemMoved(fromPosition, toPosition);
    }

    public void setArchivedPullDrawable(PullForegroundDrawable drawable) {
        pullForegroundDrawable = drawable;
    }

    public void didDatabaseCleared() {
        if (preloader != null) {
            preloader.clear();
        }
    }

    public void resume() {
        if (preloader != null) {
            preloader.resume();
        }
    }

    public void pause() {
        if (preloader != null) {
            preloader.pause();
        }
    }

    @Override
    public void onButtonClicked(DialogCell dialogCell) {

    }

    @Override
    public void onButtonLongPress(DialogCell dialogCell) {

    }

    @Override
    public boolean canClickButtonInside() {
        return selectedDialogs.isEmpty();
    }

    public static class DialogsPreloader {

        private final int MAX_REQUEST_COUNT = 4;
        private final int MAX_NETWORK_REQUEST_COUNT = 10 - MAX_REQUEST_COUNT;
        private final int NETWORK_REQUESTS_RESET_TIME = 60_000;

        HashSet<Long> dialogsReadyMap = new HashSet<>();
        HashSet<Long> preloadedErrorMap = new HashSet<>();

        HashSet<Long> loadingDialogs = new HashSet<>();
        ArrayList<Long> preloadDialogsPool = new ArrayList<>();
        int currentRequestCount;
        int networkRequestCount;

        boolean resumed;

        Runnable clearNetworkRequestCount = () -> {
            networkRequestCount = 0;
            start();
        };

        public void add(long dialog_id) {
            if (isReady(dialog_id) || preloadedErrorMap.contains(dialog_id) || loadingDialogs.contains(dialog_id) || preloadDialogsPool.contains(dialog_id)) {
                return;
            }
            preloadDialogsPool.add(dialog_id);
            start();
        }

        private void start() {
            if (!preloadIsAvilable() || !resumed || preloadDialogsPool.isEmpty() || currentRequestCount >= MAX_REQUEST_COUNT || networkRequestCount > MAX_NETWORK_REQUEST_COUNT) {
                return;
            }
            long dialog_id = preloadDialogsPool.remove(0);
            currentRequestCount++;
            loadingDialogs.add(dialog_id);
            MessagesController.getInstance(UserConfig.selectedAccount).ensureMessagesLoaded(dialog_id, 0, new MessagesController.MessagesLoadedCallback() {
                @Override
                public void onMessagesLoaded(boolean fromCache) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (!fromCache) {
                            networkRequestCount++;
                            if (networkRequestCount >= MAX_NETWORK_REQUEST_COUNT) {
                                AndroidUtilities.cancelRunOnUIThread(clearNetworkRequestCount);
                                AndroidUtilities.runOnUIThread(clearNetworkRequestCount, NETWORK_REQUESTS_RESET_TIME);
                            }
                        }
                        if (loadingDialogs.remove(dialog_id)) {
                            dialogsReadyMap.add(dialog_id);
                            updateList();
                            currentRequestCount--;
                            start();
                        }
                    });
                }

                @Override
                public void onError() {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (loadingDialogs.remove(dialog_id)) {
                            preloadedErrorMap.add(dialog_id);
                            currentRequestCount--;
                            start();
                        }
                    });
                }
            });
        }

        private boolean preloadIsAvilable() {
            return false;
           // return DownloadController.getInstance(UserConfig.selectedAccount).getCurrentDownloadMask() != 0;
        }

        public void updateList() {
        }

        public boolean isReady(long currentDialogId) {
            return dialogsReadyMap.contains(currentDialogId);
        }

        public boolean preloadedError(long currendDialogId) {
            return preloadedErrorMap.contains(currendDialogId);
        }

        public void remove(long currentDialogId) {
            preloadDialogsPool.remove(currentDialogId);
        }

        public void clear() {
            dialogsReadyMap.clear();
            preloadedErrorMap.clear();
            loadingDialogs.clear();
            preloadDialogsPool.clear();
            currentRequestCount = 0;
            networkRequestCount = 0;
            AndroidUtilities.cancelRunOnUIThread(clearNetworkRequestCount);
            updateList();
        }

        public void resume() {
            resumed = true;
            start();
        }

        public void pause() {
            resumed = false;
        }
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setForceShowEmptyCell(boolean forceShowEmptyCell) {
        this.forceShowEmptyCell = forceShowEmptyCell;
    }

    public class LastEmptyView extends View {

        public boolean moving;

        public LastEmptyView(Context context) {
            super(context);
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int size = parentFragment.getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen).size();
            boolean hasArchive = dialogsType == 0 && MessagesController.getInstance(currentAccount).dialogs_dict.get(DialogObject.makeFolderDialogId(1)) != null;
            View parent = (View) getParent();
            int height;
            int blurOffset = 0;
            if (parent instanceof BlurredRecyclerView) {
                blurOffset = ((BlurredRecyclerView) parent).blurTopPadding;
            }
            int paddingTop = parent.getPaddingTop();
            paddingTop -= blurOffset;
            if (size == 0 || paddingTop == 0 && !hasArchive) {
                height = 0;
            } else {
                height = MeasureSpec.getSize(heightMeasureSpec);
                if (height == 0) {
                    height = parent.getMeasuredHeight();
                }
                if (height == 0) {
                    height = AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight() - (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
                }
                height -= blurOffset;
                int cellHeight = AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72);
                int dialogsHeight = size * cellHeight + (size - 1);
                if (onlineContacts != null) {
                    dialogsHeight += onlineContacts.size() * AndroidUtilities.dp(58) + (onlineContacts.size() - 1) + AndroidUtilities.dp(52);
                }
                int archiveHeight = (hasArchive ? cellHeight + 1 : 0);
                if (dialogsHeight < height) {
                    height = height - dialogsHeight + archiveHeight;
                    if (paddingTop != 0) {
                        height -= AndroidUtilities.statusBarHeight;
                        if (height < 0) {
                            height = 0;
                        }
                    }
                } else if (dialogsHeight - height < archiveHeight) {
                    height = archiveHeight - (dialogsHeight - height);
                    if (paddingTop != 0) {
                        height -= AndroidUtilities.statusBarHeight;
                    }
                    if (height < 0) {
                        height = 0;
                    }
                } else {
                    height = 0;
                }
            }
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
        }
    }
}
