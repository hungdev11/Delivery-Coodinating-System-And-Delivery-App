# Priority System Documentation (1-10 Scale)

## 📊 Overview

The new priority system uses a **1-10 scale** where **higher numbers = higher priority**. This replaces the legacy 0-4 scale and provides more granular control over delivery order.

## 🎯 Priority Levels

### Scale Definition

| Priority | Range | Label | Description | Use Case |
|----------|-------|-------|-------------|----------|
| **10** | - | 🚨 URGENT | Khẩn cấp tuyệt đối - giao ngay | Medical, emergency |
| **9** | High | 🔥 EXPRESS HIGH | Express cao nhất | VIP customers |
| **8** | Express | 🔥 EXPRESS | Express tiêu chuẩn | Same-day delivery |
| **7** | Express | 🔥 EXPRESS STANDARD | Express cơ bản | Fast delivery |
| **6** | Normal | 📦 NORMAL HIGH | Normal cao | Priority normal |
| **5** | Normal | 📦 NORMAL | Normal trung bình | Standard delivery |
| **4** | Normal | 📦 NORMAL STANDARD | Normal tiêu chuẩn | Regular orders |
| **3** | Economy | 💰 ECONOMY HIGH | Economy cao | Budget conscious |
| **2** | Economy | 💰 ECONOMY | Economy tiêu chuẩn | Low-cost delivery |
| **1** | Low | 🐢 LOW | Thấp nhất | Non-urgent, bulk |

## 🧮 Priority Weight Formula

### Effective Cost Calculation (EXPONENTIAL)

```typescript
effective_cost = actual_duration ÷ (e^((priority - 5) / 3))
```

**Why Exponential?** 
- Linear scaling (`priority / 5`) doesn't guarantee URGENT always first
- Exponential scaling creates **much larger gaps** between priority levels
- Result: Even if URGENT is **4x farther**, it still beats LOW priority

### Examples

| Priority | Weight (e^((p-5)/3)) | Multiplier | Effect on Cost |
|----------|---------------------|------------|----------------|
| **10** | 4.48 | ÷ 4.48 | **22% cost** ⭐ (extremely high) |
| **9** | 3.67 | ÷ 3.67 | **27% cost** |
| **8** | 3.00 | ÷ 3.00 | **33% cost** |
| **7** | 2.46 | ÷ 2.46 | **41% cost** |
| **6** | 2.01 | ÷ 2.01 | **50% cost** |
| **5** | 1.00 | ÷ 1.00 | **100% cost** (neutral) |
| **4** | 0.72 | ÷ 0.72 | **139% cost** |
| **3** | 0.51 | ÷ 0.51 | **196% cost** |
| **2** | 0.37 | ÷ 0.37 | **270% cost** |
| **1** | 0.26 | ÷ 0.26 | **385% cost** ⬇️ (lowest) |

### Gap Analysis

**Linear vs Exponential:**

| Scenario | Linear (÷ priority/5) | Exponential (÷ e^((p-5)/3)) |
|----------|---------------------|----------------------------|
| P10 vs P1 | 10x difference | **17x difference** ✅ |
| P10 vs P5 | 2x difference | **4.5x difference** ✅ |
| P8 vs P4 | 2x difference | **4.2x difference** ✅ |

### Visual Impact (20 min actual distance)

```
Priority 10 (URGENT):   ██████ (20 min → 4.5 min effective)
Priority 8 (EXPRESS):   ████████ (20 min → 6.7 min)
Priority 5 (NORMAL):    ████████████████████ (20 min → 20 min)
Priority 2 (ECONOMY):   ██████████████████████████████████████████████████ (20 min → 54 min)
Priority 1 (LOW):       █████████████████████████████████████████████████████████████████████ (20 min → 77 min)
```

### Real-World Example

```
Scenario: P10 far away vs P1 nearby

P10 URGENT at 40 km (60 min) → effective = 60 ÷ 4.48 = 13.4 min
P1 LOW at 10 km (15 min)     → effective = 15 ÷ 0.26 = 57.7 min

Result: P10 wins even though 4x farther! ✅
```

## 🚀 Routing Strategies

### 🔄 **Algorithm: Nearest Neighbor with Priority Weighting**

**All strategies use Nearest Neighbor algorithm instead of simple sorting:**

```
Algorithm:
1. Start at current position (initially: start point)
2. Calculate effective_cost = distance_to_waypoint / priority_weight for all remaining waypoints
3. Select waypoint with lowest effective cost
4. Move to selected waypoint (update current position)
5. Repeat until all waypoints visited
```

