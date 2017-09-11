package co.pugo.apps.foodtruckfinder.model;

/**
 * Created by tobia on 1.8.2017.
 */

public class OperatorDetailsItem extends DetailsItem {

  public String operatorName;
  public String description;
  public String webUrl;
  public String email;
  public String phone;
  public String facebook;
  public String facebookUrl;
  public String twitterUrl;
  public String twitter;
  public boolean premium;

  @Override
  public int getType() {
    return DetailsItem.TYPE_OPERATOR_DETAILS;
  }
}
