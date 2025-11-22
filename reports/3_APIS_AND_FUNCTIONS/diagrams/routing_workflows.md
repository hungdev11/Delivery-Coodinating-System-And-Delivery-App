# Routing Workflows

This document contains sequence diagrams for all routing and geographic operations in the Delivery System.

## Calculate Route

This diagram illustrates the sequence of interactions for calculating a route.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (e.g., ManagementSystem)
    participant APIGateway as API Gateway
    participant ZoneService as Zone Service
    participant OSRM as OSRM Routing Engine

    User->>Client: Requests route calculation (waypoints)
    Client->>APIGateway: POST /api/v1/routing/route (waypoints)
    APIGateway->>ZoneService: Calculate Route (waypoints)
    ZoneService->>OSRM: Request Route (waypoints)
    OSRM-->>ZoneService: Route Data
    ZoneService-->>APIGateway: Route Data
    APIGateway-->>Client: Route Data
    Client-->>User: Displays Route
```

## Get OSRM Status

This diagram illustrates the sequence of interactions for retrieving the OSRM routing engine status.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (e.g., ManagementSystem)
    participant APIGateway as API Gateway
    participant ZoneService as Zone Service
    participant OSRM as OSRM Routing Engine

    User->>Client: Requests OSRM status
    Client->>APIGateway: GET /api/v1/routing/osrm-status
    APIGateway->>ZoneService: Get OSRM Status
    ZoneService->>OSRM: Query OSRM Status
    OSRM-->>ZoneService: OSRM Status Data
    ZoneService-->>APIGateway: OSRM Status Data
    APIGateway-->>Client: OSRM Status Data
    Client-->>User: Displays OSRM Status
```

## Detailed Routing Creation

This diagram illustrates the detailed sequence of interactions for calculating a route via the `/api/v1/routing/route` endpoint.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (e.g., ManagementSystem)
    participant APIGateway as API Gateway
    participant ZoneService as Zone Service
    participant OSRM as OSRM Routing Engine
    participant Database as Zone Service Database

    User->>Client: Requests route calculation (origin, destination, waypoints)
    Client->>APIGateway: POST /api/v1/routing/route (requestBody)
    APIGateway->>ZoneService: Forward Request (requestBody)

    activate ZoneService
    ZoneService->>ZoneService: Validate input (origin, destination, waypoints)
    alt Input Valid
        ZoneService->>Database: Retrieve relevant zone/address data (e.g., speed profiles)
        Database-->>ZoneService: Zone/Address Data
        ZoneService->>OSRM: Prepare OSRM request (coordinates, options)
        OSRM-->>ZoneService: OSRM Route Response (geometry, duration, distance)
        ZoneService->>ZoneService: Process OSRM response (e.g., error handling, format)
        ZoneService-->>APIGateway: Formatted Route Data
    else Input Invalid
        ZoneService-->>APIGateway: Error Response (e.g., 400 Bad Request)
    end
    deactivate ZoneService

    APIGateway-->>Client: Route Data / Error Response
    Client-->>User: Displays Route / Error Message
```
