package com.a5corp.weather;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.a5corp.weather.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class FetchWeatherTask extends AsyncTask<String, Void, Void> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private Context mContext;
    private String locationQuery;
    private Vector<ContentValues> cVVector;

    FetchWeatherTask(Context context) {
        mContext = context;
    }

    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitType = sharedPrefs.getString(
                mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_default));
        if (!unitType.equals(mContext.getString(R.string.pref_units_default))) {
            high = 32 + high * 1.8;
            low = 32 + low * 1.8;
        }
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        final String OWM_LAT = "lat";
        final String OWM_LON = "lon";

        final String OWM_LIST = "list";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WIND_SPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DATETIME = "dt";
        final String OWM_ID = "id";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);

        JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = cityCoord.getDouble(OWM_LAT);
        double cityLongitude = cityCoord.getDouble(OWM_LON);

        long locationId = addLocation(cityName , locationQuery , cityLatitude, cityLongitude);
        cVVector = new Vector<>();
        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            JSONObject day = weatherArray.getJSONObject(i);
            double dt = day.getDouble(OWM_DATETIME);
            double pressure = day.getDouble(OWM_PRESSURE);
            double humidity = day.getDouble(OWM_HUMIDITY);
            String desc = day.getJSONArray(OWM_WEATHER).getJSONObject(0).getString(OWM_DESCRIPTION);
            double windSpeed = day.getDouble(OWM_WIND_SPEED);
            double windDirection = day.getDouble(OWM_WIND_DIRECTION);
            double max = day.getJSONObject(OWM_TEMPERATURE).getDouble(OWM_MAX);
            double min = day.getJSONObject(OWM_TEMPERATURE).getDouble(OWM_MIN);
            double weatherId = day.getJSONArray(OWM_WEATHER).getJSONObject(0).getDouble(OWM_ID);

            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY , locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT , WeatherContract.getDbDateString(new Date(Double.toString(dt))));
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE , pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY , humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, desc);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES , windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, max);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, min);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID , weatherId);

            cVVector.add(weatherValues);
        }
        if (cVVector.size() > 0) {
            ContentValues[] cVArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cVArray);
            mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI , cVArray);
        }
        Log.d(LOG_TAG , "Fetch Weather Task Complete , " + cVVector.size() + " elements added.");

        return resultStrs;
    }

    @Override
    protected Void doInBackground(String... params) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        locationQuery = params[0];
        if (params.length == 0)
            return null;
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

            /* Declaring Items Required to Build the URL */
        String format = "json";
        String units = "metric";
        int numDays = 10;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM , params[0])
                    .appendQueryParameter(FORMAT_PARAM , format)
                    .appendQueryParameter(UNITS_PARAM , units)
                    .appendQueryParameter(DAYS_PARAM , Integer.toString(numDays))
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.addRequestProperty("x-api-key" , mContext.getString(R.string.owm_id));
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
            Log.v(LOG_TAG , "Forecast JSON" + forecastJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }
        return null;
    }

    private long addLocation (String locationSetting , String cityName , double lat , double lon) {
        Log.v(LOG_TAG , "Inserting Location : " + cityName + " with coords " + lat + " , " + lon);

        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[] {WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ",
                new String[] {locationSetting},
                null
        );

        if (cursor.moveToFirst()) {
            Log.v(LOG_TAG , "Found location in the database");
            int locationIdIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            return cursor.getLong(locationIdIndex);
        }
        else {
            Log.v(LOG_TAG , "Didn't find in the database , adding now!");
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING , locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME , cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT , lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LON , lon);

            Uri locationInsertUri = mContext.getContentResolver().
                    insert(WeatherContract.LocationEntry.CONTENT_URI , locationValues);
        }
        return 0;
    }
}