/**
 * Map Composable
 *
 * Handles map initialization, layer management, and interactions
 */

import { ref, onUnmounted, nextTick, type Ref } from 'vue'
import maplibregl, { type Map, type LngLatLike, type Marker } from 'maplibre-gl'
import type {
  MapConfig,
  MapLayer,
  MapMarker,
  ZonePolygon,
  RouteData,
  TrafficData,
  MapViewState,
  GeoJSONFeatureCollection,
  LayerStyle,
  TrafficLayerOptions,
  RoutingLayerOptions,
  ZoneLayerOptions,
} from '@/common/types/map.type'

export function useMap() {
  const map = ref<Map | null>(null)
  const loaded = ref(false)
  const markers = ref<Map<string, Marker>>(new Map())
  const activeLayers = ref<Set<string>>(new Set())

  /**
   * Initialize map
   */
  const initMap = async (
    container: string | HTMLElement,
    config: Partial<MapConfig> = {},
  ): Promise<Map> => {
    // Default config for Vietnam/HCMC area with fallback
    const defaultConfig: MapConfig = {
      container,
      style: {
        version: 8,
        sources: {
          'osm-tiles': {
            type: 'raster',
            tiles: [
              'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
            ],
            tileSize: 256,
            attribution: '© OpenStreetMap contributors'
          }
        },
        layers: [
          {
            id: 'osm-tiles',
            type: 'raster',
            source: 'osm-tiles',
            minzoom: 0,
            maxzoom: 19
          }
        ]
      },
      center: [106.6297, 10.8231], // Ho Chi Minh City
      zoom: 12,
      minZoom: 5,
      maxZoom: 20,
      pitch: 0,
      bearing: 0,
      interactive: true,
      attributionControl: true,
      ...config,
    }

    return new Promise((resolve, reject) => {
      try {
        const mapInstance = new maplibregl.Map(defaultConfig as any)

        mapInstance.on('load', () => {
          map.value = mapInstance
          loaded.value = true
          resolve(mapInstance)
        })

        mapInstance.on('error', (e) => {
          console.error('Map error:', e)
          reject(e)
        })
      } catch (error) {
        console.error('Failed to initialize map:', error)
        reject(error)
      }
    })
  }

  /**
   * Add a marker to the map
   */
  const addMarker = (markerData: MapMarker): Marker | null => {
    if (!map.value) return null

    const el = document.createElement('div')
    el.className = 'map-marker'
    el.style.cursor = 'pointer'

    // Apply custom styling based on marker type
    if (markerData.type) {
      el.classList.add(`marker-${markerData.type}`)
    }

    if (markerData.color) {
      el.style.backgroundColor = markerData.color
    }

    const marker = new maplibregl.Marker({ element: el })
      .setLngLat(markerData.coordinates)
      .addTo(map.value)

    if (markerData.popup) {
      const popup = new maplibregl.Popup({ offset: 25 }).setHTML(markerData.popup)
      marker.setPopup(popup)
    }

    if (markerData.onClick) {
      el.addEventListener('click', markerData.onClick)
    }

    markers.value.set(markerData.id, marker)
    return marker
  }

  /**
   * Remove marker from map
   */
  const removeMarker = (markerId: string): void => {
    const marker = markers.value.get(markerId)
    if (marker) {
      marker.remove()
      markers.value.delete(markerId)
    }
  }

  /**
   * Clear all markers
   */
  const clearMarkers = (): void => {
    markers.value.forEach((marker) => marker.remove())
    markers.value.clear()
  }

  /**
   * Add zone polygon layer
   */
  const addZoneLayer = (
    zones: ZonePolygon[],
    options: ZoneLayerOptions = {},
  ): void => {
    if (!map.value || !loaded.value) return

    const layerId = 'zones-layer'
    const sourceId = 'zones-source'

    // Create GeoJSON features from zones
    const features = zones.map((zone) => ({
      type: 'Feature' as const,
      geometry: {
        type: 'Polygon' as const,
        coordinates: zone.coordinates,
      },
      properties: {
        id: zone.id,
        name: zone.name,
        color: zone.fillColor || zone.color || '#3b82f6',
        ...zone.properties,
      },
    }))

    const geojsonData = {
      type: 'FeatureCollection',
      features,
    } as any

    // If source exists, just update data; otherwise create source
    const existingSource = map.value.getSource(sourceId) as any
    if (existingSource && typeof existingSource.setData === 'function') {
      existingSource.setData(geojsonData)
    } else {
      // Ensure any stale layers referencing the same id are removed before adding
      if (map.value.getLayer(layerId)) map.value.removeLayer(layerId)
      if (map.value.getLayer(`${layerId}-outline`)) map.value.removeLayer(`${layerId}-outline`)
      if (map.value.getLayer(`${layerId}-labels`)) map.value.removeLayer(`${layerId}-labels`)
      if (map.value.getSource(sourceId)) map.value.removeSource(sourceId)

      map.value.addSource(sourceId, {
        type: 'geojson',
        data: geojsonData,
      })
    }

    // Add fill layer
    if (!map.value.getLayer(layerId)) {
      map.value.addLayer({
        id: layerId,
        type: 'fill',
        source: sourceId,
        paint: {
          'fill-color': ['get', 'color'],
          'fill-opacity': options.fillOpacity ?? 0.2,
        },
      })
    }

    // Add outline layer
    if (!map.value.getLayer(`${layerId}-outline`)) {
      map.value.addLayer({
        id: `${layerId}-outline`,
        type: 'line',
        source: sourceId,
        paint: {
          'line-color': ['get', 'color'],
          'line-width': options.strokeWidth ?? 2,
        },
      })
    }

    // Add labels if enabled
    if (options.showLabels && !map.value.getLayer(`${layerId}-labels`)) {
      map.value.addLayer({
        id: `${layerId}-labels`,
        type: 'symbol',
        source: sourceId,
        layout: {
          'text-field': ['get', 'name'],
          'text-size': 12,
        },
        paint: {
          'text-color': '#000',
          'text-halo-color': '#fff',
          'text-halo-width': 2,
        },
      })
    }

    activeLayers.value.add(layerId)
  }

  /**
   * Add routing layer
   */
  const addRoutingLayer = (
    routes: RouteData[],
    options: RoutingLayerOptions = {},
  ): void => {
    if (!map.value || !loaded.value) return

    const layerId = 'routing-layer'
    const sourceId = 'routing-source'

    // Remove existing layer
    if (map.value.getLayer(layerId)) {
      map.value.removeLayer(layerId)
    }
    if (map.value.getSource(sourceId)) {
      map.value.removeSource(sourceId)
    }

    // Create GeoJSON features
    const features = routes.map((route, index) => ({
      type: 'Feature' as const,
      geometry: {
        type: 'LineString' as const,
        coordinates: route.coordinates,
      },
      properties: {
        id: `route-${index}`,
        distance: route.distance,
        duration: route.duration,
        ...route.properties,
      },
    }))

    // Add source
    map.value.addSource(sourceId, {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features,
      },
    })

    // Add line layer
    map.value.addLayer({
      id: layerId,
      type: 'line',
      source: sourceId,
      paint: {
        'line-color': options.color || '#10b981',
        'line-width': options.width ?? 4,
        'line-opacity': options.opacity ?? 0.8,
      },
      layout: {
        'line-join': 'round',
        'line-cap': 'round',
      },
    })

    // Add waypoint markers if enabled
    if (options.showWaypoints) {
      routes.forEach((route, routeIndex) => {
        // Start marker
        addMarker({
          id: `route-${routeIndex}-start`,
          coordinates: route.coordinates[0] as [number, number],
          type: 'custom',
          color: '#22c55e',
          label: 'Start',
        })

        // End marker
        const lastIndex = route.coordinates.length - 1
        addMarker({
          id: `route-${routeIndex}-end`,
          coordinates: route.coordinates[lastIndex] as [number, number],
          type: 'custom',
          color: '#ef4444',
          label: 'End',
        })
      })
    }

    activeLayers.value.add(layerId)
  }

  /**
   * Add traffic layer
   */
  const addTrafficLayer = (
    trafficData: TrafficData[],
    options: TrafficLayerOptions = {},
  ): void => {
    if (!map.value || !loaded.value) return

    const layerId = 'traffic-layer'
    const sourceId = 'traffic-source'

    const colors = options.colors || {
      low: '#22c55e',
      medium: '#eab308',
      high: '#f97316',
      severe: '#ef4444',
    }

    // Remove existing layer
    if (map.value.getLayer(layerId)) {
      map.value.removeLayer(layerId)
    }
    if (map.value.getSource(sourceId)) {
      map.value.removeSource(sourceId)
    }

    // Create GeoJSON features
    const features = trafficData.map((data, index) => ({
      type: 'Feature' as const,
      geometry: {
        type: 'Point' as const,
        coordinates: data.coordinates,
      },
      properties: {
        id: `traffic-${index}`,
        speed: data.speed,
        congestion: data.congestionLevel,
        color: colors[data.congestionLevel],
        timestamp: data.timestamp,
      },
    }))

    // Add source
    map.value.addSource(sourceId, {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features,
      },
    })

    // Add heatmap layer
    map.value.addLayer({
      id: layerId,
      type: 'heatmap',
      source: sourceId,
      paint: {
        'heatmap-weight': [
          'interpolate',
          ['linear'],
          ['get', 'speed'],
          0,
          1,
          100,
          0,
        ],
        'heatmap-intensity': 1,
        'heatmap-color': [
          'interpolate',
          ['linear'],
          ['heatmap-density'],
          0,
          'rgba(0, 0, 255, 0)',
          0.25,
          colors.low,
          0.5,
          colors.medium,
          0.75,
          colors.high,
          1,
          colors.severe,
        ],
        'heatmap-radius': 20,
        'heatmap-opacity': options.opacity ?? 0.6,
      },
    })

    activeLayers.value.add(layerId)
  }

  /**
   * Remove layer from map
   */
  const removeLayer = (layerId: string): void => {
    if (!map.value) return

    if (map.value.getLayer(layerId)) {
      map.value.removeLayer(layerId)
    }

    const sourceId = layerId.replace('-layer', '-source')
    if (map.value.getSource(sourceId)) {
      map.value.removeSource(sourceId)
    }

    activeLayers.value.delete(layerId)
  }

  /**
   * Toggle layer visibility
   */
  const toggleLayer = (layerId: string, visible: boolean): void => {
    if (!map.value || !map.value.getLayer(layerId)) return

    map.value.setLayoutProperty(layerId, 'visibility', visible ? 'visible' : 'none')
  }

  /**
   * Fit map to bounds
   */
  const fitBounds = (
    coordinates: [number, number][],
    options: { padding?: number; maxZoom?: number } = {},
  ): void => {
    if (!map.value || coordinates.length === 0) return

    const bounds = coordinates.reduce(
      (bounds, coord) => bounds.extend(coord),
      new maplibregl.LngLatBounds(coordinates[0], coordinates[0]),
    )

    map.value.fitBounds(bounds, {
      padding: options.padding ?? 50,
      maxZoom: options.maxZoom ?? 15,
    })
  }

  /**
   * Get current view state
   */
  const getViewState = (): MapViewState | null => {
    if (!map.value) return null

    const center = map.value.getCenter()
    return {
      center: [center.lng, center.lat],
      zoom: map.value.getZoom(),
      bearing: map.value.getBearing(),
      pitch: map.value.getPitch(),
      bounds: map.value.getBounds(),
    }
  }

  /**
   * Set view state
   */
  const setViewState = (state: Partial<MapViewState>, animate = true): void => {
    if (!map.value) return

    const options: any = { animate }

    if (state.center) options.center = state.center
    if (state.zoom !== undefined) options.zoom = state.zoom
    if (state.bearing !== undefined) options.bearing = state.bearing
    if (state.pitch !== undefined) options.pitch = state.pitch

    if (animate) {
      map.value.flyTo(options)
    } else {
      map.value.jumpTo(options)
    }
  }

  /**
   * Cleanup on unmount
   */
  const cleanup = (): void => {
    clearMarkers()

    if (map.value) {
      map.value.remove()
      map.value = null
    }

    loaded.value = false
    activeLayers.value.clear()
  }

  onUnmounted(() => {
    cleanup()
  })

  return {
    // State
    map,
    loaded,
    markers,
    activeLayers,

    // Methods
    initMap,
    addMarker,
    removeMarker,
    clearMarkers,
    addZoneLayer,
    addRoutingLayer,
    addTrafficLayer,
    removeLayer,
    toggleLayer,
    fitBounds,
    getViewState,
    setViewState,
    cleanup,
  }
}
