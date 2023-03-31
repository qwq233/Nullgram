package org.telegram.messenger;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupManager;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

public class BackupAgent extends BackupAgentHelper {

    private static BackupManager backupManager;

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, "saved_tokens", "saved_tokens_login");
        addHelper("prefs", helper);
    }

    public static void requestBackup(Context context) {
        if (backupManager == null) {
            backupManager = new BackupManager(context);
        }
        backupManager.dataChanged();
    }
}
