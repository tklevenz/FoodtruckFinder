package co.pugo.apps.foodtruckfinder;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
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

  public static String getFormattedDate(String string) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d");
    return dateFormat.format(parseDateString(string));
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
      return String.format(context.getString(R.string.distance_km), (int)(distance / 1000));
    } else {
      return String.format(context.getString(R.string.distance_miles), (int)(distance * 0.621371 / 1000));
    }
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
