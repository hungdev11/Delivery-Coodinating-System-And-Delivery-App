# Overlay XML Schema Documentation

## Overview

While the current implementation stores overrides in MySQL database, this document defines the XML schema for future external overlay files or import/export operations.

## Schema Definition

### Root Element: `<osrm_overlays>`

Container for all override data.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<osrm_overlays version="1.0" generated="2025-01-03T10:00:00Z">
  <road_overrides>
    <!-- Road override elements -->
  </road_overrides>
  <poi_priorities>
    <!-- POI priority elements -->
  </poi_priorities>
</osrm_overlays>
```

---

## Road Overrides

### Element: `<road_override>`

Defines dynamic routing adjustments for a road segment or OSM way.

#### Attributes

- `id` (required): Unique identifier for this override
- `segment_id` (optional): Internal segment ID
- `osm_way_id` (optional): OSM way ID (at least one of segment_id or osm_way_id required)
- `updated_by` (optional): Username or system that created this override
- `updated_at` (optional): Timestamp of last update

#### Child Elements

- `<block_level>`: Blocking level (none|soft|min|hard)
- `<delta>`: Additive weight adjustment (float)
- `<point_score>`: Road quality score 0-1 (float)
- `<recommend_enabled>`: Enable/disable recommendations (true|false)
- `<soft_penalty_factor>`: Custom soft block penalty (float)
- `<min_penalty_factor>`: Custom min block penalty (float)
- `<description>`: Human-readable description/reason for override

#### Example

```xml
<road_override id="override-001" segment_id="abc-123" updated_by="admin" updated_at="2025-01-03T10:00:00Z">
  <block_level>soft</block_level>
  <delta>0.5</delta>
  <point_score>0.3</point_score>
  <recommend_enabled>true</recommend_enabled>
  <soft_penalty_factor>2.5</soft_penalty_factor>
  <description>Road under construction, reduce traffic</description>
</road_override>
```

#### Hard Block Example

```xml
<road_override id="override-002" osm_way_id="123456789" updated_by="traffic_system">
  <block_level>hard</block_level>
  <description>Road closed due to accident</description>
</road_override>
```

#### Recommendation Disabled Example

```xml
<road_override id="override-003" segment_id="def-456">
  <block_level>none</block_level>
  <recommend_enabled>false</recommend_enabled>
  <description>Ignore AI recommendations for this road</description>
</road_override>
```

---

## POI Priorities

### Element: `<poi_priority>`

Defines priority level for a point of interest used in waypoint sequencing.

#### Attributes

- `id` (required): Unique identifier for this priority
- `poi_id` (required): External POI identifier
- `poi_name` (optional): Human-readable name
- `poi_type` (optional): Category/type of POI
- `updated_by` (optional): Username or system
- `updated_at` (optional): Timestamp

#### Child Elements

- `<priority>`: Priority level 1-5 (1=highest, 5=lowest)
- `<latitude>`: Latitude coordinate (decimal)
- `<longitude>`: Longitude coordinate (decimal)
- `<time_windows>`: Optional time windows when priority applies
- `<description>`: Notes or reason for priority

#### Example

```xml
<poi_priority id="poi-001" poi_id="hospital-central" poi_name="Central Hospital" poi_type="hospital">
  <priority>1</priority>
  <latitude>10.8231</latitude>
  <longitude>106.6297</longitude>
  <time_windows>
    <window>
      <start>08:00</start>
      <end>18:00</end>
      <days>1,2,3,4,5</days> <!-- Monday-Friday -->
    </window>
  </time_windows>
  <description>Critical medical facility, always highest priority</description>
</poi_priority>
```

#### Low Priority Example

```xml
<poi_priority id="poi-002" poi_id="shop-123" poi_name="Convenience Store A" poi_type="retail">
  <priority>5</priority>
  <latitude>10.8245</latitude>
  <longitude>106.6308</longitude>
  <description>Optional stop, skip if route is too long</description>
</poi_priority>
```

---

## Complete Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<osrm_overlays version="1.0" generated="2025-01-03T10:00:00Z">
  <road_overrides>
    <road_override id="override-001" segment_id="abc-123" updated_by="admin" updated_at="2025-01-03T10:00:00Z">
      <block_level>soft</block_level>
      <delta>0.5</delta>
      <point_score>0.3</point_score>
      <recommend_enabled>true</recommend_enabled>
      <soft_penalty_factor>2.5</soft_penalty_factor>
      <description>Road under construction</description>
    </road_override>

    <road_override id="override-002" osm_way_id="123456789" updated_by="traffic_system" updated_at="2025-01-03T11:30:00Z">
      <block_level>hard</block_level>
      <description>Accident blocking road, closed until further notice</description>
    </road_override>

    <road_override id="override-003" segment_id="xyz-789" updated_by="user_feedback_system">
      <block_level>none</block_level>
      <point_score>0.9</point_score>
      <delta>-0.2</delta>
      <recommend_enabled>true</recommend_enabled>
      <description>Highly rated by shippers, encourage use</description>
    </road_override>
  </road_overrides>

  <poi_priorities>
    <poi_priority id="poi-001" poi_id="hospital-central" poi_name="Central Hospital" poi_type="hospital" updated_by="admin">
      <priority>1</priority>
      <latitude>10.8231</latitude>
      <longitude>106.6297</longitude>
      <time_windows>
        <window>
          <start>08:00</start>
          <end>18:00</end>
          <days>1,2,3,4,5</days>
        </window>
      </time_windows>
      <description>Critical medical facility</description>
    </poi_priority>

    <poi_priority id="poi-002" poi_id="shop-123" poi_name="Store A" poi_type="retail">
      <priority>4</priority>
      <latitude>10.8245</latitude>
      <longitude>106.6308</longitude>
      <description>Low priority, optional stop</description>
    </poi_priority>
  </poi_priorities>
</osrm_overlays>
```

