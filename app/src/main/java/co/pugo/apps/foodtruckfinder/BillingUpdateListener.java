package co.pugo.apps.foodtruckfinder;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;

import java.util.List;

/**
 * Created by tobia on 5.10.2017.
 */

public interface BillingUpdateListener {
  void onBillingClientSetupFinished();
  void onConsumeFinished(String token, @BillingClient.BillingResponse int result);
  void onPurchasesUpdated(List<Purchase> purchases);
}
