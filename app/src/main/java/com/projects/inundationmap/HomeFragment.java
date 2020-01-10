package com.projects.inundationmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.kwabenaberko.openweathermaplib.constants.Lang;
import com.kwabenaberko.openweathermaplib.constants.Units;
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper;
import com.kwabenaberko.openweathermaplib.implementation.callbacks.CurrentWeatherCallback;
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {

    Vibrator vibrator;

    private LinearLayout rescue;
    private LinearLayout hospital;
    private LinearLayout shelter;
    private LinearLayout helplines;
    private LinearLayout foodSupply;

    private TextView wind;
    private TextView clouds;
    private TextView temperature;
    private TextView humidity;
    private TextView city;

    private Location location;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private OpenWeatherMapHelper helper;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        wind = view.findViewById(R.id.wind_speed);
        clouds = view.findViewById(R.id.cloudiness);
        temperature = view.findViewById(R.id.temperature);
        humidity = view.findViewById(R.id.humidity);
        city = view.findViewById(R.id.city);

        rescue = view.findViewById(R.id.rescue_ser);
        hospital = view.findViewById(R.id.hospital_ser);
        shelter = view.findViewById(R.id.shelter_ser);
        helplines = view.findViewById(R.id.emergency_helpline);
        foodSupply = view.findViewById(R.id.food_supply);

        googleApiClient = new GoogleApiClient.Builder(view.getContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        helper = new OpenWeatherMapHelper(getString(R.string.weather_key));
        helper.setUnits(Units.METRIC);
        helper.setLang(Lang.ENGLISH);

        rescue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=rescue");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                vibrator.vibrate(80);
            }
        });

        hospital.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=hospitals");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                vibrator.vibrate(80);
            }
        });

        shelter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=shelters");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                vibrator.vibrate(80);
            }
        });

        helplines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                // Send phone number to intent as data
                intent.setData(Uri.parse("tel:" + "+911"));
                // Start the dialer app activity with number
                startActivity(intent);
                vibrator.vibrate(80);
            }
        });

        foodSupply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=food");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                vibrator.vibrate(80);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null)
            googleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (googleApiClient != null && googleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null){
            getWeather(location.getLatitude(), location.getLongitude());
        }

        startLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        String s = null;
        if (location != null){
            getWeather(location.getLatitude(), location.getLongitude());
            try{
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
                s = addresses.get(0).getLocality();
                city.setText(s);
            }catch (IOException e){
                Log.d("Camera Fragment","Something went wrong"+e.getMessage());
            }
        }
    }
    private void startLocationUpdate(){
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(getActivity(), "Enble permissions", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }
    public void getWeather(final double lat, final double lon){
        helper.getCurrentWeatherByGeoCoordinates(lat, lon, new CurrentWeatherCallback() {
            @Override
            public void onSuccess(CurrentWeather currentWeather) {
                String s = null;
                try{
                    Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat,lon,1);
                    s = addresses.get(0).getLocality();
                    city.setText(getString(R.string.city)+": "+s);
                }catch (IOException e){
                    Log.d("Camera Fragment","Something went wrong"+e.getMessage());
                }
                    temperature.setText(getString(R.string.temp)+": "+currentWeather.getMain().getTemp()+"°C");
                    humidity.setText(getString(R.string.humid)+": "+currentWeather.getMain().getHumidity()+"%");
                    clouds.setText(getString(R.string.clouds)+": "+currentWeather.getWeather().get(0).getDescription());
                    wind.setText(getString(R.string.wind)+": "+currentWeather.getWind().getSpeed()+"/"+currentWeather.getWind().getDeg()+"°");
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getContext(), "Throws"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}
