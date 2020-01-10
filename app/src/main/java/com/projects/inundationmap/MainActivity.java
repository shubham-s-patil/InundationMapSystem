package com.projects.inundationmap;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Vibrator;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private ActionBar actionBar;
    private Vibrator vibrator;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static GoogleApiClient mGoogleApiClient;
    private static final String BROADCAST_ACTION = "android.location.PROVIDERS_CHANGED";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        BottomNavigationView navigationView = findViewById(R.id.bottomnavigationbar);
        navigationView.setOnNavigationItemSelectedListener(this);

        if (sharedPreferences.getString("pendingURI",null) != null){
            loadFragment(new CameraFragment());

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setMessage("You have a pending picture to upload\nWould you like to upload it?")
                     .setPositiveButton("YES",
                              new DialogInterface.OnClickListener() {
                                   @Override
                                 public void onClick(DialogInterface dialogInterface, int i) {
                                       loadFragment(new CameraFragment());
                                    }
                             }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                     @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                         loadFragment(new HomeFragment());
                         Toast.makeText(MainActivity.this, "Your pending data will be erased", Toast.LENGTH_SHORT).show();
                         editor.putString("pendingURI",null).apply();
                     }
            }).create().show();
        }else
            loadFragment(new HomeFragment());

        initGoogleApi();
        requestLocation();
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Fragment fragment = null;
        switch (menuItem.getItemId()){
            case R.id.home:
                fragment = new HomeFragment();
                vibrator.vibrate(80);
                break;
            case R.id.camera:
                fragment = new CameraFragment();
                vibrator.vibrate(80);
                break;
            case R.id.profile:
                fragment = new ProfileFragment();
                vibrator.vibrate(80);
                break;
        }
        return loadFragment(fragment);
    }

    public boolean loadFragment(Fragment fragment){

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.language){
            changeLanguage();
        }
        return super.onOptionsItemSelected(item);
    }

    public void requestLocation(){
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted())
                            showSettingDialog();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                        showSettingDialog();
                    }
                }).onSameThread().check();
    }

    private void initGoogleApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void showSettingDialog() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);                       //5 seconds
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            showSettingDialog();
        }
    };

    private BroadcastReceiver gpsLocationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().matches(BROADCAST_ACTION)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                } else {
                    new Handler().postDelayed(sendUpdatesToUI, 10);
                }

            }

            getApplicationContext().unregisterReceiver(this);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gpsLocationReceiver, new IntentFilter(BROADCAST_ACTION));
    }


    public void changeLanguage(){
        final String[] lang ={"English","हिन्दी","मराठी","தமிழ்","తెలుగు","മലയാളം"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_language));
        builder.setSingleChoiceItems(lang, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0)
                {
                    setLocale("en");
                    loadFragment(null);
                    recreate();
                } else if(which==1) {
                    setLocale("hi");
                    loadFragment(null);
                    recreate();
                } else if (which == 2){
                    setLocale("mr");
                    recreate();
                }else if (which == 3){
                    setLocale("ta");
                    recreate();
                }else if (which == 4){
                    setLocale("te");
                    recreate();
                }else if (which == 5){
                    setLocale("ml");
                    recreate();
                }
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void setLocale(String lang){
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale=locale;
        getBaseContext().getResources().updateConfiguration(configuration,getBaseContext().getResources().getDisplayMetrics());


        SharedPreferences.Editor editor = getSharedPreferences("setting",MODE_PRIVATE).edit();
        editor.putString("My_Lang",lang);
        editor.apply();
    }

   /* @Override
    public void onBackPressed() {
            new AlertDialog.Builder(this)
                    .setTitle("really exit?")
                    .setMessage("Are you sure?")
                    .setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.super.onBackPressed();
                        }
                    }).create().show();
    }*/
}