**Why Nearest Neighbor?**
- ✅ **Considers "convenience"**: If a waypoint is "on the way", it gets picked
- ✅ **Reduces total travel time**: Avoids backtracking
- ✅ **Still respects priority**: Through effective cost calculation
- ✅ **Better than simple sorting**: Sorting only considers distance from start, not from current position

**Visual Example:**
```
Scenario: Start → P2 (nearby) → P1 (far)
         But P1 is "on the way" from Start to P2

Old (Simple Sort):
  - P2: 5km from start → effective: 13.5 min (5 ÷ 0.37)
  - P1: 20km from start → effective: 77 min (20 ÷ 0.26)
  Result: P2 → P1 (then backtrack)

New (Nearest Neighbor):
  - From Start: P1 closer via effective cost → Pick P1
  - From P1: P2 is now very close → Pick P2
  Result: P1 → P2 (no backtracking!) ✅
```

---

### 1. **Strict Urgent** (Default)

```
Strategy: URGENT (P≥9) MUST be visited first, regardless of location
```

**Algorithm:**
1. **Urgent waypoints (P≥9)**: 
   - Use Nearest Neighbor with priority first, then distance
   - Score = (-priority × 1000) + distance_from_current
   - Visit all urgent points first

2. **Other waypoints (P<9)**:
   - Use Nearest Neighbor with priority weighting
   - effective_cost = distance_from_current / e^((priority-5)/3)
   - Start from last urgent waypoint position

**Example:**
```
Input:  Start@(0,0) → P10@(5,0), P5@(1,0), P9@(6,0), P2@(3,0)

Step 1: Order URGENT (P≥9) using Nearest Neighbor:
  - From Start: P10@5km (score: -10000+5 = -9995) vs P9@6km (-9000+6 = -8994)
  - Pick P10 first (higher priority)
  - From P10@(5,0): P9@1km away → Pick P9
  Result: [P10, P9]

Step 2: Order OTHERS (P<9) using Nearest Neighbor:
  - From P9@(6,0): P5@5km (eff: 5÷1.0=5) vs P2@3km (eff: 3÷0.37=8.1)
  - Pick P5 (lower effective cost)
  - From P5@(1,0): P2@2km away → Pick P2
  Result: [P5, P2]

Output: P10 → P9 → P5 → P2
```

### 2. **Flexible**

```
Strategy: Use priority weighting for ALL waypoints with Nearest Neighbor
```

**Algorithm:**
- Use Nearest Neighbor with priority weighting for ALL waypoints
- At each step: find waypoint with lowest effective_cost from current position
- effective_cost = distance_from_current / e^((priority-5)/3)
- Naturally prioritizes urgent but allows convenient detours

**Example:**
```
Input:  Start@(0,0) → P10@(10,0), P5@(2,0), P9@(12,0), P2@(6,0)

Step 1: From Start@(0,0):
  - P10: 10km → eff: 10÷4.48 = 2.2 min ✅ (best!)
  - P5: 2km → eff: 2÷1.0 = 2.0 min
  - P9: 12km → eff: 12÷3.67 = 3.3 min
  - P2: 6km → eff: 6÷0.37 = 16.2 min
  Pick: P5 (lowest effective cost)

Step 2: From P5@(2,0):
  - P10: 8km → eff: 8÷4.48 = 1.8 min ✅ (best!)
  - P9: 10km → eff: 10÷3.67 = 2.7 min
  - P2: 4km → eff: 4÷0.37 = 10.8 min
  Pick: P10

Step 3: From P10@(10,0):
  - P9: 2km → eff: 2÷3.67 = 0.5 min ✅
  - P2: 4km → eff: 4÷0.37 = 10.8 min
  Pick: P9

Step 4: From P9@(12,0):
  - P2: 6km → eff: 6÷0.37 = 16.2 min
  Pick: P2

Output: P5 → P10 → P9 → P2
```

**Key Benefit:** If P2 was "on the way" (e.g., at position (7,0)), it would be picked earlier despite lower priority!

## 🔄 Legacy Compatibility

The system **automatically converts** legacy priority (0-4) to new scale (1-10):

