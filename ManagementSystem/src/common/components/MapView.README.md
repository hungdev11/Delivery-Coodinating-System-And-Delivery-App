# MapView Component

A comprehensive map component built with MapLibre GL (Track-Asia compatible) for visualizing zones, routes, traffic, and custom markers.

## Features

- **Zone Visualization**: Display zone polygons with custom colors and labels
- **Routing**: Show route paths with waypoint markers
- **Traffic Overlay**: Visualize traffic data with heatmap
- **Custom Markers**: Add markers for centers, deliveries, warehouses, etc.
- **Layer Management**: Toggle layer visibility dynamically
- **Auto-fit**: Automatically fit bounds to displayed data
- **Customizable**: Extensive configuration options
- **Reactive**: Automatically updates when data changes
- **Slots**: Control panel, legend, and info panel slots
- **Events**: Map click, view change events

## Installation

Dependencies are already installed:
- `maplibre-gl`: Open-source map renderer
- `@turf/turf`: Geospatial analysis library

## Basic Usage

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { MapView } from '@/common/components'
import type { ZonePolygon, MapMarker } from '@/common/types/map.type'

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

const markers = ref<MapMarker[]>([
  {
    id: 'center-1',
    coordinates: [106.701, 10.781],
    type: 'center',
    label: 'Distribution Center',
    popup: '<strong>Main DC</strong><br>Ho Chi Minh City',
  },
])

const handleMapClick = (data: { lngLat: [number, number] }) => {
  console.log('Clicked at:', data.lngLat)
}
</script>

<template>
  <MapView
    :zones="zones"
    :markers="markers"
    height="600px"
    @map-click="handleMapClick"
  />
</template>
```

## Props

### Map Configuration

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `config` | `Partial<MapConfig>` | - | Map configuration (center, zoom, etc.) |
| `height` | `string` | `'600px'` | Map height |
| `width` | `string` | `'100%'` | Map width |

### Layer Data

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `zones` | `ZonePolygon[]` | - | Zone polygons to display |
| `routes` | `RouteData[]` | - | Route paths to display |
| `traffic` | `TrafficData[]` | - | Traffic data points |
| `markers` | `MapMarker[]` | - | Custom markers |

### Layer Options

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `zoneOptions` | `ZoneLayerOptions` | - | Zone layer styling |
| `routingOptions` | `RoutingLayerOptions` | - | Routing layer styling |
| `trafficOptions` | `TrafficLayerOptions` | - | Traffic layer styling |

### Layer Visibility

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `showZones` | `boolean` | `true` | Show/hide zones layer |
| `showRouting` | `boolean` | `true` | Show/hide routing layer |
| `showTraffic` | `boolean` | `false` | Show/hide traffic layer |

### Other Options

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `autoFit` | `boolean` | `true` | Auto-fit bounds to data |
| `fitPadding` | `number` | `50` | Padding for auto-fit |
| `loading` | `boolean` | `false` | Show loading overlay |

## Events

| Event | Payload | Description |
|-------|---------|-------------|
| `map-loaded` | - | Emitted when map is ready |
| `map-click` | `{ lngLat, point }` | Emitted on map click |
| `view-change` | `MapViewState` | Emitted when view changes |

## Slots

### Controls Slot

Add custom controls to the top-right corner:

```vue
<MapView>
  <template #controls>
    <div class="space-y-2">
      <UButton @click="zoomIn">Zoom In</UButton>
      <UButton @click="zoomOut">Zoom Out</UButton>
    </div>
  </template>
</MapView>
```

### Legend Slot

Add a legend to the bottom-left corner:

```vue
<MapView>
  <template #legend>
    <div class="space-y-1">
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 bg-blue-500 rounded" />
        <span class="text-sm">Zone 1</span>
      </div>
    </div>
  </template>
</MapView>
```

### Info Slot

Add info panel to the top-left corner:

```vue
<MapView>
  <template #info>
    <div>
      <h4 class="font-semibold mb-2">Map Info</h4>
      <p class="text-sm">Selected: {{ selectedZone }}</p>
    </div>
  </template>
