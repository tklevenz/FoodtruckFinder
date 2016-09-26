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
    String OPERATOR_DETAILS = FoodtruckDatabase.OPERATOR_DETAILS;
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
            type = "vnd.android.cursor.dir/location"
    )
    public static final Uri CONTENT_URI = buildUri(Path.LOCATIONS);

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
}
