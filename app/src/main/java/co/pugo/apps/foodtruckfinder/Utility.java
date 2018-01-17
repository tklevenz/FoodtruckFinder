package co.pugo.apps.foodtruckfinder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import co.pugo.apps.foodtruckfinder.data.FavouritesColumns;
import co.pugo.apps.foodtruckfinder.data.FoodtruckDatabase;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.ImpressionsColumns;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import co.pugo.apps.foodtruckfinder.data.RegionsColumns;
import co.pugo.apps.foodtruckfinder.data.TagsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint("SimpleDateFormat")
public class Utility {

  public static final String FOODTRUCK_SERVICE_RESPONSE = "Foodtruck-Service-Response";
  public static final String MESSAGE_UPDATE_DISTANCE_TASK = "message_update_distance";
  private static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";
  private static final String LOCATION_LAST_UPDATED = "location_last_updated";
  private static final String OPERATORS_LAST_UPDATED = "operators_last_updated";
  private static final String REGIONS_LAST_UPDATED = "regions_last_updated";
  public static final String KEY_PREF_LATITUDE = "pref_latitude";
  public static final String KEY_PREF_LONGITUDE = "pref_longitude";
  private static final String KEY_PREF_LOCATION = "pref_location";

  public static final String KEY_IS_FIRST_LAUNCH_PREF = "pref_first_launch";
  public static final String LAST_IMAGE_TIMESTAMP_PREF = "last_image_time_pref";
  public static final boolean MESSAGE_SUCCESS = true;

  /**
   * gets formatted date
   * @param dateString parsable date string
   * @param context ApplicationContext
   * @return Today, Tomorrow or dateString
   */
  public static String getFormattedDate(String dateString, Context context) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d");

    Date date = parseDateString(dateString);

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(calendar.getTimeZone());
    Date today = calendar.getTime();
    calendar.add(Calendar.DAY_OF_YEAR, 1);
    Date tomorrow = calendar.getTime();

    if (dateFormat.format(date).equals(dateFormat.format(today)))
      return context.getString(R.string.today);
    else if (dateFormat.format(date).equals(dateFormat.format(tomorrow)))
      return context.getString(R.string.tomorrow);

