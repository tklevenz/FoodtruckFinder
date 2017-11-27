package co.pugo.apps.foodtruckfinder.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.ui.MapActivity;

/**
 * Created by tobia on 22.8.2017.
 */

public class MapSearchSuggestionAdapter extends SimpleCursorAdapter {

  public MapSearchSuggestionAdapter(Context context) {
    super(context, R.layout.search_view_maps_suggestion, null, new String[]{}, new int[]{}, 0);
  }


  @Override
  public void bindView(View view, final Context context, Cursor cursor) {

    final double latitude = cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LATITUDE));
    final double longitude = cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LONGITUDE));

    view.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(MapActivity.LATITUDE_TAG, latitude);
        intent.putExtra(MapActivity.LONGITUDE_TAG, longitude);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
      }
    });

    TextView suggestion = (TextView) view.findViewById(R.id.search_suggestion);
    suggestion.setText(cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_NAME)));

    TextView location = (TextView) view.findViewById(R.id.search_suggestion_location);
    location.setText(Utility.formatDistance(context, cursor.getFloat(cursor.getColumnIndex(LocationsColumns.DISTANCE))) + " - " +
                      cursor.getString(cursor.getColumnIndex(LocationsColumns.LOCATION_NAME)));

    ImageView logo = (ImageView) view.findViewById(R.id.search_suggestion_logo);
    Glide.with(context)
            .load(cursor.getString(cursor.getColumnIndex(OperatorsColumns.LOGO_URL)))
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
            .into(logo);
  }
}
