package com.ds.session.session_service.common.interfaces;

import com.ds.session.session_service.common.entities.dto.request.LocationUpdateRequest;

public interface IShipperLocationTrackingService {
    void addTrackingPoint(String sessionId, LocationUpdateRequest request);
    void clearCache(String sessionId);
}
