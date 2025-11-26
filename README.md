# Delivery-Coodinating-System-And-Delivery-App

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
