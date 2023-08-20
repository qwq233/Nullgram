package org.telegram.messenger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.telegram.messenger.utils.BillingUtilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.PremiumPreviewFragment;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BillingController implements PurchasesUpdatedListener, BillingClientStateListener {
    public final static String PREMIUM_PRODUCT_ID = "telegram_premium";
    public final static QueryProductDetailsParams.Product PREMIUM_PRODUCT = QueryProductDetailsParams.Product.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .setProductId(PREMIUM_PRODUCT_ID)
            .build();

    @Nullable
    public static ProductDetails PREMIUM_PRODUCT_DETAILS;

    private static BillingController instance;

    public static boolean billingClientEmpty;

    private final Map<String, Consumer<BillingResult>> resultListeners = new HashMap<>();
    private final List<String> requestingTokens = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> currencyExpMap = new HashMap<>();
    private final BillingClient billingClient;
    private String lastPremiumTransaction;
    private String lastPremiumToken;
    private boolean isDisconnected;

    public static BillingController getInstance() {
        if (instance == null) {
            instance = new BillingController(ApplicationLoader.applicationContext);
        }
        return instance;
    }

    private BillingController(Context ctx) {
        billingClient = BillingClient.newBuilder(ctx)
                .enablePendingPurchases()
                .setListener(this)
                .build();
    }

    public String getLastPremiumTransaction() {
        return lastPremiumTransaction;
    }

    public String getLastPremiumToken() {
        return lastPremiumToken;
    }

    public String formatCurrency(long amount, String currency) {
        return formatCurrency(amount, currency, getCurrencyExp(currency));
    }

    public String formatCurrency(long amount, String currency, int exp) {
        if (currency.isEmpty()) {
            return String.valueOf(amount);
        }
        Currency cur = Currency.getInstance(currency);
        if (cur != null) {
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
            numberFormat.setCurrency(cur);
            return numberFormat.format(amount / Math.pow(10, exp));
        }
        return amount + " " + currency;
    }

    @SuppressWarnings("ConstantConditions")
    public int getCurrencyExp(String currency) {
        return currencyExpMap.getOrDefault(currency, 0);
    }

    public void startConnection() {
        if (isReady()) {
            return;
        }
        BillingUtilities.extractCurrencyExp(currencyExpMap);
        if (!BuildVars.useInvoiceBilling()) {
            billingClient.startConnection(this);
        }
    }

    private void switchToInvoice() {
        if (billingClientEmpty) {
            return;
        }
        billingClientEmpty = true;
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.billingProductDetailsUpdated);
    }

    public boolean isReady() {
        return billingClient.isReady();
    }

    public void queryProductDetails(List<QueryProductDetailsParams.Product> products, ProductDetailsResponseListener responseListener) {
        if (!isReady()) {
            throw new IllegalStateException("Billing: Controller should be ready for this call!");
        }
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build(), responseListener);
    }

    /**
     * {@link BillingClient#queryPurchasesAsync} returns only active subscriptions and not consumed purchases.
     */
    public void queryPurchases(String productType, PurchasesResponseListener responseListener) {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(productType).build(), responseListener);
    }

    public boolean startManageSubscription(Context ctx, String productId) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://play.google.com/store/account/subscriptions?sku=%s&package=%s", productId, ctx.getPackageName()))));
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    public void addResultListener(String productId, Consumer<BillingResult> listener) {
        resultListeners.put(productId, listener);
    }

    public void launchBillingFlow(Activity activity, AccountInstance accountInstance, TLRPC.InputStorePaymentPurpose paymentPurpose, List<BillingFlowParams.ProductDetailsParams> productDetails) {
        launchBillingFlow(activity, accountInstance, paymentPurpose, productDetails, null, false);
    }

    public void launchBillingFlow(Activity activity, AccountInstance accountInstance, TLRPC.InputStorePaymentPurpose paymentPurpose, List<BillingFlowParams.ProductDetailsParams> productDetails, BillingFlowParams.SubscriptionUpdateParams subscriptionUpdateParams, boolean checkedConsume) {
        if (!isReady() || activity == null) {
            return;
        }

        if (paymentPurpose instanceof TLRPC.TL_inputStorePaymentGiftPremium && !checkedConsume) {
            queryPurchases(BillingClient.ProductType.INAPP, (billingResult, list) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Runnable callback = () -> launchBillingFlow(activity, accountInstance, paymentPurpose, productDetails, subscriptionUpdateParams, true);

                    AtomicInteger productsToBeConsumed = new AtomicInteger(0);
                    List<String> productsConsumed = new ArrayList<>();
                    for (Purchase purchase : list) {
                        if (purchase.isAcknowledged()) {
                            for (BillingFlowParams.ProductDetailsParams params : productDetails) {
                                String productId = params.zza().getProductId();
                                if (purchase.getProducts().contains(productId)) {
                                    productsToBeConsumed.incrementAndGet();
                                    billingClient.consumeAsync(ConsumeParams.newBuilder()
                                                    .setPurchaseToken(purchase.getPurchaseToken())
                                            .build(), (billingResult1, s) -> {
                                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                            productsConsumed.add(productId);

                                            if (productsToBeConsumed.get() == productsConsumed.size()) {
                                                callback.run();
                                            }
                                        }
                                    });
                                    break;
                                }
                            }
                        } else {
                            onPurchasesUpdated(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), Collections.singletonList(purchase));
                            return;
                        }
                    }

                    if (productsToBeConsumed.get() == 0) {
                        callback.run();
                    }
                }
            });
            return;
        }

        Pair<String, String> payload = BillingUtilities.createDeveloperPayload(paymentPurpose, accountInstance);
        String obfuscatedAccountId = payload.first;
        String obfuscatedData = payload.second;

        BillingFlowParams.Builder flowParams = BillingFlowParams.newBuilder()
                .setObfuscatedAccountId(obfuscatedAccountId)
                .setObfuscatedProfileId(obfuscatedData)
                .setProductDetailsParamsList(productDetails);
        if (subscriptionUpdateParams != null) {
            flowParams.setSubscriptionUpdateParams(subscriptionUpdateParams);
        }
        int responseCode = billingClient.launchBillingFlow(activity, flowParams.build()).getResponseCode();
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            FileLog.d("Billing: Launch Error: " + responseCode + ", " + obfuscatedAccountId + ", " + obfuscatedData);
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billing, @Nullable List<Purchase> list) {
        FileLog.d("Billing: Purchases updated: " + billing + ", " + list);
        if (billing.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            if (billing.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                PremiumPreviewFragment.sentPremiumBuyCanceled();
            }
            return;
        }
        if (list == null || list.isEmpty()) {
            return;
        }
        lastPremiumTransaction = null;
        for (Purchase purchase : list) {
            if (purchase.getProducts().contains(PREMIUM_PRODUCT_ID)) {
                lastPremiumTransaction = purchase.getOrderId();
                lastPremiumToken = purchase.getPurchaseToken();
            }

            if (!requestingTokens.contains(purchase.getPurchaseToken()) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                Pair<AccountInstance, TLRPC.InputStorePaymentPurpose> payload = BillingUtilities.extractDeveloperPayload(purchase);
                if (payload == null) {
                    continue;
                }
                if (!purchase.isAcknowledged()) {
                    requestingTokens.add(purchase.getPurchaseToken());

                    TLRPC.TL_payments_assignPlayMarketTransaction req = new TLRPC.TL_payments_assignPlayMarketTransaction();
                    req.receipt = new TLRPC.TL_dataJSON();
                    req.receipt.data = purchase.getOriginalJson();
                    req.purpose = payload.second;

                    AccountInstance acc = payload.first;
                    acc.getConnectionsManager().sendRequest(req, (response, error) -> {
                        requestingTokens.remove(purchase.getPurchaseToken());

                        if (response instanceof TLRPC.Updates) {
                            acc.getMessagesController().processUpdates((TLRPC.Updates) response, false);

                            for (String productId : purchase.getProducts()) {
                                Consumer<BillingResult> listener = resultListeners.remove(productId);
                                if (listener != null) {
                                    listener.accept(billing);
                                }
                            }

                            consumeGiftPurchase(purchase, req.purpose);
                        } else if (error != null) {
                            FileLog.d("Billing: Confirmation Error: " + error.code + " " + error.text);
                            NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.billingConfirmPurchaseError, req, error);
                        }
                    }, ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagInvokeAfter);
                } else {
                    consumeGiftPurchase(purchase, payload.second);
                }
            }
        }
    }

    /**
     * All consumable purchases must be consumed. For us it is a gift.
     * Without confirmation the user will not be able to buy the product again.
     */
    private void consumeGiftPurchase(Purchase purchase, TLRPC.InputStorePaymentPurpose purpose) {
        if (purpose instanceof TLRPC.TL_inputStorePaymentGiftPremium) {
            billingClient.consumeAsync(
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build(), (r, s) -> {
                    });
        }
    }

    /**
     * May occur in extremely rare cases.
     * For example when Google Play decides to update.
     */
    @SuppressWarnings("Convert2MethodRef")
    @Override
    public void onBillingServiceDisconnected() {
        FileLog.d("Billing: Service disconnected");
        int delay = isDisconnected ? 15000 : 5000;
        isDisconnected = true;
        AndroidUtilities.runOnUIThread(() -> startConnection(), delay);
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult setupBillingResult) {
        FileLog.d("Billing: Setup finished with result " + setupBillingResult);
        if (setupBillingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            isDisconnected = false;
            queryProductDetails(Collections.singletonList(PREMIUM_PRODUCT), (billingResult, list) -> {
                FileLog.d("Billing: Query product details finished " + billingResult + ", " + list);
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (ProductDetails details : list) {
                        if (details.getProductId().equals(PREMIUM_PRODUCT_ID)) {
                            PREMIUM_PRODUCT_DETAILS = details;
                        }
                    }
                    if (PREMIUM_PRODUCT_DETAILS == null) {
                        switchToInvoice();
                    } else {
                        NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.billingProductDetailsUpdated);
                    }
                } else {
                    switchToInvoice();
                }
            });
            queryPurchases(BillingClient.ProductType.INAPP, this::onPurchasesUpdated);
            queryPurchases(BillingClient.ProductType.SUBS, this::onPurchasesUpdated);
        } else {
            if (!isDisconnected) {
                switchToInvoice();
            }
        }
    }
}
