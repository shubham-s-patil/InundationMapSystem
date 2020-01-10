package com.projects.inundationmap;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Loaded {
    private String userName, descr, url, city;
    Timestamp date;
    private GeoPoint geoPoint;

    public Loaded(){

    }

    public Loaded(String userName, String descr, String url, GeoPoint geoPoint, Timestamp date, String city){
        this.userName = userName;
        this.descr = descr;
        this.url = url;
        this.geoPoint = geoPoint;
        this.date = date;
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

