# Parcel Service API Reference

This document provides a summary of the Parcel Service API.

## 1. Parcel Management

### Create Parcel

*   **URL**: `/api/v1/parcels`
*   **Method**: `POST`
*   **Request Body**: `ParcelCreateRequest` object.

### Update Parcel

*   **URL**: `/api/v1/parcels/{parcelId}`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`
*   **Request Body**: `ParcelUpdateRequest` object.

### Get Parcel by ID

*   **URL**: `/api/v1/parcels/{parcelId}`
*   **Method**: `GET`
*   **URL Params**: `parcelId=[UUID]`

### Get Parcel by Code

*   **URL**: `/api/v1/parcels/code/{code}`
*   **Method**: `GET`
*   **URL Params**: `code=[string]`

### Get Parcels

*   **URL**: `/api/v1/parcels`
*   **Method**: `GET`
*   **Query Params**: Supports filtering by status, delivery type, creation date, and pagination.

### Delete Parcel

*   **URL**: `/api/v1/parcels/{parcelId}`
*   **Method**: `DELETE`
*   **URL Params**: `parcelId=[UUID]`

## 2. Parcel Status Management

### Change Parcel Status

*   **URL**: `/api/v1/parcels/change-status/{parcelId}`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`
*   **Query Params**: `event=[ParcelEvent]` (e.g., `CUSTOMER_RECEIVED`, `ACCIDENT`, `POSTPONE`).

### Confirm Parcel Arrival

*   **URL**: `/api/v1/parcels/confirm/{parcelId}`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`

### Notify Broken/Accident

*   **URL**: `/api/v1/parcels/broken-accident/{parcelId}`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`

### Refuse Parcel

*   **URL**: `/api/v1/parcels/refuse/{parcelId}`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`

### Return to Warehouse

*   **URL**: `/api/v1/parcels/return-to-warehouse/{parcelId}`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`
