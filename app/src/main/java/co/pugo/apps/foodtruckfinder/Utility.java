package co.pugo.apps.foodtruckfinder;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;


/**
 * Created by tobias on 8.9.2016.
 */
public class Utility {

  public static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssZZZ";
  public static final String LAST_UPDATED_ON = "last_updated_on";
  public static final String KEY_PREF_LATITUDE = "pref_latitude";
  public static final String KEY_PREF_LONGITUDE = "pref_longitude";
  public static final String KEY_PREF_LOCATION = "pref_location";

  public static String getFormattedDate(String string, Context context) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d");

    Date date = parseDateString(string);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);

    Calendar today = Calendar.getInstance();
    today.setTimeZone(calendar.getTimeZone());

    if (calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
      return context.getString(R.string.today);
    else if (calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1)
      return context.getString(R.string.tomorrow);

    return dateFormat.format(date);
  }

  public static String getDateNow() {
    Calendar today = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.format(today.getTime());
  }

  public static String getFormattedTime(String string) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("H:mm");
    return dateFormat.format(parseDateString(string));
  }

  public static Date parseDateString(String string) {
    SimpleDateFormat format = new SimpleDateFormat(ISO_8601);
    Date date = null;
    try {
      date = format.parse(string);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return date;
  }

  public static long getDateMillis(String string) {
    Date date = parseDateString(string);
    return date.getTime();
  }

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    return networkInfo != null && networkInfo.isConnected();
  }

  public static void setLastUpdatePref(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor prefsEditor = prefs.edit();
    Calendar calendar = Calendar.getInstance();
    prefsEditor.putInt(LAST_UPDATED_ON, calendar.get(Calendar.DAY_OF_YEAR));
    prefsEditor.apply();
  }

  public static boolean isOutOfDate(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int lastUpdatedOn = prefs.getInt(LAST_UPDATED_ON, 0);
    Calendar calendar = Calendar.getInstance();
    return lastUpdatedOn != calendar.get(Calendar.DAY_OF_YEAR);
  }

  public static boolean operatorDetailsExist(Context context, String operatorId) {
    Cursor cursor = context.getContentResolver()
            .query(FoodtruckProvider.OperatorDetails.withOperatorId(operatorId),
                    new String[]{OperatorDetailsColumns.OPERATOR_ID},
                    null,
                    null,
                    null
            );
    boolean exists = false;
    if (cursor != null) {
      exists = cursor.moveToFirst();
      cursor.close();
    }
    return exists;
  }

  public static boolean operatorsExist(Context context) {
    Cursor cursor = context.getContentResolver().query(FoodtruckProvider.Operators.CONTENT_URI,
            new String[]{OperatorsColumns.ID},
            null,
            null,
            null);
    boolean exists = false;
    if (cursor != null) {
      exists = cursor.moveToFirst();
      cursor.close();
    }
    return exists;
  }

  public static float getOperatorDistance(Context context, double latitude, double longitude) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Location deviceLocation = new Location("");
    deviceLocation.setLatitude(prefs.getFloat(KEY_PREF_LATITUDE, 0f));
    deviceLocation.setLongitude(prefs.getFloat(KEY_PREF_LONGITUDE, 0f));
    Location operatorLocation = new Location("");
    operatorLocation.setLatitude(latitude);
    operatorLocation.setLongitude(longitude);
    return deviceLocation.distanceTo(operatorLocation);
  }

  public static void updateLocationSharedPref(Context context, Location location) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Location lastLocation = new Location("");
    lastLocation.setLatitude(prefs.getFloat(KEY_PREF_LATITUDE, 0f));
    lastLocation.setLongitude(prefs.getFloat(KEY_PREF_LONGITUDE, 0f));

    // change stored location if location changed more then 1000m
    if (lastLocation.distanceTo(location) >= 1000 || prefs.getString(Utility.KEY_PREF_LOCATION, "").equals("")) {
      SharedPreferences.Editor prefsEdit = prefs.edit();
      prefsEdit.putFloat(Utility.KEY_PREF_LATITUDE, (float) location.getLatitude());
      prefsEdit.putFloat(Utility.KEY_PREF_LONGITUDE, (float) location.getLongitude());
      prefsEdit.putString(Utility.KEY_PREF_LOCATION, location.toString());
      prefsEdit.apply();

      Log.d("Utility", "run update distance task...");
      // update distance in database
      new UpdateDistanceTask(context).execute();
    }
  }

  public static void updateLocationSharedPref(Context context, double latitude, double longitude) {
    Location location = new Location("");
    location.setLatitude(latitude);
    location.setLongitude(longitude);
    updateLocationSharedPref(context, location);
  }

  public static void setToolbarTitleFont(Toolbar toolbar) {
    for (int i = 0; i < toolbar.getChildCount(); i++) {
      View v = toolbar.getChildAt(i);
      if (v instanceof TextView && ((TextView) v).getText().equals(toolbar.getTitle())) {
        ((TextView) v).setTypeface(MainActivity.mRobotoSlab);
      }
    }
  }

  public static String formatDistance(Context context, Float distance) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String distanceUnits = prefs.getString(context.getString(R.string.pref_distance_unit_key),
            context.getString(R.string.pref_unit_killometers));
    if (distanceUnits.equals(context.getString(R.string.pref_unit_killometers))) {
      return String.format(context.getString(R.string.distance_km), (int) (distance / 1000));
    } else {
      return String.format(context.getString(R.string.distance_miles), (int) (distance * 0.621371 / 1000));
    }
  }

  public static Bitmap colorBitmap(Bitmap marker, int color) {
    Bitmap markerColored = Bitmap.createBitmap(marker.getWidth(), marker.getHeight(), marker.getConfig());
    Paint paint = new Paint();
    paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
    Canvas c = new Canvas(markerColored);
    c.drawBitmap(marker, 0, 0, paint);
    return markerColored;
  }

  public static void loadMapMarkerIcon(final Context context, final Marker marker, String iconUrl, final int size, final Bitmap markerBg) {
    Glide.with(context).load(iconUrl)
            .asBitmap().fitCenter().into(new SimpleTarget<Bitmap>(size, size) {
      @Override
      public void onResourceReady(Bitmap logo, GlideAnimation<? super Bitmap> glideAnimation) {
        Bitmap bmMarkerAndLogo = Bitmap.createBitmap(markerBg.getWidth(), markerBg.getHeight(), markerBg.getConfig());
        Canvas canvas = new Canvas(bmMarkerAndLogo);
        canvas.drawBitmap(markerBg, new Matrix(), null);
        canvas.drawBitmap(logo, (markerBg.getWidth() - logo.getWidth()) / 2, (markerBg.getHeight() - logo.getHeight()) / 2, null);

        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(addDropShadow(bmMarkerAndLogo, Color.GRAY, 10, 0, 2));
        marker.setIcon(icon);
      }
    });
  }

  public static Bitmap addDropShadow(Bitmap bm, int color, int size, int dx, int dy) {
    int dstWidth = bm.getWidth() + dx;
    int dstHeight = bm.getHeight() + dy;
    Bitmap mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8);

    Matrix scaleToFit = new Matrix();
    RectF src = new RectF(0, 0, bm.getWidth(), bm.getHeight());
    RectF dst = new RectF(0, 0, dstWidth - dx, dstHeight - dy);
    scaleToFit.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);

    Matrix dropShadow = new Matrix(scaleToFit);
    dropShadow.postTranslate(dx, dy);

    Canvas maskCanvas = new Canvas(mask);
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    maskCanvas.drawBitmap(bm, scaleToFit, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
    maskCanvas.drawBitmap(bm, dropShadow, paint);

    BlurMaskFilter filter = new BlurMaskFilter(size, BlurMaskFilter.Blur.NORMAL);
    paint.reset();
    paint.setAntiAlias(true);
    paint.setColor(color);
    paint.setMaskFilter(filter);
    paint.setFilterBitmap(true);

    Bitmap ret = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
    Canvas retCanvas = new Canvas(ret);
    retCanvas.drawBitmap(mask, 0, 0, paint);
    retCanvas.drawBitmap(bm, (dstWidth - bm.getWidth()) / 2, (dstHeight - bm.getHeight()) / 2, null);
    mask.recycle();
    return ret;
  }


  public static int getStatusBarHeight(Context context) {
    int result = 0;
    int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      result = context.getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }


  private static class UpdateDistanceTask extends AsyncTask<Void, Void, Integer> {
    private Cursor mCursor;
    private Context mContext;

    public UpdateDistanceTask(Context context) {
      mContext = context;
      mCursor = context.getContentResolver().query(
              FoodtruckProvider.Locations.CONTENT_URI,
              new String[]{
                      LocationsColumns.OPERATOR_ID,
                      LocationsColumns.LONGITUDE,
                      LocationsColumns.LATITUDE
              },
              null,
              null,
              null);
    }

    @Override
    protected Integer doInBackground(Void... voids) {
      int rowsUpdated = 0;
      if (mCursor != null && mCursor.moveToFirst()) {
        while (mCursor.moveToNext()) {
          ContentValues values = new ContentValues();
          values.put(LocationsColumns.DISTANCE,
                  Utility.getOperatorDistance(mContext,
                          mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LATITUDE)),
                          mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LONGITUDE))));

          rowsUpdated += mContext.getContentResolver().update(
                  FoodtruckProvider.Locations.withOperatorId(
                          mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_ID))),
                  values,
                  null,
                  null);
        }
      }
      return rowsUpdated;
    }

    @Override
    protected void onPostExecute(Integer integer) {
      Log.d("UpdateDistanceTask", "Updated " + integer + " rows...");
    }
  }
}
