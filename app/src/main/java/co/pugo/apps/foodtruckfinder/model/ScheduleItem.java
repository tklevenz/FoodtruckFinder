package co.pugo.apps.foodtruckfinder.model;


/**
 * Created by tobia on 1.8.2017.
 */

public class ScheduleItem extends DetailsItem {

  public String date;
  public String location;
  public String street;
  public String city;
  public String time;
  public String distance;

  public ScheduleItem(String date, String location, String street, String city, String time, String distance) {
    this.date =  date;
    this.location = location;
    this.street = street;
    this.city = city;
    this.time = time;
    this.distance = distance;
  }

  @Override
  public int getType() {
    return DetailsItem.TYPE_SCHEDULE_ITEM;
  }
}
