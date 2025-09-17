package com.gisbackend.buildingstreamer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessRight {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("GuidelineClassificationId")
    private String guidelineClassificationId;

    @JsonProperty("UserGroupId")
    private String userGroupId;

    @JsonProperty("UseCaseId")
    private String useCaseId;

    @JsonProperty("GuidlineClassificationPropertyId")
    private String guidlineClassificationPropertyId;

    @JsonProperty("Right")
    private int right;
}
