# Delivery-Coodinating-System-And-Delivery-App

## Build Status

![Backend CI/CD](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/workflows/Backend%20CI%2FCD%20Pipeline/badge.svg)
![Frontend CI/CD](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/workflows/Frontend%20CI%2FCD%20Pipeline/badge.svg)
![Android App CI/CD](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/workflows/Android%20App%20CI%2FCD%20Pipeline/badge.svg)

## Downloads & Packages

### Android App

[![Download APK](https://img.shields.io/badge/Download-APK-green.svg)](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/releases/latest)
[![Download AAB](https://img.shields.io/badge/Download-AAB-blue.svg)](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/releases/latest)

Download the latest Android app from [GitHub Releases](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/releases/latest).

### Docker Images

All Docker images are available on [GitHub Container Registry](https://github.com/hungdev11?tab=packages).

#### Backend Services

- **API Gateway**: [`ghcr.io/hungdev11/dss-api-gateway`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-api-gateway)
- **User Service**: [`ghcr.io/hungdev11/dss-user-service`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-user-service)
- **Settings Service**: [`ghcr.io/hungdev11/dss-settings-service`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-settings-service)
- **Zone Service**: [`ghcr.io/hungdev11/dss-zone-service`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-zone-service)
- **Parcel Service**: [`ghcr.io/hungdev11/dss-parcel-service`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-parcel-service)
- **Session Service**: [`ghcr.io/hungdev11/dss-session-service`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-session-service)
- **Communication Service**: [`ghcr.io/hungdev11/dss-communication-service`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-communication-service)

#### Frontend

- **Management System**: [`ghcr.io/hungdev11/dss-management-system`](https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/pkgs/container/dss-management-system)

**Usage Example:**
```bash
docker pull ghcr.io/hungdev11/dss-api-gateway:latest
docker pull ghcr.io/hungdev11/dss-management-system:latest
```

### Quick start:

- Clone the repository
- Create .env file in the root directory
```bash
cp env.local.example env.local
```
- Add zone service database .env file
```bash
cp BE/zone_service/.env.example BE/zone_service/.env
```
- (Optional) Seed zone service database
```bash
cd BE/zone_service
npm run seed
```
- Create osrm data
```bash
cd BE/zone_service
npm run osrm:generate
```

- (Optional) Add cloudflare tunnel config
```bash
cd cloudflare
cp config.yml.example config.yml
```
> **Required**: You will need a cloudflare tunnel id and credentials file.

- Start the services
```bash
cd ../.. # Back to root directory
docker-compose up -d
```

**Note:** In normal mode, only the Nginx port (`8080`) is exposed. All service ports are only accessible via Docker network or through Nginx reverse proxy.

### Debug Mode

To enable remote debugging and expose service ports:

```bash
docker-compose -f docker-compose.yml -f docker-compose.debug.yml up -d
```

**Service Ports (exposed in debug mode):**
- Settings Service: `21502`
- User Service: `21501`
- Zone Service: `21503`
- Parcel Service: `21506`
- Session Service: `21505`
- Communication Service: `21511`
- API Gateway: `21500`

**Debug Ports:**
- Settings Service: `5005`
- User Service: `5006`
- Zone Service (Node.js): `9229`
- Parcel Service: `5007`
- Session Service: `5008`
- Communication Service: `5009`
- API Gateway: `5010`

**IDE Configuration:**
- Connect your IDE debugger to `localhost:<debug-port>`
- Use "Remote JVM Debug" configuration in IntelliJ IDEA or VS Code
- Ensure `suspend=n` in JAVA_TOOL_OPTIONS (services start without waiting for debugger)

### Documentation Map

- **Backend overview**: [BE/README.md](BE/README.md)

- **Services**
  - **API Gateway**
    - [Docs README](BE/api-gateway/.docs/README.md)
    - [Routes folder](BE/api-gateway/.docs/route/)
  - **User Service**
    - [Docs README](BE/User_service/.docs/README.md)
    - [Routes folder](BE/User_service/.docs/route/)
  - **Settings Service**
    - [Docs README](BE/Settings_service/.docs/README.md)
    - [Routes folder](BE/Settings_service/.docs/route/)
  - **Zone Service**
    - [Docs README](BE/zone_service/.docs/README.md)
    - [Routes folder](BE/zone_service/.docs/route/)

- **Environment**
  - Root env file: [env.local](env.local)
