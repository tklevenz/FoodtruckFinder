package co.pugo.apps.foodtruckfinder.ui;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;

public class SettingsActivity extends AppCompatPreferenceActivity {

  public static final int PLACE_PICKER_REQUEST = 9999;
  private SettingsFragment mSettingsFragment;

  @BindView(R.id.toolbar) Toolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);
    ButterKnife.bind(this);


    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    mSettingsFragment = new SettingsFragment();


    getFragmentManager().beginTransaction()
            .replace(R.id.content, mSettingsFragment)
            .commit();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == PLACE_PICKER_REQUEST) {
      if (resultCode == RESULT_OK) {
        Place place = PlacePicker.getPlace(this, data);
        Utility.updateLocationSharedPref(this, place.getLatLng().latitude, place.getLatLng().longitude);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(getString(R.string.pref_custom_location_key), place.getAddress().toString());
        prefsEditor.apply();

        mSettingsFragment.findPreference(getString(R.string.pref_custom_location_key)).setSummary(place.getAddress());

        CheckBoxPreference deviceLocationPref =
                (CheckBoxPreference) mSettingsFragment.findPreference(getString(R.string.pref_use_location_key));

        if (deviceLocationPref.isChecked())
          deviceLocationPref.setChecked(false);
      }
    }
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }


  public static class SettingsFragment extends PreferenceFragment
          implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    private static final String KM = "km";
    private static final String MI = "mi.";
    private Preference mCustomLocationPref;
    private Preference mDistanceUnitsPref;
    private Preference mUseDeviceLocationPref;
    private EditTextPreference mLocationRadiusPref;
    private String mUnit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.xml.preferences);

      mCustomLocationPref = findPreference(getString(R.string.pref_custom_location_key));
      setPreferenceSummary(mCustomLocationPref);

      mDistanceUnitsPref = findPreference(getString(R.string.pref_distance_unit_key));
      setPreferenceSummary(mDistanceUnitsPref);

      mUseDeviceLocationPref = findPreference(getString(R.string.pref_use_location_key));
      mUseDeviceLocationPref.setOnPreferenceChangeListener(this);

      mLocationRadiusPref = (EditTextPreference) findPreference(getString(R.string.pref_location_radius_key));
      setPreferenceSummary(mLocationRadiusPref);
    }

    private void setPreferenceSummary(Preference preference) {
      if (preference.getKey().equals(getString(R.string.pref_location_radius_key))) {
        mUnit = mDistanceUnitsPref.getSharedPreferences()
                .getString(getString(R.string.pref_distance_unit_key), getString(R.string.pref_unit_killometers))
                .equals(getString(R.string.pref_unit_killometers)) ? KM : MI;
        preference.setSummary(PreferenceManager
                                      .getDefaultSharedPreferences(preference.getContext())
                                      .getString(preference.getKey(), "") + mUnit);
      } else {
        preference.setSummary(PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), ""));
      }
    }

    @Override
    public void onResume() {
      super.onResume();
      getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
      super.onPause();
      getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      if (key.equals(getString(R.string.pref_distance_unit_key))) {
        mDistanceUnitsPref.setSummary(sharedPreferences.getString(key, ""));
        mUnit = sharedPreferences.getString(key, "").equals(getString(R.string.pref_unit_killometers)) ? KM : MI;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mDistanceUnitsPref.getContext());
        int radius = Integer.parseInt(prefs.getString(getString(R.string.pref_location_radius_key), "0"));
        radius = sharedPreferences.getString(key, "").equals(getString(R.string.pref_unit_killometers))
                ? (int) Math.round(radius * 1.60934)
                : (int) Math.round(radius * 0.621371);
        mLocationRadiusPref.setText(radius + "");
      } else if (key.equals(getString(R.string.pref_custom_location_key))) {
        mCustomLocationPref.setSummary(sharedPreferences.getString(key, ""));
      } else if (key.equals(getString(R.string.pref_location_radius_key))) {
        mLocationRadiusPref.setSummary(sharedPreferences.getString(key, "200") + mUnit);
      }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
      if (preference.getKey().equals(getString(R.string.pref_use_location_key))) {
        CheckBoxPreference pref = (CheckBoxPreference) preference;
        if (pref.isEnabled()) {
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pref.getContext());
          SharedPreferences.Editor prefsEditor = prefs.edit();
          prefsEditor.putString(getString(R.string.pref_custom_location_key), getString(R.string.pref_no_custom_location));
          prefsEditor.apply();
        }
      }
      return true;
    }
  }
}
