package com.gisbackend.buildingstreamer.model;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphDataModel {
    private String graphTemplate;
    private String graphData;
    private List<MetaDataNode> graphMetadata;
    // private List<AccessRight> accessRights;
    // private UseCase useCase;

    public String getGraphTemplate() {
        return graphTemplate;
    }

    public void setGraphTemplate(String graphTemplate) {
        if (isValidTurtle(graphTemplate)) {
            this.graphTemplate = graphTemplate;
        } else {
            throw new IllegalArgumentException("The Turtle file contains invalid syntax.");
        }
    }

    public String getGraphData() {
        return graphData;
    }

    public void setGraphData(String graphData) {
        if (isValidTurtle(graphData)) {
            this.graphData = graphData;
        } else {
            throw new IllegalArgumentException("The Turtle file contains invalid syntax.");
        }
    }

    // public List<AccessRight> getAccessRights() {
    //     return accessRights;
    // }

    // public void setAccessRights(List<AccessRight> accessRights) {
    //     this.accessRights = accessRights;
    // }

    // public UseCase getUseCase() {
    //     return useCase;
    // }

    // public void setUseCase(UseCase useCase) {
    //     this.useCase = useCase;
    // }

    public List<MetaDataNode> getGraphMetadata() {
        return graphMetadata;
    }

    public void setGraphMetadata(List<MetaDataNode> graphMetadata) {
        if (areValidMetadataEntries(graphMetadata)) {
            this.graphMetadata = graphMetadata;
        } else {
            throw new IllegalArgumentException("At least one metadata entry is invalid. All keys and values must be valid strings.");
        }
    }

    public GraphDataModel() {
        this.graphMetadata = new ArrayList<>();
    }

    public void serializeToFile(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), this);
    }

    public String serializeToJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public static GraphDataModel deserializeFromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, GraphDataModel.class);
    }

    public static GraphDataModel deserializeFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("The file was not found: " + filePath);
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, GraphDataModel.class);
    }

    private static boolean isValidTurtle(String turtleContent) {
        if (turtleContent == null || turtleContent.trim().isEmpty()) {
            return false;
        }
        String turtlePattern = "@prefix\\s+\\w+:\\s+<.*?>\\s*\\.";
        return Pattern.compile(turtlePattern).matcher(turtleContent).find();
    }

    private static boolean areValidMetadataEntries(List<MetaDataNode> metadataEntries) {
        if (metadataEntries == null || metadataEntries.isEmpty()) {
            return false;
        }
        for (MetaDataNode node : metadataEntries) {
            if (node.getId() == null || node.getId().trim().isEmpty() ||
                node.getClassType() == null || node.getClassType().trim().isEmpty()) {
                return false;
            }
            for (Map.Entry<String, String> kvp : node.getPropertiesValues().entrySet()) {
                if (kvp.getKey() == null || kvp.getKey().trim().isEmpty() ||
                    kvp.getValue() == null || kvp.getValue().trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}

