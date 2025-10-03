# Keycloak Initialization Services

## Tổng quan

Thư mục này chứa các service chuyên biệt để khởi tạo dữ liệu Keycloak, được tách ra theo từng chức năng riêng biệt để dễ bảo trì và mở rộng.

## Cấu trúc Services

### 1. **MasterRealmConnectionService**
- **Chức năng**: Quản lý kết nối tới Keycloak master realm
- **Trách nhiệm**:
  - Tạo kết nối Keycloak client với master realm
  - Xác thực bằng admin credentials (dev/dev)
  - Test và validate kết nối

**Phương thức chính**:
```java
public Keycloak connectToMasterRealm()
```

### 2. **RealmInitializationService**
- **Chức năng**: Quản lý khởi tạo realms
- **Trách nhiệm**:
  - Kiểm tra realm đã tồn tại
  - Tạo realm mới nếu chưa có
  - Cấu hình các thuộc tính realm (login, registration, email, etc.)

**Phương thức chính**:
```java
public RealmResource createOrGetRealm(Keycloak masterKeycloak, RealmConfig config)
```

### 3. **RoleInitializationService**
- **Chức năng**: Quản lý khởi tạo roles (realm và client roles)
- **Trách nhiệm**:
  - Tạo realm-level roles
  - Tạo client-level roles
  - Kiểm tra roles đã tồn tại để tránh duplicate

**Phương thức chính**:
```java
public void createRealmRoles(RealmResource realmResource, List<RoleConfig> roleConfigs)
public void createClientRoles(RealmResource realmResource, String clientUuid, List<String> roleNames)
```

### 4. **ClientInitializationService**
- **Chức năng**: Quản lý khởi tạo clients
- **Trách nhiệm**:
  - Tạo clients (public hoặc confidential)
  - Cấu hình client settings (redirectUris, webOrigins, flows, etc.)
  - Tạo client roles thông qua RoleInitializationService
  - Quản lý client secrets

**Phương thức chính**:
```java
public void createClient(RealmResource realmResource, ClientConfig clientConfig)
```

**Dependencies**: `RoleInitializationService`

### 5. **UserInitializationService**
- **Chức năng**: Quản lý khởi tạo users với credentials và roles
- **Trách nhiệm**:
  - Tạo users mới
  - Set passwords (non-temporary)
  - Gán realm roles cho users
  - Gán client roles cho users
  - Cấu hình user attributes (enabled, emailVerified, etc.)

**Phương thức chính**:
```java
public void createUsers(RealmResource realmResource, List<UserConfig> userConfigs)
```

**Phương thức hỗ trợ**:
```java
private void setUserPassword(RealmResource realmResource, String userId, String password)
private void assignRealmRoles(RealmResource realmResource, String userId, List<String> roleNames)
private void assignClientRoles(RealmResource realmResource, String userId, Map<String, List<String>> clientRoles)
```

## Luồng hoạt động

```
KeycloakDataInitializer (CommandLineRunner)
    ↓
KeycloakInitializationService (Orchestrator)
    ↓
    ├─→ MasterRealmConnectionService (Kết nối master realm)
    │
    └─→ Cho mỗi realm config:
        ├─→ RealmInitializationService (Tạo/lấy realm)
        ├─→ RoleInitializationService (Tạo realm roles)
        ├─→ ClientInitializationService (Tạo clients)
        │       ↓
        │       └─→ RoleInitializationService (Tạo client roles)
        │
        └─→ UserInitializationService (Tạo users + assign roles)
```

## Dependency Graph

```
KeycloakInitializationService
    ├── MasterRealmConnectionService
    ├── RealmInitializationService
    ├── RoleInitializationService
    ├── ClientInitializationService
    │       └── RoleInitializationService
    └── UserInitializationService
```

## Tính năng chung

### Idempotency
Tất cả services đều được thiết kế để **idempotent** - có thể chạy nhiều lần mà không tạo duplicate:
- Kiểm tra resource đã tồn tại trước khi tạo
- Nếu đã tồn tại → Log info và bỏ qua
- Nếu chưa có → Tạo mới

### Error Handling
- Sử dụng try-catch cho từng operation
- Log chi tiết errors với context
- Không throw exception để không block các operations khác
- Continue với resources tiếp theo nếu một resource fail

### Logging
- Log level INFO cho operations thành công
- Log level WARN cho situations có thể bỏ qua (role not found, etc.)
- Log level ERROR cho failures nghiêm trọng
- Include context information trong mọi log message

## Testing

### Unit Testing
Mỗi service có thể test riêng biệt:
```java
@Mock
private Keycloak keycloak;

@Mock
private RealmResource realmResource;

@InjectMocks
private RealmInitializationService realmInitializationService;
```

### Integration Testing
Test toàn bộ flow với Keycloak testcontainers:
```java
@Container
private static KeycloakContainer keycloak = new KeycloakContainer();
```

## Mở rộng

### Thêm service mới
1. Tạo service trong package này
2. Inject vào `KeycloakInitializationService`
3. Gọi service trong method `initializeRealm()`

Ví dụ: Thêm `GroupInitializationService`:
```java
@Service
public class GroupInitializationService {
    public void createGroups(RealmResource realmResource, List<GroupConfig> groupConfigs) {
        // Implementation
    }
}
```

### Thêm chức năng cho service hiện tại
- Thêm method mới vào service tương ứng
- Update interface nếu cần
- Maintain idempotency và error handling patterns

## Best Practices

1. **Single Responsibility**: Mỗi service chỉ quản lý một loại resource
2. **Dependency Injection**: Sử dụng constructor injection với `@RequiredArgsConstructor`
3. **Logging**: Log đầy đủ với context phù hợp
4. **Error Handling**: Graceful degradation, không fail toàn bộ process
5. **Configuration**: Tất cả config từ `KeycloakInitConfig`, không hardcode
6. **Idempotency**: Luôn check exist trước khi create
7. **Documentation**: Javadoc cho mọi public method

## Troubleshooting

### Service không tạo được resource
1. Check logs của service cụ thể
2. Verify Keycloak connection (MasterRealmConnectionService)
3. Check permissions của admin user (dev/dev)
4. Verify configuration trong application.yaml

### Resources đã tồn tại nhưng vẫn báo lỗi
- Check exact match của tên resources
- Verify realm context đúng
- Check logs để xác định operation nào fail

### Performance issues
- Giảm số lượng users khởi tạo
- Tối ưu số lượng roles
- Consider batch operations cho large datasets

