package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobias on 8.11.2016.
 *
 * parent class for items in MainActivity RecyclerView
 */

public abstract class FoodtruckListItem {
  public static final int TYPE_DIVIDER = 0;
  public static final int TYPE_FOODTRUCK = 1;

  abstract public int getType();
}
