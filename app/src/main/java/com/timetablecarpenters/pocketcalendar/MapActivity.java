package com.timetablecarpenters.pocketcalendar;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * MapActivity is an activity where the user can look up locations and get name and address information from.
 * This class uses the google maps Api to create a MapFragment and manipulate it to move around it. This class also uses
 * Google Places API to AutoComplete user inputs and find information about the location searched. The user can select a location to be
 * added to their CalendarEvent via this class
 * @author: Deniz Mert Dilaverler
 * @version 30.04.2021
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    public final static String INTENT_ID_KEY = "activity";
    public final static String EVENT_KEY = "event_from_maps";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    //widgets
    private AutocompleteSupportFragment autocompleteFragment;
    private ImageView mGps;
    private BottomSheetDialog bottomSheetDialog;


    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Place placeToSearch;
    private CalendarEvent event;
    private String previousActivityKey;

    /**
     * initialisation of the autocompleteFragment and the GPS button, it calls getLocationPermission to check wether
     * the user consents to their Location data to be used
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                event = (CalendarEvent) extras.get(DayActivity.MAPS_INTENT_KEY);
            } catch (Exception e) {
                Log.e(TAG, "onCreate: couldn't find an event: " + e );
            }
            previousActivityKey = (String) extras.get(INTENT_ID_KEY);
        }

        View content = findViewById(R.id.week_content);
        autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        mGps = (ImageView) findViewById((R.id.ic_gps));

        getLocationPermission();

    }

    /**
     * this is the callback method which is called when the map is set up
     * initializes mMap. If the location permissions are granted, then gets the location of the user and initializes GooglePlaces
     * Autocomplete api and the GPS button's on click listener.
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;


        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);


            initPlacesAutoComplete();
            init();
        }
    }

    /**
     * set an onClickListener that takes the camera of the map to the user location
     */
    private void init(){
        Log.d(TAG, "init: initializing");

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();

            }
        });

    }

    /**
     * Finds the location with the Latlng value that the Place instance provide. Takes the camera to the location of the place
     * calls the initSheetView().initSheetView() initializes the SheetView that shows the user, the location they searched for.
     */
    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating");

       //String searchString = autocompleteFragment.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        if (placeToSearch != null) {
            try {
                Log.d(TAG, "geoLocate: " + placeToSearch.getLatLng().latitude + " " + placeToSearch.getLatLng().longitude);
                list = geocoder.getFromLocation(placeToSearch.getLatLng().latitude, placeToSearch.getLatLng().longitude, 1);
            } catch (IOException e) {
                Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
            }

            if (list.size() > 0) {
                Address address = list.get(0);

                Log.d(TAG, "geoLocate: found a location: " + address.toString());
                //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
                moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));

               initSheetView();

            }
        }
    }

    private void initSheetView() {
        bottomSheetDialog = new BottomSheetDialog(MapActivity.this, R.style.BottomSheetTheme);
        View sheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.bottom_sheet_layout,
                (ViewGroup) findViewById(R.id.bottom_sheet));

        bottomSheetDialog.setContentView(sheetView);
        ((TextView) sheetView.findViewById(R.id.place_name_tv)).setText(placeToSearch.getName());
        ((TextView) sheetView.findViewById(R.id.address_tv)).setText(placeToSearch.getAddress());
        ImageView imageView = ((ImageView) sheetView.findViewById((R.id.dialog_place_pic)));
        AppCompatImageView  button = (AppCompatImageView) sheetView.findViewById(R.id.save_place_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                event.setLocation(placeToSearch.getLatLng());
                Class returnActivity;
                // Todo: if there are multiple Activities to possibly return add an if statement
                returnActivity = DayActivity.class;
                Intent intent = new Intent(MapActivity.this, returnActivity);
                // intent.putExtra(EVENT_KEY);
                startActivity(intent);

            }
        });

        List<PhotoMetadata> photos = placeToSearch.getPhotoMetadatas();
        if( photos == null || photos.size() == 0) {
            Log.d(TAG, "initSheetView: attributions " );
            imageView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_broken_image, null));
            imageView.setScaleType(ImageView.ScaleType.CENTER);
        } else {

            // The code beneath has been taken from the Google PlacesAPI documentation page https://developers.google.com/maps/documentation/places/android-sdk/photos
            // Photo metadata (HTML String) is converted to a bitmap to be portrayed in the UI
            final PhotoMetadata photoMetadata = photos.get(0);

            // Get the attribution text.
            final String attributions = photoMetadata.getAttributions();

            PlacesClient placesClient = Places.createClient(this);

            // Create a FetchPhotoRequest.
            final FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                    .setMaxWidth(500) // Optional.
                    .setMaxHeight(300) // Optional.
                    .build();
            placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                Bitmap bitmap = fetchPhotoResponse.getBitmap();
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }).addOnFailureListener((exception) -> {
                if (exception instanceof ApiException) {
                    final ApiException apiException = (ApiException) exception;
                    Log.e(TAG, "Place not found: " + exception.getMessage());
                    final int statusCode = apiException.getStatusCode();
                }
            });
        }

        bottomSheetDialog.show();
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            if(currentLocation != null)
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM, "My Location");

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equalsIgnoreCase("My Location")) {
            MarkerOptions options = new MarkerOptions().position(latLng).title(title);
            mMap.addMarker(options);
        }

    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment_encapsulater);

        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }



    private void initPlacesAutoComplete() {
        // Initialize the SDK
        Places.initialize(getApplicationContext(), "AIzaSyAaoyx0rOYoobFetCe34LdwVo6BLKp2HCU");

        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);


        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.PHOTO_METADATAS, Place.Field.ADDRESS));
        Log.d(TAG, "initPlacesAutoComplete: " +  this.getResources().getConfiguration().locale.getCountry()) ;
        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName() );
                placeToSearch = place;
                mMap.clear();
                geoLocate();
            }

            @Override
            public void onError(@NotNull Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });


    }
}











