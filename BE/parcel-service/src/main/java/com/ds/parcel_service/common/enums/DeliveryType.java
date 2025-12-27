package com.ds.parcel_service.common.enums;

/**
 * DeliveryType enum với priority mapping:
 * ECONOMY: 0-1 (priority = 0)
 * NORMAL: 2-4 (priority = 3)
 * FAST: 5-6 (priority = 5)
 * EXPRESS: 7-9 (priority = 8)
 * URGENT: 10 (priority = 10)
 */
public enum DeliveryType {
    ECONOMY(0), NORMAL(3), FAST(5), EXPRESS(8), URGENT(10);

    private final int priority;
    
    private DeliveryType(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() {
        return priority;
    }
}
