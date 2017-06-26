package co.pugo.apps.foodtruckfinder.ui;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.MapInfoWindowAdapter;
import co.pugo.apps.foodtruckfinder.data.FoodtruckDatabase;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.model.MarkerItem;

/**
 * Created by tobias on 3.10.2016.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LoaderManager.LoaderCallbacks<Cursor>, ClusterManager.OnClusterClickListener<MarkerItem> {

  public static final String LONGITUDE_TAG = "longitude_tag";
  public static final String LATITUDE_TAG = "latitude_tag";
  public static final String LOGO_URL_EXTRA = "logo_tag";
  public static final String LOCATION_ID = "location_id";
  private double mLongitude;
  private double mLatitude;
  private SupportMapFragment mMapFragment;

  private ClusterManager<MarkerItem> mClusterManager;
  private Cursor mCursor;
  private GoogleMap mMap;
  private int mLocationId;
  private int markerCount;
  private ArrayList<MarkerItem> mMarkerItems = new ArrayList<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);

    getLoaderManager().initLoader(1, null, this);

    mLongitude = getIntent().getDoubleExtra(LONGITUDE_TAG, 0);
    mLatitude = getIntent().getDoubleExtra(LATITUDE_TAG, 0);
    mLocationId = getIntent().getIntExtra(LOCATION_ID, -1);

    mMapFragment =
            (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);

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
  public void onMapReady(final GoogleMap googleMap) {
    mMap = googleMap;

    googleMap.setInfoWindowAdapter(new MapInfoWindowAdapter(getLayoutInflater()));
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 12));

    mClusterManager = new ClusterManager<>(this, googleMap);
    mClusterManager.addItems(mMarkerItems);
    mClusterManager.setRenderer(new MarkerRenderer(googleMap));
    mClusterManager.setOnClusterClickListener(this);

    googleMap.setOnCameraIdleListener(mClusterManager);
    googleMap.setOnMarkerClickListener(mClusterManager);
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
                    FoodtruckDatabase.LOCATIONS + "." + LocationsColumns._ID,
                    LocationsColumns.LATITUDE,
                    LocationsColumns.LONGITUDE,
                    LocationsColumns.OPERATOR_NAME,
                    LocationsColumns.OPERATOR_LOGO_URL,
                    LocationsColumns.START_DATE,
                    LocationsColumns.END_DATE,
                    OperatorsColumns.LOGO_BACKGROUND
            },
            null,
            null,
            LocationsColumns.LATITUDE + "," + LocationsColumns.LONGITUDE + "," + LocationsColumns.START_DATE);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    mCursor = data;
    if (mCursor != null && mCursor.moveToFirst()) {
      double lat, lng;
      String logoUrl, title, snippet;
      int color;
      ArrayList<String> schedule = new ArrayList<>();
      ArrayList<Integer> ids = new ArrayList<>();
      String latitude, longitude, nextLatitude = "", nextLongitude = "";

      do {
        latitude = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.LATITUDE));
        longitude = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.LONGITUDE));
        if (!mCursor.isLast()) {
          mCursor.moveToNext();
          nextLatitude = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.LATITUDE));
          nextLongitude = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.LONGITUDE));
          mCursor.moveToPrevious();
        }

        String date = Utility.getFormattedDate(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.START_DATE)), this);
        String time = String.format(getString(R.string.schedule_time),
                Utility.getFormattedTime(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.START_DATE))),
                Utility.getFormattedTime(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.END_DATE))));

        schedule.add(date + ": " + time);
        ids.add(mCursor.getInt(mCursor.getColumnIndex(LocationsColumns._ID)));

        if (mCursor.isLast() || !nextLatitude.equals(latitude) || !nextLongitude.equals(longitude)) {

          lat = mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LATITUDE));
          lng = mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LONGITUDE));
          logoUrl = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL));
          title = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_NAME));
          snippet = TextUtils.join("\n", schedule);

          try {
            color = Color.parseColor(mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.LOGO_BACKGROUND)));
          } catch (Exception e) {
            color = Color.WHITE;
            e.printStackTrace();
          }

          boolean onTop = ids.contains(mLocationId);


          mMarkerItems.add(new MarkerItem(lat, lng, snippet, title, logoUrl, color, onTop));

          schedule = new ArrayList<>();
          ids = new ArrayList<>();
        }
      } while (mCursor.moveToNext());
    }
    mMapFragment.getMapAsync(this);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {

  }

  @Override
  public boolean onClusterClick(Cluster<MarkerItem> cluster) {
    // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
    // inside of bounds, then animate to center of the bounds.

    // Create the builder to collect all essential cluster items for the bounds.
    LatLngBounds.Builder builder = LatLngBounds.builder();
    for (ClusterItem item : cluster.getItems()) {
      builder.include(item.getPosition());
    }
    // Get the LatLngBounds
    final LatLngBounds bounds = builder.build();

    // Animate camera to the bounds
    try {
      mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return true;
  }


  private class MarkerRenderer extends DefaultClusterRenderer<MarkerItem> {
    private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
    private View mClusterView;
    private Bitmap mMarkerBG;
    private TextView mClusterTextView;

    MarkerRenderer(GoogleMap googleMap) {
      super(getApplicationContext(), googleMap, mClusterManager);
      mClusterView = getLayoutInflater().inflate(R.layout.marker_cluster, null);
      mMarkerBG = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_marker_bg_bubble);
      mClusterTextView = (TextView) mClusterView.findViewById(R.id.amu_text);
    }

    @Override
    protected void onBeforeClusterItemRendered(final MarkerItem item, MarkerOptions markerOptions) {
      mClusterTextView.setVisibility(View.GONE);
      Bitmap bg = Utility.getMarkerBitmap(item.logoUrl, getApplicationContext());
      if (bg != null) {
        mIconGenerator.setBackground(new BitmapDrawable(getResources(), bg));
      } else {
        mIconGenerator.setBackground(new BitmapDrawable(getResources(), mMarkerBG));
      }
      Bitmap icon = mIconGenerator.makeIcon();
      markerOptions.title(item.title);
      markerOptions.snippet(item.snippet);
      markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
      markerOptions.anchor(1, 1);
      if (item.onTop) {
        markerOptions.zIndex(99999);
      }
    }

    @Override
    protected void onClusterItemRendered(MarkerItem clusterItem, Marker marker) {
      if (clusterItem.onTop) {
        marker.showInfoWindow();
        clusterItem.onTop = false;
      }
      super.onClusterItemRendered(clusterItem, marker);
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<MarkerItem> cluster, MarkerOptions markerOptions) {
      mClusterTextView.setVisibility(View.VISIBLE);
      Bitmap bg = Utility.colorBitmap(mMarkerBG, getColor(cluster.getSize()));
      mIconGenerator.setBackground(new BitmapDrawable(getResources(), Utility.addDropShadow(bg, Color.GRAY, 10, 0, 2)));
      mIconGenerator.setTextAppearance(R.style.MarkerClusterText);
      mClusterView.setLayoutParams(new ViewGroup.LayoutParams(bg.getWidth(), bg.getHeight()));
      mIconGenerator.setContentView(mClusterView);
      Bitmap icon = mIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
      markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
      markerOptions.anchor(1, 1);
    }

    @Override
    protected int getColor(int clusterSize) {
      float maxV = 84;
      float percentage = (float) clusterSize / (float) mMarkerItems.size();

      // base color is HSV 51, 94, 84
      // scale brightness from 84 to 0
      return Color.HSVToColor(new float[]{
              51, 0.94f, (maxV - (maxV * percentage)) / 100
      });
    }
  }
}
