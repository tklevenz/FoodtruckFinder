package co.pugo.apps.foodtruckfinder.ui;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

import co.pugo.apps.foodtruckfinder.R;

/**
 * Created by tobias on 13.9.2016.
 */
public class PlacePickerPreference extends Preference {


  public PlacePickerPreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    setWidgetLayoutResource(R.layout.place_picker_preference);
  }

  @Override
  protected void onClick() {
    Context context = getContext();
    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
    Activity settingsActivity = (SettingsActivity) context;
    try {
      settingsActivity.startActivityForResult(builder.build(settingsActivity), SettingsActivity.PLACE_PICKER_REQUEST);
    } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
      e.printStackTrace();
    }
  }
}
