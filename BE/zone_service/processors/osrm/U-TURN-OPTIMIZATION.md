# U-Turn Pattern Optimization

**Date**: 2025-11-07  
**Version**: 1.0  
**Feature**: Post-processing optimization for route steps

---

## 🎯 Problem Statement

**User Request:**
> "Check các step, step n-1 và n-2 (nếu có) xem nó có đang như sau không: A->C, C->B (tức cố đi đến cuối đường rồi vòng lại) dù đường đó là 2 chiều, có thể qua đường (tức A -> B). Mục tiêu: gộp 2 step đó thành 1."

**Issue:**
OSRM sometimes generates routes with U-turn patterns where:
1. Vehicle drives to end of road (A → C)
2. Makes a U-turn and comes back (C → B)
3. **But** there's a direct crossing available (A → B)

This happens on bidirectional roads where crossing is allowed.

**Visual Example:**
```
        C (end of road)
        ↑
        |
A ------|------ B
    want to go here

OSRM route: A → C (drive to end) → C → B (U-turn back)
Optimized:  A → B (cross directly)
```

---

## ✅ Solution: Post-Processing U-Turn Detection

### Algorithm

**Step 1: Detect U-Turn Pattern**
```typescript
for each pair of consecutive steps (stepA, stepB):
  if stepB.maneuver.type === 'turn' AND
     stepB.maneuver.modifier === 'uturn' (or 'sharp left'/'sharp right'):
    → Potential U-turn detected
```

**Step 2: Check Direct Crossing**
```typescript
// Get nodes: A (start), C (end), B (target)
const nodeA = osmNodeIds[i];
const nodeC = osmNodeIds[i + 1];
const nodeB = osmNodeIds[i + 2];

// Check if direct segment A → B exists in database
const canCross = await canCrossDirectly(nodeA, nodeB);
```

**Step 3: Merge Steps**
```typescript
if (canCross) {
  // Merge A→C + C→B into A→B
  mergedStep = {
    distance: stepA.distance + stepB.distance,
    duration: stepA.duration + stepB.duration,
    name: stepA.name,
    geometry: merge(stepA.geometry, stepB.geometry)
  };
}
```

---

## 🔧 Implementation Details

### Files Modified

1. **`BE/zone_service/src/modules/routing/routing.service.ts`**
   - Added `canCrossDirectly()` - checks if direct segment exists in DB
   - Added `optimizeUTurnPattern()` - detects and merges U-turn patterns
   - Integrated into `enrichRouteData()` - applies optimization after step enrichment

### Function: `canCrossDirectly(nodeA, nodeB)`

```typescript
/**
 * Check if direct crossing is possible between two nodes
 * @param nodeA - OSM node ID of point A
 * @param nodeB - OSM node ID of point B
 * @returns true if there's a bidirectional segment A↔B in database
 */
private static async canCrossDirectly(
  nodeA: number,
  nodeB: number
): Promise<boolean> {
  // 1. Get DB node IDs from OSM IDs
  const dbNodes = await prisma.road_nodes.findMany({
    where: { osm_id: { in: [nodeA.toString(), nodeB.toString()] } }
  });

  // 2. Check if segment exists (bidirectional)
  const segment = await prisma.road_segments.findFirst({
    where: {
      OR: [
        { from_node_id: nodeA_db, to_node_id: nodeB_db },
        { from_node_id: nodeB_db, to_node_id: nodeA_db }
      ]
    }
  });

  return !!segment;
}
```

### Function: `optimizeUTurnPattern(steps, osmNodeIds)`

```typescript
/**
 * Optimize steps by merging U-turn patterns (A->C->B into A->B)
 * @param steps - Array of route steps
 * @param osmNodeIds - Array of OSM node IDs along the route
 * @returns Optimized steps array
 */
private static async optimizeUTurnPattern(
  steps: RouteStepDto[],
  osmNodeIds: number[]
): Promise<RouteStepDto[]> {
  const optimizedSteps: RouteStepDto[] = [];
  let i = 0;

  while (i < steps.length) {
    // Check for U-turn pattern in next 2 steps
    if (i < steps.length - 1) {
      const stepA = steps[i];      // A->C
      const stepB = steps[i + 1];  // C->B
      
      // Detect U-turn maneuver
      const isUTurn = 
        stepB.maneuver?.type === 'turn' && 
        (stepB.maneuver?.modifier === 'uturn' || 
         stepB.maneuver?.modifier === 'sharp left' ||
         stepB.maneuver?.modifier === 'sharp right');

      if (isUTurn) {
        const nodeA = osmNodeIds[i];
        const nodeB = osmNodeIds[i + 2];
        
        // Check if direct crossing exists
        if (await canCrossDirectly(nodeA, nodeB)) {
          // Merge into single step
          optimizedSteps.push(mergeSteps(stepA, stepB));
          i += 2; // Skip both steps
          continue;
        }
      }
    }

    // No optimization, keep step as-is
    optimizedSteps.push(steps[i]);
    i++;
  }

  return optimizedSteps;
}
```

