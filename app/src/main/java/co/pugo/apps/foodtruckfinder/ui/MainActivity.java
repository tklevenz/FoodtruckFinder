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
import android.widget.ImageView;
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


public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

  @BindView(R.id.recyclerview_locations) RecyclerView mRecyclerView;
  @BindView(R.id.fab) FloatingActionButton mFab;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.empty_view) TextView emptyView;
  @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
  @BindView(R.id.recyclerview_tags) RecyclerView recyclerViewTags;
  @BindView(R.id.filter_favourite) ImageView imageViewFavourites;
  @BindView(R.id.tags_title) TextView tagsTitle;

  private static final String LOG_TAG = MainActivity.class.getSimpleName();

  private static final int LOCATION_PERMISSION_REQUEST = 0;

  private static final int FOODTRUCK_LOADER_ID = 0;
  private static final int TAGS_LOADER_ID = 1;

  private static final String LOCATIONS_PERIODIC_TASK = "periodic_task_locations";
  private static final String OPERATORS_PERIODIC_TASK = "periodic_task_operators";

  private Uri mContentUri = FoodtruckProvider.Operators.CONTENT_URI_JOINED;

  private String[] LOCATION_COLUMNS = {
          FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID,
          OperatorsColumns.NAME,
          OperatorsColumns.OFFER,
          OperatorsColumns.LOGO_URL,
          LocationsColumns.LATITUDE,
          LocationsColumns.LONGITUDE,
          LocationsColumns.DISTANCE,
          LocationsColumns.LOCATION_NAME,
          OperatorsColumns.REGION,
          LocationsColumns.START_DATE,
          OperatorsColumns.DISTANCE_APROX
  };

  private GoogleApiClient mGoogleApiClient;

  private FoodtruckAdapter mFoodtruckAdapter;
  private TagsAdapter mTagsAdapter;
  private Intent mServiceIntent;

  public Tracker mTracker;

  public static Typeface mRobotoSlab;

  private ArrayList<String> mSelectedTags;
  private int mRadius;

  private boolean mIsLoadFinished;
  private boolean mFavouritesSelected;
  private boolean mIsLocationGranted;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    setSupportActionBar(toolbar);

    // set up drawer right
    recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));
    mTagsAdapter = new TagsAdapter(this);
    recyclerViewTags.setAdapter(mTagsAdapter);


    // set Google Analytics tracker
    FoodtruckApplication application = (FoodtruckApplication) getApplication();
    mTracker = application.getDefaultTracker();


    // set typeface for toolbar
    mRobotoSlab = Typeface.createFromAsset(this.getAssets(), "RobotoSlab-Regular.ttf");
    Utility.setToolbarTitleFont(toolbar);
    tagsTitle.setTypeface(mRobotoSlab);

    // set up recycler view
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mFoodtruckAdapter = new FoodtruckAdapter(this);
    mRecyclerView.setAdapter(mFoodtruckAdapter);


    // init loaders
    getLoaderManager().initLoader(FOODTRUCK_LOADER_ID, null, this);
    getLoaderManager().initLoader(TAGS_LOADER_ID, null, this);

    // setup google api client for location api access
    mGoogleApiClient = new GoogleApiClient
            .Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();

    // check for location permission
    if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
              LOCATION_PERMISSION_REQUEST);

    } else {
      mGoogleApiClient.connect();
      mIsLocationGranted = true;
      updateEmptyView();
      startFetchOperatorsIntent();
      startFetchLocationsIntent();
    }


    // update foodtruck location data every 24 hours
    schedulePeriodicTask(FoodtruckTaskService.TASK_FETCH_LOCATIONS, 86400L, LOCATIONS_PERIODIC_TASK);

    // update foodtruck operator data every 7 days
    schedulePeriodicTask(FoodtruckTaskService.TASK_FETCH_OPERATORS, 604800L, OPERATORS_PERIODIC_TASK);
  }

  @Override
  protected void onResume() {
    super.onResume();
    // get location radius
    // TODO: change default to 50 before release
    mRadius = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_location_radius_key), "200")) * 1000;
    if (PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_distance_unit_key), "").equals(getString(R.string.pref_unit_miles)))
      mRadius = (int) Math.round(mRadius * 1.60924);
    // restart loaders
    getLoaderManager().restartLoader(FOODTRUCK_LOADER_ID, null, this);
    getLoaderManager().restartLoader(TAGS_LOADER_ID, null, this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mGoogleApiClient.isConnected())
      mGoogleApiClient.disconnect();
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

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      startActivity(new Intent(this, SettingsActivity.class));
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
          throws SecurityException {
    switch (requestCode) {
      case LOCATION_PERMISSION_REQUEST: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mGoogleApiClient.connect();
          mIsLocationGranted = true;
          updateEmptyView();
          startFetchLocationsIntent();
        } else {
          updateEmptyView();
        }
      }
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case FOODTRUCK_LOADER_ID:
        return new CursorLoader(this,
                mContentUri,
                LOCATION_COLUMNS,
                LocationsColumns.DISTANCE + " < " + mRadius + " OR " +
                OperatorsColumns.DISTANCE_APROX + " < " + mRadius,
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
        mIsLoadFinished = true;
        updateEmptyView();

        // update widget
        // TODO: fix widget
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

  // update text displayed in empty view shown when no foodtrucks are displayed
  private void updateEmptyView() {
    if (mFoodtruckAdapter.getItemCount() == 0) {
      emptyView.setVisibility(View.VISIBLE);
      if (Utility.isNetworkAvailable(this)) {
        if (mFavouritesSelected) {
          emptyView.setText(getString(R.string.no_favourites));
        } else if (mIsLocationGranted) {
          if (Utility.operatorsExist(this) && mIsLoadFinished) {
            emptyView.setText(R.string.no_foodtrucks_found_for_radius);
          } else {
            emptyView.setText(getString(R.string.getting_foodtuck_data));
          }
        } else {
          emptyView.setText(getString(R.string.no_location_available));
        }
      } else {
        emptyView.setText(getString(R.string.no_network_available));
      }
    } else {
      emptyView.setVisibility(View.GONE);
    }
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
  }


  // start service that fetches operator data
  private void startFetchOperatorsIntent() {
    if (Utility.isOutOfDate(this, FoodtruckTaskService.TASK_FETCH_OPERATORS)) {
      mServiceIntent = new Intent(this, FoodtruckIntentService.class);
      mServiceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_OPERATORS);
      startService(mServiceIntent);
      Utility.setLastUpdatePref(this, FoodtruckTaskService.TASK_FETCH_OPERATORS);
    }
  }

  // start service that fetched weekly location data
  private void startFetchLocationsIntent() {
    if (Utility.isOutOfDate(this, FoodtruckTaskService.TASK_FETCH_LOCATIONS)) {
      mServiceIntent = new Intent(this, FoodtruckIntentService.class);
      mServiceIntent.putExtra(FoodtruckIntentService.TASK_TAG, FoodtruckTaskService.TASK_FETCH_LOCATIONS);
      startService(mServiceIntent);
      Utility.setLastUpdatePref(this, FoodtruckTaskService.TASK_FETCH_LOCATIONS);
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

      mFoodtruckAdapter.swapCursor(getContentResolver().query(
              mContentUri,
              LOCATION_COLUMNS,
              "(" + LocationsColumns.DISTANCE + " < " + mRadius + " OR " +
              OperatorsColumns.DISTANCE_APROX + " < " + mRadius + ") AND " +
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
              OperatorsColumns.NAME + " LIKE ? OR " + OperatorsColumns.OFFER + " LIKE ?",
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
              OperatorsColumns.NAME + " LIKE ? OR " + OperatorsColumns.OFFER + " LIKE ?",
              new String[]{"%" + newText + "%", "%" + newText + "%"},
              LocationsColumns.DISTANCE + " ASC");
      mFoodtruckAdapter.swapCursor(cursor);
      return true;
    }
  }

}