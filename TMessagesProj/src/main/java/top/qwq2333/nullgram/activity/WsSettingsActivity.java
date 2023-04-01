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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.util.ArrayList;

import top.qwq2333.nullgram.helpers.WebSocketHelper;
import top.qwq2333.nullgram.ui.PopupBuilder;


public class WsSettingsActivity extends BaseActivity {

    private int descriptionRow;
    private int settingsRow;
    private int enableTLSRow;
    private int localProxyRow;
    private int enableDoHRow;
    private int switchBackendRow;
    private final SharedConfig.ProxyInfo currentProxyInfo;

    public WsSettingsActivity(SharedConfig.ProxyInfo proxyInfo) {
        super();
        currentProxyInfo = proxyInfo;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableTLSRow) {
            WebSocketHelper.toggleWsEnableTLS();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(WebSocketHelper.wsEnableTLS);
            }
            WebSocketHelper.wsReloadConfig();
        } else if (position == localProxyRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add(LocaleController.getString("UseProxySocks5", R.string.UseProxySocks5));
            arrayList.add(LocaleController.getString("UseProxyTelegram", R.string.UseProxyTelegram));
            PopupBuilder.show(arrayList, LocaleController.getString("WsLocalProxy", R.string.WsLocalProxy), WebSocketHelper.wsUseMTP ? 1 : 0, getParentActivity(), view, i -> {
                WebSocketHelper.setWsUseMTP(i == 1);
                listAdapter.notifyItemChanged(localProxyRow);
                WebSocketHelper.wsReloadConfig();
            });
        } else if (position == enableDoHRow) {
            WebSocketHelper.toggleWsUseDoH();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(WebSocketHelper.wsUseDoH);
            }
            WebSocketHelper.wsReloadConfig();
        } else if (position == switchBackendRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add("Nekogram");
            arrayList.add("Nekogram X");
            PopupBuilder.show(arrayList, LocaleController.getString("SwitchBackend", R.string.SwitchBackend),
                WebSocketHelper.backend == 0 ? 0 : 1, getParentActivity(), view, i -> {
                    WebSocketHelper.setBackend(i == 0 ? 0 : 1);
                    listAdapter.notifyItemChanged(descriptionRow);
                    listAdapter.notifyItemChanged(switchBackendRow);
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                });
        }
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        return false;
    }

    @Override
    protected String getKey() {
        return null;
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return currentProxyInfo.address;
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

        settingsRow = rowCount++;
        enableTLSRow = rowCount++;
        localProxyRow = rowCount++;
        enableDoHRow = rowCount++;
        switchBackendRow = -1;
        descriptionRow = rowCount++;
    }

    @Override
    protected boolean hasWhiteActionBar() {
        return false;
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == localProxyRow) {
                        String value = WebSocketHelper.wsUseMTP ? LocaleController.getString("UseProxyTelegram", R.string.UseProxyTelegram) : LocaleController.getString("UseProxySocks5", R.string.UseProxySocks5);
                        textCell.setTextAndValue(LocaleController.getString("WsLocalProxy", R.string.WsLocalProxy), value, true, true);
                    } else if (position == switchBackendRow) {
                        String value = WebSocketHelper.backend == 0 ? "Nekogram" : "Nekogram X";
                        textCell.setTextAndValue(LocaleController.getString("Switch Backend", R.string.SwitchBackend), value, true, true);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == enableTLSRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableTls", R.string.WsEnableTls), WebSocketHelper.wsEnableTLS, true);
                    } else if (position == enableDoHRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableDoh", R.string.WsEnableDoh), WebSocketHelper.wsUseDoH, false);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == settingsRow) {
                        headerCell.setText(LocaleController.getString("Settings", R.string.Settings));
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setText(WebSocketHelper.backend == 0 ? LocaleController.getString("NekogramWsDescription",
                        R.string.NekogramWsDescription) : LocaleController.getString("NekogramXWsDescription", R.string.NekogramXWsDescription));
                    cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == descriptionRow) {
                return TYPE_INFO_PRIVACY;
            } else if (position == settingsRow) {
                return TYPE_HEADER;
            } else if (position == enableTLSRow || position == enableDoHRow) {
                return TYPE_CHECK;
            } else if (position == localProxyRow || position == switchBackendRow) {
                return TYPE_SETTINGS;
            }
            return TYPE_SETTINGS;
        }
    }

}
