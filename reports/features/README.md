# Features Documentation

This folder documents end-to-end features for every user persona. Each sub-folder contains feature documentation with detailed activity diagrams, sequence diagrams, and implementation notes.

## Table of Contents

- [Persona Folders](#persona-folders)
- [Feature Organization](#feature-organization)
- [Related Documentation](#related-documentation)

## Persona Folders

- **[Admin Features](admin/README.md)** – Web management console flows (operations, monitoring, confirmation)
- **[Shipper Features](shipper/README.md)** – Android Delivery App flows (sessions, scanning, navigation, chat)
- **[Client Features](client/README.md)** – Customer-facing flows (parcel create, tracking, confirmation & disputes)

## Feature Organization

Each persona folder follows this structure:

```
features/
├── admin/
│   ├── README.md              # Index of all admin features
│   ├── confirm-delivery.md    # Individual feature documentation
│   ├── approve-postpone.md
│   ├── manage-parcels.md
│   └── ...
├── shipper/
│   ├── README.md
│   ├── scan-accept-parcel.md
│   ├── complete-task.md
│   └── ...
└── client/
    ├── README.md
    ├── create-parcel.md
    ├── confirm-delivery.md
    └── ...
```

Each feature file includes:
- Feature name, version, module, and related documentation links
- Overview description
- Activity diagram showing high-level flow
- Sequence diagram showing detailed component interactions
- Implementation notes with code references
- API references with links to detailed endpoint documentation
- Code references with file paths

## Related Documentation

- [System Analysis](../SYSTEM_ANALYSIS.md) - Comprehensive system analysis and technical assessment
- [Backend Services](../2_BACKEND/) - Detailed documentation for each backend service
- [API Documentation](../3_APIS_AND_FUNCTIONS/README.md) - Complete API endpoint documentation
- [Client Applications](../1_CLIENTS/) - Client application architecture and components
