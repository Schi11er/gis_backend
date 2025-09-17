package com.gisbackend.buildingstreamer.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gisbackend.buildingstreamer.model.AccessRight;
import com.gisbackend.buildingstreamer.model.Address;
import com.gisbackend.buildingstreamer.model.Building;
import com.gisbackend.buildingstreamer.model.BuildingAttributeRequest;
import com.gisbackend.buildingstreamer.service.AccessRightsService;
import com.gisbackend.buildingstreamer.service.BuildingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Building Information", description = "Get Building Information")
@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    @Autowired
    private BuildingService buildingService;
    
    @Autowired
    private AccessRightsService accessRightsService;

    @Operation(summary = "Get all buildings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Building.class))),
        @ApiResponse(responseCode = "404", description = "No buildings found")
    })
    @GetMapping
    public ResponseEntity<List<Building>> getAllBuildings() {
        List<Building> buildings = buildingService.getAllBuildings();
        return ResponseEntity.ok(buildings);
    }

    @Operation(summary = "Get a building by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Building.class))),
        @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@Parameter(description = "ID of the building to retrieve", required = true) @PathVariable String id) {
        Building building = buildingService.getBuildingById(id);
        if (building != null) {
            return ResponseEntity.ok(building);
        }
        return ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "Get all cities with buildings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "404", description = "No cities found")
    })
    @GetMapping("/cities")
    public ResponseEntity<Set<String>> getAllCities() {
        List<Building> buildings = buildingService.getAllBuildings();
        Set<String> cities = buildings.stream()
            .map(building -> building.getAddress().getCity())
            .filter(city -> city != null && !city.isEmpty())
            .collect(Collectors.toSet());
        return ResponseEntity.ok(cities);
    }

    @Operation(summary = "Get all buildings in a specific city")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Building.class))),
        @ApiResponse(responseCode = "404", description = "No buildings found in the specified city")
    })
    @GetMapping("/by-city/{city}")
    public ResponseEntity<List<Building>> getBuildingsByCity(@Parameter(description = "City to filter buildings", required = true) @PathVariable String city) {
        List<Building> buildings = buildingService.getBuildingsByCity(city);
        return ResponseEntity.ok(buildings);
    }

    @Operation(summary = "Get all buildings with a specific energy efficiency class")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Building.class))),
        @ApiResponse(responseCode = "404", description = "No buildings found with the specified energy class")
    })
    @GetMapping("/by-energy-class/{energyClass}")
    public ResponseEntity<List<Building>> getBuildingsByEnergyClass(@Parameter(description = "Energy class to filter buildings", required = true) @PathVariable String energyClass) {
        List<Building> buildings = buildingService.getBuildingsByEnergyClass(energyClass);
        return ResponseEntity.ok(buildings);
    }

    @Tag(name = "Statistics", description = "Get Building Statistics")
    @Operation(summary = "Get building count by type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = java.util.Map.class))),
        @ApiResponse(responseCode = "404", description = "No buildings found")
    })
    @GetMapping("/statistics/by-building-type")
    public ResponseEntity<java.util.Map<String, Long>> getBuildingCountByType() {
        java.util.Map<String, Long> statistics = buildingService.getBuildingCountByType();
        return ResponseEntity.ok(statistics);
    }

    @Operation(summary = "Get address by building ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Address.class))),
        @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @GetMapping("/address/{id}")
    public ResponseEntity<Address> getAddressByBuildingId(@Parameter(description = "ID of the building to retrieve address", required = true) @PathVariable String id) {
        Address address = buildingService.getAddressByBuildingId(id);
        if (address != null) {
            return ResponseEntity.ok(address);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Add additional attributes to a building")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attributes successfully added"),
        @ApiResponse(responseCode = "404", description = "Building not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/{id}/attributes")
    public ResponseEntity<String> addAttributesToBuilding(
            @Parameter(description = "ID of the building to add attributes to", required = true) 
            @PathVariable String id,
            @RequestBody BuildingAttributeRequest request) {
        
        if (request == null || request.getAttributes() == null || request.getAttributes().isEmpty()) {
            return ResponseEntity.badRequest().body("Attributes cannot be empty");
        }
        
        boolean success = buildingService.addAttributesToBuilding(id, request.getAttributes());
        if (success) {
            return ResponseEntity.ok("Attributes successfully added to building with ID: " + id);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get AccessRights by GuidelineClassificationId")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AccessRight.class))),
        @ApiResponse(responseCode = "404", description = "No AccessRights found for the specified GuidelineClassificationId")
    })
    @GetMapping("/access-rights/class")
    public ResponseEntity<List<AccessRight>> getAccessRightsByClass(
            @Parameter(description = "ClassUri to filter AccessRights", required = true)
            @RequestParam String classUri) {
        log.info("Fetching AccessRights for ClassUri: {}", classUri);
        List<AccessRight> accessRights = accessRightsService.getAccessRightsByGuidelineClassificationId(classUri);
        if (accessRights != null && !accessRights.isEmpty()) {
            return ResponseEntity.ok(accessRights);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get all AccessRights")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AccessRight.class)))
    })
    @GetMapping("/access-rights")
    public ResponseEntity<List<AccessRight>> getAllAccessRights() {
        List<AccessRight> accessRights = accessRightsService.getAllAccessRights();
        return ResponseEntity.ok(accessRights);
    }
}