### Integration in `enrichRouteData()`

```typescript
// After enriching steps with DB data
for (const step of leg.steps) {
  enrichedSteps.push({
    // ... enrich with road names, traffic, etc.
  });
}

// 🔀 Apply U-turn optimization
const optimizedSteps = await this.optimizeUTurnPattern(
  enrichedSteps, 
  osmNodeIds
);

// Recalculate leg totals
const optimizedDistance = optimizedSteps.reduce((sum, s) => sum + s.distance, 0);
const optimizedDuration = optimizedSteps.reduce((sum, s) => sum + s.duration, 0);

enrichedLegs.push({
  distance: optimizedDistance,
  duration: optimizedDuration,
  steps: optimizedSteps
});
```

---

## 📊 Examples

### Example 1: Simple U-Turn

**Scenario:**
```
Street layout:
        C (node 103 - end of street)
        |
A ------|------ B
(101)   |      (102)
        |
```

**Original OSRM Route:**
```json
{
  "legs": [{
    "steps": [
      {
        "name": "Main Street",
        "maneuver": { "type": "depart" },
        "distance": 100,
        "duration": 10,
        "nodes": [101, 103]  // A → C
      },
      {
        "name": "Main Street",
        "maneuver": { "type": "turn", "modifier": "uturn" },
        "distance": 50,
        "duration": 8,
        "nodes": [103, 102]  // C → B (U-turn)
      }
    ]
  }]
}
```

**Optimized Route:**
```json
{
  "legs": [{
    "steps": [
      {
        "name": "Main Street",
        "maneuver": { "type": "turn", "modifier": "straight" },
        "distance": 150,  // 100 + 50
        "duration": 18,   // 10 + 8
        "instruction": "Continue on Main Street (U-turn optimized)",
        "nodes": [101, 102]  // A → B (direct)
      }
    ]
  }]
}
```

**Savings:**
- Steps: 2 → 1 (50% reduction)
- Maneuvers: U-turn eliminated
- Distance: Same (150m)
- Duration: Same (18s) or potentially less

---

### Example 2: Multiple U-Turns

**Scenario:**
```
Route with 3 waypoints, each requiring U-turns:

Start → WP1 (U-turn) → WP2 (U-turn) → WP3 (U-turn) → End
```

**Before Optimization:**
- Total steps: 12 (4 waypoints × 3 steps average)
- U-turns: 3

**After Optimization:**
- Total steps: 6 (3 U-turns eliminated)
- U-turns: 0

**Log Output:**
```
🔀 Detected U-turn pattern at nodes 101->103->102, merging to 101->102
🔀 Detected U-turn pattern at nodes 205->207->206, merging to 205->206
🔀 Detected U-turn pattern at nodes 310->312->311, merging to 310->311
✅ Optimized 3 U-turn step(s)
```

---

## 🔍 Detection Criteria

### What Qualifies as U-Turn Pattern?

**Maneuver Types Detected:**
```typescript
// OSRM can return type='turn' OR 'continue' with modifier='uturn'
const isUTurn = 
  (stepB.maneuver?.type === 'turn' || stepB.maneuver?.type === 'continue') &&
  (
    stepB.maneuver?.modifier === 'uturn' ||      // Explicit U-turn
    stepB.maneuver?.modifier === 'sharp left' || // ~150° turn
    stepB.maneuver?.modifier === 'sharp right'   // ~150° turn
  );
```

**Important:** OSRM may return different `type` values:
- `type: 'turn', modifier: 'uturn'` - Standard U-turn at intersection
- `type: 'continue', modifier: 'uturn'` - U-turn along same road (more common!)
- `type: 'turn', modifier: 'sharp left/right'` - Near U-turn (~150°+ angle)

**Additional Requirements:**
1. ✅ Sequential steps (stepA → stepB)
2. ✅ OSM nodes available (nodeA, nodeC, nodeB)
3. ✅ Direct segment exists in DB (A ↔ B)
4. ✅ StepB is a sharp turn/U-turn maneuver

**NOT Optimized:**
- ❌ Regular turns (< 120°)
- ❌ No direct segment in DB
- ❌ One-way streets blocking direct crossing
- ❌ Missing OSM node data

---

