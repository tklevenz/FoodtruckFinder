package co.pugo.apps.foodtruckfinder.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.common.api.ApiException;

import java.util.List;

import co.pugo.apps.foodtruckfinder.R;

/**
 * Created by tobias on 29.6.2017.
 */

public class GeofenceTransitionsIntentService extends IntentService {

  private static final String TAG = "GeofenceTransitionsIS";

  public GeofenceTransitionsIntentService() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
    GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
    if (geofencingEvent.hasError()) {
      String errorMessage = GeofenceErrorMessages.getErrorString(this,
              geofencingEvent.getErrorCode());
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
      String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition,
              triggeringGeofences);

      // Send notification and log the transition details.
      sendNotification(geofenceTransitionDetails);
      Log.i(TAG, geofenceTransitionDetails);
    } else {
      // Log the error.
      Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
    }
  }

  private String getGeofenceTransitionDetails(int geofenceTransition, List<Geofence> triggeringGeofences) {
    return null;
  }

  private void sendNotification(String geofenceTransitionDetails) {
  }

}

class GeofenceErrorMessages {
  /**
   * Prevents instantiation.
   */
  private GeofenceErrorMessages() {}

  /**
   * Returns the error string for a geofencing exception.
   */
  public static String getErrorString(Context context, Exception e) {
    if (e instanceof ApiException) {
      return getErrorString(context, ((ApiException) e).getStatusCode());
    } else {
      return context.getResources().getString(R.string.unknown_geofence_error);
    }
  }

  /**
   * Returns the error string for a geofencing error code.
   */
  public static String getErrorString(Context context, int errorCode) {
    Resources mResources = context.getResources();
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