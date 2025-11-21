# API Documentation: Zone Proxy Controller (v1)

**Base Path:** `/api/v1`

This controller is a large proxy to the `zone-service`, covering zones, centers, routing, addresses, and OSRM management.

## Zone Endpoints

| Method   | Path                         | Business Function                                  |
|----------|------------------------------|----------------------------------------------------|
| `POST`   | `/zones`                     | List zones based on a query.                       |
| `GET`    | `/zones/{id}`                | Get a zone by its ID.                              |
| `GET`    | `/zones/code/{code}`         | Get a zone by its code.                            |
| `GET`    | `/zones/center/{centerId}`   | Get zones belonging to a center.                   |
| `POST`   | `/zones/create`              | Create a new zone.                                 |
| `PUT`    | `/zones/{id}`                | Update a zone.                                     |
| `DELETE` | `/zones/{id}`                | Delete a zone.                                     |
| `GET`    | `/zones/filterable-fields`   | Get fields that can be used for filtering zones.   |
| `GET`    | `/zones/sortable-fields`     | Get fields that can be used for sorting zones.     |
| `GET`    | `/zone/health`               | Health check for the zone service.                 |

## Center Endpoints

| Method   | Path                   | Business Function        |
|----------|------------------------|--------------------------|
| `GET`    | `/centers`             | List centers.            |
| `GET`    | `/centers/{id}`        | Get a center by its ID.  |
| `GET`    | `/centers/code/{code}` | Get a center by its code.|
| `POST`   | `/centers`             | Create a new center.     |
| `PUT`    | `/centers/{id}`        | Update a center.         |
| `DELETE` | `/centers/{id}`        | Delete a center.         |

## Routing Endpoints

| Method | Path                   | Business Function                      |
|--------|------------------------|----------------------------------------|
| `POST` | `/routing/route`       | Calculate a route.                     |
| `POST` | `/routing/demo-route`  | Calculate a demo route.                |
| `GET`  | `/routing/osrm-status` | Get the status of the OSRM engine.     |

## Address Endpoints

| Method   | Path                          | Business Function                                |
|----------|-------------------------------|--------------------------------------------------|
| `GET`    | `/addresses`                  | List addresses.                                  |
| `GET`    | `/addresses/{id}`             | Get an address by its ID.                        |
| `GET`    | `/addresses/nearest`          | Get nearest addresses to a point.                |
| `GET`    | `/addresses/by-point`         | Get addresses within a certain radius of a point.|
| `GET`    | `/addresses/search`           | Search for addresses.                            |
| `POST`   | `/addresses`                  | Create a new address.                            |
| `POST`   | `/addresses/get-or-create`    | Get an existing address or create a new one.     |
| `PUT`    | `/addresses/{id}`             | Update an address.                               |
| `DELETE` | `/addresses/{id}`             | Delete an address.                               |
| `POST`   | `/addresses/batch`            | Batch import addresses.                          |
| `GET`    | `/addresses/segments/{segmentId}` | Get addresses belonging to a road segment.     |
| `GET`    | `/addresses/zones/{zoneId}`   | Get addresses within a zone.                     |

## OSRM Management Endpoints

| Method   | Path                      | Business Function                             |
|----------|---------------------------|-----------------------------------------------|
| `POST`   | `/osrm/build/{instanceId}`  | Trigger a build for an OSRM instance.         |
| `POST`   | `/osrm/build-all`         | Trigger a build for all OSRM instances.       |
| `POST`   | `/osrm/start/{instanceId}`  | Start an OSRM instance.                       |
| `POST`   | `/osrm/stop/{instanceId}`   | Stop an OSRM instance.                        |
| `POST`   | `/osrm/rolling-restart`   | Perform a rolling restart of OSRM instances.  |
| `GET`    | `/osrm/status`            | Get the status of all OSRM instances.         |
| `GET`    | `/osrm/status/{instanceId}` | Get the status of a specific OSRM instance.   |
| `GET`    | `/osrm/health`            | Health check for OSRM.                        |
| `GET`    | `/osrm/validate/{instanceId}` | Validate the data for an OSRM instance.       |
