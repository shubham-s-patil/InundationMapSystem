package com.projects.inundationmap;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {

    CircleImageView profileImage;
    TextView userName;
    Button signout;

    private TabLayout tabLayout;
    private TabAdapter tabAdapter;
    private ViewPager viewPager;

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImage = view.findViewById(R.id.profile_photo);
        userName = view.findViewById(R.id.profile_name);
        signout = view.findViewById(R.id.logout);
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);

        tabAdapter = new TabAdapter(getChildFragmentManager());
        tabAdapter.addFragment(new InFlood(),getString(R.string.in_flood));
        tabAdapter.addFragment(new AfterFlood(),getString(R.string.after_flood));

        viewPager.setAdapter(tabAdapter);
        tabLayout.setupWithViewPager(viewPager);

        if (firebaseAuth.getCurrentUser() != null){
            profileImage.setImageURI(firebaseAuth.getCurrentUser().getPhotoUrl());
            userName.setText(firebaseAuth.getCurrentUser().getDisplayName());
            Glide.with(getContext()).load(firebaseAuth.getCurrentUser().getPhotoUrl()).into(profileImage);
        }

        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });

        return view;
    }

    public void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getContext(), MainLogin.class);
        startActivity(intent);
        getActivity().finish();
    }

}
