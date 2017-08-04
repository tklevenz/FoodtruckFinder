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

  public OperatorDetailsItem() {}

  public OperatorDetailsItem(String operatorName, String description, String webUrl, String email,
                             String phone, String facebook, String facebookUrl, String twitterUrl, String twitter) {
    this.operatorName = operatorName;
    this.description = description;
    this.webUrl = webUrl;
    this.email = email;
    this.phone = phone;
    this.facebook = facebook;
    this.facebookUrl =  facebookUrl;
    this.twitterUrl = twitterUrl;
    this.twitter = twitter;
  }

  @Override
  public int getType() {
    return DetailsItem.TYPE_OPERATOR_DETAILS;
  }
}
