package co.pugo.apps.foodtruckfinder.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by tobias on 10.10.2016.
 */

public class OperatorsColumns {
  @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
  public static final String _ID = "_id";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String ID = "id";
  @DataType(DataType.Type.TEXT)
  public static final String NAME = "name";
  @DataType(DataType.Type.TEXT)
  public static final String OFFER = "offer";
  @DataType(DataType.Type.TEXT)
  public static final String LOGO_URL = "logo_url";
  @DataType(DataType.Type.TEXT)
  public static final String LOGO_BACKGROUND = "logo_background";
}
