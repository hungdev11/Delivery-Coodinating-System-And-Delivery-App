/**
 * TomTom Traffic API Service
 * 
 * Fetches real-time traffic data from TomTom API and updates database
 * Based on: https://developer.tomtom.com/traffic-api/documentation/traffic-incidents/incident-viewport
 */

import { PrismaClient, TrafficLevel } from '@prisma/client';
import axios from 'axios';
import { calculateDeltaWeight, getTrafficMultiplier, recalculateCurrentWeight } from '../utils/weight-calculator.js';

const prisma = new PrismaClient();

interface TomTomTrafficIncident {
  id: string;
  point: {
    lat: number;
    lon: number;
  };
  severity: string;
  delay: number;
  description: string;
  roadName?: string;
  from: string;
  to: string;
  length: number;
  roadClosure: boolean;
  startTime: string;
  endTime?: string;
  zone_id?: string; // Added for zone-based traffic
  zone_name?: string; // Added for zone-based traffic
}

interface TomTomTrafficFlow {
  frc: string; // Functional Road Class
  currentSpeed: number;
  freeFlowSpeed: number;
  confidence: number;
  roadClosure: boolean;
}

class TomTomTrafficService {
  private apiKey: string;
  private baseUrl = 'https://api.tomtom.com/traffic/services/4';

  constructor() {
    this.apiKey = process.env.TOMTOM_API_KEY || '';
    if (!this.apiKey) {
      throw new Error('TOMTOM_API_KEY environment variable is required');
    }
  }

  /**
   * Get traffic viewport info and traffic model ID
   */
  async getTrafficModelId(boundingBox: string, zoom: number = 10): Promise<string> {
    const tomTomBbox = this.convertBoundingBox(boundingBox);
    const url = `${this.baseUrl}/incidentViewport/${tomTomBbox}/${zoom}/${tomTomBbox}/${zoom}/true/json`;
    
    console.log(`🔍 Getting traffic model ID from: ${url}`);
    
    try {
      const response = await axios.get(url, {
        params: { key: this.apiKey },
        timeout: 10000
      });

      console.log(`📊 Traffic model response:`, response.data);
      return response.data.viewpResp.trafficState['@trafficModelId'];
    } catch (error: any) {
      console.error('Failed to get traffic model ID:', error.message);
      if (error.response) {
        console.error('Response status:', error.response.status);
        console.error('Response data:', error.response.data);
      }
      throw error;
    }
  }

  /**
   * Convert lat/lon to Web Mercator (EPSG:3857)
   */
  private latLonToWebMercator(lat: number, lon: number): { x: number, y: number } {
    const x = lon * 20037508.34 / 180;
    const y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
    const yMercator = y * 20037508.34 / 180;
    return { x, y: yMercator };
  }

  /**
   * Convert lat/lon bounding box to TomTom Web Mercator format
   */
  private convertBoundingBox(boundingBox: string): string {
    // Input: "10.3,106.3,11.2,107.0" (minLat,minLon,maxLat,maxLon)
    // Output: "minY,minX,maxY,maxX" in Web Mercator projection
    const [minLat, minLon, maxLat, maxLon] = boundingBox.split(',').map(Number);
    
    console.log(`🗺️  Input bbox: ${minLat}, ${minLon}, ${maxLat}, ${maxLon}`);
    
    const minPoint = this.latLonToWebMercator(minLat, minLon);
    const maxPoint = this.latLonToWebMercator(maxLat, maxLon);
    
    console.log(`🗺️  Min point (Web Mercator): ${minPoint.x}, ${minPoint.y}`);
    console.log(`🗺️  Max point (Web Mercator): ${maxPoint.x}, ${maxPoint.y}`);
    
    // TomTom format: minY,minX,maxY,maxX
    const result = `${minPoint.y},${minPoint.x},${maxPoint.y},${maxPoint.x}`;
    console.log(`🗺️  TomTom bbox: ${result}`);
    
    return result;
  }

