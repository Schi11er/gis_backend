package com.gisbackend.buildingstreamer.model;

import lombok.Data;

@Data
public class GeoCoordinate {
    
    private String id;
    private String coordinateReferenceSystem;
    private String latitude;
    private String longitude;
}
