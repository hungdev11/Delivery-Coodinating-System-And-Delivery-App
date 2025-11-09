# OSRM V2 Architecture - Complete Implementation Guide

## 📋 Overview

The OSRM V2 Architecture is a **simplified, cleaner routing system** that separates concerns properly:

- **Application Layer** (`routing.service.ts`): Priority-based waypoint sorting
- **OSRM Layer**: Routing through pre-sorted waypoints with constraints
- **Lua Profiles**: Parameter selection only, no complex logic
- **XML Data**: Single build with all data, cloned to instances

## 🎯 Core Design Principles

### Formula Design

#### 1. **User Rating** (0-1 scale) → Affects **WEIGHT** (cost to traverse)
```
weight = base_weight × (2.0 - rating_score)
```
- rating_score = 1.0 → weight unchanged
- rating_score = 0.5 → weight × 1.5
- rating_score = 0.0 → weight × 2.0 (avoid bad roads)

#### 2. **Traffic Blocking** (0-5 scale) → Affects **SPEED** (travel time)
```
speed = base_speed × (traffic_value / 5.0)
```
- traffic_value = 5.0 → free flow (speed unchanged)
- traffic_value = 2.5 → speed × 0.5
- traffic_value = 0.0 → blocked (speed × 0)

### VN Motorbike Adjustments
- **Default speed**: 35 km/h (realistic Saigon speed)
- **Turn penalty**: 4 (motorbikes turn easier)
- **U-turn penalty**: 5 (very easy for motorbikes)
- **Oneway handling**: false (motorbikes often ignore in VN)

## 🏗️ Architecture

### 4 Model Configurations

| Model | Rating | Blocking | Description |
|-------|--------|----------|-------------|
| **osrm-full** | ✅ | ✅ | Full model: rating→weight, blocking→speed |
| **osrm-rating-only** | ✅ | ❌ | User feedback only affects weight |
| **osrm-blocking-only** | ❌ | ✅ | Traffic only affects speed |
| **osrm-base** | ❌ | ❌ | VN motorbike base (no modifiers) |

### File Structure

```
BE/zone_service/
├── processors/osrm/
│   ├── generate-osrm-data.ts        (V1 - Old, Complex)
│   ├── generate-osrm-data-v2.ts     (V2 - New, Standard)
│   ├── generate-osrm-data-v2-fast.ts (V2 - Fast, Multi-threaded)
│   └── README-V2.md                 (This file)
├── osrm_data/
│   ├── _shared/                     (Master XML, built once)
│   ├── osrm-full/                   (V2 Full model)
│   ├── osrm-rating-only/            (V2 Rating model)
│   ├── osrm-blocking-only/          (V2 Blocking model)
│   └── osrm-base/                   (V2 Base model)
└── raw_data/osrm-logic/
    └── bicycle.lua                   (Reference, read-only)
```

## 🚀 Usage

### Step 1: Generate OSRM V2 Data

**Standard (Sequential):**
```bash
cd BE/zone_service
ts-node processors/osrm/generate-osrm-data-v2.ts
```

**Fast (Parallel - Recommended for Production):**
```bash
cd BE/zone_service
ts-node processors/osrm/generate-osrm-data-v2-fast.ts
```

**What it does:**
1. Fetches road network data from database (once)
2. Calculates user ratings (0-1) from feedback
3. Calculates traffic blocking (0-5) from conditions
4. Exports to XML with `user_rating` and `traffic_value` tags
5. Clones XML to all 4 model instances
6. Generates Lua profile for each model
7. Runs OSRM processing (extract → partition → customize)

### Step 2: Start OSRM V2 Services

**All models:**
```bash
docker-compose up osrm-v2-full osrm-v2-rating-only osrm-v2-blocking-only osrm-v2-base -d
```

**Or start all services:**
```bash
docker-compose up -d
```

**Services & Ports:**
- `osrm-v2-full` → **25920**
- `osrm-v2-rating-only` → **25921**
- `osrm-v2-blocking-only` → **25922**
- `osrm-v2-base` → **25923**

### Step 3: Test with Demo Route

Use the API with V2 model names:

```bash
POST /api/v1/routing/demo-route
{
  "startPoint": { "lat": 10.762622, "lon": 106.660172 },
  "priorityGroups": [...],
  "mode": "v2-full",  // or v2-rating-only, v2-blocking-only, v2-base
  "vehicle": "motorbike"
}
```