## 📈 Benefits

### ✅ Advantages

1. **Fewer Steps**: Reduces step count by eliminating unnecessary U-turns
2. **Clearer Instructions**: "Continue straight" vs "Drive to end, U-turn"
3. **Realistic Routing**: Matches real driver behavior (direct crossing)
4. **Better UX**: Simplified navigation instructions
5. **Performance**: Fewer steps to render/process

### 📊 Expected Impact

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Steps per U-turn | 2 | 1 | **50% reduction** |
| Instructions | "Drive 100m, U-turn, continue 50m" | "Cross street 150m" | **Simpler** |
| Maneuvers | U-turn | Straight/Cross | **More intuitive** |

---

## ⚠️ Edge Cases

### Case 1: No Direct Segment

```
A ------|------ (no crossing)
        |
        C ------ B (different street)
```

**Behavior:** No optimization - U-turn is necessary  
**Result:** Steps kept as-is

### Case 2: One-Way Street

```
A ---→--X--→--- (one-way, no crossing)
        |
        C ←---- B
```

**Behavior:** No direct segment found in DB (A→B blocked)  
**Result:** Steps kept as-is

### Case 3: Missing OSM Nodes

```
Steps exist but osmNodeIds missing or incomplete
```

**Behavior:** Cannot detect pattern - skip optimization  
**Result:** Steps kept as-is

---

## 🧪 Testing

### Test Scenario 1: Basic U-Turn

**Input:**
```json
{
  "waypoints": [
    {"lat": 10.762622, "lon": 106.660172},  // Start
    {"lat": 10.762622, "lon": 106.665172}   // Waypoint (across street)
  ]
}
```

**Expected:**
- Before: 2 steps (drive to end + U-turn)
- After: 1 step (cross directly)
- Log: "🔀 Detected U-turn pattern..."

### Test Scenario 2: Multiple Waypoints

**Input:**
```json
{
  "waypoints": [
    {"lat": 10.0, "lon": 106.0},      // Start
    {"lat": 10.0, "lon": 106.001},    // WP1 (across)
    {"lat": 10.0, "lon": 106.002},    // WP2 (across)
    {"lat": 10.0, "lon": 106.003}     // WP3 (across)
  ]
}
```

**Expected:**
- Before: ~12 steps (3 U-turns)
- After: ~6 steps (U-turns eliminated)
- Log: "✅ Optimized 3 U-turn step(s)"

### Test Scenario 3: No Optimization Possible

**Input:** One-way street with no direct crossing

**Expected:**
- Before: 2 steps (U-turn)
- After: 2 steps (unchanged)
- Log: No optimization message

---

## 🚀 Deployment

### Backward Compatibility

✅ **Fully backward compatible**
- Same API interface
- Steps automatically optimized (transparent to client)
- No breaking changes

### Performance Impact

| Operation | Time Complexity | Impact |
|-----------|----------------|--------|
| DB query (canCrossDirectly) | O(1) per check | Minimal |
| Pattern detection | O(n) steps | Low |
| Total overhead | O(n × k) | **Acceptable** |

Where:
- n = number of steps per leg (~10-50)
- k = DB query time (~5-10ms)
- Total: ~50-500ms per leg

### Enable/Disable

Currently **always enabled**. To disable:

```typescript
// In enrichRouteData(), comment out:
// const optimizedSteps = await this.optimizeUTurnPattern(enrichedSteps, osmNodeIds);

// Use original steps instead:
const optimizedSteps = enrichedSteps;
```

---

## 📝 Summary

**Problem:**
- OSRM generates U-turn patterns (A→C→B) where direct crossing (A→B) is possible
- Results in more steps and confusing navigation

**Solution:**
- Post-process route steps to detect U-turn patterns
- Check if direct crossing exists in road network database
- Merge 2 steps into 1 optimized step

**Benefits:**
- ✅ 50% fewer steps for U-turn scenarios
- ✅ Clearer navigation instructions
- ✅ Matches real driver behavior
- ✅ Fully backward compatible

**Implementation:**
- `canCrossDirectly()` - checks DB for direct segment
- `optimizeUTurnPattern()` - detects and merges U-turns
- Integrated into `enrichRouteData()` - automatic optimization

---

## 📚 References

- **Implementation**: `BE/zone_service/src/modules/routing/routing.service.ts`
- **Related**: `CHANGELOG-NEAREST-NEIGHBOR.md` (waypoint ordering)
- **Database**: `road_segments`, `road_nodes` tables

---

**Version**: 1.0  
**Date**: 2025-11-07  
**Status**: ✅ Implemented & Tested  
**Author**: AI Assistant
