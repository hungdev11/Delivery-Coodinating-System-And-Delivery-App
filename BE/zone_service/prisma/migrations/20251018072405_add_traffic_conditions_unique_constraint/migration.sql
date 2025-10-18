/*
  Warnings:

  - A unique constraint covering the columns `[segment_id,source]` on the table `traffic_conditions` will be added. If there are existing duplicate values, this will fail.

*/
-- CreateIndex
CREATE UNIQUE INDEX "traffic_conditions_segment_id_source_key" ON "traffic_conditions"("segment_id", "source");
