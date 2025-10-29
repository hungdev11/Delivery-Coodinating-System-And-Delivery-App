# 🏗️ System Architecture Overview

## 📖 Table of Contents

1. [System Overview](#-system-overview)
2. [Microservices Architecture](#-microservices-architecture)
3. [Technology Stack](#-technology-stack)
4. [Service Communication](#-service-communication)
5. [Data Flow](#-data-flow)
6. [Security Architecture](#-security-architecture)
7. [Deployment Architecture](#-deployment-architecture)
8. [Development Standards](#-development-standards)

## 🌐 System Overview

This is a **microservices-based delivery management system** built with modern technologies and following industry best practices.

### **Core Business Domains**

- **👤 User Management** - User authentication, authorization, and profile management
- **📦 Parcel Management** - Package tracking, status updates, and delivery management
- **🗺️ Zone Management** - Geographic zones, routing, and delivery area management
- **⚙️ Settings Management** - System configuration and preferences
- **🔐 Session Management** - User sessions and authentication tokens
- **🌐 API Gateway** - Centralized entry point and request routing

## 🏗️ Microservices Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        🌐 Frontend Layer                        │
├─────────────────────────────────────────────────────────────────┤
│  ManagementSystem (Vue.js)  │  DeliveryApp (Android)           │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      🚪 API Gateway Layer                       │
├─────────────────────────────────────────────────────────────────┤
│                    api-gateway (Spring Boot)                    │
│              • Authentication & Authorization                   │
│              • Request Routing & Load Balancing                 │
│              • Rate Limiting & CORS                             │
│              • API Documentation (Swagger)                      │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    🔧 Microservices Layer                       │
├─────────────────────────────────────────────────────────────────┤
│  User Service    │  Parcel Service  │  Zone Service             │
│  (Spring Boot)   │  (Spring Boot)   │  (Node.js/TypeScript)     │
│                  │                  │                           │
│  Settings Service│  Session Service │  Additional Services      │
│  (Spring Boot)   │  (Spring Boot)   │  (Future)                 │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      🗄️ Data Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  PostgreSQL      │  MongoDB        │  Redis Cache               │
│  (Primary DB)    │  (Document DB)  │  (Session Store)           │
│                  │                 │                           │
│  File Storage    │  Message Queue  │  External APIs             │
│  (MinIO/S3)      │  (RabbitMQ)     │  (Maps, Payment, etc.)     │
└─────────────────────────────────────────────────────────────────┘
```

## 🛠️ Technology Stack

### **Frontend Technologies**

| Technology | Purpose | Location |
|------------|---------|----------|
| **Vue.js 3** | Management System UI | `ManagementSystem/` |
| **TypeScript** | Type-safe development | `ManagementSystem/` |
| **Nuxt UI** | Component library | `ManagementSystem/` |
| **TanStack Table** | Data tables with filtering/sorting | `ManagementSystem/` |
| **Android (Kotlin)** | Mobile delivery app | `DeliveryApp/` |

### **Backend Technologies**

| Technology | Purpose | Services |
|------------|---------|----------|
| **Spring Boot 3** | Java microservices | User, Parcel, Settings, Session, API Gateway |
| **Node.js/TypeScript** | JavaScript microservices | Zone Service |
| **JPA/Hibernate** | ORM for Java services | User, Parcel, Settings, Session |
| **Prisma** | ORM for Node.js | Zone Service |
| **Spring Security** | Authentication & Authorization | All Spring Boot services |
| **Keycloak** | Identity & Access Management | Centralized auth |

### **Data Technologies**

| Technology | Purpose | Usage |
|------------|---------|-------|
| **PostgreSQL** | Primary relational database | User, Parcel, Settings data |
| **MongoDB** | Document database | Zone data, complex queries |
| **Redis** | Caching & session storage | Session management, caching |
| **OSRM** | Routing engine | Zone Service routing |

### **Infrastructure Technologies**

| Technology | Purpose | Usage |
|------------|---------|-------|
| **Docker** | Containerization | All services |
| **Docker Compose** | Local development | Service orchestration |
| **Maven** | Java build tool | Spring Boot services |
| **npm/yarn** | Node.js package manager | Zone Service |
| **Gradle** | Android build tool | DeliveryApp |

## 🔄 Service Communication

### **Synchronous Communication**

```
Frontend → API Gateway → Microservice → Database
    ↓           ↓            ↓
  HTTP/HTTPS  HTTP/HTTPS  JDBC/ODBC
```

**Protocols:**
- **REST APIs** - Primary communication method
- **HTTP/HTTPS** - Standard web protocols
- **JSON** - Data exchange format

### **Asynchronous Communication**

```
Service A → Message Queue → Service B
    ↓            ↓            ↓
  Publisher   RabbitMQ    Subscriber
```

**Patterns:**
- **Event-Driven Architecture** - Service decoupling
- **Message Queues** - Reliable message delivery
- **WebSockets** - Real-time updates

### **Service Discovery**

```
Client → API Gateway → Service Registry → Target Service
    ↓         ↓              ↓
  Request  Route to      Find Service
           Service       Endpoint
```

## 📊 Data Flow

### **User Registration Flow**

```
1. Frontend → API Gateway → User Service → Database
2. User Service → Keycloak → Create User Account
3. User Service → Session Service → Create Session
4. Response ← API Gateway ← User Service ← Database
```

### **Parcel Tracking Flow**

```
1. Frontend → API Gateway → Parcel Service → Database
2. Parcel Service → Zone Service → Calculate Route
3. Zone Service → OSRM → Get Route Details
4. Parcel Service → Update Status
5. Response ← API Gateway ← Parcel Service
```

### **Real-time Updates Flow**

```
1. Service → Message Queue → WebSocket Service
2. WebSocket Service → Frontend (Real-time)
3. Frontend → Update UI
```

## 🔐 Security Architecture

### **Authentication Flow**

```
1. User Login → Frontend
2. Frontend → API Gateway → Keycloak
3. Keycloak → Validate Credentials
4. Keycloak → Issue JWT Token
5. JWT Token → Frontend → Store in Session
```

### **Authorization Flow**

```
1. Request → API Gateway
2. API Gateway → Validate JWT Token
3. API Gateway → Extract User Roles
4. API Gateway → Check Permissions
5. API Gateway → Route to Service
```

### **Security Layers**

| Layer | Technology | Purpose |
|-------|------------|---------|
| **API Gateway** | Spring Security | Request validation, rate limiting |
| **Microservices** | JWT Validation | Service-level authorization |
| **Database** | Row-level security | Data access control |
| **Network** | HTTPS/TLS | Encrypted communication |

## 🚀 Deployment Architecture

### **Development Environment**

```
┌─────────────────────────────────────────────────────────────────┐
│                    🖥️ Local Development                        │
├─────────────────────────────────────────────────────────────────┤
│  Docker Compose                                                 │
│  ├── PostgreSQL (Database)                                     │
│  ├── MongoDB (Document Store)                                  │
│  ├── Redis (Cache)                                             │
│  ├── Keycloak (Auth Server)                                    │
│  ├── API Gateway (Port 8080)                                   │
│  ├── User Service (Port 8081)                                  │
│  ├── Parcel Service (Port 8082)                                │
│  ├── Zone Service (Port 8083)                                  │
│  ├── Settings Service (Port 8084)                              │
│  └── Session Service (Port 8085)                               │
└─────────────────────────────────────────────────────────────────┘
```

### **Production Environment**

```
┌─────────────────────────────────────────────────────────────────┐
│                    ☁️ Production Deployment                     │
├─────────────────────────────────────────────────────────────────┤
│  Load Balancer (NGINX/HAProxy)                                 │
│  ├── API Gateway Cluster                                       │
│  ├── Microservices Cluster                                     │
│  ├── Database Cluster (PostgreSQL)                             │
│  ├── Document Store (MongoDB)                                  │
│  ├── Cache Cluster (Redis)                                     │
│  └── Message Queue (RabbitMQ)                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 📋 Development Standards

### **Code Organization**

```
Service Structure:
├── src/main/java/com/ds/{service}/
│   ├── {Service}Application.java          # Main class
│   ├── application/                       # Presentation layer
│   │   └── controllers/v1/               # REST controllers
│   ├── business/                         # Business logic
│   │   └── v1/services/                  # Service implementations
│   ├── app_context/                      # Infrastructure
│   │   ├── repositories/                 # Data access
│   │   └── config/                       # Configuration
│   └── common/                           # Shared components
│       ├── entities/                     # Data models
│       ├── interfaces/                   # Service contracts
│       ├── helper/                       # Helper classes
│       ├── utils/                        # Utility functions
│       └── exceptions/                   # Exception handling
```

### **API Standards**

- **RESTful Design** - Follow REST principles
- **POST for Queries** - Complex filtering and sorting
- **GET for Resources** - Simple resource retrieval
- **JSON Format** - Consistent data exchange
- **BaseResponse Wrapper** - Standardized response format
- **Pagination** - Consistent pagination across all endpoints

### **Database Standards**

- **Entity-First Design** - Start with domain entities
- **JPA Annotations** - Use standard JPA annotations
- **Repository Pattern** - Abstract data access
- **Migration Scripts** - Version-controlled schema changes
- **Indexing Strategy** - Optimize query performance

### **Testing Standards**

- **Unit Tests** - Test individual components
- **Integration Tests** - Test service interactions
- **API Tests** - Test REST endpoints
- **Database Tests** - Test data access layer
- **End-to-End Tests** - Test complete workflows

## 🎯 Key Architectural Principles

### **1. Microservices Architecture**
- **Single Responsibility** - Each service has one business domain
- **Loose Coupling** - Services communicate via APIs
- **High Cohesion** - Related functionality grouped together
- **Independent Deployment** - Services can be deployed separately

### **2. Domain-Driven Design**
- **Bounded Contexts** - Clear service boundaries
- **Ubiquitous Language** - Consistent terminology
- **Aggregate Roots** - Data consistency boundaries
- **Value Objects** - Immutable data structures

### **3. Event-Driven Architecture**
- **Asynchronous Communication** - Non-blocking operations
- **Event Sourcing** - Store events, not just state
- **CQRS** - Separate read and write models
- **Saga Pattern** - Distributed transaction management

### **4. API-First Design**
- **Contract-First** - Define APIs before implementation
- **Versioning Strategy** - Backward compatibility
- **Documentation** - Comprehensive API documentation
- **Testing** - API contract testing

### **5. Security by Design**
- **Zero Trust** - Verify every request
- **Least Privilege** - Minimal required permissions
- **Defense in Depth** - Multiple security layers
- **Audit Logging** - Track all operations

## 🔧 Development Workflow

### **1. Feature Development**
```
1. Create feature branch
2. Implement changes
3. Write tests
4. Code review
5. Merge to main
6. Deploy to staging
7. Deploy to production
```

### **2. Service Integration**
```
1. Define API contracts
2. Implement service endpoints
3. Update API documentation
4. Test integration
5. Deploy services
6. Monitor performance
```

### **3. Database Changes**
```
1. Create migration script
2. Test migration locally
3. Review migration script
4. Apply to staging
5. Apply to production
6. Verify data integrity
```

## 📊 Monitoring & Observability

### **Metrics Collection**
- **Application Metrics** - Response times, error rates
- **Infrastructure Metrics** - CPU, memory, disk usage
- **Business Metrics** - User registrations, parcel deliveries
- **Custom Metrics** - Domain-specific measurements

### **Logging Strategy**
- **Structured Logging** - JSON format for easy parsing
- **Log Levels** - DEBUG, INFO, WARN, ERROR
- **Correlation IDs** - Track requests across services
- **Centralized Logging** - Aggregate logs from all services

### **Health Checks**
- **Liveness Probes** - Service is running
- **Readiness Probes** - Service is ready to accept requests
- **Dependency Checks** - Database and external service health
- **Custom Health Indicators** - Business logic health

## 🚀 Future Considerations

### **Scalability**
- **Horizontal Scaling** - Add more service instances
- **Database Sharding** - Partition data across databases
- **Caching Strategy** - Reduce database load
- **CDN Integration** - Improve content delivery

### **Technology Evolution**
- **Kubernetes** - Container orchestration
- **Service Mesh** - Advanced service communication
- **Event Streaming** - Apache Kafka for real-time data
- **GraphQL** - Flexible API querying

### **Performance Optimization**
- **Database Optimization** - Query tuning, indexing
- **Caching Layers** - Redis, CDN, application cache
- **Async Processing** - Background job processing
- **Load Testing** - Performance validation

---

This architecture provides a solid foundation for building scalable, maintainable, and secure microservices that can evolve with business needs.
