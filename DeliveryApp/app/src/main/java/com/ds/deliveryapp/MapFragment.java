package com.ds.deliveryapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MapFragment extends Fragment {

    private FloatingActionButton fabListTasks;
    private CoordinatorLayout coordinatorLayout;
    private MapView mapView;
    private DeliveryAssignment currentAssignment;
    private List<DeliveryAssignment> tasks = new ArrayList<>();
    private static final String DRIVER_ID = "0bbfa6a6-1c0b-4e4f-9e6e-11e36c142ea5";
    private static final String TAG = "MapFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 💡 Cấu hình OSMDroid trước khi inflate (RẤT QUAN TRỌNG)
        Configuration.getInstance().load(getContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Ánh xạ CoordinatorLayout (ROOT VIEW) và MapView
        coordinatorLayout = (CoordinatorLayout) view;
        mapView = view.findViewById(R.id.map_view_osm);

        fabListTasks = view.findViewById(R.id.fab_list_tasks);

        // 1. Khởi tạo và Cấu hình Map
        setupOSMMap();

        // 2. TẢI DỮ LIỆU
        fetchTodayTasks(DRIVER_ID);

        // 3. Thiết lập sự kiện FAB
        setupFabListener();
        return view;
    }

    private void setupOSMMap() {
        if (mapView == null) return;

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Vị trí mặc định (Ví dụ: TP.HCM)
        GeoPoint startPoint = new GeoPoint(10.8231, 106.6297);
        mapView.getController().setCenter(startPoint);
    }

    private void displayTaskOnMap(DeliveryAssignment assignment) {
        if (mapView == null || assignment == null) return;

        // 💡 ĐIỂM 1: Vị trí Bắt đầu (Giả định vị trí tài xế hiện tại)
        GeoPoint startPoint = new GeoPoint(10.775843, 106.697412); // Ví dụ: Quận 1, TP.HCM

        // 💡 ĐIỂM 2: Vị trí Giao hàng (Ví dụ: Lấy từ tọa độ giả định của task)
        GeoPoint endPoint = new GeoPoint(10.8231 + Math.random() * 0.05, 106.6297 + Math.random() * 0.05);

        // 💡 BƯỚC SỬA LỖI QUAN TRỌNG: XÓA TẤT CẢ OVERLAYS CŨ
        mapView.getOverlays().clear();

        // 1. Thêm Marker Bắt đầu (Tài xế)
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setTitle("Vị trí của bạn");
        startMarker.setSnippet("Bắt đầu");
        mapView.getOverlays().add(startMarker);

        // 2. Thêm Marker Kết thúc (Giao hàng)
        Marker endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setTitle(assignment.getReceiverName());
        endMarker.setSnippet("Mã đơn: " + assignment.getParcelCode());
        mapView.getOverlays().add(endMarker);

        // 3. Gọi Task vẽ đường đi (Hàm này cũng sẽ xóa Polyline cũ)
        drawRouteBetweenPoints(startPoint, endPoint);

        mapView.invalidate();
    }
    // Trong MapFragment.java

    public void drawRouteBetweenPoints(GeoPoint startPoint, GeoPoint endPoint) {
        if (mapView == null) return;

        // 💡 SỬA LỖI: CHỈ XÓA POLYLINE CŨ (Giữ lại các Marker đã thêm)
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Polyline); // Xóa Polyline

        // Gọi Task vẽ đường đi
        new GetRouteTask(startPoint, endPoint).execute();

        // Di chuyển camera đến điểm bắt đầu
        mapView.getController().animateTo(startPoint);
        mapView.invalidate();
    }


    // --- LỚP ASYNCTASK ĐỂ GỌI API ROUTING (OSRM) ---
    private class GetRouteTask extends AsyncTask<Void, Void, ArrayList<GeoPoint>> {
        private final GeoPoint start;
        private final GeoPoint end;

        // Sử dụng OSRM Demo Server (Miễn phí, nhưng có thể chậm)
        // ⚠️ LƯU Ý: Nên dùng một instance OSRM riêng nếu ứng dụng phát hành thương mại
        private static final String OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";

        public GetRouteTask(GeoPoint start, GeoPoint end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected ArrayList<GeoPoint> doInBackground(Void... voids) {
            String urlString = String.format(Locale.US,
                    "%s%.5f,%.5f;%.5f,%.5f?overview=full&geometries=geojson",
                    OSRM_URL,
                    start.getLongitude(), start.getLatitude(),
                    end.getLongitude(), end.getLatitude());

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    return parseRoute(response.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi gọi OSRM API: " + e.getMessage());
            }
            return null;
        }

        private ArrayList<GeoPoint> parseRoute(String jsonResponse) throws Exception {
            ArrayList<GeoPoint> routePoints = new ArrayList<>();
            JSONObject json = new JSONObject(jsonResponse);

            // Truy cập mảng routes
            if (json.has("routes")) {
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject geometry = route.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray coord = coordinates.getJSONArray(i);
                        // OSRM trả về [Longitude, Latitude]
                        double lon = coord.getDouble(0);
                        double lat = coord.getDouble(1);
                        routePoints.add(new GeoPoint(lat, lon));
                    }
                }
            }
            return routePoints;
        }

        @Override
        protected void onPostExecute(ArrayList<GeoPoint> result) {
            if (result != null && !result.isEmpty()) {
                Polyline roadOverlay = new Polyline();
                roadOverlay.setPoints(result);
                roadOverlay.setColor(Color.BLUE);
                roadOverlay.setWidth(8.0f);

                // 💡 Chỉ cần thêm vào, vì Polyline cũ đã bị xóa bởi drawRouteBetweenPoints
                mapView.getOverlays().add(roadOverlay);
                mapView.invalidate();
            } else {
                Toast.makeText(getContext(), "Không tìm thấy đường đi.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupFabListener() {
        fabListTasks.setOnClickListener(v -> {
            if (tasks.isEmpty()) {
                Toast.makeText(getContext(), "Không có nhiệm vụ đang xử lý.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Chỉ mở Dialog nếu tasks đã được lọc không rỗng
            TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(tasks);
            dialog.show(getParentFragmentManager(), "TaskListDialog");
        });
    }

    private void showTaskSnackbar(DeliveryAssignment assignment) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "", Snackbar.LENGTH_INDEFINITE);
        final View snackbarView = snackbar.getView();

        // Ẩn TextView mặc định và đặt padding 0
        if (snackbarView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) snackbarView;
            if (viewGroup.getChildCount() > 0) {
                viewGroup.getChildAt(0).setVisibility(View.INVISIBLE);
            }
        }

        View customView = LayoutInflater.from(getContext()).inflate(R.layout.layout_task_snackbar, null);

        // Ánh xạ và Gán dữ liệu
        TextView tvCustomerName = customView.findViewById(R.id.tv_customer_name);
        TextView tvTaskDetails = customView.findViewById(R.id.tv_task_details);
        ImageButton btnCall = customView.findViewById(R.id.btn_call);

        tvCustomerName.setText(assignment.getReceiverName());
        tvTaskDetails.setText("Mã đơn: " + assignment.getParcelCode() + " | " + assignment.getDeliveryLocation());

        // Thiết lập sự kiện Call
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + assignment.getReceiverPhone()));
            startActivity(intent);
        });

        // Thêm Custom View vào Snackbar
        try {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            customView.setLayoutParams(params);

            ((ViewGroup) snackbarView).addView(customView, 0);

        } catch (ClassCastException e) {
            Log.e(TAG, "Lỗi thêm customView vào Snackbar: " + e.getMessage());
            return;
        }

        snackbarView.setPadding(0, 0, 0, 0);
        snackbar.show();
    }

    public void fetchTodayTasks(String driverId) {
        Retrofit retrofit = RetrofitClient.getSessionRetrofitInstance();
        SessionClient service = retrofit.create(SessionClient.class);

        // API này trả về List<DeliveryAssignment>
        // ⚠️ Nếu API của bạn có thể lọc theo status, bạn nên sửa lại gọi API ở đây
        // Ví dụ: service.getTasksToday(driverId, Arrays.asList("PROCESSING"));
        Call<List<DeliveryAssignment>> call = service.getTasksToday(driverId);

        call.enqueue(new Callback<List<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<List<DeliveryAssignment>> call, Response<List<DeliveryAssignment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 💡 LOGIC LỌC: CHỈ GIỮ LẠI NHIỆM VỤ CÓ STATUS = "PROCESSING"
                    tasks.clear();

                    for (DeliveryAssignment assignment : response.body()) {
                        if ("PROCESSING".equals(assignment.getStatus())) {
                            tasks.add(assignment);
                        }
                    }

                    if (!tasks.isEmpty()) {
                        // Lấy nhiệm vụ PROCESSING đầu tiên
                        currentAssignment = tasks.get(0);
                        showTaskSnackbar(currentAssignment);
                        displayTaskOnMap(currentAssignment);
                    } else {
                        // 💡 Xử lý khi không còn nhiệm vụ PROCESSING nào
                        currentAssignment = null;
                        Toast.makeText(getContext(), "Không có nhiệm vụ đang xử lý nào hôm nay.", Toast.LENGTH_SHORT).show();
                        // Đảm bảo Snackbar/Map được làm sạch (Nếu cần)
                        mapView.getOverlays().clear();
                        mapView.invalidate();
                    }
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<DeliveryAssignment>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                // Xử lý lỗi mạng
                currentAssignment = null;
                mapView.getOverlays().clear();
                mapView.invalidate();
            }
        });
    }

    // --- Lifecycle Methods Quan Trọng cho OSMDroid ---
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    public void displayTaskAndRoute(DeliveryAssignment assignment) {
        // 💡 Chỉ xử lý nếu nhiệm vụ đang ở trạng thái PROCESSING
        if (!"PROCESSING".equals(assignment.getStatus())) {
            Toast.makeText(getContext(), "Nhiệm vụ đã hoàn tất hoặc thất bại.", Toast.LENGTH_SHORT).show();
            return;
        }

        this.currentAssignment = assignment;

        showTaskSnackbar(assignment);

        // Vẽ Marker và Đường đi
        displayTaskOnMap(assignment);
    }
}