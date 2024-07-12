package org.telegram.ui.Stars;

import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.getCurrencyExpDivider;
import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.LongSparseArray;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BillingController;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChannelMonetizationLayout;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PaymentFormActivity;
import org.telegram.ui.bots.BotWebViewSheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class BotStarsController {

    private static volatile BotStarsController[] Instance = new BotStarsController[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];
    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }
    }

    public static BotStarsController getInstance(int num) {
        BotStarsController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (lockObjects[num]) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new BotStarsController(num);
                }
            }
        }
        return localInstance;
    }

    public final int currentAccount;

    private BotStarsController(int account) {
        currentAccount = account;
    }

    private final HashMap<Long, Long> lastLoadedStats = new HashMap<>();
    private final HashMap<Long, TLRPC.TL_payments_starsRevenueStats> stats = new HashMap<>();

    public long getBalance(long bot_id) {
        TLRPC.TL_payments_starsRevenueStats botStats = getRevenueStats(bot_id);
        return botStats == null ? 0 : botStats.status.current_balance;
    }

    public long getAvailableBalance(long bot_id) {
        TLRPC.TL_payments_starsRevenueStats botStats = getRevenueStats(bot_id);
        return botStats == null ? 0 : botStats.status.available_balance;
    }

    public boolean isBalanceAvailable(long bot_id) {
        return getRevenueStats(bot_id) != null;
    }

    public TLRPC.TL_payments_starsRevenueStats getRevenueStats(long bot_id) {
        return getRevenueStats(bot_id, false);
    }

    public boolean hasStars(long bot_id) {
        TLRPC.TL_payments_starsRevenueStats stats = getRevenueStats(bot_id);
        return stats != null && stats.status != null && (stats.status.available_balance > 0 || stats.status.overall_revenue > 0 || stats.status.current_balance > 0);
    }

    public void preloadRevenueStats(long bot_id) {
        Long lastLoaded = lastLoadedStats.get(bot_id);
        TLRPC.TL_payments_starsRevenueStats botStats = stats.get(bot_id);
        getRevenueStats(bot_id, lastLoaded == null || System.currentTimeMillis() - lastLoaded > 1000 * 30);
    }

    public TLRPC.TL_payments_starsRevenueStats getRevenueStats(long bot_id, boolean force) {
        Long lastLoaded = lastLoadedStats.get(bot_id);
        TLRPC.TL_payments_starsRevenueStats botStats = stats.get(bot_id);
        if (lastLoaded == null || System.currentTimeMillis() - lastLoaded > 1000 * 60 * 5 || force) {
            TLRPC.TL_payments_getStarsRevenueStats req = new TLRPC.TL_payments_getStarsRevenueStats();
            req.dark = Theme.isCurrentThemeDark();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(bot_id);
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res instanceof TLRPC.TL_payments_starsRevenueStats) {
                    TLRPC.TL_payments_starsRevenueStats r = (TLRPC.TL_payments_starsRevenueStats) res;
                    stats.put(bot_id, r);
                } else {
                    stats.put(bot_id, null);
                }
                lastLoadedStats.put(bot_id, System.currentTimeMillis());
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botStarsUpdated, bot_id);
            }));
        }
        return botStats;
    }

    public void onUpdate(TLRPC.TL_updateStarsRevenueStatus update) {
        if (update == null) return;
        long dialogId = DialogObject.getPeerDialogId(update.peer);
        if (dialogId < 0) {
            if (ChannelMonetizationLayout.instance != null && ChannelMonetizationLayout.instance.dialogId == DialogObject.getPeerDialogId(update.peer)) {
                ChannelMonetizationLayout.instance.setupBalances(update.status);
                ChannelMonetizationLayout.instance.reloadTransactions();
            }
        } else {
            TLRPC.TL_payments_starsRevenueStats s = getRevenueStats(dialogId, true);
            if (s != null) {
                s.status = update.status;
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botStarsUpdated, dialogId);
            }
            invalidateTransactions(dialogId, true);
        }
    }


    public static final int ALL_TRANSACTIONS = 0;
    public static final int INCOMING_TRANSACTIONS = 1;
    public static final int OUTGOING_TRANSACTIONS = 2;

    private class TransactionsState {
        public final ArrayList<TLRPC.StarsTransaction>[] transactions = new ArrayList[] { new ArrayList<>(), new ArrayList<>(), new ArrayList<>() };
        public final boolean[] transactionsExist = new boolean[3];
        private final String[] offset = new String[3];
        private final boolean[] loading = new boolean[3];
        private final boolean[] endReached = new boolean[3];
    }

    private final HashMap<Long, TransactionsState> transactions = new HashMap<>();

    @NonNull
    private TransactionsState getTransactionsState(long bot_id) {
        TransactionsState state = transactions.get(bot_id);
        if (state == null) {
            transactions.put(bot_id, state = new TransactionsState());
        }
        return state;
    }

    @NonNull
    public ArrayList<TLRPC.StarsTransaction> getTransactions(long bot_id, int type) {
        TransactionsState state = getTransactionsState(bot_id);
        return state.transactions[type];
    }

    public void invalidateTransactions(long bot_id, boolean load) {
        final TransactionsState state = getTransactionsState(bot_id);
        for (int i = 0; i < 3; ++i) {
            if (state.loading[i]) continue;
            state.transactions[i].clear();
            state.offset[i] = null;
            state.loading[i] = false;
            state.endReached[i] = false;
            if (load)
                loadTransactions(bot_id, i);
        }
    }

    public void preloadTransactions(long bot_id) {
        final TransactionsState state = getTransactionsState(bot_id);
        for (int i = 0; i < 3; ++i) {
            if (!state.loading[i] && !state.endReached[i] && state.offset[i] == null) {
                loadTransactions(bot_id, i);
            }
        }
    }

    public void loadTransactions(long bot_id, int type) {
        final TransactionsState state = getTransactionsState(bot_id);
        if (state.loading[type] || state.endReached[type]) {
            return;
        }

        state.loading[type] = true;

        TLRPC.TL_payments_getStarsTransactions req = new TLRPC.TL_payments_getStarsTransactions();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(bot_id);
        req.inbound = type == INCOMING_TRANSACTIONS;
        req.outbound = type == OUTGOING_TRANSACTIONS;
        req.offset = state.offset[type];
        if (req.offset == null) {
            req.offset = "";
        }
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            state.loading[type] = false;
            if (res instanceof TLRPC.TL_payments_starsStatus) {
                TLRPC.TL_payments_starsStatus r = (TLRPC.TL_payments_starsStatus) res;
                MessagesController.getInstance(currentAccount).putUsers(r.users, false);
                MessagesController.getInstance(currentAccount).putChats(r.chats, false);

                state.transactions[type].addAll(r.history);
                state.transactionsExist[type] = !state.transactions[type].isEmpty() || state.transactionsExist[type];
                state.endReached[type] = (r.flags & 1) == 0;
                state.offset[type] = state.endReached[type] ? null : r.next_offset;

//                state.updateBalance(r.balance);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botStarsTransactionsLoaded, bot_id);
            }
        }));
    }

    public boolean isLoadingTransactions(long bot_id, int type) {
        final TransactionsState state = getTransactionsState(bot_id);
        return state.loading[type];
    }

    public boolean didFullyLoadTransactions(long bot_id, int type) {
        final TransactionsState state = getTransactionsState(bot_id);
        return state.endReached[type];
    }

    public boolean hasTransactions(long bot_id) {
        return hasTransactions(bot_id, ALL_TRANSACTIONS);
    }

    public boolean hasTransactions(long bot_id, int type) {
        final TransactionsState state = getTransactionsState(bot_id);
        return !state.transactions[type].isEmpty();
    }

}
