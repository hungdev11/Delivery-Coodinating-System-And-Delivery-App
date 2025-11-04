# Demo Routing Page

## Overview

The Demo Routing page is a testing interface for priority-based routing functionality. It allows you to:
- Set a starting point
- Add multiple destination waypoints grouped by priority levels
- Calculate and visualize an optimized route that visits higher priority destinations first

## Location

The page is accessible at: `/zones/map/demo-routing`

## Features

### Priority Levels

The system supports four priority levels:
1. **Express** (Priority 1) - Highest priority, visited first
2. **Fast** (Priority 2) - High priority
3. **Normal** (Priority 3) - Medium priority
4. **Economy** (Priority 4) - Lowest priority

### How to Use

1. **Select Click Mode**
   - Choose "Set Start Point" to place your starting location
   - Choose "Add Waypoint" to place destination points

2. **Set Start Point**
   - Select "Set Start Point" mode
   - Click anywhere on the map to set the starting location
   - The start point will appear as a green marker labeled "Start"

3. **Add Waypoints**
   - Select "Add Waypoint" mode
   - Choose a priority level (Express, Fast, Normal, or Economy)
   - Click on the map to add waypoints at that priority level
   - Each waypoint will appear with a color-coded marker based on its priority

4. **Calculate Route**
   - Once you have a start point and at least one waypoint, click "Calculate Route"
   - The system will:
     - Sort waypoints by priority (higher priority first)
     - Calculate an optimized route
     - Display the route on the map as a blue line
     - Show route summary with distance, duration, and traffic information

5. **View Route Summary**
   - Total distance and duration
   - Average speed
   - Traffic congestion level
   - Priority breakdown showing count per priority level

### Controls

- **Reset** - Clear all points and start over
- **Zones Map** - Navigate to the zones management map
- **X button** - Remove all waypoints from a specific priority group

## API Endpoint

The demo routing page uses the following API endpoint:

```
POST /v1/routing/demo-route
```

**Request Body:**
```json
{
  "startPoint": {
    "lat": 10.762622,
    "lon": 106.660172
  },
  "priorityGroups": [
    {
      "priority": 1,
      "waypoints": [
        { "lat": 10.773162, "lon": 106.657490 },
        { "lat": 10.771192, "lon": 106.661320 }
      ]
    },
    {
      "priority": 2,
      "waypoints": [
        { "lat": 10.765432, "lon": 106.654321 }
      ]
    }
  ],
  "steps": true,
  "annotations": true
}
```

**Response:**
```json
{
  "result": {
    "code": "Ok",
    "route": {
      "distance": 5432.1,
      "duration": 789.5,
      "geometry": "...",
      "legs": [...],
      "trafficSummary": {
        "averageSpeed": 35.2,
        "congestionLevel": "NORMAL",
        "estimatedDelay": 45
      }
    },
    "visitOrder": [...],
    "summary": {
      "totalDistance": 5432.1,
      "totalDuration": 789.5,
      "totalWaypoints": 3,
      "priorityCounts": {
        "express": 2,
        "fast": 1
      }
    }
  }
}
```

## Technical Details

### State Management

The page uses Pinia store (`useRoutingStore`) for managing:
- Start point
- Priority groups with waypoints
- Route calculation results
- Loading and error states

### Map Integration

- Interactive MapLibre GL JS map
- Click-to-add waypoints functionality
- Route visualization with GeoJSON
- Color-coded markers for different priority levels

### Utilities

Helper functions available in `utils/routingHelper.ts`:
- `formatDistance()` - Format meters to human-readable distance
- `formatDuration()` - Format seconds to human-readable duration
- `formatSpeed()` - Convert m/s to km/h
- `parseRouteGeometry()` - Parse GeoJSON geometry
- `calculateBounds()` - Calculate map bounds for waypoints
- `getCongestionColor()` - Get color for congestion level
- `getCongestionLabel()` - Get label for congestion level

## Notes

- The routing algorithm prioritizes completing all higher-priority stops before moving to lower-priority ones
- Traffic information and estimated delays are calculated based on current conditions
- The map automatically fits to show all waypoints and the calculated route
- All changes are stored in the Pinia store and can be reset at any time
