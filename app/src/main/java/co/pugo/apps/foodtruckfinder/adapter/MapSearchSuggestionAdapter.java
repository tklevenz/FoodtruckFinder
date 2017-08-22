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
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.ui.MapActivity;

/**
 * Created by tobia on 22.8.2017.
 */

public class MapSearchSuggestionAdapter extends SimpleCursorAdapter {

  public MapSearchSuggestionAdapter(Context context) {
    super(context, R.layout.search_view_maps_suggestion, null,
            new String[]{
                    LocationsColumns.OPERATOR_NAME,
                    LocationsColumns.LOCATION_NAME
            },
            new int[]{
                    R.id.search_suggestion,
                    R.id.search_suggestion_location
            }, 0);
  }

  @Override
  public void bindView(View view, final Context context, final Cursor cursor) {
    super.bindView(view, context, cursor);

    view.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(MapActivity.LATITUDE_TAG, cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LATITUDE)));
        intent.putExtra(MapActivity.LONGITUDE_TAG, cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LONGITUDE)));
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
      }
    });

    ImageView imageView = (ImageView) view.findViewById(R.id.search_suggestion_logo);

    Glide.with(context)
            .load(cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL)))
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
            .into(imageView);
  }
}
