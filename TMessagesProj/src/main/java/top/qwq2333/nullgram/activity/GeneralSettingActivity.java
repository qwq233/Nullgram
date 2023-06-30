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
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.DrawerLayoutContainer;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Locale;

import kotlin.Unit;
import top.qwq2333.nullgram.config.ConfigManager;
import top.qwq2333.nullgram.helpers.TranslateHelper;
import top.qwq2333.nullgram.helpers.TranslateHelper.ProviderType;
import top.qwq2333.nullgram.translate.providers.DeepLTranslator;
import top.qwq2333.nullgram.ui.DrawerProfilePreviewCell;
import top.qwq2333.nullgram.ui.PopupBuilder;
import top.qwq2333.nullgram.utils.Defines;

@SuppressLint("NotifyDataSetChanged")
public class GeneralSettingActivity extends BaseActivity {

    private DrawerProfilePreviewCell profilePreviewCell;

    private int generalRow;

    private int drawerRow;
    private int avatarAsDrawerBackgroundRow;
    private int avatarBackgroundBlurRow;
    private int avatarBackgroundDarkenRow;
    private int largeAvatarAsBackgroundRow;
    private int hidePhoneRow;
    private int drawer2Row;

    private int translatorRow;
    private int showOriginalRow;
    private int deepLFormalityRow;
    private int translatorTypeRow;
    private int translationProviderRow;
    private int translationTargetRow;
    private int doNotTranslateRow;
    private int autoTranslateRow;
    private int translator2Row;


    private int showBotAPIRow;
    private int showExactNumberRow;
    private int showExactTimeRow;
    private int disableInstantCameraRow;
    private int disableUndoRow;
    private int skipOpenLinkConfirmRow;
    private int tabsTitleTypeRow;
    private int openArchiveOnPullRow;
    private int hideAllTabRow;
    private int ignorMutedCountRow;


    private int devicesRow;
    private int useSystemEmojiRow;
    private int disableVibrationRow;
    private int autoDisableBuiltInProxyRow;
    private int overrideDevicePerformanceRow;
    private int overrideDevicePerformanceDescRow;
    private int devices2Row;

