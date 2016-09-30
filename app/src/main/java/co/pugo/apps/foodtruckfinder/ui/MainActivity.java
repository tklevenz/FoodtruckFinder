package co.pugo.apps.foodtruckfinder.ui;

import android.Manifest;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.location.LocationServices;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.AnalyticsApplication;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.FoodtruckAdapter;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;


public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

  private static final String LOG_TAG = MainActivity.class.getSimpleName();
  private static final int LOCATION_PERMISSION_REQUEST = 0;
  private static final int FOODTRUCK_LOADER_ID = 0;
  public static final String LOCATIONS_PERIODIC_TASK = "periodic_task";

  private String[] LOCATION_COLUMNS = {
          LocationsColumns.OPERATOR_ID,
          LocationsColumns.OPERATOR_NAME,
          LocationsColumns.OPERATOR_OFFER,
          LocationsColumns.OPERATOR_LOGO_URL,
          LocationsColumns.LATITUDE,
          LocationsColumns.LONGITUDE,
          LocationsColumns.DISTANCE,
          LocationsColumns.NAME
  };


  @BindView(R.id.recyclerview_locations) RecyclerView mRecyclerView;
  @BindView(R.id.fab) FloatingActionButton fab;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.empty_view) TextView emptyView;

  private GoogleApiClient mGoogleApiClient;
  private FoodtruckAdapter mFoodtruckAdapter;
  private RecyclerView.LayoutManager mLayoutManager;
  private Intent mServiceIntent;
  public static Tracker mTracker;

  private boolean isLocationGranted;

  public static Typeface mRobotoSlab;
  private boolean fabHidden;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    setSupportActionBar(toolbar);

    AnalyticsApplication application = (AnalyticsApplication) getApplication();
    mTracker = application.getDefaultTracker();

    mRobotoSlab = Typeface.createFromAsset(this.getAssets(), "RobotoSlab-Regular.ttf");
    Utility.setToolbarTitleFont(toolbar);

    mRecyclerView.setHasFixedSize(true);
    mLayoutManager = new LinearLayoutManager(this);
    mRecyclerView.setLayoutManager(mLayoutManager);
    mFoodtruckAdapter = new FoodtruckAdapter(this);
    mRecyclerView.setAdapter(mFoodtruckAdapter);

    getLoaderManager().initLoader(FOODTRUCK_LOADER_ID, null, this);


    mGoogleApiClient = new GoogleApiClient
            .Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();

    if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
              LOCATION_PERMISSION_REQUEST);

    } else {
      mGoogleApiClient.connect();
      isLocationGranted = true;
      updateEmptyView();
      runServiceIntentFetchOperators();
    }

    // update foodtruck location data every 24 hours
    PeriodicTask periodicTask = new PeriodicTask.Builder()
            .setService(FoodtruckTaskService.class)
            .setPeriod(86400L)
            .setTag(LOCATIONS_PERIODIC_TASK)
            .setPersisted(true)
            .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
            .setRequiresCharging(false)
            .build();

    GcmNetworkManager.getInstance(this).schedule(periodicTask);
  }




  @Override
  protected void onStop() {
    super.onStop();
    if (mGoogleApiClient.isConnected())
      mGoogleApiClient.disconnect();
  }


  private void runServiceIntentFetchOperators() {
    if (Utility.isOutOfDate(this)) {
      mServiceIntent = new Intent(this, FoodtruckIntentService.class);
      mServiceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_OPERATORS);
      startService(mServiceIntent);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
          throws SecurityException {
    switch (requestCode) {
      case LOCATION_PERMISSION_REQUEST: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mGoogleApiClient.connect();
          isLocationGranted = true;
          updateEmptyView();
          runServiceIntentFetchOperators();
        } else {
          updateEmptyView();
        }
      }
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);

    final MenuItem searchItem = menu.findItem(R.id.menu_search);
    MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(MenuItem menuItem) {
        fab.animate().translationYBy(fab.getHeight() * 2);
        fab.setVisibility(View.GONE);
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        fab.animate().setDuration(500).translationYBy(-(fab.getHeight() * 2));
        fab.setVisibility(View.VISIBLE);
        return true;
      }
    });

    // disable item and icon, because fab is used for search
    searchItem.setEnabled(false);
    searchItem.setIcon(new ColorDrawable(Color.TRANSPARENT));
    final SearchView searchView = (SearchView) searchItem.getActionView();
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    searchView.setOnQueryTextListener(new SearchViewListener(this));

    fab.setOnClickListener(new View.OnClickListener() {
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

  private void animateFab(boolean slideOut) {
    if (fabHidden) {
      float y = slideOut ? fab.getHeight() * 2 : -(fab.getHeight() * 2);
      fab.animate().setDuration(500).translationYBy(y);
      fab.setVisibility(slideOut ? View.INVISIBLE : View.VISIBLE);
      fabHidden = slideOut;
    }
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      startActivity(new Intent(this, SettingsActivity.class));
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    Log.d(LOG_TAG, "onCreateLoader");
    return new CursorLoader(this,
            FoodtruckProvider.Locations.CONTENT_URI,
            LOCATION_COLUMNS,
            LocationsColumns.OPERATOR_ID + " IS NOT NULL) GROUP BY (" + LocationsColumns.OPERATOR_ID,
            null,
            LocationsColumns.DISTANCE + " ASC");
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    Log.d(LOG_TAG, "onLoadFinished");
    mFoodtruckAdapter.swapCursor(data);
    updateEmptyView();

    // update widget
    int widgetIds[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), FoodtruckWidget.class));
    AppWidgetManager.getInstance(getApplication()).notifyAppWidgetViewDataChanged(widgetIds, R.layout.foodtruck_widget);
  }

  private void updateEmptyView() {
    if (mFoodtruckAdapter.getItemCount() == 0) {
      emptyView.setVisibility(View.VISIBLE);
      if (Utility.isNetworkAvailable(this)) {
        if (isLocationGranted) {
          emptyView.setText(getString(R.string.getting_foodtuck_data));
        } else {
          emptyView.setText(getString(R.string.no_location_available));
        }
      } else {
        emptyView.setText(getString(R.string.no_network_available));
      }
    } else {
      emptyView.setVisibility(View.INVISIBLE);
    }
  }


  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mFoodtruckAdapter.swapCursor(null);
  }

  @Override
  protected void onResume() {
    super.onResume();
    animateFab(false);
    getLoaderManager().restartLoader(FOODTRUCK_LOADER_ID, null, this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    animateFab(true);
  }


  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(LOG_TAG, "connected to google api client");
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
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

  private class SearchViewListener implements SearchView.OnQueryTextListener {
    private Context mContext;

    public SearchViewListener(Context context) {
      mContext = context;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
      Cursor cursor = mContext.getContentResolver().query(
              FoodtruckProvider.Locations.CONTENT_URI,
              LOCATION_COLUMNS,
              LocationsColumns.OPERATOR_NAME + " LIKE ? OR " + LocationsColumns.OPERATOR_OFFER +
                      " LIKE ?) GROUP BY (" + LocationsColumns.OPERATOR_ID,
              new String[]{"%" + query + "%", "%" + query + "%"},
              LocationsColumns.DISTANCE + " ASC");

      // send search query to analytcis
      mTracker.send(new HitBuilders.EventBuilder()
              .setCategory("MainActivity")
              .setAction("Search")
              .setLabel(query)
              .setValue(1)
              .build());

      mFoodtruckAdapter.swapCursor(cursor);
      return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
      Cursor cursor = mContext.getContentResolver().query(
              FoodtruckProvider.Locations.CONTENT_URI,
              LOCATION_COLUMNS,
              LocationsColumns.OPERATOR_NAME + " LIKE ? OR " + LocationsColumns.OPERATOR_OFFER +
                      " LIKE ?) GROUP BY (" + LocationsColumns.OPERATOR_ID,
              new String[]{"%" + newText + "%", "%" + newText + "%"},
              LocationsColumns.DISTANCE + " ASC");
      mFoodtruckAdapter.swapCursor(cursor);
      return true;
    }
  }

}
