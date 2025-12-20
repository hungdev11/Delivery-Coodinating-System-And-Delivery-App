package com.ds.parcel_service.common.enums;

public enum ParcelEvent {
    SCAN_QR("Driver scan QR"),
    POSTPONE("Parcel postpone"), 
    END_SESSION("All task done"),
    DELIVERY_SUCCESSFUL("Driver done delivery job"),
    CONFIRM_REMINDER("Remind customer to confirm parcel is received"),
    CUSTOMER_RECEIVED("Customer confirm parcel is received"), 
    CONFIRM_TIMEOUT("Timeout if not confirm parcel received within 2 days"),
    CUSTOMER_REJECT("Customer refused to receive parcel within 2 days"),
    CAN_NOT_DELIVERY("Accident, phantom parcel's address or unable to contact with customer"),
    CUSTOMER_CONFIRM_NOT_RECEIVED("Customer confirm that not received the parcel yet"),
    CUSTOMER_RETRACT_DISPUTE("Customer retract dispute after confirming received"),
    MISSUNDERSTANDING_DISPUTE("Customer mistake, shipper have evident data"),
    FAULT_DISPUTE("Shipper mistake, no clear evident"),
    
    ;
    String description;
    ParcelEvent(String description) {
        this.description = description;
    }
}
