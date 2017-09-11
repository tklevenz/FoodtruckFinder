package co.pugo.apps.foodtruckfinder.ui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.WelcomeSlidePagerAdapter;

public class WelcomeActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        EditRadiusDialog.EditRadiusDialogListener {

  private static final String LOG_TAG = WelcomeActivity.class.getSimpleName();
  @BindView(R.id.view_pager) WelcomeSlideViewPager mViewPager;
  @BindView(R.id.btn_next) Button mBtnNext;

  private WelcomeSlidePagerAdapter mPagerAdapeter;
  private int[] mLayouts;
  private static final int LOCATION_PERMISSION_REQUEST = 0;
  private GoogleApiClient mGoogleApiClient;
  private Context mContext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_welcome);
    ButterKnife.bind(this);

    mContext = getApplicationContext();

    // setup google api client for location api access
    mGoogleApiClient = new GoogleApiClient
            .Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();


    if (Build.VERSION.SDK_INT >= 21) {
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    mLayouts = new int[]{
            R.layout.slide_welcome,
            R.layout.slide_location,
            R.layout.slide_radius,
            R.layout.slide_nearby_notificaton,
            R.layout.slide_thanks
    };

    mPagerAdapeter = new WelcomeSlidePagerAdapter(this, mLayouts);

    mViewPager.setAdapter(mPagerAdapeter);

    final String radius = getString(R.string.default_radius) + " " + getString(R.string.pref_unit_killometers);

    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override
      public void onPageSelected(int position) {
        switch (mLayouts[position]) {
          case R.layout.slide_location:
            mBtnNext.setText(R.string.btn_next);
            mViewPager.setPagingEnabled(false);
            mBtnNext.setEnabled(false);
            mBtnNext.setTextColor(Color.argb(100, 255, 255, 255));
            break;
          case R.layout.slide_thanks:
            mBtnNext.setText(R.string.btn_next_ok);
            break;
          case R.layout.slide_radius:
            mBtnNext.setText(R.string.btn_next);
            TextView tv = (TextView) mViewPager.findViewById(R.id.slide_radius_description);
            tv.setText(getString(R.string.slide_radius_desc, radius));
            Button btn = (Button) mViewPager.findViewById(R.id.btn_change_radius);
            btn.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                EditRadiusDialog editRadiusDialog = new EditRadiusDialog();
                editRadiusDialog.show(fragmentManager, "fragment_edit_radius");
              }
            });
            break;
          case R.layout.slide_nearby_notificaton:
            mBtnNext.setText(R.string.btn_next);
            SwitchCompat switchCompat = (SwitchCompat)mViewPager.findViewById(R.id.switch_nearby);
            switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
              @Override
              public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor prefsEdit = preferences.edit();
                prefsEdit.putBoolean(getString(R.string.pref_nearby_key), isChecked);
                prefsEdit.apply();
              }
            });
            break;
          default:
            mBtnNext.setText(R.string.btn_next);
        }
      }

      @Override
      public void onPageScrollStateChanged(int state) {

      }
    });

    mBtnNext.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        int position = mViewPager.getCurrentItem();
        if (mLayouts[position] == R.layout.slide_thanks) {
          setFirstLaunchPref();
          finish();
        } else {
          mViewPager.setCurrentItem(position + 1);
        }
      }
    });
  }

  private void setFirstLaunchPref() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor prefsEdit = prefs.edit();
    prefsEdit.putBoolean(Utility.KEY_IS_FIRST_LAUNCH_PREF, false);
    prefsEdit.apply();
  }

  @Override
  public void onBackPressed() {
  }

  public void requestLocationAccess(View view) {
    if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              LOCATION_PERMISSION_REQUEST);

    } else {
      mViewPager.setPagingEnabled(true);
      mBtnNext.setEnabled(true);
      mBtnNext.setTextColor(Color.WHITE);
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
          throws SecurityException {
    switch (requestCode) {
      case LOCATION_PERMISSION_REQUEST: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mViewPager.setPagingEnabled(true);
          mBtnNext.setEnabled(true);
          mBtnNext.setTextColor(Color.WHITE);
          mGoogleApiClient.connect();
        }
      }
    }
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_use_location_key), true)) {
      Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
      if (location == null) {
        Log.d(LOG_TAG, "Failed to get location...");
      } else {
        Log.d(LOG_TAG, location.toString());
        Utility.updateLocationSharedPref(this, location);
      }
    }
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

  }

  @Override
  public void onFinishEditDialog(String input) {
    TextView tv = (TextView) mViewPager.findViewById(R.id.slide_radius_description);
    tv.setText(getString(R.string.slide_radius_desc, input + " " + getString(R.string.pref_unit_killometers)));

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor prefsEdit = prefs.edit();
    prefsEdit.putString(getString(R.string.pref_location_radius_key), input);
    prefsEdit.apply();
  }
}
