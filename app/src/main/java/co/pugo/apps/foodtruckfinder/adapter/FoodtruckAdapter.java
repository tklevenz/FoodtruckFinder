package co.pugo.apps.foodtruckfinder.adapter;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.model.AdListItem;
import co.pugo.apps.foodtruckfinder.model.DividerItem;
import co.pugo.apps.foodtruckfinder.model.FoodtruckItem;
import co.pugo.apps.foodtruckfinder.model.FoodtruckListItem;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;
import co.pugo.apps.foodtruckfinder.ui.DetailActivity;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;

/**
 * Created by tobias on 3.9.2016.
 */
public class FoodtruckAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final String LOG_TAG = FoodtruckAdapter.class.getSimpleName();

  private Context mContext;
  private List<FoodtruckListItem> mListItems;
  private boolean mIsPremium;

  public FoodtruckAdapter(Context context) {
    mContext = context;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (parent instanceof RecyclerView) {
      switch (viewType) {
        case FoodtruckListItem.TYPE_AD:
          return new AdItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foodtruck_ad, parent, false));
        case FoodtruckListItem.TYPE_DIVIDER:
          return new DividerItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foodtruck_divider, parent, false));
        case FoodtruckListItem.TYPE_FOODTRUCK:
          View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foodtruck, parent, false);
          final FoodtruckItemViewHolder vh = new FoodtruckItemViewHolder(view);

          view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
              if (vh.getAdapterPosition() > -1) {

                FoodtruckItem foodtruckItem = (FoodtruckItem) mListItems.get(vh.getAdapterPosition());

                Intent serviceIntent = new Intent(mContext, FoodtruckIntentService.class);
                serviceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_DETAILS);
                serviceIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, foodtruckItem.operatorId);
                mContext.startService(serviceIntent);

                final Intent detailIntent = new Intent(mContext, DetailActivity.class);
                detailIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, foodtruckItem.operatorId);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                  TransitionManager.beginDelayedTransition((ViewGroup) view);
                  int finalRadius = Math.max(view.getWidth(), view.getHeight()) / 2;
                  Animator anim = ViewAnimationUtils.createCircularReveal(view, view.getWidth() / 2, view.getHeight() / 2, 0, finalRadius);
                  view.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.highlightColor));
                  anim.start();
                  anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                      mContext.startActivity(detailIntent);
                      view.setBackgroundColor(Color.WHITE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                  });
                } else {
                  mContext.startActivity(detailIntent);
                }
              }
            }
          });
          return vh;
        default:
          throw new RuntimeException("Could not find viewType");
      }
    } else {
      throw new RuntimeException("Not bound to RecyclerView");
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
    switch (getItemViewType(position)) {
      case FoodtruckListItem.TYPE_AD:
        final AdItemViewHolder adItemViewHolder = (AdItemViewHolder) viewHolder;
        AdRequest request = new AdRequest.Builder().addTestDevice("DBD56745D830B1B5B43BC7C28B5BF8E8").build();
        adItemViewHolder.adView.setAdListener(new AdListener(){
          @Override
          public void onAdLoaded() {
            adItemViewHolder.adView.setVisibility(View.VISIBLE);
            adItemViewHolder.adViewBorder.setVisibility(View.VISIBLE);
          }
        });
        adItemViewHolder.adView.loadAd(request);
        break;
      case FoodtruckListItem.TYPE_DIVIDER:
        DividerItem dividerItem = (DividerItem) mListItems.get(position);
        DividerItemViewHolder dividerItemViewHolder = (DividerItemViewHolder) viewHolder;

        dividerItemViewHolder.itemHeader.setText(dividerItem.date);
        break;
      case FoodtruckListItem.TYPE_FOODTRUCK:
        FoodtruckItem foodtruckItem = (FoodtruckItem) mListItems.get(position);
        FoodtruckItemViewHolder foodtruckItemViewHolder = (FoodtruckItemViewHolder) viewHolder;

        foodtruckItemViewHolder.operatorName.setText(foodtruckItem.name);
        foodtruckItemViewHolder.operatorName.setTypeface(MainActivity.mRobotoSlab);
        foodtruckItemViewHolder.operatorOffer.setText(foodtruckItem.offer);

        if (foodtruckItem.distance > 0) {
          foodtruckItemViewHolder.operatorDistance.setVisibility(View.GONE);
          foodtruckItemViewHolder.hyphen.setVisibility(View.GONE);
          foodtruckItemViewHolder.operatorLocation.setText(Utility.formatDistance(mContext, foodtruckItem.distance) + " - " + foodtruckItem.location);
        } else {
          foodtruckItemViewHolder.operatorDistance.setVisibility(View.GONE);
          foodtruckItemViewHolder.hyphen.setVisibility(View.GONE);
          foodtruckItemViewHolder.operatorLocation.setText(foodtruckItem.region);
        }

        foodtruckItemViewHolder.operatorLogo.setContentDescription(foodtruckItem.name);

        Glide.with(mContext)
                .load(foodtruckItem.logoUrl)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(foodtruckItemViewHolder.operatorLogo);

        break;
    }
  }

  @Override
  public int getItemViewType(int position) {
    return mListItems.get(position).getType();
  }

  @Override
  public int getItemCount() {
    if (null == mListItems) return 0;
    return mListItems.size();
  }

  public void setPremium(boolean isPremium) {
    mIsPremium = isPremium;
    notifyDataSetChanged();
  }

  public void swapCursor(Cursor cursor) {
    mListItems = new ArrayList<>();
    if (cursor != null && cursor.moveToFirst()) {

      Map<String, FoodtruckItem> mapToday = new LinkedHashMap<>();
      Map<String, FoodtruckItem> mapTomorrow = new LinkedHashMap<>();
      Map<String, FoodtruckItem> mapThisWeek = new LinkedHashMap<>();
      List<FoodtruckItem> listNotAvailable = new ArrayList<>();

      do {
        String endDate = cursor.getString(cursor.getColumnIndex(LocationsColumns.END_DATE));

        String name = cursor.getString(cursor.getColumnIndex(OperatorsColumns.NAME));

        FoodtruckItem item = new FoodtruckItem(
                cursor.getString(cursor.getColumnIndex(OperatorsColumns.ID)),
                name,
                cursor.getString(cursor.getColumnIndex(OperatorsColumns.OFFER)),
                cursor.getString(cursor.getColumnIndex(OperatorsColumns.LOGO_URL)),
                cursor.getFloat(cursor.getColumnIndex(LocationsColumns.DISTANCE)),
                cursor.getString(cursor.getColumnIndex(LocationsColumns.LOCATION_NAME)),
                cursor.getString(cursor.getColumnIndex(OperatorsColumns.REGION)));

        if (endDate == null) {
          listNotAvailable.add(item);
        } else if (Utility.isActiveToday(endDate) || Utility.isActiveTomorrow(endDate)) {

          if (Utility.isActiveToday(endDate) && !mapToday.containsKey(name))
            mapToday.put(name, item);

          if (Utility.isActiveTomorrow(endDate) && !mapTomorrow.containsKey(name))
            mapTomorrow.put(name, item);

          if (!mapThisWeek.containsKey(name))
            mapThisWeek.put(name, item);

        } else if (!mapThisWeek.containsKey(name)) {
          mapThisWeek.put(name, item);
        }

      } while (cursor.moveToNext());


      if (mapToday.size() > 0) {
        mListItems.add(new DividerItem(mContext.getString(R.string.divider_today)));
        for (Map.Entry entry : mapToday.entrySet()) {
          mListItems.add((FoodtruckItem) entry.getValue());
        }
        if (!mIsPremium)
          mListItems.add(new AdListItem());
      }

      if (mapTomorrow.size() > 0) {
        mListItems.add(new DividerItem(mContext.getString(R.string.divider_tomorrow)));
        for (Map.Entry entry : mapTomorrow.entrySet()) {
          mListItems.add((FoodtruckItem) entry.getValue());
        }
        if (mapToday.size() == 0 && !mIsPremium)
          mListItems.add(new AdListItem());
      }

      if (mapThisWeek.size() > 0) {
        if (mapToday.size() == 0 && mapTomorrow.size() == 0)
          mListItems.add(new AdListItem());

        mListItems.add(new DividerItem(mContext.getString(R.string.divider_this_week)));
        for (Map.Entry entry : mapThisWeek.entrySet()) {
          mListItems.add((FoodtruckItem) entry.getValue());
        }
      }

      if (listNotAvailable.size() > 0) {
        if (mapToday.size() == 0 && mapTomorrow.size() == 0 && mapThisWeek.size() == 0 && !mIsPremium)
          mListItems.add(new AdListItem());

        mListItems.add(new DividerItem(mContext.getString(R.string.divider_not_available)));
        for (FoodtruckItem item : listNotAvailable) {
          mListItems.add(item);
        }
      }

      mListItems.add(new DividerItem(""));
    }
    notifyDataSetChanged();
  }


  class FoodtruckItemViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.operator_name) TextView operatorName;
    @BindView(R.id.operator_offer) TextView operatorOffer;
    @BindView(R.id.operator_logo) ImageView operatorLogo;
    @BindView(R.id.operator_distance) TextView operatorDistance;
    @BindView(R.id.operator_location_name) TextView operatorLocation;
    @BindView(R.id.operator_location_distance_hyphen) TextView hyphen;
    @BindView(R.id.card_view) CardView cardView;

    FoodtruckItemViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  class DividerItemViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.list_item_header) TextView itemHeader;

    DividerItemViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  class AdItemViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.adView) NativeExpressAdView adView;
    @BindView(R.id.adViewBorder) View adViewBorder;

    AdItemViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
