# API Documentation

This document lists the identified API endpoints across various microservices, extracted from their respective controller files.

## User Service

### `User_service/src/main/java/com/ds/user/application/controllers/v0/UserControllerV0.java`
- Base Path: `/api/v0/users`
  - `POST /`
  
### `User_service/src/main/java/com/ds/user/application/controllers/v2/UserControllerV2.java`
- Base Path: `/api/v2/users`
  - `GET /me`
  - `POST /`

### `User_service/src/main/java/com/ds/user/application/controllers/v2/DeliveryManControllerV2.java`
- Base Path: `/api/v2/users/shippers`
  - `POST /`

### `User_service/src/main/java/com/ds/user/application/controllers/v1/ParcelSeedController.java`
- Base Path: `/api/v1/parcels/seed`
  - `POST /`

### `User_service/src/main/java/com/ds/user/application/controllers/v1/DeliveryManController.java`
- Base Path: `/api/v1/users`
  - `GET /{userId}/delivery-man`

### `User_service/src/main/java/com/ds/user/application/controllers/v1/UserController.java`
- Base Path: `/api/v1/users`
  - `POST /create`
  - `GET /{id}`
  - `GET /me`
  - `GET /username/{username}`
  - `POST /`
  - `PUT /{id}`
  - `DELETE /{id}`
  - `POST /sync`

### `User_service/src/main/java/com/ds/user/application/controllers/v1/UserAddressController.java`
- Base Path: `/api/v1/users`
  - `POST /me/addresses`
  - `GET /me/addresses`
  - `GET /me/addresses/primary`
  - `GET /me/addresses/{addressId}`
  - `PUT /me/addresses/{addressId}`
  - `DELETE /me/addresses/{addressId}`
  - `PUT /me/addresses/{addressId}/set-primary`
  - `POST /{userId}/addresses`
  - `GET /{userId}/addresses`
  - `GET /{userId}/addresses/primary`
  - `GET /{userId}/addresses/{addressId}`
  - `PUT /{userId}/addresses/{addressId}`
  - `DELETE /{userId}/addresses/{addressId}`

### `User_service/src/main/java/com/ds/user/application/controllers/internal/DeliveryManDumpController.java`
- Base Path: `/internal`
  - `GET /delivery-man-dump`

### `User_service/src/main/java/com/ds/user/application/controllers/internal/UserDumpController.java`
- Base Path: `/internal`
  - `GET /user-dump`

## Settings Service

### `Settings_service/src/main/java/com/ds/setting/application/controllers/v2/SettingsControllerV2.java`
- Base Path: `/api/v2/settings`
  - `POST /`

### `Settings_service/src/main/java/com/ds/setting/application/controllers/v0/SettingsControllerV0.java`
- Base Path: `/api/v0/settings`
  - `POST /`

### `Settings_service/src/main/java/com/ds/setting/application/controllers/v1/HealthController.java`
- Base Path: (none)
  - `GET /health`
  - `GET /actuator/health`

### `Settings_service/src/main/java/com/ds/setting/application/controllers/v1/SettingsController.java`
- Base Path: `/api/v1/settings`
  - `POST /`
  - `GET /{group}`
  - `GET /{group}/{key}`
  - `GET /{group}/{key}/value`
  - `PUT /{group}/{key}`
  - `DELETE /{group}/{key}`
  - `PUT /{group}/bulk`

## Session Service

### `session-service/src/main/java/com/ds/session/session_service/application/controllers/SessionController.java`
- Base Path: `/api/v1/sessions`
  - `POST /drivers/{deliveryManId}/accept-parcel`
  - `GET /{sessionId}`
  - `POST /{sessionId}/complete`
  - `POST /{sessionId}/fail`
  - `POST /`
  - `POST /drivers/{deliveryManId}/prepare`
  - `POST /{sessionId}/start`
  - `GET /drivers/{deliveryManId}/active`
  - `PUT /{sessionId}/assignments/{assignmentId}/status`

### `session-service/src/main/java/com/ds/session/session_service/application/controllers/QRController.java`
- Base Path: `/api/v1/qr`
  - `GET /generate`
  - `POST /decode`

### `session-service/src/main/java/com/ds/session/session_service/application/controllers/v0/DeliveryAssignmentControllerV0.java`
- Base Path: `/api/v0/assignments`
  - `POST /`

### `session-service/src/main/java/com/ds/session/session_service/application/controllers/v2/DeliverySessionControllerV2.java`
- Base Path: `/api/v2/delivery-sessions`
  - `POST /`

