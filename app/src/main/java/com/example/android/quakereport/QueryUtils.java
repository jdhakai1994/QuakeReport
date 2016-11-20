package com.example.android.quakereport;

/**
 * Created by Jayabrata Dhakai on 11/19/2016.
 */

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods related to requesting and receiving earthquake data from USGS.
 */
public final class QueryUtils {

    public static final String LOG_TAG = QueryUtils.class.getName();

    /**
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    /**
     * Converts the url received to a list of {@link Earthquake} objects
     *
     * @param requestUrl is the url passed from doInBackground() in EarthquakeAsyncTask class
     * @return earthquakeList a list of {@link Earthquake} objects
     */
    public static List<Earthquake> fetchEarthquakeData(String requestUrl) {

        //create an URL object from the requestUrl string
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error closing input stream", e);
        }

        // Extract relevant fields from the JSON response and create a list of Earthquake object
        List<Earthquake> earthquakeList = extractFeatureFromJson(jsonResponse);

        // Return the list of earthquake objects
        return earthquakeList;
    }

    /**
     * Converts a String to {@link URL} object
     *
     * @param requestUrl is the query to be used
     * @return url a {@link URL} object
     */
    private static URL createUrl(String requestUrl) {
        URL url = null;
        try {
            url = new URL(requestUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error with creating URL ", e);
        }
        return url;
    }

    /**
     * Takes a {@link URL} object as input and returns the json response from the USGS server
     *
     * @param url is the corresponding URL object of the query
     * @return jsonResponse is the String which is obtained as response from USGS
     * @throws IOException
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the whole JSON response from the server.
     *
     * @param inputStream is stream obtained from USGS server
     * @return output is the String format of the inputStream
     * @throws IOException
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Parses the jsonResponse to list of {@link Earthquake} objects
     *
     * @param jsonResponse contains the response returned while hitting the USGS query
     * @return earthquakeList a list of {@link Earthquake} objects
     */
    private static List<Earthquake> extractFeatureFromJson(String jsonResponse) {

        // Create an empty ArrayList that we can start adding earthquakes to
        List<Earthquake> earthquakeList = new ArrayList<>();

        // build up a list of Earthquake objects with the corresponding data.
        try {

            JSONObject rootJsonResponse = new JSONObject(jsonResponse);

            JSONArray featuresArray = rootJsonResponse.getJSONArray("features");

            for (int i = 0; i < featuresArray.length(); i++) {
                JSONObject currentFeature = featuresArray.getJSONObject(i);
                JSONObject properties = currentFeature.getJSONObject("properties");

                double magnitude = properties.getDouble("mag");
                String location = properties.getString("place");
                long time = properties.getLong("time");
                String url = properties.getString("url");

                earthquakeList.add(new Earthquake(magnitude, location, time, url));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
        }

        // Return the list of earthquake objects
        return earthquakeList;
    }
}