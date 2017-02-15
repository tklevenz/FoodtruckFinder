package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobias on 17.1.2017.
 */
public class FoodtruckItem extends FoodtruckListItem {

  public String operatorId;
  public String name;
  public String offer;
  public String logoUrl;
  public float distance;
  public String location;
  public String region;

  public FoodtruckItem(String operatorId, String name, String offer, String logoUrl, float distance, String location, String region) {
    this.operatorId = operatorId;
    this.name = name;
    this.offer = offer;
    this.logoUrl = logoUrl;
    this.distance = distance;
    this.location = location;
    this.region = region;
  }

  @Override
  public int getType() {
    return TYPE_FOODTRUCK;
  }
}
