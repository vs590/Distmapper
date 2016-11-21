package com.example.vyom.distmapper;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends FragmentActivity {

    GoogleMap map;
    ArrayList<LatLng> markerPoints;
    TextView dist,dur;

    Button find,reset;
    EditText source,destination;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reset=(Button)findViewById(R.id.button2);
        source=(EditText)findViewById(R.id.editText2);

        // can be added after layout inflation; it doesn't have to be fixed

        // value


        destination=(EditText)findViewById(R.id.editText);


        dist=(TextView)findViewById(R.id.dist);
        dur=(TextView)findViewById(R.id.dur);
        find=(Button)findViewById(R.id.button);
        //tvDistanceDuration = (TextView) findViewById(R.id.tv_distance_time);
       // String text=tvDistanceDuration.getText().toString();
        // Initializing
        markerPoints = new ArrayList<LatLng>();

        // Getting reference to SupportMapFragment of the activity_main
        SupportMapFragment fm = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);

        // Getting Map for the SupportMapFragment
        map = fm.getMap();

        // Enable MyLocation Button in the Map
        map.setMyLocationEnabled(true);
        map.setTrafficEnabled(true);

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            int c=0;
            @Override

            public void onMapLongClick(LatLng latLng) {
               c++;
                if(c%2!=0)
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                else
                 map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });

        // Setting onclick event listener for the map



            find.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(find.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    map.setOnMapClickListener(null);
                    map.clear();
                    markerPoints.clear();

                    String address1 = source.getText().toString();
                    String address2 = destination.getText().toString();

                    if (!(address1.equals("")) && !(address2.equals(""))) {
                        GeocodingLocation locationAddress1 = new GeocodingLocation();
                        locationAddress1.getAddressFromLocation(address1, address2,
                                getApplicationContext(), new GeocoderHandler());


                    } else {
                        Toast.makeText(getApplicationContext(), "enter both location", Toast.LENGTH_LONG).show();
                    }


                }
            });
            reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(source.getText().equals("") && destination.getText().equals("")) {

                        Toast.makeText(getApplicationContext(),"NOTHING TO RESET",Toast.LENGTH_LONG).show();
                    }
                    else {
                        map.clear();
                        markerPoints.clear();
                        dist.setText("0 Miles");
                        dur.setText("0 Minutes");
                        source.setText("");
                        destination.setText("");


                    }
                }
            });



            map.setOnMapClickListener(new OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {
                    find.setOnClickListener(null);
                    // Already two locations
                    if (markerPoints.size() > 1) {
                        markerPoints.clear();
                        map.clear();
                    }

                    // Adding new item to the ArrayList

                    markerPoints.add(point);

                    // Creating MarkerOptions
                    MarkerOptions options = new MarkerOptions();

                    // Setting the position of the marker
                    options.position(point);

                    /**
                     * For the start location, the color of marker is GREEN and
                     * for the end location, the color of marker is RED.
                     */
                    if (markerPoints.size() == 1) {
                        options.position(point).title("Source");
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    } else if (markerPoints.size() == 2) {
                       options.position(point).title("Destination");
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                    }


                    // Add new marker to the Google Map Android API V2
                    map.addMarker(options);

                    // Checks, whether start and end locations are captured
                    if (markerPoints.size() >= 2) {
                        LatLng origin = markerPoints.get(0);
                        LatLng dest = markerPoints.get(1);

                        // Getting URL to the Google Directions API
                        String url = getDirectionsUrl(origin, dest);

                        DownloadTask downloadTask = new DownloadTask();

                        // Start downloading json data from Google Directions API
                        downloadTask.execute(url);
                    }

                }
            });
        }


    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;


        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }



    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";



            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }


            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    if(j==0){    // Get distance from the list
                        distance = (String)point.get("distance");


                        continue;
                    }else if(j==1){ // Get duration from the list
                        duration = (String)point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(4);
                lineOptions.color(Color.BLUE);

            }


            //tvDistanceDuration.setText("Distance:"+distance+" miles"+ "\nDuration:"+duration);
            dist.setText(" "+distance + " Miles");
            dur.setText(" "+duration);
            // Drawing polyline in the Google Map for the i-th route
            map.addPolyline(lineOptions);
        }
    }
    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            Double lat, lon,lat2,lon2;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            //source.setText(locationAddress);
            if(!(locationAddress.equals("Unable to get Latitude and Longitude."))) {
                locationAddress.trim();
                lat = Double.parseDouble(locationAddress.substring(0, locationAddress.indexOf(",") - 1));
                lon = Double.parseDouble(locationAddress.substring(locationAddress.indexOf(",") + 2, locationAddress.indexOf("N") - 1));
                lat2 = Double.parseDouble(locationAddress.substring(locationAddress.indexOf("N") + 2, locationAddress.indexOf("O") - 1));
                lon2 = Double.parseDouble(locationAddress.substring(locationAddress.indexOf("O") + 2, locationAddress.length()));
                System.out.print(lat + " " + lon + " " + lat2 + " " + lon2);
                // String mess=lat + " " + lon+" "+lat2+" "+lon2;
                //Toast.makeText(getApplicationContext(),mess,Toast.LENGTH_LONG).show();
                LatLng sourcemark = new LatLng(lat, lon);
                map.addMarker(new MarkerOptions().position(sourcemark).title(source.getText().toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                markerPoints.add(sourcemark);
                CameraUpdate zoom = CameraUpdateFactory.newLatLngZoom(sourcemark, 13);
                map.animateCamera(zoom);
                LatLng destmark = new LatLng(lat2, lon2);
                map.addMarker(new MarkerOptions().position(destmark).title(destination.getText().toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                markerPoints.add(destmark);
                LatLng origin = markerPoints.get(0);
                LatLng dest = markerPoints.get(1);

                // Getting URL to the Google Directions API
                String url = getDirectionsUrl(origin, dest);

                DownloadTask downloadTask = new DownloadTask();

                // Start downloading json data from Google Directions API
                downloadTask.execute(url);


                Log.println(1, "markerpoint", "" + markerPoints.size());
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Unable To Fetch From Server",Toast.LENGTH_LONG).show();
            }



        }
    }















}
