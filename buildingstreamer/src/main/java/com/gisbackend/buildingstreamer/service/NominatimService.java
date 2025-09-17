package com.gisbackend.buildingstreamer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gisbackend.buildingstreamer.model.Address;

@Service
public class NominatimService {

    private static final Logger logger = LoggerFactory.getLogger(NominatimService.class);
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds
    private static final long REQUEST_DELAY_MS = 1500; // 1.5 seconds between requests
    
    @Value("${app.geocoding.enabled:true}")
    private boolean geocodingEnabled;
    
    @Autowired
    @Qualifier("nominatimRestTemplate")
    private RestTemplate restTemplate;

    /**
     * Enriches an address with latitude and longitude coordinates if they are missing.
     * Uses retry logic and graceful fallback to prevent blocking the main application flow.
     * 
     * @param address The address to enrich with coordinates
     * @return The address with coordinates (if found) or the original address if geocoding failed
     */
    public Address enrichAddressWithCoordinates(Address address) {
        // Check if geocoding is enabled
        if (!geocodingEnabled) {
            logger.debug("Geocoding is disabled, skipping address {}", address.getId());
            return address;
        }
        
        // Check if coordinates are already present
        if (hasCoordinates(address)) {
            logger.debug("Address {} already has coordinates", address.getId());
            return address;
        }

        try {
            // Build the search query from address components
            String query = buildAddressQuery(address);
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Cannot build address query for address {}", address.getId());
                return address;
            }

            // Make request to Nominatim with retry logic
            NominatimResponse[] responses = searchCoordinatesWithRetry(query);
            
            if (responses != null && responses.length > 0) {
                NominatimResponse bestMatch = responses[0]; // Take the first (best) result
                address.setDeprecatedLatitude(bestMatch.getLat());
                address.setDeprecatedLongitude(bestMatch.getLon());
                // logger.info("Successfully geocoded address {} - lat: {}, lon: {}", 
                    // address.getId(), bestMatch.getLat(), bestMatch.getLon());
            } else {
                logger.warn("No coordinates found for address {}: {}", address.getId(), query);
            }
        } catch (Exception e) {
            logger.error("Error geocoding address {} (continuing without coordinates): {}", 
                address.getId(), e.getMessage());
            // Continue processing without coordinates - don't let geocoding failures stop the main flow
        }

        return address;
    }

    /**
     * Checks if an address already has coordinates.
     */
    private boolean hasCoordinates(Address address) {
        return address.getDeprecatedLatitude() != null && !address.getDeprecatedLatitude().trim().isEmpty() &&
               address.getDeprecatedLongitude() != null && !address.getDeprecatedLongitude().trim().isEmpty();
    }

    /**
     * Builds a search query string from address components.
     */
    private String buildAddressQuery(Address address) {
        StringBuilder query = new StringBuilder();
        
        if (address.getHouseNumber() != null && !address.getHouseNumber().trim().isEmpty()) {
            query.append(address.getHouseNumber()).append(" ");
        }
        
        if (address.getStreetName() != null && !address.getStreetName().trim().isEmpty()) {
            query.append(address.getStreetName()).append(", ");
        }
        
        if (address.getCity() != null && !address.getCity().trim().isEmpty()) {
            query.append(address.getCity()).append(", ");
        }
        
        if (address.getPostalCode() != null && !address.getPostalCode().trim().isEmpty()) {
            query.append(address.getPostalCode()).append(", ");
        }
        
        if (address.getCountry() != null && !address.getCountry().trim().isEmpty()) {
            query.append(address.getCountry());
        }
        
        // Remove trailing comma and space
        String result = query.toString().trim();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        
        return result;
    }

    /**
     * Makes the actual request to Nominatim API with retry logic.
     */
    private NominatimResponse[] searchCoordinatesWithRetry(String query) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Geocoding attempt {} for query: {}", attempt, query);
                
                // Add delay to respect Nominatim usage policy
                Thread.sleep(REQUEST_DELAY_MS);
                
                NominatimResponse[] result = searchCoordinates(query);
                
                if (result != null && result.length > 0) {
                    logger.info("Geocoding successful on attempt {} - found {} results", attempt, result.length);
                    return result;
                } else {
                    logger.warn("Geocoding attempt {} returned empty result for query: {}", attempt, query);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted during geocoding attempt {}", attempt, e);
                break;
            } catch (RestClientException e) {
                logger.warn("Geocoding attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        logger.info("Retrying in {} ms...", RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread interrupted during retry delay", ie);
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Unexpected error during geocoding attempt {}: {}", attempt, e.getMessage(), e);
                break;
            }
        }
        
        logger.error("Failed to geocode after {} attempts: {}", MAX_RETRIES, query);
        return null;
    }

    /**
     * Makes the actual request to Nominatim API.
     */
    private NominatimResponse[] searchCoordinates(String query) {
        try {
            String url = UriComponentsBuilder.fromUriString(NOMINATIM_BASE_URL)
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("limit", "1")
                    .queryParam("addressdetails", "1")
                    .queryParam("user-agent", "GIS-Backend/1.0 (your-email@example.com)") // Important for Nominatim
                    .build()
                    .toUriString();

            logger.info("Making Nominatim request: {}", url);
            
            NominatimResponse[] response = restTemplate.getForObject(url, NominatimResponse[].class);
            
            if (response != null) {
                logger.info("Nominatim response: {} results returned", response.length);
                if (response.length > 0) {
                    logger.info("First result: lat={}, lon={}, display_name={}", 
                        response[0].getLat(), response[0].getLon(), response[0].getDisplayName());
                }
            } else {
                logger.warn("Nominatim response is null for URL: {}", url);
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error making request to Nominatim: {}", e.getMessage(), e);
            throw new RestClientException("Error making request to Nominatim: " + e.getMessage(), e);
        }
    }

    /**
     * Inner class to represent Nominatim API response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NominatimResponse {
        
        @JsonProperty("lat")
        private String lat;
        
        @JsonProperty("lon")
        private String lon;
        
        @JsonProperty("display_name")
        private String displayName;
        
        public String getLat() {
            return lat;
        }
        
        public void setLat(String lat) {
            this.lat = lat;
        }
        
        public String getLon() {
            return lon;
        }
        
        public void setLon(String lon) {
            this.lon = lon;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
