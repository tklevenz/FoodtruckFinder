package co.pugo.apps.foodtruckfinder.data;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Created by tobias on 1.9.2016.
 */
@ContentProvider(authority = FoodtruckProvider.AUTHORITY, database = FoodtruckDatabase.class)
public class FoodtruckProvider {
  public static final String AUTHORITY = "co.pugo.apps.foodtruckfinder.data.FoodtruckProvider";
  static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  interface Path {
    String LOCATIONS = FoodtruckDatabase.LOCATIONS;
    String LOCATIONS_TODAY = FoodtruckDatabase.LOCATIONS + "_today";
    String LOCATIONS_OPERATORS = FoodtruckDatabase.LOCATIONS + "_operators";
    String OPERATOR_DETAILS = FoodtruckDatabase.OPERATOR_DETAILS;
    String OPERATORS = FoodtruckDatabase.OPERATORS;
    String OPERATORS_TODAY = FoodtruckDatabase.OPERATORS + "_today";
    String OPERATORS_WEEK = FoodtruckDatabase.OPERATORS + "_week";
    String TAGS = FoodtruckDatabase.TAGS;
    String LOCATIONS_TAGS = "location_tags";
    String FAVOURITES = FoodtruckDatabase.FAVOURITES;
  }

  private static Uri buildUri(String... paths) {
    Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
    for (String path : paths)
      builder.appendPath(path);
    return builder.build();
  }

  @TableEndpoint(table = FoodtruckDatabase.LOCATIONS)
  public static class Locations {
    @ContentUri(
            path = Path.LOCATIONS,
            type = "vnd.android.cursor.dir/location",
            groupBy = LocationsColumns.OPERATOR_ID
    )
    public static final Uri CONTENT_URI = buildUri(Path.LOCATIONS);

    @ContentUri(
            path = Path.LOCATIONS_TODAY,
            type = "vnd.android.cursor.dir/location",
            where = "date(" + LocationsColumns.START_DATE + ") = date('now')",
            groupBy = LocationsColumns.OPERATOR_ID
    )
    public static final Uri CONTENT_URI_TODAY = buildUri(Path.LOCATIONS_TODAY);

    @ContentUri(
            path = Path.LOCATIONS_OPERATORS,
            type = "vnd.android.cursor.dir/location_operators",
            join = "join " + FoodtruckDatabase.OPERATORS + " on " + LocationsColumns.OPERATOR_ID + " = " + OperatorsColumns.ID,
            groupBy = LocationsColumns.OPERATOR_ID + "," + FoodtruckDatabase.LOCATIONS + "." + LocationsColumns._ID
    )
    public static final Uri CONTENT_URI_JOIN_OPERATORS = buildUri(Path.LOCATIONS_OPERATORS);

    @InexactContentUri(
            name = "LOCATION_ID",
            path = Path.LOCATIONS + "/*",
            type = "vnd.android.cursor.item/location",
            whereColumn = LocationsColumns.OPERATOR_ID,
            pathSegment = 1
    )
    public static Uri withOperatorId(String operatorid) {
      return buildUri(Path.LOCATIONS, operatorid);
    }

    @ContentUri(
            path = Path.LOCATIONS_TAGS,
            type = "vnd.android.cursor.dir/location_tag",
            join = "join " + FoodtruckDatabase.TAGS + " on " + LocationsColumns.OPERATOR_ID + " = " + TagsColumns.ID,
            groupBy = LocationsColumns.OPERATOR_ID
    )
    public static final Uri CONTENT_URI_JOIN_TAGS = buildUri(Path.LOCATIONS_TAGS);
  }

  @TableEndpoint(table = FoodtruckDatabase.OPERATOR_DETAILS)
  public static class OperatorDetails {
    @ContentUri(
            path = Path.OPERATOR_DETAILS,
            type = "vnd.android.cursor.item/dir"
    )
    public static final Uri CONTENT_URI = buildUri(Path.OPERATOR_DETAILS);

    @InexactContentUri(
            name = "OPERATOR_DETAILS_ID",
            path = Path.OPERATOR_DETAILS + "/*",
            type = "vnd.android.cursor.item/operatorDetails",
            whereColumn = LocationsColumns.OPERATOR_ID,
            pathSegment = 1
    )
    public static Uri withOperatorId(String operatorId) {
      return buildUri(Path.OPERATOR_DETAILS, operatorId);
    }
  }

