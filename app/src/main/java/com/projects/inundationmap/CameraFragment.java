package com.projects.inundationmap;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.gesture.GestureLibraries;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;

import static android.app.Activity.RESULT_OK;


public class CameraFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private ImageView mainImageView;
    private Uri imageUri;
    private Uri extraUri;
    private static final int IMAGE_CAPTURE_CODE = 8889;
    private LinearLayout sendData;
    private EditText descrText;
    private TextView timestamp;
    private TextView location;
    private String globalImageURL;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;


    private ProgressDialog progressDialog;

    private String[] floods = {"After-Flood","In-Flood"};

    private Spinner spinner;

    String selected;

    FirebaseFirestore firebaseFirestore;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    private Location locationAccess;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true).build();

    double lat, lon;

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_camera, container, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = sharedPreferences.edit();

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseFirestore.setFirestoreSettings(settings);
        mainImageView = view.findViewById(R.id.main_image);
        sendData = view.findViewById(R.id.send_data);
        descrText = view.findViewById(R.id.descr);
        timestamp = view.findViewById(R.id.timestamp);
        location = view.findViewById(R.id.location);
        spinner = view.findViewById(R.id.spinner);
        storageReference = firebaseStorage.getReference().child(FirebaseAuth.getInstance().getCurrentUser()
        .getEmail());

        if (sharedPreferences.getString("pendingURI",null) == null)
            openCamera();
        else {
            Glide.with(getActivity()).load(sharedPreferences.getString("pendingURI",""))
                    .centerCrop()
                    .into(mainImageView);
        }

        ArrayAdapter arrayAdapter = new ArrayAdapter(getContext(),android.R.layout.simple_spinner_item,floods);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);

        googleApiClient = new GoogleApiClient.Builder(view.getContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selected = floods[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });



        sendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendImage();
                sendData.setVisibility(View.GONE);
                progressDialog = new ProgressDialog(getContext());
                progressDialog.setMax(100);
                progressDialog.setMessage("Post is uploading....");
                progressDialog.setTitle("Uploading");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            }
        });

        return view;
    }

    public void openCamera(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "FloodRescue Images");
        values.put(MediaStore.Images.Media.DESCRIPTION, "image is taken from FloodRescue");
        imageUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cam, IMAGE_CAPTURE_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            Glide.with(getContext()).load(imageUri).centerCrop().into(mainImageView);
            String city = null;
            timestamp.setText((new Date()).toString());
            try{
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat,lon,1);
                city = addresses.get(0).getLocality();
                location.setText(city);
            }catch (IOException e){
                Log.d("Camera Fragment","Something went wrong"+e.getMessage());
            }catch (IndexOutOfBoundsException e){
                Log.d("Index error",""+e.getMessage());
            }
            extraUri = imageUri;
        }else{
            goBack();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null)
            googleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        if (mainImageView.getDrawable() != null){
            editor.putString("pendingURI",extraUri.toString());
            editor.commit();
        }
        super.onDestroy();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (googleApiClient != null && googleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    public void sendData(String url){
        String city = null;
        try{
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat,lon,1);
            city = addresses.get(0).getLocality();
        }catch (IOException e){
            Log.d("Camera Fragment","Something went wrong"+e.getMessage());
        }


        Map<String, Object> data = new HashMap<>();
        data.put("userName", FirebaseAuth.getInstance().getCurrentUser().getEmail());
        data.put("descr",descrText.getText().toString());
        data.put("date", new Timestamp(new Date()));
        data.put("url",url);
        data.put("geoPoint", new GeoPoint(lat,lon));
        data.put("city",city);

        globalImageURL = url;
        final String[] key = new String[1];

        firebaseFirestore.collection(selected).document(FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .collection("Posts")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(getContext(), "Data Uploaded Successfully", Toast.LENGTH_SHORT).show();
                        key[0] = documentReference.getId();

                        Map<String, Object> links = new HashMap<>();
                        Map<String, Object> keys = new HashMap<>();

                        if (selected.equals("In-Flood")){
                            links.put("email",FirebaseAuth.getInstance().getCurrentUser().getEmail());
                            links.put("keys",documentReference.getId());
                            firebaseFirestore.collection("In-Links").add(links);
                        }else{
                            links.put("email",FirebaseAuth.getInstance().getCurrentUser().getEmail());
                            links.put("keys",documentReference.getId());
                            firebaseFirestore.collection("After-Links").add(links);
                        }
                        goBack();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                StorageReference sRef;
                firebaseFirestore.collection(selected).document(FirebaseAuth.getInstance().getCurrentUser()
                .getEmail()).collection("Posts").document(key[0]).delete();
                sRef = firebaseStorage.getReference().child(globalImageURL);
                sRef.delete();
                Toast.makeText(getContext(), "Data upload failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void sendImage(){
        mainImageView.setDrawingCacheEnabled(true);
        mainImageView.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) mainImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        final byte[] data = baos.toByteArray();

        final String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName()+new Date();

        UploadTask uploadTask;
        uploadTask = storageReference.child(name).putBytes(data);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                sendData(name);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), "Failed to upload data: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Toast.makeText(getContext(), "Failed"+e.getCause(), Toast.LENGTH_LONG).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressDialog.incrementProgressBy((int)progress);
                progressDialog.show();
                if ((int)progress == 100)
                    progressDialog.dismiss();
            }
        });
    }

    public void goBack(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Do you want to upload another picture?")
                .setPositiveButton("YES",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                openCamera();
                                descrText.setText(null);
                                sendData.setVisibility(View.VISIBLE);
                            }
                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container,new HomeFragment()).commitAllowingStateLoss();
            }
        }).create().show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        locationAccess = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (locationAccess != null){
            lat = locationAccess.getLatitude();
            lon = locationAccess.getLongitude();
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
    public void onLocationChanged(Location l) {
        if (l != null){
            lat = l.getLatitude();
            lon = l.getLongitude();
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
}