| Legacy | New | Label |
|--------|-----|-------|
| 0 (URGENT) | → 10 | 🚨 URGENT |
| 1 (EXPRESS) | → 8 | 🔥 EXPRESS |
| 2 (FAST) | → 6 | ⚡ FAST |
| 3 (NORMAL) | → 4 | 📦 NORMAL |
| 4 (ECONOMY) | → 2 | 💰 ECONOMY |

## 💻 Implementation Details

### Backend (`routing.service.ts`)

```typescript
// Helper function to calculate priority weight (exponential)
const calculatePriorityWeight = (priority: number): number => {
  return Math.exp((priority - 5) / 3);
};

// Helper function to calculate effective cost
const calculateEffectiveCost = (actualDuration: number, priority: number): number => {
  const weight = calculatePriorityWeight(priority);
  return actualDuration / weight;
};

// Nearest Neighbor algorithm
const remaining = [...allWaypoints];
let currentPos = 0; // Start point index in matrix

while (remaining.length > 0) {
  let bestIdx = 0;
  let bestEffectiveCost = Infinity;

  // Find best next waypoint from current position
  for (let i = 0; i < remaining.length; i++) {
    const waypointIdx = allWaypoints.indexOf(remaining[i]) + 1;
    const duration = matrix.durations[currentPos]?.[waypointIdx] ?? Infinity;
    const effectiveCost = calculateEffectiveCost(duration, remaining[i].priority);
    
    if (effectiveCost < bestEffectiveCost) {
      bestEffectiveCost = effectiveCost;
      bestIdx = i;
    }
  }

  // Select best waypoint and update current position
  const selected = remaining.splice(bestIdx, 1)[0];
  orderedWaypoints.push(selected);
  currentPos = allWaypoints.indexOf(selected) + 1;
}
```

### Frontend (`DemoRoutingView.vue`)

```vue
<USelect 
  v-model="selectedPriority" 
  :items="priorityOptions" 
/>
```

Priority options: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10

## 📊 Logging & Debugging

The system logs priority weighting for debugging:

```
Applied priority weighting: [P10:30.0min→15.0min] [P5:10.0min→10.0min] [P9:40.0min→22.2min]
```

Format: `[P{priority}:{actual_duration}min→{effective_duration}min]`

## 🎨 UI Features

### Priority Selector

- **New Scale (1-10)**: Default, recommended
- **Legacy Scale (0-4)**: Toggle for backward compatibility
- Help text explains the conversion

### Toggle Between Scales

```vue
<UToggle v-model="useLegacyPriority" />
```

- OFF: Use 1-10 scale (recommended)
- ON: Use 0-4 scale (auto-converted to 1-10)

## 📝 Best Practices

### Choosing Priority Levels

1. **Priority 10 (URGENT)**: Reserve for true emergencies
   - Medical deliveries
   - Critical business needs
   - VIP customer requests

2. **Priority 7-9 (EXPRESS)**: Time-sensitive deliveries
   - Same-day delivery
   - Hot food delivery
   - Flash sales

3. **Priority 4-6 (NORMAL)**: Standard deliveries
   - Regular e-commerce orders
   - Standard shipping
   - Most common use case

4. **Priority 2-3 (ECONOMY)**: Budget deliveries
   - Bulk orders
   - Non-urgent items
   - Cost-optimized routes

5. **Priority 1 (LOW)**: Lowest priority
   - Return pickups
   - Flexible delivery windows
   - Storage relocations

### Strategy Selection

**Use STRICT_URGENT when:**
- You have true emergency orders (P≥9)
- Urgent orders MUST be delivered first
- Detours are acceptable for urgent orders

**Use FLEXIBLE when:**
- All orders are similar priority
- Want to minimize total travel time
- Cost efficiency is priority

## 🔍 Testing

### Test Scenarios

1. **All Same Priority**
   - Expected: Sorted by distance/duration

2. **Mixed Priorities**
   - Expected: Higher priority visited earlier

3. **Urgent + Normal**
   - Strict: All urgent first
   - Flexible: Weighted by cost

4. **Edge Cases**
   - Priority 10 far away vs Priority 1 nearby
   - Multiple urgent orders
   - Single priority level

## 📚 References

- **Backend Implementation**: `BE/zone_service/src/modules/routing/routing.service.ts`
- **Frontend UI**: `ManagementSystem/src/modules/Zones/DemoRoutingView.vue`
- **Type Definitions**: `ManagementSystem/src/modules/Zones/routing.type.ts`

---

**Version**: 2.0  
**Date**: 2025-11-07  
**Status**: ✅ Active