  /**
   * Fetch traffic flow data for multiple points across zones
   */
  async fetchTrafficFlowForZones(): Promise<any[]> {
    console.log('🗺️  Fetching traffic data for all zones...');
    
    // Get all zones with their centroids
    console.log('📊 Querying zones from database...');
    const zones = await prisma.zones.findMany({
      select: {
        zone_id: true,
        name: true,
        polygon: true
      }
    });

    console.log(`📍 Found ${zones.length} zones to scan`);

    const allFlowData: any[] = [];
    let processedZones = 0;

    for (const zone of zones) {
      try {
        console.log(`\n🔍 Processing zone ${processedZones + 1}/${zones.length}: ${zone.name}`);
        
        // Calculate zone centroid
        console.log(`   📐 Calculating centroid...`);
        const centroid = this.calculateZoneCentroid(zone.polygon);
        if (!centroid) {
          console.log(`   ⚠️  No centroid found for zone ${zone.name}`);
          processedZones++;
          continue;
        }

        console.log(`   📍 Centroid: ${centroid.lat}, ${centroid.lon}`);

        // Fetch traffic flow for this zone
        console.log(`   🌐 Calling TomTom API...`);
        const flowData = await this.fetchTrafficFlowForPoint(centroid.lat, centroid.lon);
        
        if (flowData) {
          console.log(`   ✅ Got traffic data: ${flowData.currentSpeed} km/h (free flow: ${flowData.freeFlowSpeed} km/h)`);
          allFlowData.push({
            ...flowData,
            zone_id: zone.zone_id,
            zone_name: zone.name,
            point: { lat: centroid.lat, lon: centroid.lon }
          });
        } else {
          console.log(`   ⚠️  No traffic data returned for zone ${zone.name}`);
        }

        // Small delay to avoid rate limiting
        console.log(`   ⏳ Waiting 100ms to avoid rate limiting...`);
        await new Promise(resolve => setTimeout(resolve, 100));
        
        processedZones++;
        
      } catch (error) {
        console.error(`❌ Failed to fetch traffic for zone ${zone.name}:`, error);
        processedZones++;
      }
    }

    console.log(`\n📊 Zone scanning completed:`);
    console.log(`   📊 Processed zones: ${processedZones}/${zones.length}`);
    console.log(`   📊 Total traffic data points: ${allFlowData.length}`);
    return allFlowData;
  }

  /**
   * Calculate centroid of a zone polygon
   */
  private calculateZoneCentroid(polygon: any): { lat: number, lon: number } | null {
    if (!polygon || !polygon.coordinates || !polygon.coordinates[0]) return null;
    
    const coords = polygon.coordinates[0];
    let latSum = 0, lonSum = 0;
    
    for (const coord of coords) {
      lonSum += coord[0];
      latSum += coord[1];
    }
    
    return {
      lat: latSum / coords.length,
      lon: lonSum / coords.length
    };
  }

  /**
   * Fetch traffic flow data for a specific point
   */
  private async fetchTrafficFlowForPoint(lat: number, lon: number): Promise<any | null> {
    const url = `${this.baseUrl}/flowSegmentData/absolute/10/json`;
    
    console.log(`   🌐 API URL: ${url}`);
    console.log(`   📍 Point: ${lat}, ${lon}`);
    
    try {
      console.log(`   ⏳ Making API request...`);
      const response = await axios.get(url, {
        params: { 
          key: this.apiKey,
          point: `${lat},${lon}`,
          unit: 'KMPH'
        },
        timeout: 10000
      });

      console.log(`   📊 Response status: ${response.status}`);
      console.log(`   📊 Response data:`, response.data);

      const flowData = response.data.flowSegmentData || null;
      if (flowData) {
        console.log(`   ✅ Flow data: ${flowData.currentSpeed} km/h (free: ${flowData.freeFlowSpeed} km/h)`);
      } else {
        console.log(`   ⚠️  No flow data in response`);
      }

      return flowData;
    } catch (error: any) {
      console.error(`   ❌ API call failed for ${lat},${lon}:`, error.message);
      if (error.response) {
        console.error(`   📊 Response status: ${error.response.status}`);
        console.error(`   📊 Response data:`, error.response.data);
      }
      return null;
    }
  }

  /**
   * Fetch traffic flow data (legacy method for single point)
   */
  async fetchTrafficFlow(boundingBox: string, zoom: number = 10): Promise<any[]> {
    // Use zone-based approach instead
    return await this.fetchTrafficFlowForZones();
  }

  /**
   * Fetch traffic incidents for a bounding box
   */
  async fetchTrafficIncidents(
    boundingBox: string, 
    trafficModelId: string,
    zoom: number = 10
  ): Promise<TomTomTrafficIncident[]> {
    // Use zone-based traffic flow approach
    console.log('🗺️  Using zone-based traffic flow approach...');
    const flowData = await this.fetchTrafficFlow(boundingBox, zoom);
    
    if (flowData.length > 0) {
      console.log(`📊 Found ${flowData.length} flow segments across zones`);
      // Convert flow data to incident-like format with zone info
      return flowData.map((flow: any, index: number) => ({
        id: `flow_${flow.zone_id}_${index}`,
        point: {
          lat: flow.point.lat,
          lon: flow.point.lon
        },
        severity: flow.currentSpeed < flow.freeFlowSpeed * 0.5 ? 'major' : 'minor',
        delay: Math.max(0, (flow.freeFlowSpeed - flow.currentSpeed) * 2),
        description: `Traffic flow in ${flow.zone_name}: ${flow.currentSpeed} km/h`,
        roadName: flow.zone_name || 'Unknown',
        from: 'Unknown',
        to: 'Unknown',
        length: 0,
        roadClosure: false,
        startTime: new Date().toISOString(),
        endTime: undefined,
        zone_id: flow.zone_id,
        zone_name: flow.zone_name
      }));
    }
    
    return [];
  }

