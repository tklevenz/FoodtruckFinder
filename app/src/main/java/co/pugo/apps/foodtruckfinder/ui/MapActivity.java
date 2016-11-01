package co.pugo.apps.foodtruckfinder.ui;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;

/**
 * Created by tobias on 3.10.2016.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LoaderManager.LoaderCallbacks<Cursor> {
  public static final String LONGITUDE_TAG = "longitude_tag";
  public static final String LATITUDE_TAG = "latitude_tag";
  public static final String LOGO_URL_EXTRA = "logo_tag";
  private double mLongitude;
  private double mLatitude;
  private String mLogoUrl;
  private SupportMapFragment mMapFragment;
  private Cursor mCursor;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);

    mLongitude = getIntent().getDoubleExtra(LONGITUDE_TAG, 0);
    mLatitude = getIntent().getDoubleExtra(LATITUDE_TAG, 0);
    mLogoUrl = getIntent().getStringExtra(LOGO_URL_EXTRA);

    mMapFragment =
            (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);

    getLoaderManager().initLoader(1, null, this);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      getWindow().getDecorView().setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

      CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) toolbar.getLayoutParams();
      int statusBarHeight = Utility.getStatusBarHeight(this);
      params.height += statusBarHeight;
      toolbar.setLayoutParams(params);

      toolbar.setPadding(0, statusBarHeight, 0, 0);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
      }
    }


    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    onBackPressed();
    return true;
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    if (mCursor != null && mCursor.moveToFirst()) {
      while (mCursor.moveToNext()) {
        int color;
        try {
          color = Color.parseColor(mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.LOGO_BACKGROUND)));
        } catch (Exception e) {
          color = Color.WHITE;
          e.printStackTrace();
        }
        Bitmap markerBg = Utility.colorBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_marker_bg_bubble), color);
        Marker marker = googleMap.addMarker(
                new MarkerOptions()
                        .title(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_NAME)))
                        .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.blank_16dp)))
                        .position(new LatLng(
                                mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LATITUDE)),
                                mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LONGITUDE)))));
        String logoUrl = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL));

        // if marker is current truck place on top
        if (logoUrl.equals(mLogoUrl)) {
          marker.setZIndex(99999);
        }
        Utility.loadMapMarkerIcon(this, marker, logoUrl, 280, markerBg);
      }
    }
    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 12));
  }

  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    super.onSaveInstanceState(outState, outPersistentState);
    Log.d("MapActivity", "onSaveInstanceState");
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    Log.d("MapActivity", "onRestoreInstanceState");
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(this,
            FoodtruckProvider.Locations.CONTENT_URI_JOIN_OPERATORS,
            new String[]{
                    LocationsColumns.LATITUDE,
                    LocationsColumns.LONGITUDE,
                    LocationsColumns.OPERATOR_NAME,
                    LocationsColumns.OPERATOR_LOGO_URL,
                    LocationsColumns.START_DATE,
                    OperatorsColumns.LOGO_BACKGROUND
            },
            "date(" + LocationsColumns.START_DATE + ") = ?",
            new String[] {
                    Utility.getDateNow()
            },
            null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    mMapFragment.getMapAsync(this);
    mCursor = data;
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {

  }
}
