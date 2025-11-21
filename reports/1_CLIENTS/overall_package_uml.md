# Overall System Package Diagram

This diagram illustrates the high-level architecture of the system, showing the main components and their relationships.

```mermaid
packageDiagram
    package "Clients" {
        [DeliveryApp (Android)]
        [ManagementSystem (Web)]
    }

    package "Backend" {
        package "API Gateway" {
            [Nginx]
            [API Gateway Service]
        }
        package "Microservices" {
            [Communication Service]
            [Parcel Service]
            [Session Service]
            [Settings Service]
            [User Service]
            [Zone Service]
        }
    }

    "DeliveryApp (Android)" --> "API Gateway"
    "ManagementSystem (Web)" --> "API Gateway"
    "API Gateway" --> "Microservices"
    "API Gateway Service" --> "Communication Service"
    "API Gateway Service" --> "Parcel Service"
    "API Gateway Service" --> "Session Service"
    "API Gateway Service" --> "Settings Service"
    "API Gateway Service" --> "User Service"
    "API Gateway Service" --> "Zone Service"
```