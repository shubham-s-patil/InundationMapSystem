package com.projects.inundationmap;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class InFlood extends Fragment {

    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference inFloodCollectionReference = firebaseFirestore.collection("In-Flood");
    private FirestoreRecyclerAdapter firestoreRecyclerAdapter;

    RecyclerView inFloodRecycler;

    public InFlood() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_in_flood, container, false);

        inFloodRecycler = view.findViewById(R.id.in_flood_recycler);

        loadInFloodData();
        return view;
    }

    public void loadInFloodData(){
        Query q = inFloodCollectionReference.document(FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .collection("Posts")
                .orderBy("date",Query.Direction.DESCENDING).limit(10);

        final FirestoreRecyclerOptions<Loaded> loadedFirestoreRecyclerOptions = new FirestoreRecyclerOptions.Builder<Loaded>()
                .setQuery(q, Loaded.class)
                .build();
        firestoreRecyclerAdapter = new PostHolder(loadedFirestoreRecyclerOptions,getContext());

        inFloodRecycler.setHasFixedSize(true);
        inFloodRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        inFloodRecycler.setAdapter(firestoreRecyclerAdapter);

        inFloodRecycler.addOnItemTouchListener(new RecyclerTouchListner(getContext(), inFloodRecycler, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
            }

            @Override
            public void onLongClick(View view, final int position) {

                String s = FirebaseFirestore.getInstance().collection("In-Flood").document(FirebaseAuth.getInstance().getCurrentUser().getEmail())
                        .collection("Posts").getParent().getId();

                Toast.makeText(getContext(), ""+s, Toast.LENGTH_SHORT).show();

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
