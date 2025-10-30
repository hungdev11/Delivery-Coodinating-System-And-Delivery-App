# User Service Documentation

Welcome to the complete documentation for the User Service.

This service is responsible for managing all user-related data and operations, including authentication, roles, and user profiles. It features a powerful dynamic query system for advanced data filtering and retrieval.

## 📚 Table of Contents

1.  **[Architecture](./ARCHITECTURE.md)**
    *   An overview of the service's structure, components, and design principles.

2.  **[API Reference](./API.md)**
    *   Detailed documentation for all API endpoints, including request/response formats and examples.

3.  **[Dynamic Query System](./DYNAMIC_QUERY.md)**
    *   A deep dive into the advanced filtering, sorting, and pagination capabilities of the service.

4.  **[Setup and Deployment](./SETUP.md)**
    *   Instructions for setting up the development environment, running the service, and deploying to production.

## Notes

*   All APIs follow the `BaseResponse<T>` contract.
*   Pagination is zero-based and follows the `Paging/PagedData` types.
*   See global RESTful standards at `../../RESTFUL.md`.