package com.a5corp.weather.test;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.a5corp.weather.data.WeatherContract.LocationEntry;
import com.a5corp.weather.data.WeatherContract.WeatherEntry;
import com.a5corp.weather.data.WeatherDbHelper;

import java.util.Map;
import java.util.Set;

public class TestProvider extends AndroidTestCase {
    private String LOG_TAG = TestProvider.class.getSimpleName();

    public void testDeleteDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
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
    public String TEST_LOCATION = "99705";
    public String TEST_DATE = "20141205";

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

    public void testInsertReadProvider() {
        ContentValues values = getLocationValues();
        Uri locationUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI , values);

        long locationRowId = ContentUris.parseId(locationUri);
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG , "New Row ID : " + locationRowId);

        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.buildLocationUri(locationRowId),
                null,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            validateCursor(getLocationValues(), cursor);
        }
        else {
            fail("No Weather Data Returned");
        }
        cursor.close();

        Uri weatherUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, getWeatherValues(locationRowId));
        long weatherRowId = ContentUris.parseId(weatherUri);
        assertTrue(weatherRowId != -1);
        Log.d(LOG_TAG , "New Row ID : " + locationRowId);
        Cursor weatherCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocation(TEST_LOCATION),
                null,
                null,
                null,
                null);
        if (weatherCursor.moveToFirst()) {
            validateCursor(getWeatherValues(locationRowId) , weatherCursor);
        }
        else {
            fail("No values Returned");
        }
        weatherCursor.close();

        weatherCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocationWithStartDate(TEST_LOCATION , TEST_DATE),
                null,
                null,
                null,
                null);
        if (weatherCursor.moveToFirst()) {
            validateCursor(getWeatherValues(locationRowId) , weatherCursor);
        }
        else {
            fail("No values Returned");
        }
        weatherCursor.close();

        weatherCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocationWithDate(TEST_LOCATION , TEST_DATE),
                null,
                null,
                null,
                null);
        if (weatherCursor.moveToFirst()) {
            validateCursor(getWeatherValues(locationRowId) , weatherCursor);
        }
        else {
            fail("No values Returned");
        }
        weatherCursor.close();

        Uri insertUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, getWeatherValues(locationRowId));
        weatherRowId = ContentUris.parseId(insertUri);

        weatherCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocation(TEST_LOCATION),
                null,
                null,
                null,
                null);
        if (weatherCursor.moveToFirst()) {
            validateCursor(getWeatherValues(locationRowId) , weatherCursor);
        }
        else {
            fail("No values Returned");
        }
        weatherCursor.close();
    }

    public void testGetType() {
        String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "94074";
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals(WeatherEntry.CONTENT_TYPE , type);

        String testDate = "20140612";
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocationWithDate(testLocation , testDate));
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE , type);

        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        assertEquals(LocationEntry.CONTENT_TYPE , type);

        type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(1L));
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);
    }

    public void testDeleteAllRecords() {
        mContext.getContentResolver().delete(
                WeatherEntry.CONTENT_URI,
                null,
                null
        );

        mContext.getContentResolver().delete(
                LocationEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        assertEquals(cursor.getCount(), 0);
        cursor.close();

        cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        assertEquals(cursor.getCount(), 0);
        cursor.close();
    }
}
