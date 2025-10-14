-- CreateEnum
CREATE TYPE "AddressType" AS ENUM ('GENERAL', 'SCHOOL', 'HOSPITAL', 'GOVERNMENT', 'SHOPPING', 'RESTAURANT', 'HOTEL', 'BANK', 'GAS_STATION', 'PARKING', 'BUS_STOP', 'LANDMARK');

-- CreateEnum
CREATE TYPE "OsrmBuildStatus" AS ENUM ('PENDING', 'BUILDING', 'TESTING', 'READY', 'DEPLOYED', 'FAILED', 'DEPRECATED');

-- CreateEnum
CREATE TYPE "NodeType" AS ENUM ('INTERSECTION', 'TRAFFIC_LIGHT', 'STOP_SIGN', 'ROUNDABOUT', 'ENDPOINT', 'WAYPOINT');

-- CreateEnum
CREATE TYPE "RoadType" AS ENUM ('MOTORWAY', 'TRUNK', 'PRIMARY', 'SECONDARY', 'TERTIARY', 'RESIDENTIAL', 'SERVICE', 'UNCLASSIFIED', 'LIVING_STREET', 'PEDESTRIAN', 'TRACK', 'PATH');

-- CreateEnum
CREATE TYPE "TrafficLevel" AS ENUM ('FREE_FLOW', 'NORMAL', 'SLOW', 'CONGESTED', 'BLOCKED');

-- CreateEnum
CREATE TYPE "FeedbackType" AS ENUM ('ROAD_CLOSED', 'CONSTRUCTION', 'ACCIDENT', 'POOR_CONDITION', 'TRAFFIC_ALWAYS_BAD', 'BETTER_ROUTE', 'INCORRECT_INFO', 'OTHER');

-- CreateEnum
CREATE TYPE "FeedbackSeverity" AS ENUM ('MINOR', 'MODERATE', 'MAJOR', 'CRITICAL');

-- CreateEnum
CREATE TYPE "FeedbackStatus" AS ENUM ('PENDING', 'REVIEWING', 'APPROVED', 'REJECTED', 'RESOLVED');

-- CreateTable
CREATE TABLE "addresses" (
    "address_id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "name_en" TEXT,
    "address_text" TEXT,
    "lat" DOUBLE PRECISION NOT NULL,
    "lon" DOUBLE PRECISION NOT NULL,
    "segment_id" TEXT,
    "zone_id" TEXT,
    "ward_name" TEXT,
    "district_name" TEXT,
    "address_type" "AddressType" NOT NULL DEFAULT 'GENERAL',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "addresses_pkey" PRIMARY KEY ("address_id")
);

-- CreateTable
CREATE TABLE "centers" (
    "center_id" TEXT NOT NULL,
    "code" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "address" TEXT,
    "lat" DOUBLE PRECISION,
    "lon" DOUBLE PRECISION,
    "polygon" JSONB,

    CONSTRAINT "centers_pkey" PRIMARY KEY ("center_id")
);

-- CreateTable
CREATE TABLE "destination" (
    "destination_id" TEXT NOT NULL,
    "lat" DOUBLE PRECISION NOT NULL,
    "lon" DOUBLE PRECISION NOT NULL,
    "address_text" TEXT,
    "geohash_cell_id" TEXT,

    CONSTRAINT "destination_pkey" PRIMARY KEY ("destination_id")
);

-- CreateTable
CREATE TABLE "osrm_builds" (
    "build_id" TEXT NOT NULL,
    "instance_name" TEXT NOT NULL,
    "build_number" SERIAL NOT NULL,
    "status" "OsrmBuildStatus" NOT NULL DEFAULT 'PENDING',
    "data_snapshot_time" TIMESTAMP(3) NOT NULL,
    "total_segments" INTEGER NOT NULL,
    "avg_weight" DOUBLE PRECISION,
    "started_at" TIMESTAMP(3),
    "completed_at" TIMESTAMP(3),
    "deployed_at" TIMESTAMP(3),
    "error_message" TEXT,
    "pbf_file_path" TEXT,
    "osrm_output_path" TEXT,
    "lua_script_version" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "osrm_builds_pkey" PRIMARY KEY ("build_id")
);

-- CreateTable
CREATE TABLE "road_nodes" (
    "node_id" TEXT NOT NULL,
    "osm_id" TEXT,
    "lat" DOUBLE PRECISION NOT NULL,
    "lon" DOUBLE PRECISION NOT NULL,
    "node_type" "NodeType" NOT NULL DEFAULT 'INTERSECTION',
    "zone_id" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "road_nodes_pkey" PRIMARY KEY ("node_id")
);

