/**
 * Utility to parse OSM PBF files and extract road network data
 * Uses osmium-tool for efficient PBF parsing
 */

import { readFileSync, existsSync, mkdirSync, unlinkSync } from 'fs';
import { join, dirname } from 'path';
import { tmpdir } from 'os';
import { OsmiumWrapper, parseOsmiumGeoJson } from './osmium-wrapper.js';

export interface OSMNode {
  id: string;
  lat: number;
  lon: number;
  tags?: Record<string, string>;
}

export interface OSMWay {
  id: string;
  nodes: string[]; // Array of node IDs
  tags: Record<string, string>;
}

export interface RoadData {
  nodes: Map<string, OSMNode>;
  ways: OSMWay[];
}

/**
 * OSM Parser using osmium-tool for efficient PBF parsing
 */
export class OSMParser {
  private nodes: Map<string, OSMNode> = new Map();
  private ways: OSMWay[] = [];
  private osmium: OsmiumWrapper;
  private verbose: boolean;

  constructor(verbose: boolean = false) {
    this.osmium = new OsmiumWrapper(verbose);
    this.verbose = verbose;
  }

  /**
   * Parse OSM PBF file using osmium-tool
   * This extracts road network data efficiently
   *
   * @param filePath - Path to OSM PBF file
   * @param polyFile - Optional poly file to filter by boundary
   */
  async parsePBF(filePath: string, polyFile?: string): Promise<RoadData> {
    console.log(`Parsing OSM PBF file: ${filePath}`);
    if (polyFile) {
      console.log(`Filtering by polygon: ${polyFile}`);
    }

    // Check if osmium is installed
    const isInstalled = await this.osmium.checkInstallation();
    if (!isInstalled) {
      throw new Error('osmium-tool is required but not installed');
    }

    // Check if input file exists
    if (!existsSync(filePath)) {
      throw new Error(`Input file not found: ${filePath}`);
    }

    // Check if poly file exists
    if (polyFile && !existsSync(polyFile)) {
      throw new Error(`Poly file not found: ${polyFile}`);
    }

    // Get file info
    if (this.verbose) {
      try {
        const fileInfo = await this.osmium.getFileInfo(filePath);
        console.log('File info:', {
          bbox: fileInfo.header?.box,
          nodes: fileInfo.data?.count?.nodes,
          ways: fileInfo.data?.count?.ways,
        });
      } catch (error) {
        // File info is optional, continue if it fails
      }
    }

    // Create temporary directory for processing
    const tempDir = join(tmpdir(), 'osmium-parse-' + Date.now());
    if (!existsSync(tempDir)) {
      mkdirSync(tempDir, { recursive: true });
    }

    try {
      let currentPbf = filePath;

      // Step 0: Filter by polygon if provided
      if (polyFile) {
        console.log('Step 0: Filtering by polygon boundary...');
        const polyFilteredPbf = join(tempDir, 'poly-filtered.osm.pbf');
        await this.osmium.extractByPoly(filePath, polyFilteredPbf, polyFile);
        currentPbf = polyFilteredPbf;
        console.log('✓ Polygon filtered');
      }

      // Step 1: Extract only roads to a filtered PBF
      console.log('Step 1: Filtering roads from PBF...');
      const filteredPbf = join(tempDir, 'roads-filtered.osm.pbf');
      await this.osmium.extractRoads(currentPbf, filteredPbf);
      console.log('✓ Roads filtered');

      // Step 2: Convert filtered PBF to GeoJSON
      console.log('Step 2: Converting to GeoJSON...');
      const geoJsonFile = join(tempDir, 'roads.geojson');
      await this.osmium.extractToJson(filteredPbf, geoJsonFile);
      console.log('✓ Converted to GeoJSON');

      // Step 3: Parse GeoJSON
      console.log('Step 3: Parsing GeoJSON...');
      const geoJson = await parseOsmiumGeoJson(geoJsonFile);

      // GeoJSON FeatureCollection from osmium export
      if (geoJson.type !== 'FeatureCollection') {
        throw new Error('Expected GeoJSON FeatureCollection');
      }

      // Process features
      for (const feature of geoJson.features) {
        if (feature.geometry.type === 'LineString') {
          // This is a way (road)
          const way = this.parseWayFromGeoJson(feature);
          if (way) {
            this.ways.push(way);
          }
        } else if (feature.geometry.type === 'Point') {
          // This is a node
          const node = this.parseNodeFromGeoJson(feature);
          if (node) {
            this.nodes.set(node.id, node);
          }
        }
      }

      console.log(`✓ Parsed ${this.nodes.size} nodes and ${this.ways.length} ways`);

      return {
        nodes: this.nodes,
        ways: this.ways,
      };
    } finally {
      // Clean up temp directory
      try {
        const { rmSync } = await import('fs');
        if (existsSync(tempDir)) {
          rmSync(tempDir, { recursive: true, force: true });
        }
      } catch (error) {
        // Cleanup is best effort
        if (this.verbose) {
          console.warn('Failed to clean up temp directory:', tempDir);
        }
      }
    }
  }

  /**
   * Parse a way from GeoJSON feature
   */
  private parseWayFromGeoJson(feature: any): OSMWay | null {
    const props = feature.properties || {};
    const osmId = String(feature.id || feature['@id'] || `way_${Date.now()}_${Math.random()}`);

    // Extract node references from geometry
    const coordinates = feature.geometry.coordinates;
    if (!coordinates || coordinates.length < 2) {
      return null;
    }

    const nodeIds: string[] = [];

    // For LineString, create synthetic node IDs from coordinates
    // In a real scenario with full OSM data, these would be actual node IDs
    for (let i = 0; i < coordinates.length; i++) {
      const [lon, lat] = coordinates[i];
      const nodeId = `${osmId}_node_${i}`;
      nodeIds.push(nodeId);

      // Create node if it doesn't exist
      if (!this.nodes.has(nodeId)) {
        this.nodes.set(nodeId, {
          id: nodeId,
          lat,
          lon,
          tags: {},
        });
      }
    }

    return {
      id: osmId,
      nodes: nodeIds,
      tags: this.extractTags(props),
    };
  }

