package co.pugo.apps.foodtruckfinder.ui;


import android.animation.Animator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.analytics.HitBuilders;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FilenameFilter;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.adapter.ScheduleAdapter;
import co.pugo.apps.foodtruckfinder.data.FoodtruckProvider;
import co.pugo.apps.foodtruckfinder.data.LocationsColumns;
import co.pugo.apps.foodtruckfinder.data.OperatorDetailsColumns;
import co.pugo.apps.foodtruckfinder.service.FoodtruckIntentService;

/**
 * Created by tobias on 29.9.2016.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {

  private static final String LOG_TAG = "DetailActivity";
  private static final Uri BASE_URI = Uri.parse("http://foodtruckfinder.pugo.co/foodtruck/");
  @BindView(R.id.textView_description) TextView description;
  @BindView(R.id.container_description) View containerDescription;
  @BindView(R.id.recyclerview_schedule) RecyclerView rvSchedule;
  @BindView(R.id.details_divider) View detailsDivider;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.toolbar_image) ImageView toolbarImage;
  @BindView(R.id.background_protection) View backgroundProtection;
  @BindView(R.id.appbar_detail) View appbarDetail;
  @BindView(R.id.loading_spinner) View spinner;
  @BindView(R.id.operator_logo) ImageView logo;
  @BindView(R.id.contact_web) TextView webTexView;
  @BindView(R.id.contact_email) TextView emailTextView;
  @BindView(R.id.contact_phone) TextView phoneTextView;
  @BindView(R.id.contact_facebook) TextView faceboookTextView;
  @BindView(R.id.contact_twitter) TextView twitterTextView;
  @BindView(R.id.content_detail) View contentDetail;
  @BindView(R.id.mapview) MapView mapView;
  @BindView(R.id.map_overlay) View mapOverlay;

  private static final int DETAILS_LOADER_ID = 0;
  private static final int SCHEDULE_LOADER_ID = 1;
  private String mOperatorId;
  private ScheduleAdapter mScheduleAdapter;
  private String mWebUrl;
  private String mEmail;
  private String mPhone;

  private String mFacebookUrl;
  private String mTwitterUrl;
  private String mTwitter;
  private GoogleApiClient mGoogleApiClient;
  private String mOperatorName;
  private AppCompatActivity mActivity;
  private View.OnClickListener mOnContactLinkListener;
  private Cursor mScheduleCursor;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_detail, container, false);
    ButterKnife.bind(this, view);

    mActivity = (AppCompatActivity) getActivity();

    mOperatorId = mActivity.getIntent().getStringExtra(FoodtruckIntentService.OPERATORID_TAG);
    if (mOperatorId == null) {
      String data = mActivity.getIntent().getDataString();
      mOperatorId = data.substring(data.lastIndexOf("/") + 1);
    }

    mapView.onCreate(savedInstanceState);
    MapsInitializer.initialize(mActivity);

    mScheduleAdapter = new ScheduleAdapter(mActivity);
    rvSchedule.setAdapter(mScheduleAdapter);
    rvSchedule.setNestedScrollingEnabled(false);
    rvSchedule.setLayoutManager(new LinearLayoutManager(mActivity));

    getLoaderManager().initLoader(DETAILS_LOADER_ID, null, this);
    getLoaderManager().initLoader(SCHEDULE_LOADER_ID, null, this);

    mGoogleApiClient = new GoogleApiClient
            .Builder(mActivity)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(AppIndex.API)
            .build();

    mOnContactLinkListener = new OpenContactLinkListener();

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
                        OperatorDetailsColumns.LOGO_URL
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
                        LocationsColumns.NAME,
                        LocationsColumns.CITY,
                        LocationsColumns.ZIPCODE,
                        LocationsColumns.STREET,
                        LocationsColumns.NUMBER,
                        LocationsColumns.DISTANCE,
                        LocationsColumns.OPERATOR_LOGO_URL
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
  public void onStop() {
    if (mGoogleApiClient.isConnected()) {
      final Uri APP_URI = BASE_URI.buildUpon().appendPath(mOperatorId).build();
      Action viewAction = Action.newAction(Action.TYPE_VIEW, mOperatorName, APP_URI);
      PendingResult<Status> result = AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);

      result.setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
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
  public void onResume() {
    Log.d(LOG_TAG, "paddingTop onReume " + contentDetail.getPaddingTop());
    super.onResume();
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    if (data != null && data.moveToFirst()) {
      switch (loader.getId()) {
        case DETAILS_LOADER_ID:
          description.setText(data.getString(data.getColumnIndex(OperatorDetailsColumns.DESCRIPTION)));
          spinner.setVisibility(View.GONE);
          mOperatorName = Html.fromHtml(data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_NAME))).toString();
          toolbar.setTitle(mOperatorName);
          toolbar.setSubtitle(Html.fromHtml(data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_OFFER))));
          Utility.setToolbarTitleFont(toolbar);
          appbarDetail.setVisibility(View.VISIBLE);

          Glide.with(this)
                  .load(data.getString(data.getColumnIndex(OperatorDetailsColumns.LOGO_URL)))
                  .into(logo);


          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final String operatorId = data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_ID));
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
              Log.d(LOG_TAG, fileList[i].getPath());
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

              // send screenview to analytics
              /*
              MainActivity.mTracker.setScreenName("Details~" +
                      data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_NAME)));
              MainActivity.mTracker.send(new HitBuilders.ScreenViewBuilder().build());
              */
            }

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

          Utility.setToolbarTitleFont(toolbar);
          contentDetail.setVisibility(View.VISIBLE);
          rvSchedule.setVisibility(View.VISIBLE);


          mActivity.setSupportActionBar(toolbar);

          mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

          mGoogleApiClient.connect();

          Log.d(LOG_TAG, "paddingTop onLoadFinished Loader 1 " + contentDetail.getPaddingTop());
          break;
        case SCHEDULE_LOADER_ID:
          mScheduleAdapter.swapCursor(data);
          Log.d(LOG_TAG, "paddingTop onLoadFinished Loader 2 " + contentDetail.getPaddingTop());
          mapView.getMapAsync(this);
          mScheduleCursor = data;
      }
    }
  }

  @Override
  public void onPause() {
    Log.d(LOG_TAG, "paddingTop onPause " + contentDetail.getPaddingTop());

    super.onPause();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mScheduleAdapter.swapCursor(null);
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {

    Log.d(LOG_TAG, "paddingTop onConnected " + contentDetail.getPaddingTop());

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
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    if (mScheduleCursor != null && mScheduleCursor.moveToFirst()) {
      double latitude = 0, longitude = 0;
      boolean isFirstLocation = true;
      do {
        double newLat = mScheduleCursor.getDouble(mScheduleCursor.getColumnIndex(LocationsColumns.LATITUDE));
        double newLong = mScheduleCursor.getDouble(mScheduleCursor.getColumnIndex(LocationsColumns.LONGITUDE));
        if (newLat != latitude || newLong != longitude) {
          latitude = newLat;
          longitude = newLong;
          Log.d(LOG_TAG, "latitude = " + latitude + " longitude = " + longitude);
          Marker marker = googleMap.addMarker(new MarkerOptions()
                  .position(new LatLng(latitude, longitude)));
          if (isFirstLocation) {
            final String logoUrl = mScheduleCursor.getString(mScheduleCursor.getColumnIndex(LocationsColumns.OPERATOR_LOGO_URL));
            Utility.loadMapMarkerIcon(mActivity, marker, logoUrl);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));

            final double finalLongitude = longitude;
            final double finalLatitude = latitude;
            mapOverlay.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                Intent mapIntent = new Intent(mActivity, MapActivity.class);
                mapIntent.putExtra(MapActivity.LONGITUDE_TAG, finalLongitude);
                mapIntent.putExtra(MapActivity.LATITUDE_TAG, finalLatitude);
                mapIntent.putExtra(MapActivity.LOGO_URL_EXTRA, logoUrl);

                mActivity.startActivity(mapIntent);
              }
            });
          }
          isFirstLocation = false;
          mapView.onResume();
        }
      } while (mScheduleCursor.moveToNext());
    }
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
