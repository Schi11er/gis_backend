package com.gisbackend.buildingstreamer.model;

import lombok.Data;

@Data
public class Building {
    
    private String id;
    private String name;
    private String validFrom;
    private String buildingCode;
    private String parkingSpaces;
    private String constructionYear;
    private String primaryHeatingType;
    private String energyEfficiencyClass;
    private String primaryTypeOfBuilding;
    private Address address;
}
