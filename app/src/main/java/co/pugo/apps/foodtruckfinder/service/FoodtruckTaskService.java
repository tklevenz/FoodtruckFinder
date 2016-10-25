package co.pugo.apps.foodtruckfinder.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.os.ResultReceiver;
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
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import co.pugo.apps.foodtruckfinder.BuildConfig;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import co.pugo.apps.foodtruckfinder.data.TagsColumns;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;
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

  private final String FOODTRUCK_API_URL = "https://www.food-trucks-deutschland.de/api/app/";
  private final String LOGIN = "token";
  private final String AUTH = "Authorization";
  private final String CREDENTIALS = Credentials.basic(LOGIN, BuildConfig.FOODTRUCK_API_TOKEN);

  private final Context mContext;

  private OkHttpClient okHttpClient = new OkHttpClient();


  public FoodtruckTaskService() {
    mContext = this;
  }

  public FoodtruckTaskService(Context context) {
    mContext = context;
  }


  private String fetchLocations() throws IOException {
    Calendar calendar = Calendar.getInstance();

    RequestBody requestBody = new FormBody.Builder()
            .add("date", "weekfull")
            .add("timezone", calendar.getTimeZone().getDisplayName())
            .build();


    Request request = new Request.Builder()
            .url(FOODTRUCK_API_URL + "getLocations.json")
            .header(AUTH, CREDENTIALS)
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
            .url(FOODTRUCK_API_URL + "getOperatorDetails.json")
            .header(AUTH, CREDENTIALS)
            .post(requestBody)
            .build();

    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  private String fetchOperators() throws IOException {
    Request request = new Request.Builder()
            .url(FOODTRUCK_API_URL + "getOperators.json")
            .header(AUTH, CREDENTIALS)
            .build();

    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }


  @Override
  public int onRunTask(TaskParams taskParams) {
    int result = GcmNetworkManager.RESULT_FAILURE;
    String response;
    try {
      if (taskParams.getExtras() != null) {

        int task = taskParams.getExtras().getInt(FoodtruckIntentService.TASK_TAG, 0);
        ResultReceiver receiver = taskParams.getExtras().getParcelable(FoodtruckIntentService.RECEIVER_TAG);

        Log.d(LOG_TAG, "Task " + task);

        switch (task) {
          case TASK_FETCH_LOCATIONS:
            response = fetchLocations();
            mContext.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, getLocationDataFromJson(response));
            // delete old data
            int deletedRows = mContext.getContentResolver().delete(FoodtruckProvider.Locations.CONTENT_URI,
                    LocationsColumns.END_DATE + " <= ?",
                    new String[]{
                            Utility.getDateNow()
                    });
            Log.d(LOG_TAG, deletedRows + " rows deleted");

            receiver.send(FoodtruckResultReceiver.SUCCESS, null);

            break;

          case TASK_FETCH_DETAILS:
            String operatorId = taskParams.getExtras().getString(FoodtruckIntentService.OPERATORID_TAG);
            response = fetchOperatorDetails(operatorId);
            storeHeaderImages(response);

            mContext.getContentResolver().insert(
                    FoodtruckProvider.OperatorDetails.withOperatorId(operatorId),
                    getDetailsContentValuesFromJson(response, operatorId));
            break;

          case TASK_FETCH_OPERATORS:
            // dump tables
            mContext.getContentResolver().delete(FoodtruckProvider.Operators.CONTENT_URI, null, null);
            mContext.getContentResolver().delete(FoodtruckProvider.Tags.CONTENT_URI, null, null);
            // get operators and tags data
            response = fetchOperators();
            mContext.getContentResolver().applyBatch(FoodtruckProvider.AUTHORITY, getOperatorsFromJson(response));

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
          if (!entryExistsInDatabase(jsonLocations.getJSONObject(key)))
            operations.add(buildLocationOperation(jsonLocations.getJSONObject(key)));
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return operations;
  }


  private ContentProviderOperation buildLocationOperation(JSONObject jsonObject) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Locations.CONTENT_URI);

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
    builder.withValue(LocationsColumns.LOCATION_NAME, jsonObject.getString("name"));
    builder.withValue(LocationsColumns.STREET, jsonObject.getString(LocationsColumns.STREET));
    builder.withValue(LocationsColumns.NUMBER, jsonObject.getString(LocationsColumns.NUMBER));
    builder.withValue(LocationsColumns.CITY, jsonObject.getString(LocationsColumns.CITY));
    builder.withValue(LocationsColumns.ZIPCODE, jsonObject.getString(LocationsColumns.ZIPCODE));

    return builder.build();
  }

  private boolean entryExistsInDatabase(JSONObject jsonObject) {
    Cursor cursor = null;
    try {
      String operatorId = jsonObject.getString(LocationsColumns.OPERATOR_ID);
      cursor = mContext.getContentResolver().query(
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

  private ArrayList getOperatorsFromJson(String json) {
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
          if (!tag.equals(""))
            operations.add(buildTagsOperation(operator.getString(OperatorsColumns.ID), tag));
        }

      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return operations;
  }

  private ContentProviderOperation buildTagsOperation(String operatorId, String tag) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Tags.CONTENT_URI);

    builder.withValue(TagsColumns.ID, operatorId);
    builder.withValue(TagsColumns.TAG, tag);

    return builder.build();
  }

  private ContentProviderOperation buildOperatorsOperation(JSONObject jsonObject) throws JSONException {
    ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(FoodtruckProvider.Operators.CONTENT_URI);

    builder.withValue(OperatorsColumns.ID, jsonObject.getString(OperatorsColumns.ID));
    builder.withValue(OperatorsColumns.NAME, Html.fromHtml(jsonObject.getString(OperatorsColumns.NAME)).toString());
    builder.withValue(OperatorsColumns.OFFER, Html.fromHtml(jsonObject.getString(OperatorsColumns.OFFER)).toString());
    builder.withValue(OperatorsColumns.LOGO_URL, jsonObject.getString(OperatorsColumns.LOGO_URL));
    builder.withValue(OperatorsColumns.REGION, jsonObject.getString(OperatorsColumns.REGION));
    String logoBackground = jsonObject.getString(OperatorDetailsColumns.LOGO_BACKGROUND);
    if (logoBackground.length() == 4) {
      logoBackground = "#" + logoBackground.substring(1, 2) + logoBackground.substring(1, 2)
              + logoBackground.substring(2, 3) + logoBackground.substring(2, 3)
              + logoBackground.substring(3, 4) + logoBackground.substring(3, 4);
    }
    builder.withValue(OperatorsColumns.LOGO_BACKGROUND, logoBackground);

    return builder.build();
  }

}