  /**
   * Parse a node from GeoJSON feature
   */
  private parseNodeFromGeoJson(feature: any): OSMNode | null {
    const props = feature.properties || {};
    const osmId = String(feature.id || feature['@id'] || `node_${Date.now()}_${Math.random()}`);

    const [lon, lat] = feature.geometry.coordinates;

    return {
      id: osmId,
      lat,
      lon,
      tags: this.extractTags(props),
    };
  }

  /**
   * Extract OSM tags from GeoJSON properties
   */
  private extractTags(props: any): Record<string, string> {
    const tags: Record<string, string> = {};

    // Keep all properties as tags
    for (const [key, value] of Object.entries(props)) {
      if (key.startsWith('@')) continue; // Skip osmium metadata
      if (value !== null && value !== undefined) {
        tags[key] = String(value);
      }
    }

    return tags;
  }

  /**
   * Filter ways to only include roads
   */
  static isRoadWay(way: OSMWay): boolean {
    const highwayTypes = [
      'motorway', 'trunk', 'primary', 'secondary', 'tertiary',
      'unclassified', 'residential', 'service', 'motorway_link',
      'trunk_link', 'primary_link', 'secondary_link', 'tertiary_link',
      'living_street', 'pedestrian', 'track', 'road'
    ];

    return way.tags.highway && highwayTypes.includes(way.tags.highway);
  }

  /**
   * Extract road type from OSM tags
   */
  static getRoadType(tags: Record<string, string>): string {
    const highway = tags.highway?.toUpperCase();

    const typeMapping: Record<string, string> = {
      'MOTORWAY': 'MOTORWAY',
      'MOTORWAY_LINK': 'MOTORWAY',
      'TRUNK': 'TRUNK',
      'TRUNK_LINK': 'TRUNK',
      'PRIMARY': 'PRIMARY',
      'PRIMARY_LINK': 'PRIMARY',
      'SECONDARY': 'SECONDARY',
      'SECONDARY_LINK': 'SECONDARY',
      'TERTIARY': 'TERTIARY',
      'TERTIARY_LINK': 'TERTIARY',
      'RESIDENTIAL': 'RESIDENTIAL',
      'SERVICE': 'SERVICE',
      'LIVING_STREET': 'LIVING_STREET',
      'PEDESTRIAN': 'PEDESTRIAN',
      'TRACK': 'TRACK',
      'PATH': 'PATH',
    };

    return typeMapping[highway] || 'UNCLASSIFIED';
  }

  /**
   * Extract speed information from OSM tags
   */
  static getSpeedInfo(tags: Record<string, string>): { maxSpeed?: number; avgSpeed?: number } {
    let maxSpeed: number | undefined;

    if (tags.maxspeed) {
      const speed = parseInt(tags.maxspeed);
      if (!isNaN(speed)) {
        maxSpeed = speed;
      }
    }

    // Estimate average speed based on road type if not specified
    const avgSpeed = maxSpeed ? maxSpeed * 0.7 : this.estimateAvgSpeed(tags.highway);

    return { maxSpeed, avgSpeed };
  }

  /**
   * Estimate average speed based on road type
   */
  static estimateAvgSpeed(roadType?: string): number {
    const speedMap: Record<string, number> = {
      'motorway': 80,
      'trunk': 60,
      'primary': 50,
      'secondary': 40,
      'tertiary': 35,
      'residential': 25,
      'service': 20,
      'living_street': 15,
      'unclassified': 30,
    };

    return speedMap[roadType?.toLowerCase() || 'unclassified'] || 30;
  }

  /**
   * Check if way is one-way
   */
  static isOneWay(tags: Record<string, string>): boolean {
    return tags.oneway === 'yes' || tags.oneway === '1' || tags.oneway === 'true';
  }

  /**
   * Get number of lanes
   */
  static getLanes(tags: Record<string, string>): number | undefined {
    if (tags.lanes) {
      const lanes = parseInt(tags.lanes);
      if (!isNaN(lanes)) {
        return lanes;
      }
    }
    return undefined;
  }

  /**
   * Get road surface type
   */
  static getSurface(tags: Record<string, string>): string | undefined {
    return tags.surface;
  }

  /**
   * Get road name in Vietnamese
   */
  static getRoadName(tags: Record<string, string>): { name?: string; nameEn?: string } {
    return {
      name: tags.name || tags['name:vi'],
      nameEn: tags['name:en'],
    };
  }
}

/**
 * Calculate distance between two coordinates (Haversine formula)
 */
export function calculateDistance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371e3; // Earth radius in meters
  const φ1 = (lat1 * Math.PI) / 180;
  const φ2 = (lat2 * Math.PI) / 180;
  const Δφ = ((lat2 - lat1) * Math.PI) / 180;
  const Δλ = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
    Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c; // Distance in meters
}

/**
 * Calculate length of a linestring
 */
export function calculateLineLength(coordinates: Array<[number, number]>): number {
  let totalLength = 0;

  for (let i = 1; i < coordinates.length; i++) {
    const [lon1, lat1] = coordinates[i - 1];
    const [lon2, lat2] = coordinates[i];
    totalLength += calculateDistance(lat1, lon1, lat2, lon2);
  }

  return totalLength;
}
