# Zone Service Package Diagram

This diagram illustrates the high-level package structure of the Zone Service.

```mermaid
packageDiagram
    package "Zone Service" {
        [Node.js/TypeScript Application (src, index.ts)]
        [Package Configuration (package.json)]
        [TypeScript Configuration (tsconfig.json)]
        [Prisma ORM (prisma.ts)]
        [Dockerfile]
    }
```