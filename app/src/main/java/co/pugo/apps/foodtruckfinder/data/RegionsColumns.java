package co.pugo.apps.foodtruckfinder.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Unique;

/**
 * Created by tobias on 1.12.2016.
 */

public class RegionsColumns {
  @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
  public static final String _ID = "_id";
  @DataType(DataType.Type.TEXT) @NotNull @Unique
  public static final String ID = "id";
  @DataType(DataType.Type.TEXT)
  public static final String NAME = "name";
  @DataType(DataType.Type.TEXT)
  public static final String LATITUDE = "latitude";
  @DataType(DataType.Type.TEXT)
  public static final String LONGITUDE = "longitude";
  @DataType(DataType.Type.REAL)
  public static final String DISTANCE_APROX = "distance_aprox";
}
