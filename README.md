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
