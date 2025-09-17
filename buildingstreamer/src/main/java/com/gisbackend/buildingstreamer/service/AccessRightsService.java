package com.gisbackend.buildingstreamer.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.gisbackend.buildingstreamer.model.AccessRight;

@Service
public class AccessRightsService {

    private final List<AccessRight> accessRights = new ArrayList<>();

    // Retrieve all AccessRights
    public List<AccessRight> getAllAccessRights() {
        return new ArrayList<>(accessRights);
    }

    // Retrieve AccessRights by GuidelineClassificationId
    public List<AccessRight> getAccessRightsByGuidelineClassificationId(String guidelineClassificationId) {
        List<AccessRight> filteredAccessRights = new ArrayList<>();
        for (AccessRight accessRight : accessRights) {
            if (accessRight.getGuidelineClassificationId().equals(guidelineClassificationId)) {
                filteredAccessRights.add(accessRight);
            }
        }
        return filteredAccessRights;
    }

    // Add a new AccessRight
    public void addAccessRight(AccessRight accessRight) {
        accessRights.add(accessRight);
    }

    // Clear all AccessRights (for testing or reset purposes)
    public void clearAccessRights() {
        accessRights.clear();
    }
}
