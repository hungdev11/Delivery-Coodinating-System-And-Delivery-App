# Mục tiêu
- Tôi cần sử dụng Open source street map và Open source routing map cho dữ liệu địa lý
- Sử dụng tracking-asia để làm map, hiển thị và các thông tin như tình trạng giao thông, ...
- Lưu các dữ liệu và thiết kế để có thể tự cung cấp 1 phần (từ 2 dữ liệu của open source), đồng thời hỗ trợ cập nhật gần realtime (mỗi 15-30-60 phút) dữ liệu để xác nhận tuyến đường
- Lưu các con đường, nút giao (từ đó tạo ra các cung đường) để có thể sử dụng cho việc tính toán đường đi, tìm kiếm đường đi, ...
- Kết hợp với dữ liệu từ (./BE/zone_service/prisma/schema.prisma)[BE/zone_service/prisma/schema.prisma] để hoàn thiện db

# Những vấn đề đã biết
- OSM và OSRM có dữ liệu không tương thích với tracking-asia
- Việt Nam vừa cập nhật đơn vị hành chính, dẫn đến thông tin các khu vực bị thay đổi
- Mồ hình OSRM dùng để self hosting cần được cập nhật weight dựa trên custom weight mà tôi thu thập

# Xử lý dữ liệu
- Tôi sẽ dùng OSM lấy danh sách vị trí địa lý của các khu vực (thành phố Hồ Chí Minh, lấy các quận mới theo khu vực Thủ Đức cũ)
    - Dữ liệu của toàn Việt Nam: (BE\zone_service\raw_data\vietnam)[BE/zone_service/raw_data/vietnam]
    - Dữ liệu của Thành phố Hồ Chí Minh: (BE\zone_service\raw_data\new_hochiminh_city)[BE/zone_service/raw_data/new_hochiminh_city] (bao gồm poly của các quận mới)
    - Dữ liệu poly của Thành phố Thủ Đức cũ: (BE\zone_service\raw_data\new_hochiminh_city\old_thuduc_city)[BE/zone_service/raw_data/new_hochiminh_city/old_thuduc_city]
- Dựa trên dữ liệu đó, tôi muốn cập nhật db trong (./BE/zone_service/prisma/schema.prisma)[BE/zone_service/prisma/schema.prisma] để lưu thông tin bao gồm
    - Các khu vực là quận mới, giới hạn trong khu vực Thủ Đức cũ (dựa trên db, cập nhật vào db)
    - Các con đường, nếu có con đường trùng tên, lưu lại và xử lý ở bước 2
    - Các con đường trùng tên, kiểm tra xem nó có gần nhau hay không, nếu gần nhau và có thể nối lại thành 1 con đường, lưu lại vào db (cần chờ lấy hết); ví dụ như Xa lộ Hà Nội sẽ qua gần hết Thủ Đức cũ, do đó đa phần các quận đều có
    - Tính các nút giao, từ đó tạo ra các cung đường
    - Các cung đường sẽ có thông tin các địa chỉ trực thuộc (ví dụ Học viện Công nghệ Bưu chính Viễn thông sẽ nằm trên đường Man Thiện, phường Tăng Nhơn Phú)
    - Các cung đường sẽ lưu các thông tin về tên đường (kế thừa từ con đường chứa nó), thông tin về tốc độ trung bình của con đường đó (kế thừa từ con đường chứa nó), thông tin về tốc độ tối đa của con đường đó (kế thừa từ con đường chứa nó), thông tin về loại đường (kế thừa từ con đường chứa nó, là 1 chiều,cao tốc, hay hẻm nhỏ), weight của nó (tính từ các thông tin trên, cố định - chỉ thay đổi khi con đường bố bị thay đổi thông tin), delta weight (tính dựa trên các thông tin: tình trạng giao thông, số lượng người sử dụng đường đó, đề xuất từ user - tạo bảng mới rồi đấy; delta weight sẽ cập nhật mỗi khi có thay đổi)
- Thông tin giao thông: 
    - Sử dụng dữ liệu từ tracking-asia để lấy thông tin giao thông
    - Lưu thông tin giao thông vào db (chưa rõ có cần thiết hay không)
    - Tính toàn lại delta weight cho các cung đường
    - Cập nhật wight vào mô hình OSRM (dưa trên lua script - sẽ thực hiện bằng các deploy 2 mô hình OSRM luân phiên)
- Trả dữ liệu cho người dùng:
    - Sẽ trả về routing chi tiết để đưa vào tracking-asia, hiện thị trên màn hình người dùng (android, web Vue3). Thông tin bố sung bao gồm: hướng rẽ, tên đường, tên các địa chỉ trực thuộc, tình trạng giao thông, ...
    - Sẽ sử dụng mô hình OSRM để tính toán đường đi, tìm kiếm đường đi, ... qua nhiều điểm, có thể là nhiều route như vậy nối với nhau (tuyến đường ưu tiên 1 xong thì mới đến tuyến 2 - hỗ trợ giao hàng các đơn hàng độ ưu tiên khác nhau)

# Các bước thực hiện
- Bước 1: Lấy dữ liệu từ OSM và OSRM (đã có trong (BE\zone_service\raw_data)[BE/zone_service/raw_data])
- Bước 2: Cập nhật db trong (./BE/zone_service/prisma/schema.prisma)[BE/zone_service/prisma/schema.prisma]
- Bước 3: Tạo cơ chế để sử dụng dữ liệu từ tracking-asia (tình trạng giao thông, ...) và tính toán lại delta weight cho các cung đường
- Bước 3: Tạo service phụ để tạo ra dữ liệu cho mồ hình OSRM (để build lại) - mồ hình OSRM được deploy 2 mô hình luân phiên như đã nói ở trên

# Kết quả mong đợi:
- Có seed để tạo ra dữ liệu từ raw data vào db (tôi sẽ có 1 posgresql db để lưu dữ liệu - thay thế mysql hiện tại)
- Có thể sử dụng dữ liệu từ db để tính toán đường đi, tìm kiếm đường đi, ...
- Có thể sử dụng dữ liệu từ db để hiển thị trên map, tình trạng giao thông, ... (api)
