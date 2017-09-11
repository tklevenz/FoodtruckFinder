package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobia on 1.8.2017.
 */

public abstract class DetailsItem {
  public static final int TYPE_MAPVIEW = 0;
  public static final int TYPE_OPERATOR_DETAILS = 1;
  public static final int TYPE_SCHEDULE_ITEM_DATE = 2;
  public static final int TYPE_SCHEDULE_ITEM_LOCATION = 3;
  public static final int TYPE_DIVIDER = 4;

  abstract public int getType();
}
