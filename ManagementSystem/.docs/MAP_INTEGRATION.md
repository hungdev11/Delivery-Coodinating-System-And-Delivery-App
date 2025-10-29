# Map Integration Guide

Complete guide for using the MapView component with Track-Asia/MapLibre GL.

## Overview

The map integration provides:
- **MapView Component**: Reusable Vue component for displaying maps
- **useMap Composable**: Programmatic map control
- **Map Types**: Comprehensive TypeScript definitions
- **Layer Support**: Zones, routing, traffic, markers
- **Reactive**: Auto-updates when data changes

## Quick Start

### 1. Install Dependencies (Already Done)

```bash
npm install maplibre-gl @turf/turf
```

Dependencies installed:
- `maplibre-gl`: Open-source map renderer (Track-Asia compatible)
- `@turf/turf`: Geospatial analysis utilities

### 2. Import and Use

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { MapView } from '@/common/components'
import type { ZonePolygon } from '@/common/types/map.type'

const zones = ref<ZonePolygon[]>([
  {
    id: 'zone-1',
    name: 'District 1',
    coordinates: [
      [
        [106.695, 10.776],
        [106.707, 10.776],
        [106.707, 10.787],
        [106.695, 10.787],
        [106.695, 10.776],
      ],
    ],
    fillColor: '#3b82f6',
  },
])
</script>

<template>
  <MapView :zones="zones" height="600px" />
</template>
```

## File Structure

```
src/
├── common/
│   ├── component/
│   │   ├── MapView.vue              # Main map component
│   │   └── MapView.README.md        # Component documentation
│   ├── composables/
│   │   └── useMap.ts                # Map logic composable
│   └── types/
│       └── map.type.ts              # TypeScript definitions
└── modules/
    └── Zones/
        └── ZoneDetailView.vue       # Example integration
```

## Components

### MapView.vue

Main map component with props for:
- Zone polygons
- Route paths
- Traffic data
- Custom markers
- Layer visibility toggles
- Auto-fit bounds

See `MapView.README.md` for full documentation.

### Example Usage in Zone Module

Check `src/modules/Zones/ZoneDetailView.vue` for a complete implementation:

```vue
<MapView
  :zones="zonePolygons"
  :markers="centerMarkers"
  :show-zones="showMap"
  :show-routing="false"
  :show-traffic="false"
  :zone-options="{ fillOpacity: 0.25, strokeWidth: 2, showLabels: true }"
  height="500px"
>
  <template #controls>
    <UButton @click="showMap = !showMap">
      {{ showMap ? 'Hide' : 'Show' }} Zone
    </UButton>
  </template>

  <template #legend>
    <div class="space-y-2">
      <h4 class="font-semibold text-sm">Legend</h4>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 bg-blue-500/25 border-2 border-blue-600 rounded" />
        <span class="text-xs">Zone Name</span>
      </div>
    </div>
  </template>
</MapView>
```

## Composable: useMap

For programmatic control:

```typescript
import { useMap } from '@/common/composables/useMap'

const {
  map,               // MapLibre Map instance
  loaded,            // Map loaded state
  initMap,           // Initialize map
  addMarker,         // Add marker
  addZoneLayer,      // Add zone polygons
  addRoutingLayer,   // Add route paths
  addTrafficLayer,   // Add traffic heatmap
  removeLayer,       // Remove layer
  toggleLayer,       // Toggle visibility
  fitBounds,         // Fit to bounds
  getViewState,      // Get camera state
  setViewState,      // Set camera state
} = useMap()
```

## Types

All types are exported from `@/common/types/map.type`:

```typescript
import type {
  MapConfig,
  MapLayer,
  MapMarker,
  ZonePolygon,
  RouteData,
  TrafficData,
  MapViewState,
  // ... and more
} from '@/common/types/map.type'
```

Key types:
- `ZonePolygon`: Zone boundary data
- `RouteData`: Route path with distance/duration
- `TrafficData`: Traffic congestion points
- `MapMarker`: Custom markers
- `MapConfig`: Map initialization config
- `LayerStyle`: Layer styling options

## Use Cases

### 1. Zone Visualization (Implemented)

Display delivery zones with boundaries:

```typescript
const zonePolygons = computed<ZonePolygon[]>(() => {
  if (!zone.value?.polygon) return []

  return [{
    id: zone.value.id,
    name: zone.value.displayName,
    coordinates: zone.value.polygon.coordinates as [number, number][][],
    fillColor: '#3b82f6',
  }]
})
```

### 2. Route Visualization

Show delivery routes:

```typescript
const routes: RouteData[] = [
  {
    coordinates: [
      [106.695, 10.776],
      [106.700, 10.780],
      [106.707, 10.787],
    ],
    distance: 1500,    // meters
    duration: 300,     // seconds
  },
]
```

```vue
<MapView
  :routes="routes"
  :routing-options="{ color: '#10b981', width: 4, showWaypoints: true }"
