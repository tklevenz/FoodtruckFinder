package co.pugo.apps.foodtruckfinder.ui;


import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.DetailsAdapter;
import co.pugo.apps.foodtruckfinder.data.FavouritesColumns;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.ImpressionsColumns;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import co.pugo.apps.foodtruckfinder.model.DetailsDividerItem;
import co.pugo.apps.foodtruckfinder.model.DetailsItem;
import co.pugo.apps.foodtruckfinder.model.MapItem;
import co.pugo.apps.foodtruckfinder.model.OperatorDetailsItem;
import co.pugo.apps.foodtruckfinder.model.ScheduleItem;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;

/**
 * Created by tobias on 29.9.2016.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks /*, OnMapReadyCallback*/ {

  @BindView(R.id.recyclerview_detail) RecyclerView rvDetaill;

  @BindView(R.id.toolbar) Toolbar toolbar;
  /*
  @BindView(R.id.toolbar_image) ImageView toolbarImage;
  @BindView(R.id.toolbar_image_1) ImageView toolbarImage1;
  @BindView(R.id.toolbar_image_2) ImageView toolbarImage2;
  @BindView(R.id.toolbar_image_3) ImageView toolbarImage3;
  */
  @BindView(R.id.toolbar_image_container) LinearLayout toolbarImageContainer;
  @BindView(R.id.appbar_detail) AppBarLayout appBarDetail;
  @BindView(R.id.loading_spinner) View spinner;
  @BindView(R.id.fab_favourite) FloatingActionButton fabFavourite;
  @BindView(R.id.collapsing_toolbar_detail) CollapsingToolbarLayout collapsingToolbarDetail;

  private static final String LOG_TAG = "DetailActivity";

  private static final Uri BASE_URI = Uri.parse("http://foodtruckfinder.pugo.co/foodtruck/");

  private static final int DETAILS_LOADER_ID = 0;
  private static final int SCHEDULE_LOADER_ID = 1;
  private static final int IMPRESSIONS_LOADER_ID = 2;

  public static Typeface mRobotoSlab;

  private GoogleApiClient mGoogleApiClient;

  private AppCompatActivity mActivity;

  private String mOperatorId;
  private String mOperatorName;
  private int mLogoColor;

  private boolean mIsFavourite;
  private boolean mSnackBarShown = false;
  private boolean mIsLocalTime = true;
  private boolean mIsPremium;
  private boolean[] mLoaderFinished = {false, false};
  private MapItem mMapItem = new MapItem();
  private OperatorDetailsItem mOperatorDetailsItem;
  private List<ScheduleItem> mScheduleItems = new ArrayList<>();
  private DetailsAdapter mDetailsAdapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_detail, container, false);
    ButterKnife.bind(this, view);

    mActivity = (AppCompatActivity) getActivity();

    mRobotoSlab = Typeface.createFromAsset(mActivity.getAssets(), "RobotoSlab-Regular.ttf");

    mOperatorId = mActivity.getIntent().getStringExtra(FoodtruckIntentService.OPERATORID_TAG);
    if (mOperatorId == null) {
      String data = mActivity.getIntent().getDataString();
      mOperatorId = data.substring(data.lastIndexOf("/") + 1);
    }

    boolean isActive = Utility.isActive(mActivity, mOperatorId);

    // init loaders
    if (isActive)
      getLoaderManager().initLoader(SCHEDULE_LOADER_ID, null, this);
    else
      mLoaderFinished[SCHEDULE_LOADER_ID] = true;

    getLoaderManager().initLoader(DETAILS_LOADER_ID, null, this);
    getLoaderManager().initLoader(IMPRESSIONS_LOADER_ID, null, this);

    // set toolbar font
    Utility.setToolbarTitleFont(toolbar);

    rvDetaill.setHasFixedSize(true);
    rvDetaill.setLayoutManager(new LinearLayoutManager(mActivity));
    mDetailsAdapter = new DetailsAdapter(mActivity);
    mDetailsAdapter.setHasStableIds(true);
    rvDetaill.setAdapter(mDetailsAdapter);

    // setup google api client for app index api access
    mGoogleApiClient = new GoogleApiClient
            .Builder(mActivity)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(AppIndex.API)
            .build();

    // toggle favourite icon in fab
    // TODO: notification about truck added to favourites
    mIsFavourite = Utility.isFavourite(mActivity, mOperatorId);
    setFabFavourite(mIsFavourite);
    fabFavourite.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (mIsFavourite) {
          setFabFavourite(false);
          mActivity.getContentResolver().delete(FoodtruckProvider.Favourites.CONTENT_URI, FavouritesColumns.ID + " = ?", new String[]{mOperatorId});
        } else {
          setFabFavourite(true);
          ContentValues cv = new ContentValues();
          cv.put(FavouritesColumns.ID, mOperatorId);
          cv.put(FavouritesColumns.FAVOURITE, true);
          mActivity.getContentResolver().insert(FoodtruckProvider.Favourites.CONTENT_URI, cv);
        }
        mIsFavourite = !mIsFavourite;
      }
    });

    return view;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch (id) {
      case DETAILS_LOADER_ID:
        return new CursorLoader(mActivity,
                FoodtruckProvider.OperatorDetails.withOperatorId(mOperatorId),
                new String[]{
                        OperatorDetailsColumns.OPERATOR_ID,
                        OperatorDetailsColumns.OPERATOR_NAME,
                        OperatorDetailsColumns.OPERATOR_OFFER,
                        OperatorDetailsColumns.DESCRIPTION,
                        OperatorDetailsColumns.DESCRIPTION_LONG,
                        OperatorDetailsColumns.WEBSITE,
                        OperatorDetailsColumns.WEBSITE_URL,
                        OperatorDetailsColumns.FACEBOOK,
                        OperatorDetailsColumns.FACEBOOK_URL,
                        OperatorDetailsColumns.TWITTER,
                        OperatorDetailsColumns.TWITTER_URL,
                        OperatorDetailsColumns.EMAIL,
                        OperatorDetailsColumns.PHONE,
                        OperatorDetailsColumns.LOGO_URL,
                        OperatorDetailsColumns.LOGO_BACKGROUND,
                        OperatorDetailsColumns.PREMIUM,
                        OperatorDetailsColumns.REGION
                },
                null,
                null,
                null
        );
      case SCHEDULE_LOADER_ID:
        return new CursorLoader(mActivity,
                FoodtruckProvider.Locations.withOperatorId(mOperatorId),
                new String[]{
                        LocationsColumns._ID,
                        LocationsColumns.OPERATOR_NAME,
                        LocationsColumns.START_DATE,
                        LocationsColumns.END_DATE,
                        LocationsColumns.LATITUDE,
                        LocationsColumns.LONGITUDE,
                        LocationsColumns.LOCATION_NAME,
                        LocationsColumns.CITY,
                        LocationsColumns.ZIPCODE,
                        LocationsColumns.STREET,
                        LocationsColumns.NUMBER,
                        LocationsColumns.DISTANCE,
                        LocationsColumns.OPERATOR_LOGO_URL,
                        LocationsColumns.OPERATOR_ID,
                        LocationsColumns.IMAGE_ID
                },
                null,
                null,
                LocationsColumns.START_DATE + " ASC," + LocationsColumns.DISTANCE + " ASC"
        );
      case IMPRESSIONS_LOADER_ID:
        return new CursorLoader(mActivity,
                FoodtruckProvider.Impressions.withId(mOperatorId),
                new String[]{
                        ImpressionsColumns.IMPRESSION
                },
                null,
                null,
                null);
      default:
        return null;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    if (data != null && data.moveToFirst()) {

      switch (loader.getId()) {
        case DETAILS_LOADER_ID:
          mOperatorDetailsItem = new OperatorDetailsItem();

          mOperatorDetailsItem.operatorName = Html.fromHtml(data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_NAME))).toString();
          mOperatorDetailsItem.description = data.getString(data.getColumnIndex(OperatorDetailsColumns.DESCRIPTION));

          mOperatorName = Html.fromHtml(data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_NAME))).toString();


          mIsPremium = data.getInt(data.getColumnIndex(OperatorDetailsColumns.PREMIUM)) == 1;

          // if operator is premium showing contact links
          if (mIsPremium) {
            mOperatorDetailsItem.webUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.WEBSITE_URL));
            mOperatorDetailsItem.email = data.getString(data.getColumnIndex(OperatorDetailsColumns.EMAIL));
            mOperatorDetailsItem.phone = data.getString(data.getColumnIndex(OperatorDetailsColumns.PHONE));
            mOperatorDetailsItem.facebook = data.getString(data.getColumnIndex(OperatorDetailsColumns.FACEBOOK));
            mOperatorDetailsItem.facebookUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.FACEBOOK_URL));
            mOperatorDetailsItem.twitter = data.getString(data.getColumnIndex(OperatorDetailsColumns.TWITTER));
            mOperatorDetailsItem.twitterUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.TWITTER_URL));
          }

          mOperatorDetailsItem.premium = mIsPremium;

          collapsingToolbarDetail.setTitle(mOperatorName);
          collapsingToolbarDetail.setCollapsedTitleTypeface(mRobotoSlab);

          // listener hides title in toolbar when expanded and shows in collapsed mode
          appBarDetail.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean showTitle = false;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

              final TypedArray styledAttributes = getContext().getTheme().obtainStyledAttributes(
                      new int[]{android.R.attr.actionBarSize});
              int actionBarSize = (int) styledAttributes.getDimension(0, 0);
              styledAttributes.recycle();

              int scrollRange = appBarLayout.getTotalScrollRange();

              if (scrollRange + verticalOffset == actionBarSize && showTitle) {
                collapsingToolbarDetail.setCollapsedTitleTextColor(Color.WHITE);
                collapsingToolbarDetail.setExpandedTitleColor(Color.WHITE);
              } else {
                collapsingToolbarDetail.setCollapsedTitleTextColor(Color.TRANSPARENT);
                collapsingToolbarDetail.setExpandedTitleColor(Color.TRANSPARENT);
              }

              // if scrolled to the top
              if (scrollRange + verticalOffset == 0)
                showTitle = true;

              // if scrolled to the bottom
              if (verticalOffset == 0)
                showTitle = false;
            }
          });

          mGoogleApiClient.connect();

          // setup map view
          try {
            mLogoColor = Color.parseColor(data.getString(data.getColumnIndex(OperatorDetailsColumns.LOGO_BACKGROUND)));
          } catch (Exception e) {
            mLogoColor = Color.WHITE;
          }

          mMapItem.region = data.getString(data.getColumnIndex(OperatorDetailsColumns.REGION));
          mMapItem.logoUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.LOGO_URL));

          mLoaderFinished[DETAILS_LOADER_ID] = true;

          setUpItems();

          break;
        case SCHEDULE_LOADER_ID:
          mIsLocalTime = Utility.isLocalTime(data.getString(data.getColumnIndex(LocationsColumns.START_DATE)));

          mMapItem.latitude = data.getDouble(data.getColumnIndex(LocationsColumns.LATITUDE));
          mMapItem.longitude = data.getDouble(data.getColumnIndex(LocationsColumns.LONGITUDE));
          mMapItem.locationId = data.getInt(data.getColumnIndex(LocationsColumns._ID));

          String logoUrl = data.getString(data.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL));
          String imageId = data.getString(data.getColumnIndex(LocationsColumns.IMAGE_ID));

          Bitmap markerBitmap = Utility.getMarkerBitmap(getContext(), mOperatorId, imageId, false);

          //markerBitmap = (markerBitmap != null) ? markerBitmap : Utility.createMapMarker(getContext(), logoUrl, mLogoColor, Utility.getMarkerFileName(mOperatorId, imageId));

          mMapItem.logo = markerBitmap;
          mMapItem.markerColor = mLogoColor;

          String endDate = data.getString(data.getColumnIndex(LocationsColumns.END_DATE));
          String startDate = data.getString(data.getColumnIndex(LocationsColumns.START_DATE));

          if (Utility.isActiveToday(endDate))
            mMapItem.dateRange = MapActivity.DATE_RANGE_TODAY;
          else if (Utility.isActiveTomorrow(endDate))
            mMapItem.dateRange = MapActivity.DATE_RANGE_TOMORROW;
          else
            mMapItem.dateRange = MapActivity.DATE_RANGE_THIS_WEEK;

          do {
            mScheduleItems.add(new ScheduleItem(
                    Utility.getFormattedDate(data.getString(data.getColumnIndex(LocationsColumns.START_DATE)), mActivity),
                    data.getString(data.getColumnIndex(LocationsColumns.LOCATION_NAME)),
                    data.getString(data.getColumnIndex(LocationsColumns.STREET)) + " " + data.getString(data.getColumnIndex(LocationsColumns.NUMBER)),
                    data.getString(data.getColumnIndex(LocationsColumns.CITY)),
                    String.format(
                            mActivity.getString(R.string.schedule_time),
                            Utility.getFormattedTime(startDate),
                            Utility.getFormattedTime(endDate)
                    ),
                    Utility.formatDistance(mActivity, data.getFloat(data.getColumnIndex(LocationsColumns.DISTANCE)))
            ));

          } while (data.moveToNext());

          mLoaderFinished[SCHEDULE_LOADER_ID] = true;

          setUpItems();

          break;

        case IMPRESSIONS_LOADER_ID:
          int i = 0;
          if (toolbarImageContainer.getChildCount() == 0) {
            do {
              ImageView imageView = new ImageView(mActivity);
              imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

              LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
              params.weight = 1;

              imageView.setLayoutParams(params);

              Glide.with(mActivity)
                      .load(data.getString(data.getColumnIndex(ImpressionsColumns.IMPRESSION)))
                      .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                      .into(imageView);

              toolbarImageContainer.addView(imageView);
            } while (data.moveToNext() && ++i < 3);
          }

      }
    } else if (loader.getId() == SCHEDULE_LOADER_ID) {
      mLoaderFinished[SCHEDULE_LOADER_ID] = true;
    }
  }

  private void setUpItems() {
    Log.d(LOG_TAG, "details loader: " + mLoaderFinished[DETAILS_LOADER_ID] + " schedule loader: " + mLoaderFinished[SCHEDULE_LOADER_ID]);
    if (mLoaderFinished[DETAILS_LOADER_ID] && mLoaderFinished[SCHEDULE_LOADER_ID]) {
      List<DetailsItem> items = new ArrayList<>();
      items.add(mMapItem);
      items.add(new DetailsDividerItem());
      items.add(mOperatorDetailsItem);
      DetailsDividerItem divider = new DetailsDividerItem();
      divider.Color = getResources().getColor(R.color.light_gray);
      items.add(divider);
      for (ScheduleItem item : mScheduleItems) {
        items.add(item);
      }

      mDetailsAdapter.swapItems(items);

      showUiElements();
    }
  }


  private void showUiElements() {
    spinner.setVisibility(View.GONE);

    appBarDetail.setVisibility(View.VISIBLE);
    fabFavourite.setVisibility(View.VISIBLE);

    if (!mIsLocalTime)
      showSnackBar();
  }


  @Override
  public void onLoaderReset(Loader<Cursor> loader) {

  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    final Uri APP_URI = BASE_URI.buildUpon().appendPath(mOperatorId).build();
    Action viewAction = Action.newAction(Action.TYPE_VIEW, mOperatorName, APP_URI);
    PendingResult<Status> result = AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);

    result.setResultCallback(new ResultCallback<Status>() {
      @Override
      public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
          Log.d(LOG_TAG, "App Indexing API: Indexed foodtruck "
                         + mOperatorName + " view successfully.");
        } else {
          Log.e(LOG_TAG, "App Indexing API: There was an error indexing the foodtruck view."
                         + status.toString());
        }
      }
    });
  }

  @Override
  public void onStop() {
    if (mGoogleApiClient.isConnected()) {
      final Uri APP_URI = BASE_URI.buildUpon().appendPath(mOperatorId).build();
      Action viewAction = Action.newAction(Action.TYPE_VIEW, mOperatorName, APP_URI);
      PendingResult<Status> result = AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);

      result.setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
          if (status.isSuccess()) {
            Log.d(LOG_TAG, "App Indexing API: Indexed foodtruck "
                           + mOperatorName + " view end successfully.");
          } else {
            Log.e(LOG_TAG, "App Indexing API: There was an error indexing the recipe view."
                           + status.toString());
          }
        }
      });

      mGoogleApiClient.disconnect();
    }
    super.onStop();
  }

  @Override
  public void onConnectionSuspended(int i) {
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
  }

  private void setFabFavourite(boolean isFav) {
    if (isFav) {
      fabFavourite.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_favorite_white_24dp));
    } else {
      fabFavourite.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_favorite_border_white_24dp));
    }
  }

  private void showSnackBar() {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    boolean snackBarDismissed = prefs.getBoolean(getString(R.string.pref_snackbar_dismissed_key), false);

    if (!mSnackBarShown && !snackBarDismissed) {
      final Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content),
              getString(R.string.snackbar_display_local_time), Snackbar.LENGTH_LONG);

      snackbar.setAction(getString(R.string.snackbar_btn_ok), new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          snackbar.dismiss();
          SharedPreferences.Editor prefsEdit = prefs.edit();
          prefsEdit.putBoolean(getString(R.string.pref_snackbar_dismissed_key), true);
          prefsEdit.apply();
        }
      });

      snackbar.setDuration(10000);

      snackbar.show();
    }
    mSnackBarShown = true;
  }
}
