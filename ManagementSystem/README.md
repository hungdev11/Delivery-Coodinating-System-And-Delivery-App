# Management System - Frontend

A modern, scalable management system built with **Vue 3**, **Nuxt UI v3**, and **TypeScript** following best practices and clean architecture principles.

## Tech Stack

- **[Vue 3](https://vuejs.org/)** - Progressive JavaScript framework
- **[Vue Router](https://router.vuejs.org/)** - Official router for Vue.js
- **[Pinia](https://pinia.vuejs.org/)** - State management
- **[VueUse](https://vueuse.org/)** - Collection of Vue Composition utilities
- **[Nuxt UI v3](https://ui.nuxt.com/)** - Modern UI component library
- **[Tailwind CSS v4](https://tailwindcss.com/)** - Utility-first CSS framework
- **[Track-Asia Map](https://www.track-asia.com)** - Map display and interaction
- **[Axios](https://axios-http.com/)** - HTTP client
- **[TypeScript](https://www.typescriptlang.org/)** - Type safety

## Project Structure

```
ManagementSystem/
├── .docs/                      # Project documentation
│   └── STYLE_GUIDE.md         # Design system and coding standards
├── src/
│   ├── common/                # Shared resources
│   │   ├── component/         # Reusable UI components
│   │   ├── config/            # App configuration
│   │   ├── guards/            # Route guards
│   │   ├── store/             # Pinia stores
│   │   ├── types/             # Shared TypeScript types
│   │   └── utils/             # Utility functions
│   ├── layouts/               # Layout components
│   │   ├── DefaultLayout.vue  # Main app layout
│   │   └── BlankLayout.vue    # Minimal layout
│   ├── modules/               # Feature modules
│   │   ├── LoginScreen/       # Authentication module
│   │   ├── Users/             # User management module
│   │   │   ├── components/    # Module-specific components
│   │   │   ├── composables/   # Module composables
│   │   │   ├── api.ts         # API client
│   │   │   ├── model.type.ts  # Type definitions
│   │   │   ├── Users.routes.ts # Route definitions
│   │   │   ├── UsersView.vue  # List view
│   │   │   ├── UserDetailView.vue # Detail view
│   │   │   └── README.md      # Module documentation
│   │   └── common/            # Common pages (Home, 404, etc.)
│   ├── router/                # Router configuration
│   ├── assets/                # Static assets
│   ├── App.vue                # Root component
│   └── main.ts                # Application entry point
├── public/                    # Public static files
└── ...config files
```

## Architecture Principles

### Module-Based Architecture

Each feature is organized as a self-contained module with:
- **Components**: Module-specific UI components
- **Composables**: Reusable business logic
- **API**: HTTP client functions
- **Types**: TypeScript definitions
- **Routes**: Route configuration
- **Documentation**: Module README

### Separation of Concerns

- **UI Layer**: Vue components (presentation)
- **Logic Layer**: Composables (business logic)
- **Data Layer**: API clients (data fetching)
- **Type Layer**: TypeScript types (contracts)

### Best Practices

1. **Composition API**: Use `<script setup>` for all components
2. **TypeScript**: Strong typing for all code
3. **Composables**: Extract reusable logic
4. **Props/Emits**: Type-safe component communication
5. **useOverlay**: Programmatic modals (Nuxt UI v3 pattern)
6. **Auto-imports**: Leverage Vue/Nuxt auto-imports
7. **Code Splitting**: Lazy load routes and components

## Key Features

### Common Components

- **DataTable**: Feature-rich data table with:
  - Pagination
  - Search
  - Bulk selection (across multiple pages)
  - Bulk actions
  - Custom columns and slots
  - Loading states

- **PageHeader**: Consistent page headers with:
  - Title and description
  - Action buttons
  - Back navigation

### User Management Module

Complete CRUD operations for users with:
- List view with pagination and search
- Detail view for individual users
- Create/Edit forms using modals
- Bulk delete and export
- Role-based access control

**File Structure:**
```
Users/
├── components/
│   ├── UserFormModal.vue      # Create/Edit modal
│   └── UserDeleteModal.vue    # Delete confirmation
├── composables/
│   ├── useUsers.ts            # User CRUD logic
│   └── useUserExport.ts       # Export functionality
├── api.ts                     # API client functions
├── model.type.ts              # Type definitions
├── Users.routes.ts            # Route configuration
├── UsersView.vue              # List view
├── UserDetailView.vue         # Detail view
└── README.md                  # Module documentation
```

**Key Patterns:**
- Composables for business logic separation
- `useOverlay()` for programmatic modals
- Detail page as separate route
- Bulk actions with cross-page selection

## Getting Started

### Prerequisites

- Node.js >= 20.19.0 or >= 22.12.0
- npm or yarn

### Installation

```bash
npm install
```

### Environment Setup

Create a `.env` file:

```env
VITE_API_URL=http://localhost:8080/api
VITE_MAP_STYLE_URL=https://api.track-asia.com/v1/styles/streets
VITE_MAP_API_KEY=your_api_key
VITE_ENV=development
```

### Development

```bash
npm run dev
```

### Build

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## Configuration

### App Config

Configuration is centralized in `src/common/config/`:

- **app.config.ts**: API endpoints, pagination, constants
- **theme.config.ts**: Design tokens (colors, typography, spacing)

### Routing

Routes are defined per module and aggregated in `src/router/index.ts`.

Each route supports:
- Authentication requirements
- Role-based access
- Layout selection
- Meta information

### State Management

Pinia stores in `src/common/store/`:
- `sidebar.store.ts`: Sidebar collapse state
- `layout.store.ts`: Layout preferences
- Module-specific stores as needed

## Styling

### Design System

Follows the design system defined in `.docs/STYLE_GUIDE.md`:

- Consistent color palette
- Typography scale
- Spacing system
- Component patterns

### Tailwind CSS

Utility-first CSS with Tailwind CSS v4. Theme customization in `app.config.ts`.

### Nuxt UI Theming

Customize Nuxt UI components through the theme configuration.

## API Integration

### HTTP Client

Axios-based client in `src/common/utils/axios.ts`:

- Automatic token injection
- Request/response interceptors
- Error handling
- Toast notifications

### Module APIs

Each module defines its own API client:

```typescript
// Example: Users API
export const getUsers = async (params) => { ... }
export const createUser = async (data) => { ... }
export const updateUser = async (id, data) => { ... }
export const deleteUser = async (id) => { ... }
```

## Authentication

JWT-based authentication with Keycloak integration:

- Token storage in cookies
- Automatic token refresh
- Role-based route guards
- Protected routes

## Code Style

### Naming Conventions

- **Components**: PascalCase (`UserFormModal.vue`)
- **Composables**: camelCase with `use` prefix (`useUsers.ts`)
- **Types**: PascalCase (`UserDto`, `ApiResponse`)
- **Constants**: UPPER_SNAKE_CASE (`API_CONFIG`)
- **Variables/Functions**: camelCase (`loadUsers`, `userId`)

### Component Structure

```vue
<script setup lang="ts">
// 1. Imports
import { ref, computed, onMounted } from 'vue'

// 2. Props & Emits
interface Props { ... }
const props = defineProps<Props>()
const emit = defineEmits<{ ... }>()

// 3. Composables
const toast = useToast()

// 4. State
const loading = ref(false)

// 5. Computed
const displayName = computed(() => ...)

// 6. Methods
function handleAction() { ... }

// 7. Lifecycle
onMounted(() => { ... })
</script>

<template>
  <!-- Template content -->
</template>
```

## Testing

While unit tests are not currently implemented, the architecture supports easy testing:

- Composables can be tested independently
- Components use dependency injection
- API clients are mockable

## Deployment

### Build for Production

```bash
npm run build
```

Outputs to `dist/` directory.

### Environment Variables

Set production environment variables:

```env
VITE_API_URL=https://api.production.com
VITE_ENV=production
```

### Docker

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 4173
CMD ["npm", "run", "preview"]
```

## Modules

### Implemented

- ✅ **Login**: Authentication and user sync
- ✅ **Users**: Complete user management with CRUD operations

### Planned

- ⏳ **Zones**: Zone management with map integration
- ⏳ **Settings**: System settings management
- ⏳ **Dashboard**: Home dashboard with analytics

## Contributing

### Adding a New Module

1. Create module directory in `src/modules/YourModule/`
2. Implement:
   - `api.ts` - API client
   - `model.type.ts` - Types
   - `composables/` - Business logic
   - `components/` - UI components
   - `YourModule.routes.ts` - Routes
   - `YourModuleView.vue` - Main view
   - `README.md` - Documentation

3. Register routes in `src/router/index.ts`
4. Update navigation in `SidebarPart.vue`

### Code Standards

- Follow the [Style Guide](.docs/STYLE_GUIDE.md)
- Use TypeScript for type safety
- Document public APIs and complex logic
- Keep components small and focused
- Extract reusable logic to composables

## Resources

- [Vue 3 Documentation](https://vuejs.org/)
- [Nuxt UI Documentation](https://ui.nuxt.com/)
- [Pinia Documentation](https://pinia.vuejs.org/)
- [VueUse Documentation](https://vueuse.org/)
- [Project Style Guide](.docs/STYLE_GUIDE.md)

## License

[Your License]

## Support

For issues and questions, contact [your contact info]
