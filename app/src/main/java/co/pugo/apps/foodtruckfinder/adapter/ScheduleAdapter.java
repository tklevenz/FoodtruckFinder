package co.pugo.apps.foodtruckfinder.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;
import co.pugo.apps.foodtruckfinder.ui.MapActivity;

/**
 * Created by tobias on 5.9.2016.
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleAdapterViewHolder> {
  private Cursor mCursor;
  private Context mContext;
  private String mAddress = "";

  public ScheduleAdapter(Context context) {
    mContext = context;
  }

  @Override
  public ScheduleAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (parent instanceof RecyclerView) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
      return new ScheduleAdapterViewHolder(view);
    } else {
      throw new RuntimeException("Not bound to RecyclerView");
    }
  }

  @Override
  public void onBindViewHolder(ScheduleAdapterViewHolder holder, final int position) {
    mCursor.moveToPosition(position);
    final String startDate = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.START_DATE));
    final String endDate = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.END_DATE));
    final String address = String.format(
            mContext.getString(R.string.address),
            mCursor.getString(mCursor.getColumnIndex(LocationsColumns.STREET)),
            mCursor.getString(mCursor.getColumnIndex(LocationsColumns.NUMBER)),
            mCursor.getString(mCursor.getColumnIndex(LocationsColumns.CITY))
    );

    if (address.equals(mAddress)) {
      holder.locationContainer.setVisibility(View.GONE);
    } else {
      mAddress = address;
      holder.scheduleLocation.setTypeface(MainActivity.mRobotoSlab);
      holder.scheduleLocation.setText(address);
      holder.scheduleDistance.setTypeface(MainActivity.mRobotoSlab);
      holder.scheduleDistance.setText(Utility.formatDistance(mContext, mCursor.getFloat(mCursor.getColumnIndex(LocationsColumns.DISTANCE))));

      holder.locationContainer.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Uri navUri = Uri.parse("google.navigation:q=" + address);

          final Intent mapIntent = new Intent(Intent.ACTION_VIEW, navUri);
          mapIntent.setPackage("com.google.android.apps.maps");

          if (navUri != null)
            mContext.startActivity(mapIntent);
        }
      });
    }

    holder.scheduleTime.setText(String.format(
            mContext.getString(R.string.schedule_time),
            Utility.getFormattedTime(startDate),
            Utility.getFormattedTime(endDate)
    ));

    holder.scheduleDate.setText(Utility.getFormattedDate(startDate, mContext));

    holder.dateContainer.setContentDescription(mContext.getString(R.string.add_to_calendar));

    holder.dateContainer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent calendarIntent = new Intent(Intent.ACTION_EDIT);
        calendarIntent.setType("vnd.android.cursor.item/event");
        calendarIntent.putExtra(CalendarContract.Events.TITLE,
                mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_NAME)));
        calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                Utility.getDateMillis(startDate));
        calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                Utility.getDateMillis(endDate));
        calendarIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, address);
        calendarIntent.putExtra(CalendarContract.Events.ALL_DAY, false);
        mContext.startActivity(calendarIntent);
      }
    });
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

  public class ScheduleAdapterViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.schedule_date) TextView scheduleDate;
    @BindView(R.id.schedule_location) TextView scheduleLocation;
    @BindView(R.id.schedule_time) TextView scheduleTime;
    @BindView(R.id.schedule_distance) TextView scheduleDistance;
    @BindView(R.id.schedule_location_container) View locationContainer;
    @BindView(R.id.schedule_date_container) View dateContainer;

    public ScheduleAdapterViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

}