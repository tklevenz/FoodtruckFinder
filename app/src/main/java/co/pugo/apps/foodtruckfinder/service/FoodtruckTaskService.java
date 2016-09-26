package co.pugo.apps.foodtruckfinder.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
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

  public static final int TASK_FETCH_OPERATORS = 1;
  public static final int TASK_FETCH_DETAILS = 2;

  private final String FOODTRUCK_LOCATIONS_URL = "https://www.food-trucks-deutschland.de/api/app/getLocations.json";
  private final String FOODTRUCK_OPEARTOR_URL = "https://www.food-trucks-deutschland.de/api/app/getOperatorDetails.json";
  private final String LOGIN = "token";
  private final Context mContext;

  private OkHttpClient okHttpClient = new OkHttpClient();

  public FoodtruckTaskService() {
    mContext = this;
  }

  public FoodtruckTaskService(Context context) {
    mContext = context;
  }


  @Override
  public int onRunTask(TaskParams taskParams) {
    int result = GcmNetworkManager.RESULT_FAILURE;
    String response;
    try {
      if (taskParams.getExtras() != null) {

        int task = taskParams.getExtras().getInt(FoodtruckIntentService.TASK_TAG, 0);

        Log.d(LOG_TAG, "Task " + task);

        switch (task) {
          case TASK_FETCH_OPERATORS:
            response = fetchLocations();
            mContext.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, getLocationDataFromJson(response));
            Utility.setLastUpdatePref(mContext);

            // delete old data
            Calendar calendar = Calendar.getInstance();
            mContext.getContentResolver().delete(FoodtruckProvider.Locations.CONTENT_URI,
                    LocationsColumns.YEARDAY + " <= ?",
                    new String[]{"" + calcYearDay(calendar)});
            break;

          case TASK_FETCH_DETAILS:
            String operatorId = taskParams.getExtras().getString(FoodtruckIntentService.OPERATORID_TAG);
            response = fetchOperatorDetails(operatorId);
            storeHeaderImages(response);
            
            mContext.getContentResolver().insert(
                    FoodtruckProvider.OperatorDetails.withOperatorId(operatorId),
                    getDetailsContentValuesFromJson(response, operatorId));
            break;
        }
      }
      result = GcmNetworkManager.RESULT_SUCCESS;
    } catch (IOException | OperationApplicationException | RemoteException e) {
      e.printStackTrace();
    }

    return result;
  }



  private void storeHeaderImages(String response) {
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
        Log.d(LOG_TAG, fileName);
        Bitmap bitmap = Glide.with(mContext)
                .load(impressionUrl)
                .asBitmap()
                .centerCrop()
                .into(240, 240)
                .get();
        outputStream = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        outputStream.close();
      }
    } catch (JSONException | InterruptedException | ExecutionException | IOException e) {
      e.printStackTrace();
    }
  }

  private String fetchLocations() throws IOException {
    RequestBody requestBody = new FormBody.Builder()
            .add("date", "week")
            .build();

    Request request = new Request.Builder()
            .url(FOODTRUCK_LOCATIONS_URL)
            .header("Authorization", Credentials.basic(LOGIN, BuildConfig.FOODTRUCK_API_TOKEN))
            .post(requestBody)
            .build();

    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  private String fetchOperatorDetails(String operatorid) throws IOException {
    RequestBody requestBody = new FormBody.Builder()
            .add(FoodtruckIntentService.OPERATORID_TAG, operatorid)
            .build();

    Request request = new Request.Builder()
            .url(FOODTRUCK_OPEARTOR_URL)
            .header("Authorization", Credentials.basic(LOGIN, BuildConfig.FOODTRUCK_API_TOKEN))
            .post(requestBody)
            .build();

    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }


  private ArrayList getLocationDataFromJson(String json) {
    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    JSONObject jsonObject, jsonLocations;
    try {
      jsonObject = new JSONObject(json);
      if (jsonObject.length() > 0) {
        jsonLocations = jsonObject.getJSONObject("locations");
        Iterator it = jsonLocations.keys();
        while (it.hasNext()) {
          String key = (String) it.next();
          operations.add(buildLocationOperation(jsonLocations.getJSONObject(key)));

        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return operations;
  }

  private ContentValues getDetailsContentValuesFromJson(String json, String operatorId) {
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
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return contentValues;
  }

  private ContentProviderOperation buildLocationOperation(JSONObject jsonObject) {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Locations.CONTENT_URI);
    try {
      builder.withValue(LocationsColumns.OPERATOR_ID, jsonObject.getString(LocationsColumns.OPERATOR_ID));
      builder.withValue(LocationsColumns.OPERATOR_NAME, Html.fromHtml(jsonObject.getString(LocationsColumns.OPERATOR_NAME)).toString());
      builder.withValue(LocationsColumns.OPERATOR_OFFER, Html.fromHtml(jsonObject.getString(LocationsColumns.OPERATOR_OFFER)).toString());
      builder.withValue(LocationsColumns.OPERATOR_LOGO_URL, jsonObject.getString(LocationsColumns.OPERATOR_LOGO_URL));
      builder.withValue(LocationsColumns.LATITUDE, jsonObject.getDouble(LocationsColumns.LATITUDE));
      builder.withValue(LocationsColumns.LONGITUDE, jsonObject.getDouble(LocationsColumns.LONGITUDE));
      builder.withValue(LocationsColumns.DISTANCE,
              Utility.getOperatorDistance(mContext,
                      jsonObject.getDouble(LocationsColumns.LATITUDE),
                      jsonObject.getDouble(LocationsColumns.LONGITUDE)));
      builder.withValue(LocationsColumns.START_DATE, jsonObject.getString(LocationsColumns.START_DATE));
      builder.withValue(LocationsColumns.END_DATE, jsonObject.getString(LocationsColumns.END_DATE));
      builder.withValue(LocationsColumns.NAME, jsonObject.getString(LocationsColumns.NAME));
      builder.withValue(LocationsColumns.STREET, jsonObject.getString(LocationsColumns.STREET));
      builder.withValue(LocationsColumns.NUMBER, jsonObject.getString(LocationsColumns.NUMBER));
      builder.withValue(LocationsColumns.CITY, jsonObject.getString(LocationsColumns.CITY));
      builder.withValue(LocationsColumns.ZIPCODE, jsonObject.getString(LocationsColumns.ZIPCODE));
      builder.withValue(LocationsColumns.YEARDAY, getYearDay(jsonObject.getString(LocationsColumns.START_DATE)));

    } catch (JSONException e) {
      e.printStackTrace();
    }

    return builder.build();
  }

  private int getYearDay(String string) {
    Date date = Utility.parseDateString(string);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return calcYearDay(calendar);
  }

  private int calcYearDay(Calendar calendar) {
    return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR);
  }

}
