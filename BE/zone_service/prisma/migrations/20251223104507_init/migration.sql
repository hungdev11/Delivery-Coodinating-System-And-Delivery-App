/*
  Warnings:

  - A unique constraint covering the columns `[lat,lon,name]` on the table `addresses` will be added. If there are existing duplicate values, this will fail.

*/
-- CreateIndex
CREATE UNIQUE INDEX `addresses_lat_lon_name_key` ON `addresses`(`lat`, `lon`, `name`);
