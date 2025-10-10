# Service Interfaces

This package contains the service interfaces following Object-Oriented Programming (OOP) principles.

## Interface Design

### IUserService
Defines the contract for user-related operations:
- User CRUD operations
- User authentication
- Role assignment/removal
- User validation

### IRoleService
Defines the contract for role-related operations:
- Role CRUD operations
- Role management
- Default role initialization

### ISettingService
Defines the contract for setting-related operations:
- Setting CRUD operations
- Setting retrieval by key/group
- Setting management

## Implementation Structure

The interfaces are implemented in the `@business/v1/services/` package:

- **UserService** implements **IUserService**
- **RoleService** implements **IRoleService**  
- **SettingService** implements **ISettingService**

## Benefits of Interface-Based Design

1. **Abstraction**: Controllers depend on interfaces, not concrete implementations
2. **Testability**: Easy to mock interfaces for unit testing
3. **Flexibility**: Can swap implementations without changing controllers
4. **Maintainability**: Clear separation of concerns
5. **Extensibility**: Easy to add new implementations (e.g., caching, validation)

## Usage in Controllers

Controllers inject the interfaces:

```java
@RestController
public class UserController {
    private final IUserService userService; // Interface injection
    
    // Controller methods use interface methods
}
```

## Future Enhancements

- **Caching Layer**: Implement caching decorators
- **Validation Layer**: Add validation decorators
- **Audit Layer**: Add audit logging decorators
- **Multiple Implementations**: Different implementations for different environments
