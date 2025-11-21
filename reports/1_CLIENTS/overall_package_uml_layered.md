# Overall System Package Diagram (Layered View)

This diagram illustrates the high-level architecture of the system, organized into logical layers: Client Layer, Nginx, API Gateway, and Microservices (Core Services and Shared Components).

```mermaid
packageDiagram
    package "Client Layer" {
        [DeliveryApp (Android)]
        [ManagementSystem (Web)]
    }

    package "Infrastructure" {
        [Nginx (Reverse Proxy)]
        [API Gateway Service]
    }

    package "Microservices" {
        package "Core Services" {
            [User Service]
            [Parcel Service]
            [Session Service]
            [Zone Service]
            [Settings Service]
            [Communication Service]
        }
        package "Shared Components" {
            [Common Libraries]
            [Utility Modules]
        }
    }

    "DeliveryApp (Android)" --> "Nginx (Reverse Proxy)"
    "ManagementSystem (Web)" --> "Nginx (Reverse Proxy)"
    "Nginx (Reverse Proxy)" --> "API Gateway Service"
    "API Gateway Service" --> "Core Services"
    "Core Services" --> "Shared Components"
```