"use strict";
/**
 * Generate Service
 * Handles OSRM data generation from database
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.GenerateService = void 0;
const fs_1 = require("fs");
const path_1 = require("path");
const child_process_1 = require("child_process");
const util_1 = require("util");
const logger_1 = require("../common/logger");
const build_tracker_service_1 = require("./build-tracker.service");
const execAsync = (0, util_1.promisify)(child_process_1.exec);
const MODELS = [
    {
        name: 'osrm-full',
        useRating: true,
        useBlocking: true,
        description: 'Full model: rating affects weight, blocking affects speed',
    },
    {
        name: 'osrm-rating-only',
        useRating: true,
        useBlocking: false,
        description: 'Rating only: user feedback affects weight',
    },
    {
        name: 'osrm-blocking-only',
        useRating: false,
        useBlocking: true,
        description: 'Blocking only: traffic affects speed',
    },
    {
        name: 'osrm-base',
        useRating: false,
        useBlocking: false,
        description: 'Base: VN motorbike profile with no modifiers',
    },
];
class GenerateService {
    prisma;
    osrmDataPath;
    buildTracker;
    constructor(prisma) {
        this.prisma = prisma;
        this.osrmDataPath = process.env.OSRM_DATA_PATH || './osrm_data';
        this.buildTracker = new build_tracker_service_1.BuildTrackerService(prisma);
    }
    /**
     * Generate all OSRM models (sync-like: sequential processing)
     */
    async generateAllModels() {
        const instanceName = 'generate-all-models';
        return this.buildTracker.executeSequentially(instanceName, async (buildId) => {
            return this._generateAllModels(buildId);
        });
    }
    async _generateAllModels(buildId) {
        const startTime = Date.now();
        try {
            logger_1.logger.info(`Starting OSRM V2 generation (Build ID: ${buildId})...`);
            // Step 1: Fetch data from database
            const roadNetworkData = await this.fetchRoadNetworkData();
            // Update build with segment count
            await this.prisma.osrm_builds.update({
                where: { build_id: buildId },
                data: {
                    total_segments: roadNetworkData.segments.length,
                },
            });
            // Step 2: Export to OSM XML
            const sharedXmlPath = await this.exportToOSMXML(roadNetworkData);
            // Step 3: Setup all model instances
            logger_1.logger.info('Setting up model instances...');
            for (const config of MODELS) {
                await this.setupModelInstance(config, sharedXmlPath);
            }
            // Step 4: Process all OSRM instances
            logger_1.logger.info('Processing OSRM instances...');
            const buildResults = [];
            for (const config of MODELS) {
                const modelBuildId = await this.startModelBuild(config.name, roadNetworkData.segments.length);
                try {
                    await this.processOSRMInstance(config);
                    const outputPath = (0, path_1.join)(this.osrmDataPath, config.name, 'network.osrm');
                    await this.buildTracker.markReady(modelBuildId, outputPath);
                    buildResults.push({ model: config.name, buildId: modelBuildId });
                }
                catch (error) {
                    await this.buildTracker.markFailed(modelBuildId, error.message);
                    throw error;
                }
            }
            // Mark main build as READY
            await this.buildTracker.markReady(buildId, sharedXmlPath);
            const duration = Date.now() - startTime;
            const modelNames = MODELS.map(m => m.name);
            logger_1.logger.info(`Generation completed in ${duration}ms`);
            return {
                success: true,
                models: modelNames,
                duration,
            };
        }
        catch (error) {
            logger_1.logger.error('Generation failed', { error: error.message, buildId });
            await this.buildTracker.markFailed(buildId, error.message);
            return {
                success: false,
                error: error.message,
                duration: Date.now() - startTime,
            };
        }
    }
    async startModelBuild(instanceName, totalSegments) {
        return this.buildTracker.startBuild(instanceName, totalSegments);
    }
    async fetchRoadNetworkData() {
        logger_1.logger.info('Fetching road network data from database...');
        const [roads, nodes] = await Promise.all([
            this.prisma.roads.findMany(),
            this.prisma.road_nodes.findMany(),
        ]);
        logger_1.logger.info(`Found ${roads.length} roads, ${nodes.length} nodes`);
        // Load segments with feedback and traffic
        const BATCH_SIZE = 20000;
        const segments = [];
        let offset = 0;
        while (true) {
            const batch = await this.prisma.road_segments.findMany({
                skip: offset,
                take: BATCH_SIZE,
                include: {
                    from_node: true,
                    to_node: true,
                    user_feedback: {
                        orderBy: { created_at: 'desc' },
                        take: 10,
                    },
                    traffic_conditions: {
                        where: { expires_at: { gte: new Date() } },
                        orderBy: { source_timestamp: 'desc' },
                        take: 1,
                    },
                },
            });
            if (batch.length === 0)
                break;
            segments.push(...batch);
            offset += BATCH_SIZE;
            if (batch.length < BATCH_SIZE)
                break;
        }
        logger_1.logger.info(`Loaded ${segments.length} segments`);
        // Calculate ratings and traffic
        const userFeedback = new Map();
        const trafficValue = new Map();
        for (const segment of segments) {
            const rating = this.calculateUserRating(segment);
            if (rating !== null) {
                userFeedback.set(segment.segment_id, rating);
            }
            const traffic = this.calculateBlockingStatus(segment);
            trafficValue.set(segment.segment_id, traffic);
        }
        return { roads, nodes, segments, userFeedback, trafficValue };
    }
    calculateUserRating(segment) {
        if (!segment.user_feedback || segment.user_feedback.length === 0) {
            return null;
        }
        // Calculate rating from weight_adjustment or severity
        // If weight_adjustment exists, use it to infer rating
        // Otherwise, use severity as a proxy (MINOR=good, CRITICAL=bad)
        const ratings = [];
        for (const feedback of segment.user_feedback) {
            // If weight_adjustment exists, convert to rating (negative = bad, positive = good)
            if (feedback.weight_adjustment !== null && feedback.weight_adjustment !== undefined) {
                // weight_adjustment > 0 means road is worse (higher weight = avoid)
                // Convert to 0-1 scale: -1.0 (very bad) -> 0.0, 0.0 (neutral) -> 0.5, 1.0 (very good) -> 1.0
                const normalized = Math.max(0, Math.min(1, 0.5 - feedback.weight_adjustment * 0.5));
                ratings.push(normalized);
            }
            else if (feedback.severity) {
                // Use severity as proxy: MINOR=good (0.8), MODERATE=ok (0.6), MAJOR=bad (0.4), CRITICAL=very bad (0.2)
                const severityMap = {
                    MINOR: 0.8,
                    MODERATE: 0.6,
                    MAJOR: 0.4,
                    CRITICAL: 0.2,
                };
                const rating = severityMap[feedback.severity] || 0.5;
                ratings.push(rating);
            }
        }
        if (ratings.length === 0)
            return null;
        const avgRating = ratings.reduce((sum, r) => sum + r, 0) / ratings.length;
        return avgRating;
    }
    calculateBlockingStatus(segment) {
        const traffic = segment.traffic_conditions?.[0];
        if (!traffic)
            return 5.0;
        const levelMap = {
            FREE_FLOW: 5.0,
            NORMAL: 4.0,
            SLOW: 2.5,
            CONGESTED: 1.0,
            BLOCKED: 0.0,
        };
        return levelMap[traffic.traffic_level] || 5.0;
    }
    async exportToOSMXML(data) {
        logger_1.logger.info('Exporting road network to OSM XML...');
        const outputDir = (0, path_1.join)(this.osrmDataPath, '_shared');
        if (!(0, fs_1.existsSync)(outputDir)) {
            (0, fs_1.mkdirSync)(outputDir, { recursive: true });
        }
        const osmFilePath = (0, path_1.join)(outputDir, 'network.osm.xml');
        const { roads, nodes, segments, userFeedback, trafficValue } = data;
        // Build coordinate -> node mapping
        const coordToNodeId = new Map();
        const nodeCoords = new Map();
        const coordToDbNode = new Map();
        for (const node of nodes) {
            if (node.lat && node.lon) {
                const key = `${node.lat.toFixed(7)},${node.lon.toFixed(7)}`;
                coordToDbNode.set(key, node);
            }
        }
        const ensureNodeForCoord = (lat, lon) => {
            const key = `${lat.toFixed(7)},${lon.toFixed(7)}`;
            if (coordToNodeId.has(key))
                return coordToNodeId.get(key);
            const dbNode = coordToDbNode.get(key);
            let nodeId;
            if (dbNode && dbNode.osm_id && !isNaN(Number(dbNode.osm_id))) {
                nodeId = Number(dbNode.osm_id);
            }
            else {
                nodeId = 10_000_000 + coordToNodeId.size;
            }
            coordToNodeId.set(key, nodeId);
            nodeCoords.set(nodeId, { lat, lon });
            return nodeId;
        };
        // Build ways
        const ways = [];
        let wayIdCounter = 5_000_000;
        for (const segment of segments) {
            const wayId = wayIdCounter++;
            const nodeRefs = [];
            const tags = [];
            let coords = [];
            try {
                const geom = typeof segment.geometry === 'string'
                    ? JSON.parse(segment.geometry)
                    : segment.geometry;
                coords = geom?.coordinates || [];
            }
            catch (e) {
                continue;
            }
            if (coords.length < 2)
                continue;
            for (const [lon, lat] of coords) {
                nodeRefs.push(ensureNodeForCoord(lat, lon));
            }
            if (nodeRefs.length < 2)
                continue;
            const road = roads.find((r) => r.road_id === segment.road_id);
            const roadType = road ? this.mapRoadTypeToOSM(road.road_type) : 'unclassified';
            tags.push({ k: 'highway', v: roadType });
            if (segment.name) {
                tags.push({ k: 'name', v: this.escapeXML(segment.name) });
            }
            const rating = userFeedback.get(segment.segment_id);
            if (rating !== undefined) {
                tags.push({ k: 'user_rating', v: rating.toFixed(3) });
            }
            const traffic = trafficValue.get(segment.segment_id);
            if (traffic !== undefined) {
                tags.push({ k: 'traffic_value', v: traffic.toFixed(1) });
            }
            if (segment.max_speed) {
                tags.push({ k: 'maxspeed', v: String(segment.max_speed) });
            }
            const isOneWay = segment.one_way || (road && road.one_way);
            if (isOneWay) {
                tags.push({ k: 'oneway', v: 'yes' });
                ways.push({ id: wayId, nodeRefs, tags });
            }
            else {
                ways.push({ id: wayId, nodeRefs, tags });
                const reverseWayId = wayIdCounter++;
                ways.push({ id: reverseWayId, nodeRefs: [...nodeRefs].reverse(), tags });
            }
        }
        // Write XML
        let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
        xml += '<osm version="0.6" generator="osrm-management-system">\n';
        xml += '  <bounds minlat="10.3" minlon="106.3" maxlat="11.2" maxlon="107.0"/>\n';
        for (const [nodeId, coords] of nodeCoords.entries()) {
            xml += `  <node id="${nodeId}" lat="${coords.lat}" lon="${coords.lon}"/>\n`;
        }
        for (const way of ways) {
            xml += `  <way id="${way.id}">\n`;
            for (const nodeRef of way.nodeRefs) {
                xml += `    <nd ref="${nodeRef}"/>\n`;
            }
            for (const tag of way.tags) {
                xml += `    <tag k="${tag.k}" v="${tag.v}"/>\n`;
            }
            xml += `  </way>\n`;
        }
        xml += '</osm>\n';
        (0, fs_1.writeFileSync)(osmFilePath, xml, 'utf-8');
        logger_1.logger.info(`Saved OSM XML to ${osmFilePath}`);
        return osmFilePath;
    }
    generateLuaProfile(config) {
        const { useRating, useBlocking } = config;
        return `-- OSRM Bicycle Profile V2 (VN Motorbike Optimized)
-- Generated by OSRM Management System
-- Model: ${config.name}
-- Config: rating=${useRating}, blocking=${useBlocking}

api_version = 4

function setup()
  return {
    properties = {
      max_speed_for_map_matching = 40/3.6,
      use_turn_restrictions = false,
      continue_straight_at_waypoint = true,
      weight_name = 'routability',
    },
    
    default_mode = mode.cycling,
    default_speed = 35,
    oneway_handling = false,
    turn_penalty = 4,
    u_turn_penalty = 5,
    
    use_rating = ${useRating},
    use_blocking = ${useBlocking},
  }
end

function process_way(profile, way, result, relations)
  local highway = way:get_value_by_key("highway")
  if not highway then
    return
  end

  local speed_map = {
    trunk = 60,
    primary = 50,
    secondary = 45,
    tertiary = 40,
    unclassified = 30,
    residential = 30,
    service = 20,
    living_street = 15,
    track = 15,
    path = 12,
    cycleway = 25,
  }

  local speed = speed_map[highway] or 25
  
  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  if maxspeed and maxspeed > 0 then
    speed = math.min(speed, maxspeed * 0.9)
  end

  if profile.use_blocking then
    local traffic_value = tonumber(way:get_value_by_key("traffic_value"))
    if traffic_value then
      local traffic_multiplier = traffic_value / 5.0
      speed = speed * traffic_multiplier
    end
  end

  local oneway = way:get_value_by_key("oneway")
  if oneway == "yes" or oneway == "1" then
    result.forward_mode = mode.cycling
    result.backward_mode = mode.inaccessible
  elseif oneway == "-1" then
    result.forward_mode = mode.inaccessible
    result.backward_mode = mode.cycling
  else
    result.forward_mode = mode.cycling
    result.backward_mode = mode.cycling
  end

  result.forward_speed = speed
  result.backward_speed = speed

  local name = way:get_value_by_key("name")
  if name then
    result.name = name
  end

  result.duration = 1
  result.weight = 1

  if profile.use_rating then
    local user_rating = tonumber(way:get_value_by_key("user_rating"))
    if user_rating then
      local rating_multiplier = 2.0 - user_rating
      result.weight = result.weight * rating_multiplier
    end
  end
end

function process_node(profile, node, result, relations)
  local highway = node:get_value_by_key("highway")
  if highway == "traffic_signals" then
    result.traffic_lights = true
  end
end

function process_turn(profile, turn)
  local angle = math.abs(turn.angle)
  
  if angle >= 160 then
    turn.duration = turn.duration + profile.u_turn_penalty
  elseif angle >= 45 then
    turn.duration = turn.duration + profile.turn_penalty
  end
  
  turn.weight = turn.duration
end

return {
  setup = setup,
  process_way = process_way,
  process_node = process_node,
  process_turn = process_turn,
}
`;
    }
    async setupModelInstance(config, sharedXmlPath) {
        const instanceDir = (0, path_1.join)(this.osrmDataPath, config.name);
        if (!(0, fs_1.existsSync)(instanceDir)) {
            (0, fs_1.mkdirSync)(instanceDir, { recursive: true });
        }
        const instanceXmlPath = (0, path_1.join)(instanceDir, 'network.osm.xml');
        (0, fs_1.copyFileSync)(sharedXmlPath, instanceXmlPath);
        const luaScript = this.generateLuaProfile(config);
        const luaPath = (0, path_1.join)(instanceDir, 'custom_bicycle.lua');
        (0, fs_1.writeFileSync)(luaPath, luaScript, 'utf-8');
        logger_1.logger.info(`Setup ${config.name}`);
    }
    async processOSRMInstance(config) {
        logger_1.logger.info(`Processing ${config.name}...`);
        // Use absolute path for Docker volume mount
        const dataDir = (0, path_1.resolve)(this.osrmDataPath, config.name);
        const osmFile = 'network.osm.xml';
        const profileFile = 'custom_bicycle.lua';
        const osrmFile = 'network.osrm';
        try {
            const dockerImage = process.env.OSRM_DOCKER_IMAGE || 'osrm/osrm-backend:latest';
            // Extract
            await execAsync(`docker run --rm -v "${dataDir}:/data" ${dockerImage} osrm-extract -p /data/${profileFile} /data/${osmFile}`, { maxBuffer: 100 * 1024 * 1024 });
            // Partition
            await execAsync(`docker run --rm -v "${dataDir}:/data" ${dockerImage} osrm-partition /data/${osrmFile}`, { maxBuffer: 100 * 1024 * 1024 });
            // Customize
            await execAsync(`docker run --rm -v "${dataDir}:/data" ${dockerImage} osrm-customize /data/${osrmFile}`, { maxBuffer: 100 * 1024 * 1024 });
            logger_1.logger.info(`${config.name} completed`);
        }
        catch (error) {
            logger_1.logger.error(`${config.name} failed: ${error.message}`);
            throw error;
        }
    }
    mapRoadTypeToOSM(roadType) {
        const mapping = {
            MOTORWAY: 'motorway',
            TRUNK: 'trunk',
            PRIMARY: 'primary',
            SECONDARY: 'secondary',
            TERTIARY: 'tertiary',
            RESIDENTIAL: 'residential',
            SERVICE: 'service',
            UNCLASSIFIED: 'unclassified',
        };
        return mapping[roadType] || 'unclassified';
    }
    escapeXML(text) {
        if (!text)
            return '';
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&apos;');
    }
}
exports.GenerateService = GenerateService;
//# sourceMappingURL=generate.service.js.map