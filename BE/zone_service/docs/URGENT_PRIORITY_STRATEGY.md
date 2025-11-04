# URGENT Priority Strategy

## Overview

Hệ thống hỗ trợ 2 chiến lược xử lý đơn hàng **URGENT** (gấp tuyệt đối):

1. **Strict Urgent**: URGENT orders MUST be delivered first (phải giao đầu tiên)
2. **Flexible**: URGENT gets very high weight but can be optimized with other orders

## Priority Levels

Hệ thống có **5 mức độ ưu tiên**:

```typescript
export const PriorityLevel = {
  URGENT: 0,     // 🚨 Gấp tuyệt đối - phải giao đầu tiên
  EXPRESS: 1,    // 🔥 Đơn hàng gấp
  FAST: 2,       // ⚡ Giao nhanh
  NORMAL: 3,     // 📦 Đơn bình thường
  ECONOMY: 4,    // 💰 Ưu tiên giá (có thể giao sau)
}
```

### Mapping với Yêu cầu

| Yêu cầu của User | Priority Level | Mô tả |
|------------------|----------------|-------|
| Đơn hàng gấp (tuyệt đối) | `URGENT` (0) | Phải giao trước tiên, không được bỏ qua |
| Đơn hàng gấp | `EXPRESS` (1) | Ưu tiên cao, cố gắng giao sớm |
| Đơn giao nhanh | `FAST` (2) | Ưu tiên tốc độ |
| Đơn bình thường | `NORMAL` (3) | Mức độ ưu tiên tiêu chuẩn |
| Đơn ưu tiên giá | `ECONOMY` (4) | Có thể giao sau, tiết kiệm chi phí |

## Strategy 1: Strict Urgent (Recommended)

### Behavior

- **URGENT orders** được giao **trước tiên**, bất kể detour cost
- Sequencing algorithm sẽ:
  1. **Phase 1**: Visit all URGENT waypoints first (in optimal order among themselves)
  2. **Phase 2**: Visit remaining waypoints (EXPRESS → FAST → NORMAL → ECONOMY)

### Implementation Logic

```typescript
// Pseudocode
function sequenceWithStrictUrgent(waypoints) {
  const urgentWaypoints = waypoints.filter(w => w.priority === 0);
  const otherWaypoints = waypoints.filter(w => w.priority > 0);
  
  // Step 1: Sequence URGENT waypoints only
  const urgentRoute = greedySequencing(urgentWaypoints, lambda=1.0);
  
  // Step 2: Sequence other waypoints
  const otherRoute = greedySequencing(otherWaypoints, lambda);
  
  // Combine: URGENT first, then others
  return [...urgentRoute, ...otherRoute];
}
```

### Use Cases

- **Đơn khẩn cấp y tế**: Thuốc, máu, vaccine
- **Đơn có deadline cứng**: Hợp đồng, tài liệu pháp lý
- **VIP customers**: Khách hàng đặc biệt, không được chậm trễ

### Example

**Input**:
- Start: A
- URGENT: [B, C]
- EXPRESS: [D, E]
- NORMAL: [F]

**Output Route**:
```
A → B → C → D → E → F
     ↑___↑   (URGENT phải trước)
```

Ngay cả khi D nằm giữa A và B, vẫn phải giao B, C trước.

## Strategy 2: Flexible (Cost-Optimized)

### Behavior

- **URGENT** được xử lý như priority rất cao (weight multiplier = 10x)
- Nhưng vẫn có thể optimize với các waypoint khác nếu hợp lý
- Sử dụng lambda-greedy với penalty lớn cho việc bỏ qua URGENT

### Implementation Logic

```typescript
// Pseudocode
function sequenceWithFlexible(waypoints, lambda) {
  // URGENT gets 10x priority weight
  const adjustedWaypoints = waypoints.map(w => ({
    ...w,
    adjustedPriority: w.priority === 0 ? -10 : w.priority
  }));
  
  // Normal sequencing with adjusted priorities
  return greedySequencing(adjustedWaypoints, lambda);
}
```

### Use Cases

- **Đơn hàng nhiều**: Khi có nhiều URGENT gần nhau nhưng cách xa
- **Tối ưu chi phí**: Khi cost là quan trọng
- **Flexible deadline**: URGENT nhưng có thể chấp nhận delay 5-10 phút

### Example

**Input**:
- Start: A
- URGENT: [B (far away), C (nearby)]
- EXPRESS: [D (very close)]

**Output Route** (có thể):
```
A → D → C → B
    ↑   ↑___↑
   (EXPRESS gần → URGENT C → URGENT B xa)
```

Nếu D nằm ngay cạnh A, và B rất xa, có thể giao D trước để giảm total cost.

## Comparison

| Aspect | Strict Urgent | Flexible |
|--------|---------------|----------|
| **URGENT Priority** | Absolute (100%) | Very High (90-95%) |
| **Route Cost** | Higher | Lower (optimized) |
| **Guarantee** | URGENT always first | URGENT very likely first |
| **Use Case** | Mission-critical | Cost-sensitive |
| **Flexibility** | Low | High |

## API Request Format

```typescript
interface DemoRouteRequest {
  startPoint: Waypoint;
  priorityGroups: PriorityGroup[];  // Includes URGENT group
  mode: 'priority_first' | 'speed_leaning' | 'balanced' | 'no_recommend' | 'base';
  strategy: 'strict_urgent' | 'flexible';  // 🚨 NEW
}
```

### Example Request

