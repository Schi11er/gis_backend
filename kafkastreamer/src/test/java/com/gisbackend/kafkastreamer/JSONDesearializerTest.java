package com.gisbackend.kafkastreamer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisbackend.kafkastreamer.model.GraphDataModel;
import com.gisbackend.kafkastreamer.model.GraphDataModelsWrapper;

import org.junit.jupiter.api.Test;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class GraphDataModelDeserializationTest {

    @Test
    void testDeserialization() throws Exception {
        // Datei aus dem resources-Ordner laden
        // InputStream inputStream = getClass().getClassLoader().getResourceAsStream("response_1750681694253.json");
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("message.json");

        assertNotNull(inputStream, "JSON file not found");

        ObjectMapper mapper = new ObjectMapper();
        GraphDataModelsWrapper wrapper = mapper.readValue(inputStream, GraphDataModelsWrapper.class);
        GraphDataModel graphDataModel = wrapper.getGraphDataModels().get(0);
        assertNotNull(graphDataModel);
        assertNotNull(graphDataModel.getGraphData());
        System.out.println("Hello world from GraphDataModelDeserializationTest!");
        System.out.println("Graph Metadata: " + graphDataModel.getGraphMetadata().get(0).getId());
        // assertEquals("ExampleClass", graphDataModel.getClassType());
        // assertEquals("value1", graphDataModel.getPropertiesValues().get("key1"));
    }
}