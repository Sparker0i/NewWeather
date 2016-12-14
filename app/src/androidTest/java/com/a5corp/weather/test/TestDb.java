package com.a5corp.weather.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.a5corp.weather.data.WeatherContract.LocationEntry;
import com.a5corp.weather.data.WeatherContract.WeatherEntry;
import com.a5corp.weather.data.WeatherDbHelper;

import java.util.Map;
import java.util.Set;

public class TestDb extends AndroidTestCase {
    private String LOG_TAG = TestDb.class.getSimpleName();

    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public static void validateCursor(ContentValues expectedValues , Cursor valueCursor) {
        Set<Map.Entry<String , Object>> valueSet = expectedValues.valueSet();

        for (Map.Entry<String , Object> entry : valueSet) {
            String columnName = entry.getKey();
            int index = valueCursor.getColumnIndex(columnName);
            assertTrue(index != -1);

            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue , valueCursor.getString(index));
        }
    }

    public String TEST_CITY_NAME = "North Pole";

    public ContentValues getLocationValues() {
        String locationTestSetting = "99705";
        double testLatitude = 64.772;
        double testLongitude = -147.355;

        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_CITY_NAME , TEST_CITY_NAME);
        values.put(LocationEntry.COLUMN_LOCATION_SETTING , locationTestSetting);
        values.put(LocationEntry.COLUMN_COORD_LAT , testLatitude);
        values.put(LocationEntry.COLUMN_COORD_LON , testLongitude);
        return values;
    }

    public static ContentValues getWeatherValues(long locationRowId) {
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY , locationRowId);
        weatherValues.put(WeatherEntry.COLUMN_DATETEXT , "20141205");
        weatherValues.put(WeatherEntry.COLUMN_DEGREES , 1.1);
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY , 1.2);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE , 1.3);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP , 75);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP , 65);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC , "Asteroids");
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED , 5.5);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID , 321);
        return weatherValues;
    }

    public void testInsertReadDb() {


        WeatherDbHelper dbHelper = new WeatherDbHelper(this.mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();


        long locationRowId = db.insert(LocationEntry.TABLE_NAME , null, getLocationValues());

        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG , "New Row ID : " + locationRowId);

        Cursor cursor = db.query(
                LocationEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            validateCursor(getLocationValues() , cursor);
            long weatherRowId = db.insert(WeatherEntry.TABLE_NAME , null, getWeatherValues(locationRowId));

            assertTrue(weatherRowId != -1);
            Log.d(LOG_TAG , "New Row ID : " + locationRowId);

            Cursor weatherCursor = db.query(
                    WeatherEntry.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            if (weatherCursor.moveToFirst()) {
                validateCursor(getWeatherValues(locationRowId) , weatherCursor);
            }
        }
        else {
            fail("No values Returned");
        }
    }
}