```json
{
  "startPoint": { "lat": 10.762622, "lon": 106.660172 },
  "priorityGroups": [
    {
      "priority": 0,
      "waypoints": [
        { "lat": 10.782622, "lon": 106.680172 }
      ]
    },
    {
      "priority": 1,
      "waypoints": [
        { "lat": 10.772622, "lon": 106.670172 }
      ]
    },
    {
      "priority": 3,
      "waypoints": [
        { "lat": 10.752622, "lon": 106.650172 }
      ]
    }
  ],
  "mode": "balanced",
  "strategy": "strict_urgent",
  "steps": true,
  "annotations": true
}
```

## Frontend Integration

### UI Selector

```vue
<UCard>
  <template #header>
    <h3 class="text-lg font-semibold">🚨 URGENT Strategy</h3>
  </template>

  <URadioGroup 
    v-model="routingStrategy" 
    :items="[
      { 
        label: '🚨 Strict Urgent (URGENT phải giao đầu tiên)', 
        value: 'strict_urgent' 
      },
      { 
        label: '🎯 Flexible (cân nhắc tất cả priority)', 
        value: 'flexible' 
      }
    ]" 
  />
</UCard>
```

### Priority Selector

```vue
<UFormField label="Priority Level">
  <USelect v-model="selectedPriority" :items="[
    { label: '🚨 Urgent (Gấp tuyệt đối)', value: 0 },
    { label: '🔥 Express (Đơn hàng gấp)', value: 1 },
    { label: '⚡ Fast (Giao nhanh)', value: 2 },
    { label: '📦 Normal (Bình thường)', value: 3 },
    { label: '💰 Economy (Ưu tiên giá)', value: 4 }
  ]" />
</UFormField>
```

## Backend Implementation

### Sequencing Service

```typescript
// In sequencing-service.ts
export async function sequenceWaypoints(
  start: Waypoint,
  priorityGroups: PriorityGroup[],
  mode: RoutingMode,
  strategy: 'strict_urgent' | 'flexible' = 'strict_urgent'
): Promise<Waypoint[]> {
  
  if (strategy === 'strict_urgent') {
    // Separate URGENT from others
    const urgentGroup = priorityGroups.find(g => g.priority === 0);
    const otherGroups = priorityGroups.filter(g => g.priority > 0);
    
    // Sequence URGENT first
    const urgentSequence = await lambdaGreedy(
      start, 
      urgentGroup?.waypoints || [], 
      1.0  // High lambda for URGENT
    );
    
    // Sequence others
    const lastUrgent = urgentSequence[urgentSequence.length - 1] || start;
    const otherSequence = await lambdaGreedy(
      lastUrgent, 
      flattenGroups(otherGroups),
      getLambda(mode)
    );
    
    return [...urgentSequence, ...otherSequence];
  } else {
    // Flexible: treat all together with adjusted priorities
    const allWaypoints = flattenGroups(priorityGroups).map(w => ({
      ...w,
      adjustedPriority: w.priority === 0 ? -10 : w.priority
    }));
    
    return await lambdaGreedy(start, allWaypoints, getLambda(mode));
  }
}
```

## Testing Scenarios

### Scenario 1: Multiple URGENT Orders

**Setup**:
- Start: Depot (10.762622, 106.660172)
- URGENT: [Hospital (10.772, 106.680), VIP (10.782, 106.670)]
- EXPRESS: [Store (10.765, 106.665)]
- NORMAL: [Home (10.760, 106.655)]

**Expected (Strict Urgent)**:
```
Depot → Hospital → VIP → Store → Home
```

**Expected (Flexible)**:
```
Depot → Store → Hospital → VIP → Home
```
(Store có thể được giao trước nếu rất gần)

### Scenario 2: URGENT Far Away

**Setup**:
- Start: Depot (10.762, 106.660)
- URGENT: [Remote (10.850, 106.750)] (20km away)
- EXPRESS: [Near1 (10.763, 106.661), Near2 (10.764, 106.662)]

**Expected (Strict Urgent)**:
```
Depot → Remote (20km) → Near1 → Near2
```
(Phải giao URGENT trước, dù xa)

**Expected (Flexible)**:
```
Depot → Near1 → Near2 → Remote
```
(Có thể giao Near1, Near2 trước để tối ưu)

### Scenario 3: No URGENT Orders

Cả 2 strategies đều cho kết quả giống nhau (không có URGENT để phân biệt).

## Performance Impact

| Strategy | Route Distance | Route Time | Computation Time |
|----------|---------------|------------|------------------|
| Strict Urgent | +15-30% | +20-40% | Fast (2-phase) |
| Flexible | +0-10% | +0-15% | Medium (single-phase) |

**Note**: Strict Urgent có thể tăng distance/time nhưng đảm bảo SLA cho URGENT orders.

## Recommendations

### Use Strict Urgent when:
- ✅ URGENT orders are mission-critical
- ✅ SLA violations are very costly
- ✅ Customer satisfaction > cost optimization
- ✅ Small number of URGENT orders (<20% of total)

### Use Flexible when:
- ✅ URGENT orders have some flexibility (5-10 min buffer)
- ✅ Cost optimization is important
- ✅ Large number of URGENT orders (>30% of total)
- ✅ URGENT orders are geographically spread out

## Future Enhancements

1. **Time Windows**: Add hard time windows for URGENT orders
2. **Dynamic Strategy**: Auto-switch strategy based on order distribution
3. **Partial Strict**: Some URGENT are strict, others are flexible
4. **Multi-Vehicle**: Assign dedicated vehicle for URGENT clusters
5. **Real-time Adjustment**: Re-sequence when new URGENT orders arrive

## References

- [Sequencing Service Implementation](../services/sequencing-service.ts)
- [5 OSRM Instances Setup](./5_OSRM_INSTANCES_SETUP.md)
- [Implementation Summary](../IMPLEMENTATION_SUMMARY.md)
