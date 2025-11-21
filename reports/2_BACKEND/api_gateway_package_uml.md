# API Gateway Package Diagram

This diagram illustrates the high-level package structure of the API Gateway service.

```mermaid
packageDiagram
    package "API Gateway" {
        [Nginx Configuration (nginx.conf)]
        [Spring Boot Application (src, pom.xml)]
        [Dockerfile]
    }
```