  @TableEndpoint(table = FoodtruckDatabase.OPERATORS)
  public static class Operators {
    private static final String JOIN_LOCATIONS = "left outer join " + FoodtruckDatabase.LOCATIONS + " on " +
            FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID + " = " + LocationsColumns.OPERATOR_ID;
    private static final String JOIN_TAGS = "join " + FoodtruckDatabase.TAGS + " on " +
            FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID + " = " + FoodtruckDatabase.TAGS + "." + TagsColumns.ID;
    private static final String JOIN_FAVOURITES = "join " + FoodtruckDatabase.FAVOURITES + " on " +
            FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID + " = " + FoodtruckDatabase.FAVOURITES + "." + FavouritesColumns.ID;

    @ContentUri(
            path = Path.OPERATORS,
            type = "vnd.android.cursor.item/dir",
            join = JOIN_LOCATIONS + " " + JOIN_TAGS,
            groupBy = FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID

    )
    public static final Uri CONTENT_URI = buildUri(Path.OPERATORS);

    @ContentUri(
            path = Path.OPERATORS + "_fav",
            type = "vnd.android.cursor.item/dir",
            join = JOIN_LOCATIONS + " " + JOIN_TAGS + " " + JOIN_FAVOURITES,
            where = FavouritesColumns.FAVOURITE + " = 1",
            groupBy = FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID

    )
    public static final Uri CONTENT_URI_FAVOURITES = buildUri(Path.OPERATORS + "_fav");

    @ContentUri(
            path = Path.OPERATORS_TODAY,
            type = "vnd.android.cursor.item/dir",
            join = JOIN_LOCATIONS + " " + JOIN_TAGS,
            where = LocationsColumns.OPERATOR_ID + " is not null AND date(" + LocationsColumns.START_DATE + ") = date('now')",
            groupBy = FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID
    )
    public static final Uri CONTENT_URI_TODAY = buildUri(Path.OPERATORS_TODAY);

    @ContentUri(
            path = Path.OPERATORS_TODAY + "_fav",
            type = "vnd.android.cursor.item/dir",
            join = JOIN_LOCATIONS + " " + JOIN_TAGS + " " + JOIN_FAVOURITES,
            where = LocationsColumns.OPERATOR_ID + " is not null AND date(" + LocationsColumns.START_DATE + ") = date('now') AND " + FavouritesColumns.FAVOURITE + " = 1",
            groupBy = FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID
    )
    public static final Uri CONTENT_URI_TODAY_FAVOURITES = buildUri(Path.OPERATORS_TODAY + "_fav");

    @ContentUri(
            path = Path.OPERATORS_WEEK,
            type = "vnd.android.cursor.item/dir",
            join = JOIN_LOCATIONS + " " + JOIN_TAGS,
            where = LocationsColumns.OPERATOR_ID + " is not null",
            groupBy = FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID
    )
    public static final Uri CONTENT_URI_WEEK = buildUri(Path.OPERATORS_WEEK);

    @ContentUri(
            path = Path.OPERATORS_WEEK + "_fav",
            type = "vnd.android.cursor.item/dir",
            join = JOIN_LOCATIONS + " " + JOIN_TAGS + " " + JOIN_FAVOURITES,
            where = LocationsColumns.OPERATOR_ID + " is not null AND " + FavouritesColumns.FAVOURITE + " = 1",
            groupBy = FoodtruckDatabase.OPERATORS + "." + OperatorsColumns.ID
    )
    public static final Uri CONTENT_URI_WEEK_FAVOURITES = buildUri(Path.OPERATORS_WEEK + "_fav");
  }

  @TableEndpoint(table = FoodtruckDatabase.TAGS)
  public static class Tags {
    @ContentUri(
            path = Path.TAGS,
            type = "vnd.android.cursor.item/dir",
            groupBy = TagsColumns.TAG
    )
    public static final Uri CONTENT_URI = buildUri(Path.TAGS);
  }

  @TableEndpoint(table = FoodtruckDatabase.FAVOURITES)
  public static class Favourites {
    @ContentUri(
            path = Path.FAVOURITES,
            type = "vnd.android.cursor.item/dir"
    )
    public static final Uri CONTENT_URI = buildUri(Path.FAVOURITES);
  }
}
