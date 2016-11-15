package co.pugo.apps.foodtruckfinder.adapter;

/**
 * Created by tobias on 8.11.2016.
 *
 * http://stackoverflow.com/questions/34848401/divide-elements-on-groups-in-recyclerview
 */

public abstract class FoodtruckListItem {
  public static final int TYPE_HEADER = 0;
  public static final int TYPE_FOODTRUCK = 1;

  abstract public int getType();
}
