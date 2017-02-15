package co.pugo.apps.foodtruckfinder.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;

/**
 * Created by tobias on 17.1.2017.
 */

public class MarkerItem implements ClusterItem {
  private LatLng mPosition;
  public String title;
  public String snippet;
  public int color;
  public boolean onTop;
  public String logoUrl;

  public MarkerItem(double lat, double lng, String snippet, String title, String logoUrl, int color, boolean onTop) {
    mPosition = new LatLng(lat, lng);
    this.title = title;
    this.color = color;
    this.snippet = snippet;
    this.onTop = onTop;
    this.logoUrl = logoUrl;
  }

  @Override
  public LatLng getPosition() {
    return mPosition;
  }
}