/>
```

### 3. Traffic Overlay

Display traffic conditions:

```typescript
const traffic: TrafficData[] = [
  {
    coordinates: [106.701, 10.781],
    speed: 30,
    congestionLevel: 'medium',
    timestamp: new Date().toISOString(),
  },
]
```

```vue
<MapView
  :traffic="traffic"
  :show-traffic="true"
  :traffic-options="{
    colors: {
      low: '#22c55e',
      medium: '#eab308',
      high: '#f97316',
      severe: '#ef4444',
    }
  }"
/>
```

### 4. Custom Markers

Add markers for centers, deliveries, etc:

```typescript
const markers: MapMarker[] = [
  {
    id: 'center-1',
    coordinates: [106.701, 10.781],
    type: 'center',
    label: 'Main DC',
    popup: '<strong>Distribution Center</strong><br>HCMC',
    onClick: () => console.log('Marker clicked'),
  },
]
```

```vue
<MapView :markers="markers" />
```

## Map Configuration

### Default Configuration

```typescript
{
  style: 'https://tiles.track-asia.com/tiles/v3/style.json',
  center: [106.6297, 10.8231], // Ho Chi Minh City
  zoom: 12,
  minZoom: 5,
  maxZoom: 20,
  pitch: 0,
  bearing: 0,
}
```

### Custom Configuration

```vue
<MapView
  :config="{
    center: [106.695, 10.776],
    zoom: 14,
    pitch: 45,
    bearing: 0,
  }"
/>
```

## Layer Options

### Zone Layer Options

```typescript
interface ZoneLayerOptions {
  fillOpacity?: number        // 0-1, default 0.2
  strokeWidth?: number        // pixels, default 2
  showLabels?: boolean        // show zone names
  interactive?: boolean       // enable click events
}
```

### Routing Layer Options

```typescript
interface RoutingLayerOptions {
  color?: string              // line color
  width?: number              // line width
  opacity?: number            // 0-1
  showArrows?: boolean        // direction arrows
  showWaypoints?: boolean     // start/end markers
}
```

### Traffic Layer Options

```typescript
interface TrafficLayerOptions {
  colors?: {
    low: string
    medium: string
    high: string
    severe: string
  }
  opacity?: number
  lineWidth?: number
}
```

## Events

### Map Loaded

```vue
<MapView @map-loaded="handleMapLoaded" />
```

```typescript
const handleMapLoaded = () => {
  console.log('Map is ready!')
}
```

### Map Click

```vue
<MapView @map-click="handleMapClick" />
```

```typescript
const handleMapClick = (data: { lngLat: [number, number] }) => {
  console.log('Clicked at:', data.lngLat)
  // Example: Add marker at click position
}
```

### View Change

```vue
<MapView @view-change="handleViewChange" />
```

```typescript
const handleViewChange = (state: MapViewState) => {
  console.log('Zoom:', state.zoom)
  console.log('Center:', state.center)
}
```

## Slots

### Controls Slot

Top-right corner controls:

```vue
<MapView>
  <template #controls>
    <div class="space-y-2">
      <UButton @click="toggleZones">Toggle Zones</UButton>
      <UButton @click="toggleTraffic">Toggle Traffic</UButton>
    </div>
  </template>
</MapView>
```

### Legend Slot

Bottom-left corner legend:

```vue
<MapView>
  <template #legend>
    <div>
      <h4 class="font-semibold text-sm mb-2">Legend</h4>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 bg-blue-500/25 border-2 border-blue-600 rounded" />
        <span class="text-xs">Active Zone</span>
      </div>
    </div>
  </template>
