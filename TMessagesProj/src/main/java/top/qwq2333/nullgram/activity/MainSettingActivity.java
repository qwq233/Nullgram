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
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.jakewharton.processphoenix.ProcessPhoenix;

import org.json.JSONException;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.DocumentSelectActivity;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.util.ArrayList;

import top.qwq2333.nullgram.config.ConfigManager;
import top.qwq2333.nullgram.helpers.PasscodeHelper;
import top.qwq2333.nullgram.utils.AlertUtil;
import top.qwq2333.nullgram.utils.Defines;
import top.qwq2333.nullgram.utils.FileUtils;
import top.qwq2333.nullgram.utils.JsonUtils;
import top.qwq2333.nullgram.utils.Log;
import top.qwq2333.nullgram.utils.PermissionUtils;
import top.qwq2333.nullgram.utils.ShareUtil;

@SuppressLint("NotifyDataSetChanged")
public class MainSettingActivity extends BaseActivity {

    private int categoriesRow;
    private int generalRow;
    private int chatRow;
    private int experimentRow;
    private int categories2Row;

    private int aboutRow;
    private int channelRow;
    private int websiteRow;
    private int sourceCodeRow;
    private int licenseRow;
    private int about2Row;
    private int updateRow;
    private int passcodeRow;
    private int pressCount = 0;
    private Context context;

    private boolean sensitiveEnabled;
    private boolean sensitiveCanChange;

