package com.ds.session.session_service.business.v1.services;

import java.util.Arrays;
import java.util.List;

public class ParcelMock {
        public static class Location {
            public String street;
            public String district;
            public String city;

            public Location(String street, String district, String city) {
                this.street = street;
                this.district = district;
                this.city = city;
            }

            @Override
            public String toString() {
                return street + ", " + district + ", " + city;
            }
        }

        public static class Parcel {
            public String parcelId;
            public double weight;
            public String receiverName;
            public String receiverPhone;
            public Location deliveryLocation;
            public String note;

            public Parcel(String parcelId, double weight, String receiverName,
                          String receiverPhone, Location deliveryLocation, String note) {
                this.parcelId = parcelId;
                this.weight = weight;
                this.receiverName = receiverName;
                this.receiverPhone = receiverPhone;
                this.deliveryLocation = deliveryLocation;
                this.note = note;
            }
        }

        public static List<Parcel> getParcels() {
            return Arrays.asList(
                    new Parcel("P1001", 2.5, "Nguyen Van A", "0901234567",
                            new Location("Nguyen Trai", "Quan 1", "Ho Chi Minh"), "Giao sau 17h"),
                    new Parcel("P1002", 1.2, "Tran Thi B", "0912345678",
                            new Location("Le Loi", "Quan 3", "Ho Chi Minh"), "De o bao ve"),
                    new Parcel("P1003", 3.0, "Le Van C", "0923456789",
                            new Location("Tran Hung Dao", "Quan 5", "Ho Chi Minh"), "Goi truoc khi giao")
            );
        }
    }
