## OSRM Car Profile — Phân tích cơ chế duyệt qua các điểm (nodes/turns)

### 1) Tổng quan
- **Callbacks chính**: `process_node`, `process_way`, `process_turn`.
- **Luồng thực thi**: OSRM gọi các hàm này cho từng phần tử bản đồ; hồ sơ không tự lặp.
  - `process_node`: xử lý từng node, phát hiện rào cản và ghi vào `obstacle_map`.
  - `process_way`: xử lý thuộc tính từng way qua chuỗi `WayHandlers` (tốc độ, lớp, weight…).
  - `process_turn`: xử lý lượt rẽ tại giao lộ, cộng phạt theo obstacles, góc rẽ, U-turn.

### 2) Duyệt node — process_node
- Tìm `access` theo `access_tags_hierarchy`; nếu thuộc blacklist (không nằm trong restricted allow-list) ➜ thêm `Obstacle.barrier`.
- Nếu có `barrier=*`:
  - Ngoại lệ: `bollard=rising`, `barrier=kerb` khi `kerb` hạ thấp/flush hoặc `highway=crossing`.
  - Kiểm tra giới hạn chiều cao với `barrier=height_restrictor` so với `profile.vehicle_height`.
- Gọi `Obstacles.process_node(profile, node)` để ghi nhận các chướng ngại khác.

Hệ quả: node có thể sinh "obstacle" để `process_turn` sử dụng khi tính chi phí tại giao lộ.

### 3) Duyệt way — process_way
- Prefetch tag: `highway`, `bridge`, `route`; loại sớm way không định tuyến được.
- Chuỗi `handlers` điển hình:
  - Chặn/né đường: `blocked_ways`, `avoid_ways`.
  - Ràng buộc kích thước xe: `handle_height/width/length/weight`.
  - Truy cập/chiều đi: `access`, `oneway`, `destinations`.
  - Phương tiện đặc thù: `ferries`, `movables`.
  - Loại đường/phạt: `service`, `hov`, `speed`, `maxspeed`, `surface`, `penalties`.
  - Phân lớp/hướng dẫn: `classes`, `turn_lanes`, `classification`, `roundabouts`, `driving_side`.
  - Nhãn & trọng số: `names`, `weights`, `way_classification_for_turn`.

Hệ quả: mỗi way được gán tốc độ/lớp/trọng số để xây dựng đồ thị routing.

### 4) Duyệt turn — process_turn
- Lấy danh sách obstacles cho cặp `(turn.from, turn.via)` và cộng `obs.duration` với 2 loại bỏ:
  - Bỏ `stop_minor` khi vào từ đường chính (`entering_by_minor_road` là false).
  - Bỏ `stop` không hướng ở nút 2 nhánh nếu `source_road.distance < 20m` và `target_road.distance > 20m`.
- Phạt theo góc rẽ bằng hàm sigmoid (0–180°), có thiên lệch `turn_bias` tùy chiều lái xe.
- Cộng phạt U-turn bằng `u_turn_penalty`.
- Đặt `turn.weight`:
  - `weight_name='distance'`: `weight = 0` (không phạt góc).
  - Ngược lại: `weight = duration`.
  - Với `routability`: nếu từ đoạn không restricted sang đoạn restricted ➜ `weight = max_turn_weight`.

Hệ quả: chi phí giao lộ phản ánh thực tế nhờ obstacles, góc rẽ và U-turn.

### 5) Tham số ảnh hưởng chính
- **Hình phạt rẽ**: `profile.turn_penalty`, `profile.properties.u_turn_penalty`, `profile.turn_bias`.
- **Tốc độ bề mặt/đường**: `surface_speeds`, `tracktype_speeds`, `smoothness_speeds`, `maxspeed_table`.
- **Truy cập & kích thước xe**: `access_tag_blacklist/whitelist`, `barrier_whitelist`, `vehicle_height/width/length/weight`.
- **Chính sách weight**: `properties.weight_name` (`routability`/`duration`/`distance`).

### 6) Gợi ý tinh chỉnh/kiểm thử
- Hiệu chỉnh `turn_penalty` và `turn_bias` theo dữ liệu địa phương (GPS traces).
- Kiểm thử nút 2 nhánh gần có nhiều `stop` để xác nhận heuristic bỏ qua hợp lý.
- Điều chỉnh `surface_speeds`/`tracktype_speeds` theo chất lượng mặt đường thực tế.
- Sử dụng `excludable` để bật/tắt tránh `toll/motorway/ferry` nếu cần các profile tuỳ chọn.

### 7) Kết luận
- Việc "duyệt điểm" diễn ra qua các callback:
  - Node ➜ phát hiện/ghi obstacles.
  - Way ➜ thiết lập thuộc tính cạnh.
  - Turn ➜ tính chi phí rẽ sử dụng obstacles + phạt góc/U-turn.
- `process_turn` là nơi quyết định chi phí tại giao lộ, ảnh hưởng trực tiếp đến lộ trình.
