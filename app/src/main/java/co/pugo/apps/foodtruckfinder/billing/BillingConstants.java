package co.pugo.apps.foodtruckfinder.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.SkuType;
import java.util.Arrays;
import java.util.List;

/**
 * Static fields and methods useful for billing
 */
public final class BillingConstants {
  // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
  public static final String SKU_PREMIUM = "premium";

  private static final String[] IN_APP_SKUS = {SKU_PREMIUM};

  private BillingConstants(){}

  /**
   * Returns the list of all SKUs for the billing type specified
   */
  public static List<String> getSkuList(@BillingClient.SkuType String billingType) {
    return (billingType.equals(SkuType.INAPP)) ? Arrays.asList(IN_APP_SKUS) : null;
  }
}

