# Style Guide

## Overview
This document defines the design system and styling guidelines for the Management System UI. The project uses Nuxt UI v3 with Tailwind CSS v4 for a modern, consistent user experience.

## Design Principles

### 1. Consistency
- Use predefined components from the design system
- Maintain consistent spacing, typography, and color usage
- Follow established patterns for layouts and interactions

### 2. Clarity
- Clear visual hierarchy
- Intuitive navigation
- Meaningful labels and messaging

### 3. Efficiency
- Minimize clicks to accomplish tasks
- Provide shortcuts and quick actions
- Optimize for common workflows

### 4. Responsiveness
- Mobile-first approach
- Adaptive layouts for all screen sizes
- Touch-friendly interfaces

## Color System

### Primary Colors
```typescript
const colors = {
  primary: {
    50: '#f0f9ff',
    100: '#e0f2fe',
    200: '#bae6fd',
    300: '#7dd3fc',
    400: '#38bdf8',
    500: '#0ea5e9',  // Main brand color
    600: '#0284c7',
    700: '#0369a1',
    800: '#075985',
    900: '#0c4a6e',
  }
}
```

### Semantic Colors
- **Success**: Green tones for positive actions
- **Warning**: Amber/yellow tones for caution
- **Error**: Red tones for destructive actions or errors
- **Info**: Blue tones for informational messages
- **Neutral**: Gray tones for general UI elements

### Usage Guidelines
- Use primary color for CTAs and important actions
- Use semantic colors to convey meaning
- Maintain sufficient contrast ratios (WCAG AA minimum)
- Use color purposefully, not decoratively

## Typography

### Font Family
- **Primary**: System font stack for optimal performance
- **Monospace**: For code snippets and technical data

### Type Scale
```css
/* Headings */
h1: 2.25rem (36px) - font-bold
h2: 1.875rem (30px) - font-bold
h3: 1.5rem (24px) - font-semibold
h4: 1.25rem (20px) - font-semibold
h5: 1.125rem (18px) - font-medium
h6: 1rem (16px) - font-medium

/* Body */
base: 1rem (16px) - font-normal
sm: 0.875rem (14px) - font-normal
xs: 0.75rem (12px) - font-normal
```

### Usage Guidelines
- Use headings to establish hierarchy
- Keep body text at comfortable reading sizes
- Use font weights to emphasize important information
- Limit line length to 60-80 characters for readability

## Spacing System

### Scale
Based on 4px baseline grid:
```typescript
const spacing = {
  0: '0',
  1: '0.25rem',  // 4px
  2: '0.5rem',   // 8px
  3: '0.75rem',  // 12px
  4: '1rem',     // 16px
  5: '1.25rem',  // 20px
  6: '1.5rem',   // 24px
  8: '2rem',     // 32px
  10: '2.5rem',  // 40px
  12: '3rem',    // 48px
  16: '4rem',    // 64px
}
```

### Usage Guidelines
- Use consistent spacing between elements
- Larger spacing for sections, smaller for related items
- Maintain visual breathing room

## Layout

### Grid System
- 12-column responsive grid
- Gutters: 24px (desktop), 16px (mobile)
- Max width: 1280px for main content

### Breakpoints
```typescript
const breakpoints = {
  sm: '640px',   // Small devices
  md: '768px',   // Medium devices
  lg: '1024px',  // Large devices
  xl: '1280px',  // Extra large
  '2xl': '1536px' // 2X large
}
```

### Common Layouts
1. **Dashboard Layout**: Sidebar + Main content
2. **List View**: Header + Filters + Table + Pagination
3. **Detail View**: Header + Tabs + Form/Content
4. **Modal/Drawer**: Overlay with actions

## Components

### Buttons
```vue
<!-- Primary Action -->
<UButton color="primary" variant="solid">Save</UButton>

<!-- Secondary Action -->
<UButton color="neutral" variant="outline">Cancel</UButton>

<!-- Destructive Action -->
<UButton color="error" variant="soft">Delete</UButton>

<!-- Icon Button -->
<UButton icon="i-heroicons-pencil" variant="ghost" />
```

### Forms
```vue
<!-- Form Field -->
<UFormField label="Email" name="email" required>
  <UInput v-model="form.email" placeholder="Enter email" />
</UFormField>

<!-- Select -->
<UFormField label="Status" name="status">
  <USelect v-model="form.status" :options="statusOptions" />
</UFormField>
```

### Tables
```vue
<UTable
  :columns="columns"
  :data="rows"
  :loading="loading"
/>
```

### Cards
```vue
<UCard>
  <template #header>
    <h3>Card Title</h3>
  </template>
  <p>Card content goes here</p>
  <template #footer>
    <UButton>Action</UButton>
  </template>
</UCard>
```

## Icons

### Icon System
Using Iconify with Heroicons as primary icon set

