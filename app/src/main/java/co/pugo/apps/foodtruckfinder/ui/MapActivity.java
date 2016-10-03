package co.pugo.apps.foodtruckfinder.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;

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

    ActionBar actionBar = getSupportActionBar();
    actionBar.setHomeButtonEnabled(true);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);
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
        Marker marker = googleMap.addMarker(
                new MarkerOptions()
                        .title(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_NAME)))
                        .position(new LatLng(
                                mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LATITUDE)),
                                mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LONGITUDE)))));
        Utility.loadMapMarkerIcon(this, marker, mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL)));
      }
    }
    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 12));
  }


  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(this,
            FoodtruckProvider.Locations.CONTENT_URI,
            new String[]{
                    LocationsColumns.LATITUDE,
                    LocationsColumns.LONGITUDE,
                    LocationsColumns.OPERATOR_NAME,
                    LocationsColumns.OPERATOR_LOGO_URL,
                    LocationsColumns.START_DATE
            },
            "date(" + LocationsColumns.START_DATE + ") = ?",
            new String[] {
                    "2016-10-04"
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
