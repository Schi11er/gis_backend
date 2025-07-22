package com.gisbackend.buildingstreamer.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisbackend.buildingstreamer.model.Address;
import com.gisbackend.buildingstreamer.model.Building;
import com.gisbackend.buildingstreamer.model.GraphDataModelsWrapper;

@Service
public class KafkaService {

    @Autowired
    private BuildingService buildingService;

    @KafkaListener(topics = "${KAFKA_TOPIC}", groupId = "gis_group", containerFactory = "graphModelListener")
    public void publish(GraphDataModelsWrapper wrapper) {
        // Process the received GraphDataModel
        // GraphDataModel graphDataModel = wrapper.getGraphDataModels().get(0);
        // System.out.println("Received GraphDataModel: " +
        // graphDataModel.getGraphTemplate());
       
        List<Building> buildings = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setConfig(mapper.getDeserializationConfig().with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        
        // Convert GraphDataModel to Building and Address objects
        wrapper.getGraphDataModels().forEach(model -> {
            // System.out.println("Processing GraphDataModel with template: " + model.getGraphTemplate());
            final Address[] addressWrapper = { new Address() };
            final Building[] buildingWrapper = { new Building() };
            model.getGraphMetadata().forEach(metaDataNode -> {
                if (metaDataNode.getClassType().equals("https://ibpdi.datacat.org/class/Address")) {
                    addressWrapper[0] = mapper.convertValue(metaDataNode.getPropertiesValues(), Address.class);
                    addressWrapper[0].setId(metaDataNode.getId());
                } else if (metaDataNode.getClassType().equals("https://ibpdi.datacat.org/class/Building")) {
                    buildingWrapper[0] = mapper.convertValue(metaDataNode.getPropertiesValues(), Building.class);
                    buildingWrapper[0].setId(metaDataNode.getId());
                }

            });
            // Set the address for the building and store it in the list
            buildingWrapper[0].setAddress(addressWrapper[0]);
            buildings.add(buildingWrapper[0]);
            buildingService.addBuilding(buildingWrapper[0]);
        });
   
        // buildings.forEach(building -> {
        //     System.out.println("Street: " + building.getAddress().getStreetName());
        // });

    }

}
