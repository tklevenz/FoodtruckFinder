package co.pugo.apps.foodtruckfinder.adapter;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;
import co.pugo.apps.foodtruckfinder.model.DetailsItem;
import co.pugo.apps.foodtruckfinder.model.MapItem;
import co.pugo.apps.foodtruckfinder.model.OperatorDetailsItem;
import co.pugo.apps.foodtruckfinder.model.ScheduleItem;
import co.pugo.apps.foodtruckfinder.ui.MapActivity;

/**
 * Created by tobia on 1.8.2017.
 */

public class DetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private final Typeface mRobotoSlab;
  private Context mContext;
  private List<DetailsItem> mListItems;
  private MapItem mMapItem;
  private View mMapOverlay;

  public DetailsAdapter(Context context, List<DetailsItem> items) {
    mContext = context;
    mListItems = items;
    mRobotoSlab = Typeface.createFromAsset(context.getAssets(), "RobotoSlab-Regular.ttf");
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (parent instanceof RecyclerView) {
      switch (viewType) {
        case DetailsItem.TYPE_DIVIDER:
          return new DividerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_details_divider, parent, false));
        case DetailsItem.TYPE_MAPVIEW:
          return new MapViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_map_view, parent, false));
        case DetailsItem.TYPE_OPERATOR_DETAILS:
          return new OperatorDetailsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_operator_details, parent, false));
        case DetailsItem.TYPE_SCHEDULE_ITEM:
          return new ScheduleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false));
        default:
          return null;
      }
    } else {
      throw new RuntimeException("Not bound to RecyclerView");
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    Log.d("DetailsAdapter", "onBindViewHolder " + getItemViewType(position));
    switch (getItemViewType(position)) {
      case DetailsItem.TYPE_MAPVIEW:
        mMapItem = (MapItem) mListItems.get(position);
        MapViewHolder mapViewHolder = (MapViewHolder) holder;
        mapViewHolder.mapLogoOverlay.setImageDrawable(new BitmapDrawable(mContext.getResources(), mMapItem.logo));
        mMapOverlay = mapViewHolder.mapOverlay;
        GoogleMapOptions options = new GoogleMapOptions();
        LatLng latLng = (mMapItem.latitude != null && mMapItem.longitude != null) ?
                new LatLng(mMapItem.latitude, mMapItem.longitude) :
                Utility.getLatLngFromRegion(mContext, mMapItem.region);
        options.camera(CameraPosition.builder()
                .target(latLng)
                .zoom(15)
                .build());
        options.liteMode(true);
        MapView mapView = new MapView(mContext, options);
        mapViewHolder.mapViewContainer.addView(mapView);
        mapViewHolder.mapView = mapView;
        // initialize map view
        mapViewHolder.initMapView();
        mapViewHolder.mapOverlay.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent mapIntent = new Intent(mContext, MapActivity.class);
            mapIntent.putExtra(MapActivity.LONGITUDE_TAG, mMapItem.longitude);
            mapIntent.putExtra(MapActivity.LATITUDE_TAG, mMapItem.latitude);
            mapIntent.putExtra(MapActivity.LOCATION_ID, mMapItem.locationId);
            mapIntent.putExtra(MapActivity.DATE_RANGE, mMapItem.dateRange);
            mContext.startActivity(mapIntent);
          }
        });

        break;
      case DetailsItem.TYPE_OPERATOR_DETAILS:
        OperatorDetailsItem operatorDetailsItem = (OperatorDetailsItem) mListItems.get(position);
        OperatorDetailsViewHolder operatorDetailsViewHolder = (OperatorDetailsViewHolder) holder;
        operatorDetailsViewHolder.operatorName.setText(operatorDetailsItem.operatorName);
        operatorDetailsViewHolder.operatorName.setTypeface(mRobotoSlab);

        operatorDetailsViewHolder.description.setText(operatorDetailsItem.description);
        operatorDetailsViewHolder.description.setVisibility(View.VISIBLE);

        if (operatorDetailsItem.webUrl != null && operatorDetailsItem.webUrl.length() > 0) {
          operatorDetailsViewHolder.webTexView.setText(operatorDetailsItem.webUrl);
          operatorDetailsViewHolder.webTexView.setVisibility(View.VISIBLE);
          operatorDetailsViewHolder.webTexView.setOnClickListener(new OpenContactLinkListener(operatorDetailsItem.webUrl));
        }
        if (operatorDetailsItem.email != null && operatorDetailsItem.email.length() > 0) {
          operatorDetailsViewHolder.emailTextView.setText(operatorDetailsItem.email);
          operatorDetailsViewHolder.emailTextView.setVisibility(View.VISIBLE);
          operatorDetailsViewHolder.emailTextView.setOnClickListener(new OpenContactLinkListener(operatorDetailsItem.email));
        }
        if (operatorDetailsItem.phone != null && operatorDetailsItem.phone.length() > 0) {
          operatorDetailsViewHolder.phoneTextView.setText(operatorDetailsItem.phone);
          operatorDetailsViewHolder.phoneTextView.setVisibility(View.VISIBLE);
          operatorDetailsViewHolder.phoneTextView.setOnClickListener(new OpenContactLinkListener(operatorDetailsItem.phone));
        }
        if (operatorDetailsItem.facebookUrl != null && operatorDetailsItem.facebookUrl.length() > 0) {
          operatorDetailsViewHolder.faceboookTextView.setText(operatorDetailsItem.facebook);
          operatorDetailsViewHolder.faceboookTextView.setVisibility(View.VISIBLE);
          operatorDetailsViewHolder.faceboookTextView.setOnClickListener(new OpenContactLinkListener(operatorDetailsItem.facebookUrl));
        }
        if (operatorDetailsItem.twitterUrl != null && operatorDetailsItem.twitterUrl.length() > 0) {
          operatorDetailsViewHolder.twitterTextView.setText(operatorDetailsItem.twitter);
          operatorDetailsViewHolder.twitterTextView.setVisibility(View.VISIBLE);
          operatorDetailsViewHolder.twitterTextView.setOnClickListener(new OpenContactLinkListener(operatorDetailsItem.twitterUrl));
        }
        break;
      case DetailsItem.TYPE_SCHEDULE_ITEM:
        ScheduleItem scheduleItem = (ScheduleItem) mListItems.get(position);
        ScheduleViewHolder scheduleViewHolder = (ScheduleViewHolder) holder;
        scheduleViewHolder.scheduleDate.setText(scheduleItem.date);
        scheduleViewHolder.scheduleTime.setText(scheduleItem.time);
        scheduleViewHolder.scheduleLocationName.setText(scheduleItem.location);
        scheduleViewHolder.scheduleLocationCity.setText(scheduleItem.city);
        scheduleViewHolder.scheduleLocationStreet.setText(scheduleItem.street);
        scheduleViewHolder.scheduleDistance.setText(scheduleItem.distance);

        scheduleViewHolder.locationContainer.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            // launch maps with address
          }
        });
    }
  }

  @Override
  public int getItemViewType(int position) {
    return mListItems.get(position).getType();
  }

  @Override
  public int getItemCount() {
    if (null == mListItems) return 0;
    return mListItems.size();
  }

  class DividerViewHolder extends RecyclerView.ViewHolder {
    public DividerViewHolder(View itemView) {
      super(itemView);
    }
  }

  class MapViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback {
    @BindView(R.id.map_snapshot) ImageView mapSnapshot;
    @BindView(R.id.map_overlay) View mapOverlay;
    @BindView(R.id.map_logo_overlay) ImageView mapLogoOverlay;
    @BindView(R.id.map_view_container) FrameLayout mapViewContainer;

    MapView mapView;

    public MapViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void initMapView() {
      mapView.onCreate(null);
      mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
      MapsInitializer.initialize(mContext);

      googleMap.getUiSettings().setMapToolbarEnabled(false);

      if (mMapItem.latitude == null || mMapItem.longitude == null) {

        // remove overlay to disable on click for trucks that are not on the road
        mMapOverlay.setVisibility(View.GONE);

        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mContext, R.raw.map_style_silver));

        Glide.with(mContext)
                .load(mMapItem.logoUrl)
                .asBitmap()
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new SimpleTarget<Bitmap>(300, 300) {
                  @Override
                  public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    resource = Utility.addDropShadow(resource, Color.GRAY, 10, 0, 2);
                    mapLogoOverlay.setImageDrawable(new BitmapDrawable(mContext.getResources(), resource));
                  }
                });
      }
    }
  }

  class OperatorDetailsViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.textView_operator) TextView operatorName;
    @BindView(R.id.textView_description) TextView description;
    @BindView(R.id.contact_web) TextView webTexView;
    @BindView(R.id.contact_email) TextView emailTextView;
    @BindView(R.id.contact_phone) TextView phoneTextView;
    @BindView(R.id.contact_facebook) TextView faceboookTextView;
    @BindView(R.id.contact_twitter) TextView twitterTextView;

    public OperatorDetailsViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  class ScheduleViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.schedule_date) TextView scheduleDate;
    @BindView(R.id.schedule_location_name) TextView scheduleLocationName;
    @BindView(R.id.schedule_location_street) TextView scheduleLocationStreet;
    @BindView(R.id.schedule_location_city) TextView scheduleLocationCity;
    @BindView(R.id.schedule_time) TextView scheduleTime;
    @BindView(R.id.schedule_distance) TextView scheduleDistance;
    @BindView(R.id.schedule_location_container) View locationContainer;
    @BindView(R.id.schedule_date_container) View dateContainer;

    public ScheduleViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }


  private class OpenContactLinkListener implements View.OnClickListener {
    private String link;

    public OpenContactLinkListener(String link) {
      this.link = link;
    }

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
            openLink(view, link);
          }

          @Override
          public void onAnimationCancel(Animator animator) {
          }

          @Override
          public void onAnimationRepeat(Animator animator) {
          }
        });
      } else {
        openLink(view, link);
      }
    }

    private void openLink(View view, String link) {
      Uri uri;
      switch (view.getId()) {
        case R.id.contact_web:
          mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
          break;
        case R.id.contact_email:
          mContext.startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + link)));
          break;
        case R.id.contact_phone:
          mContext.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + link)));
          break;
        case R.id.contact_facebook:
          try {
            mContext.getPackageManager().getApplicationInfo("com.facebook.katana", 0);
            uri = Uri.parse("fb://facewebmodal/f?href=" + link);
          } catch (PackageManager.NameNotFoundException e) {
            uri = Uri.parse(link);
          }
          mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
          break;
        case R.id.contact_twitter:
          try {
            mContext.getPackageManager().getApplicationInfo("com.twitter.android", 0);
            uri = Uri.parse("twitter://user?user_id=" + link);
          } catch (PackageManager.NameNotFoundException e) {
            uri = Uri.parse(link);
          }
          mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
      }
    }
  }
}