  /**
   * Convert TomTom severity to our TrafficLevel enum
   */
  private mapSeverityToTrafficLevel(severity: string, roadClosure: boolean): TrafficLevel {
    if (roadClosure) return 'BLOCKED';
    
    switch (severity.toLowerCase()) {
      case 'minor':
        return 'SLOW';
      case 'moderate':
        return 'CONGESTED';
      case 'major':
        return 'CONGESTED';
      case 'critical':
        return 'BLOCKED';
      default:
        return 'NORMAL';
    }
  }

  /**
   * Calculate congestion score from delay and severity
   */
  private calculateCongestionScore(delay: number, severity: string): number {
    const baseScore = Math.min(delay / 60, 50); // Max 50 from delay
    
    const severityBonus: Record<string, number> = {
      'minor': 10,
      'moderate': 25,
      'major': 40,
      'critical': 60
    };
    
    return Math.min(baseScore + (severityBonus[severity.toLowerCase()] || 0), 100);
  }

  /**
   * Find road segments affected by traffic incidents
   */
  private async findAffectedSegments(incident: TomTomTrafficIncident): Promise<any[]> {
    console.log(`🔍 Looking for segments for traffic at ${incident.point.lat}, ${incident.point.lon}`);
    
    // If incident has zone_id, find segments in that zone
    if (incident.zone_id) {
      console.log(`📍 Looking for segments in zone: ${incident.zone_id}`);
      
      const segments = await prisma.road_segments.findMany({
        where: {
          zone_id: incident.zone_id
        },
        include: {
          from_node: true,
          to_node: true
        }
      });

      console.log(`📍 Found ${segments.length} segments in zone ${incident.zone_id}`);
      return segments;
    }
    
    // Fallback: find segments within radius
    const radius = 0.01; // ~1km in degrees
    const segments = await prisma.road_segments.findMany({
      where: {
        OR: [
          // Check if any coordinate in the geometry is within radius
          {
            geometry: {
              path: 'coordinates' as const,
              array_contains: [
                [
                  [incident.point.lon - radius, incident.point.lat - radius],
                  [incident.point.lon + radius, incident.point.lat + radius]
                ]
              ]
            }
          },
          // Fallback: get some segments from the same area
          {
            zone_id: {
              not: null
            }
          }
        ]
      },
      include: {
        from_node: true,
        to_node: true
      },
      take: 20 // Increased limit
    });

    console.log(`📍 Found ${segments.length} segments near traffic point`);
    return segments;
  }

