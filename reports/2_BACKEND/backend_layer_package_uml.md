# Backend Layer Package Diagram

This diagram illustrates the package structure of the Backend Layer, including the API Gateway, Microservices, and Shared Components.

```mermaid
packageDiagram
    package "Backend Layer" {
        package "API Gateway" {
            [API Gateway Service]
        }
        package "Microservices" {
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

    "API Gateway Service" --> "Microservices"
    "Microservices" --> "Shared Components"
```