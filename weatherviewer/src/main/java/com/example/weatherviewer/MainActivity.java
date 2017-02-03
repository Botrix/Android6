package com.example.weatherviewer;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<Weather> weatherList = new ArrayList<>();  // List of Weather objects representing the forecast
    private WeatherArrayAdapter weatherArrayAdapter;        // ArrayAdapter for binding Weather objects to a ListView
    private ListView weatherListView;                       // displays weather info

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // create ArrayAdapter to bind weatherList to the weatherListView
        weatherListView = (ListView) findViewById(R.id.weatherListView);
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);

        // configure FAB to hide keyboard and initiate web service request
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText locationEditText = (EditText) findViewById(R.id.locationEditText);
                URL url = createURL(locationEditText.getText().toString());
                // hide keyboard and initiate a GetWeatherTask to download
                // weather data from OpenWeatherMap.org in a separate thread
                if (url != null) {
                    dismissKeyboard(locationEditText);
                    GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
                    getLocalWeatherTask.execute(url);
                } else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            R.string.invalid_url, Snackbar.LENGTH_LONG).show();
                }
            }
        });

    }

    // create openweathermap.org web service URL using city
    private URL createURL(String city) {
        String apiKey = getString(R.string.api_key);
        String baseUrl = getString(R.string.web_service_url);
        try {
            // create URL for specified city and imperial units (Fahrenheit)
            String urlString = baseUrl + URLEncoder.encode(city, "UTF-8") +
                    "&units=imperial&cnt=16&APPID=" + apiKey;   // 281
            return new URL(urlString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // URL was malformed
    }

    // programmatically dismiss keyboard when user touches FAB
    private void dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * makes the REST web service call to get weather data and saves the data to a local HTML file
     * <p>
     * URL        for the variable-length parameter-list type of AsyncTask’s doInBackground
     * method ——— the URL of the web service request is passed as the only
     * argument to the GetWeatherTask’s execute method.
     * Void       for the variable-length parameter-list type for the onProgressUpdate
     * method ——— we do not use this method
     * JSONObject for the type of the task’s result, which is passed to onPostExecute
     * in the UI thread to display the results.
     */
    private class GetWeatherTask extends AsyncTask<URL, Void, JSONObject> {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected JSONObject doInBackground(URL... params) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) params[0].openConnection();
                int response = connection.getResponseCode();
                // the REST web service was invoked properly and there is a response to process
                if (response == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (Exception e) {
                        Snackbar.make(findViewById(R.id.coordinatorLayout),
                                R.string.read_error, Snackbar.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    // converts the JSON String in the StringBuilder to a JSONObject
                    // and return it to the UI thread
                    return new JSONObject(builder.toString());
                } else { // If there’s an error reading the weather data or connecting to the web service
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            R.string.connect_error, Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Snackbar.make(findViewById(R.id.coordinatorLayout),
                        R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                connection.disconnect(); // close the HttpURLConnection
            }
            return null;
        }

        // process JSON response and update ListView
        @Override
        protected void onPostExecute(JSONObject weather) {
            convertJSONtoArrayList(weather);            // repopulate weatherList
            weatherArrayAdapter.notifyDataSetChanged(); // rebind to ListView
            // reposition the ListView's first item to the top of the ListView
            // this ensures that the new weather forecast's first day is shown at the top
            weatherListView.smoothScrollToPosition(0);  // scroll to top
        }
    }

    // create Weather objects from JSONObject containing the forecast
    private void convertJSONtoArrayList(JSONObject forecast) {
        weatherList.clear(); // clear old weather data
        try {
            JSONArray list = forecast.getJSONArray("list"); // get forecast's "list" JSONArray
            // convert each element of list to a Weather object
            for (int i = 0; i < list.length(); i++) {
                JSONObject day = list.getJSONObject(i); // get one day's data
                // get the day's temperatures ("temp") JSONObject
                JSONObject temperatures = day.getJSONObject("temp");
                // get day's "weather" JSONObject for the description and icon
                JSONObject weather = day.getJSONArray("weather").getJSONObject(0);

                // add new Weather object to weatherList
                weatherList.add(new Weather(
                        day.getLong("dt"),                // date/time timestamp
                        temperatures.getDouble("min"),    // minimum temperature
                        temperatures.getDouble("max"),    // maximum temperature
                        day.getDouble("humidity"),        // percent humidity
                        weather.getString("description"), // weather conditions
                        weather.getString("icon")));      // icon name
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