-- CreateTable
CREATE TABLE "road_segments" (
    "segment_id" TEXT NOT NULL,
    "from_node_id" TEXT NOT NULL,
    "to_node_id" TEXT NOT NULL,
    "road_id" TEXT NOT NULL,
    "geometry" JSONB NOT NULL,
    "length_meters" DOUBLE PRECISION NOT NULL,
    "name" TEXT NOT NULL,
    "road_type" "RoadType" NOT NULL,
    "max_speed" DOUBLE PRECISION,
    "avg_speed" DOUBLE PRECISION,
    "one_way" BOOLEAN NOT NULL DEFAULT false,
    "base_weight" DOUBLE PRECISION NOT NULL,
    "current_weight" DOUBLE PRECISION NOT NULL,
    "delta_weight" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "zone_id" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,
    "weight_updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "road_segments_pkey" PRIMARY KEY ("segment_id")
);

-- CreateTable
CREATE TABLE "roads" (
    "road_id" TEXT NOT NULL,
    "osm_id" TEXT,
    "name" TEXT NOT NULL,
    "name_en" TEXT,
    "road_type" "RoadType" NOT NULL,
    "max_speed" DOUBLE PRECISION,
    "avg_speed" DOUBLE PRECISION,
    "one_way" BOOLEAN NOT NULL DEFAULT false,
    "lanes" INTEGER,
    "surface" TEXT,
    "geometry" JSONB,
    "zone_id" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "roads_pkey" PRIMARY KEY ("road_id")
);

-- CreateTable
CREATE TABLE "traffic_conditions" (
    "traffic_condition_id" TEXT NOT NULL,
    "segment_id" TEXT NOT NULL,
    "traffic_level" "TrafficLevel" NOT NULL DEFAULT 'NORMAL',
    "congestion_score" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "current_speed" DOUBLE PRECISION,
    "speed_ratio" DOUBLE PRECISION,
    "weight_multiplier" DOUBLE PRECISION NOT NULL DEFAULT 1,
    "source" TEXT NOT NULL DEFAULT 'tracking-asia',
    "source_timestamp" TIMESTAMP(3) NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "expires_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "traffic_conditions_pkey" PRIMARY KEY ("traffic_condition_id")
);

-- CreateTable
CREATE TABLE "user_feedback" (
    "feedback_id" TEXT NOT NULL,
    "segment_id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "feedback_type" "FeedbackType" NOT NULL,
    "severity" "FeedbackSeverity" NOT NULL DEFAULT 'MINOR',
    "description" TEXT,
    "suggested_weight_adj" DOUBLE PRECISION,
    "status" "FeedbackStatus" NOT NULL DEFAULT 'PENDING',
    "reviewed_by" TEXT,
    "reviewed_at" TIMESTAMP(3),
    "applied" BOOLEAN NOT NULL DEFAULT false,
    "applied_at" TIMESTAMP(3),
    "weight_adjustment" DOUBLE PRECISION,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "user_feedback_pkey" PRIMARY KEY ("feedback_id")
);

-- CreateTable
CREATE TABLE "weight_history" (
    "history_id" TEXT NOT NULL,
    "segment_id" TEXT NOT NULL,
    "base_weight" DOUBLE PRECISION NOT NULL,
    "delta_weight" DOUBLE PRECISION NOT NULL,
    "current_weight" DOUBLE PRECISION NOT NULL,
    "traffic_multiplier" DOUBLE PRECISION NOT NULL DEFAULT 1,
    "user_feedback_adj" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "other_adjustments" JSONB,
    "calculated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "calculation_trigger" TEXT,

    CONSTRAINT "weight_history_pkey" PRIMARY KEY ("history_id")
);

-- CreateTable
CREATE TABLE "working_places" (
    "working_places_id" TEXT NOT NULL,
    "delivery_man_id" TEXT NOT NULL,
    "start_at" TIMESTAMP(3) NOT NULL,
    "end_at" TIMESTAMP(3),
    "zone_id" TEXT NOT NULL,

    CONSTRAINT "working_places_pkey" PRIMARY KEY ("working_places_id")
);

-- CreateTable
CREATE TABLE "zone_geohash_cells" (
    "geohash_cell_id" TEXT NOT NULL,
    "geohash" TEXT NOT NULL,
    "level" INTEGER NOT NULL,
    "parent" TEXT,
    "polygon" JSONB,
    "zone_id" TEXT NOT NULL,

    CONSTRAINT "zone_geohash_cells_pkey" PRIMARY KEY ("geohash_cell_id")
);

-- CreateTable
CREATE TABLE "zones" (
    "zone_id" TEXT NOT NULL,
    "code" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "polygon" JSONB,
    "center_id" TEXT NOT NULL,

    CONSTRAINT "zones_pkey" PRIMARY KEY ("zone_id")
);

-- CreateIndex
CREATE INDEX "addresses_segment_id_idx" ON "addresses"("segment_id");

-- CreateIndex
CREATE INDEX "addresses_zone_id_idx" ON "addresses"("zone_id");

-- CreateIndex
CREATE INDEX "addresses_name_idx" ON "addresses"("name");

-- CreateIndex
CREATE UNIQUE INDEX "centers_code_key" ON "centers"("code");

-- CreateIndex
CREATE INDEX "osrm_builds_instance_name_idx" ON "osrm_builds"("instance_name");

-- CreateIndex
CREATE INDEX "osrm_builds_status_idx" ON "osrm_builds"("status");

