package co.pugo.apps.foodtruckfinder.adapter;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.MapView;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;
import co.pugo.apps.foodtruckfinder.ui.DetailActivity;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;

/**
 * Created by tobias on 3.9.2016.
 */
public class FoodtruckAdapter extends RecyclerView.Adapter<FoodtruckAdapter.FoodtruckAdapterViewHolder> {

  private static final String LOG_TAG = FoodtruckAdapter.class.getSimpleName();

  private Context mContext;
  private Cursor mCursor;

  public FoodtruckAdapter(Context context) {
    mContext = context;
  }

  @Override
  public FoodtruckAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (parent instanceof RecyclerView) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foodtruck, parent, false);
      final FoodtruckAdapterViewHolder vh = new FoodtruckAdapterViewHolder(view);
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          mCursor.moveToPosition(vh.getAdapterPosition());
          String operatorId = mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.ID));

          if (!(Utility.operatorDetailsExist(mContext, operatorId))) {
            Intent serviceIntent = new Intent(mContext, FoodtruckIntentService.class);
            serviceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_DETAILS);
            serviceIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, operatorId);
            mContext.startService(serviceIntent);
          }

          final Intent detailIntent = new Intent(mContext, DetailActivity.class);
          detailIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, operatorId);

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
                view.setBackgroundColor(Color.TRANSPARENT);
                mContext.startActivity(detailIntent);
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
      });
      return vh;
    } else {
      throw new RuntimeException("Not bound to RecyclerView");
    }
  }

  @Override
  public void onBindViewHolder(FoodtruckAdapterViewHolder holder, int position) {
    mCursor.moveToPosition(position);

    String operatorName = mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.NAME));
    holder.operatorName.setText(operatorName);
    holder.operatorName.setTypeface(MainActivity.mRobotoSlab);
    holder.operatorOffer.setText(mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.OFFER)));

    float distance = mCursor.getFloat(mCursor.getColumnIndex(LocationsColumns.DISTANCE));
    if (distance > 0) {
      holder.operatorDistance.setText(
              Utility.formatDistance(mContext, distance));
      holder.operatorLocation.setText(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.LOCATION_NAME)));
    } else {
      holder.operatorDistance.setVisibility(View.GONE);
      holder.hyphen.setVisibility(View.GONE);
      holder.operatorLocation.setText(mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.REGION)));
    }

    holder.operatorLogo.setContentDescription(operatorName);
    Glide.with(mContext)
            .load(mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.LOGO_URL)))
            .into(holder.operatorLogo);

  }

  @Override
  public int getItemCount() {
    if (null == mCursor) return 0;
    return mCursor.getCount();
  }

  public void swapCursor(Cursor cursor) {
    mCursor = cursor;
    notifyDataSetChanged();
  }


  public class FoodtruckAdapterViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.operator_name) TextView operatorName;
    @BindView(R.id.operator_offer) TextView operatorOffer;
    @BindView(R.id.operator_logo) ImageView operatorLogo;
    @BindView(R.id.operator_distance) TextView operatorDistance;
    @BindView(R.id.operator_location_name) TextView operatorLocation;
    @BindView(R.id.map_container) View mapContainer;
    @BindView(R.id.map_view) MapView mapView;
    @BindView(R.id.map_divider) View mapDivider;
    @BindView(R.id.operator_location_distance_hyphen) TextView hyphen;

    public FoodtruckAdapterViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

}
