package co.pugo.apps.foodtruckfinder.model;


/**
 * Created by tobia on 1.8.2017.
 */

public class ScheduleItemLocation extends DetailsItem {

  public String location;
  public String street;
  public String city;
  public String distance;

  public ScheduleItemLocation(String location, String street, String city, String distance) {
    this.location = location;
    this.street = street;
    this.city = city;
    this.distance = distance;
  }

  @Override
  public int getType() {
    return DetailsItem.TYPE_SCHEDULE_ITEM_LOCATION;
  }
}