# ManagementSystem (Web) Package Diagram

This diagram illustrates the high-level package structure of the ManagementSystem web application.

```mermaid
packageDiagram
    package "ManagementSystem (Web)" {
        package "src" {
            [main.ts (Entry Point)]
            [App.vue (Root Component)]
            [Router]
            [Stores (State Management)]
            [Modules (Feature Modules)]
            [Layouts]
            [Common (Utilities, Components)]
            [Assets (Images, CSS)]
        }
    }
```