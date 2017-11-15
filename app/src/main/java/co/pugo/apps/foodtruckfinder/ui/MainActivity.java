package co.pugo.apps.foodtruckfinder.ui;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.FoodtruckApplication;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.FoodtruckAdapter;
import co.pugo.apps.foodtruckfinder.adapter.TagsAdapter;
import co.pugo.apps.foodtruckfinder.billing.BillingManager;
import co.pugo.apps.foodtruckfinder.data.FoodtruckDatabase;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.data.RegionsColumns;
import co.pugo.apps.foodtruckfinder.data.TagsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;
import co.pugo.apps.foodtruckfinder.service.GeofenceTransitionsIntentService;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, OnCompleteListener<Void> {


  private static final String PREFS_FILE_NAME = "shared_prefs";
  @BindView(R.id.recyclerview_locations) RecyclerView mRecyclerView;
  @BindView(R.id.fab) FloatingActionButton mFab;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.empty_view) TextView emptyView;
  @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
  @BindView(R.id.recyclerview_tags) RecyclerView recyclerViewTags;
  @BindView(R.id.filter_favourite) ImageView imageViewFavourites;
  @BindView(R.id.tags_title) TextView tagsTitle;
  @BindView(R.id.btn_location_access) Button btnLocationAccess;

  private static final String LOG_TAG = MainActivity.class.getSimpleName();

  private static final int LOCATION_PERMISSION_REQUEST = 0;

  protected static final int REQUEST_CHECK_SETTINGS = 0x1;

  private static final int FOODTRUCK_LOADER_ID = 0;
  private static final int TAGS_LOADER_ID = 1;

  private static final String LOCATIONS_PERIODIC_TASK = "periodic_task_locations";
  private static final String OPERATORS_PERIODIC_TASK = "periodic_task_operators";

  private Uri mContentUri = FoodtruckProvider.Operators.CONTENT_URI_JOINED;

  private String[] LOCATION_COLUMNS = {
          FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID,
          FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.NAME,
          FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.OFFER,
          FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.LOGO_URL,
          FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.REGION,
          FoodtruckDatabase.LOCATIONS + "." + LocationsColumns.DISTANCE,
          FoodtruckDatabase.LOCATIONS + "." + LocationsColumns.LOCATION_NAME,
          FoodtruckDatabase.LOCATIONS + "." + LocationsColumns.START_DATE,
          FoodtruckDatabase.LOCATIONS + "." + LocationsColumns.END_DATE,
          FoodtruckDatabase.REGIONS + "." + RegionsColumns.DISTANCE_APROX
  };

  private FoodtruckAdapter mFoodtruckAdapter;
  private TagsAdapter mTagsAdapter;

  public Tracker mTracker;

  public static Typeface mRobotoSlab;

  private ArrayList<String> mSelectedTags;
  private int mRadius;

  private boolean mIsLoadFinished;
  private boolean mFavouritesSelected;
  private boolean mIsLocationGranted;

  private ArrayList<Geofence> mGeofenceList;
  private PendingIntent mGeofencePendingIntent;
  private GeofencingClient mGeofencingClient;

  private FirebaseAnalytics mFirebaseAnalytics;

  private static final float GEOFENCE_RADIUS = 2000;
  private static final long GEOFENCE_EXPIRATION_DURATION = 24 * 60 * 60 * 1000;
  private static final String GEOFENCE_SHARED_PREFERENCE_KEY = "geo_pref_key";
  private LinearLayoutManager mLayoutManager;
  private static int mScrollPosition = -1;
  private static int mScrollTop = -1;
  private boolean mLocationDenied;
  private LocationRequest mLocationRequest;
  private boolean mLocationDisabled;
  private FusedLocationProviderClient mFusedLocationClient;
  private LocationCallback mLocationCallback;
  private BillingClient mBillingClient;

  private final String[] skus = { "pro_1", "pro_2", "pro_5" };
  private boolean mIsPaid;
  private BillingManager mBillingManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    if (Utility.isFirstLaunch(this)) {
      Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
      startActivity(welcomeIntent);

      // init db tables
      if (!Utility.dataExists(this, FoodtruckProvider.Regions.CONTENT_URI))
        Utility.initRegionsTable(this);


      if (!Utility.dataExists(this, FoodtruckProvider.Operators.CONTENT_URI))
        Utility.initOperatorsTable(this);

    }

    // check location settings are available
    mLocationRequest = new LocationRequest();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    mLocationRequest.setInterval(10000);
    mLocationRequest.setFastestInterval(5000);
    mLocationRequest.setNumUpdates(1);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        for (Location location : locationResult.getLocations()) {
          Log.d(LOG_TAG, location.toString());
          Utility.updateLocationSharedPref(MainActivity.this, location);
        }
      }
    };

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest);

    com.google.android.gms.tasks.Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

    result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
      @Override
      public void onComplete(@NonNull com.google.android.gms.tasks.Task<LocationSettingsResponse> task) {
        try {
          LocationSettingsResponse response = task.getResult(ApiException.class);
          // settings available

          Log.d(LOG_TAG, "SETTINGS AVAILABLE: " + response.toString());

        } catch (ApiException e) {
          switch (e.getStatusCode()) {
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
              try {
                ResolvableApiException resolvable = (ResolvableApiException) e;
                resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
              } catch (IntentSender.SendIntentException e1) {
                e1.printStackTrace();
              }
              break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

              break;
          }
        }
      }
    });

    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    // setup receiver
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Utility.FOODTRUCK_SERVICE_RESPONSE));



    setSupportActionBar(toolbar);


    if (Utility.isNetworkAvailable(this)) {
      runTask(FoodtruckTaskService.TASK_FETCH_LOCATIONS);
      runTask(FoodtruckTaskService.TASK_FETCH_OPERATORS);
      runTask(FoodtruckTaskService.TASK_FETCH_REGIONS);
    }

    Utility.cacheLogos(this);

    // set up drawer right
    recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));
    mTagsAdapter = new TagsAdapter(this);
    recyclerViewTags.setAdapter(mTagsAdapter);


    // set Google Analytics tracker
    FoodtruckApplication application = (FoodtruckApplication) getApplication();
    mTracker = application.getDefaultTracker();


    // set typeface for toolbar
    mRobotoSlab = Typeface.createFromAsset(this.getAssets(), "RobotoSlab-Bold.ttf");
    Utility.setToolbarTitleFont(toolbar);
    tagsTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), "RobotoSlab-Regular.ttf"));

    // set up recycler view
    mRecyclerView.setHasFixedSize(true);
    mLayoutManager = new LinearLayoutManager(this);
    mRecyclerView.setLayoutManager(mLayoutManager);
    mFoodtruckAdapter = new FoodtruckAdapter(this);
    mRecyclerView.setAdapter(mFoodtruckAdapter);


    mBillingManager = new BillingManager(this, new BillingManager.BillingUpdatesListener() {
      @Override
      public void onBillingClientSetupFinished() {

      }

      @Override
      public void onConsumeFinished(String token, @BillingClient.BillingResponse int result) {

      }

      @Override
      public void onPurchasesUpdated(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
          switch (purchase.getSku()) {
            case "pro_1":
            case "pro_2":
            case "pro_5":
              Log.d(LOG_TAG, "You are Premium! Congratulations!!!");
              mIsPaid = true;
              mFoodtruckAdapter.setPremium(true);
              break;
          }
        }
      }
    });

    if (!mIsPaid)
      MobileAds.initialize(this, "ca-app-pub-2185917688565953~6784346227");


    // init loaders
    getLoaderManager().initLoader(FOODTRUCK_LOADER_ID, null, this);
    getLoaderManager().initLoader(TAGS_LOADER_ID, null, this);

    // setup geofences
    mGeofenceList = new ArrayList<>();
    mGeofencePendingIntent = null;
    mGeofencingClient = LocationServices.getGeofencingClient(this);

    // check for location permission
    if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            !Utility.isFirstLaunch(this)) {

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

          Toast.makeText(this, R.string.no_location_available, Toast.LENGTH_SHORT).show();

          ActivityCompat.requestPermissions(this,
                  new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                  LOCATION_PERMISSION_REQUEST);

        } else {

          if (isFirstTimeAskingPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            firstTimeAskingPermission(Manifest.permission.ACCESS_FINE_LOCATION, false);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
          }

          else {
            mLocationDenied = true;
            updateEmptyView();
          }
        }
      }
    }

    // runTask(FoodtruckTaskService.TASK_FETCH_OPERATORS);

    // update foodtruck location data every 24 hours
    schedulePeriodicTask(FoodtruckTaskService.TASK_FETCH_LOCATIONS, 86400L, LOCATIONS_PERIODIC_TASK);

    // update foodtruck operator data every 7 days
    schedulePeriodicTask(FoodtruckTaskService.TASK_FETCH_OPERATORS, 604800L, OPERATORS_PERIODIC_TASK);


  }

  @Override
  protected void onStart() {
    super.onStart();

    if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

      getLocation();

      mIsLocationGranted = true;
      updateEmptyView();

    }
    // get location radius
    mRadius = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.pref_location_radius_key), getString(R.string.default_radius))) * 1000;
    if (PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_distance_unit_key), "").equals(getString(R.string.pref_unit_miles)))
      mRadius = (int) Math.round(mRadius * 1.60924);
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (mBillingManager != null
        && mBillingManager.getBillingClientResponseCode() == BillingClient.BillingResponse.OK) {
      mBillingManager.queryPurchases();
    }

    if (mIsLocationGranted) {
      // restart loaders
      getLoaderManager().restartLoader(FOODTRUCK_LOADER_ID, null, this);
      getLoaderManager().restartLoader(TAGS_LOADER_ID, null, this);
    }
  }



  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    // unregister message receiver
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);

    // setup searchbar
    final MenuItem searchItem = menu.findItem(R.id.menu_search);
    MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
      // hide mFab on expanding searchbar
      @Override
      public boolean onMenuItemActionExpand(MenuItem menuItem) {
        mFab.animate().translationYBy(mFab.getHeight() * 2);
        mFab.setVisibility(View.GONE);
        return true;
      }

      // show mFab when searchbar is collapsed
      @Override
      public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        mFab.animate().setDuration(500).translationYBy(-(mFab.getHeight() * 2));
        mFab.setVisibility(View.VISIBLE);
        onResume();
        return true;
      }
    });

    // disable item and icon, because mFab is used for search
    searchItem.setEnabled(false);
    searchItem.setIcon(new ColorDrawable(Color.TRANSPARENT));
    final SearchView searchView = (SearchView) searchItem.getActionView();
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    searchView.setOnQueryTextListener(new SearchViewListener(this));

    // close search when lost focus/keyboard hidden
    searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus){
          searchItem.collapseActionView();
          searchView.setQuery("", false);
        }
      }
    });

    mFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        searchItem.expandActionView();
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
      }
    });
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();


    if (id == R.id.action_settings) {
      startActivity(new Intent(this, SettingsActivity.class));
      return true;
    }

    if (id == R.id.action_map) {
      Float latitude = PreferenceManager.getDefaultSharedPreferences(this).getFloat(Utility.KEY_PREF_LATITUDE, 0f);
      Float longitude = PreferenceManager.getDefaultSharedPreferences(this).getFloat(Utility.KEY_PREF_LONGITUDE, 0f);
      Intent intent = new Intent(this, MapActivity.class);
      intent.putExtra(MapActivity.LATITUDE_TAG, Double.parseDouble(latitude.toString()));
      intent.putExtra(MapActivity.LONGITUDE_TAG, Double.parseDouble(longitude.toString()));
      startActivity(intent);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }


  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
          throws SecurityException {
    Log.d(LOG_TAG, "PERMISSION RESULTS:" + grantResults[0] );
    switch (requestCode) {
      case LOCATION_PERMISSION_REQUEST: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mIsLocationGranted = true;
          btnLocationAccess.setVisibility(View.GONE);
          updateEmptyView();
          addGeofencesToClient();
          runTask(FoodtruckTaskService.TASK_FETCH_LOCATIONS);
        } else {
          mFoodtruckAdapter.swapCursor(null);
          updateEmptyView();
        }
      }
    }
  }

  private void firstTimeAskingPermission(String permission, boolean isFirstTime){
    SharedPreferences sharedPreference = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
    sharedPreference.edit().putBoolean(permission, isFirstTime).apply();
  }

  private boolean isFirstTimeAskingPermission(String permission){
    return getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(permission, true);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String selection = LocationsColumns.DISTANCE + " < " + mRadius + " or (" +
                       LocationsColumns.DISTANCE + " IS NULL AND " + RegionsColumns.DISTANCE_APROX + " < " + mRadius + ")";
    switch (id) {
      case FOODTRUCK_LOADER_ID:
        return new CursorLoader(this,
                mContentUri,
                LOCATION_COLUMNS,
                selection,
                null,
                LocationsColumns.DISTANCE + ", " + LocationsColumns.DISTANCE + " is null, " + RegionsColumns.DISTANCE_APROX + " ASC");
      case TAGS_LOADER_ID:
        return new CursorLoader(this,
                FoodtruckProvider.Operators.CONTENT_URI_JOINED_TAGS,
                new String[]{
                        TagsColumns.TAG
                },
                selection,
                null,
                "count(*) DESC LIMIT 20");
      default:
        return null;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    switch (loader.getId()) {
      case FOODTRUCK_LOADER_ID:
        mFoodtruckAdapter.swapCursor(data);
        mIsLoadFinished = true;
        updateEmptyView();
        populateGeofenceList();

        if (mScrollPosition != -1) {
          mLayoutManager.scrollToPositionWithOffset(mScrollPosition, mScrollTop);
        }



        // update widget
        // TODO: fix widget (low)
        int widgetIds[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), FoodtruckWidget.class));
        AppWidgetManager.getInstance(getApplication()).notifyAppWidgetViewDataChanged(widgetIds, R.layout.foodtruck_widget);
        break;
      case TAGS_LOADER_ID:
        mTagsAdapter.swapCursor(data);

        break;
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mFoodtruckAdapter.swapCursor(null);
  }


  private void getLocation() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_use_location_key), true)) {

      mFusedLocationClient.getLastLocation()
              .addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                  if (location == null) {
                    Log.d(LOG_TAG, "Failed to get location...");
                    startLocationUpdates();
                  } else {
                    Log.d(LOG_TAG, location.toString());
                    Utility.updateLocationSharedPref(MainActivity.this, location);
                  }
                }
              })
              .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                  Log.d(LOG_TAG, "Failed to get location!!!" + e);
                  startLocationUpdates();
                }
              });
    }
  }

  private void startLocationUpdates() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_use_location_key), true)) {
      Log.d(LOG_TAG, "starting location updates...");
      mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }
  }

  // update text displayed in empty view shown when no foodtrucks are displayed
  private void updateEmptyView() {
    if (mFoodtruckAdapter.getItemCount() == 0) {
      emptyView.setVisibility(View.VISIBLE);
      if (Utility.isNetworkAvailable(this)) {
        if (mFavouritesSelected) {
          emptyView.setText(getString(R.string.no_favourites));
          btnLocationAccess.setVisibility(View.GONE);
        } else if (mIsLocationGranted && !mLocationDisabled) {
          if (Utility.dataExists(this, FoodtruckProvider.Operators.CONTENT_URI) && mIsLoadFinished) {
            emptyView.setText(R.string.no_foodtrucks_found_for_radius);
            btnLocationAccess.setVisibility(View.GONE);
          } else {
            emptyView.setText(getString(R.string.getting_data));
            btnLocationAccess.setVisibility(View.GONE);
          }
        } else if (mLocationDisabled) {
          emptyView.setText(getString(R.string.location_disabled));
          btnLocationAccess.setVisibility(View.GONE);
        } else if (mLocationDenied) {
          emptyView.setText(getString(R.string.location_denied));
          btnLocationAccess.setVisibility(View.GONE);
        } else {
          emptyView.setText(getString(R.string.no_location_available));
          btnLocationAccess.setVisibility(View.VISIBLE);
        }
      } else {
        emptyView.setText(getString(R.string.no_network_available));
        btnLocationAccess.setVisibility(View.GONE);
      }
    } else {
      emptyView.setVisibility(View.GONE);
      btnLocationAccess.setVisibility(View.GONE);
    }
  }

  public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent != null && intent.getBooleanExtra(Utility.MESSAGE_UPDATE_DISTANCE_TASK, false)) {
        Log.d(LOG_TAG, "received result success message...");
        onResume();
      }
    }
  };


  // start service that fetches operator/location data
  private void runTask(int task) {
    if (Utility.isOutOfDate(this, task)) {
      Intent serviceIntent = new Intent(this, FoodtruckIntentService.class);
      serviceIntent.putExtra(FoodtruckIntentService.TASK_TAG, task);
      startService(serviceIntent);
    }
  }

  // schedule periodic tasks that fetch data
  private void schedulePeriodicTask(int task_tag, long period, String tag) {
    Bundle args = new Bundle();
    args.putInt(FoodtruckIntentService.TASK_TAG, task_tag);

    PeriodicTask periodicTask = new PeriodicTask.Builder()
            .setService(FoodtruckTaskService.class)
            .setPeriod(period)
            .setTag(tag)
            .setExtras(args)
            .setPersisted(true)
            .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
            .setRequiresCharging(false)
            .build();

    GcmNetworkManager.getInstance(this).schedule(periodicTask);
  }

  // onClick method for filter in filterbar
  public void showRightDrawer(View view) {
    if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
      drawerLayout.openDrawer(GravityCompat.END);
    }
  }

  // onClick method for tags in drawer
  public void filterTags(View view) {
    TextView textViewTag = (TextView) view.findViewById(R.id.tag);
    TextView textViewDbTag = (TextView) view.findViewById(R.id.dbTag);
    ImageView imageView = (ImageView) view.findViewById(R.id.tag_image);
    if (mSelectedTags == null)
      mSelectedTags = new ArrayList<>();
    String tag = textViewDbTag.getText().toString();
    if (mSelectedTags.contains(tag)) {
      mSelectedTags.remove(tag);
      imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_lightgray_12dp));
      view.setBackgroundColor(Color.WHITE);
      textViewTag.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
    } else {
      mSelectedTags.add(tag);
      imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check_white_12dp));
      view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
      textViewTag.setTextColor(Color.WHITE);
    }


    if (mSelectedTags.size() > 0) {
      String queryString = "('" + TextUtils.join("','", mSelectedTags) + "')";

      // TODO: Distance global defined (low)
      mFoodtruckAdapter.swapCursor(getContentResolver().query(
              mContentUri,
              LOCATION_COLUMNS,
              "(" + LocationsColumns.DISTANCE + " < " + mRadius + " OR " +
              RegionsColumns.DISTANCE_APROX + " < " + mRadius + ") AND " +
              TagsColumns.TAG + " IN " + queryString,
              null,
              LocationsColumns.DISTANCE + " ASC")
      );
    } else {
      onResume();
    }
  }

  // onClick method for favourites icon in filterbar
  public void filterFavourites(View view) {
    if (mFavouritesSelected) {
      imageViewFavourites.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_favorite_border_white_24dp));
      setFavouritesContentUri(false);
    } else {
      imageViewFavourites.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_favorite_white_24dp));
      setFavouritesContentUri(true);
    }
    mFavouritesSelected = !mFavouritesSelected;
  }

  // adds or removes _fav to the content uri to show or hide saved favourites
  private void setFavouritesContentUri(boolean b) {
    String uri = mContentUri.toString();
    boolean isFavUri = uri.lastIndexOf("_fav") == uri.length() - 4;
    if (!b && isFavUri) {
      mContentUri = Uri.parse(uri.substring(0, uri.length() - 4));
      onResume();
    } else {
      if (!isFavUri)
        mContentUri = Uri.parse(mContentUri + "_fav");
      onResume();
    }
  }


  /**
   * populate geofence list with todays fooodtruck locations
   */
  private void populateGeofenceList() {
    if (PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(getString(R.string.pref_nearby_key), true)) {
      Cursor cursor = getContentResolver().query(
              FoodtruckProvider.Locations.CONTENT_URI,
              new String[]{
                      LocationsColumns.LOCATION_ID,
                      LocationsColumns.LATITUDE,
                      LocationsColumns.LONGITUDE
              },
              "date(" + LocationsColumns.START_DATE + ") = date('now')",
              null,
              LocationsColumns.DISTANCE + " ASC LIMIT 100");

      if (cursor != null && cursor.moveToFirst()) {
        do {
          mGeofenceList.add(new Geofence.Builder()
                  .setRequestId(cursor.getString(cursor.getColumnIndex(LocationsColumns.LOCATION_ID)))
                  .setCircularRegion(
                          cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LATITUDE)),
                          cursor.getDouble(cursor.getColumnIndex(LocationsColumns.LONGITUDE)),
                          GEOFENCE_RADIUS
                  )
                  .setExpirationDuration(GEOFENCE_EXPIRATION_DURATION)
                  // TODO: consider GEOFENCE_TRANSITION_DWELL for release (medium)
                  .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                  .build()

          );
        } while (cursor.moveToNext());

        cursor.close();
      }

      addGeofencesToClient();
    } else {
      mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
    }
  }

  @SuppressWarnings("MissingPermission")
  private void addGeofencesToClient() {
      mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);

      if (mGeofenceList.size() > 0)
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
  }

  /**
   * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
   * Also specifies how the geofence notifications are initially triggered.
   */
  private GeofencingRequest getGeofencingRequest() {
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

    // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
    // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
    // is already inside that geofence.
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

    // Add the geofences to be monitored by geofencing service.
    builder.addGeofences(mGeofenceList);

    // Return a GeofencingRequest.
    return builder.build();
  }

  /**
   * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
   * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
   * current list of geofences.
   *
   * @return A PendingIntent for the IntentService that handles geofence transitions.
   */
  private PendingIntent getGeofencePendingIntent() {
    // Reuse the PendingIntent if we already have it.
    if (mGeofencePendingIntent != null) {
      return mGeofencePendingIntent;
    }
    Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
    // addGeofences() and removeGeofences().
    return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  @Override
  public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
    if (task.isSuccessful()) {
      Log.d(LOG_TAG, "geofences added...");
    } else {
      Exception e = task.getException();
      if (e instanceof ApiException) {
        Log.d(LOG_TAG, "ApiException: " + ((ApiException) e).getStatusCode());
      } else {
        Log.d(LOG_TAG, getResources().getString(R.string.unknown_geofence_error));
      }
    }
  }

  public void requestLocationAccess(View view) {
    if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              LOCATION_PERMISSION_REQUEST);

    }
  }


  // search listener
  private class SearchViewListener implements SearchView.OnQueryTextListener {
    private Context mContext;

    SearchViewListener(Context context) {
      mContext = context;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
      Cursor cursor = mContext.getContentResolver().query(
              mContentUri,
              LOCATION_COLUMNS,
              FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.NAME + " LIKE ? OR " + OperatorsColumns.OFFER + " LIKE ?",
              new String[]{"%" + query + "%", "%" + query + "%"},
              LocationsColumns.DISTANCE + " ASC");

      // TODO: send search query to analytics (low)
      mTracker.send(new HitBuilders.EventBuilder()
              .setCategory("MainActivity")
              .setAction("Search")
              .setLabel(query)
              .setValue(1)
              .build());

      mFoodtruckAdapter.swapCursor(cursor);

      Utility.hideSoftKeyboard((Activity) mContext);

      return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
      Cursor cursor = mContext.getContentResolver().query(
              mContentUri,
              LOCATION_COLUMNS,
              FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.NAME + " LIKE ? OR " + OperatorsColumns.OFFER + " LIKE ?",
              new String[]{"%" + newText + "%", "%" + newText + "%"},
              LocationsColumns.DISTANCE + " ASC");
      mFoodtruckAdapter.swapCursor(cursor);
      return true;
    }
  }
}