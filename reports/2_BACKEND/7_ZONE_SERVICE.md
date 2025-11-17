### 1.11. Component Diagram: Zone Service

This diagram shows the internal structure of the `zone_service`. It is a specialized geospatial microservice built with Node.js, Express, and TypeScript. It handles zone management, routing via OSRM, and processes geospatial data.

```mermaid
graph TD
    subgraph "Zone Service (Node.js)"
        direction LR

        subgraph "Application & API Layer"
            direction TB
            entry["index.ts (Entry Point)"]
            app["app.ts (Express App)"]
            routes["API Routes (@/modules/routes.ts)"]
        end

        subgraph "Business Modules"
            direction TB
            zone_module["Zone Module"]
            address_module["Address Module"]
            routing_module["Routing Module"]
            center_module["Center Module"]
        end

        subgraph "Infrastructure & Services"
            direction TB
            prisma["Prisma Client"]
            kafka["KafkaJS Client"]
            osrm_client["OSRM Service Client (@/services/osrm)"]
            winston["Winston Logger"]
        end
        
        subgraph "Data Processing (Offline)"
            direction TB
            osm_parser["OSM Parser (@/parsers)"]
            data_processors["Data Processors (@/processors)"]
        end

        %% Connections
        entry --> app
        app --> routes
        routes -- "Route to" --> zone_module
        routes -- "Route to" --> address_module
        routes -- "Route to" --> routing_module
        routes -- "Route to" --> center_module

        routing_module -- "Uses" --> osrm_client
        
        zone_module -- "Uses" --> prisma
        address_module -- "Uses" --> prisma
        center_module -- "Uses" --> prisma

        zone_module -- "Publishes/Subscribes via" --> kafka
        
        osm_parser -- "Input for" --> data_processors
        data_processors -- "Seeds" --> prisma
    end

    subgraph "External Systems"
        api_gateway["API Gateway"]
        database["PostgreSQL/MySQL"]
        kafka_bus["Kafka Bus"]
        osrm_engine["OSRM Engine"]
    end

    api_gateway -- "Routes requests to" --> routes
    prisma -- "Connects to" --> database
    kafka -- "Connects to" --> kafka_bus
    osrm_client -- "Connects to" --> osrm_engine
```
