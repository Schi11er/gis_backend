package com.gisbackend.buildingstreamer.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Building {
    
    private String id;
    private String buildingId;
    private String name;
    private String validFrom;
    private String buildingCode;
    private String parkingSpaces;
    private String constructionYear;
    private String primaryHeatingType;
    private String energyEfficiencyClass;
    private String primaryTypeOfBuilding;
    private Address address;
    private Map<String, String> additionalAttributes = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalAttribute(String key, String value) {
        this.additionalAttributes.put(key, value);
    }

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttributes(Map<String, String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }
}
