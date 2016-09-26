package co.pugo.apps.foodtruckfinder.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
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
        public void onClick(View view) {
          mCursor.moveToPosition(vh.getAdapterPosition());
          String operatorId = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_ID));
          String logoUrl = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL));

          if (!(Utility.operatorDetailsExist(mContext, operatorId))) {
            Intent serviceIntent = new Intent(mContext, FoodtruckIntentService.class);
            serviceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_DETAILS);
            serviceIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, operatorId);
            mContext.startService(serviceIntent);
          }

          Intent detailIntent = new Intent(mContext, DetailActivity.class);
          detailIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, operatorId);
          detailIntent.putExtra(DetailActivity.LOGO_URL_TAG, logoUrl);
          mContext.startActivity(detailIntent);
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
    String operatorName = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_NAME));
    holder.operatorName.setText(operatorName);
    holder.operatorName.setTypeface(MainActivity.mRobotoSlab);
    holder.operatorOffer.setText(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_OFFER)));

    holder.operatorDistance.setText(
            Utility.formatDistance(mContext, mCursor.getFloat(mCursor.getColumnIndex(LocationsColumns.DISTANCE))));

    holder.operatorLogo.setContentDescription(operatorName);
    Glide.with(mContext)
            .load(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL)))
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

    public FoodtruckAdapterViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

}
