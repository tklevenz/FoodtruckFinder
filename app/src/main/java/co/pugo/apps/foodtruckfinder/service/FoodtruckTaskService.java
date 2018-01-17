package co.pugo.apps.foodtruckfinder.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


import co.pugo.apps.foodtruckfinder.BuildConfig;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by tobias on 1.9.2016.
 */
public class FoodtruckTaskService extends GcmTaskService {
  private static final String LOG_TAG = FoodtruckTaskService.class.getSimpleName();

  public static final int TASK_FETCH_LOCATIONS = 1;
  public static final int TASK_FETCH_DETAILS = 2;
  public static final int TASK_FETCH_OPERATORS = 3;
  public static final int TASK_FETCH_REGIONS = 4;
  public static final int TASK_SEND_LOCATION = 5;
  public static final int TASK_INIT_TABLES = 6;

  private final String FOODTRUCK_API_URL = "https://www.food-trucks-deutschland.de/api/app/";
  private final String LOGIN = "token";
  private final String AUTH = "Authorization";
  private final String CREDENTIALS = Credentials.basic(LOGIN, BuildConfig.FOODTRUCK_API_TOKEN);


  private Context mContext;

  private OkHttpClient okHttpClient = new OkHttpClient();

  public FoodtruckTaskService() {}

  public FoodtruckTaskService(Context context) {
    mContext = context;
  }


