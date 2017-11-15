package co.pugo.apps.foodtruckfinder.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.adapter.BillingAdapter;
import co.pugo.apps.foodtruckfinder.billing.BillingManager;
import co.pugo.apps.foodtruckfinder.billing.BillingProvider;

public class BillingActivity extends AppCompatActivity {
  private static final String TAG = BillingActivity.class.getSimpleName();
  @BindView(R.id.recyclerview_billing) RecyclerView mRecyclerView;
  @BindView(R.id.toolbar) Toolbar toolbar;

  private BillingAdapter mAdapter;
  private BillingManager mBillingManager;
  private final String[] SKUS = {"pro_2"};
  private List<String> purchasedSkus = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_billing);

    ButterKnife.bind(this);


    mBillingManager = new BillingManager(this, new BillingManager.BillingUpdatesListener() {
      @Override
      public void onBillingClientSetupFinished() {
        onManagerReady();
      }

      @Override
      public void onConsumeFinished(String token, @BillingClient.BillingResponse int result) {

      }

      @Override
      public void onPurchasesUpdated(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
          for (String SKU : SKUS) {
            if (purchase.getSku().equals(SKU)) {
              purchasedSkus.add(SKU);
            }
          }
        }
      }
    });

    mAdapter = new BillingAdapter(this);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mRecyclerView.setAdapter(mAdapter);


    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  private void onManagerReady() {
    mBillingManager.querySkuDetailsAsync(BillingClient.SkuType.INAPP, Arrays.asList(SKUS), new SkuDetailsResponseListener() {
      @Override
      public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
        if (responseCode != BillingClient.BillingResponse.OK) {
          Log.w(TAG, "Unsuccessful query, Error code: " + responseCode);
        } else if (skuDetailsList != null
                   && skuDetailsList.size() > 0) {

          mAdapter.setPurchasedSkus(purchasedSkus);
          mAdapter.setSkuDetails(skuDetailsList);
        } else {
          Log.d(TAG, "No SkuDetails found...");
        }
      }
    });
  }

  public BillingManager getBillingManager() {
    return mBillingManager;
  }

  @Override
  protected void onResume() {
    super.onResume();
  }
}
