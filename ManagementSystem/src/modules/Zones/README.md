# Zones Module

This module handles zone management functionality including CRUD operations and map integration.

## Features

- List all zones with pagination and filtering
- Search zones by name or code
- Filter zones by distribution center
- Create new zones
- Edit existing zones
- Delete zones
- Bulk delete multiple zones
- View zone details with map visualization
- **Interactive map view with zone management**
- **Zone selection with detailed drawer**
- **Visual zone representation with colors**
- Define zone boundaries using polygons

## File Structure

```
Zones/
├── components/
│   ├── ZoneFormModal.vue      # Create/Edit modal
│   ├── ZoneDeleteModal.vue    # Delete confirmation
│   └── ZoneMapView.vue        # Interactive map view
├── composables/
│   └── useZones.ts            # Zone CRUD logic
├── api.ts                     # API client functions
├── model.type.ts              # TypeScript types and classes
├── Zones.routes.ts            # Route definitions
├── ZonesView.vue              # List view
├── ZoneDetailView.vue         # Detail view
└── README.md                  # This file
```

## Types

### ZoneDto

Represents a delivery zone entity:

- `id`: Unique identifier (UUID)
- `code`: Zone code (e.g., "ZONE001")
- `name`: Zone display name
- `polygon`: GeoJSON polygon defining zone boundaries (optional)
- `centerId`: Parent distribution center ID
- `centerCode`: Center code (read-only)
- `centerName`: Center name (read-only)

Methods:

- `displayName`: Returns formatted zone name
- `hasPolygon`: Checks if polygon is defined
- `centerDisplayName`: Returns formatted center name

### CenterDto

Represents a distribution center:

- `id`: Unique identifier (UUID)
- `code`: Center code
- `name`: Center display name
- `address`: Physical address (optional)
- `location`: GeoJSON point for center location (optional)

### CreateZoneRequest

Request payload for creating a new zone:

- `code`: Required
- `name`: Required
- `polygon`: Optional GeoJSON polygon
- `centerId`: Required

### UpdateZoneRequest

Request payload for updating a zone (all fields optional):

- `code`
- `name`
- `polygon`
- `centerId`

## API Functions

### getZones(params?)

Get paginated list of zones.

**Parameters:**

- `params.page`: Page number (0-indexed)
- `params.size`: Page size
- `params.search`: Search query
- `params.centerId`: Filter by center ID

**Returns:** `Promise<GetZonesResponse>`

### getZoneById(id)

Get zone by ID.

**Parameters:**

- `id`: Zone UUID

**Returns:** `Promise<GetZoneResponse>`

### getZoneByCode(code)

Get zone by code.

**Parameters:**

- `code`: Zone code string

**Returns:** `Promise<GetZoneResponse>`

### getZonesByCenter(centerId)

Get all zones for a distribution center.

**Parameters:**

- `centerId`: Center UUID

**Returns:** `Promise<ZoneDto[]>`

### createZone(data)

Create a new zone.

**Parameters:**

- `data`: CreateZoneRequest object

**Returns:** `Promise<CreateZoneResponse>`

### updateZone(id, data)

Update an existing zone.

**Parameters:**

- `id`: Zone UUID
- `data`: UpdateZoneRequest object

**Returns:** `Promise<UpdateZoneResponse>`

### deleteZone(id)

Delete a zone.

**Parameters:**

- `id`: Zone UUID

**Returns:** `Promise<DeleteZoneResponse>`

### getCenters()

Get list of all distribution centers.

**Returns:** `Promise<GetCentersResponse>`

## Components

### ZonesView

Main view component that provides:

- Data table with sortable columns
- Search functionality
- Center filter dropdown
- Pagination
- Bulk selection and actions
- Create zone modal
- Edit zone modal
- Delete confirmation dialog

### ZoneDetailView

Detail view component that displays:

- Zone information card
- Distribution center details
- Polygon status
- Map visualization (placeholder for future implementation)
- Edit and delete actions

## Usage

### Add to Router

```typescript
import { zonesRoutes } from '@/modules/Zones/Zones.routes'

const router = createRouter({
  routes: [
    ...zonesRoutes,
    // other routes
  ],
})
```

### Navigation Link

```vue
<RouterLink to="/zones">Manage Zones</RouterLink>
```

### Filter by Center

The Zones module supports filtering by distribution center:

```vue
<USelect
  :model-value="selectedCenterId"
  :options="centerOptions"
  @update:model-value="filterByCenter"
/>
```

## Map Integration

The zone detail view includes a placeholder for map integration. Future implementation will include:

- Interactive map display using Track-Asia
- Draw and edit polygon boundaries
- Visualize zone coverage areas
- Show related delivery addresses

## Permissions

Access to the Zones module requires authentication and one of the following roles:

- ADMIN
- MANAGER

## Dependencies

- Vue 3
- Vue Router
- Pinia (state management)
- Nuxt UI (component library)
- Axios (HTTP client)
- Track-Asia Map (planned for polygon drawing)

## Backend API

The module communicates with the following backend endpoints:

- `GET /v1/zone/health` - Health check
- `GET /v1/zones` - List zones (paginated)
- `GET /v1/zones/:id` - Get zone by ID
- `GET /v1/zones/code/:code` - Get zone by code
- `GET /v1/zones/center/:centerId` - Get zones by center
- `POST /v1/zones` - Create zone
- `PUT /v1/zones/:id` - Update zone
- `DELETE /v1/zones/:id` - Delete zone
- `GET /v1/centers` - Get distribution centers

All endpoints require authentication via JWT token in the Authorization header.

## Error Handling

All API errors are handled by the global axios interceptor and displayed via toast notifications.

## Routes

The module provides the following routes:

- `/zones` - Zone list view with table and filters
- `/zones/map` - Interactive map view with zone management
- `/zones/:id` - Zone detail view with map visualization

## Future Enhancements

- ✅ Polygon drawing tool on map
- ✅ Zone coverage analytics
- ✅ Delivery address clustering
- ✅ Zone capacity management
- ✅ Auto-assignment rules
- ✅ Interactive map view with drawer
- ✅ Zone color coding and legend
- ✅ Zone overlap detection
- ✅ Import zones from file
- ✅ Export zone boundaries to GeoJSON
