package com.gisbackend.buildingstreamer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisbackend.buildingstreamer.model.AccessRight;
import com.gisbackend.buildingstreamer.model.Address;
import com.gisbackend.buildingstreamer.model.Building;
import com.gisbackend.buildingstreamer.model.GeoCoordinate;
import com.gisbackend.buildingstreamer.model.GraphDataModel;
import com.gisbackend.buildingstreamer.model.MetaDataNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KafkaService {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private NominatimService nominatimService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AccessRightsService accessRightsService;

    @Value("${KAFKA_TOPIC}")
    private String kafkaTopic;

    @KafkaListener(topics = "${KAFKA_TOPIC}", groupId = "gis_group", containerFactory = "graphModelListener")
    public void publish(GraphDataModel graphDataModel, ConsumerRecord<?, ?> record) {
        try {
            log.info("Processing Kafka message with Offset: {}", record.offset());

            // Process the received GraphDataModel
            List<Building> buildings = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            mapper.setConfig(mapper.getDeserializationConfig().with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));

            // Initialize variables outside the loop
            Address address = null;
            Building building = null;
            GeoCoordinate geoCoordinate = null;

            // Process metadata nodes
            for (MetaDataNode metaDataNode : graphDataModel.getGraphMetadata()) {
                try {
                    if (metaDataNode.getClassType().equals("https://ibpdi.datacat.org/class/Address")) {
                        address = mapper.convertValue(metaDataNode.getPropertiesValues(), Address.class);
                        address.setId(metaDataNode.getId());

                        if (address.getDeprecatedLatitude() == null || address.getDeprecatedLongitude() == null) {
                            try {
                                log.warn("Address {} is missing coordinates, attempting to enrich...", address.getId());
                                address = nominatimService.enrichAddressWithCoordinates(address);
                            } catch (Exception e) {
                                log.warn("Failed to enrich address {} with coordinates: {}", address.getId(),
                                        e.getMessage());
                            }
                        }
                    } else if (metaDataNode.getClassType().equals("https://ibpdi.datacat.org/class/Building")) {
                        building = mapper.convertValue(metaDataNode.getPropertiesValues(), Building.class);
                        building.setId(metaDataNode.getId());
                    } else if (metaDataNode.getClassType().equals("https://ibpdi.datacat.org/class/GeoCoordinate")) {
                        geoCoordinate = mapper.convertValue(metaDataNode.getPropertiesValues(), GeoCoordinate.class);
                        geoCoordinate.setId(metaDataNode.getId());
                    }

                } catch (Exception e) {
                    log.error("Error processing metadata node {}: {}", metaDataNode.getId(), e.getMessage());
                }
            }

            // Create GeoCoordinate if missing
            if (geoCoordinate == null && address != null
                    && address.getDeprecatedLatitude() != null
                    && address.getDeprecatedLongitude() != null) {
                geoCoordinate = new GeoCoordinate();
                geoCoordinate.setId(UUID.randomUUID().toString());
                geoCoordinate.setLatitude(address.getDeprecatedLatitude());
                geoCoordinate.setLongitude(address.getDeprecatedLongitude());
                geoCoordinate.setCoordinateReferenceSystem("EPSG:4326");

                // Send GeoCoordinate to Kafka
                sendGeoCoordinate(geoCoordinate, graphDataModel, address.getId());
            }

            // Link Address, GeoCoordinate and Building
            if (address != null && building != null) {
                address.setGeoCoordinate(geoCoordinate);
                building.setAddress(address);
                buildings.add(building);

                buildingService.addBuilding(building);
                buildingService.saveGraphDataModelForBuilding(building.getId(), graphDataModel);
                log.debug("Added or replaced building with ID: {}", building.getId());
            }

            // Add AccessRights from graphDataModel if not already present
            if (graphDataModel.getAccessRights() != null) {
                for (AccessRight accessRight : graphDataModel.getAccessRights()) {
                    boolean exists = accessRightsService.getAllAccessRights().stream()
                        .anyMatch(existing -> existing.getId().equals(accessRight.getId()));
                    if (!exists) {
                        accessRightsService.addAccessRight(accessRight);
                        log.info("Added new AccessRight with ID: {}", accessRight.getId());
                    }
                }
            }

            log.info("Successfully processed {} buildings from Kafka message", buildings.size());

        } catch (Exception e) {
            log.error("Critical error processing Kafka message: {}", e.getMessage(), e);
        }
    }

    public void sendGeoCoordinate(GeoCoordinate geoCoordinate, GraphDataModel graphDataModel, String addressId) {
        try {
            // Create MetaDataNode for GeoCoordinate
            MetaDataNode geoCoordinateNode = new MetaDataNode();
            geoCoordinateNode.setId(geoCoordinate.getId());
            geoCoordinateNode.setClassType("https://ibpdi.datacat.org/class/GeoCoordinate");

            // Set properties for GeoCoordinate
            geoCoordinateNode.getPropertiesValues().put("Latitude", geoCoordinate.getLatitude());
            geoCoordinateNode.getPropertiesValues().put("Longitude", geoCoordinate.getLongitude());
            geoCoordinateNode.getPropertiesValues().put("CoordinateReferenceSystem", geoCoordinate.getCoordinateReferenceSystem());

            // Add the MetaDataNode to the provided GraphDataModel
            graphDataModel.getGraphMetadata().add(geoCoordinateNode);
            String graph = graphDataModel.getGraphData() + "\ninst:" + addressId + " <https://ibpdi.datacat.org/class/hasGeoCoordinate> inst:" + geoCoordinate.getId() + ".\n";
            graphDataModel.setGraphData(graph);

            // Convert GraphDataModel to JSON
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(graphDataModel);

            // Send the structured message
            String key = UUID.randomUUID().toString();
            kafkaTemplate.send(kafkaTopic, key, message);
            log.info("Sent structured GeoCoordinate message to Kafka topic {}", kafkaTopic);
        } catch (Exception e) {
            log.error("Failed to send structured GeoCoordinate message: {}", e.getMessage());
        }
    }

    public void sendBuildingAttributes(Building building) {
        try {
            GraphDataModel graphDataModel = buildingService.getGraphDataModelForBuilding(building.getId());

            // Find the MetaDataNode for the building
            MetaDataNode buildingAttributesNode = graphDataModel.getGraphMetadata().stream()
                .filter(node -> node.getId().equals(building.getId()))
                .findFirst()
                .orElse(null);

            if (buildingAttributesNode == null) {
                log.warn("No MetaDataNode found for building with ID: {}", building.getId());
                return;
            }

            // Set additional attributes as properties
            if (building.getAdditionalAttributes() != null) {
                building.getAdditionalAttributes().forEach((key, value) -> {
                    if (!buildingAttributesNode.getPropertiesValues().containsKey(key)) {
                        buildingAttributesNode.getPropertiesValues().put(key, value);
                    }
                });
            }

            // Convert GraphDataModel to JSON
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(graphDataModel);

            // Send the structured message
            String key = UUID.randomUUID().toString();
            kafkaTemplate.send(kafkaTopic, key, message);
            log.info("Sent structured BuildingAttributes message to Kafka topic {}", kafkaTopic);
        } catch (Exception e) {
            log.error("Failed to send structured BuildingAttributes message: {}", e.getMessage());
        }
    }


}
