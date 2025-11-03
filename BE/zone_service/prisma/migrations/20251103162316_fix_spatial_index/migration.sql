-- CreateTable
CREATE TABLE `addresses` (
    `address_id` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `name_en` VARCHAR(191) NULL,
    `address_text` VARCHAR(191) NULL,
    `lat` DOUBLE NOT NULL,
    `lon` DOUBLE NOT NULL,
    `geohash` VARCHAR(191) NULL,
    `segment_id` VARCHAR(191) NULL,
    `segment_position` DOUBLE NULL,
    `distance_to_segment` DOUBLE NULL,
    `projected_lat` DOUBLE NULL,
    `projected_lon` DOUBLE NULL,
    `zone_id` VARCHAR(191) NULL,
    `ward_name` VARCHAR(191) NULL,
    `district_name` VARCHAR(191) NULL,
    `address_type` ENUM('GENERAL', 'SCHOOL', 'HOSPITAL', 'GOVERNMENT', 'SHOPPING', 'RESTAURANT', 'HOTEL', 'BANK', 'GAS_STATION', 'PARKING', 'BUS_STOP', 'LANDMARK') NOT NULL DEFAULT 'GENERAL',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,

    INDEX `addresses_segment_id_idx`(`segment_id`),
    INDEX `addresses_zone_id_idx`(`zone_id`),
    INDEX `addresses_name_idx`(`name`),
    INDEX `addresses_geohash_idx`(`geohash`),
    INDEX `addresses_lat_lon_idx`(`lat`, `lon`),
    PRIMARY KEY (`address_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `centers` (
    `center_id` VARCHAR(191) NOT NULL,
    `code` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `address` VARCHAR(191) NULL,
    `lat` DOUBLE NULL,
    `lon` DOUBLE NULL,
    `polygon` JSON NULL,

    UNIQUE INDEX `centers_code_key`(`code`),
    PRIMARY KEY (`center_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `destination` (
    `destination_id` VARCHAR(191) NOT NULL,
    `lat` DOUBLE NOT NULL,
    `lon` DOUBLE NOT NULL,
    `address_text` VARCHAR(191) NULL,
    `geohash_cell_id` VARCHAR(191) NULL,

    PRIMARY KEY (`destination_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `osrm_builds` (
    `build_id` VARCHAR(191) NOT NULL,
    `instance_name` VARCHAR(191) NOT NULL,
    `build_number` INTEGER NOT NULL AUTO_INCREMENT,
    `status` ENUM('PENDING', 'BUILDING', 'TESTING', 'READY', 'DEPLOYED', 'FAILED', 'DEPRECATED') NOT NULL DEFAULT 'PENDING',
    `data_snapshot_time` DATETIME(3) NOT NULL,
    `total_segments` INTEGER NOT NULL,
    `avg_weight` DOUBLE NULL,
    `started_at` DATETIME(3) NULL,
    `completed_at` DATETIME(3) NULL,
    `deployed_at` DATETIME(3) NULL,
    `error_message` VARCHAR(191) NULL,
    `pbf_file_path` VARCHAR(191) NULL,
    `osrm_output_path` VARCHAR(191) NULL,
    `lua_script_version` VARCHAR(191) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,

    UNIQUE INDEX `osrm_builds_build_number_key`(`build_number`),
    INDEX `osrm_builds_instance_name_idx`(`instance_name`),
    INDEX `osrm_builds_status_idx`(`status`),
    INDEX `osrm_builds_created_at_idx`(`created_at`),
    PRIMARY KEY (`build_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `poi_priorities` (
    `priority_id` VARCHAR(191) NOT NULL,
    `poi_id` VARCHAR(191) NOT NULL,
    `poi_name` VARCHAR(191) NULL,
    `poi_type` VARCHAR(191) NULL,
    `priority` INTEGER NOT NULL DEFAULT 3,
    `time_windows` JSON NULL,
    `latitude` DECIMAL(10, 7) NULL,
    `longitude` DECIMAL(10, 7) NULL,
    `updated_by` VARCHAR(191) NULL,
    `updated_at` DATETIME(3) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    UNIQUE INDEX `poi_priorities_poi_id_key`(`poi_id`),
    INDEX `poi_priorities_priority_idx`(`priority`),
    INDEX `poi_priorities_poi_type_idx`(`poi_type`),
    INDEX `poi_priorities_latitude_longitude_idx`(`latitude`, `longitude`),
    PRIMARY KEY (`priority_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `road_nodes` (
    `node_id` VARCHAR(191) NOT NULL,
    `osm_id` VARCHAR(191) NULL,
    `lat` DOUBLE NOT NULL,
    `lon` DOUBLE NOT NULL,
    `node_type` ENUM('INTERSECTION', 'TRAFFIC_LIGHT', 'STOP_SIGN', 'ROUNDABOUT', 'ENDPOINT', 'WAYPOINT') NOT NULL DEFAULT 'INTERSECTION',
    `zone_id` VARCHAR(191) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,

    UNIQUE INDEX `road_nodes_osm_id_key`(`osm_id`),
    INDEX `road_nodes_lat_lon_idx`(`lat`, `lon`),
    INDEX `road_nodes_zone_id_idx`(`zone_id`),
    PRIMARY KEY (`node_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `road_overrides` (
    `override_id` VARCHAR(191) NOT NULL,
    `segment_id` VARCHAR(191) NULL,
    `osm_way_id` BIGINT NULL,
    `block_level` ENUM('none', 'soft', 'min', 'hard') NOT NULL DEFAULT 'none',
    `delta` FLOAT NULL,
    `point_score` FLOAT NULL,
    `recommend_enabled` BOOLEAN NOT NULL DEFAULT true,
    `soft_penalty_factor` FLOAT NULL,
    `min_penalty_factor` FLOAT NULL,
    `updated_by` VARCHAR(191) NULL,
    `updated_at` DATETIME(3) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `road_overrides_segment_id_idx`(`segment_id`),
    INDEX `road_overrides_osm_way_id_idx`(`osm_way_id`),
    INDEX `road_overrides_block_level_idx`(`block_level`),
    INDEX `road_overrides_updated_at_idx`(`updated_at`),
    PRIMARY KEY (`override_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `road_segments` (
    `segment_id` VARCHAR(191) NOT NULL,
    `osm_way_id` BIGINT NULL,
    `from_node_id` VARCHAR(191) NOT NULL,
    `to_node_id` VARCHAR(191) NOT NULL,
    `road_id` VARCHAR(191) NOT NULL,
    `geometry` JSON NOT NULL,
    `geom` geometry NULL,
    `length_meters` DOUBLE NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `road_type` ENUM('MOTORWAY', 'TRUNK', 'PRIMARY', 'SECONDARY', 'TERTIARY', 'RESIDENTIAL', 'SERVICE', 'UNCLASSIFIED', 'LIVING_STREET', 'PEDESTRIAN', 'TRACK', 'PATH') NOT NULL,
    `max_speed` DOUBLE NULL,
    `avg_speed` DOUBLE NULL,
    `one_way` BOOLEAN NOT NULL DEFAULT false,
    `base_weight` DOUBLE NOT NULL,
    `current_weight` DOUBLE NOT NULL,
    `delta_weight` DOUBLE NOT NULL DEFAULT 0,
    `zone_id` VARCHAR(191) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,
    `weight_updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `road_segments_from_node_id_idx`(`from_node_id`),
    INDEX `road_segments_to_node_id_idx`(`to_node_id`),
    INDEX `road_segments_road_id_idx`(`road_id`),
    INDEX `road_segments_zone_id_idx`(`zone_id`),
    INDEX `road_segments_current_weight_idx`(`current_weight`),
    INDEX `road_segments_osm_way_id_idx`(`osm_way_id`),
    PRIMARY KEY (`segment_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `roads` (
    `road_id` VARCHAR(191) NOT NULL,
    `osm_id` VARCHAR(191) NULL,
    `name` VARCHAR(191) NOT NULL,
    `name_en` VARCHAR(191) NULL,
    `road_type` ENUM('MOTORWAY', 'TRUNK', 'PRIMARY', 'SECONDARY', 'TERTIARY', 'RESIDENTIAL', 'SERVICE', 'UNCLASSIFIED', 'LIVING_STREET', 'PEDESTRIAN', 'TRACK', 'PATH') NOT NULL,
    `max_speed` DOUBLE NULL,
    `avg_speed` DOUBLE NULL,
    `one_way` BOOLEAN NOT NULL DEFAULT false,
    `lanes` INTEGER NULL,
    `surface` VARCHAR(191) NULL,
    `geometry` JSON NULL,
    `zone_id` VARCHAR(191) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,

    UNIQUE INDEX `roads_osm_id_key`(`osm_id`),
    INDEX `roads_name_idx`(`name`),
    INDEX `roads_road_type_idx`(`road_type`),
    INDEX `roads_zone_id_idx`(`zone_id`),
    PRIMARY KEY (`road_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `traffic_conditions` (
    `traffic_condition_id` VARCHAR(191) NOT NULL,
    `segment_id` VARCHAR(191) NOT NULL,
    `traffic_level` ENUM('FREE_FLOW', 'NORMAL', 'SLOW', 'CONGESTED', 'BLOCKED') NOT NULL DEFAULT 'NORMAL',
    `congestion_score` DOUBLE NOT NULL DEFAULT 0,
    `current_speed` DOUBLE NULL,
    `speed_ratio` DOUBLE NULL,
    `weight_multiplier` DOUBLE NOT NULL DEFAULT 1,
    `source` VARCHAR(191) NOT NULL DEFAULT 'tracking-asia',
    `source_timestamp` DATETIME(3) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `expires_at` DATETIME(3) NOT NULL,

    INDEX `traffic_conditions_segment_id_idx`(`segment_id`),
    INDEX `traffic_conditions_source_timestamp_idx`(`source_timestamp`),
    INDEX `traffic_conditions_expires_at_idx`(`expires_at`),
    UNIQUE INDEX `traffic_conditions_segment_id_source_key`(`segment_id`, `source`),
    PRIMARY KEY (`traffic_condition_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `user_feedback` (
    `feedback_id` VARCHAR(191) NOT NULL,
    `segment_id` VARCHAR(191) NOT NULL,
    `user_id` VARCHAR(191) NOT NULL,
    `feedback_type` ENUM('ROAD_CLOSED', 'CONSTRUCTION', 'ACCIDENT', 'POOR_CONDITION', 'TRAFFIC_ALWAYS_BAD', 'BETTER_ROUTE', 'INCORRECT_INFO', 'OTHER') NOT NULL,
    `severity` ENUM('MINOR', 'MODERATE', 'MAJOR', 'CRITICAL') NOT NULL DEFAULT 'MINOR',
    `description` VARCHAR(191) NULL,
    `suggested_weight_adj` DOUBLE NULL,
    `status` ENUM('PENDING', 'REVIEWING', 'APPROVED', 'REJECTED', 'RESOLVED') NOT NULL DEFAULT 'PENDING',
    `reviewed_by` VARCHAR(191) NULL,
    `reviewed_at` DATETIME(3) NULL,
    `applied` BOOLEAN NOT NULL DEFAULT false,
    `applied_at` DATETIME(3) NULL,
    `weight_adjustment` DOUBLE NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL,

    INDEX `user_feedback_segment_id_idx`(`segment_id`),
    INDEX `user_feedback_user_id_idx`(`user_id`),
    INDEX `user_feedback_status_idx`(`status`),
    INDEX `user_feedback_created_at_idx`(`created_at`),
    PRIMARY KEY (`feedback_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `weight_history` (
    `history_id` VARCHAR(191) NOT NULL,
    `segment_id` VARCHAR(191) NOT NULL,
    `base_weight` DOUBLE NOT NULL,
    `delta_weight` DOUBLE NOT NULL,
    `current_weight` DOUBLE NOT NULL,
    `traffic_multiplier` DOUBLE NOT NULL DEFAULT 1,
    `user_feedback_adj` DOUBLE NOT NULL DEFAULT 0,
    `other_adjustments` JSON NULL,
    `calculated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `calculation_trigger` VARCHAR(191) NULL,

    INDEX `weight_history_segment_id_idx`(`segment_id`),
    INDEX `weight_history_calculated_at_idx`(`calculated_at`),
    PRIMARY KEY (`history_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `working_places` (
    `working_places_id` VARCHAR(191) NOT NULL,
    `delivery_man_id` VARCHAR(191) NOT NULL,
    `start_at` DATETIME(3) NOT NULL,
    `end_at` DATETIME(3) NULL,
    `zone_id` VARCHAR(191) NOT NULL,

    PRIMARY KEY (`working_places_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `zone_geohash_cells` (
    `geohash_cell_id` VARCHAR(191) NOT NULL,
    `geohash` VARCHAR(191) NOT NULL,
    `level` INTEGER NOT NULL,
    `parent` VARCHAR(191) NULL,
    `polygon` JSON NULL,
    `zone_id` VARCHAR(191) NOT NULL,

    PRIMARY KEY (`geohash_cell_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `zones` (
    `zone_id` VARCHAR(191) NOT NULL,
    `code` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `polygon` JSON NULL,
    `center_id` VARCHAR(191) NOT NULL,

    UNIQUE INDEX `zones_code_key`(`code`),
    PRIMARY KEY (`zone_id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `addresses` ADD CONSTRAINT `addresses_segment_id_fkey` FOREIGN KEY (`segment_id`) REFERENCES `road_segments`(`segment_id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `addresses` ADD CONSTRAINT `addresses_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `destination` ADD CONSTRAINT `destination_geohash_cell_id_fkey` FOREIGN KEY (`geohash_cell_id`) REFERENCES `zone_geohash_cells`(`geohash_cell_id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `road_nodes` ADD CONSTRAINT `road_nodes_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `road_overrides` ADD CONSTRAINT `road_overrides_segment_id_fkey` FOREIGN KEY (`segment_id`) REFERENCES `road_segments`(`segment_id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `road_segments` ADD CONSTRAINT `road_segments_from_node_id_fkey` FOREIGN KEY (`from_node_id`) REFERENCES `road_nodes`(`node_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `road_segments` ADD CONSTRAINT `road_segments_to_node_id_fkey` FOREIGN KEY (`to_node_id`) REFERENCES `road_nodes`(`node_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `road_segments` ADD CONSTRAINT `road_segments_road_id_fkey` FOREIGN KEY (`road_id`) REFERENCES `roads`(`road_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `road_segments` ADD CONSTRAINT `road_segments_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `roads` ADD CONSTRAINT `roads_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `traffic_conditions` ADD CONSTRAINT `traffic_conditions_segment_id_fkey` FOREIGN KEY (`segment_id`) REFERENCES `road_segments`(`segment_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `user_feedback` ADD CONSTRAINT `user_feedback_segment_id_fkey` FOREIGN KEY (`segment_id`) REFERENCES `road_segments`(`segment_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `working_places` ADD CONSTRAINT `working_places_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `zone_geohash_cells` ADD CONSTRAINT `zone_geohash_cells_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `zones` ADD CONSTRAINT `zones_center_id_fkey` FOREIGN KEY (`center_id`) REFERENCES `centers`(`center_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
