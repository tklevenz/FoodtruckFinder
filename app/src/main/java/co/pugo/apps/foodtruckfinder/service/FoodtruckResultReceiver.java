package co.pugo.apps.foodtruckfinder.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by tobias on 29.11.2016.
 */

public class FoodtruckResultReceiver extends ResultReceiver {
  public static int SUCCESS = 1;
  private Receiver mReceiver;

  public FoodtruckResultReceiver(Handler handler) {
    super(handler);
  }

  public interface Receiver {
    void onReceiveResult(int resultCode, Bundle resultData);
  }

  public void setReceiver(Receiver receiver) {
    mReceiver = receiver;
  }

  @Override
  protected void onReceiveResult(int resultCode, Bundle resultData) {
    if (mReceiver != null)
      mReceiver.onReceiveResult(resultCode, resultData);
  }
}