  /**
   * Update traffic conditions in database using batch operations
   */
  async updateTrafficConditions(incidents: TomTomTrafficIncident[]): Promise<void> {
    console.log(`📊 Updating traffic conditions for ${incidents.length} incidents...`);

    let totalSegments = 0;
    const trafficConditionsToUpsert: any[] = [];
    const segmentUpdates: any[] = [];

    // Collect all segments and prepare batch operations
    for (const incident of incidents) {
      try {
        console.log(`🔍 Processing incident: ${incident.id} (${incident.zone_name})`);
        
        // Find affected segments
        const segments = await this.findAffectedSegments(incident);
        
        if (segments.length === 0) {
          console.log(`   ⚠️  No segments found for incident ${incident.id}`);
          continue;
        }

        console.log(`   📍 Found ${segments.length} segments`);

        const trafficLevel = this.mapSeverityToTrafficLevel(incident.severity, incident.roadClosure);
        const congestionScore = this.calculateCongestionScore(incident.delay, incident.severity);
        const weightMultiplier = getTrafficMultiplier(trafficLevel, congestionScore);
        const expiresAt = new Date(Date.now() + 60 * 60 * 1000);

        // Prepare batch data
        for (const segment of segments) {
          const deltaWeight = calculateDeltaWeight(segment.base_weight, weightMultiplier);
          const newCurrentWeight = recalculateCurrentWeight(segment.base_weight, deltaWeight);

          // Traffic conditions to upsert
          trafficConditionsToUpsert.push({
            segment_id: segment.segment_id,
            traffic_level: trafficLevel,
            congestion_score: congestionScore,
            current_speed: segment.avg_speed ? segment.avg_speed * (1 / weightMultiplier) : null,
            speed_ratio: segment.avg_speed ? (1 / weightMultiplier) : null,
            weight_multiplier: weightMultiplier,
            source: 'tomtom',
            source_timestamp: new Date(incident.startTime),
            expires_at: expiresAt
          });

          // Segment updates
          segmentUpdates.push({
            segment_id: segment.segment_id,
            current_weight: newCurrentWeight,
            delta_weight: deltaWeight,
            weight_updated_at: new Date()
          });
        }

        totalSegments += segments.length;
        console.log(`   ✅ Prepared ${segments.length} segments for batch update`);
        
      } catch (error) {
        console.error(`❌ Failed to process incident ${incident.id}:`, error);
      }
    }

    console.log(`\n📊 Batch operations prepared:`);
    console.log(`   📊 Total segments: ${totalSegments}`);
    console.log(`   📊 Traffic conditions: ${trafficConditionsToUpsert.length}`);
    console.log(`   📊 Segment updates: ${segmentUpdates.length}`);

    // Execute batch operations
    try {
      console.log(`\n💾 Step 1: Batch upserting traffic conditions...`);
      
      // Use createMany with skipDuplicates for traffic conditions
      // Note: This will skip duplicates but won't update existing records
      // For true upsert, we need to use raw SQL or individual upserts
      const trafficResult = await prisma.traffic_conditions.createMany({
        data: trafficConditionsToUpsert,
        skipDuplicates: true
      });
      console.log(`   ✅ Created ${trafficResult.count} traffic conditions`);

      console.log(`\n💾 Step 2: Batch updating road segments...`);
      
      // Use updateMany for segments (this won't work with different values per segment)
      // We need to use raw SQL for batch updates with different values
      const segmentUpdatePromises = segmentUpdates.map(update => 
        prisma.road_segments.update({
          where: { segment_id: update.segment_id },
          data: {
            current_weight: update.current_weight,
            delta_weight: update.delta_weight,
            weight_updated_at: update.weight_updated_at
          }
        })
      );

      // Execute in smaller batches to avoid connection pool exhaustion
      const batchSize = 100
      for (let i = 0; i < segmentUpdatePromises.length; i += batchSize) {
        const batch = segmentUpdatePromises.slice(i, i + batchSize);
        await Promise.all(batch);
        console.log(`   📊 Updated batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(segmentUpdatePromises.length / batchSize)}`);
        
        // Add small delay between batches to prevent connection pool exhaustion
        if (i + batchSize < segmentUpdatePromises.length) {
          await new Promise(resolve => setTimeout(resolve, 100)); // 100ms delay
        }
      }

      console.log(`\n✅ Batch operations completed:`);
      console.log(`   📊 Traffic conditions: ${trafficResult.count} created`);
      console.log(`   📊 Road segments: ${segmentUpdates.length} updated`);

    } catch (error) {
      console.error(`❌ Batch operations failed:`, error);
      throw error;
    }
  }

  /**
   * Clean up expired traffic conditions
   */
  async cleanupExpiredConditions(): Promise<void> {
    const deleted = await prisma.traffic_conditions.deleteMany({
      where: {
        expires_at: {
          lt: new Date()
        }
      }
    });

    if (deleted.count > 0) {
      console.log(`🧹 Cleaned up ${deleted.count} expired traffic conditions`);
    }
  }

  /**
   * Main method to fetch and update traffic data
   */
  async fetchAndUpdateTrafficData(boundingBox: string): Promise<void> {
    try {
      console.log('🌐 Fetching traffic data from TomTom API...');
      console.log(`📊 Bounding box: ${boundingBox}`);
      
      // Clean up expired data first
      console.log('🧹 Step 1: Cleaning up expired conditions...');
      await this.cleanupExpiredConditions();
      console.log('✅ Cleanup completed');

      // Get traffic model ID
      console.log('📡 Step 2: Getting traffic model ID...');
      const trafficModelId = await this.getTrafficModelId(boundingBox);
      console.log(`✅ Traffic Model ID: ${trafficModelId}`);

      // Fetch incidents
      console.log('🚨 Step 3: Fetching traffic incidents...');
      const incidents = await this.fetchTrafficIncidents(boundingBox, trafficModelId);
      console.log(`✅ Found ${incidents.length} traffic incidents`);

      if (incidents.length > 0) {
        // Update database
        console.log('💾 Step 4: Updating database...');
        await this.updateTrafficConditions(incidents);
        console.log('✅ Database update completed');
      } else {
        console.log('ℹ️  No traffic incidents found, skipping database update');
      }

      console.log('🎉 Traffic data fetch and update completed successfully!');

    } catch (error) {
      console.error('❌ Failed to fetch traffic data:', error);
      throw error;
    }
  }
}

export { TomTomTrafficService };
