# API Gateway API Reference

This document details the APIs exposed by the API Gateway. The gateway acts as a unified entry point and a proxy to the downstream microservices.

## 1. Authentication API (`/api/v1/auth`)

Handles user authentication and token management with Keycloak.

*   `POST /login`: Login with username/password.
*   `POST /login/default`: Login with default realm/client.
*   `POST /login/custom`: Login with custom realm/client.
*   `POST /validate-token`: Validate a JWT.
*   `POST /refresh-token`: Refresh an access token.
*   `POST /logout`: Logout and invalidate a refresh token.
*   `GET /me`: Get the current user's info from their JWT.
*   `POST /sync`: Sync user data from Keycloak to the User Service.

## 2. User Service API (`/api/v1/users`)

Proxies requests to the User Service.

*   `POST /`: Get a paginated list of users with advanced filtering.
*   `POST /create`: Create a new user.
*   `GET /{id}`: Get a user by ID.
*   `GET /username/{username}`: Get a user by username.
*   `PUT /{id}`: Update a user.
*   `DELETE /{id}`: Delete a user.

## 3. Zone Service API (`/api/v1`)

Proxies requests to the Zone Service.

### Zones
*   `POST /zones`: List zones with filtering.
*   `GET /zones/{id}`: Get zone by ID.
*   ... (and other zone endpoints)

### Centers
*   `GET /centers`: List distribution centers.
*   ... (and other center endpoints)

### Routing
*   `POST /routing/route`: Calculate a route.
*   ...

(This is a summary. The Zone Service has many endpoints, all proxied through the gateway.)

## 4. Parcel Service API (`/api/v1/parcels`)

Proxies requests to the Parcel Service.

*   `POST /`: Create a parcel.
*   `GET /`: Get a list of parcels with filtering.
*   `GET /{parcelId}`: Get a parcel by ID.
*   `PUT /{parcelId}`: Update a parcel.
*   `PUT /change-status/{parcelId}`: Change a parcel's status.
*   ...

## 5. Session Service API (`/api/v1/session`)

Proxies requests to the Session Service, which manages delivery tasks.

*   `POST /tasks/{parcelId}/accept`: Accept a delivery task.
*   `POST /tasks/{taskId}/complete`: Mark a task as complete.
*   `POST /tasks/{taskId}/fail`: Mark a task as failed.
*   `GET /delivery-man/{deliveryManId}/tasks`: Get tasks for a delivery person.
*   ...

## 6. Settings Service API (`/api/v1/settings`)

Proxies requests to the Settings Service.

*   `POST /`: List all settings with filtering.
*   `GET /{group}`: Get all settings for a group.
*   `GET /{group}/{key}`: Get a specific setting.
*   `PUT /{group}/{key}`: Create or update a setting.
*   `DELETE /{group}/{key}`: Delete a setting.

## 7. Health Check API (`/api/v1/health`)

*   `GET /`: Returns the health status of the API Gateway itself.
