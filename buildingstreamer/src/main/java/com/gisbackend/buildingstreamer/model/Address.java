package com.gisbackend.buildingstreamer.model;

import lombok.Data;

@Data
public class Address {

    private String id;
    private String country;
    private String city;
    private String postalCode;
    private String streetName;
    private String houseNumber;
    private String lat;
    private String lon;

}
