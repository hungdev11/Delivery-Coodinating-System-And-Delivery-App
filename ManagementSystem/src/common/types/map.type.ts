/**
 * Map Types and Interfaces
 *
 * Types for map components, layers, and interactions
 */

import type { Map, LngLatLike, LngLatBoundsLike } from 'maplibre-gl'

/**
 * Map layer types
 */
export type MapLayerType = 'routing' | 'traffic' | 'zones' | 'markers' | 'heatmap' | 'custom'

/**
 * Map layer configuration
 */
export interface MapLayer {
  id: string
  type: MapLayerType
  visible: boolean
  data?: any
  style?: any
  options?: Record<string, any>
}

/**
 * Traffic data point
 */
export interface TrafficData {
  coordinates: [number, number]
  speed: number
  congestionLevel: 'low' | 'medium' | 'high' | 'severe'
  timestamp: string
}

/**
 * Route data
 */
export interface RouteData {
  coordinates: [number, number][]
  distance: number
  duration: number
  properties?: Record<string, any>
}

/**
 * Map marker
 */
export interface MapMarker {
  id: string
  coordinates: [number, number]
  type?: 'center' | 'delivery' | 'warehouse' | 'custom'
  label?: string
  icon?: string
  color?: string
  popup?: string
  onClick?: () => void
}

/**
 * Zone polygon
 */
export interface ZonePolygon {
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

/**
 * Map configuration
 */
export interface MapConfig {
  container: string | HTMLElement
  style?: string
  center?: [number, number]
  zoom?: number
  minZoom?: number
  maxZoom?: number
  bounds?: LngLatBoundsLike
  pitch?: number
  bearing?: number
  interactive?: boolean
  attributionControl?: boolean
}

/**
 * Map view state
 */
export interface MapViewState {
  center: [number, number]
  zoom: number
  bearing: number
  pitch: number
  bounds?: LngLatBoundsLike
}

/**
 * Map event handlers
 */
export interface MapEventHandlers {
  onClick?: (event: { lngLat: [number, number]; point: [number, number] }) => void
  onLoad?: (map: Map) => void
  onMoveEnd?: (viewState: MapViewState) => void
  onZoomEnd?: (zoom: number) => void
}

/**
 * Layer style configuration
 */
export interface LayerStyle {
  type: 'fill' | 'line' | 'symbol' | 'circle' | 'heatmap' | 'fill-extrusion'
  paint?: Record<string, any>
  layout?: Record<string, any>
  filter?: any[]
  minzoom?: number
  maxzoom?: number
}

/**
 * GeoJSON Feature
 */
export interface GeoJSONFeature {
  type: 'Feature'
  geometry: {
    type: 'Point' | 'LineString' | 'Polygon' | 'MultiPoint' | 'MultiLineString' | 'MultiPolygon'
    coordinates: any
  }
  properties?: Record<string, any>
}

/**
 * GeoJSON FeatureCollection
 */
export interface GeoJSONFeatureCollection {
  type: 'FeatureCollection'
  features: GeoJSONFeature[]
}

/**
 * Map layer source
 */
export interface MapLayerSource {
  type: 'geojson' | 'vector' | 'raster' | 'raster-dem' | 'image' | 'video'
  data?: string | GeoJSONFeatureCollection
  url?: string
  tiles?: string[]
  bounds?: [number, number, number, number]
  minzoom?: number
  maxzoom?: number
  attribution?: string
}

/**
 * Traffic layer options
 */
export interface TrafficLayerOptions {
  colors?: {
    low: string
    medium: string
    high: string
    severe: string
  }
  opacity?: number
  lineWidth?: number
}

/**
 * Routing layer options
 */
export interface RoutingLayerOptions {
  color?: string
  width?: number
  opacity?: number
  showArrows?: boolean
  showWaypoints?: boolean
}

/**
 * Zone layer options
 */
export interface ZoneLayerOptions {
  fillOpacity?: number
  strokeWidth?: number
  showLabels?: boolean
  interactive?: boolean
}
