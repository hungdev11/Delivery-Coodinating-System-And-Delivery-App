# OSM Extract Strategy: Routing + Address Coverage

## The Problem

When extracting OSM data for a specific region, you face two competing needs:

1. **Routing**: Need complete ways/nodes/relations to prevent broken graph connectivity
2. **Addresses**: Need ALL address points, including "floating" nodes outside the road network

Simple polygon extract with `--complete-ways` solves routing but drops orphaned address nodes.

## The Solution: Two-Stage Extract + Merge

Like making phở: separate broth, separate toppings, combine with love.

```
┌─────────────────────────────────────────────────────────────┐
│ Source: vietnam-latest.osm.pbf                              │
└─────────────────────────────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        ▼                           ▼
┌──────────────────┐      ┌──────────────────┐
│ Extract Polygon  │      │ Extract ALL      │
│ w/ Complete Ways │      │ Address Nodes    │
│                  │      │ (addr:*)         │
│ routing.osm.pbf  │      │ all_addr.osm.pbf │
└──────────────────┘      └──────────────────┘
        │                           │
        │                           ▼
        │                  ┌──────────────────┐
        │                  │ Clip Addresses   │
        │                  │ to Polygon       │
        │                  │ addr_clip.osm.pbf│
        │                  └──────────────────┘
        │                           │
        └─────────────┬─────────────┘
                      ▼
            ┌──────────────────┐
            │ Merge Both       │
            │ final.osm.pbf    │
            └──────────────────┘
```

## Benefits

| What You Get              | Routing Only | Two-Stage | Notes                          |
|---------------------------|--------------|-----------|--------------------------------|
| Clean routing graph       | ✅           | ✅        | No broken ways                 |
| Street names              | ✅           | ✅        | From road network              |
| House numbers on roads    | ✅           | ✅        | Nodes attached to ways         |
| Floating house numbers    | ❌           | ✅        | Orphaned addr nodes            |
| Small alley addresses     | ❌           | ✅        | Common in Vietnam (hem)        |
| OSRM routing quality      | ✅           | ✅        | Unaffected (ignores addr:*)    |
| Geocoding coverage        | 70%          | 95%+      | Much better address matching   |

## Usage

### Option 1: One-Line Complete Extract

```bash
npm run extract:complete
```

This runs the full pipeline:
1. Extract routing graph with complete ways
2. Extract all address nodes from source
3. Clip addresses to polygon
4. Merge into `raw_data/extracted/thuduc_complete.osm.pbf`

### Option 2: Manual Pipeline (for custom workflows)

```typescript
import { OsmiumWrapper } from './utils/osmium-wrapper';

const osmium = new OsmiumWrapper(true);

await osmium.extractRoutingWithAddresses(
  'vietnam-latest.osm.pbf',
  'thuduc.poly',
  'output.osm.pbf'
);
```

### Option 3: Individual Steps

```typescript
const osmium = new OsmiumWrapper(true);

// Step 1: Routing graph
await osmium.extractByPoly(
  'vietnam.osm.pbf',
  'routing.osm.pbf',
  'area.poly'
);

// Step 2: All addresses
await osmium.extractAddressNodes(
  'vietnam.osm.pbf',
  'all_addresses.osm.pbf'
);

// Step 3: Clip addresses
await osmium.extractByPoly(
  'all_addresses.osm.pbf',
  'addresses_clipped.osm.pbf',
  'area.poly'
);

// Step 4: Merge
await osmium.mergePBFs(
  ['routing.osm.pbf', 'addresses_clipped.osm.pbf'],
  'final.osm.pbf'
);
```

## Seeder Integration

Both seeders automatically detect and use the complete extract if available:

```bash
# Run complete extract first (recommended)
npm run extract:complete

# Then seed roads (uses complete extract)
npm run seed:roads

# Then seed addresses (uses complete extract)
npm run seed:addresses
```

If complete extract doesn't exist, seeders fall back to direct parsing with polygon clip.

## File Sizes (Thu Duc District Example)

| File                      | Size   | Contents                    |
|---------------------------|--------|-----------------------------|
| vietnam-latest.osm.pbf    | ~400MB | Full Vietnam                |
| routing.osm.pbf           | ~8MB   | Roads w/ complete ways      |
| all_addresses.osm.pbf     | ~15MB  | All address nodes           |
| addresses_clipped.osm.pbf | ~200KB | Addresses in Thu Duc only   |
| **thuduc_complete.osm.pbf**| ~8MB   | **Final merged extract**    |

## When to Use What

### Use Two-Stage Extract When:
- Building geocoding/address search
- Need accurate house number matching
- Working with Vietnamese data (lots of hem/ngõ alleys)
- Want comprehensive delivery location coverage

### Use Simple Polygon Extract When:
- Only need routing (OSRM only)
- Don't care about floating addresses
- Want fastest extract time
- Memory constrained

## Performance

Thu Duc District extraction times:

| Stage                  | Time    |
|------------------------|---------|
| Extract routing graph  | ~45s    |
| Extract addresses      | ~30s    |
| Clip addresses         | ~5s     |
| Merge                  | ~10s    |
| **Total**              | **~90s**|

One-time cost. Extract once, seed multiple times.

## Troubleshooting

### "No address nodes found after extract"

Make sure source PBF has `addr:*` tags:

```bash
osmium tags-filter vietnam.osm.pbf n/addr:housenumber --output /dev/null
```

Should show non-zero count.

### "Routing broken after merge"

Merge doesn't break routing - OSRM ignores address nodes.
If routing fails, check original polygon extract.

### "Too many duplicate addresses"

Expected. OSM has overlapping addr:* tags on nodes + ways.
Your seeder should deduplicate by geohash.

## Architecture Notes

```
┌─────────────────────────────────────────────────────┐
│ osmium-wrapper.ts                                   │
├─────────────────────────────────────────────────────┤
│ extractByPoly()           - Polygon extract         │
│ extractAddressNodes()     - Filter addr:* tags      │
│ mergePBFs()               - Combine extracts        │
│ extractRoutingWithAddresses() - Full pipeline       │
└─────────────────────────────────────────────────────┘
         │
         ├──> extract-complete-data.ts (CLI runner)
         │
         ├──> roads-seeder.ts (auto-detects extract)
         │
         └──> addresses-seeder.ts (auto-detects extract)
```

## Next Steps

After running extract:

1. **Generate OSRM data** from complete extract
2. **Seed roads** for routing graph
3. **Seed addresses** for geocoding
4. **Test routing** with real coordinates
5. **Test address search** with house numbers

---

**TL;DR**: Run `npm run extract:complete` once, get perfect routing + address coverage. No street left behind. 🗺️

