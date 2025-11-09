# Development Mode - Quick Guide

Có 2 file Docker Compose cho development, chọn theo OS:

## 📦 File nào cho OS nào?

| File | Platform | Cách hoạt động |
|------|----------|----------------|
| `docker-compose.dev.yml` | **Mac/Windows** ✅ | Dùng `host.docker.internal` |
| `docker-compose.dev-linux.yml` | **Linux** ⚡ | Dùng `network_mode: host` (nhanh hơn) |

---

## 🚀 Cách chạy

### Mac/Windows:

```bash
# Chạy tất cả services trong Docker
docker-compose -f docker-compose.dev.yml up -d

# Hoặc chạy zone-service locally:
# 1. Comment out zone-service trong docker-compose.dev.yml
# 2. Set env: ZONE_SERVICE_URL=http://host.docker.internal:21503
# 3. Chạy Docker:
docker-compose -f docker-compose.dev.yml up -d
# 4. Chạy zone-service:
cd BE/zone_service && npm run dev
```

### Linux:

```bash
# Chạy tất cả services trong Docker (với host network)
docker-compose -f docker-compose.dev-linux.yml up -d

# Hoặc chạy zone-service locally:
# 1. Comment out zone-service trong docker-compose.dev-linux.yml
# 2. Không cần set ZONE_SERVICE_URL (tự động dùng localhost)
# 3. Chạy Docker:
docker-compose -f docker-compose.dev-linux.yml up -d
# 4. Chạy zone-service:
cd BE/zone_service && npm run dev
```

---

## 🔑 Điểm khác biệt chính

### Mac/Windows (`docker-compose.dev.yml`):
- ✅ Cross-platform compatible
- Services giao tiếp qua Docker network names
- Chạy service locally → cần set `XXX_SERVICE_URL=http://host.docker.internal:PORT`
- OSRM URLs: `http://host.docker.internal:259XX`

### Linux (`docker-compose.dev-linux.yml`):
- ⚡ Nhanh hơn (direct host network)
- ✅ Đơn giản hơn (không cần `host.docker.internal`)
- Services tự động dùng `localhost:PORT`
- OSRM URLs: `http://localhost:259XX`
- ⚠️ KHÔNG work trên Mac/Windows

---

## 💡 Tips

1. **Đang dùng Linux?** → Dùng `docker-compose.dev-linux.yml` (tốc độ tốt hơn)
2. **Đang dùng Mac/Windows?** → Dùng `docker-compose.dev.yml`
3. **Chỉ dev 1 service?** → Comment out service đó, chạy locally
4. **Production deployment?** → Dùng `docker-compose.yml` (không phải dev files)

---

## 📝 Example: Chạy zone-service locally (Mac)

```bash
# 1. Comment out zone-service trong docker-compose.dev.yml:
# # Zone Service
# # zone-service:
# #   restart: on-failure
# #   ...

# 2. Thêm vào .env:
echo "ZONE_SERVICE_URL=http://host.docker.internal:21503" >> .env

# 3. Start các services khác:
docker-compose -f docker-compose.dev.yml up -d

# 4. Run zone-service locally:
cd BE/zone_service
npm install
npm run dev

# 5. Test API:
curl http://localhost:21500/health  # api-gateway
curl http://localhost:21503/health  # zone-service (local)
```

---

## ❓ Khi nào dùng file nào?

**Production:**
```bash
docker-compose up
```

**Development (Mac/Windows):**
```bash
docker-compose -f docker-compose.dev.yml up
```

**Development (Linux):**
```bash
docker-compose -f docker-compose.dev-linux.yml up
```

---

**Questions?** Check [docker-compose.dev.yml](./docker-compose.dev.yml) or [docker-compose.dev-linux.yml](./docker-compose.dev-linux.yml) comments for details.
