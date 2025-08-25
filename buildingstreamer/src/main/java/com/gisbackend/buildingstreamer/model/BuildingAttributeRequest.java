package com.gisbackend.buildingstreamer.model;

import java.util.Map;

import lombok.Data;

@Data
public class BuildingAttributeRequest {
    private Map<String, String> attributes;
}