---

## Import/Export Functions (Future Implementation)

### Export Overrides to XML

```typescript
import { PrismaClient } from '@prisma/client';
import { writeFileSync } from 'fs';

async function exportOverridesToXML(filePath: string) {
  const prisma = new PrismaClient();
  
  const overrides = await prisma.road_overrides.findMany();
  const pois = await prisma.poi_priorities.findMany();
  
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
  xml += `<osrm_overlays version="1.0" generated="${new Date().toISOString()}">\n`;
  xml += '  <road_overrides>\n';
  
  for (const o of overrides) {
    xml += `    <road_override id="${o.override_id}"`;
    if (o.segment_id) xml += ` segment_id="${o.segment_id}"`;
    if (o.osm_way_id) xml += ` osm_way_id="${o.osm_way_id}"`;
    xml += '>\n';
    xml += `      <block_level>${o.block_level}</block_level>\n`;
    if (o.delta) xml += `      <delta>${o.delta}</delta>\n`;
    if (o.point_score) xml += `      <point_score>${o.point_score}</point_score>\n`;
    xml += `      <recommend_enabled>${o.recommend_enabled}</recommend_enabled>\n`;
    xml += '    </road_override>\n';
  }
  
  xml += '  </road_overrides>\n';
  xml += '  <poi_priorities>\n';
  
  for (const p of pois) {
    xml += `    <poi_priority id="${p.priority_id}" poi_id="${p.poi_id}">\n`;
    xml += `      <priority>${p.priority}</priority>\n`;
    if (p.latitude) xml += `      <latitude>${p.latitude}</latitude>\n`;
    if (p.longitude) xml += `      <longitude>${p.longitude}</longitude>\n`;
    xml += '    </poi_priority>\n';
  }
  
  xml += '  </poi_priorities>\n';
  xml += '</osrm_overlays>\n';
  
  writeFileSync(filePath, xml, 'utf-8');
  console.log(`Exported to ${filePath}`);
}
```

### Import Overrides from XML

Use XML parser (e.g., `xml2js`) to parse and insert into database.

---

## Validation Rules

1. **At least one key**: Each `road_override` must have `segment_id` OR `osm_way_id`
2. **Block level**: Must be one of: none, soft, min, hard
3. **Point score range**: 0.0 to 1.0 if provided
4. **Priority range**: 1 to 5 (integers only)
5. **Coordinates**: Valid WGS84 latitude/longitude
6. **Time format**: HH:MM (24-hour)
7. **Days**: Comma-separated integers 1-7 (1=Monday, 7=Sunday)

---

## Use Cases

### Scenario 1: Traffic Incident

```xml
<road_override id="incident-001" osm_way_id="987654321" updated_by="traffic_control">
  <block_level>hard</block_level>
  <description>Multi-vehicle accident, road closed</description>
</road_override>
```

### Scenario 2: Road Construction (Temporary)

```xml
<road_override id="construction-001" segment_id="seg-abc-123" updated_by="public_works">
  <block_level>soft</block_level>
  <soft_penalty_factor>3.0</soft_penalty_factor>
  <description>Lane closure, heavy delays expected until 2025-02-01</description>
</road_override>
```

### Scenario 3: Highly Recommended Route (Shipper Feedback)

```xml
<road_override id="feedback-001" segment_id="seg-xyz-789" updated_by="feedback_system">
  <block_level>none</block_level>
  <point_score>0.95</point_score>
  <delta>-0.3</delta>
  <recommend_enabled>true</recommend_enabled>
  <description>Excellent road condition, 4.8/5 shipper rating</description>
</road_override>
```

### Scenario 4: Rush Hour Priority POI

```xml
<poi_priority id="poi-hospital-rush" poi_id="hospital-001" poi_type="hospital">
  <priority>1</priority>
  <latitude>10.8231</latitude>
  <longitude>106.6297</longitude>
  <time_windows>
    <window>
      <start>07:00</start>
      <end>09:00</end>
      <days>1,2,3,4,5</days>
    </window>
    <window>
      <start>17:00</start>
      <end>19:00</end>
      <days>1,2,3,4,5</days>
    </window>
  </time_windows>
  <description>Highest priority during rush hours</description>
</poi_priority>
```

---

## Migration Path

**Current**: Database-first (MySQL tables)  
**Future**: Hybrid approach
- Database for live/operational overrides
- XML for backup, version control, and bulk import/export
- Sync mechanism to keep both in sync

This allows:
- Quick database updates via API
- Git version control of XML files
- Easy rollback via XML restore
- Audit trail in both systems
