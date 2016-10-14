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
import android.content.SharedPreferences;
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
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.FoodtruckApplication;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.FoodtruckAdapter;
import co.pugo.apps.foodtruckfinder.adapter.TagsAdapter;
import co.pugo.apps.foodtruckfinder.data.FoodtruckDatabase;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorsColumns;
import co.pugo.apps.foodtruckfinder.data.TagsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;
import co.pugo.apps.foodtruckfinder.service.FoodtruckTaskService;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

  private static final String LOG_TAG = MainActivity.class.getSimpleName();
  private static final int LOCATION_PERMISSION_REQUEST = 0;
  private static final int FOODTRUCK_LOADER_ID = 0;
  private static final int TAGS_LOADER_ID = 1;
  public static final String LOCATIONS_PERIODIC_TASK = "periodic_task";
  public static final String PREF_FILTER_AVAILABILITY = "filter_availability";

  private Uri mContentUri;
  private String[] LOCATION_COLUMNS = {
          FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID,
          OperatorsColumns.NAME,
          OperatorsColumns.OFFER,
          OperatorsColumns.LOGO_URL,
          LocationsColumns.LATITUDE,
          LocationsColumns.LONGITUDE,
          LocationsColumns.DISTANCE,
          LocationsColumns.LOCATION_NAME
  };


  @BindView(R.id.recyclerview_locations) RecyclerView mRecyclerView;
  @BindView(R.id.fab) FloatingActionButton fab;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.empty_view) TextView emptyView;
  @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
  @BindView(R.id.recyclerview_tags) RecyclerView recyclerViewTags;
  @BindView(R.id.open_today) RadioButton radioButtonOpenToday;
  @BindView(R.id.open_week) RadioButton radioButtonOpenWeek;
  @BindView(R.id.open_closed) RadioButton radioButtonOpenClosed;
  @BindView(R.id.radio_group_availability) RadioGroup radioGroupAvailability;

  private GoogleApiClient mGoogleApiClient;
  private FoodtruckAdapter mFoodtruckAdapter;
  private TagsAdapter mTagsAdapter;
  private RecyclerView.LayoutManager mLayoutManager;
  private Intent mServiceIntent;
  public static Tracker mTracker;

  private boolean isLocationGranted;

  public static Typeface mRobotoSlab;
  private boolean fabHidden;
  private ArrayList mSelectedTags;

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    setSupportActionBar(toolbar);

    // set up drawer right
    int filter = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREF_FILTER_AVAILABILITY, R.id.open_today);
    radioGroupAvailability.check(filter);
    recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));
    mTagsAdapter = new TagsAdapter();
    recyclerViewTags.setAdapter(mTagsAdapter);

    switch (filter) {
      case R.id.open_today:
        mContentUri = FoodtruckProvider.Operators.CONTENT_URI_TODAY;
        break;
      case R.id.open_closed:
        mContentUri = FoodtruckProvider.Operators.CONTENT_URI;
        break;
      default:
        mContentUri = FoodtruckProvider.Operators.CONTENT_URI_WEEK;
    }


    // set Google Analytics tracker
    FoodtruckApplication application = (FoodtruckApplication) getApplication();
    mTracker = application.getDefaultTracker();


    // set toolbar typedace
    mRobotoSlab = Typeface.createFromAsset(this.getAssets(), "RobotoSlab-Regular.ttf");
    Utility.setToolbarTitleFont(toolbar);


    // set up main recyclerview
    mRecyclerView.setHasFixedSize(true);
    mLayoutManager = new LinearLayoutManager(this);
    mRecyclerView.setLayoutManager(mLayoutManager);
    mFoodtruckAdapter = new FoodtruckAdapter(this);
    mRecyclerView.setAdapter(mFoodtruckAdapter);


    // init loaders
    getLoaderManager().initLoader(FOODTRUCK_LOADER_ID, null, this);
    getLoaderManager().initLoader(TAGS_LOADER_ID, null, this);


    // fetch data
    mServiceIntent = new Intent(this, FoodtruckIntentService.class);
    mServiceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_OPERATORS);
    startService(mServiceIntent);

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
      mServiceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_LOCATIONS);
      startService(mServiceIntent);

      Utility.setLastUpdatePref(this);
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
    switch (id) {
      case FOODTRUCK_LOADER_ID:
        return new CursorLoader(this,
                mContentUri,
                LOCATION_COLUMNS,
                null,
                null,
                LocationsColumns.DISTANCE + " is null, " + LocationsColumns.DISTANCE + " ASC");
      case TAGS_LOADER_ID:
        return new CursorLoader(this,
                FoodtruckProvider.Tags.CONTENT_URI,
                new String[]{
                        TagsColumns.TAG
                },
                null,
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
        updateEmptyView();

        // update widget
        int widgetIds[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), FoodtruckWidget.class));
        AppWidgetManager.getInstance(getApplication()).notifyAppWidgetViewDataChanged(widgetIds, R.layout.foodtruck_widget);
        break;
      case TAGS_LOADER_ID:
        mTagsAdapter.swapCursor(data);

        break;
    }
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
    getLoaderManager().restartLoader(TAGS_LOADER_ID, null, this);
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

  public void showRightDrawer(View view) {
    if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
      drawerLayout.openDrawer(GravityCompat.END);
    }
  }

  public void filterAvailability(View view) {
    if (view instanceof RadioButton) {

      Log.d(LOG_TAG, FoodtruckProvider.Operators.CONTENT_URI_TODAY.toString());

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      SharedPreferences.Editor prefsEdit = prefs.edit();
      prefsEdit.putInt(PREF_FILTER_AVAILABILITY, view.getId());
      prefsEdit.apply();

      Uri contentUri;
      switch (view.getId()) {
        case R.id.open_today:
          contentUri = FoodtruckProvider.Operators.CONTENT_URI_TODAY;
          Log.d(LOG_TAG, contentUri.toString());
          break;
        case R.id.open_closed:
          contentUri = FoodtruckProvider.Operators.CONTENT_URI;
          Log.d(LOG_TAG, contentUri.toString());
          break;
        default:
          contentUri = FoodtruckProvider.Operators.CONTENT_URI_WEEK;
      }

      if (!contentUri.equals(mContentUri)) {
        mContentUri = contentUri;
        onResume();
      }
    }
  }

  public void filter(View view) {
    TextView textView = (TextView) view.findViewById(R.id.tag);


    ImageView imageView = (ImageView) view.findViewById(R.id.tag_image);
    if (mSelectedTags == null)
      mSelectedTags = new ArrayList<>();
    String tag = textView.getText().toString();
    if (mSelectedTags.contains(tag)) {
      mSelectedTags.remove(tag);
      imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_lightgray_12dp));
      view.setBackgroundColor(Color.WHITE);
      textView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
    } else {
      mSelectedTags.add(tag);
      imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check_white_12dp));
      view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
      textView.setTextColor(Color.WHITE);
    }


    if (mSelectedTags.size() > 0) {
      String queryString = "(";
      for (int i = 0; i < mSelectedTags.size(); i++) {
        queryString += "'" + mSelectedTags.get(i) + "'";
      }
      queryString += ")";

      mFoodtruckAdapter.swapCursor(getContentResolver().query(
              mContentUri,
              LOCATION_COLUMNS,
              TagsColumns.TAG + " IN " + queryString,
              null,
              LocationsColumns.DISTANCE + " ASC")
      );
    } else {
      onResume();
    }
  }

  private class SearchViewListener implements SearchView.OnQueryTextListener {
    private Context mContext;

    public SearchViewListener(Context context) {
      mContext = context;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
      Cursor cursor = mContext.getContentResolver().query(
              mContentUri,
              LOCATION_COLUMNS,
              LocationsColumns.OPERATOR_NAME + " LIKE ? OR " + LocationsColumns.OPERATOR_OFFER + " LIKE ?",
              new String[]{"%" + query + "%", "%" + query + "%"},
              LocationsColumns.DISTANCE + " ASC");

      // send search query to analytics
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
              mContentUri,
              LOCATION_COLUMNS,
              LocationsColumns.OPERATOR_NAME + " LIKE ? OR " + LocationsColumns.OPERATOR_OFFER + " LIKE ?",
              new String[]{"%" + newText + "%", "%" + newText + "%"},
              LocationsColumns.DISTANCE + " ASC");
      mFoodtruckAdapter.swapCursor(cursor);
      return true;
    }
  }

}
