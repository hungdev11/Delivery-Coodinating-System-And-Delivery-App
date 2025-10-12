# Project Structure

```
src/main/java/com/ds/
├── app_context/                    # Database entities and db base queries
│   └── models/                    # Database models
│   └── repositories/              # Database repositories
|
├── common/                        # Shared utilities and DTOs
│   ├── entities/
│   │   ├── base/
│   │   │   └── BaseEntity.java    # Base entity with common fields (id, deleted, timestamps)
│   │   ├── dto/                   # Data Transfer Objects
│   │   │   ├── UserDto.java       # User data transfer object
│   │   │   ├── RoleDto.java       # Role data transfer object
│   │   │   ├── SettingDto.java    # Setting data transfer object
│   │   │   ├── request/           # Request DTOs
│   │   │   └── response/          # Response DTOs
│   │   └── common/                # Common entities used across modules
│   ├── interfaces/                # Service interfaces
│   ├── exceptions/
│   └── utils/
│
├── business/                      # Business logic layer
│   └── v1/
│       └── services/
│
└── project/                       # Interface layer and project setup
    ├── annotations/
    │   ├── AuthRequired.java      # Authentication required annotation
    │   ├── PublicRoute.java       # Public access annotation
    │   └── README.md              # Annotation documentation
    ├── configs/
    │   ├── AppConfig.java         # Application configuration
    │   ├── CorsConfig.java        # CORS configuration
    │   └── SecurityConfig.java    # Security configuration 
    ├── controllers/
    │   └── v1/
    │       ├── UserController.java    # User REST endpoints
    │       ├── RoleController.java    # Role REST endpoints
    │       └── SettingController.java # Setting REST endpoints
    ├── startup/                   # Application startup data initialization
    │   ├── AppStartup.java        # Main startup coordinator
    │   └── data/
    │       ├── RoleStartup.java   # Role data initialization (ADMIN, MANAGER)
    │       └── UserStartup.java   # User data initialization (admin + dev users)
    ├── security/
    │   └── UserContext.java       # User context holder
    └── DeliveryProjectApplication.java # Main application class
```