-- CreateIndex
CREATE INDEX "osrm_builds_created_at_idx" ON "osrm_builds"("created_at");

-- CreateIndex
CREATE UNIQUE INDEX "road_nodes_osm_id_key" ON "road_nodes"("osm_id");

-- CreateIndex
CREATE INDEX "road_nodes_lat_lon_idx" ON "road_nodes"("lat", "lon");

-- CreateIndex
CREATE INDEX "road_nodes_zone_id_idx" ON "road_nodes"("zone_id");

-- CreateIndex
CREATE INDEX "road_segments_from_node_id_idx" ON "road_segments"("from_node_id");

-- CreateIndex
CREATE INDEX "road_segments_to_node_id_idx" ON "road_segments"("to_node_id");

-- CreateIndex
CREATE INDEX "road_segments_road_id_idx" ON "road_segments"("road_id");

-- CreateIndex
CREATE INDEX "road_segments_zone_id_idx" ON "road_segments"("zone_id");

-- CreateIndex
CREATE INDEX "road_segments_current_weight_idx" ON "road_segments"("current_weight");

-- CreateIndex
CREATE UNIQUE INDEX "roads_osm_id_key" ON "roads"("osm_id");

-- CreateIndex
CREATE INDEX "roads_name_idx" ON "roads"("name");

-- CreateIndex
CREATE INDEX "roads_road_type_idx" ON "roads"("road_type");

-- CreateIndex
CREATE INDEX "roads_zone_id_idx" ON "roads"("zone_id");

-- CreateIndex
CREATE INDEX "traffic_conditions_segment_id_idx" ON "traffic_conditions"("segment_id");

-- CreateIndex
CREATE INDEX "traffic_conditions_source_timestamp_idx" ON "traffic_conditions"("source_timestamp");

-- CreateIndex
CREATE INDEX "traffic_conditions_expires_at_idx" ON "traffic_conditions"("expires_at");

-- CreateIndex
CREATE INDEX "user_feedback_segment_id_idx" ON "user_feedback"("segment_id");

-- CreateIndex
CREATE INDEX "user_feedback_user_id_idx" ON "user_feedback"("user_id");

-- CreateIndex
CREATE INDEX "user_feedback_status_idx" ON "user_feedback"("status");

-- CreateIndex
CREATE INDEX "user_feedback_created_at_idx" ON "user_feedback"("created_at");

-- CreateIndex
CREATE INDEX "weight_history_segment_id_idx" ON "weight_history"("segment_id");

-- CreateIndex
CREATE INDEX "weight_history_calculated_at_idx" ON "weight_history"("calculated_at");

-- CreateIndex
CREATE UNIQUE INDEX "zones_code_key" ON "zones"("code");

-- AddForeignKey
ALTER TABLE "addresses" ADD CONSTRAINT "addresses_segment_id_fkey" FOREIGN KEY ("segment_id") REFERENCES "road_segments"("segment_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "addresses" ADD CONSTRAINT "addresses_zone_id_fkey" FOREIGN KEY ("zone_id") REFERENCES "zones"("zone_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "destination" ADD CONSTRAINT "destination_geohash_cell_id_fkey" FOREIGN KEY ("geohash_cell_id") REFERENCES "zone_geohash_cells"("geohash_cell_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "road_nodes" ADD CONSTRAINT "road_nodes_zone_id_fkey" FOREIGN KEY ("zone_id") REFERENCES "zones"("zone_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "road_segments" ADD CONSTRAINT "road_segments_from_node_id_fkey" FOREIGN KEY ("from_node_id") REFERENCES "road_nodes"("node_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "road_segments" ADD CONSTRAINT "road_segments_to_node_id_fkey" FOREIGN KEY ("to_node_id") REFERENCES "road_nodes"("node_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "road_segments" ADD CONSTRAINT "road_segments_road_id_fkey" FOREIGN KEY ("road_id") REFERENCES "roads"("road_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "road_segments" ADD CONSTRAINT "road_segments_zone_id_fkey" FOREIGN KEY ("zone_id") REFERENCES "zones"("zone_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "roads" ADD CONSTRAINT "roads_zone_id_fkey" FOREIGN KEY ("zone_id") REFERENCES "zones"("zone_id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "traffic_conditions" ADD CONSTRAINT "traffic_conditions_segment_id_fkey" FOREIGN KEY ("segment_id") REFERENCES "road_segments"("segment_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "user_feedback" ADD CONSTRAINT "user_feedback_segment_id_fkey" FOREIGN KEY ("segment_id") REFERENCES "road_segments"("segment_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "working_places" ADD CONSTRAINT "working_places_zone_id_fkey" FOREIGN KEY ("zone_id") REFERENCES "zones"("zone_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "zone_geohash_cells" ADD CONSTRAINT "zone_geohash_cells_zone_id_fkey" FOREIGN KEY ("zone_id") REFERENCES "zones"("zone_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "zones" ADD CONSTRAINT "zones_center_id_fkey" FOREIGN KEY ("center_id") REFERENCES "centers"("center_id") ON DELETE RESTRICT ON UPDATE CASCADE;
