package com.gisbackend.buildingstreamer.model;

import java.util.*;

public class MetaDataNode {
    private String id;

    private String classType;

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