  private String fetchLocations() throws IOException {
/*    Calendar calendar = Calendar.getInstance();

    RequestBody requestBody = new FormBody.Builder()
            .add("date", "weekfull")
            .add("timezone", calendar.getTimeZone().getDisplayName())
            .build();


    Request request = new Request.Builder()
            .url(FOODTRUCK_API_URL + "getLocations.json")
            .header(AUTH, CREDENTIALS)
            .post(requestBody)
            .build();
*/
    Request request = new Request.Builder()
            .url("http://foodtruckfinder-1473089412231.appspot.com/ftd?fetch=getLocations.json&date=weekfull&output=gzip")
            .build();



    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  private String fetchOperatorDetails(String operatorid) throws IOException {
    RequestBody requestBody = new FormBody.Builder()
            .add(FoodtruckIntentService.OPERATORID_TAG, operatorid)
            .build();

    Request request = new Request.Builder()
            .url(FOODTRUCK_API_URL + "getOperatorDetails.json")
            .header(AUTH, CREDENTIALS)
            .post(requestBody)
            .build();

    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  private String fetchOperators() throws IOException {
    Request request = new Request.Builder()
            .url("http://foodtruckfinder-1473089412231.appspot.com/ftd?fetch=getOperators.json")
            .build();


    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  private String fetchRegions() throws IOException {
    Request request = new Request.Builder()
            .url("http://foodtruckfinder-1473089412231.appspot.com/ftd?fetch=getRegions.json")
            .build();


    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  private String fetchImages() throws IOException {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    int lastchange = preferences.getInt(Utility.LAST_IMAGE_TIMESTAMP_PREF, 0);

    Request request = new Request.Builder()
            .url("http://foodtruckfinder-1473089412231.appspot.com/ftd?fetch=checkImages.json&lastchange=" + lastchange)
            .build();

    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  private void sendLocation(String lat, String lng, String tz, String type) throws IOException {
    RequestBody requestBody = new FormBody.Builder()
            .add("latitude", lat)
            .add("longitude", lng)
            .add("timezone", tz)
            .add("type", type)
            .build();

    Request request = new Request.Builder()
            .url("https://api.foodtrucks-worldwide.com/app/setUserPosition.json")
            .header(AUTH, CREDENTIALS)
            .post(requestBody)
            .build();

    okHttpClient.newCall(request).execute();
  }


  @Override
  public int onRunTask(TaskParams taskParams) {
    int result = GcmNetworkManager.RESULT_FAILURE;
    mContext = (mContext != null) ? mContext : getApplicationContext();
    String response;
    try {
      if (taskParams.getExtras() != null) {

        int task = taskParams.getExtras().getInt(FoodtruckIntentService.TASK_TAG, 0);

        Log.d(LOG_TAG, "Task " + task);

        switch (task) {
          case TASK_FETCH_LOCATIONS:
            response = fetchLocations();
            ArrayList<ContentProviderOperation> locationData = Utility.getLocationDataFromJson(response, mContext);
            mContext.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, locationData);

            // delete old data
            int deletedRows = mContext.getContentResolver().delete(FoodtruckProvider.Locations.CONTENT_URI,
                    "datetime(" + LocationsColumns.END_DATE + ") <= datetime('" + Utility.getTimeNow() + "')",
                    null);
            Log.d(LOG_TAG, deletedRows + " rows deleted");

            new Utility.UpdateDistanceTask(mContext, Utility.UpdateDistanceTask.LOCATIONS).execute();

            Utility.cacheLogos(mContext);

            if(locationData.size() > 0)
              Utility.setLastUpdatePref(mContext, task);

            break;

          case TASK_FETCH_DETAILS:
            String operatorId = taskParams.getExtras().getString(FoodtruckIntentService.OPERATORID_TAG);
            response = fetchOperatorDetails(operatorId);

            ContentValues contentValues = Utility.getDetailsContentValuesFromJson(response, operatorId);
            boolean hasNewData = false;

            if (Utility.dataExists(mContext, FoodtruckProvider.OperatorDetails.withOperatorId(operatorId))) {
              Cursor cursor = mContext.getContentResolver().query(
                      FoodtruckProvider.OperatorDetails.withOperatorId(operatorId),
                      null, null, null, null);
              if (cursor != null) {
                for (Field f : OperatorDetailsColumns.class.getDeclaredFields()) {
                  String value = (String) f.get(null);
                  if (!value.equals(OperatorDetailsColumns._ID)) {
                    hasNewData = !cursor.getString(cursor.getColumnIndex(value)).equals(contentValues.getAsString(value));
                  }
                }

                cursor.close();
              }
            } else {
              hasNewData = true;
            }

            if (hasNewData) {
              mContext.getContentResolver().insert(
                      FoodtruckProvider.OperatorDetails.withOperatorId(operatorId),
                      contentValues);
            }

            ArrayList<ContentProviderOperation> impressions = Utility.getImpressionsContentValuesFromJson(response, operatorId);
            if (impressions.size() > 0) {
              mContext.getContentResolver().delete(FoodtruckProvider.Impressions.withId(operatorId), null, null);
              mContext.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, impressions);
            }


            break;

          case TASK_FETCH_OPERATORS:
            // get operators and tags data
            response = fetchOperators();

            ArrayList<ContentProviderOperation> operatorOperations = Utility.getOperatorsFromJson(response, mContext);

            // apply batch insert
            if (operatorOperations.size() > 0)
              mContext.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, operatorOperations);

            Utility.setLastUpdatePref(mContext, task);

            break;

          case TASK_FETCH_REGIONS:
            response = fetchRegions();

            ArrayList<ContentProviderOperation> regionsOperations = Utility.getRegionsDataFromJson(response, mContext);

            if (regionsOperations.size() > 0)
              mContext.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, regionsOperations);

            Utility.setLastUpdatePref(mContext, task);

            break;
          case TASK_SEND_LOCATION:

            Bundle extras = taskParams.getExtras();
            sendLocation(
                    extras.getString("latitude"),
                    extras.getString("longitude"),
                    extras.getString("timezone"),
                    extras.getString("type")
            );

            break;

          case TASK_INIT_TABLES:
            if (!Utility.dataExists(mContext, FoodtruckProvider.Regions.CONTENT_URI))
              Utility.initRegionsTable(mContext);


            if (!Utility.dataExists(mContext, FoodtruckProvider.Operators.CONTENT_URI))
              Utility.initOperatorsTable(mContext);

            break;
        }
      }
      result = GcmNetworkManager.RESULT_SUCCESS;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }


}
