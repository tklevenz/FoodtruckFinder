package co.pugo.apps.foodtruckfinder.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by tobias on 1.9.2016.
 */
@Database(version = FoodtruckDatabase.VERSION)
public class FoodtruckDatabase {
  private FoodtruckDatabase() {}

  public static final int VERSION = 3;
  @Table(LocationsColumns.class) public static final String LOCATIONS = "locations";
  @Table(OperatorDetailsColumns.class) public static final String OPERATOR_DETAILS = "operator_details";
  @Table(OperatorsColumns.class) public static final String OPERATORS = "operators";
  @Table(TagsColumns.class) public static final String TAGS = "tags";
  @Table(FavouritesColumns.class) public static final String FAVOURITES = "favourites";
}
