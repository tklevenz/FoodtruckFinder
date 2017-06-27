package co.pugo.apps.foodtruckfinder.ui;


import android.animation.Animator;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.io.File;
import java.io.FilenameFilter;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.ScheduleAdapter;
import co.pugo.apps.foodtruckfinder.data.FavouritesColumns;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;

/**
 * Created by tobias on 29.9.2016.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {
  @BindView(R.id.textView_operator) TextView operatorName;
  @BindView(R.id.textView_description) TextView description;
  @BindView(R.id.container_description) View containerDescription;
  @BindView(R.id.recyclerview_schedule) RecyclerView rvSchedule;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.toolbar_image) ImageView toolbarImage;
  @BindView(R.id.appbar_detail) AppBarLayout appBarDetail;
  @BindView(R.id.loading_spinner) View spinner;
  @BindView(R.id.contact_web) TextView webTexView;
  @BindView(R.id.contact_email) TextView emailTextView;
  @BindView(R.id.contact_phone) TextView phoneTextView;
  @BindView(R.id.contact_facebook) TextView faceboookTextView;
  @BindView(R.id.contact_twitter) TextView twitterTextView;
  @BindView(R.id.content_detail) View contentDetail;
  @BindView(R.id.map_view) MapView mapView;
  @BindView(R.id.map_overlay) View mapOverlay;
  @BindView(R.id.fab_favourite) FloatingActionButton fabFavourite;
  @BindView(R.id.map_logo_overlay) ImageView mapLogoOverlay;
  @BindView(R.id.collapsing_toolbar_detail) CollapsingToolbarLayout collapsingToolbarDetail;

  private static final String LOG_TAG = "DetailActivity";

  private static final Uri BASE_URI = Uri.parse("http://foodtruckfinder.pugo.co/foodtruck/");

  public static final int MAP_MARKER_ICON_SIZE = 280;

  private static final int DETAILS_LOADER_ID = 0;
  private static final int SCHEDULE_LOADER_ID = 1;

  public static Typeface mRobotoSlab;

  private ScheduleAdapter mScheduleAdapter;

  private GoogleApiClient mGoogleApiClient;

  private AppCompatActivity mActivity;

  private View.OnClickListener mOnContactLinkListener;

  private String mOperatorId;
  private String mWebUrl;
  private String mEmail;
  private String mPhone;
  private String mFacebookUrl;
  private String mTwitterUrl;
  private String mTwitter;
  private String mOperatorName;
  private String mOperatorRegion;
  private String mLogoUrl;

  private Cursor mScheduleCursor;

  private Bitmap mMarkerBg;

  private boolean mIsFavourite;
  private boolean mSnackBarShown = false;
  private boolean mIsLocalTime = true;

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

    // initialize map view
    mapView.onCreate(null);
    MapsInitializer.initialize(mActivity);

    // setup recycler view
    mScheduleAdapter = new ScheduleAdapter(mActivity);
    rvSchedule.setAdapter(mScheduleAdapter);
    rvSchedule.setNestedScrollingEnabled(false);
    rvSchedule.setLayoutManager(new LinearLayoutManager(mActivity));

    // init loaders
    getLoaderManager().initLoader(SCHEDULE_LOADER_ID, null, this);
    getLoaderManager().initLoader(DETAILS_LOADER_ID, null, this);

    // setup google api client for app index api access
    mGoogleApiClient = new GoogleApiClient
            .Builder(mActivity)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(AppIndex.API)
            .build();

    // listener for contact link clicks
    mOnContactLinkListener = new OpenContactLinkListener();

    // toggle favourite icon in fab
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
      default:
        return null;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    if (data != null && data.moveToFirst()) {
      switch (loader.getId()) {
        case DETAILS_LOADER_ID:
          spinner.setVisibility(View.GONE);

          mOperatorName = Html.fromHtml(data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_NAME))).toString();
          operatorName.setText(mOperatorName);
          operatorName.setTypeface(mRobotoSlab);
          description.setText(data.getString(data.getColumnIndex(OperatorDetailsColumns.DESCRIPTION)));

          Utility.setToolbarTitleFont(toolbar);

          appBarDetail.setVisibility(View.VISIBLE);
          fabFavourite.setVisibility(View.VISIBLE);
          description.setVisibility(View.VISIBLE);

          // creating toolbar background from locally stored bitmap files
          final String operatorId = mOperatorId;
          File file = mActivity.getFilesDir();
          File[] fileList = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
              return s.matches(operatorId + "-image" + "[0-2].png");
            }
          });
          Bitmap[] bitmaps = new Bitmap[3];
          for (int i = 0; i < fileList.length; i++) {
            bitmaps[i] = BitmapFactory.decodeFile(fileList[i].getPath());
          }
          if (bitmaps[0] != null || bitmaps[1] != null || bitmaps[2] != null) {
            int height = bitmaps[0].getHeight();
            int width = bitmaps[0].getWidth() + bitmaps[1].getWidth() + bitmaps[2].getWidth();

            Bitmap combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(combinedBitmap);
            canvas.drawBitmap(bitmaps[0], 0, 0, null);
            canvas.drawBitmap(bitmaps[1], bitmaps[0].getWidth(), 0, null);
            canvas.drawBitmap(bitmaps[2], bitmaps[0].getWidth() + bitmaps[1].getWidth(), 0, null);

            toolbarImage.setImageDrawable(new BitmapDrawable(getResources(), combinedBitmap));
          }

          // if operator is premium showing contact links
          if (data.getInt(data.getColumnIndex(OperatorDetailsColumns.PREMIUM)) == 1) {
            // setup contact views
            String webUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.WEBSITE_URL));
            if (webUrl.length() > 0) {
              webTexView.setVisibility(View.VISIBLE);
              webTexView.setText(data.getString(data.getColumnIndex(OperatorDetailsColumns.WEBSITE)));
              mWebUrl = webUrl;
              webTexView.setOnClickListener(mOnContactLinkListener);
            }
            String email = data.getString(data.getColumnIndex(OperatorDetailsColumns.EMAIL));
            if (email.length() > 0) {
              emailTextView.setVisibility(View.VISIBLE);
              emailTextView.setText(email);
              mEmail = email;
              emailTextView.setOnClickListener(mOnContactLinkListener);
            }
            String phone = data.getString(data.getColumnIndex(OperatorDetailsColumns.PHONE));
            if (phone.length() > 0) {
              phoneTextView.setVisibility(View.VISIBLE);
              phoneTextView.setText(phone);
              mPhone = phone;
              phoneTextView.setOnClickListener(mOnContactLinkListener);
            }
            String facebookUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.FACEBOOK_URL));
            if (facebookUrl.length() > 0) {
              faceboookTextView.setVisibility(View.VISIBLE);
              faceboookTextView.setText(data.getString(data.getColumnIndex(OperatorDetailsColumns.FACEBOOK)));
              mFacebookUrl = facebookUrl;
              faceboookTextView.setOnClickListener(mOnContactLinkListener);
            }
            String twitter = data.getString(data.getColumnIndex(OperatorDetailsColumns.TWITTER));
            if (twitter.length() > 0) {
              twitterTextView.setVisibility(View.VISIBLE);
              twitterTextView.setText(twitter.startsWith("@") ? twitter : "@" + twitter);
              mTwitter = twitter;
              mTwitterUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.TWITTER_URL));
              twitterTextView.setOnClickListener(mOnContactLinkListener);
            }
          }

          contentDetail.setVisibility(View.VISIBLE);
          rvSchedule.setVisibility(View.VISIBLE);

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
          int color;
          try {
            color = Color.parseColor(data.getString(data.getColumnIndex(OperatorDetailsColumns.LOGO_BACKGROUND)));
          } catch (Exception e) {
            color = Color.WHITE;
          }
          mMarkerBg = Utility.colorBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_marker_bg_bubble), color);
          mOperatorRegion = data.getString(data.getColumnIndex(OperatorDetailsColumns.REGION));
          mLogoUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.LOGO_URL));
          mapView.getMapAsync(this);

          if (!mIsLocalTime)
            showSnackBar();

          break;
        case SCHEDULE_LOADER_ID:
          mIsLocalTime = Utility.isLocalTime(data.getString(data.getColumnIndex(LocationsColumns.START_DATE)));
          mScheduleAdapter.swapCursor(data);
          mScheduleCursor = data;
      }
    }
  }


  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mScheduleAdapter.swapCursor(null);
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

  @Override
  public void onMapReady(GoogleMap googleMap) {
    if (mScheduleCursor != null && mScheduleCursor.moveToFirst()) {
      double latitude = mScheduleCursor.getDouble(mScheduleCursor.getColumnIndex(LocationsColumns.LATITUDE));
      double longitude = mScheduleCursor.getDouble(mScheduleCursor.getColumnIndex(LocationsColumns.LONGITUDE));
      final int location_id = mScheduleCursor.getInt(mScheduleCursor.getColumnIndex(LocationsColumns._ID));
      final String logoUrl = mScheduleCursor.getString(mScheduleCursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL));

      Bitmap markerBitmap = Utility.getMarkerBitmap(getContext(),
              mScheduleCursor.getString(mScheduleCursor.getColumnIndex(LocationsColumns.OPERATOR_ID)),
              mScheduleCursor.getString(mScheduleCursor.getColumnIndex(LocationsColumns.IMAGE_ID)));

      Marker marker = googleMap.addMarker(new MarkerOptions()
              .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
              .position(new LatLng(latitude, longitude)));
      marker.setAnchor(1, 1);

      // center map on LatLng
      googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));

      // get projection to translate from and to screen location
      Projection projection = googleMap.getProjection();
      Point bottomRight = projection.toScreenLocation(projection.getVisibleRegion().nearRight);
      int markerW = markerBitmap != null ? markerBitmap.getWidth() : 0;
      LatLng center = projection.fromScreenLocation(new Point(bottomRight.x / 2 - markerW / 2, bottomRight.y / 2 - markerW / 2));

      // recenter map to center the map marker icon
      googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15));

      final double finalLongitude = longitude;
      final double finalLatitude = latitude;

      mapOverlay.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent mapIntent = new Intent(mActivity, MapActivity.class);
          mapIntent.putExtra(MapActivity.LONGITUDE_TAG, finalLongitude);
          mapIntent.putExtra(MapActivity.LATITUDE_TAG, finalLatitude);
          mapIntent.putExtra(MapActivity.LOGO_URL_EXTRA, logoUrl);
          mapIntent.putExtra(MapActivity.LOCATION_ID, location_id);

          mActivity.startActivity(mapIntent);
        }
      });
      mapView.onResume();

    } else {
      LatLng regionLatLng = Utility.getLatLngFromRegion(mActivity, mOperatorRegion);


      if (regionLatLng != null) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(regionLatLng, 11));
        mapView.onResume();
      }

      googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mActivity, R.raw.map_style_silver));

      mapLogoOverlay.setVisibility(View.VISIBLE);
      Glide.with(mActivity)
              .load(mLogoUrl)
              .asBitmap()
              .fitCenter()
              .diskCacheStrategy(DiskCacheStrategy.ALL)
              .into(new SimpleTarget<Bitmap>(300, 300) {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                  resource = Utility.addDropShadow(resource, Color.GRAY, 10, 0, 2);
                  mapLogoOverlay.setImageDrawable(new BitmapDrawable(mActivity.getResources(), resource));
                }
              });

    }
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

  private class OpenContactLinkListener implements View.OnClickListener {
    @Override
    public void onClick(final View view) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        int finalRadius = Math.max(view.getWidth(), view.getHeight()) / 2;
        Animator anim = ViewAnimationUtils.createCircularReveal(view, view.getWidth() / 2, view.getHeight() / 2, 0, finalRadius);
        view.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.highlightColor));
        anim.start();
        anim.addListener(new Animator.AnimatorListener() {
          @Override
          public void onAnimationStart(Animator animator) {
          }

          @Override
          public void onAnimationEnd(Animator animator) {
            view.setBackgroundColor(Color.TRANSPARENT);
            openLink(view);
          }

          @Override
          public void onAnimationCancel(Animator animator) {
          }

          @Override
          public void onAnimationRepeat(Animator animator) {
          }
        });
      } else {
        openLink(view);
      }
    }

    private void openLink(View view) {
      Uri uri;
      switch (view.getId()) {
        case R.id.contact_web:
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mWebUrl)));
          break;
        case R.id.contact_email:
          startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + mEmail)));
          break;
        case R.id.contact_phone:
          startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mPhone)));
          break;
        case R.id.contact_facebook:
          try {
            mActivity.getPackageManager().getApplicationInfo("com.facebook.katana", 0);
            uri = Uri.parse("fb://facewebmodal/f?href=" + mFacebookUrl);
          } catch (PackageManager.NameNotFoundException e) {
            uri = Uri.parse(mFacebookUrl);
          }
          startActivity(new Intent(Intent.ACTION_VIEW, uri));
          break;
        case R.id.contact_twitter:
          try {
            mActivity.getPackageManager().getApplicationInfo("com.twitter.android", 0);
            uri = Uri.parse("twitter://user?user_id=" + mTwitter);
          } catch (PackageManager.NameNotFoundException e) {
            uri = Uri.parse(mTwitterUrl);
          }
          startActivity(new Intent(Intent.ACTION_VIEW, uri));
      }
    }
  }
}
