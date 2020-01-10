package com.projects.inundationmap;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PostHolder extends FirestoreRecyclerAdapter<Loaded, PostHolder.PstHolder> {

    Context context;

    public PostHolder(@NonNull FirestoreRecyclerOptions<Loaded> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull final PstHolder postHolder, int i, @NonNull Loaded loaded) {
        String s;
        postHolder.descr.setText(loaded.getDescr());
        postHolder.timest.setText(""+loaded.getDate().toDate());
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(FirebaseAuth.getInstance()
                .getCurrentUser().getEmail()+"/"+loaded.getUrl());
        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context).load(uri).centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL).into(postHolder.mainImage);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("Image Failure",e.getCause());
            }
        });
        try{
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(loaded.getGeoPoint().getLatitude(),loaded
                    .getGeoPoint().getLongitude(),1);
            s = addresses.get(0).getAddressLine(0);
            postHolder.location.setText(s);
        }catch (IOException e){
            Log.d("Camera Fragment","Something went wrong"+e.getMessage());
        }
    }

    @NonNull
    @Override
    public PstHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.data_loader,parent,false);
        return new PstHolder(view);
    }

    class PstHolder extends RecyclerView.ViewHolder{
        public TextView descr, location, timest;
        public ImageView mainImage;
        public PstHolder(View holder){
            super(holder);
            descr = holder.findViewById(R.id.descr_prof);
            location = holder.findViewById(R.id.location_prof);
            timest = holder.findViewById(R.id.t_stamp);
            mainImage = holder.findViewById(R.id.main_image);
        }
    }
}
