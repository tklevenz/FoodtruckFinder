package co.pugo.apps.foodtruckfinder.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.BuildConfig;
import co.pugo.apps.foodtruckfinder.R;

/**
 * Created by tobia on 14.11.2017.
 */

public class SettingsAboutActivity extends AppCompatPreferenceActivity {

  private SettingsFragment mSettingsFragment;

  @BindView(R.id.toolbar) Toolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings_about);
    ButterKnife.bind(this);

    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    mSettingsFragment = new SettingsFragment();


    getFragmentManager().beginTransaction()
            .replace(R.id.content, mSettingsFragment)
            .commit();

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public static class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.xml.preferences_about);

      Preference versionPref = findPreference(getString(R.string.pref_version_key));
      versionPref.setSummary(BuildConfig.VERSION_NAME);

      Preference privacyPref = findPreference(getString(R.string.pref_privacy_key));
      privacyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.iubenda.com/privacy-policy/8209160")));
          return true;
        }
      });

      Preference craftplacesPref = findPreference(getString(R.string.pref_craftplaces_key));
      craftplacesPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://craftplaces.com")));
          return true;
        }
      });
    }
  }
}
