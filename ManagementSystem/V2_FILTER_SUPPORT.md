# V2 Filter System Support in Frontend

## Summary

The frontend now supports the V2 filter system! The frontend can automatically convert V1 filter format (used in UI components) to V2 format (required by backend V2 APIs).

## Current Status

### ✅ Users Module
- **V2 API Support**: ✅ Implemented
- **V2 Filter Conversion**: ✅ Implemented
- **Default**: Uses V2 API by default (`useV2Api = true`)
- **Filter Conversion**: Automatically converts V1 `FilterGroup` to V2 `FilterGroupItemV2` when calling V2 endpoints

### ⚠️ Settings Module
- **V2 API Support**: ✅ Added (`listSettingsV2` function)
- **V2 Filter Conversion**: ⚠️ Not yet implemented (Settings module doesn't use advanced filters yet)
- **Status**: V2 API endpoint is ready, but Settings module currently only uses simple search (not filter groups)

## How It Works

### V1 Filter Format (UI)
The frontend UI components use V1 filter format:
```typescript
{
  logic: 'AND' | 'OR',
  conditions: [
    { field: 'status', operator: 'eq', value: 'ACTIVE' },
    { field: 'age', operator: 'gte', value: 18 }
  ]
}
```

### V2 Filter Format (Backend API)
The V2 backend expects this format:
```typescript
{
  type: 'group',
  items: [
    { type: 'condition', field: 'status', operator: 'EQUALS', value: 'ACTIVE' },
    { type: 'operator', value: 'AND' },
    { type: 'condition', field: 'age', operator: 'GREATER_THAN_OR_EQUAL', value: 18 }
  ]
}
```

### Automatic Conversion
When `useV2Api` is enabled, the frontend automatically converts V1 format to V2 format using `convertV1ToV2Filter()` utility function.

## Files Created/Modified

### New Files
1. **`src/common/types/filter-v2.ts`**
   - TypeScript types for V2 filter system
   - `FilterItemV2`, `FilterConditionItemV2`, `FilterOperatorItemV2`, `FilterGroupItemV2`
   - Type guards for type checking

2. **`src/common/utils/filter-v2-converter.ts`**
   - Conversion utilities between V1 and V2 filter formats
   - `convertV1ToV2Filter()` - Converts V1 FilterGroup to V2 FilterGroupItemV2
   - `convertV2ToV1Filter()` - Converts V2 back to V1 (for display)
   - Operator mapping between formats

### Modified Files
1. **`src/modules/Users/composables/useUsers.ts`**
   - Added V2 filter conversion in `loadUsers()` method
   - Automatically converts filters when `useV2Api.value === true`

2. **`src/modules/Users/api.ts`**
   - Already had `getUsersV2()` function (from previous implementation)

3. **`src/modules/Settings/api.ts`**
   - Added `listSettingsV2()` function for V2 API support

## Usage Examples

### Users Module
```typescript
const { useV2Api, filters, loadUsers } = useUsers()

// V2 API is enabled by default
useV2Api.value = true

// Set V1 filter format (UI format)
filters.value = {
  logic: 'AND',
  conditions: [
    { field: 'status', operator: 'eq', value: 'ACTIVE' },
    { field: 'age', operator: 'gte', value: 18 }
  ]
}

// When loadUsers() is called, filters are automatically converted to V2 format
// and sent to /api/v2/users endpoint
loadUsers()
```

### Settings Module
```typescript
// Settings module currently doesn't use filter groups
// It only uses simple search
// When V2 filter support is needed, follow the same pattern as Users module
```

## Operator Mapping

The converter automatically maps operators between formats:

| V1 Format | V2 Format |
|-----------|-----------|
| `eq` / `equals` | `EQUALS` |
| `ne` / `not_equals` | `NOT_EQUALS` |
| `gt` / `greater_than` | `GREATER_THAN` |
| `gte` / `greater_than_or_equal` | `GREATER_THAN_OR_EQUAL` |
| `lt` / `less_than` | `LESS_THAN` |
| `lte` / `less_than_or_equal` | `LESS_THAN_OR_EQUAL` |
| `contains` | `CONTAINS` |
| `startsWith` / `starts_with` | `STARTS_WITH` |
| `endsWith` / `ends_with` | `ENDS_WITH` |
| `regex` | `REGEX` |
| `in` | `IN` |
| `notIn` / `not_in` | `NOT_IN` |
| `between` | `BETWEEN` |
| `isNull` / `is_null` | `IS_NULL` |
| `isNotNull` / `is_not_null` | `IS_NOT_NULL` |

## Testing

### Test V2 Filter Conversion
```typescript
import { convertV1ToV2Filter } from '@/common/utils/filter-v2-converter'
import { createEmptyFilterGroup } from '@/common/utils/query-builder'

const v1Filter = {
  logic: 'AND',
  conditions: [
    { field: 'status', operator: 'eq', value: 'ACTIVE' },
    { field: 'age', operator: 'gte', value: 18 }
  ]
}

const v2Filter = convertV1ToV2Filter(v1Filter)
console.log(v2Filter)
// Output:
// {
//   type: 'group',
//   items: [
//     { type: 'condition', field: 'status', operator: 'EQUALS', value: 'ACTIVE' },
//     { type: 'operator', value: 'AND' },
//     { type: 'condition', field: 'age', operator: 'GREATER_THAN_OR_EQUAL', value: 18 }
//   ]
// }
```

### Test in Browser
1. Navigate to Users page
2. Open browser DevTools → Network tab
3. Apply filters in the UI
4. Check the request payload to `/api/v2/users`
5. Verify filters are in V2 format

## Future Enhancements

1. **Native V2 Filter UI**: Create UI components that work directly with V2 filter format (for advanced use cases)

2. **Settings Module Filter Support**: Add full filter support to Settings module with V2 conversion

3. **V2 Filter Builder**: Create a visual filter builder that supports mixed AND/OR operations (V2's main advantage)

4. **Bidirectional Conversion**: Improve `convertV2ToV1Filter()` to handle complex V2 filters with mixed operators better

## Notes

- The frontend UI continues to use V1 filter format for simplicity
- V2 conversion happens automatically when needed
- V1 API endpoints still work for backward compatibility
- V2 API endpoints provide enhanced filtering capabilities (mixed AND/OR operations)

## Related Documentation

- Backend V2 Filter Guide: `/BE/FILTER_SYSTEM_V0_V1_V2_GUIDE.md`
- API Quick Reference: `/FILTER_API_QUICK_REFERENCE.md`
- Implementation Status: `/IMPLEMENTATION_STATUS.md`


