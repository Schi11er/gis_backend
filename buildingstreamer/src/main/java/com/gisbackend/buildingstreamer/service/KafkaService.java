package com.gisbackend.buildingstreamer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisbackend.buildingstreamer.model.Address;
import com.gisbackend.buildingstreamer.model.Building;
import com.gisbackend.buildingstreamer.model.GraphDataModelsWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KafkaService {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private NominatimService nominatimService;
    
    // Compile regex patterns once for better performance
    private static final Pattern STREET_CORRECTIONS = Pattern.compile("(?i)(strafe|straBe|strae|strage|stratle)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRASSE_DUPLICATES = Pattern.compile("(?i)(Straße)(aße|ate)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROSSE_CORRECTIONS = Pattern.compile("(?i)(Gro)(fe|Be)", Pattern.CASE_INSENSITIVE);

    @KafkaListener(topics = "${KAFKA_TOPIC}", groupId = "gis_group", containerFactory = "graphModelListener")
    public void publish(GraphDataModelsWrapper wrapper) {
        try {
            log.info("Processing Kafka message with {} models", wrapper.getGraphDataModels().size());
            
            // Process the received GraphDataModel
            List<Building> buildings = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            mapper.setConfig(mapper.getDeserializationConfig().with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
            
            // Convert GraphDataModel to Building and Address objects
            wrapper.getGraphDataModels().forEach(model -> {
                try {
                    final Address[] addressWrapper = { new Address() };
                    final Building[] buildingWrapper = { new Building() };
                    
                    model.getGraphMetadata().forEach(metaDataNode -> {
                        try {
                            if (metaDataNode.getClassType().equals("https://ibpdi.datacat.org/class/Address")) {
                                addressWrapper[0] = mapper.convertValue(metaDataNode.getPropertiesValues(), Address.class);
                                addressWrapper[0].setId(metaDataNode.getId());
                                
                                addressWrapper[0].setStreetName(correctStreet(addressWrapper[0].getStreetName()));
                                // Enrich address with coordinates if missing
                                try {
                                    addressWrapper[0] = nominatimService.enrichAddressWithCoordinates(addressWrapper[0]);
                                } catch (Exception e) {
                                    log.warn("Failed to enrich address {} with coordinates: {}", addressWrapper[0].getId(), e.getMessage());
                                    // Continue processing without coordinates
                                }
        
                            } else if (metaDataNode.getClassType().equals("https://ibpdi.datacat.org/class/Building")) {
                                buildingWrapper[0] = mapper.convertValue(metaDataNode.getPropertiesValues(), Building.class);
                                buildingWrapper[0].setId(metaDataNode.getId());
                            }
                        } catch (Exception e) {
                            log.error("Error processing metadata node {}: {}", metaDataNode.getId(), e.getMessage());
                        }
                    });
                    
                    // Set the address for the building and store it in the list
                    buildingWrapper[0].setAddress(addressWrapper[0]);
                    buildings.add(buildingWrapper[0]);
                    
                    // Only add building if it doesn't already exist (prevent duplicate processing)
                    if (buildingService.getBuildingById(buildingWrapper[0].getId()) == null) {
                        buildingService.addBuilding(buildingWrapper[0]);
                        log.debug("Added new building with ID: {}", buildingWrapper[0].getId());
                    } else {
                        log.debug("Building with ID {} already exists, skipping...", buildingWrapper[0].getId());
                    }
                } catch (Exception e) {
                    log.error("Error processing building model: {}", e.getMessage());
                }
            });
            
            log.info("Successfully processed {} buildings from Kafka message", buildings.size());
            
        } catch (Exception e) {
            log.error("Critical error processing Kafka message: {}", e.getMessage(), e);
            // Even if there's an error, we don't want to retry the message indefinitely
            // The message will be marked as processed to prevent endless retries
        }
    }


    public String correctStreet(String streetName) {
        // Korrigiere verschiedene falsche Schreibweisen von "straße" mit einem Pattern
        streetName = STREET_CORRECTIONS.matcher(streetName).replaceAll("straße");
        
        // Korrigiere doppelte Endungen nach "Straße" 
        streetName = STRASSE_DUPLICATES.matcher(streetName).replaceAll("$1");
        
        // Korrigiere verschiedene falsche Schreibweisen von "Große"
        streetName = GROSSE_CORRECTIONS.matcher(streetName).replaceAll("$1ße");

        return streetName;
    }


}
