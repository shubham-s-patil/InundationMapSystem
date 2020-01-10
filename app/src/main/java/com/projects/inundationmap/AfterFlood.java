package com.projects.inundationmap;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Movie;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.Date;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AfterFlood extends Fragment {

    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference inFloodCollectionReference = firebaseFirestore.collection("After-Flood");
    private FirestoreRecyclerAdapter firestoreRecyclerAdapter;

    RecyclerView afterFloodRecycler;

    public AfterFlood() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_after_flood, container, false);

        afterFloodRecycler = view.findViewById(R.id.after_flood_recycler);
        loadAfterFloodData();

        return view;
    }

    public void loadAfterFloodData(){
        final Query q = inFloodCollectionReference.document(FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .collection("Posts")
                .orderBy("date", Query.Direction.DESCENDING).limit(10);

        final FirestoreRecyclerOptions<Loaded> loadedFirestoreRecyclerOptions = new FirestoreRecyclerOptions.Builder<Loaded>()
                .setQuery(q, Loaded.class)
                .build();
        firestoreRecyclerAdapter = new PostHolder(loadedFirestoreRecyclerOptions,getContext());

        afterFloodRecycler.setHasFixedSize(true);
        afterFloodRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        afterFloodRecycler.setAdapter(firestoreRecyclerAdapter);

        afterFloodRecycler.addOnItemTouchListener(new RecyclerTouchListner(getContext(), afterFloodRecycler, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
            }

            @Override
            public void onLongClick(View view, final int position) {
                Toast.makeText(getContext(), ""+loadedFirestoreRecyclerOptions.getSnapshots().get(position).getUrl(), Toast.LENGTH_SHORT).show();
                
            }
        }));
    }

    @Override
    public void onStart() {
        super.onStart();
        firestoreRecyclerAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        firestoreRecyclerAdapter.stopListening();
    }
}