### `session-service/src/main/java/com/ds/session/session_service/application/controllers/v2/DeliveryAssignmentControllerV2.java`
- Base Path: `/api/v2/assignments`
  - `POST /`

### `session-service/src/main/java/com/ds/session/session_service/application/controllers/DeliveryAssignmentController.java`
- Base Path: `/api/v1/assignments`
  - `GET /session/delivery-man/{deliveryManId}/tasks/today`
  - `GET /session/{sessionId}/tasks`
  - `GET /session/delivery-man/{deliveryManId}/tasks`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/complete`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/fail`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/refuse`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/postpone`
  - `GET /current-shipper/parcels/{parcelId}`

## Parcel Service

### `parcel-service/src/main/java/com/ds/parcel_service/application/controllers/ParcelController.java`
- Base Path: `/api/v1/parcels`
  - `POST /`
  - `PUT /{parcelId}`
  - `GET /{parcelId}`
  - `GET /code/{code}`
  - `GET /me`
  - `GET /me/receive`
  - `GET /`
  - `DELETE /{parcelId}`
  - `PUT /change-status/{parcelId}`
  - `PUT /deliver/{parcelId}`
  - `PUT /confirm/{parcelId}`
  - `PUT /accident/{parcelId}`
  - `PUT /refuse/{parcelId}`
  - `PUT /postpone/{parcelId}`
  - `PUT /dispute/{parcelId}`
  - `PUT /resolve-dispute/misunderstanding/{parcelId}`
  - `PUT /resolve-dispute/fault/{parcelId}`
  - `POST /bulk`
  - `PUT /{parcelId}/priority`
  - `PUT /{parcelId}/delay`
  - `PUT /{parcelId}/undelay`

### `parcel-service/src/main/java/com/ds/parcel_service/application/controllers/v0/ParcelControllerV0.java`
- Base Path: `/api/v0/parcels`
  - `POST /`

### `parcel-service/src/main/java/com/ds/parcel_service/application/controllers/v2/ParcelControllerV2.java`
- Base Path: `/api/v2/parcels`
  - `POST /`

## Communication Service

### `communication_service/src/main/java/com/ds/communication_service/application/controller/ProposalConfigController.java`
- Base Path: `/api/v1/admin/proposals/configs`
  - `GET /`
  - `POST /`
  - `PUT /{configId}`
  - `DELETE /{configId}`

### `communication_service/src/main/java/com/ds/communication_service/application/controller/NotificationController.java`
- Base Path: `/api/v1/notifications`
  - `GET /`
  - `GET /unread`
  - `GET /unread/count`
  - `PUT /{notificationId}/read`
  - `PUT /read-all`
  - `DELETE /{notificationId}`

### `communication_service/src/main/java/com/ds/communication_service/application/controller/ProposalController.java`
- Base Path: `/api/v1/proposals`
  - `POST /`
  - `POST /{proposalId}/respond`
  - `GET /available-configs`

### `communication_service/src/main/java/com/ds/communication_service/application/controller/ConversationApiController.java`
- Base Path: `/api/v1/conversations`
  - `GET /{conversationId}/messages`
  - `GET /find-by-users`
  - `GET /user/{currentUserId}`

## API Gateway

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/SettingsProxyControllerV2.java`
- Base Path: `/api/v2/settings`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/ZoneProxyControllerV2.java`
- Base Path: `/api/v2/zones`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/UserShipperControllerV2.java`
- Base Path: `/api/v2/users/shippers`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/UserControllerV2.java`
- Base Path: `/api/v2/users`
  - `POST /`
  - `GET /me`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/ParcelServiceControllerV2.java`
- Base Path: `/api/v2/parcels`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/DeliverySessionControllerV2.java`
- Base Path: `/api/v2/delivery-sessions`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/DeliveryAssignmentControllerV2.java`
- Base Path: `/api/v2/assignments`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v2/ClientDeliverySessionControllerV2.java`
- Base Path: `/api/v2/client/delivery-sessions`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/UserController.java`
- Base Path: `/api/v1/users`
  - `GET /me`
  - `POST /`
  - `GET /{id}`
  - `GET /username/{username}`
  - `POST /create`
  - `PUT /{id}`
  - `DELETE /{id}`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/UserAddressProxyController.java`
