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

## 5. Session Service API (`/api/v1/sessions`)

Proxies requests to the Session Service, which manages delivery tasks.

### Basic Endpoints (Direct Proxy)
*   `GET /sessions/{sessionId}`: Get basic session information (proxied directly to session-service)
*   `POST /sessions/drivers/{deliveryManId}/accept-parcel`: Accept a parcel to session
*   `POST /sessions/{sessionId}/complete`: Complete a session
*   `POST /sessions/{sessionId}/fail`: Fail a session
*   `POST /sessions`: Create session batch
*   `POST /sessions/drivers/{deliveryManId}/prepare`: Create prepared session (CREATED status)
*   `POST /sessions/{sessionId}/start`: Start session (CREATED -> IN_PROGRESS)
*   `GET /sessions/drivers/{deliveryManId}/active`: Get active session for delivery man
*   `GET /sessions/drivers/{deliveryManId}/sessions`: Get all sessions for delivery man

### Enriched Endpoint (Aggregated)
*   `GET /sessions/{sessionId}/enriched`: Get enriched session with full assignment details
    - **Aggregates data from**: session-service, parcel-service, delivery-proofs
    - **Includes**: 
      - Full shipper information (name, vehicle, phone, email)
      - Complete parcel information for each assignment (receiver, location, value, weight, coordinates)
      - All delivery proofs (images/videos) for each assignment
    - **Use case**: When you need complete session details in a single API call

### Assignment Endpoints (`/api/v1/assignments`)

*   `GET /assignments/session/delivery-man/{deliveryManId}/tasks/today`: Get daily tasks
*   `GET /assignments/session/delivery-man/{deliveryManId}/tasks`: Get task history
*   `POST /assignments/drivers/{deliveryManId}/parcels/{parcelId}/complete`: Complete task
*   `POST /assignments/{assignmentId}/complete`: Complete task by assignmentId (preferred)
*   `POST /assignments/drivers/{deliveryManId}/parcels/{parcelId}/fail`: Fail task
*   `POST /assignments/drivers/{deliveryManId}/parcels/{parcelId}/refuse`: Refuse task
*   `POST /assignments/drivers/{deliveryManId}/parcels/{parcelId}/postpone`: Postpone task
*   `GET /assignments/current-shipper/parcels/{parcelId}`: Get current shipper

### Delivery Proof Endpoints (`/api/v1/delivery-proofs`)

*   `GET /delivery-proofs/assignments/{assignmentId}`: Get all proofs for a specific assignment
*   `GET /delivery-proofs/parcels/{parcelId}`: Get all proofs for a specific parcel (from all assignments)

## 6. Settings Service API (`/api/v1/settings`)

Proxies requests to the Settings Service.

*   `POST /`: List all settings with filtering.
*   `GET /{group}`: Get all settings for a group.
*   `GET /{group}/{key}`: Get a specific setting.
*   `PUT /{group}/{key}`: Create or update a setting.
*   `DELETE /{group}/{key}`: Delete a setting.

## 7. Health Check API (`/api/v1/health`)

*   `GET /`: Returns the health status of the API Gateway itself.
