package co.pugo.apps.foodtruckfinder.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;

/**
 * Created by tobias on 18.9.2016.
 */
public class FoodtruckWidgetService extends RemoteViewsService {
  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return new RemoteViewsFactory() {
      private Cursor cursor = null;

      @Override
      public void onCreate() {

      }

      @Override
      public void onDataSetChanged() {
        if (cursor != null)
          cursor.close();

        cursor = getContentResolver().query(
                FoodtruckProvider.Locations.CONTENT_URI,
                new String[]{
                        LocationsColumns.OPERATOR_NAME,
                        LocationsColumns.DISTANCE,
                        LocationsColumns.OPERATOR_LOGO_URL},
                LocationsColumns.OPERATOR_ID + " IS NOT NULL) GROUP BY (" + LocationsColumns.OPERATOR_ID,
                null,
                LocationsColumns.DISTANCE + " ASC LIMIT 10");
      }

      @Override
      public void onDestroy() {
        if (cursor != null) {
          cursor.close();
          cursor = null;
        }
      }

      @Override
      public int getCount() {
        return cursor == null ? 0 : cursor.getCount();
      }

      @Override
      public RemoteViews getViewAt(int i) {
        if (i == AdapterView.INVALID_POSITION || cursor == null || !cursor.moveToPosition(i))
          return null;

        Log.d("WidgetService", cursor.getString(cursor.getColumnIndex(LocationsColumns.DISTANCE)));

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_list_item);

        remoteViews.setTextViewText(R.id.operator_name,
                cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_NAME)));
         remoteViews.setTextViewText(R.id.operator_distance,
                Utility.formatDistance(getApplicationContext(), cursor.getFloat(cursor.getColumnIndex(LocationsColumns.DISTANCE))));



        Bitmap operatorLogo = null;
        try {
          operatorLogo = Glide.with(FoodtruckWidgetService.this)
                  .load(cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL)))
                  .asBitmap()
                  .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                  .get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
        remoteViews.setImageViewBitmap(R.id.operator_logo, operatorLogo);

        return remoteViews;
      }

      @Override
      public RemoteViews getLoadingView() {
        return null;
      }

      @Override
      public int getViewTypeCount() {
        return 1;
      }

      @Override
      public long getItemId(int i) {
        return i;
      }

      @Override
      public boolean hasStableIds() {
        return true;
      }
    };
  }
}
