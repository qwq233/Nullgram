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
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;

import java.util.ArrayList;

import top.qwq2333.nullgram.config.ConfigManager;
import top.qwq2333.nullgram.ui.PopupBuilder;
import top.qwq2333.nullgram.utils.Defines;
import top.qwq2333.nullgram.utils.Log;
import ws.vinta.pangu.Pangu;

@SuppressLint("NotifyDataSetChanged")
public class ExperimentSettingActivity extends BaseActivity {


    private int experimentRow;
    private int disableFilteringRow;
    private int blockSponsorAdsRow;
    private int hideProxySponsorChannelRow;
    private int disableSendTypingRow;
    private int syntaxHighlightRow;
    private int aliasChannelRow;
    private int keepFormattingRow;
    private int enchantAudioRow;
    private int linkedUserRow;
    private int overrideChannelAliasRow;
    private int showRPCErrorRow;

    private int panguRow;
    private int enablePanguOnSendingRow;
    private int enablePanguOnReceivingRow;
    private int pangu2Row;
    private int pangu3Row;

    private int premiumRow;
    private int hidePremiumStickerAnimRow;
    private int fastSpeedUploadRow;
    private int modifyDownloadSpeedRow;
    private int premium2Row;
    private int alwaysSendWithoutSoundRow;


    private int experiment2Row;

    private boolean sensitiveEnabled;
    private final boolean sensitiveCanChange;

    private final Pangu pangu = new Pangu();

