package co.pugo.apps.foodtruckfinder.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.google.android.gms.gcm.TaskParams;

/**
 * Created by tobias on 1.9.2016.
 */
public class FoodtruckIntentService extends IntentService {

  public static final String TASK_TAG = "task";
  public static final String OPERATORID_TAG = "operatorid";
  public static final String RECEIVER_TAG = "receiver";

  public FoodtruckIntentService() {
    super(FoodtruckIntentService.class.getName());
  }

  public FoodtruckIntentService(String name) {
    super(name);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    FoodtruckTaskService foodtruckTaskService = new FoodtruckTaskService(this);
    ResultReceiver receiver = intent.getParcelableExtra(RECEIVER_TAG);

    TaskParams taskParams = new TaskParams(null);

    int task = intent.getIntExtra(TASK_TAG, 0);
    if (task > 0) {
      Bundle args = new Bundle();
      args.putInt(TASK_TAG, task);

      if (receiver != null)
        args.putParcelable(RECEIVER_TAG, receiver);

      String operatorId = intent.getStringExtra(OPERATORID_TAG);
      if (operatorId != null)
        args.putString(OPERATORID_TAG, operatorId);

      taskParams = new TaskParams(TASK_TAG, args);
    }

    foodtruckTaskService.onRunTask(taskParams);
  }
}
