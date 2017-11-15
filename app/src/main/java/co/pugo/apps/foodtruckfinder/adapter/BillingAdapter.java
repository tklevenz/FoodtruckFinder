package co.pugo.apps.foodtruckfinder.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.SkuDetails;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.ui.BillingActivity;

/**
 * Created by tobia on 10.10.2017.
 */

public class BillingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private List<SkuDetails> mSkuDetails;
  private BillingActivity mActivity;
  private List<String> mPurchasedSkus;

  public BillingAdapter(BillingActivity activity) {
    mActivity = activity;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (parent instanceof RecyclerView) {
      return new SkuViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sku, parent, false));
    }
    return null;
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    SkuViewHolder skuViewHolder = (SkuViewHolder) holder;
    final SkuDetails skuDetails = mSkuDetails.get(position);
    skuViewHolder.skuTitle.setText(skuDetails.getTitle());
    skuViewHolder.skuPrice.setText(skuDetails.getPrice());
    skuViewHolder.skuDesc.setText(skuDetails.getDescription());

    if (mPurchasedSkus != null) {
      skuViewHolder.btnBuy.setEnabled(false);
      skuViewHolder.btnBuy.setBackgroundColor(Color.TRANSPARENT);
      skuViewHolder.btnBuy.setTextColor(mActivity.getResources().getColor(R.color.colorAccent));
      skuViewHolder.btnBuy.setText(R.string.btn_buy_purchased);
    } else {
      skuViewHolder.btnBuy.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mActivity.getBillingManager().initiatePurchaseFlow(skuDetails.getSku(), skuDetails.getType());
        }
      });
    }
  }

  @Override
  public int getItemCount() {
    return mSkuDetails != null ? mSkuDetails.size() : 0;
  }

  public void setSkuDetails(List<SkuDetails> skuDetails) {
    mSkuDetails = skuDetails;
    notifyDataSetChanged();
  }

  public void setPurchasedSkus(List<String> purchasedSkus) {
    mPurchasedSkus = purchasedSkus;
  }

  class SkuViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.sku_title) TextView skuTitle;
    @BindView(R.id.sku_price) TextView skuPrice;
    @BindView(R.id.sku_description) TextView skuDesc;
    @BindView(R.id.btn_buy) Button btnBuy;

    public SkuViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
