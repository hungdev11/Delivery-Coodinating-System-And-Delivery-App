# Motorbike Routing with Shipper Feedback 🏍️

## The Vietnamese Delivery Problem

Bạn đã build routing engine **có cảm xúc**: đường nào bực, đường nào mượt, shipper vote, OSRM obey.

## What We Built

### 1. **Dual Vehicle System**

Two separate OSRM instances running in parallel:

| Instance | Vehicle | Profile | Special Features |
|----------|---------|---------|------------------|
| `osrm-car` | 4-wheel | `custom_car.lua` | Traffic awareness, all roads |
| `osrm-motorbike` | 2-wheel | `custom_motorbike.lua` | Shipper feedback, NO motorways |

### 2. **Shipper Feedback Integration**

**Data Flow:**
```
Shipper delivers → Rates road (1-5 stars) → Stored in user_feedback
                                                      ↓
                                          Aggregated per segment
                                                      ↓
                                     Exported to OSM XML as shipper_score
                                                      ↓
                                        Lua profile reads & applies penalty
                                                      ↓
                                          Bad roads avoided in routing
```

**XML Export:**
```xml
<way id="123456">
  <tag k="highway" v="residential"/>
  <tag k="name" v="Đường Dân Cư"/>
  <tag k="custom_weight" v="42.5"/>
  <tag k="traffic_level" v="NORMAL"/>
  <tag k="shipper_score" v="0.73"/>  <!-- New! 0-1 scale -->
</way>
```

### 3. **Motorbike Profile Characteristics**

**Vietnam-Optimized Speeds:**
```lua
trunk = 60        -- Quốc lộ
primary = 50      -- Đường chính
residential = 30  -- Đường dân cư
service = 20      -- Đường phụ
living_street = 15 -- Đường nội bộ
```

**Motorway Ban:**
```lua
if highway == "motorway" or highway == "motorway_link" then
  return  -- Motorbikes can't use motorways in Vietnam
end
```

**Shipper Penalty Formula:**
```lua
-- Score 0.3 → penalty 1.7x (avoid!)
-- Score 0.5 → penalty 1.5x (not great)
-- Score 0.8 → penalty 1.2x (okay)
-- Score 1.0 → penalty 1.0x (perfect!)
local shipper_penalty = 2.0 - shipper_score
local adjusted_weight = custom_weight * shipper_penalty
```

**Traffic Handling:**
```lua
-- Motorbikes weave better in congestion
traffic_multiplier = 0.5  -- vs 0.4 for cars in CONGESTED
```

## Architecture

### Data Sources

```
┌─────────────────────────────────────────────────────┐
│ Data Inputs                                         │
├─────────────────────────────────────────────────────┤
│ 1. OSM Road Network → base_weight                   │
│ 2. TomTom Traffic API → traffic conditions          │
│ 3. Shipper Feedback → user_feedback (score 1-5)    │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ Database Aggregation                                │
├─────────────────────────────────────────────────────┤
│ Per segment:                                        │
│   • current_weight (traffic adjusted)               │
│   • congestion_score                                │
│   • avg shipper_score (normalized 0-1)             │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ OSM XML Export                                      │
├─────────────────────────────────────────────────────┤
│ <way>                                               │
│   <tag k="custom_weight" v="42.5"/>                │
│   <tag k="traffic_level" v="NORMAL"/>              │
│   <tag k="shipper_score" v="0.73"/>                │
│ </way>                                              │
└─────────────────────────────────────────────────────┘
                        ↓
         ┌──────────────┴──────────────┐
         ↓                             ↓
┌──────────────────┐         ┌──────────────────┐
│ custom_car.lua   │         │ custom_motorbike │
│                  │         │      .lua        │
│ • All roads      │         │ • No motorway    │
│ • Traffic weight │         │ • Shipper weight │
│ • Car speeds     │         │ • Bike speeds    │
└──────────────────┘         └──────────────────┘
         ↓                             ↓
┌──────────────────┐         ┌──────────────────┐
│ OSRM Instance 1  │         │ OSRM Instance 2  │
│ (Car)            │         │ (Motorbike)      │
└──────────────────┘         └──────────────────┘
```

## Shipper Feedback Formula Deep Dive

### Normalization (DB to Lua)

**Input:** Shipper ratings 1-5 stars
```typescript
// In TypeScript
const normalizedScore = feedback.score / 5.0;  // 0.2 to 1.0
```

**Usage:** Lua profile applies penalty
```lua
-- In Lua
local shipper_penalty = 2.0 - shipper_score
```

