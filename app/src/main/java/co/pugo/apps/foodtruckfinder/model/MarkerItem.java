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
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutionException;

import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.Utility;

/**
 * Created by tobias on 17.1.2017.
 */

public class MarkerItem implements ClusterItem {
  private LatLng mPosition;
  private Context mContext;
  public String title;
  public String snippet;
  public int color;
  public boolean onTop;
  public String operatorId;
  public String imageId;
  public String logoUrl;
  public Bitmap logo;


  public MarkerItem(Context context, double lat, double lng, String snippet, String title, String operatorId,
                    String imageId, int color, boolean onTop, final String logoUrl) {
    mContext = context;
    mPosition = new LatLng(lat, lng);
    this.title = title;
    this.color = color;
    this.snippet = snippet;
    this.onTop = onTop;
    this.operatorId = operatorId;
    this.imageId = imageId;
    this.logoUrl = logoUrl;

    File file = context.getFilesDir();
    File[] files = file.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.equals(logoUrl.substring(logoUrl.lastIndexOf("/")+1));
      }
    });

    if (files.length > 0) {
      logo = BitmapFactory.decodeFile(files[0].getPath());
    }
  }

  @Override
  public LatLng getPosition() {
    return mPosition;
  }

  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public String getSnippet() {
    return null;
  }
}
