package co.pugo.apps.foodtruckfinder.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by tobias on 4.9.2016.
 */
public class OperatorDetailsColumns {
  @DataType(DataType.Type.INTEGER) @PrimaryKey  @AutoIncrement
  public static final String _ID = "_id";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String OPERATOR_ID = "operator_id";
  @DataType(DataType.Type.TEXT)
  public static final String OPERATOR_NAME = "operator_name";
  @DataType(DataType.Type.TEXT)
  public static final String OPERATOR_OFFER = "operator_offer";
  @DataType(DataType.Type.TEXT)
  public static final String DESCRIPTION = "description";
  @DataType(DataType.Type.TEXT)
  public static final String DESCRIPTION_LONG = "description_long";
  @DataType(DataType.Type.TEXT)
  public static final String WEBSITE = "website";
  @DataType(DataType.Type.TEXT)
  public static final String WEBSITE_URL = "website_url";
  @DataType(DataType.Type.TEXT)
  public static final String FACEBOOK = "facebook";
  @DataType(DataType.Type.TEXT)
  public static final String FACEBOOK_URL = "facebook_url";
  @DataType(DataType.Type.TEXT)
  public static final String TWITTER = "twitter";
  @DataType(DataType.Type.TEXT)
  public static final String TWITTER_URL = "twitter_url";
  @DataType(DataType.Type.TEXT)
  public static final String EMAIL = "email";
  @DataType(DataType.Type.TEXT)
  public static final String PHONE = "phone";
  @DataType(DataType.Type.TEXT)
  public static final String LOGO_URL = "logo_url";
  @DataType(DataType.Type.TEXT)
  public static final String LOGO_BACKGROUND = "logo_background";
  @DataType(DataType.Type.INTEGER)
  public static final String PREMIUM = "premium";
  @DataType(DataType.Type.TEXT)
  public static final String REGION = "region";
}
