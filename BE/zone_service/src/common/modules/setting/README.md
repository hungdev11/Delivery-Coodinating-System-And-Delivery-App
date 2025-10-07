# Settings Module - Zone Service

Module tích hợp với Settings Service API để quản lý cài đặt hệ thống, cấu hình và feature flags trong Zone Service.

## Cài đặt

Module này sử dụng các dependencies sau:
- `axios`: HTTP client để gọi API
- `dotenv`: Để đọc environment variables

```bash
npm install axios dotenv
```

## Cấu hình

### Environment Variables

Tạo file `.env` trong thư mục root của zone_service:

```env
# Settings Service Configuration
SETTINGS_SERVICE_URL=http://localhost:8080
SETTINGS_SERVICE_TIMEOUT=10000
SETTINGS_SERVICE_API_KEY=your-api-key-here
NODE_ENV=development
```

### Cấu hình theo Environment

- **Development**: `http://localhost:8080`
- **Staging**: `http://settings-service-staging:8080`
- **Production**: `http://settings-service:8080`

## Sử dụng

### 1. Import Module

```typescript
import {
  createSettingService,
  SettingService,
  ZONE_SETTING_KEYS,
  ZONE_SETTING_GROUPS
} from './common/modules/setting';
```

### 2. Tạo Service Instance

```typescript
// Sử dụng cấu hình mặc định
const settingsService = createSettingService();

// Hoặc với cấu hình tùy chỉnh
const customService = createSettingService({
  baseUrl: 'http://custom-settings-service:8080',
  timeout: 5000
});
```

### 3. Đọc Settings

```typescript
// Lấy giá trị setting với default
const radius = await settingsService.getSettingValueOrDefault(
  ZONE_SETTING_KEYS.DEFAULT_ZONE_RADIUS,
  '1000'
);

// Lấy setting với type conversion
const setting = await settingsService.getSettingByKey(ZONE_SETTING_KEYS.MAX_ZONE_RADIUS);
const maxRadius = setting.getValueAsInteger();

// Lấy tất cả settings trong group
const zoneConfigs = await settingsService.getSettingsByGroup(ZONE_SETTING_GROUPS.ZONE_CONFIG);
```

### 4. Tạo và Cập nhật Settings

```typescript
// Tạo setting mới
const newSetting = await settingsService.createSetting({
  key: 'custom_setting',
  group: ZONE_SETTING_GROUPS.ZONE_CONFIG,
  description: 'Custom setting',
  type: SettingType.STRING,
  value: 'value',
  level: SettingLevel.SYSTEM
});

// Cập nhật setting
const updatedSetting = await settingsService.updateSetting(
  'custom_setting',
  { value: 'new_value' },
  'user-id'
);
```

## API Reference

### SettingService Class

#### Methods

- `getAllSettings()`: Lấy tất cả settings
- `getSettingByKey(key)`: Lấy setting theo key
- `getSettingValue(key)`: Lấy giá trị setting theo key
- `getSettingValueOrDefault(key, defaultValue)`: Lấy giá trị với fallback
- `getSettingsByGroup(group)`: Lấy settings theo group
- `getSettingByKeyAndGroup(key, group)`: Lấy setting theo key và group
- `searchSettings(query)`: Tìm kiếm settings
- `createSetting(request)`: Tạo setting mới
- `updateSetting(key, request, userId?)`: Cập nhật setting
- `deleteSetting(key)`: Xóa setting
- `settingExists(key)`: Kiểm tra setting có tồn tại

### SettingModel Class

#### Methods

- `getValueAsBoolean()`: Chuyển đổi giá trị thành boolean
- `getValueAsNumber()`: Chuyển đổi giá trị thành number
- `getValueAsInteger()`: Chuyển đổi giá trị thành integer
- `getValueAsJson()`: Parse giá trị thành JSON
- `isSystemLevel()`: Kiểm tra setting level
- `toJson()`: Lấy raw data

## Zone Service Settings

### Setting Keys

```typescript
export const ZONE_SETTING_KEYS = {
  DEFAULT_ZONE_RADIUS: 'DEFAULT_ZONE_RADIUS',
  MAX_ZONE_RADIUS: 'MAX_ZONE_RADIUS',
  MIN_ZONE_RADIUS: 'MIN_ZONE_RADIUS',
  DEFAULT_CENTER_LATITUDE: 'DEFAULT_CENTER_LATITUDE',
  DEFAULT_CENTER_LONGITUDE: 'DEFAULT_CENTER_LONGITUDE',
  ZONE_CACHE_TTL: 'ZONE_CACHE_TTL',
  MAX_ZONES_PER_REQUEST: 'MAX_ZONES_PER_REQUEST',
  ENABLE_ZONE_VALIDATION: 'ENABLE_ZONE_VALIDATION',
  ENABLE_GEOCODING: 'ENABLE_GEOCODING',
  MAPS_API_KEY: 'MAPS_API_KEY',
  GEOCODING_SERVICE_URL: 'GEOCODING_SERVICE_URL'
};
```

### Setting Groups

```typescript
export const ZONE_SETTING_GROUPS = {
  ZONE_CONFIG: 'zone_config',
  CENTER_CONFIG: 'center_config',
  PERFORMANCE: 'performance',
  FEATURE_FLAGS: 'feature_flags',
  INTEGRATIONS: 'integrations'
};
```

## Error Handling

```typescript
try {
  const value = await settingsService.getSettingValue('some_key');
} catch (error) {
  if (error.message.includes('not found')) {
    // Setting không tồn tại
    console.log('Setting not found, using default');
  } else {
    // Lỗi khác
    console.error('Settings service error:', error);
  }
}
```

## Caching

Settings Service sử dụng caching ở server-side. Để đảm bảo dữ liệu mới nhất:

```typescript
// Cache sẽ tự động invalidate khi update/delete
await settingsService.updateSetting('key', { value: 'new_value' });

// Hoặc có thể refresh toàn bộ cache
await settingsService.refreshCache(); // Nếu có method này
```

## Best Practices

1. **Sử dụng default values**: Luôn cung cấp giá trị mặc định khi đọc settings
2. **Error handling**: Xử lý lỗi khi settings service không available
3. **Type conversion**: Sử dụng các method type conversion của SettingModel
4. **Grouping**: Nhóm các settings liên quan theo group
5. **Environment config**: Sử dụng environment variables cho configuration

## Examples

Xem file `setting.example.ts` để có các ví dụ chi tiết về cách sử dụng module này.
