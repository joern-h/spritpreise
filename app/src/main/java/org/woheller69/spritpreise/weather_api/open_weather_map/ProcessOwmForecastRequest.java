package org.woheller69.spritpreise.weather_api.open_weather_map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.preference.PreferenceManager;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.woheller69.spritpreise.R;
import org.woheller69.spritpreise.activities.NavigationActivity;
import org.woheller69.spritpreise.database.CityToWatch;
import org.woheller69.spritpreise.database.Forecast;
import org.woheller69.spritpreise.database.PFASQLiteHelper;
import org.woheller69.spritpreise.database.WeekForecast;
import org.woheller69.spritpreise.ui.updater.ViewUpdater;
import org.woheller69.spritpreise.weather_api.IDataExtractor;
import org.woheller69.spritpreise.weather_api.IProcessHttpRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * This class processes the HTTP requests that are made to the OpenWeatherMap API requesting the
 * current weather for all stored cities.
 */
public class ProcessOwmForecastRequest implements IProcessHttpRequest {

    /**
     * Constants
     */
    private final String DEBUG_TAG = "process_forecast";

    /**
     * Member variables
     */
    private Context context;
    private PFASQLiteHelper dbHelper;

    /**
     * Constructor.
     *
     * @param context The context of the HTTP request.
     */
    public ProcessOwmForecastRequest(Context context) {
        this.context = context;
        this.dbHelper = PFASQLiteHelper.getInstance(context);
    }

    /**
     * Converts the response to JSON and updates the database. Note that for this method no
     * UI-related operations are performed.
     *
     * @param response The response of the HTTP request.
     */
    @Override
    public void processSuccessScenario(String response) {
        IDataExtractor extractor = new OwmDataExtractor();
        try {
            JSONObject json = new JSONObject(response);
            JSONArray list = json.getJSONArray("list");
            JSONObject jsoncity = json.getJSONObject("city");
            JSONObject coord = jsoncity.getJSONObject("coord");
            float lat = (float)coord.getDouble("lat");
            float lon = (float)coord.getDouble("lon");
             //          Log.d("URL JSON",Float.toString(lat));
             //          Log.d("URL JSON",Float.toString(lon));

            ArrayList<Integer> CityIDList = new ArrayList<Integer>();

            int cityId=0;
            //find CityID from lat/lon
            List<CityToWatch> citiesToWatch = dbHelper.getAllCitiesToWatch();
            for (int i = 0; i < citiesToWatch.size(); i++) {
                CityToWatch city = citiesToWatch.get(i);
                //if lat/lon of json response very close to lat/lon in citytowatch
                //OpenWeatherMaps rounds to 2 decimal places, so the response lat/lon should differ by <=0.005
                if ((Math.abs(city.getLatitude() - lat)<=0.005) && (Math.abs(city.getLongitude() - lon)<=0.005)) {
                    cityId=city.getCityId();
                    CityIDList.add(cityId);
                }
            }
            for (int c=0; c<CityIDList.size();c++) {
                cityId = CityIDList.get(c);
                List<Forecast> forecasts = new ArrayList<>();

                SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);



                    dbHelper.deleteForecastsByCityId(cityId); //start with empty forecast list


                // Continue with inserting new records

                for (int i = 0; i < list.length(); i++) {
                    String currentItem = list.get(i).toString();
                    Forecast forecast = extractor.extractForecast(currentItem);
                    // Data were not well-formed, abort
                    if (forecast == null) {
                        final String ERROR_MSG = context.getResources().getString(R.string.error_convert_to_json);
                        if (NavigationActivity.isVisible)
                            Toast.makeText(context, ERROR_MSG, Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Could retrieve all data, so proceed
                    else {
                            forecast.setCity_id(cityId);
                            // add it to the database
                            dbHelper.addForecast(forecast);
                            forecasts.add(forecast);
                    }
                }

                ViewUpdater.updateForecasts(forecasts);
                //again update Weekforecasts (new forecasts might change some rain weather symbols, see CityWeatherAdapter checkSun() )

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows an error that the data could not be retrieved.
     *
     * @param error The error that occurred while executing the HTTP request.
     */
    @Override
    public void processFailScenario(final VolleyError error) {
        Handler h = new Handler(this.context.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                if (NavigationActivity.isVisible) Toast.makeText(context, context.getResources().getString(R.string.error_fetch_forecast), Toast.LENGTH_LONG).show();
            }
        });
    }

}
