package com.gisbackend.buildingstreamer.model;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetaDataNode {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("ClassType")
    private String classType;

    @JsonProperty("PropertiesValues")
    private Map<String, String> propertiesValues;

    public MetaDataNode() {
        this.id = "";
        this.classType = "";
        this.propertiesValues = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public Map<String, String> getPropertiesValues() {
        return propertiesValues;
    }

    public void setPropertiesValues(Map<String, String> propertiesValues) {
        this.propertiesValues = propertiesValues;
    }
}
