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
ALTER TABLE `destination` ADD CONSTRAINT `destination_geohash_cell_id_fkey` FOREIGN KEY (`geohash_cell_id`) REFERENCES `zone_geohash_cells`(`geohash_cell_id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `working_places` ADD CONSTRAINT `working_places_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `zone_geohash_cells` ADD CONSTRAINT `zone_geohash_cells_zone_id_fkey` FOREIGN KEY (`zone_id`) REFERENCES `zones`(`zone_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `zones` ADD CONSTRAINT `zones_center_id_fkey` FOREIGN KEY (`center_id`) REFERENCES `centers`(`center_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