</MapView>
```

### Info Slot

Top-left corner info panel:

```vue
<MapView>
  <template #info>
    <div>
      <h4 class="font-semibold mb-2">Selected Zone</h4>
      <p class="text-sm">{{ selectedZone?.name }}</p>
    </div>
  </template>
</MapView>
```

## Advanced Features

### Accessing Map Instance

```vue
<script setup>
import { ref } from 'vue'

const mapRef = ref()

const zoomIn = () => {
  mapRef.value?.map?.zoomIn()
}

const flyTo = (coordinates: [number, number]) => {
  mapRef.value?.map?.flyTo({ center: coordinates, zoom: 14 })
}
</script>

<template>
  <MapView ref="mapRef" />
  <UButton @click="zoomIn">Zoom In</UButton>
</template>
```

### Dynamic Layer Updates

All layer data is reactive and will auto-update:

```typescript
const zones = ref<ZonePolygon[]>([])

// Load zones
const loadZones = async () => {
  const response = await fetchZones()
  zones.value = response.data.map(z => ({
    id: z.id,
    name: z.name,
    coordinates: z.polygon.coordinates,
    fillColor: z.color,
  }))
  // Map automatically updates!
}
```

### Multiple Layers

Combine zones, routes, traffic, and markers:

```vue
<MapView
  :zones="zones"
  :routes="routes"
  :traffic="traffic"
  :markers="markers"
  :show-zones="true"
  :show-routing="true"
  :show-traffic="showTraffic"
/>
```

## Styling

### Custom Marker Styles

```css
:deep(.marker-center) {
  background-color: #3b82f6;
  width: 28px;
  height: 28px;
}

:deep(.marker-delivery) {
  background-color: #10b981;
  width: 20px;
  height: 20px;
}
```

### Custom Popup Styles

```css
:deep(.maplibregl-popup-content) {
  padding: 1rem;
  border-radius: 0.5rem;
  min-width: 200px;
}
```

## Performance Tips

1. **Lazy Load**: Only render map when needed
   ```vue
   <MapView v-if="showMap" />
   ```

2. **Debounce Updates**: For frequent data changes
   ```typescript
   import { useDebounceFn } from '@vueuse/core'

   const updateZones = useDebounceFn((data) => {
     zones.value = data
   }, 300)
   ```

3. **Simplify Polygons**: For complex zones, use Turf.js
   ```typescript
   import { simplify } from '@turf/turf'

   const simplified = simplify(polygon, { tolerance: 0.01 })
   ```

## Troubleshooting

### Map Not Rendering

1. Check container has height
2. Check MapLibre GL CSS is imported
3. Check console for errors

### Layers Not Showing

1. Verify data format (coordinates as `[lng, lat]`)
2. Check layer visibility props
3. Verify bounds (data within Vietnam)

### Performance Issues

1. Reduce polygon complexity
2. Limit number of markers
3. Use clustering for many points
4. Disable features not needed

## Integration Checklist

- [x] Install dependencies (`maplibre-gl`, `@turf/turf`)
- [x] Create map types (`map.type.ts`)
- [x] Create useMap composable
- [x] Create MapView component
- [x] Export from component index
- [x] Add documentation
- [x] Integrate into ZoneDetailView
- [ ] Test with real zone data
- [ ] Add route visualization (future)
- [ ] Add traffic layer (future)
- [ ] Add drawing tools (future)

## Future Enhancements

### Drawing Tools
- Draw new zones
- Edit zone boundaries
- Move markers

### Advanced Routing
- Multi-waypoint routes
- Route optimization
- Distance matrix

### Real-time Updates
- Live traffic data
- Delivery tracking
- WebSocket integration

### Analytics
- Heatmaps
- Coverage analysis
- Zone statistics

## Resources

- [MapLibre GL JS Docs](https://maplibre.org/maplibre-gl-js/docs/)
- [Track-Asia API](https://www.track-asia.com/)
- [Turf.js Documentation](https://turfjs.org/)
- [GeoJSON Specification](https://geojson.org/)
- [Component README](./src/common/components/MapView.README.md)

## Support

For issues or questions:
1. Check component documentation
2. Review example implementations
3. Check MapLibre GL docs
4. Open GitHub issue
