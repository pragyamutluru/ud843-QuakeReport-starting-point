/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.quakereport;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EarthquakeActivity extends AppCompatActivity {
    private EarthquakeAdapter earthquakeAdapter;
    public static final String LOG_TAG = EarthquakeActivity.class.getName();
    private ArrayList<Earthquake> earthquakes;
    private RecyclerView earthquakeRecyclerView;
    private static final String SERVE_URL="https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&eventtype=earthquake&orderby=time&minmag=6&limit=10";

    private class DownloadTask extends AsyncTask<String, Void, List<Earthquake>>{
        @Override
        protected List<Earthquake> doInBackground(String... urls) {
            URL url= null;
            try {
                url = createURL(urls[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            String jsonResponse="";
            jsonResponse=makeHttpRequest(url);
            List<Earthquake> result= extractFromJson(jsonResponse);
            return result;
        }

        private URL createURL(String stringURL) throws MalformedURLException {
            URL url= new URL(stringURL);
            return url;
        }

        private String makeHttpRequest(URL url){
            String jsonResponse="";
            if(url==null){
                return jsonResponse;
            }
            HttpURLConnection urlConnection=null;
            InputStream inputStream=null;

            try {
                urlConnection=(HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                if(urlConnection.getResponseCode()==200){

                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);}
                else{
                    Log.i("RES_CODE",Integer.toString(urlConnection.getResponseCode()));
                }





            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return jsonResponse;

        }
        private String readFromStream(InputStream inputStream) throws IOException {
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
        private List<Earthquake> extractFromJson(String earthquakeJSON) {
            if(TextUtils.isEmpty(earthquakeJSON)){
                return null;
            }

            try {
                JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);
                JSONArray featureArray = baseJsonResponse.getJSONArray("features");
                List<Earthquake> earthquakes= new ArrayList<>();
                // If there are results in the features array
                for(int i=0; i<featureArray.length(); i++){

                    // Extract out the first feature (which is an earthquake)
                    JSONObject feature = featureArray.getJSONObject(i);
                    JSONObject properties = feature.getJSONObject("properties");

                    // Extract out the title, time, and tsunami values
                    String place = properties.getString("place");
                    Log.i("THISISATEST", place);
                    Double magnitude = properties.getDouble("mag");
                    Long date = properties.getLong("time");
                    String url=properties.getString("url");

                    // Create a new {@link Event} object
                    Earthquake tempE= new Earthquake();
                    tempE.setUrl(url);
                    tempE.setPlace(place);
                    tempE.setDate(date);
                    tempE.setMagnitude(magnitude);
                    earthquakes.add(tempE);
                }
                return earthquakes;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }
    }

    //colors
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.earthquake_activity);

        // Create a fake list of earthquake locations.
        DownloadTask downloadTask= new DownloadTask();
        try {
            earthquakes=(ArrayList<Earthquake>)downloadTask.execute(SERVE_URL).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


        // Find a reference to the {@link ListView} in the layout
        earthquakeRecyclerView =(RecyclerView)findViewById(R.id.earthquake_recycler_view);
        earthquakeRecyclerView.setLayoutManager(new LinearLayoutManager(EarthquakeActivity.this));
        // Create a new {@link ArrayAdapter} of earthquakes
         earthquakeAdapter= new EarthquakeAdapter(earthquakes);


        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        earthquakeRecyclerView.setAdapter(earthquakeAdapter);
        earthquakeRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(EarthquakeActivity.this, earthquakeRecyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        // do whatever
                        int itemPosition = position;
                        String item = earthquakes.get(itemPosition).getUrl();
                        if(item!=null){
                            openWebpage(item);
                        }

                    }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );






    }

    private void openWebpage(String item) {
        Intent implicit = new Intent(Intent.ACTION_VIEW, Uri.parse(item));
        startActivity(implicit);
    }

    class EarthquakeAdapter extends RecyclerView.Adapter<EarthquakeHolder>{
        private List<Earthquake> mEarthquakes;
        public EarthquakeAdapter(List<Earthquake> earthquakes){
            mEarthquakes=earthquakes;
        }




        @Override
        public EarthquakeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater= LayoutInflater.from(EarthquakeActivity.this);

            return new EarthquakeHolder(layoutInflater,parent);

            }

        @Override
        public void onBindViewHolder(EarthquakeHolder holder, int position) {

            Earthquake earthquake=mEarthquakes.get(position);
            holder.bind(earthquake);

        }

        @Override
        public int getItemCount() {
            return mEarthquakes.size();
        }
    }
//ViewHolder Class to hold the object in the Recycler View
    private class EarthquakeHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView mPlaceTextView;
        private TextView mDateTextView;
        private TextView mMagTextView;
        private TextView timeView;
        private Earthquake mEarthquake;
        private  TextView mPlaceNearTextView;

        public EarthquakeHolder(LayoutInflater inflater, ViewGroup parent){
            super(inflater.inflate(R.layout.list_item, parent, false));
            mPlaceNearTextView=itemView.findViewById(R.id.location_offset);
            mPlaceTextView=(TextView) itemView.findViewById(R.id.primary_location);
            timeView=itemView.findViewById(R.id.time);
            mDateTextView=(TextView) itemView.findViewById(R.id.date);
            mMagTextView=(TextView) itemView.findViewById(R.id.magnitude);

        }

        public void bind(Earthquake earthquake){
            mEarthquake=earthquake;
            Date dateObject = new Date(mEarthquake.getDate());
            String dateToDisplay = formatDate(dateObject);
            String formattedTime = formatTime(dateObject);
            String place=formatPlace(mEarthquake.getPlace());
            String placeNear=formatPlaceNear(mEarthquake.getPlace());
            DecimalFormat formatter = new DecimalFormat("0.00");
            // Set the proper background color on the magnitude circle.
            // Fetch the background from the TextView, which is a GradientDrawable.
            GradientDrawable magnitudeCircle = (GradientDrawable) mMagTextView.getBackground();

            // Get the appropriate background color based on the current earthquake magnitude
            int magnitudeColor = getMagnitudeColor(mEarthquake.getMagnitude());

            // Set the color on the magnitude circle
            magnitudeCircle.setColor(magnitudeColor);

            String magString = formatter.format(mEarthquake.getMagnitude());

            // Display the time of the current earthquake in that TextView
            timeView.setText(formattedTime);
            mPlaceTextView.setText(place);
            mPlaceNearTextView.setText(placeNear);

            mMagTextView.setText(magString);

            mDateTextView.setText(dateToDisplay);
        }

        @Override
        public void onClick(View view) {

        }
    }

    private int getMagnitudeColor(Double magnitude) {
         int magnitude1color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude1);

         int magnitude2color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude2);

        int magnitude3color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude3);

         int magnitude4color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude4);

         int magnitude5color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude5);

         int magnitude6color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude6);

         int magnitude7color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude7);

         int magnitude8color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude8);

         int magnitude9color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude9);

         int magnitude10color=ContextCompat.getColor(EarthquakeActivity.this, R.color.magnitude10plus);




        switch (magnitude.intValue()){

            case 0:;
            case 1:
                    return magnitude1color;
            case 2:
                return magnitude2color;
            case 3:
                return magnitude3color;
            case 4:
                return magnitude4color;
            case 5:

                return magnitude5color;
            case 6:
                return magnitude6color;
            case 7:
                return magnitude7color;
            case 8:
                return magnitude8color;
            case 9:
                return magnitude9color;
            default:
                return magnitude10color;

        }
    }

    private String formatDate(Date dateObject) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("LLL dd, yyyy");
        return dateFormat.format(dateObject);
    }

    /**
     * Return the formatted date string (i.e. "4:30 PM") from a Date object.
     */
    private String formatTime(Date dateObject) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
        return timeFormat.format(dateObject);
    }

    private String formatPlace(String place){
        int index= place.indexOf("of");
        if(index==-1){
            return place;
        }
        else{
            return place.substring(index+3,place.length());
        }
    }

    private String formatPlaceNear(String place){
        int index= place.indexOf("of");
        if(index==-1){
            return "near";
        }
        else{
            return place.substring(0,index+2);
        }
    }

}
