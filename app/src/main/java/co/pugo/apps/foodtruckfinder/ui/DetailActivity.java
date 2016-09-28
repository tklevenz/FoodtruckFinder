package co.pugo.apps.foodtruckfinder.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.util.Util;
import com.google.android.gms.analytics.HitBuilders;

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

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String LOG_TAG = "DetailActivity";
  public static final String LOGO_URL_TAG = "logo_url";
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

  private static final int DETAILS_LOADER_ID = 0;
  private static final int SCHEDULE_LOADER_ID = 1;
  private String mOperatorId;
  private String mLogoUrl;
  private ScheduleAdapter mScheduleAdapter;
  private String mWebUrl;
  private String mEmail;
  private String mPhone;
  private String mFacebookUrl;
  private String mTwitterUrl;
  private String mTwitter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detail);
    ButterKnife.bind(this);

    setSupportActionBar(toolbar);

    mOperatorId = getIntent().getStringExtra(FoodtruckIntentService.OPERATORID_TAG);
    mLogoUrl = getIntent().getStringExtra(LOGO_URL_TAG);

    //scheduleHeader.setTypeface(MainActivity.mRobotoSlab);
    mScheduleAdapter = new ScheduleAdapter(this);
    rvSchedule.setAdapter(mScheduleAdapter);
    rvSchedule.setNestedScrollingEnabled(false);
    rvSchedule.setLayoutManager(new LinearLayoutManager(this));



    getLoaderManager().initLoader(DETAILS_LOADER_ID, null, this);
    getLoaderManager().initLoader(SCHEDULE_LOADER_ID, null, this);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case DETAILS_LOADER_ID:
        return new CursorLoader(this,
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
                        OperatorDetailsColumns.PHONE
                },
                null,
                null,
                null
        );
      case SCHEDULE_LOADER_ID:
        return new CursorLoader(this,
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
                        LocationsColumns.DISTANCE
                },
                null,
                null,
                LocationsColumns.DISTANCE + " ASC," + LocationsColumns.START_DATE + " ASC"
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
          containerDescription.setVisibility(View.VISIBLE);
          description.setText(data.getString(data.getColumnIndex(OperatorDetailsColumns.DESCRIPTION)));
          spinner.setVisibility(View.GONE);
          toolbar.setTitle(Html.fromHtml(data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_NAME))));
          toolbar.setSubtitle(Html.fromHtml(data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_OFFER))));
          Utility.setToolbarTitleFont(toolbar);
          appbarDetail.setVisibility(View.VISIBLE);

          Glide.with(this)
                  .load(mLogoUrl)
                  .into(logo);


          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final String operatorId = data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_ID));
            File file = getFilesDir();
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
              backgroundProtection.setVisibility(View.VISIBLE);

              // send screenview to analytics
              MainActivity.mTracker.setScreenName("Details~" +
                      data.getString(data.getColumnIndex(OperatorDetailsColumns.OPERATOR_NAME)));
              MainActivity.mTracker.send(new HitBuilders.ScreenViewBuilder().build());
            }

            // setup contact views
            String webUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.WEBSITE_URL));
            if (webUrl.length() > 0) {
              webTexView.setVisibility(View.VISIBLE);
              webTexView.setText(data.getString(data.getColumnIndex(OperatorDetailsColumns.WEBSITE)));
              mWebUrl = webUrl;
            }
            String email = data.getString(data.getColumnIndex(OperatorDetailsColumns.EMAIL));
            if (email.length() > 0) {
              emailTextView.setVisibility(View.VISIBLE);
              emailTextView.setText(email);
              mEmail = email;
            }
            String phone = data.getString(data.getColumnIndex(OperatorDetailsColumns.PHONE));
            if (phone.length() > 0) {
              phoneTextView.setVisibility(View.VISIBLE);
              phoneTextView.setText(phone);
              mPhone = phone;
            }
            String facebookUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.FACEBOOK_URL));
            if (facebookUrl.length() > 0) {
              faceboookTextView.setVisibility(View.VISIBLE);
              faceboookTextView.setText(data.getString(data.getColumnIndex(OperatorDetailsColumns.FACEBOOK)));
              mFacebookUrl = facebookUrl;
            }
            String twitter = data.getString(data.getColumnIndex(OperatorDetailsColumns.TWITTER));
            if (twitter.length() > 0) {
              twitterTextView.setVisibility(View.VISIBLE);
              twitterTextView.setText(twitter.startsWith("@") ? twitter : "@" + twitter);
              mTwitter = twitter;
              mTwitterUrl = data.getString(data.getColumnIndex(OperatorDetailsColumns.TWITTER_URL));
            }

          }

          Utility.setToolbarTitleFont(toolbar);
          detailsDivider.setVisibility(View.VISIBLE);
          rvSchedule.setVisibility(View.VISIBLE);
          break;
        case SCHEDULE_LOADER_ID:
          mScheduleAdapter.swapCursor(data);
      }
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    //mScheduleAdapter.setGroupCursor(null);
  }

  public void openContactLink(View view) {
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
          getPackageManager().getApplicationInfo("com.facebook.katana", 0);
          uri = Uri.parse("fb://facewebmodal/f?href=" + mFacebookUrl);
        } catch (PackageManager.NameNotFoundException e) {
          uri = Uri.parse(mFacebookUrl);
        }
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
        break;
      case R.id.contact_twitter:
        try {
          getPackageManager().getApplicationInfo("com.twitter.android", 0);
          uri = Uri.parse("twitter://user?user_id=" + mTwitter);
        } catch (PackageManager.NameNotFoundException e) {
          uri = Uri.parse(mTwitterUrl);
        }
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }
  }
}
