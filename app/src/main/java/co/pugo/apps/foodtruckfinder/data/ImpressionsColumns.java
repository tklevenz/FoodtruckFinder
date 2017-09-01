package co.pugo.apps.foodtruckfinder.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by tobia on 31.8.2017.
 */

public class ImpressionsColumns {
  @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
  public static final String _ID = "_id";
  @DataType(DataType.Type.TEXT)
  public static final String ID = "id";
  @DataType(DataType.Type.TEXT)
  public static final String IMPRESSION = "impression";
}