</MapView>
```

## Advanced Usage

### With All Features

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { MapView } from '@/common/components'
import type {
  ZonePolygon,
  RouteData,
  TrafficData,
  MapMarker,
  ZoneLayerOptions,
} from '@/common/types/map.type'

// Zones data
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

// Routes data
const routes = ref<RouteData[]>([
  {
    coordinates: [
      [106.695, 10.776],
      [106.700, 10.780],
      [106.707, 10.787],
    ],
    distance: 1500,
    duration: 300,
  },
])

// Traffic data
const traffic = ref<TrafficData[]>([
  {
    coordinates: [106.701, 10.781],
    speed: 30,
    congestionLevel: 'medium',
    timestamp: new Date().toISOString(),
  },
])

// Markers
const markers = ref<MapMarker[]>([
  {
    id: 'center-1',
    coordinates: [106.701, 10.781],
    type: 'center',
    popup: '<strong>Main DC</strong>',
  },
])

// Layer options
const zoneOptions: ZoneLayerOptions = {
  fillOpacity: 0.3,
  strokeWidth: 2,
  showLabels: true,
  interactive: true,
}

// Layer visibility
const showZones = ref(true)
const showRouting = ref(true)
const showTraffic = ref(false)

const handleMapLoaded = () => {
  console.log('Map ready!')
}

const handleMapClick = (data: { lngLat: [number, number] }) => {
  console.log('Clicked at:', data.lngLat)
}
</script>

<template>
  <MapView
    :zones="zones"
    :routes="routes"
    :traffic="traffic"
    :markers="markers"
    :zone-options="zoneOptions"
    :show-zones="showZones"
    :show-routing="showRouting"
    :show-traffic="showTraffic"
    height="800px"
    @map-loaded="handleMapLoaded"
    @map-click="handleMapClick"
  >
    <template #controls>
      <div class="space-y-2">
        <UButton @click="showZones = !showZones">
          {{ showZones ? 'Hide' : 'Show' }} Zones
        </UButton>
        <UButton @click="showRouting = !showRouting">
          {{ showRouting ? 'Hide' : 'Show' }} Routes
        </UButton>
        <UButton @click="showTraffic = !showTraffic">
          {{ showTraffic ? 'Hide' : 'Show' }} Traffic
        </UButton>
      </div>
    </template>

    <template #legend>
      <div class="space-y-2">
        <h4 class="font-semibold text-sm">Legend</h4>
        <div class="space-y-1">
          <div class="flex items-center gap-2">
            <div class="w-4 h-4 bg-blue-500/30 border-2 border-blue-500 rounded" />
            <span class="text-xs">Delivery Zone</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-4 h-4 bg-green-500 rounded-full border-2 border-white" />
            <span class="text-xs">Distribution Center</span>
          </div>
        </div>
      </div>
    </template>
  </MapView>
</template>
```

### Accessing Map Instance

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { MapView } from '@/common/components'
import type { Map } from 'maplibre-gl'

const mapRef = ref<InstanceType<typeof MapView>>()

onMounted(() => {
  // Access the map instance
  const map = mapRef.value?.map
  if (map) {
    // Use MapLibre GL API directly
    map.setZoom(14)
    map.flyTo({ center: [106.695, 10.776] })
  }
})
</script>

<template>
  <MapView ref="mapRef" />
</template>
```

## Types

### ZonePolygon

```typescript
interface ZonePolygon {
  id: string
  name: string
  coordinates: [number, number][][]
  color?: string
  fillColor?: string
  fillOpacity?: number
  strokeColor?: string
  strokeWidth?: number
  properties?: Record<string, any>
}
```

### RouteData

```typescript
interface RouteData {
  coordinates: [number, number][]
  distance: number
  duration: number
  properties?: Record<string, any>
}
```

### TrafficData

```typescript
interface TrafficData {
  coordinates: [number, number]
  speed: number
  congestionLevel: 'low' | 'medium' | 'high' | 'severe'
  timestamp: string
}
```

### MapMarker

```typescript
interface MapMarker {
  id: string
  coordinates: [number, number]
  type?: 'center' | 'delivery' | 'warehouse' | 'custom'
  label?: string
  icon?: string
  color?: string
  popup?: string
  onClick?: () => void
}
```

## Composable: useMap

The `useMap` composable provides programmatic map control:

```typescript
import { useMap } from '@/common/composables/useMap'

const {
  map,
  loaded,
  initMap,
  addMarker,
  addZoneLayer,
  addRoutingLayer,
  addTrafficLayer,
  removeLayer,
  toggleLayer,
  fitBounds,
  getViewState,
  setViewState,
  clearMarkers,
} = useMap()
```

### Methods

- `initMap(container, config)`: Initialize map
- `addMarker(marker)`: Add a marker
- `removeMarker(id)`: Remove a marker
- `clearMarkers()`: Remove all markers
- `addZoneLayer(zones, options)`: Add zone polygon layer
- `addRoutingLayer(routes, options)`: Add routing layer
- `addTrafficLayer(traffic, options)`: Add traffic heatmap
- `removeLayer(id)`: Remove a layer
- `toggleLayer(id, visible)`: Toggle layer visibility
- `fitBounds(coordinates, options)`: Fit map to bounds
- `getViewState()`: Get current view state
- `setViewState(state, animate)`: Set view state

## Styling

The component includes default styles for:
- Map container with border and rounded corners
- Loading overlay
- Control panel positioning
- Legend positioning
- Info panel positioning
- Marker styles (center, delivery, warehouse, custom)
- Popup customization

You can override these styles using CSS or by providing custom classes.

## Performance Tips

1. **Lazy load map**: Use `v-if` to only render when needed
2. **Debounce updates**: Use debounce when updating layer data frequently
3. **Limit markers**: Use clustering for many markers
4. **Optimize polygons**: Simplify complex polygons
5. **Use webworkers**: For heavy geospatial calculations

## Browser Support

- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support (iOS 13.4+)
- WebGL required

## Examples in the Project

See `ZoneDetailView.vue` for real-world usage with zone visualization.

## Resources

- [MapLibre GL JS Docs](https://maplibre.org/maplibre-gl-js/docs/)
- [Track-Asia Maps](https://www.track-asia.com/)
- [Turf.js Docs](https://turfjs.org/)
- [GeoJSON Spec](https://geojson.org/)
