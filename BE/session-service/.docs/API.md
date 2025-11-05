# Session Service API Reference

This document provides a summary of the Session Service API.

## 1. Delivery Assignment API (`/api/v1/assignments`)

Manages the assignment of delivery tasks to delivery personnel.

### Accept Task

*   **URL**: `/{parcelId}/accept`
*   **Method**: `POST`
*   **URL Params**: `parcelId=[UUID]`
*   **Query Params**: `deliveryManId=[UUID]`

### Complete Task

*   **URL**: `/{parcelId}/complete`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`
*   **Query Params**: `deliveryManId=[UUID]`
*   **Request Body**: `RouteInfo` object.

### Fail Task

*   **URL**: `/{parcelId}/fail`
*   **Method**: `PUT`
*   **URL Params**: `parcelId=[UUID]`
*   **Query Params**:
    *   `deliveryManId=[UUID]`
    *   `reason=[string]`
    *   `flag=[boolean]`
*   **Request Body**: `RouteInfo` object.

### Get Daily Tasks

*   **URL**: `/today/{deliveryManId}`
*   **Method**: `GET`
*   **URL Params**: `deliveryManId=[UUID]`
*   **Query Params**: `status=[string]`, `page=[int]`, `size=[int]`

### Get Tasks

*   **URL**: `/{deliveryManId}`
*   **Method**: `GET`
*   **URL Params**: `deliveryManId=[UUID]`
*   **Query Params**: `status=[string]`, `createdAtStart=[date]`, `createdAtEnd=[date]`, `completedAtStart=[date]`, `completedAtEnd=[date]`, `page=[int]`, `size=[int]`

## 2. QR Code API (`/api/v1/qr`)

Provides QR code generation and decoding functionality.

### Generate QR Code

*   **URL**: `/generate`
*   **Method**: `GET`
*   **Query Params**: `data=[string]`
*   **Returns**: A PNG image of the QR code.

### Decode QR Code

*   **URL**: `/decode`
*   **Method**: `POST`
*   **Request Body**: A multipart form data with a file upload named `file`.
