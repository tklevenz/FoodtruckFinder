package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobia on 11.9.2017.
 */

public class ScheduleItemDate extends DetailsItem {
  public String date;
  public String time;

  public ScheduleItemDate(String date, String time) {
    this.date = date;
    this.time = time;
  }

  @Override
  public int getType() {
    return DetailsItem.TYPE_SCHEDULE_ITEM_DATE;
  }
}