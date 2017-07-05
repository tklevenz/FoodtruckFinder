package co.pugo.apps.foodtruckfinder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
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
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import co.pugo.apps.foodtruckfinder.data.FavouritesColumns;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import co.pugo.apps.foodtruckfinder.data.RegionsColumns;
import co.pugo.apps.foodtruckfinder.data.TagsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckResultReceiver;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint("SimpleDateFormat")
public class Utility {

  private static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssZZZ";
  private static final String LOCATION_LAST_UPDATED = "location_last_updated";
  private static final String OPERATORS_LAST_UPDATED = "operators_last_updated";
  private static final String KEY_PREF_LATITUDE = "pref_latitude";
  private static final String KEY_PREF_LONGITUDE = "pref_longitude";
  private static final String KEY_PREF_LOCATION = "pref_location";
  public static final String KEY_IS_FIRST_LAUNCH_PREF = "pref_first_launch";
  public static final String MAP_MARKER_SIGNATURE = "MapMarker";
  public static final String LAST_IMAGE_TIMESTAMP_PREF = "last_image_time_pref";

  public static String getFormattedDate(String dateString, Context context) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d");

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

  public static String getDateNow() {
    Calendar today = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.format(today.getTime());
  }

  public static String getFormattedTime(String string) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("H:mm");
    dateFormat.setTimeZone(getTzFromString(string));
    return dateFormat.format(parseDateString(string));
  }

  public static boolean isLocalTime(String string) {
    TimeZone deviceTz = TimeZone.getDefault();
    DateFormat dateFormat = new SimpleDateFormat(ISO_8601);
    try {
      dateFormat.parse(string);
      dateFormat.setTimeZone(getTzFromString(string));
      TimeZone locationTz = dateFormat.getTimeZone();

      return deviceTz.getDisplayName(false, TimeZone.SHORT).equals(locationTz.getDisplayName(false, TimeZone.SHORT));
    } catch (ParseException e) {
      e.printStackTrace();
      return false;
    }
  }

  public static String getTimeZone(String string) {
    if (!isLocalTime(string)) {
      TimeZone tz = getTzFromString(string);

      return " (" + tz.getDisplayName(false, TimeZone.SHORT) + ")";
    } else {
      return "";
    }
  }

  private static TimeZone getTzFromString(String string) {
    return TimeZone.getTimeZone("GMT" + TextUtils.substring(string, 19, 25));
  }

  private static Date parseDateString(String string) {
    SimpleDateFormat format = new SimpleDateFormat(ISO_8601);

    Date date = null;
    try {
      date = format.parse(string);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return date;
  }

  public static boolean isToday(String dateString) {
    Date date = parseDateString(dateString);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return dateFormat.format(date).equals(getDateNow());
  }

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    return networkInfo != null && networkInfo.isConnected();
  }

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
    }

    prefsEditor.apply();
  }


  public static boolean isOutOfDate(Context context, int task) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    long lastUpdated;
    switch (task) {
      case FoodtruckTaskService.TASK_FETCH_OPERATORS:
        lastUpdated = prefs.getLong(OPERATORS_LAST_UPDATED, 0);
        return lastUpdated == 0 || lastUpdated - currentDayMillis() >= 7 * 24 * 3600 * 1000;
      case FoodtruckTaskService.TASK_FETCH_LOCATIONS:
        lastUpdated = prefs.getLong(LOCATION_LAST_UPDATED, 0);
        return lastUpdated == 0 || lastUpdated != currentDayMillis();
      default:
        return true;
    }
  }

  private static long currentDayMillis() {
    return System.currentTimeMillis() / (1000 * 3600 * 24) * (1000 * 3600 * 24);
  }

  public static boolean dataExists(Context context, Uri contentUri) {
    Cursor cursor = context.getContentResolver().query(contentUri, new String[]{"_id"}, null, null, null);
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

  public static void updateLocationSharedPref(Context context, Location location, ResultReceiver receiver) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Location lastLocation = new Location("");
    lastLocation.setLatitude(prefs.getFloat(KEY_PREF_LATITUDE, 0f));
    lastLocation.setLongitude(prefs.getFloat(KEY_PREF_LONGITUDE, 0f));

    // change stored location if location changed more then 1000m
    if (lastLocation.distanceTo(location) >= 1000 || prefs.getString(Utility.KEY_PREF_LOCATION, "").equals("")) {

    }

    SharedPreferences.Editor prefsEdit = prefs.edit();
    prefsEdit.putFloat(Utility.KEY_PREF_LATITUDE, (float) location.getLatitude());
    prefsEdit.putFloat(Utility.KEY_PREF_LONGITUDE, (float) location.getLongitude());
    prefsEdit.putString(Utility.KEY_PREF_LOCATION, location.toString());
    prefsEdit.apply();

    Log.d("Utility", "run update distance task...");
    // update distance in database
    new UpdateDistanceTask(context, UpdateDistanceTask.REGIONS, receiver).execute();
    new UpdateDistanceTask(context, UpdateDistanceTask.LOCATIONS, receiver).execute();
  }

  public static void updateLocationSharedPref(Context context, double latitude, double longitude) {
    Location location = new Location("");
    location.setLatitude(latitude);
    location.setLongitude(longitude);
    updateLocationSharedPref(context, location, null);
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

  public static void initRegionsTable(Context context) {
    Log.d("Utility", "initializing regions table");
    try {
      InputStream inputStream = context.getAssets().open("regions.json");
      byte[] buffer = new byte[inputStream.available()];
      //noinspection ResultOfMethodCallIgnored
      inputStream.read(buffer);
      inputStream.close();

      String json = new String(buffer, "UTF-8");
      JSONObject jsonObject = new JSONObject(json);
      JSONArray regions = jsonObject.getJSONArray("regions");

      ArrayList<ContentProviderOperation> operations = new ArrayList<>();

      for (int i = 0; i < regions.length(); i++) {
        JSONObject region = regions.getJSONObject(i);
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(FoodtruckProvider.Regions.CONTENT_URI);
        builder.withValue(RegionsColumns.ID, region.getString(RegionsColumns.ID));
        builder.withValue(RegionsColumns.NAME, region.getString(RegionsColumns.NAME));
        builder.withValue(RegionsColumns.LATITUDE, region.getString(RegionsColumns.LATITUDE));
        builder.withValue(RegionsColumns.LONGITUDE, region.getString(RegionsColumns.LONGITUDE));
        operations.add(builder.build());
      }

      context.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, operations);

    } catch (IOException | JSONException | OperationApplicationException | RemoteException e) {
      e.printStackTrace();
    }
  }

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

  public static void initMapMarkers(Context context, boolean updateCache) {
    try {
      InputStream inputStream = context.getAssets().open("images.json");
      byte[] buffer = new byte[inputStream.available()];
      //noinspection ResultOfMethodCallIgnored
      inputStream.read(buffer);
      inputStream.close();

      String json = new String(buffer, "UTF-8");

      if (updateCache)
        cacheMapMarkers(context, json);
      else
        setLastImageTimestamp(context, json);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void cacheMapMarkers(Context context, String json)
          throws JSONException, ExecutionException, InterruptedException, IOException {

    JSONObject jsonObject = new JSONObject(json);
    JSONObject jsonImages = jsonObject.getJSONObject("images");
    Iterator it = jsonImages.keys();
    FileOutputStream outputStream;

    while (it.hasNext()) {
      String key = (String) it.next();
      JSONObject image = jsonImages.getJSONObject(key);
      String imageId = image.getString("image_id");
      String operatorId = image.getString("operator_id");
      String logoUrl = image.getString("url");
      String bgColor = parseColor(image.getString("background"));

      int color;
      try {
        color = Color.parseColor(bgColor);
      } catch (Exception e) {
        color = Color.WHITE;
        Log.d("Utility", bgColor);
        e.printStackTrace();
      }

      Bitmap logo = Glide.with(context)
              .load(logoUrl)
              .asBitmap()
              .fitCenter()
              .diskCacheStrategy(DiskCacheStrategy.ALL)
              .into(320, 320)
              .get();
      outputStream = context.openFileOutput(getMarkerFileName(operatorId, imageId), Context.MODE_PRIVATE);
      createMapMarker(context, logo, color).compress(Bitmap.CompressFormat.PNG, 100, outputStream);

      outputStream.close();

      Log.d("Utility", "cached: " + getMarkerFileName(operatorId, imageId));
    }
  }

  public static void setLastImageTimestamp(Context context, String json) throws JSONException {
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

  public static String parseColor(String color) {
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


  public static void storeHeaderImages(String response, Context context) {
    JSONObject jsonObject, jsonOperator;
    FileOutputStream outputStream;
    try {
      jsonObject = new JSONObject(response);
      jsonOperator = jsonObject.getJSONObject("operator");
      String operatorId = jsonOperator.getString("id");
      JSONArray impressions = jsonOperator.getJSONArray("impressions");
      for (int i = 0; i < impressions.length(); i++) {
        String impressionUrl = impressions.get(i).toString();
        String fileName = operatorId + "-image" + i + ".png";
        Bitmap bitmap = Glide.with(context)
                .load(impressionUrl)
                .asBitmap()
                .centerCrop()
                .into(240, 240)
                .get();
        outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        outputStream.close();
      }
    } catch (JSONException | InterruptedException | ExecutionException | IOException e) {
      e.printStackTrace();
    }
  }

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
          if (!entryExistsInDatabase(jsonLocations.getJSONObject(key), context))
            operations.add(buildLocationOperation(jsonLocations.getJSONObject(key), context));
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return operations;
  }

  private static ContentProviderOperation buildLocationOperation(JSONObject jsonObject, Context context) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Locations.CONTENT_URI);

    builder.withValue(LocationsColumns.OPERATOR_ID, jsonObject.getString(LocationsColumns.OPERATOR_ID));
    builder.withValue(LocationsColumns.OPERATOR_NAME, Html.fromHtml(jsonObject.getString(LocationsColumns.OPERATOR_NAME)).toString());
    builder.withValue(LocationsColumns.IMAGE_ID, jsonObject.getString(LocationsColumns.IMAGE_ID));
    builder.withValue(LocationsColumns.OPERATOR_OFFER, Html.fromHtml(jsonObject.getString(LocationsColumns.OPERATOR_OFFER)).toString());
    builder.withValue(LocationsColumns.OPERATOR_LOGO_URL, jsonObject.getString(LocationsColumns.OPERATOR_LOGO_URL));
    builder.withValue(LocationsColumns.LATITUDE, jsonObject.getString(LocationsColumns.LATITUDE));
    builder.withValue(LocationsColumns.LONGITUDE, jsonObject.getString(LocationsColumns.LONGITUDE));
    builder.withValue(LocationsColumns.START_DATE, jsonObject.getString(LocationsColumns.START_DATE));
    builder.withValue(LocationsColumns.END_DATE, jsonObject.getString(LocationsColumns.END_DATE));
    builder.withValue(LocationsColumns.LOCATION_NAME, jsonObject.getString("name"));
    builder.withValue(LocationsColumns.STREET, jsonObject.getString(LocationsColumns.STREET));
    builder.withValue(LocationsColumns.NUMBER, jsonObject.getString(LocationsColumns.NUMBER));
    builder.withValue(LocationsColumns.CITY, jsonObject.getString(LocationsColumns.CITY));
    builder.withValue(LocationsColumns.ZIPCODE, jsonObject.getString(LocationsColumns.ZIPCODE));

    return builder.build();
  }

  private static boolean entryExistsInDatabase(JSONObject jsonObject, Context context) {
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
        operations.add(buildOperatorsOperation(operator));

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

  private static boolean tagEntryExists(String operatorId, String tag, Context context) {
    Cursor cursor = context.getContentResolver().query(FoodtruckProvider.Tags.CONTENT_URI,
            new String[]{
                    TagsColumns.ID,
                    TagsColumns.TAG
            },
            TagsColumns.ID + " = ? AND " + TagsColumns.TAG + " = ?",
            new String[]{
                    TagsColumns.ID,
                    TagsColumns.TAG
            },
            null);
    boolean exists = false;
    if (cursor != null && cursor.moveToFirst()) {
      exists = true;
      cursor.close();
    }
    return exists;
  }

  private static ContentProviderOperation buildTagsOperation(String operatorId, String tag) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Tags.CONTENT_URI);

    builder.withValue(TagsColumns.ID, operatorId);
    builder.withValue(TagsColumns.TAG, tag);

    return builder.build();
  }

  private static ContentProviderOperation buildOperatorsOperation(JSONObject jsonObject) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Operators.CONTENT_URI_JOINED);

    builder.withValue(OperatorsColumns.ID, jsonObject.getString(OperatorsColumns.ID));
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

  public static void cacheLogos(Context context) {
    Cursor cursor = context.getContentResolver().query(
            FoodtruckProvider.Operators.CONTENT_URI,
            new String[]{
                    OperatorsColumns.LOGO_URL,
                    OperatorsColumns.LOGO_BACKGROUND
            },
            null,
            null,
            null);

    if (cursor != null && cursor.moveToFirst()) {
      do {
        final String logoUrl = cursor.getString(cursor.getColumnIndex(OperatorsColumns.LOGO_URL));

        Glide.with(context)
                .load(logoUrl)
                .downloadOnly(new SimpleTarget<File>() {
                  @Override
                  public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                    Log.d("Utility", "downloaded " + logoUrl);
                  }
                });


/*



        Glide.with(context)
                .load(logoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new MapMarkerTransformation(context, color))
                .override(280, 280)
                .into(new SimpleTarget<GlideDrawable>() {
                  @Override
                  public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    Log.d("Utility", "cached marker image: " + logoUrl);
                  }
                });


*/

      } while (cursor.moveToNext());

      cursor.close();
    }
  }



  public static Bitmap createMapMarker(Context context, Bitmap logo, int color) {
    Bitmap markerBg = colorBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_map_marker_bg_bubble), color);
    Bitmap marker = Bitmap.createBitmap(markerBg.getWidth(), markerBg.getHeight(), markerBg.getConfig());
    Canvas canvas = new Canvas(marker);
    canvas.drawBitmap(markerBg, new Matrix(), null);
    canvas.drawBitmap(logo, (markerBg.getWidth() - logo.getWidth()) / 2, (markerBg.getHeight() - logo.getHeight()) / 2, null);
    return addDropShadow(marker, Color.GRAY, 10, 0, 2);
  }

  public static String getMarkerFileName(String operatorId, String imageId) {
    return "markerIcon-" + operatorId + "-" + imageId + ".png";
  }

  public static Bitmap getMarkerBitmap(Context context, String operatorId, String imageId) {
    final String fileName = getMarkerFileName(operatorId, imageId);

    File file = context.getFilesDir();
    File[] fileList = file.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File file, String s) {
        return s.equals(fileName);
      }
    });

    Bitmap bm = getBitmapFromAsset(context, "images/" + fileName);

    if (fileList.length > 0)
      bm = BitmapFactory.decodeFile(fileList[0].getPath());

    return scaleMarkerToDPI(context, bm);
  }

  public static Bitmap scaleMarkerToDPI(Context context, Bitmap bm) {
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    int w = Math.round(bm.getWidth() * (metrics.densityDpi / 540f));
    int h = Math.round(bm.getHeight() * (metrics.densityDpi / 540f));

    return Bitmap.createScaledBitmap(bm, w, h, false);
  }

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

  public static boolean isFirstLaunch(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_IS_FIRST_LAUNCH_PREF, true);
  }

  public static void hideSoftKeyboard(Activity activity) {
    InputMethodManager inputMethodManager =
            (InputMethodManager) activity.getSystemService(
                    Activity.INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(
            activity.getCurrentFocus().getWindowToken(), 0);
  }


  public static class UpdateDistanceTask extends AsyncTask<Void, Void, Integer> {
    public static final int LOCATIONS = 0;
    public static final int REGIONS = 1;
    private int mTable;
    private Cursor mCursor;
    private Context mContext;
    private ResultReceiver mReceiver;
    private long startTime = System.currentTimeMillis();

    public UpdateDistanceTask(Context context, int table, ResultReceiver receiver) {
      mContext = context;
      mTable = table;
      mReceiver = receiver;
      setCursor();
    }

    private void setCursor() {
      switch (mTable) {
        case LOCATIONS:
          mCursor = mContext.getContentResolver().query(
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
          mCursor = mContext.getContentResolver().query(
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
      int rowsUpdated = 0;
      if (mCursor != null && mCursor.moveToFirst()) {
        do {
          ContentValues values = new ContentValues();

          switch (mTable) {
            case LOCATIONS:
              values.put(LocationsColumns.DISTANCE, Utility.getOperatorDistance(mContext,
                      mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LATITUDE)),
                      mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LONGITUDE))));

              rowsUpdated += mContext.getContentResolver().update(
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
                      Utility.getOperatorDistance(mContext,
                              mCursor.getDouble(mCursor.getColumnIndex(RegionsColumns.LATITUDE)),
                              mCursor.getDouble(mCursor.getColumnIndex(RegionsColumns.LONGITUDE))));

              rowsUpdated += mContext.getContentResolver().update(
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
      if (mReceiver != null)
        mReceiver.send(FoodtruckResultReceiver.SUCCESS, null);
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

  public static class MapMarkerTransformation extends BitmapTransformation {
    private int mColor;
    private Context mContext;

    MapMarkerTransformation(Context context, int color) {
      super(context);

      mContext = context;
      mColor = color;
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
      Bitmap markerBg = Utility.colorBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_map_marker_bg_bubble), mColor);
      Bitmap bmMarkerAndLogo = Bitmap.createBitmap(markerBg.getWidth(), markerBg.getHeight(), markerBg.getConfig());

      Canvas canvas = new Canvas(bmMarkerAndLogo);
      canvas.drawBitmap(markerBg, new Matrix(), null);
      canvas.drawBitmap(toTransform, 0, 0, null);

      return Utility.addDropShadow(bmMarkerAndLogo, Color.GRAY, 10, 0, 2);
    }

    @Override
    public String getId() {
      return "co.pugo.apps.foodtruckfinder.MapMarkerTransformation";
    }
  }
}
