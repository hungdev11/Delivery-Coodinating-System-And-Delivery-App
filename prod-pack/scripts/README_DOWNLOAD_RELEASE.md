# Download Release Script

Script tự động download và deploy GitHub release vào thư mục production.

## Mục đích

Script này giúp:
- Download release từ GitHub (tag hoặc branch)
- Tự động extract và merge vào thư mục `/www/wwwroot/dss`
- Giữ nguyên các file đã tồn tại (không ghi đè)
- Tự động cleanup temporary files

## Cách sử dụng

### 1. Cấp quyền thực thi

```bash
chmod +x scripts/download-release.sh
```

### 2. Chạy script với URL release

```bash
./scripts/download-release.sh <release-url>
```

## Ví dụ

### Download từ GitHub Release Tag

```bash
./scripts/download-release.sh https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/archive/refs/tags/v1.1.1.zip
```

### Download từ Branch

```bash
# Download từ branch prod
./scripts/download-release.sh https://github.com/YOUR_USERNAME/YOUR_REPO/archive/refs/heads/prod.zip

# Download từ branch main
./scripts/download-release.sh https://github.com/YOUR_USERNAME/YOUR_REPO/archive/refs/heads/main.zip
```

### Download từ Commit SHA

```bash
./scripts/download-release.sh https://github.com/YOUR_USERNAME/YOUR_REPO/archive/COMMIT_SHA.zip
```

## Cách lấy URL Release

### Từ GitHub Web UI

1. Truy cập repository trên GitHub
2. Vào **Releases** → Chọn release cần download
3. Click **Source code (zip)** → Copy link address
4. Hoặc vào **Code** → **Download ZIP** → Copy link

### Từ GitHub API

```bash
# Lấy latest release URL
curl -s https://api.github.com/repos/YOUR_USERNAME/YOUR_REPO/releases/latest | grep zipball_url

# Lấy URL từ tag cụ thể
# Format: https://github.com/USERNAME/REPO/archive/refs/tags/TAG_NAME.zip
```

## Yêu cầu hệ thống

### Dependencies

Script cần các tools sau:

```bash
# Cài đặt unzip (bắt buộc)
sudo apt-get install unzip  # Ubuntu/Debian
sudo yum install unzip      # CentOS/RHEL

# wget hoặc curl (thường có sẵn)
# rsync (optional, để merge files tốt hơn)
sudo apt-get install rsync  # Ubuntu/Debian
```

### Quyền truy cập

- Quyền ghi vào `/www/wwwroot/dss`
- Quyền tạo thư mục nếu chưa tồn tại

## Quy trình hoạt động

1. **Validate URL**: Kiểm tra format URL hợp lệ
2. **Download**: Sử dụng `wget` hoặc `curl` để download ZIP
3. **Extract**: Giải nén ZIP vào temporary directory
4. **Merge**: Copy/rsync files vào `/www/wwwroot/dss`
   - Sử dụng `rsync --ignore-existing` nếu có rsync
   - Hoặc `cp -u` (update only) nếu không có rsync
5. **Cleanup**: Xóa temporary files

## Output

Script sẽ hiển thị:
- `[INFO]`: Thông tin quá trình
- `[SUCCESS]`: Thành công
- `[WARN]`: Cảnh báo
- `[ERROR]`: Lỗi

Ví dụ output:
```
[INFO] Downloading release from: https://github.com/...
[INFO] Target directory: /www/wwwroot/dss
[SUCCESS] Release downloaded successfully
[INFO] Extracting release...
[SUCCESS] Release extracted successfully
[INFO] Merging contents into /www/wwwroot/dss...
[SUCCESS] Contents merged successfully
[SUCCESS] Release deployment completed!
```

## Troubleshooting

### Lỗi: "unzip is not installed"

```bash
sudo apt-get install unzip
```

### Lỗi: "Neither wget nor curl is available"

```bash
# Cài đặt wget hoặc curl
sudo apt-get install wget  # hoặc curl
```

### Lỗi: "Permission denied"

```bash
# Cấp quyền cho script
chmod +x scripts/download-release.sh

# Hoặc chạy với sudo (nếu cần quyền root)
sudo ./scripts/download-release.sh <url>
```

### Lỗi: "Failed to download release"

- Kiểm tra kết nối internet
- Kiểm tra URL có đúng không
- Thử download thủ công để verify URL

### Files không được merge

- Kiểm tra quyền ghi vào `/www/wwwroot/dss`
- Kiểm tra disk space: `df -h`
- Xem log chi tiết trong output

## Tùy chỉnh

### Thay đổi target directory

Sửa dòng 48 trong script:
```bash
TARGET_DIR="/www/wwwroot/dss"  # Thay đổi đường dẫn ở đây
```

### Force overwrite files

Sửa dòng 125 (rsync) hoặc 133 (cp):
```bash
# Thay --ignore-existing bằng --update hoặc bỏ flag
rsync -av "$EXTRACTED_DIR/" "$TARGET_DIR/"
```

## Best Practices

1. **Backup trước khi deploy**: 
   ```bash
   cp -r /www/wwwroot/dss /www/wwwroot/dss.backup
   ```

2. **Test trên staging trước**: Chạy script trên môi trường test trước

3. **Verify sau khi deploy**: 
   ```bash
   ls -la /www/wwwroot/dss
   ```

4. **Check logs**: Xem output của script để đảm bảo không có lỗi

## Tích hợp với CI/CD

Script có thể được gọi từ CI/CD pipeline:

```yaml
# GitHub Actions example
- name: Deploy to production
  run: |
    cd /www/wwwroot/dss
    ./scripts/download-release.sh ${{ github.event.release.zipball_url }}
```