    return dateFormat.format(date);
  }

  /**
   * get now as date
   * @return dateString formatted as yyyy-MM-dd
   */
  public static String getDateNow() {
    Calendar today = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.format(today.getTime());
  }

  /**
   * get now as date
   * @return dateString formatted as yyyy-MM-dd
   */
  public static String getTimeNow() {
    Calendar today = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601);
    Log.d("Utility", dateFormat.format(today.getTime()));
    return dateFormat.format(today.getTime());
  }

  /**
   * get tomorrow as date
   * @return dateString formatted as yyyy-MM-dd
   */
  public static String getDateTomorrow() {
    Calendar today = Calendar.getInstance();
    today.add(Calendar.DAY_OF_YEAR, 1);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.format(today.getTime());
  }

  /**
   * get day after tomorrow as date
   * @return dateString formatted as yyyy-MM-dd
   */
  public static String getDateDayAfterTomorrow() {
    Calendar today = Calendar.getInstance();
    today.add(Calendar.DAY_OF_YEAR, 2);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.format(today.getTime());
  }

  /**
   * get formatted time from dateString
   * @param dateString string that can be parsed as SimpleDate
   * @return timeString
   */
  public static String getFormattedTime(Context context, String dateString) {
    SimpleDateFormat dateFormat = android.text.format.DateFormat.is24HourFormat(context)
            ? new SimpleDateFormat("H:mm") : new SimpleDateFormat("h:mm a");
    dateFormat.setTimeZone(getTzFromString(dateString));
    return dateFormat.format(parseDateString(dateString));
  }

  /**
   * checks if timezone in dateString is devices local timezone
   * @param dateString String that can be parsed as SimpleDate
   * @return true if tz is same as local
   */
  public static boolean isLocalTime(String dateString) {
    TimeZone deviceTz = TimeZone.getDefault();
    DateFormat dateFormat = new SimpleDateFormat(ISO_8601);
    try {
      dateFormat.parse(dateString);
      dateFormat.setTimeZone(getTzFromString(dateString));
      TimeZone locationTz = dateFormat.getTimeZone();
      Date now = new Date();

      return deviceTz.getDisplayName(deviceTz.inDaylightTime(now), TimeZone.SHORT).equals(locationTz.getDisplayName(locationTz.inDaylightTime(now), TimeZone.SHORT));
    } catch (ParseException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * extract timezone from dateString
   * @param dateString String in ISO_8601
   * @return timeZone string
   */
  private static TimeZone getTzFromString(String dateString) {
    return TimeZone.getTimeZone("GMT" + TextUtils.substring(dateString, 19, 25));
  }

  /**
   * parse dateString to Date
   * @param dateString in ISO_8601
   * @return Date object
   */
  private static Date parseDateString(String dateString) {
    SimpleDateFormat format = new SimpleDateFormat(ISO_8601);

    Date date = null;
    try {
      date = format.parse(dateString);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return date;
  }

  /**
   * check if dateString is today
   * @param dateString that can be parsed as Date
   * @return true if date is today
   */
  public static boolean isToday(String dateString) {
    Date date = parseDateString(dateString);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.format(date).equals(getDateNow());
  }

  /**
   * check if a locations "now" is between a locations start_date and end_date
   * @param startDate location start date
   * @param endDate location end date
   * @return true if active now
   */
  public static boolean isActiveNow(String startDate, String endDate) {
    Date sd = parseDateString(startDate);
    Date ed = parseDateString(endDate);
    Date now = Calendar.getInstance().getTime();
    return now.getTime() > sd.getTime() && now.getTime() < ed.getTime();
  }

  /**
   * check if endDate is today and in the future
   * @param endDate location end date
   * @return true if active today
   */
  public static boolean isActiveToday(String endDate) {
    Date ed = parseDateString(endDate);
    Calendar calEd =  Calendar.getInstance();
    calEd.setTime(ed);

    Calendar cal = Calendar.getInstance();

    return cal.get(Calendar.DAY_OF_YEAR) == calEd.get(Calendar.DAY_OF_YEAR) && cal.getTimeInMillis() < calEd.getTimeInMillis();
  }

  /**
   * check if endDate is tomorrow
   * @param endDate location end date
   * @return true if active today
   */
  public static boolean isActiveTomorrow(String endDate) {
    Date ed = parseDateString(endDate);
    Calendar calEd = Calendar.getInstance();
    calEd.setTime(ed);

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, 1);

    return cal.get(Calendar.DAY_OF_YEAR) == calEd.get(Calendar.DAY_OF_YEAR);
  }

  /**
   * check if operator is currently active
   * @param context application context
   * @param operatorId id of operator
   * @return true if active
   */
  public static boolean isActive(Context context, String operatorId) {
    Cursor cursor = context.getContentResolver().query(
            FoodtruckProvider.Operators.CONTENT_URI_JOINED,
            null,
            FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID + " = ? AND " + LocationsColumns.START_DATE + " IS NOT NULL",
            new String[]{
                    operatorId
            },
            null);

    if (cursor != null && cursor.getCount() > 0) {
      cursor.close();
      return true;
    }

    return false;
  }

  /**
   * check if network is available
   * @param context ApplicationContext
   * @return true if available
   */
  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    return networkInfo != null && networkInfo.isConnected();
  }

  /**
   * Sets SharedPreferences when operator/location data has last been updated
   * @param context ApplicationContext
   * @param task id of TASK that has been updated
   */
  public static void setLastUpdatePref(Context context, int task) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor prefsEditor = prefs.edit();

    switch (task) {
      case FoodtruckTaskService.TASK_FETCH_OPERATORS:
        prefsEditor.putLong(OPERATORS_LAST_UPDATED, currentDayMillis());
        break;
      case FoodtruckTaskService.TASK_FETCH_LOCATIONS:
        prefsEditor.putLong(LOCATION_LAST_UPDATED, currentDayMillis());
        break;
      case FoodtruckTaskService.TASK_FETCH_REGIONS:
        prefsEditor.putLong(REGIONS_LAST_UPDATED, currentDayMillis());
        break;
    }

    prefsEditor.apply();
  }


  /**
   * check if data is out of date, week
   * @param context ApplicationContext
   * @param task id of TASK that has been updated
   * @return true if location has not been updated in a day, or operators in a week
   */
  public static boolean isOutOfDate(Context context, int task) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Log.d("Utility", "last updated: " + prefs.getLong(LOCATION_LAST_UPDATED, 0));
    long lastUpdated;
    switch (task) {
      case FoodtruckTaskService.TASK_FETCH_LOCATIONS:
        /*
        lastUpdated = prefs.getLong(LOCATION_LAST_UPDATED, 0);
        return lastUpdated == 0 || lastUpdated != currentDayMillis();
        */
        return true; // always updating location
      case FoodtruckTaskService.TASK_FETCH_OPERATORS:
        lastUpdated = prefs.getLong(OPERATORS_LAST_UPDATED, 0);
        return lastUpdated == 0 || lastUpdated - currentDayMillis() >= 7 * 24 * 3600 * 1000;
      case FoodtruckTaskService.TASK_FETCH_REGIONS:
        lastUpdated = prefs.getLong(REGIONS_LAST_UPDATED, 0);
        return lastUpdated == 0 || lastUpdated - currentDayMillis() >= 7 * 24 * 3600 * 1000;
      default:
        return true;
    }
  }

  /**
   * get currentDayInMillis to share in last updated shared preference
   * @return long of current day in millis based on currentTimeMillis
   */
  public static long currentDayMillis() {
    return getDayMillis(System.currentTimeMillis());
  }

  /**
   * get day from date in millis
   * @param date date in millis
   * @return long of day in millis
   */
  public static long getDayMillis(long date) {
    return date / (1000 * 3600 * 24) * (1000 * 3600 * 24);
  }

  /**
   * check if data for a specific contentUri exists
   * @param context ApplicationContext
   * @param contentUri uri of content provider
   * @return true if data has been found for specific uri
   */
  public static boolean dataExists(Context context, Uri contentUri) {
    Cursor cursor = context.getContentResolver().query(contentUri, new String[]{"_id"}, null, null, null);
    boolean exists = false;
    if (cursor != null) {
      exists = cursor.moveToFirst();
      cursor.close();
    }
    return exists;
  }

  /**
   * calculate distance between device and operator location
   * @param context ApplicationContext
   * @param latitude device latitude
   * @param longitude device longitude
   * @return distance in meters
   */
  private static float getOperatorDistance(Context context, double latitude, double longitude) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Location deviceLocation = new Location("");
    deviceLocation.setLatitude(prefs.getFloat(KEY_PREF_LATITUDE, 0f));
    deviceLocation.setLongitude(prefs.getFloat(KEY_PREF_LONGITUDE, 0f));
    Location operatorLocation = new Location("");
    operatorLocation.setLatitude(latitude);
    operatorLocation.setLongitude(longitude);

    return deviceLocation.distanceTo(operatorLocation);
  }

  // TODO: update implementation to get actual driving distance (low)
  public static int getDrivingDistance(Context context, double dLat, double dLon) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    float sLat = prefs.getFloat(KEY_PREF_LATITUDE, 0f);
    float sLon = prefs.getFloat(KEY_PREF_LONGITUDE, 0f);

    String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + sLat + "," + sLon + "&destinations=" + dLat + "," + dLon;
    OkHttpClient okHttpClient = new OkHttpClient();
    Request request = new Request.Builder()
            .url(url)
            .build();
    Response response;
    try {
      response = okHttpClient.newCall(request).execute();
      JSONObject jsonObject = new JSONObject(response.body().string());
      JSONArray rows = jsonObject.getJSONArray("rows");
      JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
      JSONObject distance = elements.getJSONObject(0).getJSONObject("distance");

      return distance.getInt("value");
    } catch (IOException | JSONException e) {
      e.printStackTrace();
      return 0;
    }
  }

  /*
    private static List<Integer> getDrivingDistances(Cursor cursor, Context context, int table) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      float sLat = prefs.getFloat(KEY_PREF_LATITUDE, 0f);
      float sLon = prefs.getFloat(KEY_PREF_LONGITUDE, 0f);

      String urlDistanceMatrix = "https://maps.googleapis.com/maps/api/distancematrix/json";
      List<String> latLongs = new ArrayList<>();
      List<String> requestUrls = new ArrayList<>();
      if (cursor.moveToFirst()) {
        int i = 0;
        do {
          switch (table) {
            case UpdateDistanceTask.LOCATIONS:
              latLongs.add(cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LATITUDE)) + "," +
                           cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LONGITUDE)));
              break;
            case UpdateDistanceTask.REGIONS:
              LatLng latLng = Utility.getLatLngFromRegion(context, cursor.getString(cursor.getColumnIndex(OperatorsColumns.REGION)));
              latLongs.add(latLng.latitude + "," + latLng.longitude);
          }
          i++;

          // max number of destinations for distancematrix is 25 so split up in multiple calls
          if (i == 24 || cursor.isLast()) {
            String url = urlDistanceMatrix + "?origins=" + +sLat + "," + sLon +
                         "&destinations=" + TextUtils.join("|", latLongs) +
                         "&key=" + context.getString(R.string.google_maps_distance_matrix);
            requestUrls.add(url);
            i = 0;
          }
        } while (cursor.moveToNext());
      }

      Log.d("Utility", requestUrls.size() + " " + cursor.getCount());

      List<Integer> values = new ArrayList<>();

      for (String url : requestUrls) {
        getDistancesFromJSON(url, values);
        Log.d("Utility", values.size() + "");
      }

      return values;
    }
  */
  private static void getDistancesFromJSON(String url, List<Integer> values) {
    OkHttpClient okHttpClient = new OkHttpClient();
    Request request = new Request.Builder()
            .url(url)
            .build();

    Response response;

    Log.d("Utility", url);

    try {
      response = okHttpClient.newCall(request).execute();
      Log.d("Utility", response.body().string());
      JSONObject jsonObject = new JSONObject(response.body().string());
      JSONArray rows = jsonObject.getJSONArray("rows");
      JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");

      for (int i = 0; i < elements.length(); i++) {
        JSONObject distance = elements.getJSONObject(i).getJSONObject("distance");
        values.add(distance.getInt("value"));
      }

    } catch (IOException | JSONException e) {
      e.printStackTrace();
    }
  }

  /**
   * update Location stored in shared preferences if location has changed more then 1000m
   * runs updateDistance task which will update the database with new distance values
   * @param context ApplicationContext
   * @param location new Location
   */
  public static void updateLocationSharedPref(Context context, Location location, String type) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Location lastLocation = new Location("");
    lastLocation.setLatitude(prefs.getFloat(KEY_PREF_LATITUDE, 0f));
    lastLocation.setLongitude(prefs.getFloat(KEY_PREF_LONGITUDE, 0f));

    // change stored location if location changed more then 1000m
    if (lastLocation.distanceTo(location) >= 1000 ||
        prefs.getString(Utility.KEY_PREF_LOCATION, "").equals("")) {
      SharedPreferences.Editor prefsEdit = prefs.edit();
      prefsEdit.putFloat(Utility.KEY_PREF_LATITUDE, (float) location.getLatitude());
      prefsEdit.putFloat(Utility.KEY_PREF_LONGITUDE, (float) location.getLongitude());
      prefsEdit.putString(Utility.KEY_PREF_LOCATION, location.toString());
      prefsEdit.apply();

      if (type != null) {
        Bundle args = new Bundle();
        Calendar calendar = Calendar.getInstance();

        args.putInt(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_SEND_LOCATION);
        args.putString("latitude", String.format(Locale.US, "%.6f", location.getLatitude()));
        args.putString("longitude", String.format(Locale.US, "%.6f", location.getLongitude()));
        args.putString("timezone", calendar.getTimeZone().getDisplayName());
        args.putString("type", type);

        OneoffTask task = new OneoffTask.Builder()
                .setTag("send_location")
                .setExecutionWindow(0, 300)
                .setService(FoodtruckTaskService.class)
                .setExtras(args)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .build();

        Log.d("Utility", "sending location " + type);
        GcmNetworkManager.getInstance(context).schedule(task);
      }

      Log.d("Utility", "run update distance task...");
      // update distance in database
      new UpdateDistanceTask(context, UpdateDistanceTask.REGIONS).execute();
      new UpdateDistanceTask(context, UpdateDistanceTask.LOCATIONS).execute();
    }
  }

  /**
   * overload for updateLocationSharedPref with latitude and longitude instead of location object
   * @param context ApplicationContext
   * @param latitude Location latitude
   * @param longitude Location longitude
   */
  public static void updateLocationSharedPref(Context context, double latitude, double longitude) {
    Location location = new Location("");
    location.setLatitude(latitude);
    location.setLongitude(longitude);
    updateLocationSharedPref(context, location, null);
  }

  /**
   * set custom font for toolbar
   * @param toolbar object
   */
  public static void setToolbarTitleFont(Toolbar toolbar, Typeface tf) {
    for (int i = 0; i < toolbar.getChildCount(); i++) {
      View v = toolbar.getChildAt(i);
      if (v instanceof TextView && ((TextView) v).getText().equals(toolbar.getTitle())) {
        ((TextView) v).setTypeface(tf);
      }
    }
  }

  /**
   * format and convert distance between miles and km
   * @param context ApplicationContext
   * @param distance float value
   * @return formatted distance as String
   */
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

  /**
   * change color of bitmap
   * @param marker bitmap image to be changed
   * @param color integer color value
   * @return recolored Bitmap
   */
  public static Bitmap colorBitmap(Bitmap marker, int color) {
    Bitmap markerColored = Bitmap.createBitmap(marker.getWidth(), marker.getHeight(), marker.getConfig());
    Paint paint = new Paint();
    paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
    Canvas c = new Canvas(markerColored);
    c.drawBitmap(marker, 0, 0, paint);
    return markerColored;
  }

  /**
   * add dropShadow to bitmap
   * @param bm bitmap to receive dropShadow
   * @param color shadow color
   * @param size shadow size
   * @param dx shadow x direction
   * @param dy shadow y direction
   * @return bitmap with added dropShadow
   */
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


  /**
   * get height of statusBar for different devices
   * @param context ApplicationContext
   * @return height as int
   */
  public static int getStatusBarHeight(Context context) {
    int result = 0;
    int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      result = context.getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }

  /**
   * get a LatLng from a region, City Name area etc
   * @param context ApplicationContext
   * @param region City, Region, Area name
   * @return LarLng object
   */
  public static LatLng getLatLngFromRegion(Context context, String region) {
    LatLng latLng = null;
    Geocoder geocoder = new Geocoder(context);
    try {
      List<Address> addresses = geocoder.getFromLocationName(region, 5);
      Iterator it = addresses.iterator();
      boolean foundLatLng = false;
      while (it.hasNext() && !foundLatLng) {
        Address address = (Address) it.next();
        if (address.hasLatitude() && address.hasLongitude()) {
          latLng = new LatLng(address.getLatitude(), address.getLongitude());
          foundLatLng = true;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return latLng;
  }

  /**
   * check if an operator is in favourites
   * @param context ApplicationContext
   * @param operatorId id of operator
   * @return true if operator is found in FAV table
   */
  public static boolean isFavourite(Context context, String operatorId) {
    boolean isFav = false;
    Cursor cursor = context.getContentResolver().query(FoodtruckProvider.Favourites.CONTENT_URI,
            new String[]{
                    FavouritesColumns.ID,
                    FavouritesColumns.FAVOURITE
            },
            FavouritesColumns.ID + " = ? AND " + FavouritesColumns.FAVOURITE + " = ? ",
            new String[]{operatorId, "1"},
            null);
    if (cursor != null && cursor.moveToFirst()) {
      isFav = true;
      cursor.close();
    }

    return isFav;
  }

  /**
   * Initialize the Regions db table from json file in assets
   * @param context ApplicationContext
   */
  public static void initRegionsTable(Context context) {
    Log.d("Utility", "initializing regions table");
    try {
      InputStream inputStream = context.getAssets().open("regions.json");
      byte[] buffer = new byte[inputStream.available()];
      //noinspection ResultOfMethodCallIgnored
      inputStream.read(buffer);
      inputStream.close();

      String json = new String(buffer, "UTF-8");
      ArrayList<ContentProviderOperation> operations = getRegionsDataFromJson(json, context);

      context.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, operations);

    } catch (IOException | JSONException | OperationApplicationException | RemoteException e) {
      e.printStackTrace();
    }
  }

  /**
   * get regions data from json
   * @param json regions data returned from getRegions
   * @return list of ContentProviderOperation
   * @throws JSONException  error from accessing json
   */
  public static ArrayList<ContentProviderOperation> getRegionsDataFromJson(String json, Context context) throws JSONException {
    Log.d("Utility", "getting regions data...");
    JSONObject jsonObject = new JSONObject(json);
    JSONArray regions = jsonObject.getJSONArray("regions");

    ArrayList<ContentProviderOperation> operations = new ArrayList<>();

    for (int i = 0; i < regions.length(); i++) {
      JSONObject region = regions.getJSONObject(i);
      if (!regionEntryExists(context, region.getString(RegionsColumns.ID))) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(FoodtruckProvider.Regions.CONTENT_URI);
        builder.withValue(RegionsColumns.ID, region.getString(RegionsColumns.ID));
        builder.withValue(RegionsColumns.NAME, region.getString(RegionsColumns.NAME));
        builder.withValue(RegionsColumns.LATITUDE, region.getString(RegionsColumns.LATITUDE));
        builder.withValue(RegionsColumns.LONGITUDE, region.getString(RegionsColumns.LONGITUDE));
        operations.add(builder.build());
      }
    }
    return operations;
  }

  private static boolean regionEntryExists(Context context, String regionId) {
    Cursor cursor = context.getContentResolver()
            .query(FoodtruckProvider.Regions.withId(regionId),
                    null, null, null, null);

    boolean exists = false;
    if (cursor != null) {
      exists = cursor.moveToFirst();
      cursor.close();
    }
    return exists;
  }

  /**
   * initialize Operators db table from json data in assets
   * @param context ApplicationContext
   */
  public static void initOperatorsTable(Context context) {
    try {
      InputStream inputStream = context.getAssets().open("operators.json");
      byte[] buffer = new byte[inputStream.available()];
      //noinspection ResultOfMethodCallIgnored
      inputStream.read(buffer);
      inputStream.close();

      String json = new String(buffer, "UTF-8");
      ArrayList<ContentProviderOperation> operations = getOperatorsFromJson(json, context);
      context.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, operations);

    } catch (IOException | RemoteException | OperationApplicationException e) {
      e.printStackTrace();
    }
  }




  /**
   * sets shared preference with date of newest image
   * used when fetching checkImages
   * @param context ApplicationContext
   * @param json String of images json data
   * @throws JSONException error from parsing JSON
   */
  private static void setLastImageTimestamp(Context context, String json) throws JSONException {
    JSONObject jsonObject = new JSONObject(json);
    JSONObject jsonImages = jsonObject.getJSONObject("images");
    Iterator it = jsonImages.keys();

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    int lastSavedImageTime = preferences.getInt(LAST_IMAGE_TIMESTAMP_PREF, 0);

    while (it.hasNext()) {
      String key = (String) it.next();
      JSONObject image = jsonImages.getJSONObject(key);
      int lastChangeTime = image.getInt("last_change_time");
      if (lastChangeTime > lastSavedImageTime)
        lastSavedImageTime = lastChangeTime;
    }

    SharedPreferences.Editor prefsEditor = preferences.edit();
    prefsEditor.putInt(LAST_IMAGE_TIMESTAMP_PREF, lastSavedImageTime);
    prefsEditor.apply();
  }

  /**
   * convert 3 char color string to 6 char color string (#000 to #000000)
   * @param color 3 char color string
   * @return 6 char color string
   */
  private static String parseColor(String color) {
    String c = "";
    if (color.length() == 7)
      c = color;
    else if (color.length() == 4) {
      c = "#";
      for (int i = 1; i < 4; i++)
        c += color.substring(i, i+1) + color.substring(i, i+1);
    }

    return c;
  }


  /**
   * create ContentProvider operations from locations JSON data
   * @param json locations json data
   * @param context ApplicationContext
   * @return ArrayLists of ContentProvider operations
   */
  public static ArrayList<ContentProviderOperation> getLocationDataFromJson(String json, Context context) {
    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    JSONObject jsonObject, jsonLocations;
    try {
      jsonObject = new JSONObject(json);
      if (jsonObject.length() > 0) {
        jsonLocations = jsonObject.getJSONObject("locations");
        Iterator it = jsonLocations.keys();
        while (it.hasNext()) {
          String key = (String) it.next();
          if (!locationEntryExistsInDatabase(jsonLocations.getJSONObject(key), context))
            operations.add(buildLocationOperation(jsonLocations.getJSONObject(key)));
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return operations;
  }

  /**
   * build content provider operation
   * @param jsonObject location json data
   * @return ContentProviderOperation that can be batch applied
   * @throws JSONException error from accessing json
   */
  private static ContentProviderOperation buildLocationOperation(JSONObject jsonObject) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Locations.CONTENT_URI);

    String startDate = jsonObject.getString(LocationsColumns.START_DATE);
    String endDate = jsonObject.getString(LocationsColumns.END_DATE);

    // fixing API error where end_date is before start_date
    if (parseDateString(startDate).getTime() > parseDateString(endDate).getTime()) {
      Date ed = parseDateString(endDate);
      Calendar cal = Calendar.getInstance();
      cal.setTime(ed);
      cal.add(Calendar.DAY_OF_YEAR, 1);
      DateFormat dateFormat = new SimpleDateFormat(ISO_8601);
      endDate = dateFormat.format(cal.getTime());
      Log.d("Utility", "new end date = " + endDate);
    }

    builder.withValue(LocationsColumns.LOCATION_ID, jsonObject.getString(LocationsColumns.LOCATION_ID));
    builder.withValue(LocationsColumns.OPERATOR_ID, jsonObject.getString(LocationsColumns.OPERATOR_ID));
    builder.withValue(LocationsColumns.OPERATOR_NAME, Html.fromHtml(jsonObject.getString(LocationsColumns.OPERATOR_NAME)).toString());
    builder.withValue(LocationsColumns.IMAGE_ID, jsonObject.getString(LocationsColumns.IMAGE_ID));
    builder.withValue(LocationsColumns.OPERATOR_OFFER, Html.fromHtml(jsonObject.getString(LocationsColumns.OPERATOR_OFFER)).toString());
    builder.withValue(LocationsColumns.OPERATOR_BACKGROUND, jsonObject.getString(LocationsColumns.OPERATOR_BACKGROUND));
    builder.withValue(LocationsColumns.OPERATOR_LOGO_URL, jsonObject.getString(LocationsColumns.OPERATOR_LOGO_URL));
    builder.withValue(LocationsColumns.LATITUDE, jsonObject.getString(LocationsColumns.LATITUDE));
    builder.withValue(LocationsColumns.LONGITUDE, jsonObject.getString(LocationsColumns.LONGITUDE));
    builder.withValue(LocationsColumns.START_DATE, startDate);
    builder.withValue(LocationsColumns.END_DATE, endDate);
    builder.withValue(LocationsColumns.LOCATION_NAME, jsonObject.getString("name"));
    builder.withValue(LocationsColumns.STREET, jsonObject.getString(LocationsColumns.STREET));
    builder.withValue(LocationsColumns.NUMBER, jsonObject.getString(LocationsColumns.NUMBER));
    builder.withValue(LocationsColumns.CITY, jsonObject.getString(LocationsColumns.CITY));
    builder.withValue(LocationsColumns.ZIPCODE, jsonObject.getString(LocationsColumns.ZIPCODE));

    return builder.build();
  }

  /**
   * check if location entry is already in database
   * @param jsonObject location data
   * @param context ApplicationContext
   * @return true if data exists
   */
  private static boolean locationEntryExistsInDatabase(JSONObject jsonObject, Context context) {
    Cursor cursor = null;
    try {
      String operatorId = jsonObject.getString(LocationsColumns.OPERATOR_ID);
      cursor = context.getContentResolver().query(
              FoodtruckProvider.Locations.withOperatorId(operatorId),
              new String[]{
                      LocationsColumns.START_DATE,
                      LocationsColumns.END_DATE,
                      LocationsColumns.LATITUDE,
                      LocationsColumns.LONGITUDE
              },
              LocationsColumns.START_DATE + " = ? AND " + LocationsColumns.END_DATE + " = ? AND " +
              LocationsColumns.LATITUDE + " = ? AND " + LocationsColumns.LONGITUDE + " = ? ",
              new String[]{
                      jsonObject.getString(LocationsColumns.START_DATE),
                      jsonObject.getString(LocationsColumns.END_DATE),
                      jsonObject.getString(LocationsColumns.LATITUDE),
                      jsonObject.getString(LocationsColumns.LONGITUDE)
              },
              null);
      if (cursor != null && cursor.getCount() > 0)
        return true;
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return false;
  }

  /**
   * create ContentValues from OperatorDetails data
   * @param json operator details data
   * @param operatorId operator id
   * @return ContentValues for ContentProvider
   */
  public static ContentValues getDetailsContentValuesFromJson(String json, String operatorId) {
    ContentValues contentValues = new ContentValues();
    JSONObject jsonObject, jsonOperator;
    try {
      jsonObject = new JSONObject(json);
      if (jsonObject.length() > 0) {
        jsonOperator = jsonObject.getJSONObject("operator");
        contentValues.put(OperatorDetailsColumns.OPERATOR_ID, operatorId);
        contentValues.put(OperatorDetailsColumns.OPERATOR_NAME, jsonOperator.getString("name"));
        contentValues.put(OperatorDetailsColumns.OPERATOR_OFFER, jsonOperator.getString("offer"));
        contentValues.put(OperatorDetailsColumns.OPERATOR_ID, operatorId);
        contentValues.put(OperatorDetailsColumns.DESCRIPTION,
                Html.fromHtml(jsonOperator.getString(OperatorDetailsColumns.DESCRIPTION)).toString());
        contentValues.put(OperatorDetailsColumns.DESCRIPTION_LONG,
                Html.fromHtml(jsonOperator.getString(OperatorDetailsColumns.DESCRIPTION_LONG)).toString());
        contentValues.put(OperatorDetailsColumns.WEBSITE, jsonOperator.getString(OperatorDetailsColumns.WEBSITE));
        contentValues.put(OperatorDetailsColumns.WEBSITE_URL, jsonOperator.getString(OperatorDetailsColumns.WEBSITE_URL));
        contentValues.put(OperatorDetailsColumns.FACEBOOK, jsonOperator.getString(OperatorDetailsColumns.FACEBOOK));
        contentValues.put(OperatorDetailsColumns.FACEBOOK_URL, jsonOperator.getString(OperatorDetailsColumns.FACEBOOK_URL));
        contentValues.put(OperatorDetailsColumns.TWITTER, jsonOperator.getString(OperatorDetailsColumns.TWITTER));
        contentValues.put(OperatorDetailsColumns.TWITTER_URL, jsonOperator.getString(OperatorDetailsColumns.TWITTER_URL));
        contentValues.put(OperatorDetailsColumns.EMAIL, jsonOperator.getString(OperatorDetailsColumns.EMAIL));
        contentValues.put(OperatorDetailsColumns.PHONE, jsonOperator.getString(OperatorDetailsColumns.PHONE));
        contentValues.put(OperatorDetailsColumns.LOGO_URL, jsonOperator.getString(OperatorDetailsColumns.LOGO_URL));
        contentValues.put(OperatorDetailsColumns.REGION, jsonOperator.getString(OperatorDetailsColumns.REGION));
        String logoBackground = jsonOperator.getString(OperatorDetailsColumns.LOGO_BACKGROUND);
        if (logoBackground.length() == 4) {
          logoBackground = "#" + logoBackground.substring(1, 2) + logoBackground.substring(1, 2)
                           + logoBackground.substring(2, 3) + logoBackground.substring(2, 3)
                           + logoBackground.substring(3, 4) + logoBackground.substring(3, 4);
        }
        contentValues.put(OperatorDetailsColumns.LOGO_BACKGROUND, logoBackground);
        contentValues.put(OperatorDetailsColumns.PREMIUM, jsonOperator.getBoolean(OperatorDetailsColumns.PREMIUM));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return contentValues;
  }


  /**
   * create ContentValues from OperatorDetails data
   * @param json operator details data
   * @param operatorId operator id
   * @return ContentValues for ContentProvider
   */
  public static ArrayList<ContentProviderOperation> getImpressionsContentValuesFromJson(String json, String operatorId) {
    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    JSONObject jsonObject, jsonOperator;
    try {
      jsonObject = new JSONObject(json);
      if (jsonObject.length() > 0) {
        jsonOperator = jsonObject.getJSONObject("operator");
        JSONArray impressions = jsonOperator.getJSONArray("impressions");
        for (int i = 0; i < impressions.length(); i++) {
          String impression = impressions.getString(i);
          if (impression != null && impression.length() > 0) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(FoodtruckProvider.Impressions.CONTENT_URI);
            builder.withValue(ImpressionsColumns.ID, operatorId);
            builder.withValue(ImpressionsColumns.IMPRESSION, impression);

            operations.add(builder.build());
          }
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return operations;
  }

  /**
   * create ContentProvideOperations fro  json data
   * @param json operators data
   * @param context ApplicationContext
   * @return ArrayList of ContentProviderOperations
   */
  public static ArrayList<ContentProviderOperation> getOperatorsFromJson(String json, Context context) {
    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    JSONObject jsonObject, jsonOperators;
    try {
      jsonObject = new JSONObject(json);
      jsonOperators = jsonObject.getJSONObject("operators");
      Iterator it = jsonOperators.keys();
      while (it.hasNext()) {
        String key = (String) it.next();

        JSONObject operator = jsonOperators.getJSONObject(key);
        operations.add(buildOperatorsOperation(operator, context));

        JSONArray tags = operator.getJSONArray("tags");
        for (int i = 0; i < tags.length(); i++) {
          String tag = tags.getString(i);
          String operatorId = operator.getString(OperatorsColumns.ID);
          if (!tag.equals("") && !tagEntryExists(operatorId, tag, context)) {
            operations.add(buildTagsOperation(operatorId, tag));
          }
        }

      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return operations;
  }

  /**
   * check if entry exists in tags table
   * @param operatorId operator id
   * @param tag tag used by operator
   * @param context ApplicationContext
   * @return true if entry exists
   */
  private static boolean tagEntryExists(String operatorId, String tag, Context context) {
    Cursor cursor = context.getContentResolver().query(FoodtruckProvider.Tags.CONTENT_URI,
            new String[]{
                    TagsColumns.ID,
                    TagsColumns.TAG
            },
            TagsColumns.ID + " = ? AND " + TagsColumns.TAG + " = ?",
            new String[]{
                    operatorId,
                    tag
            },
            null);
    boolean exists = false;
    if (cursor != null) {
      exists = cursor.moveToFirst();
      cursor.close();
    }
    return exists;
  }

  /**
   * ContentProviderOperation to store tags
   * @param operatorId operator id
   * @param tag tag used by operator
   * @return ContentProviderOperation
   * @throws JSONException from parsing JSON
   */
  private static ContentProviderOperation buildTagsOperation(String operatorId, String tag) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Tags.CONTENT_URI);

    builder.withValue(TagsColumns.ID, operatorId);
    builder.withValue(TagsColumns.TAG, tag);

    return builder.build();
  }

  /**
   * Create ContentProviderOperation for operator entry
   * @param jsonObject operator data
   * @return ContentProviderOperation
   * @throws JSONException from parsing JSON
   */
  private static ContentProviderOperation buildOperatorsOperation(JSONObject jsonObject, Context context) throws JSONException {
    String operatorId = jsonObject.getString(OperatorsColumns.ID);
    ContentProviderOperation.Builder builder;

    if (operatorEntryExists(context, operatorId)) {
      builder = ContentProviderOperation.newUpdate(FoodtruckProvider.Operators.withId(operatorId));
    } else {
      builder = ContentProviderOperation.newInsert(FoodtruckProvider.Operators.CONTENT_URI_JOINED);
    }

    builder.withValue(OperatorsColumns.ID, operatorId);
    builder.withValue(OperatorsColumns.NAME, Html.fromHtml(jsonObject.getString(OperatorsColumns.NAME)).toString());
    builder.withValue(OperatorsColumns.OFFER, Html.fromHtml(jsonObject.getString(OperatorsColumns.OFFER)).toString());
    builder.withValue(OperatorsColumns.LOGO_URL, jsonObject.getString(OperatorsColumns.LOGO_URL));
    builder.withValue(OperatorsColumns.REGION, jsonObject.getString(OperatorsColumns.REGION));
    builder.withValue(OperatorsColumns.REGION_ID, jsonObject.getString(OperatorsColumns.REGION_ID));
    String logoBackground = jsonObject.getString(OperatorDetailsColumns.LOGO_BACKGROUND);
    if (logoBackground.length() == 4) {
      logoBackground = "#" + logoBackground.substring(1, 2) + logoBackground.substring(1, 2)
                       + logoBackground.substring(2, 3) + logoBackground.substring(2, 3)
                       + logoBackground.substring(3, 4) + logoBackground.substring(3, 4);
    }
    builder.withValue(OperatorsColumns.LOGO_BACKGROUND, logoBackground);

    return builder.build();
  }

  private static boolean operatorEntryExists(Context context, String operatorId) {
    Cursor cursor = context.getContentResolver().query(FoodtruckProvider.Operators.withId(operatorId),
            null, null, null, null);

    boolean exists = false;
    if (cursor != null) {
      exists = cursor.getCount() > 0;
      cursor.close();
    }
    return exists;
  }

  /**
   * pre cache foodtruck logos to file storage
   * @param context ApplicationContext
   */
  public static void cacheLogos(Context context) {
    CacheLogosTask task = new CacheLogosTask(context);
    task.execute();
  }

  private static class CacheLogosTask extends AsyncTask<Void, Void, Void> {
    private Cursor cursor;
    private FileOutputStream outputStream;
    private Bitmap logo;
    private WeakReference<Context> contextRef;

    CacheLogosTask(Context context) {
      contextRef = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... params) {
      Context context = contextRef.get();

      cursor = context.getContentResolver().query(
              FoodtruckProvider.Operators.CONTENT_URI,
              new String[]{
                      OperatorsColumns.LOGO_URL
              },
              null,
              null,
              null);

      if (cursor != null && cursor.moveToFirst()) {
        do {
          final String logoUrl = cursor.getString(cursor.getColumnIndex(OperatorsColumns.LOGO_URL));
          final String fileName = logoUrl.substring(logoUrl.lastIndexOf("/")+1);

          File file = context.getFilesDir();

          File[] files = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              return name.equals(fileName);
            }
          });


          boolean fileInAssets = false;

          try {
            fileInAssets = Arrays.asList(context.getAssets().list("images")).contains(fileName);
          } catch (IOException e) {
            e.printStackTrace();
          }

          if (files.length == 0 && !fileInAssets) {

            try {
              logo = Glide.with(context)
                      .asBitmap()
                      .load(logoUrl)
                      .apply(new RequestOptions().fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL))
                      .submit(320, 320)
                      .get();

              outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
              logo.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

              if (outputStream != null)
                outputStream.close();
            } catch (InterruptedException | IOException | ExecutionException e) {
              e.printStackTrace();
            }

          }
        } while (cursor.moveToNext());
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      Log.d("Utility", "cacheLogos finished...");
      super.onPostExecute(aVoid);
      cursor.close();
    }
  }

  /**
   * get bitmap from assets
   * @param context ApplicationContext
   * @param filePath path to file in assets
   * @return bitmap
   */
  public static Bitmap getBitmapFromAsset(Context context, String filePath) {
    AssetManager assetManager = context.getAssets();

    InputStream in = null;
    Bitmap bitmap = null;
    try {
      in = assetManager.open(filePath);
      bitmap = BitmapFactory.decodeStream(in);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }

    return bitmap;
  }


  /**
   * check shared preference key to see if application has been launched before
   * this is used to trigger the welcome screen to display when first launched
   * @param context ApplicationContext
   * @return true if shared preference has not been set before
   */
  public static boolean isFirstLaunch(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_IS_FIRST_LAUNCH_PREF, true);
  }

  /**
   * hides the software keyboard
   * used on search submit
   * @param activity MainActivity
   */
  public static void hideSoftKeyboard(Activity activity) {
    InputMethodManager inputMethodManager =
            (InputMethodManager) activity.getSystemService(
                    Activity.INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(
            activity.getCurrentFocus().getWindowToken(), 0);
  }




  /**
   * AsyncTask used to update distance for regions and locations
   */
  public static class UpdateDistanceTask extends AsyncTask<Void, Void, Integer> {
    public static final int LOCATIONS = 0;
    public static final int REGIONS = 1;
    private int mTable;
    private Cursor mCursor;
    private WeakReference<Context> contextRef;
    private long startTime = System.currentTimeMillis();

    public UpdateDistanceTask(Context context, int table) {
      contextRef = new WeakReference<>(context);
      mTable = table;
      setCursor();
    }

    private void setCursor() {
      Context context = contextRef.get();
      switch (mTable) {
        case LOCATIONS:
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
          break;
        case REGIONS:
          mCursor = context.getContentResolver().query(
                  FoodtruckProvider.Regions.CONTENT_URI,
                  new String[]{
                          RegionsColumns.ID,
                          RegionsColumns.LATITUDE,
                          RegionsColumns.LONGITUDE
                  },
                  null,
                  null,
                  null);
          break;
      }
    }

    @Override
    protected Integer doInBackground(Void... voids) {
      Context context = contextRef.get();
      int rowsUpdated = 0;
      if (mCursor != null && mCursor.moveToFirst()) {
        do {
          ContentValues values = new ContentValues();

          switch (mTable) {
            case LOCATIONS:
              values.put(LocationsColumns.DISTANCE, Utility.getOperatorDistance(context,
                      mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LATITUDE)),
                      mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LONGITUDE))));

              rowsUpdated += context.getContentResolver().update(
                      FoodtruckProvider.Locations.withOperatorId(
                              mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_ID))),
                      values,
                      LocationsColumns.LATITUDE + " = ? AND " + LocationsColumns.LONGITUDE + " = ?",
                      new String[]{
                              mCursor.getString(mCursor.getColumnIndex(LocationsColumns.LATITUDE)),
                              mCursor.getString(mCursor.getColumnIndex(LocationsColumns.LONGITUDE))
                      });
              break;
            case REGIONS:
              values.put(RegionsColumns.DISTANCE_APROX,
                      Utility.getOperatorDistance(context,
                              mCursor.getDouble(mCursor.getColumnIndex(RegionsColumns.LATITUDE)),
                              mCursor.getDouble(mCursor.getColumnIndex(RegionsColumns.LONGITUDE))));

              rowsUpdated += context.getContentResolver().update(
                      FoodtruckProvider.Regions.withId(
                              mCursor.getString(mCursor.getColumnIndex(RegionsColumns.ID))),
                      values,
                      RegionsColumns.LATITUDE + " = ? AND " + RegionsColumns.LONGITUDE + " = ?",
                      new String[]{
                              mCursor.getString(mCursor.getColumnIndex(RegionsColumns.LATITUDE)),
                              mCursor.getString(mCursor.getColumnIndex(RegionsColumns.LONGITUDE))
                      });
              break;
          }
        } while (mCursor.moveToNext());
      }

      return rowsUpdated;
    }

    @Override
    protected void onPostExecute(Integer integer) {
      Log.d("UpdateDistanceTask " + ((mTable == LOCATIONS) ? "LOCATIONS" : "REGIONS"), "Updated " + integer + " rows in " + (System.currentTimeMillis() - startTime) + "ms");

      Intent intent = new Intent(FOODTRUCK_SERVICE_RESPONSE);
      intent.putExtra(MESSAGE_UPDATE_DISTANCE_TASK, MESSAGE_SUCCESS);
      LocalBroadcastManager.getInstance(contextRef.get()).sendBroadcast(intent);
    }
  }
/*
  private static class UpdateDrivingDistancesTask extends AsyncTask<Void, Void, List<Integer>> {

    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
    private String url;
    private OkHttpClient okHttpClient = new OkHttpClient();

    UpdateDrivingDistancesTask(Cursor cursor) {
      if (cursor != null && cursor.moveToFirst()) {
        do {

        } while (cursor.moveToNext());
      }
    }

    @Override
    protected List<Integer> doInBackground(Void... voids) {
      List<Integer> values = new ArrayList<>();
      Request request = new Request.Builder().url(url).build();
      try {
        Response response = okHttpClient.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string());
        JSONArray rows = jsonObject.getJSONArray("rows");
        JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");

        for (int i = 0; i < elements.length(); i++) {
          JSONObject distance = elements.getJSONObject(i).getJSONObject("distance");
          values.add(distance.getInt("value"));
        }
      } catch (IOException | JSONException e) {
        e.printStackTrace();
      }

      return values;
    }

    @Override
    protected void onPostExecute(List<Integer> values) {
      super.onPostExecute(values);
    }
  }*/

}