- Base Path: `/api/v1/users`
  - `POST /me/addresses`
  - `GET /me/addresses`
  - `GET /me/addresses/primary`
  - `GET /me/addresses/{addressId}`
  - `PUT /me/addresses/{addressId}`
  - `DELETE /me/addresses/{addressId}`
  - `PUT /me/addresses/{addressId}/set-primary`
  - `POST /{userId}/addresses`
  - `GET /{userId}/addresses`
  - `GET /{userId}/addresses/primary`
  - `GET /{userId}/addresses/{addressId}`
  - `PUT /{userId}/addresses/{addressId}`
  - `DELETE /{userId}/addresses/{addressId}`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/ZoneProxyController.java`
- Base Path: `/api/v1`
  - `GET /zone/health`
  - `POST /zones`
  - `GET /zones/{id}`
  - `GET /zones/code/{code}`
  - `GET /zones/center/{centerId}`
  - `POST /zones/create`
  - `PUT /zones/{id}`
  - `DELETE /zones/{id}`
  - `GET /zones/filterable-fields`
  - `GET /zones/sortable-fields`
  - `GET /centers`
  - `GET /centers/{id}`
  - `GET /centers/code/{code}`
  - `POST /centers`
  - `PUT /centers/{id}`
  - `DELETE /centers/{id}`
  - `POST /routing/route`
  - `POST /routing/demo-route`
  - `GET /routing/osrm-status`
  - `GET /addresses`
  - `GET /addresses/{id}`
  - `GET /addresses/nearest`
  - `GET /addresses/by-point`
  - `GET /addresses/search`
  - `POST /addresses`
  - `POST /addresses/get-or-create`
  - `PUT /addresses/{id}`
  - `DELETE /addresses/{id}`
  - `POST /addresses/batch`
  - `GET /addresses/segments/{segmentId}`
  - `GET /addresses/zones/{zoneId}`
  - `POST /osrm/build/{instanceId}`
  - `POST /osrm/build-all`
  - `POST /osrm/start/{instanceId}`
  - `POST /osrm/stop/{instanceId}`
  - `POST /osrm/rolling-restart`
  - `GET /osrm/status`
  - `GET /osrm/status/{instanceId}`
  - `GET /osrm/health`
  - `GET /osrm/validate/{instanceId}`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/SettingsProxyController.java`
- Base Path: `/api/v1/settings`
  - `POST /`
  - `GET /{group}`
  - `GET /{group}/{key}`
  - `GET /{group}/{key}/value`
  - `PUT /{group}/{key}`
  - `DELETE /{group}/{key}`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/SessionController.java`
- Base Path: `/api/v1/sessions`
  - `POST /drivers/{deliveryManId}/accept-parcel`
  - `GET /{sessionId}`
  - `POST /{sessionId}/complete`
  - `POST /{sessionId}/fail`
  - `POST /`
  - `POST /drivers/{deliveryManId}/prepare`
  - `POST /{sessionId}/start`
  - `GET /drivers/{deliveryManId}/active`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/QRController.java`
- Base Path: `/api/v1/qr`
  - `GET /generate`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/ParcelServiceController.java`
- Base Path: `/api/v1/parcels`
  - `POST /`
  - `PUT /{parcelId}`
  - `GET /{parcelId}`
  - `GET /code/{code}`
  - `GET /me`
  - `GET /me/receive`
  - `PUT /change-status/{parcelId}`
  - `DELETE /{parcelId}`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/ParcelSeedProxyController.java`
- Base Path: `/api/v1/parcels/seed`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/HealthController.java`
- Base Path: `/api/v1/health`
  - `GET /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/DeliveryAssignmentController.java`
- Base Path: `/api/v1/assignments`
  - `GET /session/delivery-man/{deliveryManId}/tasks/today`
  - `GET /session/{sessionId}/tasks`
  - `GET /session/delivery-man/{deliveryManId}/tasks`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/complete`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/fail`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/refuse`
  - `POST /drivers/{deliveryManId}/parcels/{parcelId}/postpone`
  - `GET /current-shipper/parcels/{parcelId}`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/DeliverySessionController.java`
- Base Path: `/api/v1/delivery-sessions`
  - `GET /{sessionId}/with-assignments`
  - `GET /{sessionId}/demo-route`
  - `PUT /{sessionId}/assignments/{assignmentId}/status`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/CommunicationController.java`
