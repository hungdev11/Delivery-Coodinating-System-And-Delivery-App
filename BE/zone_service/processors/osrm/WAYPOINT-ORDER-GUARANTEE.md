# OSRM Waypoint Order Guarantee

## ✅ **Đảm bảo: OSRM GIỮ NGUYÊN thứ tự waypoints**

### 📊 **OSRM có 2 API khác nhau**

| API | Endpoint | Hành vi | Use Case |
|-----|----------|---------|----------|
| **Route API** | `/route/v1/{profile}/{coordinates}` | ✅ **Đi theo thứ tự đã cho** | Routing với thứ tự cố định |
| **Trip API** | `/trip/v1/{profile}/{coordinates}` | ❌ **Tự động reorder (TSP)** | Tối ưu hóa tour (Traveling Salesman) |

### ✅ **Code hiện tại đang dùng Route API**

```typescript
// BE/zone_service/src/services/osrm/osrm-router.service.ts
const path = `/route/v1/${profile}/${coordinates}?${queryString}`;
```

**Kết quả:** OSRM sẽ routing qua các waypoints **ĐÚNG THEO THỨ TỰ** bạn truyền vào.

---

## 🔧 **Cách hệ thống đảm bảo thứ tự**

### 1. **Application Layer (routing.service.ts) - Sort theo Priority**

```typescript
// Step 1: Calculate effective cost với exponential weighting
const effectiveDuration = duration / Math.exp((priority - 5) / 3);

// Step 2: Sort waypoints theo effective cost
orderedWaypoints.sort((a, b) => a.effectiveDuration - b.effectiveDuration);

// Step 3: Build ordered array
const orderedWaypointDtos = [
  request.startPoint,
  ...orderedWaypoints.map(w => w.waypoint)
];

// Step 4: Log để verify
logger.info(`Final waypoint order for OSRM (will NOT be reordered):`);
orderedWaypoints.forEach((w, idx) => {
  logger.info(`  ${idx + 1}. P${w.priority} [${w.waypoint.lat}, ${w.waypoint.lon}]`);
});
```

### 2. **OSRM Layer - Giữ nguyên thứ tự**

```typescript
// Call OSRM với continue_straight=true
const osrmResponse = await this.osrmRouter.getRoute(orderedWaypointDtos, {
  continue_straight: true,  // ✅ Không skip waypoints
  // ... other options
});
```

**OSRM sẽ:**
- Routing từ waypoint[0] → waypoint[1] → waypoint[2] → ... → waypoint[n]
- **KHÔNG** thay đổi thứ tự
- **KHÔNG** skip waypoints
- **KHÔNG** tối ưu hóa TSP

### 3. **Verification - Kiểm tra sau khi nhận response**

```typescript
// Verify số legs khớp với số waypoints
const expectedLegs = orderedWaypointDtos.length - 1; // n waypoints = n-1 legs
const actualLegs = route.legs.length;

if (actualLegs !== expectedLegs) {
  logger.warn(`⚠️ OSRM leg count mismatch!`);
  // Có thể OSRM đã reorder hoặc skip waypoints
} else {
  logger.info(`✅ OSRM route has ${actualLegs} legs as expected`);
}
```

---

## 📝 **Logs để debug**

### Khi calculate route, bạn sẽ thấy logs:

```
Applied priority weighting: [P10:30.0min→6.7min] [P8:20.0min→6.7min] [P5:15.0min→15.0min]
Strategy: FLEXIBLE - all waypoints sorted by effective duration (priority-weighted)

Final waypoint order for OSRM (will NOT be reordered):
  Start: [10.762622, 106.660172]
  1. P10 [10.762622, 106.670172] PARCEL-ABC123
  2. P8 [10.772622, 106.680172] PARCEL-DEF456
  3. P5 [10.782622, 106.690172] PARCEL-GHI789

Fetching OSRM table for distance/duration matrix (mode: flexible_priority_with_delta)...
✅ OSRM route has 3 legs as expected (waypoints were NOT reordered)
```

**Nếu thấy warning:**
```
⚠️ OSRM leg count mismatch! Expected 3 legs, got 2
This might indicate OSRM reordered waypoints or skipped some.
```
→ Cần kiểm tra lại cấu hình OSRM hoặc waypoint data

