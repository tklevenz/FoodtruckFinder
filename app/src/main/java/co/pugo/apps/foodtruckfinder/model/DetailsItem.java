package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobia on 1.8.2017.
 */

public abstract class DetailsItem {
  public static final int TYPE_MAPVIEW = 0;
  public static final int TYPE_OPERATOR_DETAILS = 1;
  public static final int TYPE_SCHEDULE_ITEM = 2;
  public static final int TYPE_DIVIDER = 3;

  abstract public int getType();
}
