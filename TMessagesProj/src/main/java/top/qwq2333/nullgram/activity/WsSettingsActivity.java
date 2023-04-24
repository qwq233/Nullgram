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
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import top.qwq2333.nullgram.config.ConfigManager;
import top.qwq2333.nullgram.helpers.WebSocketHelper;
import top.qwq2333.nullgram.helpers.WebSocketHelper.WsProvider;
import top.qwq2333.nullgram.ui.PopupBuilder;
import top.qwq2333.nullgram.utils.Defines;
import top.qwq2333.nullgram.utils.Log;


public class WsSettingsActivity extends BaseActivity {

    private int settingsRow;
    private int providerRow;
    private int enableTLSRow;
    private int descriptionRow;
    private ActionBarMenuItem helpItem;
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
        } else if (position == providerRow) {
            var providers = WebSocketHelper.getProviders();
            var names = providers.first;
            var types = providers.second;
            PopupBuilder.show(names, LocaleController.getString("WsProvider", R.string.WsProvider),
                types.indexOf(WebSocketHelper.getCurrentProvider()),
                getParentActivity(), view,
                i -> {
                    if (types.get(i).equals(WsProvider.Custom)) {
                        Context context = getParentActivity();
                        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
                        builder.setTitle(LocaleController.getString("WsProvider", R.string.WsProvider));

                        LinearLayout ll = new LinearLayout(context);
                        ll.setOrientation(LinearLayout.VERTICAL);

                        final EditTextBoldCursor editText = new EditTextBoldCursor(context) {
                            @Override
                            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
                            }
                        };
                        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                        editText.setText("");
                        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
                        editText.setHintText(LocaleController.getString("WsProvider", R.string.WsProvider));
                        editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
                        editText.setSingleLine(true);
                        editText.setFocusable(true);
                        editText.setTransformHintToHeader(true);
                        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider),
                            Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider),
                            Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
                        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                        editText.setBackground(null);
                        editText.requestFocus();
                        editText.setPadding(0, 0, 0, 0);
                        ll.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24, 0, 24, 0));

                        builder.setView(ll);
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i2) -> {
                            ConfigManager.putString(Defines.wsServerHost, editText.getText().toString());
                            WebSocketHelper.setCurrentProvider(types.get(i));
                            WebSocketHelper.wsReloadConfig();
                            listAdapter.notifyItemChanged(providerRow, PARTIAL);
                            listAdapter.notifyItemChanged(descriptionRow);
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

                        AlertDialog alertDialog = builder.create();
                        alertDialog.setOnShowListener(dialog -> {
                            editText.requestFocus();
                            AndroidUtilities.showKeyboard(editText);
                        });
                        showDialog(alertDialog);
                        editText.setSelection(0, editText.getText().length());
                    } else {
                        WebSocketHelper.setCurrentProvider(types.get(i));
                        WebSocketHelper.wsReloadConfig();
                        listAdapter.notifyItemChanged(providerRow, PARTIAL);
                        listAdapter.notifyItemChanged(descriptionRow);
                    }
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
    public View createView(Context context) {
        View view = super.createView(context);

        ActionBarMenu menu = actionBar.createMenu();
        helpItem = menu.addItem(0, R.drawable.msg_emoji_question);
        helpItem.setContentDescription(LocaleController.getString("WsGetHelp", R.string.WsGetHelp));
        helpItem.setVisibility(View.VISIBLE);
        helpItem.setTag(null);
        helpItem.setOnClickListener(v -> {
            BulletinFactory bulletinFactory = BulletinFactory.of(this);
            bulletinFactory.createSimpleBulletin(R.raw.fire_on, LocaleController.getString("WsGetHelp", R.string.WsGetHelp),
                LocaleController.getString("LearnMore", R.string.LearnMore), () -> Browser.openUrl(getParentActivity(),
                    "https://github.com/qwq233/Nullgram/blob/master/docs/wsproxy/README.md")).show();
        });

        return view;
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

        settingsRow = rowCount++;
        providerRow = rowCount++;
        enableTLSRow = rowCount++;
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
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == providerRow) {
                        String value;
                        Log.d("isCustomProvider: " + WebSocketHelper.getCurrentProvider().equals(WsProvider.Custom));
                        if (WebSocketHelper.getCurrentProvider().equals(WsProvider.Custom)) {
                            value = WebSocketHelper.getCurrentProvider().getHost();
                        } else {
                            value = WebSocketHelper.getCurrentProvider().name();
                        }
                        textCell.setTextAndValue(LocaleController.getString("WsProvider", R.string.WsProvider), value, partial, true);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == enableTLSRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableTls", R.string.WsEnableTls), WebSocketHelper.wsEnableTLS, true);
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
                    String value = null;
                    if (WebSocketHelper.getCurrentProvider().equals(WsProvider.Nekogram)) {
                        value = LocaleController.getString("NekogramWsDescription", R.string.NekogramWsDescription);
                    } else if (WebSocketHelper.getCurrentProvider().equals(WsProvider.Custom)) {
                        value = LocaleController.getString("WsCustomDescription", R.string.WsCustomDescription);
                    }
                    cell.setText(value);
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
            } else if (position == enableTLSRow) {
                return TYPE_CHECK;
            } else if (position == providerRow) {
                return TYPE_SETTINGS;
            }
            return TYPE_SETTINGS;
        }
    }

}