- Base Path: `/api/v1`
  - `GET /conversations/user/{userId}`
  - `GET /conversations`
  - `GET /conversations/find-by-users`
  - `POST /conversations/find-by-users`
  - `GET /conversations/{conversationId}/messages`
  - `POST /proposals`
  - `POST /proposals/{proposalId}/respond`
  - `GET /proposals/available-configs`
  - `GET /admin/proposals/configs`
  - `POST /admin/proposals/configs`
  - `PUT /admin/proposals/configs/{configId}`
  - `DELETE /admin/proposals/configs/{configId}`
  - `POST /messages`
  - `PUT /messages/{messageId}/status`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/ClientDeliverySessionController.java`
- Base Path: `/api/v1/client/delivery-sessions`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/AuthController.java`
- Base Path: `/api/v1/auth`
  - `POST /login`
  - `POST /login/default`
  - `POST /login/custom`
  - `POST /validate-token`
  - `POST /refresh-token`
  - `POST /logout`
  - `GET /me`
  - `POST /sync`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v0/ZoneProxyControllerV0.java`
- Base Path: `/api/v0/zones`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v0/SettingsProxyControllerV0.java`
- Base Path: `/api/v0/settings`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v0/UserControllerV0.java`
- Base Path: `/api/v0/users`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v0/ParcelServiceControllerV0.java`
- Base Path: `/api/v0/parcels`
  - `POST /`

### `api-gateway/src/main/java/com/ds/gateway/application/controllers/v0/DeliveryAssignmentControllerV0.java`
- Base Path: `/api/v0/assignments`
  - `POST /`

## Zone Service

### `BE/zone_service/src/modules/zone/zone.routes.ts`
- Base Path: `/` (implicitly handled by router setup)
  - `POST /` (getZones)
  - `GET /:id` (getZoneById)
  - `POST /create` (createZone)
  - `PUT /:id` (updateZone)
  - `DELETE /:id` (deleteZone)
  - `GET /filterable-fields`
  - `GET /sortable-fields`

### `BE/zone_service/src/modules/zone/v2/zone.routes.v2.ts`
- Base Path: `/` (implicitly handled by router setup)
  - `POST /` (getZones)

### `BE/zone_service/src/modules/zone/v0/zone.routes.v0.ts`
- Base Path: `/` (implicitly handled by router setup)
  - `POST /` (getZones)

### `BE/zone_service/src/modules/routing/routing.router.ts`
- Base Path: `/` (implicitly handled by router setup)
  - `POST /route` (calculateRoute)
  - `POST /priority-route` (calculatePriorityRoute)
  - `GET /simple` (getSimpleRoute)
  - `GET /status` (getOSRMStatus)
  - `POST /switch-instance` (switchOSRMInstance)
  - `POST /demo-route` (calculateDemoRoute)

### `BE/zone_service/src/modules/osrm/osrm-data.router.ts`
- Base Path: `/` (implicitly handled by router setup)
  - `POST /build/:instanceId` (buildInstance)
  - `POST /build-all` (buildAllInstances)
  - `POST /start/:instanceId` (startInstance)
  - `POST /stop/:instanceId` (stopInstance)
  - `POST /rolling-restart` (rollingRestart)
  - `GET /status` (getAllInstancesStatus)
  - `GET /status/:instanceId` (getInstanceStatus)
  - `GET /health` (healthCheck)
  - `GET /validate/:instanceId` (validateData)
  - `GET /history` (getAllBuildHistory)
  - `GET /history/:instanceId` (getBuildHistory)
  - `GET /deployment` (getDeploymentStatus)

### `BE/zone_service/src/modules/address/address.router.ts`
- Base Path: `/` (implicitly handled by router setup)
  - `POST /` (createAddress)
  - `POST /get-or-create` (getOrCreateAddress)
  - `POST /batch` (batchImport)
  - `GET /by-point` (findByPoint)
  - `GET /search` (searchByText)
  - `GET /nearest` (findNearestAddresses)
  - `GET /segments/:segmentId` (getAddressesBySegment)
  - `GET /zones/:zoneId` (getAddressesByZone)
  - `GET /:id` (getAddress)
  - `GET /` (listAddresses)
  - `PUT /:id` (updateAddress)
  - `DELETE /:id` (deleteAddress)

### `BE/zone_service/src/modules/center/center.router.ts`
- Base Path: `/` (implicitly handled by router setup)
  - `GET /` (getAllCenters)
  - `GET /:id` (getCenterById)
  - `GET /code/:code` (getCenterByCode)
  - `POST /` (createCenter)
  - `PUT /:id` (updateCenter)
  - `DELETE /:id` (deleteCenter)

### `BE/zone_service/src/app.ts`
- Base Path: `/`
  - `GET /health` (healthCheck)
  - `GET /health/detailed` (detailedHealthCheck)
  - `GET /health/readiness` (readinessCheck)
  - `GET /health/liveness` (livenessCheck)