    private static final int backup_settings = 1;
    private static final int import_settings = 2;

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("NullSettings", R.string.NullSettings);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {

        if (position == chatRow) {
            presentFragment(new ChatSettingActivity());
        } else if (position == generalRow) {
            presentFragment(new GeneralSettingActivity());
        } else if (position == experimentRow) {
            presentFragment(new ExperimentSettingActivity(sensitiveEnabled, sensitiveCanChange));
        } else if (position == channelRow) {
            MessagesController.getInstance(currentAccount).openByUserName(LocaleController.getString("OfficialChannelName", R.string.OfficialChannelName), this, 1);
        } else if (position == websiteRow) {
            Browser.openUrl(getParentActivity(), "https://qwq2333.top");
        } else if (position == sourceCodeRow) {
            Browser.openUrl(getParentActivity(), "https://github.com/qwq233/Nullgram");
        } else if (position == licenseRow) {
            presentFragment(new LicenseActivity());
        } else if (position == updateRow) {
            Browser.openUrl(context, "tg://update");
        } else if (position == passcodeRow) {
            presentFragment(new PasscodeSettingActivity());
        }
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        if (position == experimentRow) {
            pressCount++;
            if (pressCount >= 2) {
                ConfigManager.toggleBoolean(Defines.showHiddenSettings);
                AndroidUtilities.shakeView(view);
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getKey() {
        return "";
    }

    private void checkSensitive() {
        TLRPC.TL_account_getContentSettings req = new TLRPC.TL_account_getContentSettings();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_account_contentSettings settings = (TLRPC.TL_account_contentSettings) response;
                sensitiveEnabled = settings.sensitive_enabled;
                sensitiveCanChange = settings.sensitive_can_change;
            } else {
                AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, this, req));
            }
        }));
    }

    @Override
    public View createView(Context mContext) {
        fragmentView = super.createView(mContext);

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem otherMenu = menu.addItem(0, R.drawable.ic_ab_other);
        otherMenu.addSubItem(backup_settings, LocaleController.getString("BackupSettings", R.string.BackupSettings));
        otherMenu.addSubItem(import_settings, LocaleController.getString("ImportSettings", R.string.ImportSettings));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == backup_settings) {
                    backupSettings();
                } else if (id == import_settings) {
                    try {
                        if (!PermissionUtils.isStoragePermissionGranted()) {
                            PermissionUtils.requestStoragePermission(getParentActivity());
                        }
                    } catch (Throwable ignore) {
                    }
                    DocumentSelectActivity fragment = new DocumentSelectActivity(false);
                    fragment.setMaxSelectedFiles(1);
                    fragment.setAllowPhoto(false);
                    fragment.setDelegate(new DocumentSelectActivity.DocumentSelectActivityDelegate() {
                        @Override
                        public void didSelectFiles(DocumentSelectActivity activity, ArrayList<String> files, String caption, boolean notify, int scheduleDate) {
                            activity.finishFragment();
                            importSettings(getParentActivity(), new File(files.get(0)));
                        }

                        @Override
                        public void didSelectPhotos(ArrayList<SendMessagesHelper.SendingMediaInfo> photos, boolean notify, int scheduleDate) {
                        }

                        @Override
                        public void startDocumentSelectActivity() {
                        }
                    });
                    presentFragment(fragment);
                }
            }
        });

        context = mContext;

        return fragmentView;
    }


    @Override
    public void onResume() {
        super.onResume();
        checkSensitive();
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        categoriesRow = addRow();
        generalRow = addRow("General");
        chatRow = addRow("Chat");
        experimentRow = addRow("Experiment");

        if (!PasscodeHelper.isSettingsHidden()) {
            passcodeRow = addRow("Passcode");
        } else {
            passcodeRow = -1;
        }

        categories2Row = addRow();

        aboutRow = addRow();
        channelRow = addRow();
        websiteRow = addRow();
        sourceCodeRow = addRow();
        licenseRow = addRow();
        about2Row = addRow();

        updateRow = addRow();

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
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case TYPE_SHADOW: {
                    break;
                }
                case TYPE_TEXT: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == chatRow) {
                        textCell.setTextAndIcon(LocaleController.getString("Chat", R.string.Chat), R.drawable.menu_chats, true);
                    } else if (position == generalRow) {
                        textCell.setTextAndIcon(LocaleController.getString("General", R.string.General), R.drawable.msg_theme, true);
                    } else if (position == experimentRow) {
                        textCell.setTextAndIcon(LocaleController.getString("Experiment", R.string.Experiment), R.drawable.msg_fave, true);
                    } else if (position == passcodeRow) {
                        textCell.setTextAndIcon(LocaleController.getString("Passcode1", R.string.Passcode1), R.drawable.msg_permissions, true);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == channelRow) {
                        textCell.setTextAndValue(LocaleController.getString("OfficialChannel", R.string.OfficialChannel), "@" + LocaleController.getString("OfficialChannelName", R.string.OfficialChannelName), true);
                    } else if (position == websiteRow) {
                        textCell.setTextAndValue(LocaleController.getString("OfficialSite", R.string.OfficialSite), "qwq2333.top", true);
                    } else if (position == sourceCodeRow) {
                        textCell.setTextAndValue(LocaleController.getString("ViewSourceCode", R.string.ViewSourceCode), "GitHub", true);
                    } else if (position == licenseRow) {
                        textCell.setText(LocaleController.getString("OpenSource", R.string.OpenSource), true);
                    }
                    if (position == updateRow) {
                        textCell.setTextAndValue(LocaleController.getString("CheckUpdate", R.string.CheckUpdate), "Click Me", true);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == categoriesRow) {
                        headerCell.setText(LocaleController.getString("Categories", R.string.Categories));
                    } else if (position == aboutRow) {
                        headerCell.setText(LocaleController.getString("About", R.string.About));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3 || type == 5;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == categories2Row || position == about2Row) {
                return TYPE_SHADOW;
            } else if (position > categoriesRow && position < categories2Row) {
                return TYPE_TEXT;
            } else if ((position >= channelRow && position < about2Row) || position == updateRow) {
                return TYPE_SETTINGS;
            } else if (position == categoriesRow || position == aboutRow) {
                return TYPE_HEADER;
            }
            return TYPE_TEXT;
        }
    }

    private void backupSettings() {

        try {
            File cacheFile = new File(ApplicationLoader.applicationContext.getCacheDir(), DateFormat.format("yyyyMMdd", System.currentTimeMillis()) + "-nullgram-settings.json");
            FileUtils.writeUtf8String(ConfigManager.exportConfigurationToJson(), cacheFile);
            ShareUtil.shareFile(getParentActivity(), cacheFile);
        } catch (JSONException e) {
            AlertUtil.showSimpleAlert(getParentActivity(), e);
        } catch (Exception e) {
            Log.e(e);
        }

    }

    public static void importSettings(Context context, File settingsFile) {

        AlertUtil.showConfirm(context, LocaleController.getString("ImportSettingsAlert", R.string.ImportSettingsAlert), R.drawable.baseline_security_24, LocaleController.getString("Import", R.string.Import), true, () -> importSettingsConfirmed(context, settingsFile));

    }

    public static void importSettingsConfirmed(Context context, File settingsFile) {

        try {
            JsonObject configJson = JsonUtils.toJsonObject(FileUtils.readUtf8String(settingsFile));
            ConfigManager.importSettings(configJson);

            AlertDialog restart = new AlertDialog(context, 0);
            restart.setTitle(LocaleController.getString("AppName", R.string.AppName));
            restart.setMessage(LocaleController.getString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect));
            restart.setPositiveButton(LocaleController.getString("OK", R.string.OK), (__, ___) ->
                ProcessPhoenix.triggerRebirth(context, new Intent(context, LaunchActivity.class)));
            restart.show();
        } catch (Exception e) {
            AlertUtil.showSimpleAlert(context, e);
        }

    }
}
