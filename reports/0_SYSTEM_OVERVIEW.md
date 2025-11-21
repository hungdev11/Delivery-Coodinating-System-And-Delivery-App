# Project Folder Structure Overview

This document outlines the key directories and files within the project, providing a high-level overview of its architecture.

```
/mnt/e/graduate/DS/
├───.gitignore
├───.gitleaks.toml
├───.semgrepignore
├───bake.json
├───docker-compose.dev-linux.yml
├───docker-compose.dev.yml
├───docker-compose.prod.yml
├───docker-compose.test.yml
├───docker-compose.yml
├───env.local
├───README.md
├───RESTFUL.md
├───route.md
├───ui.md
├───websocket-test.html
├───.actrc/
│   └───event.json
├───.changelog/
│   └───0511a2211.md
├───.github/
│   └───workflows/
│       ├───android-app-ci-cd.yml
│       ├───backend-ci-cd.yml
│       ├───monitoring.yml
│       └───secret-scan.yml
├───.vscode/
│   ├───launch.json
│   ├───settings.json
│   └───tasks.json
├───BE/
│   ├───.env.example
│   ├───AUDIT_LOGGING_GUIDE.md
│   ├───FILTER_SYSTEM_V0_V1_V2_GUIDE.md
│   ├───QUERY_SYSTEM.md
│   ├───README.md
│   ├───SERVICES_UPDATE_GUIDE.md
│   ├───.docs/
│   │   └───ProjectStructure.md
│   ├───api-gateway/
│   │   ├───Dockerfile
│   │   ├───nginx.conf
│   │   ├───pom.xml
│   │   ├───README.md
│   │   └───src/
│   ├───communication_service/
│   │   ├───Dockerfile
│   │   ├───pom.xml
│   │   └───src/
│   ├───parcel-service/
│   │   ├───Dockerfile
│   │   ├───pom.xml
│   │   └───src/
│   ├───scripts/
│   │   ├───health-check.sh
│   │   └───init-databases.sql
│   ├───session-service/
│   │   ├───Dockerfile
│   │   ├───pom.xml
│   │   └───src/
│   ├───Settings_service/
│   │   ├───Dockerfile
│   │   ├───pom.xml
│   │   ├───README.md
│   │   └───src/
│   ├───User_service/
│   │   ├───Dockerfile
│   │   ├───pom.xml
│   │   ├───README.MD
│   │   └───src/
│   └───zone_service/
│       ├───Dockerfile
│       ├───index.ts
│       ├───package.json
│       ├───prisma.ts
│       ├───README.md
│       ├───tsconfig.json
│       └───src/
├───cloudflared/
│   └───config.yml.example
├───DeliveryApp/
│   ├───build.gradle
│   ├───gradle.properties
│   ├───gradlew
│   ├───gradlew.bat
│   ├───settings.gradle
│   └───app/
├───ManagementSystem/
│   ├───.prettierrc.json
│   ├───Dockerfile
│   ├───eslint.config.ts
│   ├───index.html
│   ├───package.json
│   ├───README.md
│   ├───tsconfig.json
│   ├───vite.config.ts
│   └───src/
├───reports/
│   ├───0_SYSTEM_OVERVIEW.md
│   ├───1_CLIENTS/
│   ├───2_BACKEND/
│   └───3_APIS_AND_FUNCTIONS/
└───scripts/
```