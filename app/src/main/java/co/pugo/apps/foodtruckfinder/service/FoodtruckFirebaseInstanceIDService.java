package co.pugo.apps.foodtruckfinder.service;

import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;

import co.pugo.apps.foodtruckfinder.backend.registration.Registration;

/**
 * Created by tobias on 30.6.2017.
 */

public class FoodtruckFirebaseInstanceIDService extends FirebaseInstanceIdService {
  private static final String TAG = "InstanceIDService";

  @Override
  public void onTokenRefresh() {
    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
    Log.d(TAG, "Refreshed token: " + refreshedToken);

    try {
      sendRegistrationToServer(refreshedToken);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void sendRegistrationToServer(String refreshedToken) throws IOException{
    Registration.Builder builder = new Registration.Builder(AndroidHttp.newCompatibleTransport(),
            new AndroidJsonFactory(), null)
            // Need setRootUrl and setGoogleClientRequestInitializer only for local testing,
            // otherwise they can be skipped
            .setRootUrl("http://10.0.2.2:8080/_ah/api/")
            .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
              @Override
              public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest)
                      throws IOException {
                abstractGoogleClientRequest.setDisableGZipContent(true);
              }
            });
    Registration regService = builder.build();
    regService.register(refreshedToken).execute();
  }
}
