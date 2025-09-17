package com.gisbackend.buildingstreamer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.gisbackend.buildingstreamer.model.Address;
import com.gisbackend.buildingstreamer.model.Building;
import com.gisbackend.buildingstreamer.model.GraphDataModel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BuildingService {

    private final Map<String, Building> buildingStorage = new ConcurrentHashMap<>();
    private final Map<String, GraphDataModel> buildingGraphDataStorage = new HashMap<>();

    @Lazy
    @Autowired
    private KafkaService kafkaService;

    public void addBuilding(Building building) {
        buildingStorage.put(building.getId(), building);
    }

    public List<Building> getAllBuildings() {
        return buildingStorage.values().stream().collect(Collectors.toList());
    }

    public Building getBuildingById(String id) {
        return buildingStorage.get(id);
    }

    public List<Building> getBuildingsByCity(String city) {
        return buildingStorage.values().stream()
            .filter(building -> building.getAddress() != null && 
                    city.equalsIgnoreCase(building.getAddress().getCity()))
            .collect(Collectors.toList());
    }

    public List<Building> getBuildingsByEnergyClass(String energyClass) {
        return buildingStorage.values().stream()
            .filter(building -> energyClass.equalsIgnoreCase(building.getEnergyEfficiencyClass()))
            .collect(Collectors.toList());
    }

    public Map<String, Long> getBuildingCountByType() {
        return buildingStorage.values().stream()
            .filter(building -> building.getPrimaryTypeOfBuilding() != null)
            .collect(Collectors.groupingBy(
                Building::getPrimaryTypeOfBuilding,
                Collectors.counting()
            ));
    }

    public Address getAddressByBuildingId(String id) {
        Building building = buildingStorage.get(id);
        return (building != null) ? building.getAddress() : null;
    }

    public boolean addAttributesToBuilding(String buildingId, Map<String, String> attributes) {
        Building building = buildingStorage.get(buildingId);
        if (building != null) {
            if (building.getAdditionalAttributes() == null) {
                building.setAdditionalAttributes(new java.util.HashMap<>());
            }
            building.getAdditionalAttributes().putAll(attributes);
            log.info("Added attributes to building with ID: {}", buildingId);
            log.info("Current attributes: {}", building.getAdditionalAttributes());

            // Notify KafkaService to send updated attributes
            kafkaService.sendBuildingAttributes(building);

            return true;
        }
        return false;
    }

    public void saveGraphDataModelForBuilding(String buildingId, GraphDataModel graphDataModel) {
        buildingGraphDataStorage.put(buildingId, graphDataModel);
        log.info("Saved GraphDataModel for building with ID: {}", buildingId);
    }

    public GraphDataModel getGraphDataModelForBuilding(String buildingId) {
        return buildingGraphDataStorage.get(buildingId);
    }
}