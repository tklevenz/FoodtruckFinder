package co.pugo.apps.foodtruckfinder.data;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.SparseArray;

import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

import java.util.HashMap;

/**
 * Created by tobias on 1.9.2016.
 */
@Database(version = FoodtruckDatabase.VERSION)
public class FoodtruckDatabase {

  private static final SparseArray<String> MIGRATIONS = new SparseArray<>();

  private FoodtruckDatabase() {}

  public static final int VERSION = 21;

  @Table(LocationsColumns.class) public static final String LOCATIONS = "locations";
  @Table(OperatorDetailsColumns.class) public static final String OPERATOR_DETAILS = "operator_details";
  @Table(OperatorsColumns.class) public static final String OPERATORS = "operators";
  @Table(TagsColumns.class) public static final String TAGS = "tags";
  @Table(FavouritesColumns.class) public static final String FAVOURITES = "favourites";
  @Table(RegionsColumns.class) public static final String REGIONS = "regions";
  @Table(ImpressionsColumns.class) public static final String IMPRESSIONS = "impressions";


  @OnUpgrade
  public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    // add impressions table
    MIGRATIONS.append(15, "CREATE TABLE impressions ("
                         + ImpressionsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                         + ImpressionsColumns.ID + " TEXT NOT NULL,"
                         + ImpressionsColumns.IMPRESSION + " TEXT)"
    );

    MIGRATIONS.append(19, "DROP TABLE " + FoodtruckDatabase.LOCATIONS);

    MIGRATIONS.append(21, "CREATE TABLE locations ("
                          + LocationsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                          + LocationsColumns.LOCATION_ID + " TEXT NOT NULL,"
                          + LocationsColumns.OPERATOR_ID + " TEXT NOT NULL,"
                          + LocationsColumns.OPERATOR_NAME + " TEXT NOT NULL,"
                          + LocationsColumns.IMAGE_ID + " TEXT NOT NULL,"
                          + LocationsColumns.OPERATOR_OFFER + " TEXT,"
                          + LocationsColumns.OPERATOR_LOGO_URL + " TEXT,"
                          + LocationsColumns.OPERATOR_BACKGROUND + " TEXT,"
                          + LocationsColumns.LATITUDE + " TEXT NOT NULL,"
                          + LocationsColumns.LONGITUDE + " TEXT NOT NULL,"
                          + LocationsColumns.DISTANCE + " REAL,"
                          + LocationsColumns.START_DATE + " TEXT NOT NULL,"
                          + LocationsColumns.END_DATE + " TEXT NOT NULL,"
                          + LocationsColumns.LOCATION_NAME + " TEXT,"
                          + LocationsColumns.STREET + " TEXT,"
                          + LocationsColumns.NUMBER + " TEXT,"
                          + LocationsColumns.CITY + " TEXT,"
                          + LocationsColumns.ZIPCODE + " TEXT)");

    for(int i = oldVersion; i < newVersion; i++) {
      String migration = MIGRATIONS.get(i + 1);
      if (migration != null) {
        Log.d("DATABASE", "executing: " + migration);
        db.beginTransaction();
        try {
          db.execSQL(migration);
          db.setTransactionSuccessful();
        } catch(Exception e) {
          e.printStackTrace();
        } finally {
          db.endTransaction();
        }
      }
    }
  }
}