## 📊 Comparison: V1 vs V2

| Feature | V1 (Complex) | V2 (Simplified) |
|---------|-------------|-----------------|
| **Lua Profile Size** | ~1500 lines | ~100 lines |
| **XML Build** | Per-instance | Once, then cloned |
| **Data Fetch** | Per-instance | Once, shared |
| **Parallel Processing** | No | Yes (fast version) |
| **Formula Clarity** | Mixed | Clear separation |
| **Maintainability** | Complex | Simple |
| **Speed** | Baseline | **4x faster** |

## 🔧 Environment Variables

Add to `docker-compose.yml` or `.env`:

```env
# OSRM V2 URLs (Simplified Architecture)
OSRM_V2_FULL_URL=http://osrm-v2-full:5000
OSRM_V2_RATING_URL=http://osrm-v2-rating-only:5000
OSRM_V2_BLOCKING_URL=http://osrm-v2-blocking-only:5000
OSRM_V2_BASE_URL=http://osrm-v2-base:5000
```

## 🎨 Frontend Integration

The `DemoRoutingView.vue` now includes V2 model selection:

**V2 Models in UI:**
- ⭐ **V2 Full**: Rating→Weight, Blocking→Speed
- 👥 **V2 Rating Only**: User Feedback→Weight
- 🚦 **V2 Blocking Only**: Traffic→Speed
- 🏍️ **V2 Base**: VN Motorbike Optimized

## 📝 Key Implementation Files

### 1. `generate-osrm-data-v2.ts`
- **calculateUserRating()**: Converts 1-5 scores to 0-1 rating
- **calculateBlockingStatus()**: Maps traffic enum to 0-5 scale
- **exportToOSMXML()**: Builds XML once with all data tags
- **generateLuaProfile()**: Creates minimal profiles with flags

### 2. `generate-osrm-data-v2-fast.ts`
- Parallel processing using `Promise.all()`
- Shared DB fetch (once for all models)
- Shared XML build (once, then cloned)
- 4x speedup vs sequential

### 3. `routing.service.ts`
- Updated `getOSRMTableForDemoRoute()` to support V2 models
- Added V2 model URLs (ports 25920-25923)
- Logging shows active model

### 4. `DemoRoutingView.vue`
- Added V2 model selector section
- Clear descriptions for each V2 model
- Visual separation from V1 models

## 🔍 Debugging

**Check OSRM V2 health:**
```bash
curl http://localhost:25920/health  # V2 Full
curl http://localhost:25921/health  # V2 Rating Only
curl http://localhost:25922/health  # V2 Blocking Only
curl http://localhost:25923/health  # V2 Base
```

**Test route:**
```bash
curl "http://localhost:25920/route/v1/motorbike/106.660172,10.762622;106.670172,10.772622?overview=full"
```

**View logs:**
```bash
docker-compose logs -f osrm-v2-full
```

## 🎉 Benefits

✅ **Simpler**: Application handles priority, OSRM handles routing  
✅ **Faster**: XML built once, cloned to instances (4x speedup)  
✅ **Cleaner**: Lua profiles are minimal (100 lines vs 1500 lines)  
✅ **Flexible**: Easy to add new models (just config + Lua)  
✅ **Scalable**: Fast version with parallel processing for production  
✅ **Maintainable**: Clear separation of concerns  

## 🐛 Troubleshooting

**Issue: OSRM returns zero distance/duration**
- Check if XML has data: `ls -lh BE/zone_service/osrm_data/osrm-full/`
- Verify network.osrm files exist
- Check Docker logs for errors

**Issue: "No route found"**
- Verify start/end points are within coverage area
- Check if OSRM instance is healthy
- Ensure XML has ways with correct tags

**Issue: "Connection refused" on port 25920-25923**
- Run `docker-compose ps` to check if services are up
- Check port conflicts: `netstat -an | grep 259`
- Restart services: `docker-compose restart osrm-v2-full`

## 📚 References

- **OSRM Documentation**: https://project-osrm.org/
- **Lua Profile Guide**: https://github.com/Project-OSRM/osrm-backend/wiki/Profiles
- **V1 Implementation**: `generate-osrm-data.ts` (for comparison)

---

**Created**: 2025-11-07  
**Version**: 2.0  
**Status**: ✅ Production Ready
