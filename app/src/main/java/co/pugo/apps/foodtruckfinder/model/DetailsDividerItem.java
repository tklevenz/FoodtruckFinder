package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobia on 1.8.2017.
 */

public class DetailsDividerItem extends DetailsItem {
  public Integer Color;
  @Override
  public int getType() {
    return DetailsItem.TYPE_DIVIDER;
  }
}
