package co.pugo.apps.foodtruckfinder.ui;

import android.animation.Animator;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.MapInfoWindowAdapter;
import co.pugo.apps.foodtruckfinder.adapter.MapSearchSuggestionAdapter;
import co.pugo.apps.foodtruckfinder.data.FoodtruckDatabase;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.model.MapItem;
import co.pugo.apps.foodtruckfinder.model.MarkerItem;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;

/**
 * Created by tobias on 3.10.2016.
 */
public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback, LoaderManager.LoaderCallbacks<Cursor>,
        ClusterManager.OnClusterClickListener<MarkerItem>,
        ClusterManager.OnClusterItemInfoWindowClickListener<MarkerItem> {

  @BindView(R.id.toolbar) Toolbar mToolbar;
  @BindView(R.id.map_bottom_nav) BottomNavigationView mBottomNav;
  @BindView(R.id.fab) FloatingActionButton mFab;

  public static final String LONGITUDE_TAG = "longitude_tag";
  public static final String LATITUDE_TAG = "latitude_tag";
  public static final String LOCATION_ID = "location_id";
  public static final String DATE_RANGE = "date_range";

  public static final int DATE_RANGE_TODAY = 0;
  public static final int DATE_RANGE_TOMORROW = 1;
  public static final int DATE_RANGE_THIS_WEEK = 2;

  private final Integer BOTTOM_NAV_IDS[] = {
          R.id.action_map_today,
          R.id.action_map_tomorrow,
          R.id.action_map_this_week
  };

  private double mLongitude;
  private double mLatitude;
  private SupportMapFragment mMapFragment;

  private ClusterManager<MarkerItem> mClusterManager;
  private Cursor mCursor;
  private GoogleMap mMap;
  private int mLocationId;
  private int markerCount;
  private ArrayList<MarkerItem> mMarkerItems;
  private String[] COLUMNS = {
          FoodtruckDatabase.LOCATIONS + "." + LocationsColumns._ID,
          LocationsColumns.LATITUDE,
          LocationsColumns.LONGITUDE,
          LocationsColumns.OPERATOR_NAME,
          LocationsColumns.OPERATOR_ID,
          LocationsColumns.IMAGE_ID,
          LocationsColumns.OPERATOR_LOGO_URL,
          LocationsColumns.START_DATE,
          LocationsColumns.END_DATE,
          LocationsColumns.LOCATION_NAME,
          LocationsColumns.DISTANCE,
          OperatorsColumns.LOGO_BACKGROUND
  };

  private int mDateRange;
  private MenuItem mSearchItem;
  private SearchView mSearchView;
  private String mSelection;
  private MapSearchSuggestionAdapter mSuggestionAdapter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    ButterKnife.bind(this);

    mLongitude = getIntent().getDoubleExtra(LONGITUDE_TAG, 0);
    mLatitude = getIntent().getDoubleExtra(LATITUDE_TAG, 0);
    mLocationId = getIntent().getIntExtra(LOCATION_ID, -1);
    mDateRange = getIntent().getIntExtra(DATE_RANGE, 0);

    getLoaderManager().initLoader(1, null, this);

    mMapFragment =
            (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);

    mMapFragment.getMapAsync(this);

    setSupportActionBar(mToolbar);

    Menu menu = mBottomNav.getMenu();

    Calendar cal = Calendar.getInstance();
    int drawableIdToday = getResources().getIdentifier("ic_cal_" + cal.get(Calendar.DAY_OF_MONTH), "drawable", getPackageName());
    cal.add(Calendar.DAY_OF_YEAR, 1);
    int drawableIdTomorrow = getResources().getIdentifier("ic_cal_" + cal.get(Calendar.DAY_OF_MONTH), "drawable", getPackageName());
    menu.findItem(BOTTOM_NAV_IDS[DATE_RANGE_TODAY]).setIcon(drawableIdToday);
    menu.findItem(BOTTOM_NAV_IDS[DATE_RANGE_TOMORROW]).setIcon(drawableIdTomorrow);

    mBottomNav.setSelectedItemId(BOTTOM_NAV_IDS[mDateRange]);
    mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = Arrays.asList(BOTTOM_NAV_IDS).indexOf(item.getItemId());

        if (id != mDateRange) {
          mDateRange = id;
          restartLoader();
        }

        return true;
      }
    });

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      getWindow().getDecorView().setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

      CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mToolbar.getLayoutParams();
      int statusBarHeight = Utility.getStatusBarHeight(this);
      params.height += statusBarHeight;
      mToolbar.setLayoutParams(params);

      mToolbar.setPadding(0, statusBarHeight, 0, 0);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                                         View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
      }
    }


    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
  }

  private void restartLoader() {
    getLoaderManager().restartLoader(1, null, this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search_maps:
        break;
      default:
        onBackPressed();
    }
    return true;
  }

  @Override
  public void onMapReady(final GoogleMap googleMap) {

    mMap = googleMap;

    googleMap.setInfoWindowAdapter(new MapInfoWindowAdapter(getLayoutInflater()));
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 15));

    mClusterManager = new ClusterManager<>(this, googleMap);
    mClusterManager.setRenderer(new MarkerRenderer(googleMap));
    mClusterManager.setOnClusterClickListener(this);
    mClusterManager.setOnClusterItemInfoWindowClickListener(this);

    googleMap.setOnCameraIdleListener(mClusterManager);
    googleMap.setOnMarkerClickListener(mClusterManager);
    googleMap.setOnInfoWindowClickListener(mClusterManager);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_maps, menu);

    // setup searchbar
    mSearchItem = menu.findItem(R.id.menu_search_maps);
    MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
      // hide mFab on expanding searchbar
      @Override
      public boolean onMenuItemActionExpand(MenuItem menuItem) {
        mFab.animate().translationYBy(mFab.getHeight() * 2);
        mFab.setVisibility(View.GONE);
        mToolbar.setBackgroundColor(Color.WHITE);
        animateSearchBar(true);
        return true;
      }

      // show mFab when searchbar is collapsed
      @Override
      public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        mFab.animate().setDuration(500).translationYBy(-(mFab.getHeight() * 2));
        mFab.setVisibility(View.VISIBLE);
        mToolbar.setBackgroundColor(getResources().getColor(R.color.transparentToolbar));
        return true;
      }
    });

    mSearchItem.setEnabled(false);
    mSearchItem.setIcon(new ColorDrawable(Color.TRANSPARENT));

    // disable item and icon, because mFab is used for search
    mSearchView = (SearchView) mSearchItem.getActionView();
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    mSearchView.setOnQueryTextListener(new SearchViewListener(this));

    mSuggestionAdapter = new MapSearchSuggestionAdapter(this);
    mSearchView.setSuggestionsAdapter(mSuggestionAdapter);

    int searchEditTextId = R.id.search_src_text;
    final AutoCompleteTextView searchEditText = (AutoCompleteTextView) mSearchView.findViewById(searchEditTextId);
    //searchEditText.setDropDownBackgroundResource(R.drawable.blank_16dp);

    final View dropDownAnchor = mSearchView.findViewById(searchEditText.getDropDownAnchor());

    if (dropDownAnchor != null) {
      dropDownAnchor.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {

          // screen width
          int screenWidthPixel = getResources().getDisplayMetrics().widthPixels;
          searchEditText.setDropDownWidth(screenWidthPixel);
        }
      });
    }



    // close search when lost focus/keyboard hidden
    mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          clearSearch();
        }
      }
    });

    mFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mSearchItem.expandActionView();
        mSearchView.setFocusable(true);
        mSearchView.setIconified(false);
        mSearchView.requestFocusFromTouch();
      }
    });

    return true;
  }

  private void clearSearch() {
    mSuggestionAdapter.swapCursor(null);
    mSearchView.setQuery("", false);
    mSearchItem.collapseActionView();
  }

  private void animateSearchBar(boolean show) {
    if (show) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        int width = mToolbar.getWidth();
        Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(mToolbar, width, mToolbar.getHeight() / 2, 0.0f, (float) width);
        createCircularReveal.setDuration(250);
        createCircularReveal.start();
      } else {
        TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, 0.0f, (float) (-mToolbar.getHeight()), 0.0f);
        translateAnimation.setDuration(220);
        mToolbar.clearAnimation();
        mToolbar.startAnimation(translateAnimation);
      }
    } else {
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    mLongitude = intent.getDoubleExtra(LONGITUDE_TAG, 0);
    mLatitude = intent.getDoubleExtra(LATITUDE_TAG, 0);
    clearSearch();

    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 15));
  }

  private void updateCluster() {
    if (mClusterManager != null && mMarkerItems != null) {
      mClusterManager.clearItems();
      mClusterManager.addItems(mMarkerItems);
      mClusterManager.cluster();
    }
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

    String now = Utility.getTimeNow();
    String tomorrow = (mDateRange == DATE_RANGE_TODAY) ? Utility.getDateTomorrow() : Utility.getDateDayAfterTomorrow();
    mSelection = (mDateRange == DATE_RANGE_THIS_WEEK) ? null :
            "datetime(" + LocationsColumns.END_DATE + ") > datetime('" + now + "') AND " +
            "datetime(" + LocationsColumns.END_DATE + ") <= datetime('" + tomorrow + "')";

    return new CursorLoader(this,
            FoodtruckProvider.Locations.CONTENT_URI_JOIN_OPERATORS,
            COLUMNS,
            mSelection,
            null,
            LocationsColumns.LATITUDE + "," + LocationsColumns.LONGITUDE + "," + LocationsColumns.START_DATE);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    mCursor = data;
    if (mCursor != null && mCursor.moveToFirst()) {
      double lat, lng;
      String operatorId, imageId, title, snippet;
      int color;
      ArrayList<String> schedule = new ArrayList<>();
      ArrayList<Integer> ids = new ArrayList<>();
      mMarkerItems = new ArrayList<>();
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

        String startDate = Utility.getFormattedTime(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.START_DATE)));
        String ed = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.END_DATE));
        String endDate = Utility.getFormattedTime(ed);

        String date = Utility.getFormattedDate(mCursor.getString(mCursor.getColumnIndex(LocationsColumns.START_DATE)), this);
        String time = String.format(getString(R.string.schedule_time), startDate, endDate);

        schedule.add(date + ": " + time);
        ids.add(mCursor.getInt(mCursor.getColumnIndex(LocationsColumns._ID)));

        if (mCursor.isLast() || !nextLatitude.equals(latitude) || !nextLongitude.equals(longitude)) {

          lat = mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LATITUDE));
          lng = mCursor.getDouble(mCursor.getColumnIndex(LocationsColumns.LONGITUDE));
          operatorId = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_ID));
          imageId = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.IMAGE_ID));
          title = mCursor.getString(mCursor.getColumnIndex(LocationsColumns.OPERATOR_NAME));
          snippet = TextUtils.join("\n", schedule);

          try {
            color = Color.parseColor(mCursor.getString(mCursor.getColumnIndex(OperatorsColumns.LOGO_BACKGROUND)));
          } catch (Exception e) {
            color = Color.WHITE;
            e.printStackTrace();
          }

          boolean onTop = ids.contains(mLocationId);

          mMarkerItems.add(new MarkerItem(lat, lng, snippet, title, operatorId, imageId, color, onTop));

          schedule = new ArrayList<>();
          ids = new ArrayList<>();
        }
      } while (mCursor.moveToNext());
    }

    updateCluster();
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


  @Override
  public void onClusterItemInfoWindowClick(MarkerItem markerItem) {

    if (!(Utility.dataExists(this, FoodtruckProvider.OperatorDetails.withOperatorId(markerItem.operatorId)))) {
      Intent serviceIntent = new Intent(this, FoodtruckIntentService.class);
      serviceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_DETAILS);
      serviceIntent.putExtra(FoodtruckIntentService.OPERATORID_TAG, markerItem.operatorId);
      startService(serviceIntent);
    }

    Log.d("MapActivity", "OperatorId:" + markerItem.operatorId);
    Intent intent = new Intent(this, DetailActivity.class);
    intent.putExtra(FoodtruckIntentService.OPERATORID_TAG, markerItem.operatorId);
    startActivity(intent);
  }


  private class MarkerRenderer extends DefaultClusterRenderer<MarkerItem> {
    private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
    private View mClusterView;
    private Bitmap mMarkerBG;
    private TextView mClusterTextView;

    MarkerRenderer(GoogleMap googleMap) {
      super(getApplicationContext(), googleMap, mClusterManager);
      mClusterView = getLayoutInflater().inflate(R.layout.marker_cluster, null);
      mMarkerBG = Utility.scaleMarkerToDPI(getApplicationContext(),
              BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_marker_bg_bubble));
      mClusterTextView = (TextView) mClusterView.findViewById(R.id.amu_text);
    }

    @Override
    protected void onBeforeClusterItemRendered(final MarkerItem item, MarkerOptions markerOptions) {
      mClusterTextView.setVisibility(View.GONE);
      Bitmap bg = Utility.getMarkerBitmap(getApplicationContext(), item.operatorId, item.imageId, false);
      BitmapDrawable bmd = new BitmapDrawable(getResources(), (bg != null) ? bg : mMarkerBG);
      mIconGenerator.setBackground(bmd);

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

  private class SearchViewListener implements SearchView.OnQueryTextListener {
    private Context mContext;

    public SearchViewListener(Context context) {
      mContext = context;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
      return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
      if (newText.length() > 0) {
        String searchSelection = FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.NAME + " LIKE ? OR " + OperatorsColumns.OFFER + " LIKE ?";
        String selection = mSelection != null ? mSelection + " AND (" + searchSelection + ")" : searchSelection;
        Cursor cursor = mContext.getContentResolver().query(
                FoodtruckProvider.Locations.CONTENT_URI_JOIN_OPERATORS,
                COLUMNS,
                selection,
                new String[]{"%" + newText + "%", "%" + newText + "%"},
                LocationsColumns.DISTANCE + " ASC");

        if (cursor != null && cursor.getCount() > 0)
          mSuggestionAdapter.swapCursor(cursor);
        else
          mSuggestionAdapter.swapCursor(null);
      }
      return false;
    }
  }
}
