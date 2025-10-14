# Architecture - Zone Service

**Complete system design and architecture documentation**

---

## Table of Contents

1. [System Overview](#system-overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Component Architecture](#component-architecture)
4. [Database Schema](#database-schema)
5. [OSRM Integration](#osrm-integration)
6. [Weight Calculation System](#weight-calculation-system)
7. [Data Flow](#data-flow)
8. [Deployment Architecture](#deployment-architecture)
9. [Design Patterns](#design-patterns)
10. [Performance Considerations](#performance-considerations)

---

## System Overview

Zone Service is a **microservice** within the Delivery Coordination System that provides:

- **Routing**: Calculate optimal routes using OSRM
- **Zone Management**: Organize delivery areas (Thu Duc districts)
- **Traffic Integration**: Real-time traffic-aware routing
- **Weight Management**: Dynamic route cost calculation

### Key Design Principles

1. **Self-Hosted**: No external API dependencies (except traffic data)
2. **Zero Downtime**: Dual OSRM instances for continuous operation
3. **Performance First**: Optimized for high-throughput routing queries
4. **Traffic-Aware**: Adapts to real-time traffic conditions
5. **Scalable**: Horizontal scaling through instance duplication

---

## High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        MA[Mobile App<br/>Delivery Drivers]
        WD[Web Dashboard<br/>Dispatchers]
    end

    subgraph "API Layer"
        AG[API Gateway<br/>:8080]
    end

    subgraph "Zone Service :21503"
        direction TB
        ES[Express Server]

        subgraph "Controllers"
            RC[Routing Controller]
            ZC[Zone Controller]
            CC[Center Controller]
        end

        subgraph "Services"
            RS[OSRM Router Service]
            OG[OSRM Generator Service]
            TI[Traffic Integration Service]
            WC[Weight Calculator]
        end

        subgraph "Data Layer"
            ZM[Zone Model]
            RM[Road Model]
            SM[Segment Model]
        end
    end

    subgraph "OSRM Layer"
        O1[OSRM Instance 1<br/>:5000<br/>Active]
        O2[OSRM Instance 2<br/>:5001<br/>Standby]
    end

    subgraph "Data Storage"
        PG[(PostgreSQL<br/>zone_db)]
    end

    subgraph "External Services"
        TA[TrackAsia API<br/>Traffic Data]
        OSM[OpenStreetMap<br/>Initial Data]
    end

    MA --> AG
    WD --> AG
    AG --> ES

    ES --> RC
    ES --> ZC
    ES --> CC

    RC --> RS
    RS --> O1
    RS --> O2

    RC --> ZM
    ZC --> ZM
    CC --> ZM

    ZM --> PG
    RM --> PG
    SM --> PG

    TI --> TA
    TI --> WC
    WC --> SM

    OG --> PG
    OG --> O1
    OG --> O2

    OSM -.Initial Data.-> PG

    style O1 fill:#90EE90
    style O2 fill:#FFB6C1
    style PG fill:#DDA0DD
    style ES fill:#87CEEB
```

### Layer Descriptions

| Layer | Components | Responsibility |
|-------|-----------|----------------|
| **Client** | Mobile App, Web Dashboard | Send routing requests |
| **API** | API Gateway | Authentication, rate limiting, routing |
| **Application** | Controllers, Services | Business logic |
| **OSRM** | Dual instances | Route calculations |
| **Data** | PostgreSQL | Persistent storage |
| **External** | TrackAsia, OSM | Traffic data, map data |

---

## Component Architecture

### Application Structure

```
zone_service/
├── src/
│   ├── app.ts                        # Express app initialization
│   ├── index.ts                      # Entry point
│   │
│   ├── common/                       # Shared utilities
│   │   ├── config/
│   │   │   └── index.ts             # Configuration loader
│   │   ├── database/
│   │   │   ├── index.ts             # Database connection
│   │   │   └── prisma.client.ts     # Prisma client singleton
│   │   ├── decorators/
│   │   │   └── validation.decorator.ts  # Request validation
│   │   ├── health/
│   │   │   └── index.ts             # Health check endpoints
│   │   ├── kafka/
│   │   │   └── kafka.service.ts     # Kafka producer/consumer
│   │   ├── logger/
│   │   │   └── logger.service.ts    # Winston logger
│   │   ├── middleware/
│   │   │   └── logger.middleware.ts # Request logging
│   │   ├── startup/
│   │   │   └── settings-check.ts    # Startup validation
│   │   └── types/
│   │       └── index.ts             # TypeScript types
│   │
│   ├── modules/                      # Feature modules
│   │   ├── routing/
│   │   │   ├── routing.controller.ts    # Route endpoints
│   │   │   ├── routing.service.ts       # Routing logic
│   │   │   ├── routing.model.ts         # Data models
│   │   │   ├── routing.interface.ts     # TypeScript interfaces
│   │   │   └── routing.router.ts        # Express router
│   │   ├── zone/
│   │   │   ├── zone.controller.ts
│   │   │   ├── zone.model.ts
│   │   │   └── zone.router.ts
│   │   ├── center/
│   │   │   ├── center.controller.ts
│   │   │   ├── center.model.ts
│   │   │   └── center.router.ts
│   │   └── routes.ts                # Central route registration
│   │
│   └── services/                     # Business logic services
│       ├── osrm/
│       │   ├── osrm-generator.service.ts  # Generate OSRM data
│       │   └── osrm-router.service.ts     # Query OSRM instances
│       └── traffic/
│           └── traffic-integration.service.ts  # Fetch traffic data
│
├── prisma/                           # Database schema
│   ├── schema.prisma                # Prisma config
│   ├── models/                      # Model definitions
│   │   ├── zones.prisma
│   │   ├── roads.prisma
│   │   ├── road_nodes.prisma
│   │   ├── road_segments.prisma
│   │   └── traffic_conditions.prisma
│   └── migrations/                  # Migration history
│
├── processors/                       # Data processing scripts
│   ├── zones-seeder.ts              # Seed zones
│   └── roads-seeder.ts              # Seed roads (optimized!)
│
├── osrm_data/                        # OSRM data files
│   ├── osrm-instance-1/
│   └── osrm-instance-2/
│
└── .docs/                            # Documentation
```

### Component Diagram

```mermaid
graph TB
    subgraph "Routing Module"
        RC[Routing Controller]
        RS[Routing Service]
        RM[Routing Model]

        RC --> RS
        RS --> RM
    end

    subgraph "OSRM Services"
        ORS[OSRM Router Service]
        OGS[OSRM Generator Service]

        RS --> ORS
        OGS -.Generates Data.-> ORS
    end

    subgraph "Traffic Services"
        TIS[Traffic Integration Service]
        WCS[Weight Calculator Service]

        TIS --> WCS
    end

    subgraph "Data Models"
        ZM[Zone Model]
        RDM[Road Model]
        SNM[Segment Model]

        RM --> SNM
        ZM --> SNM
        WCS --> SNM
    end

    subgraph "Infrastructure"
        PC[Prisma Client]
        LOG[Logger Service]
        KS[Kafka Service]

        ZM --> PC
        RDM --> PC
        SNM --> PC
    end

    style RC fill:#87CEEB
    style ORS fill:#90EE90
    style TIS fill:#FFB6C1
    style PC fill:#DDA0DD
```

---

## Database Schema

### Entity Relationship Diagram

```mermaid
erDiagram
    centers ||--o{ zones : has
    zones ||--o{ roads : contains
    zones ||--o{ road_nodes : contains
    zones ||--o{ road_segments : contains
    zones ||--o{ zone_geohash_cells : indexed_by
    zones ||--o{ addresses : contains

    roads ||--o{ road_segments : divided_into

    road_nodes ||--o{ road_segments : from_node
    road_nodes ||--o{ road_segments : to_node

    road_segments ||--o{ traffic_conditions : has
    road_segments ||--o{ user_feedback : receives
    road_segments ||--o{ addresses : located_on

    traffic_conditions ||--o{ weight_history : generates

    centers {
        uuid center_id PK
        string code UK
        string name
        string address
        float lat
        float lon
        json polygon
    }

    zones {
        uuid zone_id PK
        string code UK
        string name
        json polygon
        uuid center_id FK
    }

    roads {
        uuid road_id PK
        string osm_id UK
        string name
        string name_en
        enum road_type
        float max_speed
        float avg_speed
        boolean one_way
        int lanes
        string surface
        json geometry
        uuid zone_id FK
        datetime created_at
        datetime updated_at
    }

    road_nodes {
        uuid node_id PK
        string osm_id UK
        float lat
        float lon
        enum node_type
        uuid zone_id FK
        datetime created_at
        datetime updated_at
    }

    road_segments {
        uuid segment_id PK
        uuid from_node_id FK
        uuid to_node_id FK
        uuid road_id FK
        json geometry
        float length_meters
        string name
        enum road_type
        float max_speed
        float avg_speed
        boolean one_way
        float base_weight
        float current_weight
        float delta_weight
        uuid zone_id FK
        datetime created_at
        datetime updated_at
        datetime weight_updated_at
    }

    traffic_conditions {
        uuid traffic_condition_id PK
        uuid segment_id FK
        enum traffic_level
        float congestion_score
        float current_speed
        float speed_ratio
        float weight_multiplier
        string source
        datetime source_timestamp
        datetime created_at
        datetime expires_at
    }

    zone_geohash_cells {
        uuid cell_id PK
        uuid zone_id FK
        string geohash
        int precision
        json bounds
    }

    addresses {
        uuid address_id PK
        string address_text
        float lat
        float lon
        uuid segment_id FK
        uuid zone_id FK
    }

    user_feedback {
        uuid feedback_id PK
        uuid segment_id FK
        enum feedback_type
        string description
        datetime created_at
    }

    weight_history {
        uuid history_id PK
        uuid segment_id FK
        float old_weight
        float new_weight
        string reason
        datetime changed_at
    }
```

### Key Tables

#### 1. **roads** - Master road information
- Represents entire roads (e.g., "Phạm Văn Đồng")
- Contains full geometry, speed limits, road characteristics
- One road → many segments

#### 2. **road_nodes** - Intersections and waypoints
- Geographic points where roads connect
- Types: INTERSECTION, TRAFFIC_LIGHT, ENDPOINT, WAYPOINT
- Used to build the routing graph

#### 3. **road_segments** - Routing units
- **Core routing unit** - represents road between two nodes
- Contains weight calculations (base, delta, current)
- Inherits properties from parent road
- One segment = one edge in routing graph

#### 4. **traffic_conditions** - Real-time traffic data
- Fetched from TrackAsia API
- Updates weight_multiplier based on congestion
- Expires after configured time (default: 30 min)

#### 5. **zones** - Delivery zones
- Thu Duc districts (8 zones)
- Contains polygon boundaries
- Linked to distribution centers

### Indexes

```sql
-- Performance-critical indexes
CREATE INDEX idx_road_segments_from_node ON road_segments(from_node_id);
CREATE INDEX idx_road_segments_to_node ON road_segments(to_node_id);
CREATE INDEX idx_road_segments_weight ON road_segments(current_weight);
CREATE INDEX idx_road_nodes_coords ON road_nodes(lat, lon);
CREATE INDEX idx_traffic_expires ON traffic_conditions(expires_at);
CREATE INDEX idx_roads_name ON roads(name);

-- Geospatial indexes (PostGIS)
CREATE INDEX idx_zones_geometry ON zones USING GIST(geometry);
CREATE INDEX idx_roads_geometry ON roads USING GIST(geometry);
CREATE INDEX idx_segments_geometry ON road_segments USING GIST(geometry);
```

---

## OSRM Integration

### Dual Instance Architecture

```mermaid
sequenceDiagram
    participant Client
    participant ZoneService
    participant Router as OSRM Router Service
    participant I1 as OSRM Instance 1 (Active)
    participant I2 as OSRM Instance 2 (Standby)
    participant Generator as OSRM Generator
    participant DB as PostgreSQL

    Note over I1,I2: Normal Operation - Instance 1 Active
    Client->>ZoneService: GET /route
    ZoneService->>Router: getRoute(waypoints)
    Router->>I1: HTTP /route/v1/driving/...
    I1-->>Router: Route response
    Router-->>ZoneService: Route data
    ZoneService-->>Client: Route response

    Note over I1,I2: Traffic Update - Rebuild Instance 2
    DB->>Generator: Traffic data changed
    Generator->>DB: Export road network
    Generator->>I2: Generate new OSRM data
    I2-->>Generator: Build complete

    Note over I1,I2: Switch to Instance 2
    Router->>Router: Set active instance = 2
    Client->>ZoneService: GET /route
    ZoneService->>Router: getRoute(waypoints)
    Router->>I2: HTTP /route/v1/driving/...
    I2-->>Router: Route response
    Router-->>ZoneService: Route data
    ZoneService-->>Client: Route response

    Note over I1,I2: Rebuild Instance 1 (Next Update)
    Generator->>I1: Generate new OSRM data
    I1-->>Generator: Build complete
    Note over I1,I2: Ready to switch back to Instance 1
```

### OSRM Data Generation Flow

```mermaid
flowchart TB
    Start([Start Generation]) --> Query[Query road_segments<br/>with current_weight]
    Query --> CreateXML[Create OSM XML<br/>with custom_weight tags]
    CreateXML --> CreateLua[Create custom_car.lua<br/>profile]
    CreateLua --> Extract[osrm-extract<br/>Parse XML → .osrm]
    Extract --> Partition[osrm-partition<br/>Create MLD cells]
    Partition --> Customize[osrm-customize<br/>Apply custom weights]
    Customize --> Deploy{Deploy to<br/>which instance?}
    Deploy -->|Active = 1| I2[Copy to Instance 2]
    Deploy -->|Active = 2| I1[Copy to Instance 1]
    I2 --> Wait[Wait for next request]
    I1 --> Wait
    Wait --> Switch[Switch active instance]
    Switch --> Done([Generation Complete])

    style Extract fill:#90EE90
    style Partition fill:#87CEEB
    style Customize fill:#FFB6C1
    style Deploy fill:#DDA0DD
```

### OSRM Files

| File | Purpose | Size | Generated By |
|------|---------|------|--------------|
| `network.osm.xml` | OSM data with custom weights | ~3 MB | Generator Service |
| `custom_car.lua` | Lua profile for weight mapping | ~2 KB | Generator Service |
| `network.osrm` | OSRM graph structure | ~5 MB | osrm-extract |
| `network.osrm.edges` | Edge data | ~1 MB | osrm-partition |
| `network.osrm.cells` | Partition cells | ~500 KB | osrm-partition |
| `network.osrm.mldgr` | Multi-level Dijkstra graph | ~3 MB | osrm-customize |
| `network.osrm.partition` | Partition metadata | ~100 KB | osrm-partition |

---

## Weight Calculation System

### Weight Formula

```typescript
// Base Weight (static - calculated once during seeding)
base_weight = (length_meters / avg_speed_kmh) * 3.6 * road_type_multiplier * lane_multiplier

// Road Type Multiplier
const ROAD_TYPE_MULTIPLIERS = {
  MOTORWAY: 1.0,      // Fastest
  TRUNK: 1.1,
  PRIMARY: 1.2,
  SECONDARY: 1.3,
  TERTIARY: 1.4,
  RESIDENTIAL: 1.6,
  SERVICE: 1.8,
  UNCLASSIFIED: 2.0,
  LIVING_STREET: 2.2,
  PEDESTRIAN: 2.5,
  TRACK: 2.8,
  PATH: 3.0           // Slowest
}

// Lane Multiplier (more lanes = lower weight)
lane_multiplier = lanes >= 3 ? 0.9 : (lanes === 2 ? 1.0 : 1.1)

// Delta Weight (dynamic - updated by traffic)
delta_weight = base_weight * (traffic_multiplier - 1) + user_feedback_adjustment

// Traffic Multiplier (from TrackAsia)
traffic_multiplier = {
  FREE_FLOW: 0.9,     // 10% faster
  NORMAL: 1.0,        // No change
  SLOW: 1.3,          // 30% slower
  CONGESTED: 1.8,     // 80% slower
  BLOCKED: 3.0        // 200% slower (avoid!)
}

// Current Weight (used for routing)
current_weight = base_weight + delta_weight
```

### Weight Calculation Flow

```mermaid
flowchart TB
    Start([New Road Segment]) --> CalcBase[Calculate base_weight<br/>length / speed * multipliers]
    CalcBase --> SaveBase[Save to road_segments]

    SaveBase --> WaitTraffic[Wait for Traffic Update]
    WaitTraffic --> FetchTraffic[Fetch from TrackAsia API]
    FetchTraffic --> AnalyzeTraffic{Traffic Level?}

    AnalyzeTraffic -->|FREE_FLOW| Multi09[Multiplier = 0.9]
    AnalyzeTraffic -->|NORMAL| Multi10[Multiplier = 1.0]
    AnalyzeTraffic -->|SLOW| Multi13[Multiplier = 1.3]
    AnalyzeTraffic -->|CONGESTED| Multi18[Multiplier = 1.8]
    AnalyzeTraffic -->|BLOCKED| Multi30[Multiplier = 3.0]

    Multi09 --> CalcDelta[Calculate delta_weight<br/>base * multiplier - 1]
    Multi10 --> CalcDelta
    Multi13 --> CalcDelta
    Multi18 --> CalcDelta
    Multi30 --> CalcDelta

    CalcDelta --> CalcCurrent[current_weight =<br/>base + delta]
    CalcCurrent --> CheckChange{Weight changed<br/>> 10%?}

    CheckChange -->|Yes| LogHistory[Log to weight_history]
    CheckChange -->|No| Skip[Skip rebuild]

    LogHistory --> CheckThreshold{Many segments<br/>changed?}
    CheckThreshold -->|Yes| TriggerRebuild[Trigger OSRM Rebuild]
    CheckThreshold -->|No| UpdateDB[Update DB only]

    TriggerRebuild --> Done([Done])
    UpdateDB --> Done
    Skip --> Done

    style CalcBase fill:#90EE90
    style CalcDelta fill:#FFB6C1
    style TriggerRebuild fill:#DDA0DD
```

### Example Weight Calculation

```typescript
// Example: Phạm Văn Đồng segment
const segment = {
  name: "Phạm Văn Đồng",
  length_meters: 500,
  avg_speed: 50, // km/h
  road_type: "PRIMARY",
  lanes: 4
}

// Step 1: Base weight
const time_seconds = (500 / (50 * 1000)) * 3600  // = 36 seconds
const road_multiplier = 1.2  // PRIMARY
const lane_multiplier = 0.9  // 4 lanes
const base_weight = 36 * 1.2 * 0.9 = 38.88

// Step 2: Traffic update (CONGESTED)
const traffic_multiplier = 1.8  // 80% slower
const delta_weight = 38.88 * (1.8 - 1) = 31.10

// Step 3: Current weight
const current_weight = 38.88 + 31.10 = 69.98

// Interpretation: Segment takes 70 seconds due to traffic (vs 39 normal)
```

---

## Data Flow

### Routing Request Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Controller as Routing Controller
    participant Service as Routing Service
    participant Router as OSRM Router
    participant OSRM as OSRM Instance
    participant DB as PostgreSQL

    Client->>Gateway: POST /api/v1/routing/route<br/>{waypoints: [...]}
    Gateway->>Gateway: Authenticate & validate
    Gateway->>Controller: Forward request

    Controller->>Controller: Validate waypoints
    Controller->>Service: getRoute(waypoints, options)

    Service->>Router: queryOSRM(waypoints)
    Router->>Router: Select active instance
    Router->>OSRM: GET /route/v1/driving/lon1,lat1;lon2,lat2
    OSRM-->>Router: Route response (distance, duration, geometry)
    Router-->>Service: Parsed route data

    Service->>DB: Log route request
    DB-->>Service: Logged

    Service->>Service: Enrich response (add zone info)
    Service-->>Controller: Route with metadata
    Controller-->>Gateway: JSON response
    Gateway-->>Client: Route response

    Note over Client,DB: Typical response time: 20-50ms
```

### Traffic Update Flow

```mermaid
sequenceDiagram
    participant Scheduler as Cron Scheduler
    participant Traffic as Traffic Service
    participant TrackAsia as TrackAsia API
    participant Calculator as Weight Calculator
    participant DB as PostgreSQL
    participant Generator as OSRM Generator
    participant OSRM as OSRM Instance

    Scheduler->>Traffic: Every 30 minutes
    Traffic->>TrackAsia: GET /traffic/segments
    TrackAsia-->>Traffic: Traffic data (congestion, speed)

    loop For each segment
        Traffic->>Calculator: updateWeight(segment_id, traffic_data)
        Calculator->>DB: Get current base_weight
        DB-->>Calculator: base_weight
        Calculator->>Calculator: Calculate delta_weight<br/>and current_weight
        Calculator->>DB: Update road_segments<br/>SET current_weight, delta_weight
        DB-->>Calculator: Updated

        alt Weight changed > 10%
            Calculator->>DB: INSERT INTO weight_history
        end
    end

    Traffic->>Traffic: Count segments with major changes

    alt > 20% segments changed
        Traffic->>Generator: Trigger OSRM rebuild
        Generator->>DB: Export road network
        DB-->>Generator: All segments with weights
        Generator->>Generator: Generate OSM XML + Lua
        Generator->>OSRM: Run osrm-extract,<br/>partition, customize
        OSRM-->>Generator: Build complete
        Generator->>Generator: Switch active instance
    end
```

### Data Seeding Flow

```mermaid
flowchart TB
    Start([Start Seeding]) --> DownloadOSM[Download OSM PBF<br/>Ho Chi Minh City]
    DownloadOSM --> ExtractThuduc[Extract Thu Duc area<br/>using osmium + polygon]
    ExtractThuduc --> ParseOSM[Parse OSM to GeoJSON<br/>osmium export]

    ParseOSM --> SeedZones[Seed Zones<br/>npm run seed:zones]
    SeedZones --> ParsePolygons[Parse .poly files]
    ParsePolygons --> CreateZones[Create 8 zone records]
    CreateZones --> GenerateGeohash[Generate geohash cells<br/>for spatial indexing]

    GenerateGeohash --> SeedRoads[Seed Roads<br/>npm run seed:roads]
    SeedRoads --> ExtractRoads[Extract roads from GeoJSON]
    ExtractRoads --> BatchInsertRoads[Batch insert roads<br/>500 at a time]

    BatchInsertRoads --> CreateNodes[Create road nodes<br/>from LineString coordinates]
    CreateNodes --> BatchInsertNodes[Batch insert nodes]

    BatchInsertNodes --> CreateSegments[Create segments<br/>between consecutive nodes]
    CreateSegments --> CalcWeights[Calculate base_weight<br/>for each segment]
    CalcWeights --> BatchInsertSegments[Batch insert segments]

    BatchInsertSegments --> MergeNodes[Merge duplicate nodes<br/>coordinate-based 1.1m precision]
    MergeNodes --> BulkUpdate[Bulk SQL update<br/>segment from/to nodes]

    BulkUpdate --> Done([Seeding Complete<br/>~51 seconds])

    style SeedZones fill:#90EE90
    style SeedRoads fill:#87CEEB
    style MergeNodes fill:#FFB6C1
    style BatchInsertRoads fill:#DDA0DD
```

---

## Deployment Architecture

### Docker Compose Setup

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres-zone:
    image: postgis/postgis:15-3.3
    container_name: dss-postgres-zone
    environment:
      POSTGRES_USER: zone_user
      POSTGRES_PASSWORD: ${ZONE_DB_PASSWORD}
      POSTGRES_DB: zone_db
    volumes:
      - zone_db_data:/var/lib/postgresql/data
    ports:
      - "5433:5432"
    networks:
      - dss-network

  # OSRM Instance 1 (Active)
  osrm-instance-1:
    image: osrm/osrm-backend:latest
    container_name: dss-osrm-1
    volumes:
      - ./BE/zone_service/osrm_data/osrm-instance-1:/data
    command: osrm-routed --algorithm mld /data/network.osrm
    ports:
      - "5000:5000"
    healthcheck:
      test: ["CMD-SHELL", "timeout 2 bash -c '</dev/tcp/localhost/5000' || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 10s
    networks:
      - dss-network

  # OSRM Instance 2 (Standby)
  osrm-instance-2:
    image: osrm/osrm-backend:latest
    container_name: dss-osrm-2
    volumes:
      - ./BE/zone_service/osrm_data/osrm-instance-2:/data
    command: osrm-routed --algorithm mld /data/network.osrm
    ports:
      - "5001:5000"
    healthcheck:
      test: ["CMD-SHELL", "timeout 2 bash -c '</dev/tcp/localhost/5000' || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 10s
    networks:
      - dss-network

  # Zone Service
  zone-service:
    build:
      context: ./BE/zone_service
      dockerfile: Dockerfile
    container_name: dss-zone-service
    environment:
      DATABASE_URL: postgresql://zone_user:${ZONE_DB_PASSWORD}@postgres-zone:5432/zone_db
      OSRM_INSTANCE_1_URL: http://osrm-instance-1:5000
      OSRM_INSTANCE_2_URL: http://osrm-instance-2:5000
      PORT: 21503
    ports:
      - "21503:21503"
    depends_on:
      postgres-zone:
        condition: service_healthy
      osrm-instance-1:
        condition: service_healthy
      osrm-instance-2:
        condition: service_healthy
    networks:
      - dss-network

volumes:
  zone_db_data:

networks:
  dss-network:
    driver: bridge
```

### Production Deployment

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Nginx / HAProxy]
    end

    subgraph "Zone Service Cluster"
        ZS1[Zone Service 1<br/>:21503]
        ZS2[Zone Service 2<br/>:21503]
        ZS3[Zone Service 3<br/>:21503]
    end

    subgraph "OSRM Cluster Instance 1"
        O1A[OSRM 1A :5000]
        O1B[OSRM 1B :5000]
    end

    subgraph "OSRM Cluster Instance 2"
        O2A[OSRM 2A :5001]
        O2B[OSRM 2B :5001]
    end

    subgraph "Database"
        PG[(PostgreSQL<br/>Primary)]
        PGR[(PostgreSQL<br/>Replica)]
    end

    LB --> ZS1
    LB --> ZS2
    LB --> ZS3

    ZS1 --> O1A
    ZS1 --> O2A
    ZS2 --> O1B
    ZS2 --> O2B
    ZS3 --> O1A
    ZS3 --> O2A

    ZS1 --> PG
    ZS2 --> PG
    ZS3 --> PG

    PG --> PGR

    style LB fill:#90EE90
    style PG fill:#DDA0DD
```

### Scaling Strategy

| Component | Scaling Strategy | Max Instances | Notes |
|-----------|-----------------|---------------|-------|
| **Zone Service** | Horizontal | Unlimited | Stateless, easy to scale |
| **OSRM Instances** | Horizontal | 10-20 per set | Each needs ~2GB RAM |
| **PostgreSQL** | Vertical + Replication | 1 Primary + N Replicas | Use read replicas for queries |
| **Traffic Service** | Singleton | 1 | Cron-based, doesn't need scaling |

---

## Design Patterns

### 1. Repository Pattern

```typescript
// Abstract data access
interface ISegmentRepository {
  findById(id: string): Promise<Segment | null>
  findByZone(zoneId: string): Promise<Segment[]>
  updateWeight(id: string, weight: number): Promise<void>
}

class SegmentRepository implements ISegmentRepository {
  constructor(private prisma: PrismaClient) {}

  async findById(id: string): Promise<Segment | null> {
    return this.prisma.road_segments.findUnique({ where: { segment_id: id } })
  }

  // ... implementation
}
```

### 2. Strategy Pattern (OSRM Instance Selection)

```typescript
interface IRoutingStrategy {
  getActiveInstance(): OSRMInstance
  switchInstance(): void
}

class DualInstanceStrategy implements IRoutingStrategy {
  private activeInstance: 1 | 2 = 1

  getActiveInstance(): OSRMInstance {
    return this.activeInstance === 1 ? instance1 : instance2
  }

  switchInstance(): void {
    this.activeInstance = this.activeInstance === 1 ? 2 : 1
  }
}
```

### 3. Factory Pattern (Weight Calculator)

```typescript
class WeightCalculatorFactory {
  static create(segmentType: RoadType): IWeightCalculator {
    switch (segmentType) {
      case 'MOTORWAY':
        return new MotorwayWeightCalculator()
      case 'RESIDENTIAL':
        return new ResidentialWeightCalculator()
      default:
        return new DefaultWeightCalculator()
    }
  }
}
```

### 4. Singleton Pattern (Prisma Client)

```typescript
class PrismaClientSingleton {
  private static instance: PrismaClient

  static getInstance(): PrismaClient {
    if (!this.instance) {
      this.instance = new PrismaClient()
    }
    return this.instance
  }
}
```

---

## Performance Considerations

### 1. Database Optimization

```sql
-- Indexes for common queries
CREATE INDEX idx_segments_zone_weight ON road_segments(zone_id, current_weight);
CREATE INDEX idx_nodes_coordinates ON road_nodes USING BTREE (lat, lon);

-- Partial index for active traffic only
CREATE INDEX idx_traffic_active ON traffic_conditions(segment_id)
WHERE expires_at > NOW();

-- Composite index for route queries
CREATE INDEX idx_segments_nodes ON road_segments(from_node_id, to_node_id, current_weight);
```

### 2. Query Optimization

```typescript
// Bad: N+1 query problem
const segments = await prisma.road_segments.findMany()
for (const seg of segments) {
  const traffic = await prisma.traffic_conditions.findFirst({
    where: { segment_id: seg.segment_id }
  })
}

// Good: Single query with join
const segments = await prisma.road_segments.findMany({
  include: {
    traffic_conditions: {
      where: { expires_at: { gt: new Date() } },
      orderBy: { source_timestamp: 'desc' },
      take: 1
    }
  }
})
```

### 3. Batch Operations

```typescript
// Bad: Individual inserts (1000x slower)
for (const node of nodes) {
  await prisma.road_nodes.create({ data: node })
}

// Good: Batch insert (500 at a time)
const batchSize = 500
for (let i = 0; i < nodes.length; i += batchSize) {
  const batch = nodes.slice(i, i + batchSize)
  await prisma.road_nodes.createMany({
    data: batch,
    skipDuplicates: true
  })
}
```

### 4. Caching Strategy

```typescript
// Cache OSRM instances
const instanceCache = new Map<number, AxiosInstance>()

// Cache zone polygons (rarely change)
const zoneCache = new NodeCache({ stdTTL: 3600 }) // 1 hour

// Cache recent routes (5 min TTL)
const routeCache = new NodeCache({ stdTTL: 300 })
```

### 5. Connection Pooling

```typescript
// Prisma connection pool
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: process.env.DATABASE_URL,
    },
  },
  // Connection pool settings
  __internal: {
    engine: {
      connection_limit: 20,
      pool_timeout: 10,
    },
  },
})
```

### Performance Metrics

| Operation | Target | Current | Notes |
|-----------|--------|---------|-------|
| Single route query | < 100ms | ~20ms | OSRM + DB lookup |
| Multi-stop route (5 stops) | < 300ms | ~150ms | Optimize with TSP |
| Traffic update (all segments) | < 5 min | ~3 min | Batch processing |
| OSRM data generation | < 3 min | ~2 min | Extract + partition + customize |
| Road seeding (17k roads) | < 5 min | ~51 sec | Batch inserts + bulk updates |

---

## Security Considerations

### 1. Input Validation

```typescript
// Validate coordinates
function validateCoordinate(lat: number, lon: number): boolean {
  return lat >= 10.7 && lat <= 11.0 && lon >= 106.6 && lon <= 106.9
}

// Validate waypoints
function validateWaypoints(waypoints: Waypoint[]): boolean {
  if (waypoints.length < 2 || waypoints.length > 25) {
    throw new ValidationError('Waypoints must be between 2 and 25')
  }
  return waypoints.every(wp => validateCoordinate(wp.lat, wp.lon))
}
```

### 2. Rate Limiting

```typescript
// Rate limit routing requests
const rateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 100, // 100 requests per minute
  message: 'Too many routing requests'
})

app.use('/api/v1/routing', rateLimiter)
```

### 3. SQL Injection Prevention

```typescript
// Bad: String concatenation (vulnerable!)
const query = `SELECT * FROM road_segments WHERE name = '${userInput}'`

// Good: Parameterized query (Prisma handles this)
const segments = await prisma.road_segments.findMany({
  where: { name: userInput }
})
```

---

## Next Steps

- **API Documentation**: See [API.md](./API.md) for endpoint details
- **Setup Guide**: See [SETUP.md](./SETUP.md) for installation
- **Workflows**: See [WORKFLOWS.md](./WORKFLOWS.md) for development process
- **Troubleshooting**: See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for common issues

---

**Last Updated:** 2025-01-15
**Version:** 1.0.0
