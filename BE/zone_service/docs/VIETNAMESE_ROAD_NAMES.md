# Vietnamese Road Name Generation

## Problem Solved

OSM data in Vietnam has **~71% unnamed roads** (12,285 out of 17,255 in Thu Duc). These are mostly:
- Small alleys (hem/ngõ)
- Service roads
- Internal residential streets
- Parking lot access roads

Skipping them breaks routing connectivity.

## Solution: Smart Vietnamese Name Generation

Instead of skipping unnamed roads, we now generate **meaningful Vietnamese names** based on road characteristics.

## Generated Name Categories

### 1. **Highway Types**

| OSM highway=       | Vietnamese Name              | English                    |
|--------------------|------------------------------|----------------------------|
| `motorway`         | Đường cao tốc không tên      | Unnamed motorway           |
| `trunk`            | Đường quốc lộ không tên      | Unnamed national road      |
| `primary`          | Đường chính không tên        | Unnamed primary road       |
| `secondary`        | Đường cấp hai không tên      | Unnamed secondary road     |
| `tertiary`         | Đường cấp ba không tên       | Unnamed tertiary road      |
| `residential`      | Đường dân cư không tên       | Unnamed residential road   |
| `unclassified`     | Đường nhỏ không tên          | Unnamed small road         |
| `living_street`    | Đường nội bộ                 | Internal street            |
| `pedestrian`       | Đường đi bộ                  | Pedestrian way             |
| `track`            | Đường mòn                    | Track                      |
| `road`             | Đường không rõ loại          | Unclassified road          |

### 2. **Service Roads** (highway=service)

| OSM service=       | Vietnamese Name              | Context                    |
|--------------------|------------------------------|----------------------------|
| `parking_aisle`    | Lối đi bãi đỗ xe             | Parking lot aisle          |
| `driveway`         | Lối vào                      | Driveway                   |
| `alley`            | Hẻm không tên                | Unnamed alley              |
| (other)            | Đường phụ                    | Service road               |

### 3. **Road Links** (ramps, connectors)

| OSM highway=       | Vietnamese Name              | Context                    |
|--------------------|------------------------------|----------------------------|
| `motorway_link`    | Nhánh rẽ                     | Highway ramp/link          |
| `trunk_link`       | Nhánh rẽ                     | Trunk link                 |
| `primary_link`     | Nhánh rẽ                     | Primary link               |
| (other links)      | Nhánh rẽ                     | Road connection            |

### 4. **Reference Numbers** (Priority)

If road has `ref` tag (like QL1A, DT743):
- Vietnamese: `Đường QL1A`
- English: `Route QL1A`

### 5. **Access Restrictions**

If `access=private`:
- Appends: ` (Riêng tư)` → "Private" marker

## Implementation

```typescript
const { name, nameEn, isNamed } = OSMParser.getRoadName(way.tags);

// Returns:
{
  name: "Đường dân cư không tên",    // Always has a name
  nameEn: undefined,                  // May have English
  isNamed: false                      // Tracks if original vs generated
}
```

## Statistics Tracking

Seeder now shows:
```
✓ Prepared 17255 roads (4970 có tên gốc, 12285 tên được tạo)
```

Translation: "17255 roads (4970 original names, 12285 generated names)"

## Benefits

### Before (Skipping Unnamed)
- ❌ 4,970 roads (29% coverage)
- ❌ Broken routing through residential areas
- ❌ Missing hem/ngõ navigation

### After (Generated Names)
- ✅ 17,255 roads (100% coverage)
- ✅ Complete routing graph
- ✅ Meaningful Vietnamese names for UI
- ✅ Better user experience

## Vietnamese Context

These generated names are culturally appropriate:

**Đường dân cư không tên** = "Unnamed residential road"
- Common for small internal streets in Vietnam
- Users understand these are auto-generated
- Better than blank or "Unnamed"

**Hẻm không tên** = "Unnamed alley"
- Vietnamese "hem" culture (alley addresses)
- Many addresses use "Hẻm 123" format
- Generated name helps users understand road type

**Đường nội bộ** = "Internal street"
- Living streets in residential compounds
- Restricted access, low speed
- Clear semantic meaning

## Search Filtering

Use `isNamed` flag to filter results:

```typescript
// Only show originally named roads in search
const searchResults = roads.filter(r => r.isNamed === true);

// Show all roads for routing
const routingGraph = roads;  // Include generated names
```

## Example Output

**Original OSM Data:**
```xml
<way id="123456789">
  <nd ref="1"/>
  <nd ref="2"/>
  <tag k="highway" v="residential"/>
  <!-- No name tag -->
</way>
```

**Generated in Database:**
```json
{
  "road_id": "uuid",
  "osm_id": "123456789",
  "name": "Đường dân cư không tên",
  "name_en": null,
  "road_type": "residential"
}
```

## Future Enhancements

Possible improvements:

1. **Contextual naming**: "Hẻm gần Đường ABC" (Alley near ABC Street)
2. **Numbered alleys**: "Hẻm 1", "Hẻm 2" based on position
3. **District-specific**: "Đường nội bộ Thủ Đức" (Thu Duc internal road)
4. **Smart suggestions**: Learn from nearby named roads

## Configuration

All name generation logic is in:
```
BE/zone_service/utils/osm-parser.ts
→ OSMParser.getRoadName()
```

To customize names, edit the `switch (highway)` cases.

## Summary

**Coverage:** 29% → 100%  
**Named roads:** 4,970 (original)  
**Generated names:** 12,285 (meaningful)  
**Total roads:** 17,255  

**Result:** Complete routing graph with culturally appropriate Vietnamese names. No street left behind. 🇻🇳🗺️
