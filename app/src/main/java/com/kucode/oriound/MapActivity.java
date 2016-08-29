package com.kucode.oriound;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapActivity extends ActionBarActivity implements SensorEventListener, LocationListener {
    // - VARIABLES -
    private MapView map;
    private LocationManager locationManager;
    private DirectedLocationOverlay currentLocationOverlay;
    private AddressResultReceiver addressResultReceiver;
    private RelativeLayout progressBarWrapper;
    private LinearLayout buttonsWrapper;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private ShakeManager shakeManager;

    public static final String TAG = "MapActivity";

    // - DATA VARIABLES -
    protected Location mCurrentLocation;
    protected String destinationAddress;
    protected boolean fetchAddressRunning = false;
    protected boolean mNavigationRunning = false;
    protected boolean mNavigationMode;
    private Road mRoad;
    private RoadNode mCurrentNode;
    private RoadNode mNextNode;
    private int mNodeIndex;
    private long mLastTime = 0;

    // - OnCreate, OnResume, OnPause -
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity_layout);

        buttonsWrapper = (LinearLayout)findViewById(R.id.mapButtonsWrapper);
        progressBarWrapper = (RelativeLayout)findViewById(R.id.progressBarWrapper);

        // Checking if map is called with destination address
        String inDestinationAddress = getIntent().getStringExtra("destination_address");
        if (inDestinationAddress != null && !inDestinationAddress.isEmpty()) {
            mNavigationMode = true;
            destinationAddress = inDestinationAddress;

            // Hiding favourite paths button
            Button button = (Button)findViewById(R.id.favourite_paths_button);
            button.setVisibility(View.GONE);

            // Setting progress bar to active until route is found
            progressBarWrapper.setVisibility(View.VISIBLE);
        }

        Typeface tf = Typeface.createFromAsset(getAssets(), Constants.FONT_PATH);
        ((Button)findViewById(R.id.current_location_button)).setTypeface(tf);
        ((Button)findViewById(R.id.current_instruction_button)).setTypeface(tf);
        ((Button)findViewById(R.id.bus_poi_button)).setTypeface(tf);
        ((Button)findViewById(R.id.favourite_paths_button)).setTypeface(tf);

        // Setting volume control stream to media
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Initializing components
        initializeMap();

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeManager = new ShakeManager();
        shakeManager.setListener(new ShakeManager.ShakeListener() {

            @Override
            public void onShake() {
                buttonsWrapper.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        boolean providerEnabled = startLocationUpdates();
        currentLocationOverlay.setEnabled(providerEnabled);
        sensorManager.registerListener(shakeManager, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override protected void onPause() {
        sensorManager.unregisterListener(shakeManager);
        locationManager.removeUpdates(this);
        super.onPause();
    }

    // - Initializing map and it's components -
    private void initializeMap() {
        // - Map -
        map = (MapView) this.findViewById(R.id.map);
        //map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(16);
        // Tiles for the map, can be changed
        map.setTileSource(TileSourceFactory.MAPNIK);

        // - Map controller -
        IMapController mapController = map.getController();
        mapController.setZoom(19);

        // - Map overlays -
        CustomResourceProxy crp = new CustomResourceProxy(getApplicationContext());
        currentLocationOverlay = new DirectedLocationOverlay(this, crp);

        map.getOverlays().add(currentLocationOverlay);

        // - Location -
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
            onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }
        else if (locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
            onLocationChanged(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
        }
        else {
            currentLocationOverlay.setEnabled(false);
        }
    }

    // - LOCATION -
    boolean startLocationUpdates() {
        boolean providerExists = false;
        for (String locationProvider : locationManager.getProviders(true)) {
            locationManager.requestLocationUpdates(locationProvider, 2*1000, 0.0f, this);
            providerExists = true;
        }
        return providerExists;
    }

    // - Location changed listener -
    private final NetworkLocationIgnorer networkLocationIgnorer = new NetworkLocationIgnorer();
    @Override public void onLocationChanged(Location location) {
        // Animate to first location change
        if (mCurrentLocation == null) {
            map.getController().animateTo(new GeoPoint(location));
        }

        mCurrentLocation = location;

        long currentTime = System.currentTimeMillis();

        if (networkLocationIgnorer.shouldIgnore(location.getProvider(), currentTime))
            return;

        double dT = currentTime - mLastTime;
        if (dT < 200.0){
            return;
        };

        mLastTime = currentTime;

        if (!currentLocationOverlay.isEnabled()){
            currentLocationOverlay.setEnabled(true);
        }

        currentLocationOverlay.setLocation(new GeoPoint(location));
        currentLocationOverlay.setBearing(location.getBearing());
        currentLocationOverlay.setAccuracy((int) location.getAccuracy());

        // Navigation mode
        if (mNavigationMode) {
            if(!mNavigationRunning) {
                // Location for the first time -> get road
                mNavigationRunning = true;

                ArrayList<GeoPoint> wayPoints = new ArrayList<GeoPoint>();
                // Start location
                wayPoints.add(new GeoPoint(location));
                // Destination location
                wayPoints.add(getLocationFromAddress(destinationAddress));

                new RoadTask().execute(wayPoints);
            }
            else if (mNextNode != null) {
                GeoPoint currentGeoPoint = new GeoPoint(mCurrentLocation);
                int distance = currentGeoPoint.distanceTo(mNextNode.mLocation);
                boolean readInstructions = false;

                // TODO: Test which distance is good for marking node as reached
                // TODO: Preveri kako zaznati ali je šel čez node
                // TODO: Povej navodila tik preden zavije
                if ( distance < 4 ) {
                    map.getController().animateTo(new GeoPoint(location));

                    // Node reached, increase node index
                    mNodeIndex++;

                    if (mNodeIndex >= mRoad.mNodes.size()) {
                        // Last node reached - turn navigation off
                        mNavigationMode = false;
                        mNavigationRunning = false;
                        mNextNode = null;
                        mCurrentNode = null;
                        mNodeIndex = 0;

                        Button button = (Button)findViewById(R.id.current_instruction_button);
                        button.setVisibility(View.GONE);

                        Button buttonFavPaths = (Button)findViewById(R.id.favourite_paths_button);
                        buttonFavPaths.setVisibility(View.VISIBLE);
                    }
                    else {
                        // Get next node
                        mCurrentNode = mNextNode;
                        mNextNode = mRoad.mNodes.get(mNodeIndex);

                        readInstructions = true;
                    }
                }
                else if (distance < 6) {
                    readInstructions = true;
                }

                // Set new instruction
                setInstruction(readInstructions);
            }
        }

        map.invalidate();
    }

    // - Navigation instructions -
    public void setInstruction (boolean readInstructions) {
        Button button = (Button)findViewById(R.id.current_instruction_button);

        GeoPoint currentGeoPoint = new GeoPoint(mCurrentLocation);
        int distance = currentGeoPoint.distanceTo(mNextNode.mLocation);

        if (mNodeIndex >= mRoad.mNodes.size()) {
            readInstructions("Prispeli ste na cilj");
        }
        else  {
            String instruction = "";

            if (mNodeIndex == mRoad.mNodes.size() - 1) {
                instruction = "Nadaljuj " + distance + " metrov do cilja";
            }
            else if (mNodeIndex == 1) {
                instruction = mCurrentNode.mInstructions + ", nato čez " + distance + " metrov ";
                instruction += mNextNode.mInstructions;
            }
            else {
                instruction = "Nadaljuj " + distance + " metrov, nato ";
                instruction += mNextNode.mInstructions;
            }

            if (readInstructions) {
                readInstructions(instruction);
            }

            button.setText(instruction);
            if (button.getVisibility() == View.GONE)
                button.setVisibility(View.VISIBLE);
        }
    }

    public void readInstructions(String instructions) {
        Intent serviceIntent = new Intent(getBaseContext(), TTSService.class);
        serviceIntent.putExtra("text", instructions);
        startService(serviceIntent);
    }

    public GeoPoint getLocationFromAddress(String strAddress){

        Geocoder geocoder = new Geocoder(this);

        try {
            List<Address> address = geocoder.getFromLocationName(strAddress,1);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            return new GeoPoint((int) (location.getLatitude() * 1E6),
                    (int) (location.getLongitude() * 1E6));
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    // - Sensor listeners -
    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        currentLocationOverlay.setAccuracy(accuracy);
        map.invalidate();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // - Road -
    private class RoadTask extends AsyncTask<Object, Void, Road> {

        protected Road doInBackground(Object... params) {
            ArrayList<GeoPoint> waypoints = (ArrayList<GeoPoint>)params[0];
            RoadManager roadManager = new MapQuestRoadManager(Constants.MAPQUEST_API_KEY);
            roadManager.addRequestOption("routeType=pedestrian");

            if(checkInternetConnection(getBaseContext())) {
                try {
                    Road road = roadManager.getRoad(waypoints);
                    return road;
                }
                catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return null;
                }
            }
            else {
                Log.e(TAG,"No connection");
                return null;
            }
        }

        protected void onPostExecute(Road result) {
            mRoad = result;
            mNodeIndex = 1;
            mCurrentNode = result.mNodes.get(0);
            mNextNode = result.mNodes.get(1);

            // Translating instructions
            translateInstructions();

            // Drawing road and points on map
            showRoadOnMap(result);

            // Hiding progress bar
            progressBarWrapper.setVisibility(View.GONE);

            // Setting first instruction
            setInstruction(true);
        }
    }

    public void showRoadOnMap(Road road) {
        if (road == null){
            Log.e(TAG, "Road is null");
        } else{
            if (road.mStatus != Road.STATUS_OK){
                Log.e(TAG, "Road status " + road.mStatus);
            }else{
                Polyline roadOverlay = RoadManager.buildRoadOverlay(road, this);
                map.getOverlays().add(roadOverlay);

                for (RoadNode node : road.mNodes) {
                    Marker nodeMarker = new Marker(map);
                    nodeMarker.setPosition(node.mLocation);
                    nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    nodeMarker.setIcon(getResources().getDrawable(R.drawable.ic_node_4));
                    nodeMarker.setInfoWindow(null);
                    map.getOverlays().add(nodeMarker);
                }

                // Placing current location overlay on top
                //map.getOverlays().remove(currentLocationOverlay);
                //map.getOverlays().add(currentLocationOverlay);
                map.getController().animateTo(new GeoPoint(mCurrentLocation));

                map.invalidate();
            }
        }
    }

    // - POIs -
    private class POIsTask extends AsyncTask<Object, Void, ArrayList<POI>> {

        protected ArrayList<POI> doInBackground(Object... params) {
            String keyword = (String)params[0];
            int maxResults = (int)params[1];
            double maxDistance = (double)params[2];

            GeoPoint currentGeoPoint = new GeoPoint(mCurrentLocation);

            int maxDistanceE6 = (int)(maxDistance * 1000000.0D);
            BoundingBoxE6 boundingBox = new BoundingBoxE6(currentGeoPoint.getLatitudeE6() + maxDistanceE6,
                    currentGeoPoint.getLongitudeE6() + maxDistanceE6,
                    currentGeoPoint.getLatitudeE6() - maxDistanceE6,
                    currentGeoPoint.getLongitudeE6() - maxDistanceE6);

            OverpassAPIProvider overpassAPIProvider = new OverpassAPIProvider();
            //NominatimPOIProvider nominatimPOIProvider = new NominatimPOIProvider("Sprehajalec/1.0");

            if(checkInternetConnection(getBaseContext())) {
                try {
                    //ArrayList<POI> pois = nominatimPOIProvider.getPOICloseTo(currentGeoPoint, keyword, maxResults, maxDistance);
                    String overpassPOIUrl = overpassAPIProvider.urlForPOISearch(keyword, boundingBox, maxResults, 10);
                    ArrayList<POI> pois = overpassAPIProvider.getPOIsFromUrl(overpassPOIUrl);
                    return pois;
                }
                catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return null;
                }
            }
            else {
                Log.e(TAG,"No connection");
                return null;
            }
        }

        protected void onPostExecute(ArrayList<POI> result) {
            showPOIsOnMap(result);
        }
    }

    public void showPOIsOnMap (ArrayList<POI> pois) {
        if (pois == null){
            Log.e(TAG, "POIs are null");
        }
        else {
            FolderOverlay poiMarkers = new FolderOverlay(this);
            map.getOverlays().add(poiMarkers);

            for (POI poi : pois) {
                Marker poiMarker = new Marker(map);
                poiMarker.setPosition(poi.mLocation);
                poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                poiMarker.setIcon(getResources().getDrawable(R.drawable.ic_bus_poi_1));
                poiMarker.setInfoWindow(null);
                poiMarkers.add(poiMarker);
            }

            map.getController().animateTo(new GeoPoint(mCurrentLocation));
            map.invalidate();

            Intent serviceIntent = new Intent(getBaseContext(), TTSService.class);
            serviceIntent.putExtra("text", "V bližini je " + pois.size() + " avtobusnih postaj");
            startService(serviceIntent);
        }
    }

    // - Text translate for instructions -
    private void translateInstructions() {
        Map<String, String> translations = new HashMap<String, String>() {{
            put("go", "pojdi");
            put("left", "levo");
            put("right", "desno");
            put("straight", "naravnost");
            put(" on unnamed road", "");
            put("on", "na");
            put("turn", "zavij");
            put("continue", "nadaljuj");
            put("stay", "ostani");
            put("north", "severno");
            put("east", "vzhodno");
            put("south", "južno");
            put("west", "zahodno");
            put("northeast", "severnovzhodno");
            put("southeast", "jugovzhodno");
            put("southwest", "jugozahodno");
            put("northwest", "servernozahodno");
        }};

        String regex = "(?i)\\b(go|left|right|straight| on unnamed road|on|turn|continue|stay|" +
                "north|east|south|west|northeast|southeast|southwest|northwest)\\b";

        Pattern pattern = Pattern.compile(regex);
        StringBuffer stringBuffer;
        Matcher matcher;

        for (RoadNode node : mRoad.mNodes) {
            String instructions = node.mInstructions;
            stringBuffer = new StringBuffer();
            matcher = pattern.matcher(instructions);

            while (matcher.find()) {
                matcher.appendReplacement(stringBuffer, translations.get(matcher.group().toLowerCase()));
            }
            matcher.appendTail(stringBuffer);

            node.mInstructions = stringBuffer.toString();
        }
    }

    // -- OTHER --
    public Boolean checkInternetConnection(Context con){
        ConnectivityManager connectivityManager= null;
        NetworkInfo wifiInfo, mobileInfo;

        try{
            connectivityManager = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
            wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if(wifiInfo.isConnected() || mobileInfo.isConnected())
            {
                return true;
            }
        }
        catch(Exception e){
            Log.e(TAG, e.getMessage());
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // -- ADDRESS FETCH --
    // - Starting FetchAddressIntentService -
    protected void startFetchAddressIS() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        addressResultReceiver = new AddressResultReceiver(new Handler());
        intent.putExtra(Constants.RECEIVER, addressResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        startService(intent);
    }

    // - Address receiver for FetchAddressIntentService -
    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Intent serviceIntent = new Intent(getBaseContext(), TTSService.class);
            if (resultCode == Constants.SUCCESS_RESULT) {
                serviceIntent.putExtra("text", (resultData.getString(Constants.RESULT_DATA_KEY)));
                startService(serviceIntent);
            }
            else if(resultCode == Constants.FAILURE_RESULT) {
                serviceIntent.putExtra("text", ("Naslov ni najden"));
                startService(serviceIntent);
            }
            fetchAddressRunning = false;
        }
    }

    // -- BUTTONS ON CLICK EVENTS --
    // - Trenutna lokacija -
    public void onCurrentStreetClick (View view) {
        buttonsWrapper.setVisibility(View.GONE);
        if(mCurrentLocation !=null && !fetchAddressRunning) {
            fetchAddressRunning = true;
            startFetchAddressIS();
        }
    }

    // - Trenutno navodilo za pot -
    public void onDirectionClick (View view) {
        buttonsWrapper.setVisibility(View.GONE);
        Intent serviceIntent = new Intent(getBaseContext(), TTSService.class);
        serviceIntent.putExtra("text", ((Button)view).getText());
        startService(serviceIntent);
    }

    // - POI avtobus -
    public void onBusPOIClick (View view) {
        buttonsWrapper.setVisibility(View.GONE);
        Intent serviceIntent = new Intent(getBaseContext(), TTSService.class);
        serviceIntent.putExtra("text", "Iščem");
        startService(serviceIntent);
        new POIsTask().execute("highway=bus_stop", 50, 0.005);
        //new POIsTask().execute("bus stop", 4, 0.01);
    }

    // - Favourite paths -
    public void onFavouritePathsClick (View view) {
        buttonsWrapper.setVisibility(View.GONE);
        Intent intent = new Intent(this, FavouriteListActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        finish();
        return;
    }
}