---

## 🔍 **Kiểm tra thực tế**

### Test Case 1: Priority Order

**Input:**
```json
{
  "startPoint": {"lat": 10.762622, "lon": 106.660172},
  "priorityGroups": [
    {"priority": 10, "waypoints": [{"lat": 10.762622, "lon": 106.670172}]},
    {"priority": 5, "waypoints": [{"lat": 10.772622, "lon": 106.680172}]},
    {"priority": 1, "waypoints": [{"lat": 10.782622, "lon": 106.690172}]}
  ]
}
```

**Expected Order (after sorting):**
```
Start → P10 → P5 → P1
```

**Verify in Response:**
```json
{
  "route": {
    "legs": [
      {"distance": 1000, "duration": 120},  // Start → P10
      {"distance": 1500, "duration": 180},  // P10 → P5
      {"distance": 2000, "duration": 240}   // P5 → P1
    ]
  },
  "visitOrder": [
    {"index": 0, "priority": 10},
    {"index": 1, "priority": 5},
    {"index": 2, "priority": 1}
  ]
}
```

✅ **3 legs = 3 waypoints visited in order**

### Test Case 2: Strict Urgent

**Input:**
```json
{
  "startPoint": {"lat": 10.762622, "lon": 106.660172},
  "priorityGroups": [
    {"priority": 5, "waypoints": [{"lat": 10.762622, "lon": 106.665172}]},  // Gần
    {"priority": 10, "waypoints": [{"lat": 10.762622, "lon": 106.690172}]}, // Xa
    {"priority": 2, "waypoints": [{"lat": 10.762622, "lon": 106.667172}]}   // Gần
  ],
  "strategy": "strict_urgent"
}
```

**Expected Order:**
```
Start → P10 (xa nhưng URGENT) → P5 (gần) → P2 (gần)
```

**Verify:** URGENT phải đầu tiên bất kể khoảng cách

---

## ⚠️ **Lưu ý quan trọng**

### 1. **KHÔNG dùng `/trip` API**

```typescript
// ❌ KHÔNG dùng - sẽ reorder waypoints
const tripResponse = await axios.get(`/trip/v1/motorbike/${coordinates}`);

// ✅ Dùng - giữ nguyên thứ tự
const routeResponse = await axios.get(`/route/v1/motorbike/${coordinates}`);
```

### 2. **Luôn set `continue_straight=true`**

```typescript
const params = {
  continue_straight: true,  // ✅ Bắt buộc
  // ...
};
```

Nếu `continue_straight=false`, OSRM có thể skip waypoints nếu chúng nằm trên đường thẳng.

### 3. **Verify số legs**

```typescript
// n waypoints → n-1 legs
if (route.legs.length !== waypoints.length - 1) {
  // ⚠️ Có vấn đề!
}
```

### 4. **Không confuse với alternatives**

```typescript
// Alternatives = multiple routes, KHÔNG phải reordering waypoints
const params = {
  alternatives: true,  // Trả về nhiều routes khác nhau
  // Nhưng TẤT CẢ routes đều đi theo thứ tự waypoints giống nhau
};
```

---

## 📚 **References**

- **OSRM Route API**: https://project-osrm.org/docs/v5.24.0/api/#route-service
- **OSRM Trip API**: https://project-osrm.org/docs/v5.24.0/api/#trip-service
- **Implementation**: `BE/zone_service/src/services/osrm/osrm-router.service.ts`
- **Priority System**: `BE/zone_service/processors/osrm/PRIORITY-SYSTEM.md`

---

## ✅ **Kết luận**

**Hệ thống hiện tại ĐẢM BẢO:**

1. ✅ Application layer sort waypoints theo priority (exponential weighting)
2. ✅ OSRM giữ nguyên thứ tự (dùng `/route` API, không phải `/trip`)
3. ✅ Verification sau khi nhận response (kiểm tra số legs)
4. ✅ Logging đầy đủ để debug

**→ Thứ tự waypoints SAU KHI SORT sẽ được GIỮ NGUYÊN hoàn toàn!**

---

**Version**: 1.0  
**Date**: 2025-11-07  
**Status**: ✅ Verified & Documented
