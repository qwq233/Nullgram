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

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import top.qwq2333.nullgram.config.ConfigManager;
import top.qwq2333.nullgram.helpers.EntitiesHelper;
import top.qwq2333.nullgram.ui.PopupBuilder;
import top.qwq2333.nullgram.ui.StickerSizePreviewMessagesCell;
import top.qwq2333.nullgram.utils.AlertUtil;
import top.qwq2333.nullgram.utils.Defines;
import top.qwq2333.nullgram.utils.NumberUtils;
import top.qwq2333.nullgram.utils.StringUtils;

@SuppressLint("NotifyDataSetChanged")
public class ChatSettingActivity extends BaseActivity {

    private ActionBarMenuItem resetItem;
    private StickerSizeCell stickerSizeCell;

    private int stickerSizeHeaderRow;
    private int stickerSizeRow;
    private int stickerSize2Row;

    private int chatRow;
    private int ignoreBlockedUserMessagesRow;
    private int hideGroupStickerRow;
    private int disablePremiumStickerRow;
    private int messageMenuRow;
    private int allowScreenshotOnNoForwardChatRow;
    private int labelChannelUserRow;
    private int displaySpoilerDirectlyRow;
    private int disableJumpToNextChannelRow;
    private int disableGreetingStickerRow;
    private int disableTrendingStickerRow;
    private int disablePreviewVideoSoundShortcutRow;
    private int quickToggleAnonymous;
    private int customDoubleClickTapRow;
    private int confirmToSendMediaMessagesRow;
    private int maxRecentStickerRow;
    private int unreadBadgeOnBackButtonRow;
    private int ignoreReactionMentionRow;
    private int showForwardDateRow;
    private int hideTimeForStickerRow;
    private int showMessageIDRow;
    private int hideQuickSendMediaBottomRow;
    private int customQuickMessageRow;
    private int scrollableChatPreviewRow;
    private int showTabsOnForwardRow;
    private int disableStickersAutoReorderRow;
    private int chat2Row;