    public ExperimentSettingActivity(boolean sensitiveEnabled, boolean sensitiveCanChange) {
        this.sensitiveEnabled = sensitiveEnabled;
        this.sensitiveCanChange = sensitiveCanChange;
    }


    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == blockSponsorAdsRow) {
            ConfigManager.toggleBoolean(Defines.blockSponsorAds);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.blockSponsorAds));
            }
        } else if (position == hideProxySponsorChannelRow) {
            ConfigManager.toggleBoolean(Defines.hideProxySponsorChannel);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.hideProxySponsorChannel));
            }
        } else if (position == disableSendTypingRow) {
            ConfigManager.toggleBoolean(Defines.disableSendTyping);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableSendTyping));
            }
        } else if (position == syntaxHighlightRow) {
            ConfigManager.putBoolean(Defines.codeSyntaxHighlight, !ConfigManager.getBooleanOrDefault(Defines.codeSyntaxHighlight, true));
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrDefault(Defines.codeSyntaxHighlight, true));
            }
        }
        if (position == disableFilteringRow) {
            sensitiveEnabled = !sensitiveEnabled;
            TLRPC.TL_account_setContentSettings req = new TLRPC.TL_account_setContentSettings();
            req.sensitive_enabled = sensitiveEnabled;
            AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
            progressDialog.show();
            getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                progressDialog.dismiss();
                if (error == null) {
                    if (response instanceof TLRPC.TL_boolTrue && view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(sensitiveEnabled);
                    }
                } else {
                    AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, this, req));
                }
            }));
        } else if (position == aliasChannelRow) {
            boolean currentStatus = ConfigManager.getBooleanOrFalse(Defines.channelAlias);
            if (!currentStatus && !ConfigManager.getBooleanOrFalse(Defines.labelChannelUser)) {
                ConfigManager.putBoolean(Defines.labelChannelUser, true);
            }
            ConfigManager.toggleBoolean(Defines.channelAlias);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.channelAlias));
            }
        } else if (position == keepFormattingRow) {
            ConfigManager.toggleBoolean(Defines.keepCopyFormatting);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.keepCopyFormatting));
            }
        } else if (position == enchantAudioRow) {
            ConfigManager.toggleBoolean(Defines.enchantAudio);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.enchantAudio));
            }
        } else if (position == linkedUserRow) {
            ConfigManager.toggleBoolean(Defines.linkedUser);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.linkedUser));
            }
        } else if (position == overrideChannelAliasRow) {
            ConfigManager.toggleBoolean(Defines.overrideChannelAlias);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.overrideChannelAlias));
            }
        } else if (position == hidePremiumStickerAnimRow) {
            ConfigManager.toggleBoolean(Defines.hidePremiumStickerAnim);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.hidePremiumStickerAnim));
            }
        } else if (position == fastSpeedUploadRow) {
            ConfigManager.toggleBoolean(Defines.fastSpeedUpload);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.fastSpeedUpload));
            }
        } else if (position == modifyDownloadSpeedRow) {
            int[] speeds = new int[]{128, 256, 384, 512, 768, 1024};
            ArrayList<String> speedsStr = new ArrayList<>();
            for (int speed : speeds) {
                speedsStr.add(speed + " Kb/block");
            }
            PopupBuilder.show(speedsStr, LocaleController.getString("modifyDownloadSpeed", R.string.modifyDownloadSpeed),
                speedsStr.indexOf(ConfigManager.getIntOrDefault(Defines.modifyDownloadSpeed, 512) + " Kb/block"), getParentActivity(), view, i -> {
                    Log.i("speeds[i]: " + speeds[i]);
                    Log.i("i: " + i);
                    ConfigManager.putInt(Defines.modifyDownloadSpeed, speeds[i]);
                    listAdapter.notifyItemChanged(modifyDownloadSpeedRow, PARTIAL);
                });
        } else if (position == alwaysSendWithoutSoundRow) {
            ConfigManager.toggleBoolean(Defines.alwaysSendWithoutSound);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.alwaysSendWithoutSound));
            }
        } else if (position == showRPCErrorRow) {
            ConfigManager.toggleBoolean(Defines.showRPCError);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.showRPCError));
            }
        } else if (position == enablePanguOnSendingRow) {
            ConfigManager.toggleBoolean(Defines.enablePanguOnSending);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.enablePanguOnSending));
            }
        } else if (position == enablePanguOnReceivingRow) {
            ConfigManager.toggleBoolean(Defines.enablePanguOnReceiving);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.enablePanguOnReceiving));
            }
        }

    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        // ignore
        return false;
    }

    @Override
    protected String getKey() {
        return "e";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        experimentRow = addRow();
        if (ConfigManager.getBooleanOrFalse(Defines.showHiddenSettings)) {
            blockSponsorAdsRow = addRow("blockSponsorAds");
            hideProxySponsorChannelRow = addRow("hideProxySponsorChannel");
            disableSendTypingRow = addRow("disableSendTyping");
        }
        disableFilteringRow = sensitiveCanChange ? addRow("disableFiltering") : -1;
        syntaxHighlightRow = addRow("syntaxHighlight");
        aliasChannelRow = addRow("aliasChannel");
        keepFormattingRow = addRow("keepFormatting");
        enchantAudioRow = addRow("enchantAudio");
        linkedUserRow = addRow("linkedUser");
        alwaysSendWithoutSoundRow = addRow();
        if (ConfigManager.getBooleanOrFalse(Defines.linkedUser) && ConfigManager.getBooleanOrFalse(Defines.labelChannelUser)) {
            overrideChannelAliasRow = addRow("overrideChannelAlias");
        } else {
            overrideChannelAliasRow = -1;
        }
        var user = UserConfig.getInstance(currentAccount).getCurrentUser();
        showRPCErrorRow = user != null && user.developer() ? addRow("showRPCError") : -1;
        experiment2Row = addRow();

        panguRow = addRow();
        enablePanguOnSendingRow = addRow("enablePanguOnSending");
        enablePanguOnReceivingRow = /*addRow("enablePanguOnReceiving")*/ -1; //todo: not finished
        pangu3Row = addRow();
        pangu2Row = addRow();

        if (ConfigManager.getBooleanOrFalse(Defines.showHiddenSettings)) {
            premiumRow = addRow();
            hidePremiumStickerAnimRow = addRow("hidePremiumStickerAnim");
            fastSpeedUploadRow = addRow("fastSpeedUpload");
            modifyDownloadSpeedRow = addRow("modifyDownloadSpeed");
            premium2Row = addRow();
        }

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }


    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("Experiment", R.string.Experiment);
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
                    if (position == experiment2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == modifyDownloadSpeedRow) {
                        textCell.setTextAndValue(LocaleController.getString("modifyDownloadSpeed", R.string.modifyDownloadSpeed),
                            ConfigManager.getIntOrDefault(Defines.modifyDownloadSpeed, 128) + " Kb/block", payload, false);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == blockSponsorAdsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("blockSponsorAds", R.string.blockSponsorAds), ConfigManager.getBooleanOrFalse(Defines.blockSponsorAds),
                            true);
                    } else if (position == hideProxySponsorChannelRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideProxySponsorChannel", R.string.hideProxySponsorChannel),
                            ConfigManager.getBooleanOrFalse(Defines.hideProxySponsorChannel), true);
                    } else if (position == disableSendTypingRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableSendTyping", R.string.disableSendTyping),
                            ConfigManager.getBooleanOrFalse(Defines.disableSendTyping), true);
                    } else if (position == syntaxHighlightRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("codeSyntaxHighlight", R.string.codeSyntaxHighlight),
                            LocaleController.getString("codeSyntaxHighlightDetails", R.string.codeSyntaxHighlightDetails),
                            ConfigManager.getBooleanOrFalse(Defines.codeSyntaxHighlight), true, true);
                    }
                    if (position == disableFilteringRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("SensitiveDisableFiltering", R.string.SensitiveDisableFiltering),
                            LocaleController.getString("SensitiveAbout", R.string.SensitiveAbout), sensitiveEnabled, true, true);
                        textCell.setEnabled(sensitiveCanChange, null);
                    } else if (position == aliasChannelRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("channelAlias", R.string.channelAlias),
                            LocaleController.getString("channelAliasDetails", R.string.channelAliasDetails), ConfigManager.getBooleanOrFalse(Defines.channelAlias), true, true);
                    } else if (position == keepFormattingRow) {
                        textCell.setTextAndCheck(LocaleController.getString("keepFormatting", R.string.keepFormatting), ConfigManager.getBooleanOrFalse(Defines.keepCopyFormatting),
                            true);
                    } else if (position == enchantAudioRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("enchantAudioRow", R.string.enchantAudio),
                            LocaleController.getString("enchantAudioDetails", R.string.enchantAudioDetails), ConfigManager.getBooleanOrFalse(Defines.enchantAudio), true, true);
                    } else if (position == linkedUserRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("linkUser", R.string.linkUser),
                            LocaleController.getString("linkUserDetails", R.string.linkUserDetails), ConfigManager.getBooleanOrFalse(Defines.linkedUser), true, true);
                    } else if (position == overrideChannelAliasRow) {
                        textCell.setTextAndValueAndCheck(
                            ConfigManager.getBooleanOrFalse(Defines.channelAlias) ?
                                LocaleController.getString("overrideChannelAlias", R.string.overrideChannelAlias) :
                                LocaleController.getString("labelChannelInChat", R.string.labelChannelInChat),
                            LocaleController.getString("overrideChannelAliasDetails", R.string.overrideChannelAliasDetails),
                            ConfigManager.getBooleanOrFalse(Defines.overrideChannelAlias), true, true);
                    } else if (position == hidePremiumStickerAnimRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hidePremiumStickerAnim", R.string.hidePremiumStickerAnim),
                            ConfigManager.getBooleanOrFalse(Defines.hidePremiumStickerAnim), true);
                    } else if (position == fastSpeedUploadRow) {
                        textCell.setTextAndCheck(LocaleController.getString("fastSpeedUpload", R.string.fastSpeedUpload), ConfigManager.getBooleanOrFalse(Defines.fastSpeedUpload),
                            true);
                    } else if (position == alwaysSendWithoutSoundRow) {
                        textCell.setTextAndCheck(LocaleController.getString("alwaysSendWithoutSound", R.string.alwaysSendWithoutSound),
                            ConfigManager.getBooleanOrFalse(Defines.alwaysSendWithoutSound), true);
                    } else if (position == showRPCErrorRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showRPCError", R.string.showRPCError), ConfigManager.getBooleanOrFalse(Defines.showRPCError), true);
                    } else if (position == enablePanguOnSendingRow) {
                        textCell.setTextAndCheck(LocaleController.getString("enablePanguOnSending", R.string.enablePanguOnSending), ConfigManager.getBooleanOrFalse(Defines.enablePanguOnSending),
                            true);
                    } else if (position == enablePanguOnReceivingRow) {
                        textCell.setTextAndCheck(LocaleController.getString("enablePanguOnReceiving", R.string.enablePanguOnReceiving), ConfigManager.getBooleanOrFalse(Defines.enablePanguOnReceiving), true);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == experimentRow) {
                        headerCell.setText(LocaleController.getString("Experiment", R.string.Experiment));
                    } else if (position == premiumRow) {
                        headerCell.setText(LocaleController.getString("Premium", R.string.premium));
                    } else if (position == panguRow) {
                        headerCell.setText(LocaleController.getString("pangu", R.string.pangu));
                    }
                    break;
                }
                case TYPE_NOTIFICATION_CHECK: {
                    NotificationsCheckCell textCell = (NotificationsCheckCell) holder.itemView;
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == pangu3Row) {
                        cell.getTextView().setMovementMethod(null);
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("panguInfo", R.string.panguInfo));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == experiment2Row || position == premium2Row || position == pangu2Row) {
                return TYPE_SHADOW;
            } else if (position == modifyDownloadSpeedRow) {
                return TYPE_SETTINGS;
            } else if (position == experimentRow || position == premiumRow || position == panguRow) {
                return TYPE_HEADER;
            } else if (position == pangu3Row) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_CHECK;
        }
    }
}