### Guidelines
- Use icons to enhance understanding, not replace text
- Maintain consistent icon sizes within contexts
- Provide accessible labels for icon-only buttons

## Accessibility

### Requirements
- Semantic HTML structure
- ARIA labels where appropriate
- Keyboard navigation support
- Focus indicators
- Sufficient color contrast
- Screen reader compatibility

### Best Practices
```vue
<!-- Accessible Button -->
<UButton aria-label="Delete user" icon="i-heroicons-trash" />

<!-- Accessible Form -->
<UFormField
  label="Username"
  name="username"
  required
  help="Must be at least 3 characters"
/>
```

## Animation & Motion

### Principles
- Use subtle animations to guide attention
- Respect user's motion preferences
- Keep animations under 300ms for UI feedback

### Common Transitions
```css
/* Fade */
transition: opacity 200ms ease-in-out;

/* Slide */
transition: transform 250ms ease-out;

/* Scale */
transition: scale 200ms cubic-bezier(0.4, 0, 0.2, 1);
```

## Code Style

### Vue Component Structure
```vue
<script setup lang="ts">
// 1. Imports
import { ref, computed, onMounted } from 'vue'
import type { User } from '@/common/types'

// 2. Props & Emits
interface Props {
  userId: string
}
const props = defineProps<Props>()
const emit = defineEmits<{
  save: [user: User]
}>()

// 3. Composables
const toast = useToast()

// 4. State
const user = ref<User | null>(null)
const loading = ref(false)

// 5. Computed
const displayName = computed(() =>
  user.value ? `${user.value.firstName} ${user.value.lastName}` : ''
)

// 6. Methods
async function loadUser() {
  loading.value = true
  try {
    // Load user logic
  } finally {
    loading.value = false
  }
}

// 7. Lifecycle
onMounted(() => {
  loadUser()
})
</script>

<template>
  <div class="space-y-4">
    <!-- Template content -->
  </div>
</template>
```

### Naming Conventions
- **Components**: PascalCase (e.g., `UserForm.vue`)
- **Composables**: camelCase with `use` prefix (e.g., `useUsers`)
- **Types**: PascalCase (e.g., `UserDto`, `ApiResponse`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_PAGE_SIZE`)
- **Variables/Functions**: camelCase (e.g., `fetchUsers`, `userId`)

### File Organization
```
src/
├── modules/
│   └── users/
│       ├── components/          # Module-specific components
│       ├── composables/          # Module-specific composables
│       ├── types/                # Module-specific types
│       ├── api.ts                # API client functions
│       ├── routes.ts             # Route definitions
│       └── UsersView.vue         # Main view
├── common/
│   ├── components/               # Shared components
│   ├── composables/              # Shared composables
│   ├── types/                    # Shared types
│   └── utils/                    # Utility functions
└── layouts/                      # Layout components
```

## Documentation

### Component Documentation
Every component should have:
1. **Purpose**: What the component does
2. **Props**: Description of each prop
3. **Emits**: Events the component emits
4. **Slots**: Available slots
5. **Examples**: Usage examples

### Example
```vue
<script setup lang="ts">
/**
 * UserCard Component
 *
 * Displays user information in a card format with avatar and actions
 *
 * @example
 * ```vue
 * <UserCard :user="user" @edit="handleEdit" @delete="handleDelete" />
 * ```
 */

interface Props {
  /** User object to display */
  user: User
  /** Whether to show action buttons */
  showActions?: boolean
}
</script>
```

## Error Handling

### User-Facing Messages
- **Success**: "User created successfully"
- **Error**: "Failed to create user. Please try again."
- **Validation**: "Email is required"
- **Not Found**: "User not found"

### Toast Notifications
```typescript
// Success
toast.add({
  title: 'Success',
  description: 'User created successfully',
  color: 'success'
})

// Error
toast.add({
  title: 'Error',
  description: 'Failed to create user',
  color: 'error'
})
```

## Performance

### Best Practices
1. Lazy load routes and components
2. Use virtual scrolling for large lists
3. Debounce search inputs
4. Cache API responses when appropriate
5. Optimize images and assets
6. Code splitting by route

### Example
```typescript
// Lazy load route component
{
  path: '/users',
  component: () => import('@/modules/users/UsersView.vue')
}

// Debounce search
const debouncedSearch = useDebounceFn((query: string) => {
  searchUsers(query)
}, 300)
```

## Testing Strategy

While unit tests are not required per project requirements, consider:
- Manual testing checklist
- Cross-browser compatibility testing
- Accessibility testing
- Performance testing

## Resources

- [Nuxt UI Documentation](https://ui.nuxt.com)
- [Tailwind CSS Documentation](https://tailwindcss.com)
- [Vue 3 Documentation](https://vuejs.org)
- [Web Accessibility Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
