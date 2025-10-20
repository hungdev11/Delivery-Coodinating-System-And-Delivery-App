# 🏗️ User Service Architecture

## 📁 Project Structure

```
BE/User_service/
├── src/main/java/com/ds/user/
│   ├── UserServiceApplication.java          # 🚀 Main Application Class
│   ├── application/                         # 🌐 Presentation Layer
│   │   └── controllers/v1/
│   │       ├── UserController.java         # 👤 User REST API
│   │       └── QueryController.java        # 🔍 Query Metadata API
│   ├── business/                           # 💼 Business Logic Layer
│   │   └── v1/services/
│   │       └── UserService.java            # 👤 User Business Logic
│   ├── app_context/                        # 🔧 Infrastructure Layer
│   │   ├── repositories/
│   │   │   └── UserRepository.java         # 🗄️ Data Access
│   │   └── config/
│   │       └── DatabaseConfig.java         # ⚙️ Database Configuration
│   └── common/                             # 🔄 Shared Components
│       ├── entities/                       # 📦 Data Models
│       │   ├── base/                       # 🏠 Base Entities
│       │   │   └── User.java               # 👤 User Entity
│       │   ├── dto/                        # 📋 Data Transfer Objects
│       │   │   └── user/
│       │   │       ├── UserDto.java        # 👤 User DTO
│       │   │       ├── CreateUserRequest.java
│       │   │       └── UpdateUserRequest.java
│       │   └── common/                     # 🔧 Common DTOs
│       │       ├── BaseResponse.java       # 📤 Standard API Response
│       │       ├── PagingRequest.java      # 📄 Pagination Request
│       │       ├── filter/                 # 🔍 Filter System
│       │       │   ├── FilterCondition.java
│       │       │   ├── FilterGroup.java
│       │       │   └── FilterOperator.java
│       │       ├── sort/                   # 📊 Sort System
│       │       │   └── SortConfig.java
│       │       └── paging/                 # 📄 Pagination System
│       │           ├── Paging.java
│       │           └── PagedData.java
│       ├── interfaces/                     # 🔌 Service Interfaces
│       │   └── IUserService.java           # 👤 User Service Contract
│       ├── helper/                         # 🛠️ Helper Classes
│       │   ├── FilterableFieldRegistry.java # 🔍 Field Registry
│       │   ├── GenericQueryService.java    # 🔄 Generic Query Service
│       │   └── QueryService.java           # 🔍 Query Metadata Service
│       ├── utils/                          # 🛠️ Utility Classes
│       │   └── EnhancedQueryParser.java    # 🔍 Query Parser
│       └── exceptions/                     # ⚠️ Exception Handling
│           └── GlobalExceptionHandler.java # 🚨 Global Error Handler
├── src/main/resources/
│   ├── application.yml                     # ⚙️ Application Configuration
│   └── application-local.yml              # 🏠 Local Configuration
├── target/                                 # 🎯 Build Output
├── pom.xml                                 # 📦 Maven Configuration
├── Dockerfile                              # 🐳 Docker Configuration
└── README.MD                               # 📖 Project Documentation
```

## 🎯 Core Components

### **1. Application Layer (`application/`)**
- **Purpose**: Handle HTTP requests and responses
- **Key Files**:
  - `UserController.java` - REST API endpoints for user operations
  - `QueryController.java` - Query metadata and validation endpoints

### **2. Business Layer (`business/`)**
- **Purpose**: Contains business logic and rules
- **Key Files**:
  - `UserService.java` - User business logic implementation

### **3. Infrastructure Layer (`app_context/`)**
- **Purpose**: Data access and external integrations
- **Key Files**:
  - `UserRepository.java` - JPA repository for data access
  - `DatabaseConfig.java` - Database configuration

### **4. Common Layer (`common/`)**
- **Purpose**: Shared components across the application
- **Key Files**:
  - `entities/` - Data models and DTOs
  - `interfaces/` - Service contracts
  - `helper/` - Business helper classes
  - `utils/` - Utility functions
  - `exceptions/` - Exception handling

## 🔧 Essential Files

### **Must-Have Files for Development:**

1. **`UserServiceApplication.java`** - Spring Boot main class
2. **`UserController.java`** - REST API endpoints
3. **`UserService.java`** - Business logic
4. **`UserRepository.java`** - Data access
5. **`User.java`** - Entity model
6. **`UserDto.java`** - Data transfer object
7. **`BaseResponse.java`** - Standard API response
8. **`PagingRequest.java`** - Pagination request
9. **`EnhancedQueryParser.java`** - Query parsing utility
10. **`FilterableFieldRegistry.java`** - Field registry for filtering

## 🚀 Getting Started

### **For LLM Understanding:**
- This is a **Spring Boot microservice** following **layered architecture**
- **Presentation Layer** handles HTTP requests
- **Business Layer** contains core logic
- **Infrastructure Layer** manages data access
- **Common Layer** provides shared utilities

### **For Developers:**
1. **Controllers** go in `application/controllers/`
2. **Business logic** goes in `business/services/`
3. **Data access** goes in `app_context/repositories/`
4. **Shared utilities** go in `common/`
5. **DTOs** go in `common/entities/dto/`
6. **Entities** go in `common/entities/base/`

## 📋 Naming Conventions

- **Controllers**: `{Entity}Controller.java`
- **Services**: `{Entity}Service.java`
- **Repositories**: `{Entity}Repository.java`
- **DTOs**: `{Entity}Dto.java`
- **Entities**: `{Entity}.java`
- **Requests**: `{Action}{Entity}Request.java`

## 🔄 Data Flow

```
HTTP Request → Controller → Service → Repository → Database
     ↓              ↓         ↓          ↓
HTTP Response ← DTO ← Business Logic ← Entity
```

## 🎯 Key Features

- ✅ **Layered Architecture** - Clear separation of concerns
- ✅ **RESTful API** - Standard HTTP endpoints
- ✅ **JPA/Hibernate** - Object-relational mapping
- ✅ **Dynamic Filtering** - Advanced query capabilities
- ✅ **Pagination** - Efficient data loading
- ✅ **Exception Handling** - Global error management
- ✅ **DTO Pattern** - Data transfer optimization
- ✅ **Service Layer** - Business logic encapsulation
