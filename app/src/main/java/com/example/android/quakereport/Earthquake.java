package com.example.android.quakereport;

/**
 * Created by suresh on 22/1/18.
 */

public class Earthquake {
    private String place;
    private Double magnitude;
    private Long date;

    private String url;

    public Earthquake(String place, Double magnitude, Long date){
        this.place=place;
        this.magnitude=magnitude;
        this.date=date;
    }

    public void setPlace(String place) {
        this.place = place;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {

        return url;
    }


    public void setMagnitude(Double magnitude) {
        this.magnitude = magnitude;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getPlace() {
        return place;
    }

    public Double getMagnitude() {
        return magnitude;
    }

    public Long getDate() {
        return date;
    }

    public Earthquake(){}

}
