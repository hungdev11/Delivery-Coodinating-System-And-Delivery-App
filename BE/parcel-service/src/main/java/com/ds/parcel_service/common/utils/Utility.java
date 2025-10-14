package com.ds.parcel_service.common.utils;

import java.util.UUID;

public class Utility {
    public static UUID toUUID(String s) {
        return UUID.fromString(s);
    }
}