    private int markdownRow;
    private int markdownDisableRow;
    private int markdownParserRow;
    private int markdownParseLinksRow;
    private int markdown2Row;


    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("Chat", R.string.Chat);
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);

        ActionBarMenu menu = actionBar.createMenu();
        resetItem = menu.addItem(0, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString("ResetStickerSize", R.string.ResetStickerSize));
        resetItem.setVisibility(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f) != 14.0f ? View.VISIBLE : View.GONE);
        resetItem.setTag(null);
        resetItem.setOnClickListener(v -> {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, false, 0.5f, true);
            ValueAnimator animator = ValueAnimator.ofFloat(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f), 14.0f);
            animator.setDuration(150);
            animator.addUpdateListener(valueAnimator -> {
                ConfigManager.putFloat(Defines.stickerSize, (Float) valueAnimator.getAnimatedValue());
                stickerSizeCell.invalidate();
            });
            animator.start();
        });

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        return view;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == ignoreBlockedUserMessagesRow) {
            ConfigManager.toggleBoolean(Defines.ignoreBlockedUser);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.ignoreBlockedUser));
            }
        } else if (position == hideGroupStickerRow) {
            ConfigManager.toggleBoolean(Defines.hideGroupSticker);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.hideGroupSticker));
            }
        } else if (position == disablePremiumStickerRow) {
            ConfigManager.toggleBoolean(Defines.disablePremiumSticker);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disablePremiumSticker));
            }
        } else if (position == messageMenuRow) {
            showMessageMenuAlert();
        } else if (position == allowScreenshotOnNoForwardChatRow) {
            ConfigManager.toggleBoolean(Defines.allowScreenshotOnNoForwardChat);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.allowScreenshotOnNoForwardChat));
            }
        } else if (position == labelChannelUserRow) {
            if (!ConfigManager.getBooleanOrFalse(Defines.channelAlias)) {
                ConfigManager.toggleBoolean(Defines.labelChannelUser);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.labelChannelUser));
                }
            } else {
                AndroidUtilities.shakeView(view);
                AlertUtil.showToast(LocaleController.getString("notAllowedWhenChannelAliasIsEnabled", R.string.notAllowedWhenChannelAliasIsEnabled));
            }
        } else if (position == displaySpoilerDirectlyRow) {
            ConfigManager.toggleBoolean(Defines.displaySpoilerMsgDirectly);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.displaySpoilerMsgDirectly));
            }
        } else if (position == disableJumpToNextChannelRow) {
            ConfigManager.toggleBoolean(Defines.disableJumpToNextChannel);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableJumpToNextChannel));
            }
        } else if (position == disableGreetingStickerRow) {
            ConfigManager.toggleBoolean(Defines.disableGreetingSticker);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableGreetingSticker));
            }
        } else if (position == disableTrendingStickerRow) {
            ConfigManager.toggleBoolean(Defines.disableTrendingSticker);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableTrendingSticker));
            }
        } else if (position == customDoubleClickTapRow) {
            final int currentConfig = ConfigManager.getIntOrDefault(Defines.doubleTab, Defines.doubleTabReaction);
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("Disable", R.string.Disable));
            types.add(Defines.doubleTabNone);
            arrayList.add(LocaleController.getString("Reactions", R.string.Reactions));
            types.add(Defines.doubleTabReaction);
            arrayList.add(LocaleController.getString("Reply", R.string.Reply));
            types.add(Defines.doubleTabReply);
            arrayList.add(LocaleController.getString("Edit", R.string.Edit));
            types.add(Defines.doubleTabEdit);
            arrayList.add(LocaleController.getString("saveMessages", R.string.saveMessages));
            types.add(Defines.doubleTabSaveMessages);
            arrayList.add(LocaleController.getString("Repeat", R.string.Repeat));
            types.add(Defines.doubleTabRepeat);
            arrayList.add(LocaleController.getString("TranslateMessage", R.string.TranslateMessage));
            types.add(Defines.doubleTabTranslate);
            PopupBuilder.show(arrayList, LocaleController.getString("customDoubleTap", R.string.customDoubleTap), types.indexOf(ConfigManager.getIntOrDefault(Defines.doubleTab,
                Defines.doubleTabReaction)), getParentActivity(), view, i -> {
                ConfigManager.putInt(Defines.doubleTab, types.get(i));
                listAdapter.notifyItemChanged(customDoubleClickTapRow, PARTIAL);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            });
        } else if (position == confirmToSendMediaMessagesRow) {
            ConfigManager.toggleBoolean(Defines.confirmToSendMediaMessages);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.confirmToSendMediaMessages));
            }
        } else if (position == maxRecentStickerRow) {
            setMaxRecentSticker(view, position);
            listAdapter.notifyItemChanged(position, PARTIAL);
        } else if (position == unreadBadgeOnBackButtonRow) {
            ConfigManager.toggleBoolean(Defines.unreadBadgeOnBackButton);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.unreadBadgeOnBackButton));
            }
        } else if (position == ignoreReactionMentionRow) {
            ConfigManager.toggleBoolean(Defines.ignoreReactionMention);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.ignoreReactionMention));
            }
        } else if (position == showForwardDateRow) {
            ConfigManager.toggleBoolean(Defines.dateOfForwardedMsg);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.dateOfForwardedMsg));
            }
        } else if (position == hideTimeForStickerRow) {
            ConfigManager.toggleBoolean(Defines.hideTimeForSticker);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.hideTimeForSticker));
            }
        } else if (position == showMessageIDRow) {
            ConfigManager.toggleBoolean(Defines.showMessageID);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.showMessageID));
            }
        } else if (position == hideQuickSendMediaBottomRow) {
            ConfigManager.toggleBoolean(Defines.hideQuickSendMediaBottom);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.hideQuickSendMediaBottom));
            }
        } else if (position == customQuickMessageRow) {
            setCustomQuickMessage();
            listAdapter.notifyItemChanged(position, PARTIAL);
        } else if (position == scrollableChatPreviewRow) {
            ConfigManager.toggleBoolean(Defines.scrollableChatPreview);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.scrollableChatPreview));
            }
        } else if (position == showTabsOnForwardRow) {
            ConfigManager.toggleBoolean(Defines.showTabsOnForward);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.showTabsOnForward));
            }
        } else if (position == disableStickersAutoReorderRow) {
            ConfigManager.putBoolean(Defines.disableStickersAutoReorder, !ConfigManager.getBooleanOrDefault(Defines.disableStickersAutoReorder, true));
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableStickersAutoReorder));
            }
        } else if (position == disablePreviewVideoSoundShortcutRow) {
            ConfigManager.toggleBoolean(Defines.disablePreviewVideoSoundShortcut);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disablePreviewVideoSoundShortcut));
            }
        } else if (position == quickToggleAnonymous) {
            ConfigManager.toggleBoolean(Defines.quickToggleAnonymous);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.quickToggleAnonymous));
            }

            AlertDialog restart = new AlertDialog(getContext(), 0);
            restart.setTitle(LocaleController.getString("AppName", R.string.AppName));
            restart.setMessage(LocaleController.getString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect));
            restart.setPositiveButton(LocaleController.getString("OK", R.string.OK), (__, ___) -> {
                ProcessPhoenix.triggerRebirth(getContext(), new Intent(getContext(), LaunchActivity.class));
            });
            restart.show();
        } else if (position == markdownParserRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add("Nullgram");
            arrayList.add("Telegram");
            boolean oldParser = ConfigManager.getBooleanOrDefault(Defines.newMarkdownParser, true);
            PopupBuilder.show(arrayList, LocaleController.getString("MarkdownParser", R.string.MarkdownParser), ConfigManager.getBooleanOrDefault(Defines.newMarkdownParser, true) ? 0 : 1, getParentActivity(), view, i -> {
                ConfigManager.putBoolean(Defines.newMarkdownParser, (i == 0));
                listAdapter.notifyItemChanged(markdownParserRow, PARTIAL);
                if (oldParser != ConfigManager.getBooleanOrDefault(Defines.newMarkdownParser, true)) {
                    if (oldParser) {
                        listAdapter.notifyItemRemoved(markdownParseLinksRow);
                        updateRows();
                    } else {
                        updateRows();
                        listAdapter.notifyItemInserted(markdownParseLinksRow);
                    }
                    listAdapter.notifyItemChanged(markdown2Row, PARTIAL);
                }
            });
        } else if (position == markdownParseLinksRow) {
            ConfigManager.putBoolean(Defines.markdownParseLinks, !ConfigManager.getBooleanOrDefault(Defines.markdownParseLinks, true));
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrDefault(Defines.markdownParseLinks, true));
            }
            listAdapter.notifyItemChanged(markdown2Row);
        } else if (position == markdownDisableRow) {
            ConfigManager.toggleBoolean(Defines.markdownDisabled);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.markdownDisabled));
            }
        }

    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        return false;
    }

    @Override
    protected String getKey() {
        return "c";
    }


    @Override
    protected void updateRows() {
        super.updateRows();

        stickerSizeHeaderRow = addRow();
        stickerSizeRow = addRow("stickerSize");
        stickerSize2Row = addRow();

        chatRow = addRow();
        ignoreBlockedUserMessagesRow = addRow("ignoreBlockedUserMessages");
        hideGroupStickerRow = addRow("hideGroupSticker");
        disablePremiumStickerRow = addRow("disablePremiumSticker");
        messageMenuRow = addRow();
        if (ConfigManager.getBooleanOrFalse(Defines.showHiddenSettings)) {
            allowScreenshotOnNoForwardChatRow = addRow("allowScreenshotOnNoForwardChat");
        }
        labelChannelUserRow = addRow("labelChannelUser");
        displaySpoilerDirectlyRow = addRow("displaySpoilerDirectly");
        disableJumpToNextChannelRow = addRow("disableJumpToNextChannel");
        disableGreetingStickerRow = addRow("disableGreetingSticker");
        disableTrendingStickerRow = addRow("disableTrendingSticker");
        disablePreviewVideoSoundShortcutRow = addRow("disablePreviewVideoSoundShortcut");
        customDoubleClickTapRow = addRow("customDoubleClickTap");
        confirmToSendMediaMessagesRow = addRow("confirmToSendMediaMessages");
        maxRecentStickerRow = addRow("maxRecentSticker");
        unreadBadgeOnBackButtonRow = addRow("unreadBadgeOnBackButton");
        ignoreReactionMentionRow = addRow("ignoreReactionMention");
        showForwardDateRow = addRow("showForwardDate");
        hideTimeForStickerRow = addRow("hideTimeForSticker");
        showMessageIDRow = addRow("showMessageID");
        quickToggleAnonymous = addRow("quickToggleAnonymous");
        hideQuickSendMediaBottomRow = addRow("hideQuickSendMediaBottom");
        customQuickMessageRow = addRow("customQuickMessage");
        scrollableChatPreviewRow = addRow("scrollableChatPreview");
        showTabsOnForwardRow = addRow("showTabsOnForward");
        disableStickersAutoReorderRow = addRow("disableStickersAutoReorder");
        chat2Row = addRow();
        markdownRow = addRow();
        markdownDisableRow = addRow("markdownDisabled");
        markdownParserRow = addRow("markdownParser");
        markdownParseLinksRow = ConfigManager.getBooleanOrFalse(Defines.newMarkdownParser) ? addRow("markdownParseLinks") : -1;
        markdown2Row = addRow();

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case TYPE_SHADOW: {
                    if (position == chat2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == stickerSizeRow) {
                        textCell.setTextAndValue(LocaleController.getString("StickerSize", R.string.StickerSize),
                            String.valueOf(Math.round(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f))), payload, true);
                    } else if (position == messageMenuRow) {
                        textCell.setText(LocaleController.getString("MessageMenu", R.string.MessageMenu), false);
                    } else if (position == maxRecentStickerRow) {
                        textCell.setTextAndValue(LocaleController.getString("maxRecentSticker", R.string.maxRecentSticker), String.valueOf(ConfigManager.getIntOrDefault(Defines.maxRecentSticker, 30)), payload, true);

                    } else if (position == customDoubleClickTapRow) {
                        String value;
                        switch (ConfigManager.getIntOrDefault(Defines.doubleTab, Defines.doubleTabReaction)) {
                            case Defines.doubleTabNone:
                                value = LocaleController.getString("Disable", R.string.Disable);
                                break;
                            case Defines.doubleTabReaction:
                                value = LocaleController.getString("Reactions", R.string.Reactions);
                                break;
                            case Defines.doubleTabReply:
                                value = LocaleController.getString("Reply", R.string.Reply);
                                break;
                            case Defines.doubleTabEdit:
                                value = LocaleController.getString("Edit", R.string.Edit);
                                break;
                            case Defines.doubleTabSaveMessages:
                                value = LocaleController.getString("saveMessages", R.string.saveMessages);
                                break;
                            case Defines.doubleTabRepeat:
                                value = LocaleController.getString("Repeat", R.string.Repeat);
                                break;
                            case Defines.doubleTabTranslate:
                                value = LocaleController.getString("TranslateMessage", R.string.TranslateMessage);
                                break;
                            default:
                                value = LocaleController.getString("Reactions", R.string.Reactions);
                        }
                        textCell.setTextAndValue(LocaleController.getString("customDoubleTap", R.string.customDoubleTap), value, payload, true);
                    } else if (position == customQuickMessageRow) {
                        textCell.setText(LocaleController.getString("setCustomQuickMessage", R.string.setCustomQuickMessage), true);
                    } else if (position == markdownParserRow) {
                        textCell.setTextAndValue(LocaleController.getString("MarkdownParser", R.string.MarkdownParser), ConfigManager.getBooleanOrDefault(Defines.newMarkdownParser, true) ? "Nullgram" : "Telegram", payload, position + 1 != markdown2Row);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ignoreBlockedUserMessagesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ignoreBlockedUser", R.string.ignoreBlockedUser), ConfigManager.getBooleanOrFalse(Defines.ignoreBlockedUser), true);
                    } else if (position == hideGroupStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideGroupSticker", R.string.hideGroupSticker), ConfigManager.getBooleanOrFalse(Defines.hideGroupSticker), true);
                    } else if (position == disablePremiumStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disablePremiumSticker", R.string.disablePremiumSticker), ConfigManager.getBooleanOrFalse(Defines.disablePremiumSticker), true);
                    } else if (position == allowScreenshotOnNoForwardChatRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("allowScreenshotOnNoForwardChat", R.string.allowScreenshotOnNoForwardChat), LocaleController.getString("allowScreenshotOnNoForwardChatWarning", R.string.allowScreenshotOnNoForwardChatWarning), ConfigManager.getBooleanOrFalse(Defines.allowScreenshotOnNoForwardChat), true, true);
                    } else if (position == labelChannelUserRow) {
                        if (ConfigManager.getBooleanOrFalse(Defines.channelAlias)) {
                            textCell.setEnabled(false, null);
                        }
                        textCell.setTextAndValueAndCheck(LocaleController.getString("labelChannelUser", R.string.labelChannelUser), LocaleController.getString("labelChannelUser", R.string.labelChannelUserDetails), ConfigManager.getBooleanOrFalse(Defines.labelChannelUser), true, true);
                    } else if (position == displaySpoilerDirectlyRow) {
                        textCell.setTextAndCheck(LocaleController.getString("displaySpoilerDirectly", R.string.displaySpoilerDirectly), ConfigManager.getBooleanOrFalse(Defines.displaySpoilerMsgDirectly), true);
                    } else if (position == disableJumpToNextChannelRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableJumpToNextChannel", R.string.disableJumpToNextChannel), ConfigManager.getBooleanOrFalse(Defines.disableJumpToNextChannel), true);
                    } else if (position == disableGreetingStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableGreetingSticker", R.string.disableGreetingSticker), ConfigManager.getBooleanOrFalse(Defines.disableGreetingSticker), true);
                    } else if (position == disableTrendingStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableTrendingSticker", R.string.disableTrendingSticker), ConfigManager.getBooleanOrFalse(Defines.disableTrendingSticker), true);
                    } else if (position == confirmToSendMediaMessagesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("confirmToSendMediaMessages", R.string.confirmToSendMediaMessages), ConfigManager.getBooleanOrFalse(Defines.confirmToSendMediaMessages), true);
                    } else if (position == unreadBadgeOnBackButtonRow) {
                        textCell.setTextAndCheck(LocaleController.getString("unreadBadgeOnBackButton", R.string.unreadBadgeOnBackButton), ConfigManager.getBooleanOrFalse(Defines.unreadBadgeOnBackButton), true);
                    } else if (position == ignoreReactionMentionRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("ignoreReactionMention", R.string.ignoreReactionMention), LocaleController.getString("ignoreReactionMentionInfo", R.string.ignoreReactionMentionInfo), ConfigManager.getBooleanOrFalse(Defines.ignoreReactionMention), true, true);
                    } else if (position == showForwardDateRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showForwardDate", R.string.showForwardDate), ConfigManager.getBooleanOrFalse(Defines.dateOfForwardedMsg), true);
                    } else if (position == hideTimeForStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showForwardName", R.string.hideTimeForSticker), ConfigManager.getBooleanOrFalse(Defines.hideTimeForSticker), true);
                    } else if (position == showMessageIDRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showMessageID", R.string.showMessageID), ConfigManager.getBooleanOrFalse(Defines.showMessageID), true);
                    } else if (position == hideQuickSendMediaBottomRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableQuickSendMediaBottom", R.string.DisableQuickSendMediaBottom),
                            ConfigManager.getBooleanOrFalse(Defines.hideQuickSendMediaBottom), true);
                    } else if (position == scrollableChatPreviewRow) {
                        textCell.setTextAndCheck(LocaleController.getString("scrollableChatPreview", R.string.scrollableChatPreview), ConfigManager.getBooleanOrFalse(Defines.scrollableChatPreview), true);
                    } else if (position == showTabsOnForwardRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showTabsOnForward", R.string.showTabsOnForward), ConfigManager.getBooleanOrFalse(Defines.showTabsOnForward), true);
                    } else if (position == disablePreviewVideoSoundShortcutRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("disablePreviewVideoSoundShortcut", R.string.disablePreviewVideoSoundShortcut), LocaleController.getString("disablePreviewVideoSoundShortcutNotice", R.string.disablePreviewVideoSoundShortcutNotice), ConfigManager.getBooleanOrFalse(Defines.disablePreviewVideoSoundShortcut), true, true);
                    } else if (position == quickToggleAnonymous) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("quickToggleAnonymous", R.string.quickToggleAnonymous), LocaleController.getString("quickToggleAnonymousNotice", R.string.quickToggleAnonymousNotice), ConfigManager.getBooleanOrFalse(Defines.quickToggleAnonymous), true, true);
                    } else if (position == disableStickersAutoReorderRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableStickersAutoReorder", R.string.disableStickersAutoReorder),
                            ConfigManager.getBooleanOrDefault(Defines.disableStickersAutoReorder, true), true);
                    } else if (position == markdownParseLinksRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MarkdownParseLinks", R.string.MarkdownParseLinks), ConfigManager.getBooleanOrDefault(Defines.markdownParseLinks, true), false);
                    } else if (position == markdownDisableRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MarkdownDisableByDefault", R.string.MarkdownDisableByDefault), ConfigManager.getBooleanOrFalse(Defines.markdownDisabled), true);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == chatRow) {
                        headerCell.setText(LocaleController.getString("Chat", R.string.Chat));
                    } else if (position == stickerSizeHeaderRow) {
                        headerCell.setText(LocaleController.getString("StickerSize", R.string.StickerSize));
                    } else if (position == markdownRow) {
                        headerCell.setText(LocaleController.getString("Markdown", R.string.Markdown));
                    }
                    break;
                }
                case TYPE_NOTIFICATION_CHECK: {
                    NotificationsCheckCell textCell = (NotificationsCheckCell) holder.itemView;
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == markdown2Row) {
                        cell.getTextView().setMovementMethod(null);
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(TextUtils.expandTemplate(EntitiesHelper.parseMarkdown(ConfigManager.getBooleanOrDefault(Defines.newMarkdownParser, true) && ConfigManager.getBooleanOrDefault(Defines.markdownParseLinks, true) ? LocaleController.getString("MarkdownAbout", R.string.MarkdownAbout) : LocaleController.getString("MarkdownAbout2", R.string.MarkdownAbout2)), "**", "__", "~~", "`", "||", "[", "](", ")"));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3 || type == 6 || type == 5;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case TYPE_SHADOW:
                    view = new ShadowSectionCell(mContext);
                    break;
                case TYPE_SETTINGS:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_CHECK:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_HEADER:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_NOTIFICATION_CHECK:
                    view = new NotificationsCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_DETAIL_SETTINGS:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_INFO_PRIVACY:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case TYPE_STICKER_SIZE:
                    view = stickerSizeCell = new StickerSizeCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == chat2Row || position == stickerSize2Row) {
                return TYPE_SHADOW;
            } else if (position == messageMenuRow || position == customDoubleClickTapRow || position == maxRecentStickerRow || position == customQuickMessageRow || position == markdownParserRow) {
                return TYPE_SETTINGS;
            } else if (position == chatRow || position == stickerSizeHeaderRow || position == markdownRow) {
                return TYPE_HEADER;
            } else if (position == stickerSizeRow) {
                return TYPE_STICKER_SIZE;
            } else if ((position > chatRow && position < chat2Row) || (position > markdownRow && position < markdown2Row) || (position > stickerSizeRow && position < stickerSize2Row)) {
                return TYPE_CHECK;
            } else if (position == markdown2Row) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_CHECK;
        }
    }

    private void showMessageMenuAlert() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("MessageMenu", R.string.MessageMenu));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout linearLayoutInviteContainer = new LinearLayout(context);
        linearLayoutInviteContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutInviteContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int count = 7;
        for (int a = 0; a < count; a++) {
            TextCheckCell textCell = new TextCheckCell(context);
            switch (a) {
                case 0: {
                    textCell.setTextAndCheck(LocaleController.getString("DeleteDownloadedFile", R.string.DeleteDownloadedFile), ConfigManager.getBooleanOrFalse(Defines.showDeleteDownloadFiles), false);
                    break;
                }
                case 1: {
                    textCell.setTextAndCheck(LocaleController.getString("NoQuoteForward", R.string.NoQuoteForward), ConfigManager.getBooleanOrDefault(Defines.showNoQuoteForward, true), false);
                    break;
                }
                case 2: {
                    textCell.setTextAndCheck(LocaleController.getString("saveMessages", R.string.saveMessages), ConfigManager.getBooleanOrFalse(Defines.showSaveMessages), false);
                    break;
                }
                case 3: {
                    textCell.setTextAndCheck(LocaleController.getString("Repeat", R.string.Repeat), ConfigManager.getBooleanOrDefault(Defines.showRepeat, true), false);
                    break;
                }
                case 4: {
                    textCell.setTextAndCheck(LocaleController.getString("ViewHistory", R.string.ViewHistory), ConfigManager.getBooleanOrFalse(Defines.showViewHistory), false);
                    break;
                }
                case 5: {
                    textCell.setTextAndCheck(LocaleController.getString("MessageDetails", R.string.MessageDetails), ConfigManager.getBooleanOrFalse(Defines.showMessagesDetail), false);
                    break;
                }
                case 6: {
                    textCell.setTextAndCheck(LocaleController.getString("CopyPhoto", R.string.CopyPhoto), ConfigManager.getBooleanOrFalse(Defines.showCopyPhoto), false);
                    break;
                }
            }
            textCell.setTag(a);
            textCell.setBackground(Theme.getSelectorDrawable(false));
            linearLayoutInviteContainer.addView(textCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textCell.setOnClickListener(v2 -> {
                Integer tag = (Integer) v2.getTag();
                switch (tag) {
                    case 0: {
                        ConfigManager.toggleBoolean(Defines.showDeleteDownloadFiles);
                        textCell.setChecked(ConfigManager.getBooleanOrFalse(Defines.showDeleteDownloadFiles));
                        break;
                    }
                    case 1: {
                        ConfigManager.putBoolean(Defines.showNoQuoteForward, !ConfigManager.getBooleanOrDefault(Defines.showNoQuoteForward, true));
                        textCell.setChecked(ConfigManager.getBooleanOrDefault(Defines.showNoQuoteForward, true));
                        break;
                    }
                    case 2: {
                        ConfigManager.toggleBoolean(Defines.showSaveMessages);
                        textCell.setChecked(ConfigManager.getBooleanOrFalse(Defines.showSaveMessages));
                        break;
                    }
                    case 3: {
                        ConfigManager.putBoolean(Defines.showRepeat, !ConfigManager.getBooleanOrDefault(Defines.showRepeat, true));
                        textCell.setChecked(ConfigManager.getBooleanOrDefault(Defines.showRepeat, true));
                        break;
                    }
                    case 4: {
                        ConfigManager.toggleBoolean(Defines.showViewHistory);
                        textCell.setChecked(ConfigManager.getBooleanOrFalse(Defines.showViewHistory));
                        break;
                    }
                    case 5: {
                        ConfigManager.toggleBoolean(Defines.showMessagesDetail);
                        textCell.setChecked(ConfigManager.getBooleanOrFalse(Defines.showMessagesDetail));
                        break;
                    }
                    case 6: {
                        ConfigManager.toggleBoolean(Defines.showCopyPhoto);
                        textCell.setChecked(ConfigManager.getBooleanOrFalse(Defines.showCopyPhoto));
                        break;
                    }
                }
            });
        }
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.setView(linearLayout);
        showDialog(builder.create());
    }

    @SuppressLint("SetTextI18n")
    private void setMaxRecentSticker(View view, int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("setMaxRecentSticker", R.string.setMaxRecentSticker));

        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        editText.setHintText(LocaleController.getString("Number", R.string.Number));
        editText.setText(ConfigManager.getIntOrDefault(Defines.maxRecentSticker, 30) + "");
        editText.setHeaderHintColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        editText.setSingleLine(true);
        editText.setFocusable(true);
        editText.setTransformHintToHeader(true);
        editText.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setBackgroundDrawable(null);
        editText.requestFocus();
        editText.setPadding(0, 0, 0, 0);
        builder.setView(editText);

        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            if (editText.getText().toString().trim().equals("")) {
                ConfigManager.putInt(Defines.maxRecentSticker, 20);
            } else {
                if (!NumberUtils.isInteger(editText.getText().toString())) {
                    AndroidUtilities.shakeView(view);
                    AlertUtil.showToast(LocaleController.getString("notANumber", R.string.notANumber));
                } else {
                    final int targetNum = Integer.parseInt(editText.getText().toString().trim());
                    if (targetNum > 150 || targetNum < 20)
                        AlertUtil.showToast(LocaleController.getString("numberInvalid", R.string.numberInvalid));
                    else
                        ConfigManager.putInt(Defines.maxRecentSticker, Integer.parseInt(editText.getText().toString()));
                }
            }
            listAdapter.notifyItemChanged(pos, PARTIAL);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show().setOnShowListener(dialog -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) editText.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
            }
            layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
            layoutParams.height = AndroidUtilities.dp(36);
            editText.setLayoutParams(layoutParams);
        }
        editText.setSelection(0, editText.getText().length());
    }

    @SuppressLint("SetTextI18n")
    private void setCustomQuickMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("setCustomQuickMessage", R.string.setCustomQuickMessage));

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditTextBoldCursor setDisplayNameEditText = new EditTextBoldCursor(getParentActivity()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };

        setDisplayNameEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        setDisplayNameEditText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        setDisplayNameEditText.setHintText(LocaleController.getString("Name", R.string.Name));
        setDisplayNameEditText.setText(ConfigManager.getStringOrDefault(Defines.customQuickMessageDisplayName, ""));
        setDisplayNameEditText.setHeaderHintColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        setDisplayNameEditText.setSingleLine(true);
        setDisplayNameEditText.setFocusable(true);
        setDisplayNameEditText.setTransformHintToHeader(true);
        setDisplayNameEditText.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        setDisplayNameEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        setDisplayNameEditText.setBackgroundDrawable(null);
        setDisplayNameEditText.setPadding(AndroidUtilities.dp(36), AndroidUtilities.dp(16), AndroidUtilities.dp(36), AndroidUtilities.dp(16));
        layout.addView(setDisplayNameEditText);

        final EditTextBoldCursor setMessageEditText = new EditTextBoldCursor(getParentActivity()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };
        setMessageEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        setMessageEditText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        setMessageEditText.setHintText(LocaleController.getString("Message", R.string.Message));
        setMessageEditText.setText(ConfigManager.getStringOrDefault(Defines.customQuickMessage, ""));
        setMessageEditText.setHeaderHintColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        setMessageEditText.setSingleLine(false);
        setMessageEditText.setFocusable(true);
        setMessageEditText.setTransformHintToHeader(true);
        setMessageEditText.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        setMessageEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        setMessageEditText.setBackgroundDrawable(null);
        setMessageEditText.setPadding(AndroidUtilities.dp(36), AndroidUtilities.dp(16), AndroidUtilities.dp(36), AndroidUtilities.dp(16));
        layout.addView(setMessageEditText);

        CheckBoxCell cell = new CheckBoxCell(getParentActivity(), 1);
        cell.setBackground(Theme.getSelectorDrawable(false));
        cell.setText(LocaleController.getString("SendAsReply", R.string.SendAsReply), "", ConfigManager.getBooleanOrFalse(Defines.customQuickMsgSAR), false);
        cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
        cell.setOnClickListener(v -> {
            CheckBoxCell cell1 = (CheckBoxCell) v;
            cell1.setChecked(!cell1.isChecked(), true);
        });
        layout.addView(cell);

        builder.setView(layout);


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            if (StringUtils.isBlank(setDisplayNameEditText.getText().toString()) || StringUtils.isBlank(setMessageEditText.getText().toString())) {
                AlertUtil.showToast(LocaleController.getString("emptyInput", R.string.emptyInput));
            } else {
                ConfigManager.putString(Defines.customQuickMessageDisplayName, setDisplayNameEditText.getText().toString());
                ConfigManager.putString(Defines.customQuickMessage, setMessageEditText.getText().toString());
                ConfigManager.putBoolean(Defines.customQuickMessageEnabled, true);
                ConfigManager.putBoolean(Defines.customQuickMsgSAR, cell.isChecked());
            }
        });


        builder.setNeutralButton(LocaleController.getString("Reset", R.string.Reset), (dialogInterface, i) -> {
            ConfigManager.deleteValue(Defines.customQuickMessage);
            ConfigManager.deleteValue(Defines.customQuickMessageDisplayName);
            ConfigManager.putBoolean(Defines.customQuickMessageEnabled, false);
            ConfigManager.putBoolean(Defines.customQuickMsgSAR, false);
        });

        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show().setOnShowListener(dialog -> {
            setDisplayNameEditText.requestFocus();
            AndroidUtilities.showKeyboard(setDisplayNameEditText);
        });

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) setDisplayNameEditText.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
            }
            layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
            layoutParams.height = AndroidUtilities.dp(36);
            setDisplayNameEditText.setLayoutParams(layoutParams);
        }
        setDisplayNameEditText.setSelection(0, setDisplayNameEditText.getText().length());

        layoutParams = (ViewGroup.MarginLayoutParams) setMessageEditText.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
            }
            layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
            layoutParams.height = AndroidUtilities.dp(36);
            setDisplayNameEditText.setLayoutParams(layoutParams);
        }
        setMessageEditText.setSelection(0, setMessageEditText.getText().length());
    }

    private class StickerSizeCell extends FrameLayout {

        private final StickerSizePreviewMessagesCell messagesCell;
        private final SeekBarView sizeBar;
        private final int startStickerSize = 2;
        private final int endStickerSize = 20;

        private final TextPaint textPaint;
        private int lastWidth;

        public StickerSizeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));

            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    sizeBar.getSeekBarAccessibilityDelegate().postAccessibilityEventRunnable(StickerSizeCell.this);
                    ConfigManager.putFloat(Defines.stickerSize, startStickerSize + (endStickerSize - startStickerSize) * progress);
                    StickerSizeCell.this.invalidate();
                    if (resetItem.getVisibility() != VISIBLE) {
                        AndroidUtilities.updateViewVisibilityAnimated(resetItem, true, 0.5f, true);
                    }
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {

                }
            });
            sizeBar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));

            messagesCell = new StickerSizePreviewMessagesCell(context, parentLayout);
            messagesCell.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 53, 0, 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText(String.valueOf(Math.round(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f))), getMeasuredWidth() - AndroidUtilities.dp(39), AndroidUtilities.dp(28), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (lastWidth != width) {
                sizeBar.setProgress((ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f) - startStickerSize) / (float) (endStickerSize - startStickerSize));
                lastWidth = width;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            lastWidth = -1;
            messagesCell.invalidate();
            sizeBar.invalidate();
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityEvent(this, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityNodeInfoInternal(this, info);
        }

        @Override
        public boolean performAccessibilityAction(int action, Bundle arguments) {
            return super.performAccessibilityAction(action, arguments) || sizeBar.getSeekBarAccessibilityDelegate().performAccessibilityActionInternal(this, action, arguments);
        }
    }

}