### Example Scenarios

| Shipper Stars | Normalized | Penalty | Weight Impact | Meaning |
|---------------|------------|---------|---------------|---------|
| 5.0 ⭐⭐⭐⭐⭐ | 1.0 | 1.0x | No change | Perfect road |
| 4.0 ⭐⭐⭐⭐ | 0.8 | 1.2x | 20% slower | Good road |
| 3.0 ⭐⭐⭐ | 0.6 | 1.4x | 40% slower | Okay road |
| 2.0 ⭐⭐ | 0.4 | 1.6x | 60% slower | Bad road |
| 1.0 ⭐ | 0.2 | 1.8x | 80% slower | Terrible road |

**Result:** OSRM avoids low-rated roads when finding routes.

## Real-World Impact

### Scenario 1: Delivery During Rush Hour

**Without Shipper Feedback:**
```
Route: Main street (jammed) → 15 min ETA
```

**With Shipper Feedback:**
```
Route: Side alley (shipper-approved, score 0.9) → 8 min ETA
Reason: Main street has traffic + low shipper score (bumpy)
```

### Scenario 2: Construction Zone

**Shipper reports:**
- Construction on Đường A → Score 1/5 (terrible)
- System learns → Penalty 1.8x
- Next shipper → Routed around it automatically

### Scenario 3: Hidden Good Routes

**Local knowledge:**
- Hẻm 123 → Score 5/5 (smooth, fast)
- Not obvious from map data
- System learns → Recommends to other shippers

## API Usage

### Car Routing
```bash
curl "http://localhost:5000/route/v1/car/106.677,10.762;106.702,10.773"
```

### Motorbike Routing
```bash
curl "http://localhost:5001/route/v1/motorbike/106.677,10.762;106.702,10.773"
```

**Response includes:**
```json
{
  "routes": [{
    "duration": 480,  // seconds
    "distance": 3500, // meters
    "weight": 42.5,   // adjusted for traffic + feedback
    "legs": [...]
  }]
}
```

## Files Modified

| File | Changes |
|------|---------|
| `generate-osrm-data.ts` | Added motorbike instance, shipper score export |
| `custom_car.lua` | Generated (traffic-aware) |
| `custom_motorbike.lua` | **New** (shipper feedback + no motorways) |

## Performance Considerations

### Shipper Score Aggregation

```typescript
// Takes last 10 feedbacks per segment
user_feedback: {
  orderBy: { created_at: 'desc' },
  take: 10
}

// Averages to single score
shipper_score = avg(last_10_feedbacks) / 5.0
```

**Why limit to 10?**
- Recent feedback more relevant
- Road conditions change over time
- Prevents stale data from dominating

## Future Enhancements

### 1. **Time-Based Feedback**
```typescript
// Morning vs evening ratings
const morningScore = getShipperScore(segment, 'morning');
const eveningScore = getShipperScore(segment, 'evening');
```

### 2. **Weather-Adjusted**
```lua
if raining and shipper_score < 0.8 then
  shipper_penalty = shipper_penalty * 1.2  -- Extra careful in rain
end
```

### 3. **Vehicle-Specific Feedback**
```typescript
// Scooter vs motorcycle ratings
motorcycleFeedback  // Heavier bikes prefer smooth roads
scooterFeedback     // Lighter bikes okay with rougher roads
```

### 4. **ML Predictions**
```python
# Train model on shipper feedback
predicted_score = model.predict(segment_features)
# Use when no human feedback available
```

## Summary

**What You Built:**
- ✅ Dual vehicle routing (car + motorbike)
- ✅ Real-time traffic integration
- ✅ Shipper feedback scoring system
- ✅ Vietnam-optimized speeds
- ✅ Motorway restrictions for bikes
- ✅ Smart weight penalties for bad roads

**The Magic:**
Your routing engine now **learns from delivery drivers**. When a shipper says "this road sucks," the system listens and routes future deliveries around it.

**Business Value:**
- Faster deliveries (better routes)
- Happier shippers (avoid bad roads)
- Real-world learning (crowdsourced data)
- Competitive advantage (Grab/Be level routing)

**Technical Achievement:**
Bạn đã tích hợp **3 layers of intelligence**:
1. **Static:** OSM base data
2. **Dynamic:** Real-time traffic
3. **Human:** Shipper experience

Kiểu kiến trúc này là **production-grade delivery routing**. Ngửi tới mùi Series A luôn. 🚀

---

**Bản đồ giờ "biết giận biết thương".** ✨
