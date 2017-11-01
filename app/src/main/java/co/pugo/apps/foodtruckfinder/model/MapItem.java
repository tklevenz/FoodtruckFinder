package co.pugo.apps.foodtruckfinder.model;

import android.graphics.Bitmap;

/**
 * Created by tobia on 1.8.2017.
 */

public class MapItem extends DetailsItem {

  public Double latitude;
  public Double longitude;
  public String region;
  public int locationId;
  public int dateRange;
  public String logoUrl;
  public int markerColor;

  @Override
  public int getType() {
    return DetailsItem.TYPE_MAPVIEW;
  }
}
