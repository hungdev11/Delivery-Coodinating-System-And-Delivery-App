# DSS Production Deployment Pack

Gói triển khai production cho Delivery System. Chứa tất cả file cần thiết để deploy hệ thống.

## 📦 Cách lấy gói triển khai

### Cách 1: Clone trực tiếp từ nhánh `prod`

```bash
# Clone chỉ nhánh prod (không có source code)
git clone -b prod --single-branch https://github.com/YOUR_USERNAME/YOUR_REPO.git dss-prod

cd dss-prod
```

### Cách 2: Download ZIP

1. Truy cập: `https://github.com/YOUR_USERNAME/YOUR_REPO/archive/refs/heads/prod.zip`
2. Giải nén và sử dụng

### Cách 3: Từ main branch

```bash
# Clone full repo
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git

# Copy prod-pack ra ngoài
cp -r YOUR_REPO/prod-pack ./dss-prod
cd dss-prod
```

## 🚀 Hướng dẫn triển khai

### 1. Chuẩn bị file `.env`

```bash
# Copy template
cp env.local .env

# Chỉnh sửa các giá trị bắt buộc
nano .env
```

**Các biến bắt buộc:**
```env
# GitHub Container Registry
REPOSITORY_OWNER=your-github-username

# Database (external MySQL)
DB_HOST=your-db-host
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password

# Keycloak
KEYCLOAK_URL=http://your-keycloak:8080
```

### 2. Chuẩn bị raw_data cho Zone Service

Zone Service cần thư mục `raw_data` để chứa các file OSM (OpenStreetMap) và polygon data. Thư mục này được mount từ host vào container để bạn có thể thêm file mà không cần rebuild image.

```bash
# Tạo thư mục raw_data với cấu trúc cơ bản
mkdir -p raw_data/{vietnam,poly,extracted,osrm-logic}

# Thêm file OSM vào thư mục vietnam (nếu có)
# Ví dụ: raw_data/vietnam/vietnam-251013.osm.pbf

# Thêm file polygon vào thư mục poly (nếu có)
# Ví dụ: raw_data/poly/thuduc_cu.poly
```

**Lưu ý:**
- Thư mục `raw_data` sẽ được mount vào container tại `/app/raw_data`
- Bạn có thể thêm/sửa/xóa file trong thư mục này bất cứ lúc nào
- Service sẽ tự động detect file OSM mới nhất trong `raw_data/vietnam/`
- Các script trong zone-service sử dụng đường dẫn tương đối `./raw_data` (từ `/app` trong container)

### 3. Chuẩn bị OSRM data (nếu cần routing)

```bash
# Tạo thư mục OSRM data
mkdir -p osrm_data/{osrm-full,osrm-rating-only,osrm-blocking-only,osrm-base}

# Download và extract OSRM data vào các thư mục tương ứng
# (Xem hướng dẫn tạo OSRM data riêng)
```

### 4. Khởi động hệ thống

```bash
# Pull images mới nhất
docker compose pull

# Khởi động tất cả services
docker compose up -d

# Xem logs
docker compose logs -f

# Kiểm tra trạng thái
docker compose ps
```

### 5. Các lệnh hữu ích

```bash
# Restart một service
docker compose restart api-gateway

# Xem logs của service cụ thể
docker compose logs -f api-gateway

# Stop tất cả
docker compose down

# Stop và xóa volumes
docker compose down -v

# Update images và restart
docker compose pull && docker compose up -d
```

## 📁 Cấu trúc thư mục

```
dss-prod/
├── docker-compose.yml    # Docker Compose configuration
├── nginx.conf            # Nginx reverse proxy config
├── env.local             # Environment template
├── .env                  # Your environment config (create from env.local)
├── README.md             # This file
├── raw_data/             # Raw OSM data for zone-service (mounted to container)
│   ├── vietnam/          # Vietnam OSM files (*.osm.pbf)
│   ├── poly/             # Polygon files (*.poly)
│   ├── extracted/        # Extracted OSM data
│   └── osrm-logic/       # OSRM profile scripts (*.lua)
└── osrm_data/            # OSRM routing data (optional)
    ├── osrm-full/
    ├── osrm-rating-only/
    ├── osrm-blocking-only/
    └── osrm-base/
```

## 🔧 Services

| Service | Port | Description |
|---------|------|-------------|
| nginx-proxy | 8080 | Reverse proxy & frontend |
| api-gateway | 21500 | API Gateway |
| user-service | 21501 | User management |
| settings-service | 21502 | System settings |
| zone-service | 21503 | Zone & routing |
| session-service | 21505 | Delivery sessions |
| parcel-service | 21506 | Parcel management |
| communication-service | 21511 | Chat & notifications |
| kafka | 9092 | Message broker |
| zookeeper | 2181 | Kafka coordinator |

## 🔒 Security Notes

- Không commit file `.env` lên git
- Sử dụng strong passwords cho database
- Cấu hình firewall chỉ expose port 8080
- Sử dụng HTTPS với reverse proxy (Cloudflare, etc.)

## 📝 Troubleshooting

### Services không khởi động
```bash
# Kiểm tra logs
docker compose logs settings-service

# Kiểm tra health
docker compose ps
```

### Database connection failed
- Kiểm tra `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD` trong `.env`
- Đảm bảo database đã được tạo

### Images pull failed
- Kiểm tra `REPOSITORY_OWNER` trong `.env`
- Đảm bảo images đã được build và push từ CI/CD
