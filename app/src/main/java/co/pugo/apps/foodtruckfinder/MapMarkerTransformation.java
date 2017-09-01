package co.pugo.apps.foodtruckfinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

/**
 * Created by tobia on 31.8.2017.
 */
public class MapMarkerTransformation extends BitmapTransformation {

  private final Context mContext;
  private final int mColor;

  public MapMarkerTransformation(Context context, int color) {
    mContext = context;
    mColor = color;
  }

  @Override
  protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    return Utility.createMapMarker(mContext, toTransform, mColor);
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(MapMarkerTransformation.class.getSimpleName().getBytes());
  }
}