    private int general2Row;


    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("General", R.string.General);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == showBotAPIRow) {
            ConfigManager.toggleBoolean(Defines.showBotAPIID);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.showBotAPIID));
            }
        } else if (position == hidePhoneRow) {
            ConfigManager.toggleBoolean(Defines.hidePhone);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.hidePhone));
            }
            parentLayout.rebuildAllFragmentViews(false, false);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
        } else if (position == avatarAsDrawerBackgroundRow) {
            ConfigManager.toggleBoolean(Defines.avatarAsDrawerBackground);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.avatarAsDrawerBackground));
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            TransitionManager.beginDelayedTransition(profilePreviewCell);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
            if (ConfigManager.getBooleanOrFalse(Defines.avatarAsDrawerBackground)) {
                updateRows();
                listAdapter.notifyItemRangeInserted(avatarBackgroundBlurRow, 2);
            } else {
                listAdapter.notifyItemRangeRemoved(avatarBackgroundBlurRow, 2);
                updateRows();
            }
        } else if (position == avatarBackgroundBlurRow) {
            ConfigManager.toggleBoolean(Defines.avatarBackgroundBlur);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.avatarBackgroundBlur));
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
        } else if (position == avatarBackgroundDarkenRow) {
            ConfigManager.toggleBoolean(Defines.avatarBackgroundDarken);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.avatarBackgroundDarken));
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
        } else if (position == largeAvatarAsBackgroundRow) {
            ConfigManager.toggleBoolean(Defines.largeAvatarAsBackground);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            TransitionManager.beginDelayedTransition(profilePreviewCell);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.largeAvatarAsBackground));
            }
        } else if (position == showExactNumberRow) {
            ConfigManager.toggleBoolean(Defines.showExactNumber);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.showExactNumber));
            }
        } else if (position == showExactTimeRow) {
            ConfigManager.toggleBoolean(Defines.showExactTime);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.showExactTime));
            }
        } else if (position == disableInstantCameraRow) {
            ConfigManager.toggleBoolean(Defines.disableInstantCamera);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableInstantCamera));
            }
        } else if (position == disableUndoRow) {
            ConfigManager.toggleBoolean(Defines.disableUndo);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableUndo));
            }
        } else if (position == skipOpenLinkConfirmRow) {
            ConfigManager.toggleBoolean(Defines.skipOpenLinkConfirm);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.skipOpenLinkConfirm));
            }
        } else if (position == useSystemEmojiRow) {
            ConfigManager.toggleBoolean(Defines.useSystemEmoji);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.useSystemEmoji));
            }
        } else if (position == disableVibrationRow) {
            ConfigManager.toggleBoolean(Defines.disableVibration);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.disableVibration));
            }
        } else if (position == tabsTitleTypeRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText));
            types.add(Defines.tabMenuText);
            arrayList.add(LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon));
            types.add(Defines.tabMenuIcon);
            arrayList.add(LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix));
            types.add(Defines.tabMenuMix);
            PopupBuilder.show(arrayList, LocaleController.getString("TabTitleType", R.string.TabTitleType), types.indexOf(ConfigManager.getIntOrDefault(Defines.tabMenu, Defines.tabMenuMix)), getParentActivity(), view, i -> {
                ConfigManager.putInt(Defines.tabMenu, types.get(i));
                listAdapter.notifyItemChanged(tabsTitleTypeRow, PARTIAL);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            });
        } else if (position == overrideDevicePerformanceRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("DevicePerformanceAuto", R.string.DevicePerformanceAuto));
            types.add(Defines.devicePerformanceAuto);
            arrayList.add(LocaleController.getString("DevicePerformanceLow", R.string.DevicePerformanceLow));
            types.add(Defines.devicePerformanceLow);
            arrayList.add(LocaleController.getString("DevicePerformanceMedium", R.string.DevicePerformanceMedium));
            types.add(Defines.devicePerformanceMedium);
            arrayList.add(LocaleController.getString("DevicePerformanceHigh", R.string.DevicePerformanceHigh));
            types.add(Defines.devicePerformanceHigh);
            PopupBuilder.show(arrayList, LocaleController.getString("OverrideDevicePerformance", R.string.OverrideDevicePerformance), types.indexOf(ConfigManager.getIntOrDefault(Defines.devicePerformance, Defines.devicePerformanceAuto)), getParentActivity(), view, i -> {
                ConfigManager.putInt(Defines.devicePerformance, types.get(i));
                listAdapter.notifyItemChanged(overrideDevicePerformanceRow, PARTIAL);
            });
        } else if (position == openArchiveOnPullRow) {
            ConfigManager.toggleBoolean(Defines.openArchiveOnPull);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.openArchiveOnPull));
            }
        } else if (position == hideAllTabRow) {
            ConfigManager.toggleBoolean(Defines.hideAllTab);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.hideAllTab));
            }
        } else if (position == ignorMutedCountRow) {
            ConfigManager.toggleBoolean(Defines.ignoreMutedCount);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.ignoreMutedCount));
            }
        } else if (position == autoDisableBuiltInProxyRow) {
            ConfigManager.toggleBoolean(Defines.autoDisableBuiltInProxy);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ConfigManager.getBooleanOrFalse(Defines.autoDisableBuiltInProxy));
            }
        } else if (position == translationProviderRow) {
            final var oldProvider = TranslateHelper.getCurrentProviderType();
            TranslateHelper.showTranslationProviderSelector(getParentActivity(), view, null, param -> {
                if (param) {
                    listAdapter.notifyItemChanged(translationProviderRow, PARTIAL);
                } else {
                    listAdapter.notifyItemChanged(translationProviderRow, PARTIAL);
                    listAdapter.notifyItemChanged(translationTargetRow, PARTIAL);
                }
                if (!oldProvider.equals(TranslateHelper.getCurrentProviderType())) {
                    if (oldProvider.equals(ProviderType.DeepLTranslator)) {
                        listAdapter.notifyItemRemoved(deepLFormalityRow);
                        updateRows();
                    } else if (TranslateHelper.getCurrentProviderType().equals(ProviderType.DeepLTranslator)) {
                        updateRows();
                        listAdapter.notifyItemInserted(deepLFormalityRow);
                    }
                }
                return Unit.INSTANCE;
            });
        } else if (position == translationTargetRow) {
            TranslateHelper.showTranslationTargetSelector(this, view, () -> {
                listAdapter.notifyItemChanged(translationTargetRow, PARTIAL);
                if (getRestrictedLanguages().size() == 1) {
                    listAdapter.notifyItemChanged(doNotTranslateRow, PARTIAL);
                }
                return Unit.INSTANCE;
            });
        } else if (position == deepLFormalityRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("DeepLFormalityDefault", R.string.DeepLFormalityDefault));
            types.add(DeepLTranslator.FORMALITY_DEFAULT);
            arrayList.add(LocaleController.getString("DeepLFormalityMore", R.string.DeepLFormalityMore));
            types.add(DeepLTranslator.FORMALITY_MORE);
            arrayList.add(LocaleController.getString("DeepLFormalityLess", R.string.DeepLFormalityLess));
            types.add(DeepLTranslator.FORMALITY_LESS);
            PopupBuilder.show(arrayList, LocaleController.getString("DeepLFormality", R.string.DeepLFormality),
                types.indexOf(ConfigManager.getIntOrDefault(Defines.deepLFormality, 0)), getParentActivity(), view, i -> {
                    ConfigManager.putInt(Defines.deepLFormality, types.get(i));
                    listAdapter.notifyItemChanged(deepLFormalityRow, PARTIAL);
                });
        } else if (position == translatorTypeRow) {
            var oldType = TranslateHelper.getCurrentStatus();
            TranslateHelper.showTranslatorTypeSelector(getParentActivity(), view, () -> {
                var newType = TranslateHelper.getCurrentStatus();
                listAdapter.notifyItemChanged(translatorTypeRow, PARTIAL);
                if (oldType != newType) {
                    int count = 3;
                    if (oldType == TranslateHelper.Status.InMessage || newType == TranslateHelper.Status.InMessage) {
                        count++;
                    }
                    if (oldType == TranslateHelper.Status.External) {
                        updateRows();
                        listAdapter.notifyItemRangeInserted(translationProviderRow, count);
                    } else if (newType == TranslateHelper.Status.External) {
                        listAdapter.notifyItemRangeRemoved(translationProviderRow, count);
                        updateRows();
                    } else if (oldType == TranslateHelper.Status.InMessage) {
                        listAdapter.notifyItemRemoved(showOriginalRow);
                        updateRows();
                    } else if (newType == TranslateHelper.Status.InMessage) {
                        updateRows();
                        listAdapter.notifyItemInserted(showOriginalRow);
                    }
                }
                return Unit.INSTANCE;
            });
        } else if (position == doNotTranslateRow) {
            if (!LanguageDetector.hasSupport()) {
                BulletinFactory.of(this).createErrorBulletinSubtitle(LocaleController.getString("BrokenMLKit", R.string.BrokenMLKit), LocaleController.getString("BrokenMLKitDetail", R.string.BrokenMLKitDetail), null).show();
                return;
            }
            presentFragment(new LanguageSelectActivity(LanguageSelectActivity.TYPE_RESTRICTED, true));
        } else if (position == autoTranslateRow) {
            if (!LanguageDetector.hasSupport()) {
                BulletinFactory.of(this).createErrorBulletinSubtitle(LocaleController.getString("BrokenMLKit", R.string.BrokenMLKit), LocaleController.getString("BrokenMLKitDetail", R.string.BrokenMLKitDetail), null).show();
                return;
            }
            TranslateHelper.toggleAutoTranslate();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(TranslateHelper.getAutoTranslate());
            }
        } else if (position == showOriginalRow) {
            TranslateHelper.toggleShowOriginal();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(TranslateHelper.getShowOriginal());
            }
        }

    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        return false;
    }

    @Override
    protected String getKey() {
        return "g";
    }

    protected void updateRows() {
        super.updateRows();

        drawerRow = addRow();
        avatarAsDrawerBackgroundRow = addRow("avatarAsDrawerBackground");
        if (ConfigManager.getBooleanOrFalse(Defines.avatarAsDrawerBackground)) {
            avatarBackgroundBlurRow = addRow("avatarBackgroundBlur");
            avatarBackgroundDarkenRow = addRow("avatarBackgroundDarken");
            largeAvatarAsBackgroundRow = addRow("largeAvatarAsBackground");
        } else {
            avatarBackgroundBlurRow = -1;
            avatarBackgroundDarkenRow = -1;
            largeAvatarAsBackgroundRow = -1;
        }
        hidePhoneRow = addRow("hidePhone");
        drawer2Row = addRow();

        translatorRow = addRow();
        translatorTypeRow = addRow("translatorType");
        if (TranslateHelper.getCurrentStatus() != TranslateHelper.Status.External) {
            showOriginalRow = TranslateHelper.getCurrentStatus() == TranslateHelper.Status.InMessage ? addRow("showOriginal") : -1;
            translationProviderRow = addRow("translationProvider");
            deepLFormalityRow = TranslateHelper.getCurrentProviderType().equals(ProviderType.DeepLTranslator) ? addRow("deepLFormality") : -1;
            translationTargetRow = addRow("translationTarget");
            doNotTranslateRow = addRow("doNotTranslate");
            autoTranslateRow = addRow("autoTranslate");
        } else {
            showOriginalRow = -1;
            translationProviderRow = -1;
            translationTargetRow = -1;
            deepLFormalityRow = -1;
            doNotTranslateRow = -1;
            autoTranslateRow = -1;
        }
        translator2Row = addRow();


        generalRow = addRow();
        showBotAPIRow = addRow("showBotAPI");
        showExactNumberRow = addRow("showExactNumber");
        showExactTimeRow = addRow("showExactTime");
        disableInstantCameraRow = addRow("disableInstantCamera");
        disableUndoRow = addRow("disableUndo");
        skipOpenLinkConfirmRow = addRow("skipOpenLinkConfirm");
        openArchiveOnPullRow = addRow("openArchiveOnPull");
        hideAllTabRow = addRow("hideAllTab");
        ignorMutedCountRow = addRow("ignoreMutedCount");
        tabsTitleTypeRow = addRow("tabsTitleType");
        general2Row = addRow();

        devicesRow = addRow();
        useSystemEmojiRow = addRow("useSystemEmoji");
        autoDisableBuiltInProxyRow = addRow("autoDisableBuiltInProxy");
        disableVibrationRow = addRow("disableVibration");
        overrideDevicePerformanceRow = addRow("overrideDevicePerformance");
        overrideDevicePerformanceDescRow = addRow();
        devices2Row = addRow();

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends BaseListAdapter {
        private final DrawerLayoutContainer mDrawerLayoutContainer;

        public ListAdapter(Context context) {
            super(context);
            mDrawerLayoutContainer = new DrawerLayoutContainer(mContext);

        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1: {
                    if (position == general2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == tabsTitleTypeRow) {
                        String value;
                        switch (ConfigManager.getIntOrDefault(Defines.tabMenu, Defines.tabMenuMix)) {
                            case Defines.tabMenuText:
                                value = LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText);
                                break;
                            case Defines.tabMenuIcon:
                                value = LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon);
                                break;
                            case Defines.tabMenuMix:
                            default:
                                value = LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix);
                        }
                        textCell.setTextAndValue(LocaleController.getString("TabTitleType", R.string.TabTitleType), value, payload, false);
                    } else if (position == overrideDevicePerformanceRow) {
                        String value;
                        switch (ConfigManager.getIntOrDefault(Defines.devicePerformance, Defines.devicePerformanceAuto)) {
                            case Defines.devicePerformanceLow:
                                value = LocaleController.getString("DevicePerformanceLow", R.string.DevicePerformanceLow);
                                break;
                            case Defines.devicePerformanceMedium:
                                value = LocaleController.getString("DevicePerformanceMedium", R.string.DevicePerformanceMedium);
                                break;
                            case Defines.devicePerformanceHigh:
                                value = LocaleController.getString("DevicePerformanceHigh", R.string.DevicePerformanceHigh);
                                break;
                            default:
                                value = LocaleController.getString("DevicePerformanceAuto", R.string.DevicePerformanceAuto);
                        }
                        textCell.setTextAndValue(LocaleController.getString("OverrideDevicePerformance", R.string.OverrideDevicePerformance), value, payload, false);
                    } else if (position == translationProviderRow) {
                        Pair<ArrayList<String>, ArrayList<TranslateHelper.ProviderType>> providers = TranslateHelper.getProviders();
                        ArrayList<String> names = providers.first;
                        ArrayList<TranslateHelper.ProviderType> types = providers.second;
                        if (names == null || types == null) {
                            return;
                        }
                        int index = types.indexOf(TranslateHelper.getCurrentProviderType());
                        if (index < 0) {
                            textCell.setTextAndValue(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), "", payload, true);
                        } else {
                            String value = names.get(index);
                            textCell.setTextAndValue(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), value, payload, true);
                        }
                    } else if (position == translationTargetRow) {
                        String language = TranslateHelper.getCurrentTargetLanguage();
                        CharSequence value;
                        if (language.equals("app")) {
                            value = LocaleController.getString("TranslationTargetApp", R.string.TranslationTargetApp);
                        } else {
                            Locale locale = Locale.forLanguageTag(language);
                            if (!TextUtils.isEmpty(locale.getScript())) {
                                value = HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
                            } else {
                                value = locale.getDisplayName();
                            }
                        }
                        textCell.setTextAndValue(LocaleController.getString("TranslationTarget", R.string.TranslationTarget), value, payload, true);
                    } else if (position == deepLFormalityRow) {
                        String value;
                        switch (ConfigManager.getIntOrDefault(Defines.deepLFormality, 0)) {
                            case DeepLTranslator.FORMALITY_DEFAULT:
                            default:
                                value = LocaleController.getString("DeepLFormalityDefault", R.string.DeepLFormalityDefault);
                                break;
                            case DeepLTranslator.FORMALITY_MORE:
                                value = LocaleController.getString("DeepLFormalityMore", R.string.DeepLFormalityMore);
                                break;
                            case DeepLTranslator.FORMALITY_LESS:
                                value = LocaleController.getString("DeepLFormalityLess", R.string.DeepLFormalityLess);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("DeepLFormality", R.string.DeepLFormality), value, payload, true);
                    } else if (position == translatorTypeRow) {
                        String value;
                        switch (TranslateHelper.getCurrentStatus()) {
                            case Popup:
                                value = LocaleController.getString("TranslatorTypePopup", R.string.TranslatorTypePopup);
                                break;
                            case External:
                                value = LocaleController.getString("TranslatorTypeExternal", R.string.TranslatorTypeExternal);
                                break;
                            case InMessage:
                            default:
                                value = LocaleController.getString("TranslatorTypeInMessage", R.string.TranslatorTypeInMessage);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("TranslatorType", R.string.TranslatorType), value, payload, position + 1 != translator2Row);
                    } else if (position == doNotTranslateRow) {
                        ArrayList<String> langCodes = getRestrictedLanguages();
                        CharSequence value;
                        if (langCodes.size() == 1) {
                            Locale locale = Locale.forLanguageTag(langCodes.get(0));
                            if (!TextUtils.isEmpty(locale.getScript())) {
                                value = HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
                            } else {
                                value = locale.getDisplayName();
                            }
                        } else {
                            value = LocaleController.formatPluralString("Languages", langCodes.size());
                        }
                        textCell.setTextAndValue(LocaleController.getString("DoNotTranslate", R.string.DoNotTranslate), value, payload, true);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == showBotAPIRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showBotAPIID", R.string.showBotAPIID), ConfigManager.getBooleanOrFalse(Defines.showBotAPIID), true);
                    } else if (position == hidePhoneRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hidePhone", R.string.hidePhone), ConfigManager.getBooleanOrFalse(Defines.hidePhone), true);
                    } else if (position == showExactNumberRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showExactNumber", R.string.showExactNumber), ConfigManager.getBooleanOrFalse(Defines.showExactNumber), true);
                    } else if (position == showExactTimeRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showExactTime", R.string.showExactTime), ConfigManager.getBooleanOrFalse(Defines.showExactTime), true);
                    } else if (position == disableInstantCameraRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableInstantCamera", R.string.disableInstantCamera), ConfigManager.getBooleanOrFalse(Defines.disableInstantCamera), true);
                    } else if (position == disableUndoRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableUndo", R.string.disableUndo), ConfigManager.getBooleanOrFalse(Defines.disableUndo), true);
                    } else if (position == skipOpenLinkConfirmRow) {
                        textCell.setTextAndCheck(LocaleController.getString("skipOpenLinkConfirm", R.string.skipOpenLinkConfirm), ConfigManager.getBooleanOrFalse(Defines.skipOpenLinkConfirm), true);
                    } else if (position == avatarAsDrawerBackgroundRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AvatarAsBackground", R.string.AvatarAsBackground), ConfigManager.getBooleanOrFalse(Defines.avatarAsDrawerBackground), true);
                    } else if (position == avatarBackgroundBlurRow) {
                        textCell.setTextAndCheck(LocaleController.getString("BlurAvatarBackground", R.string.BlurAvatarBackground), ConfigManager.getBooleanOrFalse(Defines.avatarBackgroundBlur), true);
                    } else if (position == avatarBackgroundDarkenRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DarkenAvatarBackground", R.string.DarkenAvatarBackground), ConfigManager.getBooleanOrFalse(Defines.avatarBackgroundDarken), true);
                    } else if (position == largeAvatarAsBackgroundRow) {
                        textCell.setTextAndCheck(LocaleController.getString("LargeAvatarAsBackground", R.string.largeAvatarAsBackground), ConfigManager.getBooleanOrFalse(Defines.largeAvatarAsBackground), true);
                    } else if (position == useSystemEmojiRow) {
                        textCell.setTextAndCheck(LocaleController.getString("UseSystemEmoji", R.string.useSystemEmoji), ConfigManager.getBooleanOrFalse(Defines.useSystemEmoji), true);
                    } else if (position == disableVibrationRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableVibration", R.string.disableVibration), ConfigManager.getBooleanOrFalse(Defines.disableVibration), true);
                    } else if (position == openArchiveOnPullRow) {
                        textCell.setTextAndCheck(LocaleController.getString("openArchiveOnPull", R.string.openArchiveOnPull), ConfigManager.getBooleanOrFalse(Defines.openArchiveOnPull), true);
                    } else if (position == hideAllTabRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideAllTab", R.string.hideAllTab), ConfigManager.getBooleanOrFalse(Defines.hideAllTab), true);
                    } else if (position == ignorMutedCountRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ignoreMutedCount", R.string.ignoreMutedCount),
                            ConfigManager.getBooleanOrFalse(Defines.ignoreMutedCount), true);
                    } else if (position == autoDisableBuiltInProxyRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("autoDisableBuiltInProxy", R.string.autoDisableBuiltInProxy),
                            LocaleController.getString("autoDisableBuiltInProxyDesc", R.string.autoDisableBuiltInProxyDesc),
                            ConfigManager.getBooleanOrFalse(Defines.autoDisableBuiltInProxy), true, true);
                    } else if (position == autoTranslateRow) {
                        textCell.setEnabled(LanguageDetector.hasSupport(), null);
                        textCell.setTextAndValueAndCheck(LocaleController.getString("AutoTranslate", R.string.AutoTranslate),
                            LocaleController.getString("AutoTranslateAbout",
                                R.string.AutoTranslateAbout), TranslateHelper.getAutoTranslate(), true, false);
                    } else if (position == showOriginalRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TranslatorShowOriginal", R.string.TranslatorShowOriginal), TranslateHelper.getShowOriginal(), true);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == generalRow) {
                        headerCell.setText(LocaleController.getString("General", R.string.General));
                    } else if (position == translatorRow) {
                        headerCell.setText(LocaleController.getString("Translator", R.string.Translator));
                    } else if (position == devicesRow) {
                        headerCell.setText(LocaleController.getString("Devices", R.string.Devices));
                    }
                    break;
                }
                case 5: {
                    NotificationsCheckCell textCell = (NotificationsCheckCell) holder.itemView;
                    break;
                }
                case 7: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == overrideDevicePerformanceDescRow) {
                        cell.setText(LocaleController.getString("OverrideDevicePerformanceDesc", R.string.OverrideDevicePerformanceDesc));
                    }
                    break;
                }
                case 8: {
                    DrawerProfilePreviewCell cell = (DrawerProfilePreviewCell) holder.itemView;
                    cell.setUser(getUserConfig().getCurrentUser(), false);
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
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 5:
                    view = new NotificationsCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 6:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 7:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 8:
                    profilePreviewCell = new DrawerProfilePreviewCell(mContext);
                    profilePreviewCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    profilePreviewCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                    return new RecyclerListView.Holder(profilePreviewCell);
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == general2Row || position == drawer2Row || position == translator2Row || position == devices2Row) {
                return 1;
            } else if (position == tabsTitleTypeRow || position == translationProviderRow || position == deepLFormalityRow || position == translationTargetRow ||
                position == translatorTypeRow || position == doNotTranslateRow || position == overrideDevicePerformanceRow) {
                return 2;
            } else if (position == generalRow || position == translatorRow || position == devicesRow) {
                return 4;
            } else if (position == overrideDevicePerformanceDescRow) {
                return 7;
            } else if (position == drawerRow) {
                return 8;
            } else if ((position > generalRow && position < general2Row) || (position > devicesRow && position < devices2Row) || (position > drawerRow && position < drawer2Row) || (position > translatorRow && position < translator2Row)) {
                return 3;
            }
            return -1;
        }
    }

    private ArrayList<String> getRestrictedLanguages() {
        String currentLang = TranslateHelper.stripLanguageCode(TranslateHelper.getCurrentProvider().getCurrentTargetLanguage());
        ArrayList<String> langCodes = new ArrayList<>(TranslateHelper.getRestrictedLanguages());
        if (!langCodes.contains(currentLang)) {
            langCodes.add(currentLang);
        }
        return langCodes;
    }
}
