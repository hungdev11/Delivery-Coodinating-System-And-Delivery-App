package com.ds.parcel_service.common.enums;

public enum ParcelEvent {
    POSTPONE("Parcel postpone"), SCAN_QR("Driver scan QR"),
    DELIVERY_SUCCESSFUL("Driver done delivery job"),
    CONFIRM_REMINDER("Remind customer to confirm parcel is received"),
    CUSTOMER_RECEIVED("Customer confirm parcel is received"), CONFIRM_TIMEOUT("Timeout if not confirm parcel received within a day"),
    ACCIDENT("Accident, parcel damage"),
    CUSTOMER_REJECT("Customer refused to receive parcel within a day"),
    CAN_NOT_DELIVERY("Phantom parcel's address or unable to contact with customer")
    
    ;
    String description;
    ParcelEvent(String description) {
        this.description = description;
    }
}
