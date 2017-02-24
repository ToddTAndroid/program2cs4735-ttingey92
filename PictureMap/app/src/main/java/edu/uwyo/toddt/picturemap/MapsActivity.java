package edu.uwyo.toddt.picturemap;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.support.design.widget.FloatingActionButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.Result;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.*;

public class  MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    // Constant values for app
    private static final String TAG = "MyActivity";
    private final Activity mActivity = this;
    final Context context = this;
    private static final int REC_CODE = 102;
    static final int REQUEST_IMAGE_CAPTURE =1;
    static final LatLng LARAMIE = new LatLng(41.312928, -105.587253);

    // Map objects and values
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    LatLng currentPos = new LatLng(41.312928, -105.587253);
    private LocationRequest mLocationRequest;

    // Image capture variables
    ImageView mImageView;
    Bitmap imageBitmap;
    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create Google API Client
        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


        // FAB to begin camera method
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_camera);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getLocation();

                dispatchTakePictureIntent();

            }
        });
    }

    @Override
    protected void onStart(){
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Check for permissions. If permisson not granted request permission before
        // entering requestLocatonAccess
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REC_CODE);
        } else {
            requestLocationAccess();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode) {
            case REC_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    requestLocationAccess();
                } else {
                    Log.i(TAG, "Permisson denied");
                }
                break;
            default:
                Toast.makeText(this, "Permission not matched", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // Once the camera activity has returned with picture data, create a scale bitmap option
        // for more reasonable storage in memory. Then make the bitmap with the image and scale,
        // make the marker, and store the bitmap in the marker with a tag.
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;

            imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, opts);

            Marker pin = mMap.addMarker(new MarkerOptions()
                    .position(currentPos)
            );
            pin.setTag(imageBitmap);
        }
    }

    // Keeps the users current position up to date
    @Override
    public void onLocationChanged(Location location){
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        currentPos = new LatLng(lat, lon);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                // Create a dialog to display the image
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.image_dialog);
                dialog.setTitle("Associated Image");

                mImageView = (ImageView) dialog.findViewById(R.id.imgv_img);

                // Set the image
                Bitmap markImg = (Bitmap) marker.getTag();
                mImageView.setImageBitmap(markImg);

                Button closeButton = (Button) dialog.findViewById(R.id.imgv_button);
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
                return false;
            }
        });
    }

    public void requestLocationAccess() {
        // create and format a location request
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        final com.google.android.gms.common.api.PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // Once application knows location services are available get initial
                        // location and begin location updates
                        getLocation();
                        startLocationUpdates();

                        // Move map to center on user
                        float zoom = (float) 16.0;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, zoom));
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // if gps is not on, prompt user to turn it on.
                        try{
                            status.startResolutionForResult(mActivity, 103);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate");
                        break;
                }
            }
        });
    }

    public void getLocation(){
        // Obligatory permissions check before attempting to retrieve last known location.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        currentPos = new LatLng(lat, lon);
    }

    // Method to begin image capture intent and load camera app.
    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null){

            // Create file for photo
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.i(TAG, "Error occurred while creating photo file");
            }

            if(photoFile != null){
                Uri photoURI = FileProvider.getUriForFile(this,
                                                        "edu.uwyo.toddt.picturemap",
                                                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imgFileName =  "MapAPP_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imgFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    protected void startLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this
        );
    }

}
