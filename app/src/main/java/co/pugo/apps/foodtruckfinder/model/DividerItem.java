package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobias on 17.1.2017.
 */
public class DividerItem extends FoodtruckListItem {

  public String date;

  public DividerItem(String date) {
    this.date = date;
  }

  @Override
  public int getType() {
    return TYPE_HEADER;
  }
}
