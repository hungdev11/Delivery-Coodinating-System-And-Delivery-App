-- AlterTable
ALTER TABLE "addresses" ADD COLUMN     "distance_to_segment" DOUBLE PRECISION,
ADD COLUMN     "geohash" TEXT,
ADD COLUMN     "projected_lat" DOUBLE PRECISION,
ADD COLUMN     "projected_lon" DOUBLE PRECISION,
ADD COLUMN     "segment_position" DOUBLE PRECISION;

-- CreateIndex
CREATE INDEX "addresses_geohash_idx" ON "addresses"("geohash");

-- CreateIndex
CREATE INDEX "addresses_lat_lon_idx" ON "addresses"("lat", "lon");
