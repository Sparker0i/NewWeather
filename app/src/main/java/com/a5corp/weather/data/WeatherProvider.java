package com.a5corp.weather.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

public class WeatherProvider extends ContentProvider {
    public static final int WEATHER = 100;
    public static final int WEATHER_WITH_LOCATION = 101;
    public static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    public static final int LOCATION = 300;
    public static final int LOCATION_ID = 301;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDbHelper mOpenHelper;
    private static final SQLiteQueryBuilder sWeatherByLocationStringQueryBuilder;

    static {
        sWeatherByLocationStringQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationStringQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME + " ON " +
                        WeatherContract.WeatherEntry.TABLE_NAME + "." +
                        WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " = " +
                        WeatherContract.LocationEntry.TABLE_NAME + "." +
                        WeatherContract.LocationEntry._ID);
    }

    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + " >= ? ";

    private static final String sLocationSettingWithDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";

    private Cursor getWeatherByLocationSetting (Uri uri , String[] projection , String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationFromUri(uri);
        String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[] {locationSetting};
        }
        else {
            selectionArgs = new String[] {locationSetting , startDate};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationStringQueryBuilder.query(mOpenHelper.getReadableDatabase() , projection , selection , selectionArgs , null , null , sortOrder);
    }

    private Cursor getWeatherByLocationSettingWithDate (Uri uri , String[] projection, String sortOrder) {
        String day = WeatherContract.WeatherEntry.getDateFromUri(uri);
        String locationSetting = WeatherContract.WeatherEntry.getLocationFromUri(uri);
        return sWeatherByLocationStringQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingWithDaySelection,
                new String[] {locationSetting, day},
                null,
                null,
                sortOrder
        );
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority , WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority , WeatherContract.PATH_WEATHER + "/*" , WEATHER_WITH_LOCATION);
        matcher.addURI(authority , WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority , WeatherContract.PATH_LOCATION , LOCATION);
        matcher.addURI(authority , WeatherContract.PATH_LOCATION + "/#" , LOCATION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case WEATHER_WITH_LOCATION_AND_DATE :   //  weather/*/*
            {
                retCursor = getWeatherByLocationSettingWithDate(uri, projection, sortOrder);
                break;
            }
            case WEATHER_WITH_LOCATION :            //  weather/*
            {
                retCursor = getWeatherByLocationSetting(uri , projection , sortOrder);
                break;
            }
            case WEATHER :                          //  weather
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case LOCATION_ID :                      //  location/*
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        WeatherContract.WeatherEntry._ID + " = " + ContentUris.parseId(uri),
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case LOCATION :                         //  location
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default :
                throw new UnsupportedOperationException("Unknown Uri : " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE :
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION :
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER :
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION :
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_ID :
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            default :
                throw new UnsupportedOperationException("Unknown Uri : " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int matcher = sUriMatcher.match(uri);
        Uri returnUri;

        switch (matcher) {
            case WEATHER :
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME , null, contentValues);
                if (_id > 0)
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new SQLException("Failed to insert row into : " + uri);
                break;
            case LOCATION:
                _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME , null, contentValues);
                if (_id > 0)
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new SQLException("Failed to insert row into : " + uri);
                break;
            default : throw new UnsupportedOperationException("Unknown Uri : " + uri);
        }
        getContext().getContentResolver().notifyChange(uri , null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int matcher = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (matcher) {
            case WEATHER :
                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION :
                rowsDeleted = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default : throw new UnsupportedOperationException("Unknown uri : " + uri);
        }
        if (selection == null || rowsDeleted != 0)
            getContext().getContentResolver().notifyChange(uri , null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int matcher = sUriMatcher.match(uri);
        int rowsUpdated;

        switch(matcher) {
            case WEATHER :
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME , contentValues , selection, selectionArgs);
                break;
            case LOCATION :
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME , contentValues , selection , selectionArgs);
                break;
            default : throw new UnsupportedOperationException("Unknown Uri : " + uri);
        }
        if (rowsUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
