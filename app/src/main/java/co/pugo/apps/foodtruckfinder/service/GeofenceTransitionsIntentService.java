package co.pugo.apps.foodtruckfinder.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.common.api.ApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.ui.DetailActivity;
import co.pugo.apps.foodtruckfinder.ui.MainActivity;

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsIntentService extends IntentService {

  private static final String TAG = "GeofenceTransitionsIS";

  /**
   * This constructor is required, and calls the super IntentService(String)
   * constructor with the name for a worker thread.
   */
  public GeofenceTransitionsIntentService() {
    super(TAG);
  }

  /**
   * Handles incoming intents.
   * @param intent sent by Location Services. This Intent is provided to Location
   *               Services (inside a PendingIntent) when addGeofences() is called.
   */
  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
    GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
    if (geofencingEvent.hasError()) {
      String errorMessage = getErrorString(geofencingEvent.getErrorCode());
      Log.e(TAG, errorMessage);
      return;
    }

    // Get the transition type.
    int geofenceTransition = geofencingEvent.getGeofenceTransition();

    // Test that the reported transition was of interest.
    if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
        geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

      // Get the geofences that were triggered. A single event can trigger multiple geofences.
      List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

      // Get the transition details as a String.
      ArrayList<String> locationIds = getGeofenceTransitionDetails(geofenceTransition,
              triggeringGeofences);

      // Send notification and log the transition details.
      sendNotification(locationIds);
    } else {
      // Log the error.
      Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
    }
  }

  /**
   * Gets transition details and returns them as a formatted string.
   *
   * @param geofenceTransition    The ID of the geofence transition.
   * @param triggeringGeofences   The geofence(s) triggered.
   * @return                      The transition details formatted as String.
   */
  private ArrayList<String> getGeofenceTransitionDetails(int geofenceTransition, List<Geofence> triggeringGeofences) {

    // Get the Ids of each geofence that was triggered.
    ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
    for (Geofence geofence : triggeringGeofences) {
      triggeringGeofencesIdsList.add(geofence.getRequestId());
    }

    return triggeringGeofencesIdsList;
  }

  /**
   * Maps geofence transition types to their human-readable equivalents.
   *
   * @param transitionType    A transition type constant defined in Geofence
   * @return                  A String indicating the type of transition
   */
  private String getTransitionString(int transitionType) {
    switch (transitionType) {
      case Geofence.GEOFENCE_TRANSITION_ENTER:
        return getString(R.string.geofence_transition_entered);
      case Geofence.GEOFENCE_TRANSITION_EXIT:
        return getString(R.string.geofence_transition_exited);
      default:
        return getString(R.string.unknown_geofence_transition);
    }
  }

  private void sendNotification(ArrayList<String> locationIds) {
    Cursor cursor = getContentResolver().query(
            FoodtruckProvider.Locations.CONTENT_URI,
            new String[]{
                    LocationsColumns.OPERATOR_NAME,
                    LocationsColumns.OPERATOR_OFFER,
                    LocationsColumns.OPERATOR_ID,
                    LocationsColumns.START_DATE,
                    LocationsColumns.END_DATE,
                    LocationsColumns.OPERATOR_LOGO_URL
            },
            LocationsColumns.LOCATION_ID + " in ('" + TextUtils.join("','", locationIds) + "')",
            null,
            null
    );

    ArrayList<String> operatorNames = new ArrayList<>();
    String operatorId = "";
    String logoUrl = "";
    String openTill = "";
    String operatorOffer = "";
    int i = 0;

    if (cursor != null && cursor.moveToFirst()) {
      do {
        if (Utility.isActiveNow(
                cursor.getString(cursor.getColumnIndex(LocationsColumns.START_DATE)),
                cursor.getString(cursor.getColumnIndex(LocationsColumns.END_DATE)))) {

          operatorNames.add(cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_NAME)));

          if (i == 0) {
            operatorId = cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_ID));
            logoUrl = cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL));
            operatorOffer = cursor.getString(cursor.getColumnIndex(LocationsColumns.OPERATOR_OFFER));
            openTill = "Open till: " + Utility.getFormattedTime(cursor.getString(cursor.getColumnIndex(LocationsColumns.END_DATE)));
          }

          i++;
        }
      } while (cursor.moveToNext());

      cursor.close();
    }

    String notificationTitle = (operatorNames.size() == 1) ? operatorNames.get(0) : getString(R.string.geofence_notification_title);

    // Create an explicit content Intent that starts the main Activity.
    Intent notificationIntent;

    if (operatorNames.size() == 1) {
      notificationIntent = new Intent(getApplicationContext(), DetailActivity.class);
      notificationIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, operatorId);
    } else {
      notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
    }

    // Construct a task stack.
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

    // Add the main Activity to the task stack as the parent.
    stackBuilder.addParentStack(MainActivity.class);

    // Push the content Intent onto the stack.
    stackBuilder.addNextIntent(notificationIntent);



    // Get a PendingIntent containing the entire back stack.
    PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    // Get a notification builder that's compatible with platform versions >= 4
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

    NotificationCompat.InboxStyle inboxStyle =
            new NotificationCompat.InboxStyle();

    inboxStyle.setBigContentTitle(notificationTitle);

    Bitmap largeIcon = null;

    if (operatorNames.size() > 1) {
      largeIcon = BitmapFactory.decodeResource(getResources(),
              R.mipmap.ic_launcher);
    } else {
      try {
        largeIcon = Glide.with(getApplicationContext())
                .asBitmap()
                .load(logoUrl)
                .apply(new RequestOptions().fitCenter())
                .submit(192, 192)
                .get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    // Define the notification settings.
    builder.setSmallIcon(R.drawable.ic_notification_truck)
            .setLargeIcon(largeIcon)
            .setColor(Color.RED)
            .setContentTitle(notificationTitle)
            .setContentIntent(notificationPendingIntent);

    if (operatorNames.size() > 1) {
      for (String operator : operatorNames) {
        inboxStyle.addLine(operator);
      }
    } else {
      inboxStyle.addLine(operatorOffer);
      inboxStyle.addLine(openTill);
    }

    builder.setStyle(inboxStyle);

    // Dismiss notification once the user touches it.
    builder.setAutoCancel(true);

    // Get an instance of the Notification manager
    NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    // Issue the notification
    mNotificationManager.notify(0, builder.build());
  }


  /**
   * Returns the error string for a geofencing error code.
   */
  public String getErrorString(int errorCode) {
    Resources mResources = getResources();
    switch (errorCode) {
      case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
        return mResources.getString(R.string.geofence_not_available);
      case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
        return mResources.getString(R.string.geofence_too_many_geofences);
      case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
        return mResources.getString(R.string.geofence_too_many_pending_intents);
      default:
        return mResources.getString(R.string.unknown_geofence_error);
    }
  }
}