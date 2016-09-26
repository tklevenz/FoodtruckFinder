package co.pugo.apps.foodtruckfinder.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by tobias on 1.9.2016.
 */
public class LocationsColumns {
  @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
  public static final String _ID = "_id";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String OPERATOR_ID = "operator_id";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String OPERATOR_NAME = "operator_name";
  @DataType(DataType.Type.TEXT)
  public static final String OPERATOR_OFFER = "operator_offer";
  @DataType(DataType.Type.TEXT)
  public static final String OPERATOR_LOGO_URL = "operator_logo_url";
  @DataType(DataType.Type.REAL) @NotNull
  public static final String LATITUDE = "latitude";
  @DataType(DataType.Type.REAL) @NotNull
  public static final String LONGITUDE = "longitude";
  @DataType(DataType.Type.REAL) @NotNull
  public static final String DISTANCE = "distance";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String START_DATE = "start_date";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String END_DATE = "end_date";
  @DataType(DataType.Type.TEXT)
  public static final String NAME = "name";
  @DataType(DataType.Type.TEXT)
  public static final String STREET = "street";
  @DataType(DataType.Type.TEXT)
  public static final String NUMBER = "number";
  @DataType(DataType.Type.TEXT)
  public static final String CITY = "city";
  @DataType(DataType.Type.TEXT)
  public static final String ZIPCODE = "zipcode";
  @DataType(DataType.Type.INTEGER)
  public static final String YEARDAY = "date_long";
}
