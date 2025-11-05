package com.ds.deliveryapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

// API Clients
import com.ds.deliveryapp.clients.RoutingApi;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.RoutingRequestDto;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.RoutingResponseDto;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.enums.DeliveryType;

// Models
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.SessionManager;

// UI
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

// OSMDroid
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

// Java
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import retrofit2.Response;

public class MapFragment extends Fragment implements TaskListDialogFragment.OnTaskSelectedListener, LocationListener {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // --- CẤU HÌNH ĐIỀU HƯỚNG ---
    private static final double DEVIATION_THRESHOLD_METERS = 50.0; // Ngưỡng lệch 50m
    private static final double ARRIVAL_THRESHOLD_METERS = 25.0; // Ngưỡng đến nơi 25m
    private static final double NEXT_STEP_THRESHOLD_METERS = 20.0; // Ngưỡng hoàn thành 1 bước 20m
    private static final long GPS_UPDATE_INTERVAL_MS = 2000; // 2 giây
    private static final float GPS_UPDATE_DISTANCE_M = 5; // 5 mét

    // --- Các biến UI ---
    private FloatingActionButton fabListTasks, fabNextTask, fabRecenter;
    private CoordinatorLayout coordinatorLayout;
    private MapView mapView;
    private TextView tvInstruction;
    private ProgressBar progressBar;

    // --- Dữ liệu & Trạng thái ---
    private List<DeliveryAssignment> mOriginalTasks;
    private List<DeliveryAssignment> mSortedTasks;
    private RoutingResponseDto.RouteResponseDto mRouteResponse;
    private List<ArrayList<GeoPoint>> mPrecalculatedPolylines;
    private int currentLegIndex = 0;
    private int currentStepIndex = 0; // --- NÂNG CẤP: Theo dõi bước (step) hiện tại ---
    private boolean isRouteLoaded = false;
    private boolean isRecalculating = false;
    private boolean isNavigating = true; // --- NÂNG CẤP: Trạng thái tự động điều hướng ---

    // --- GPS & Tracking ---
    private LocationManager locationManager;
    private GeoPoint mCurrentLocation;
    private Marker mDriverMarker;

    // --- API Clients ---
    private String driverId;
    private SessionClient sessionClient;
    private RoutingApi routingApi;

    // --- Icon cho nút điều hướng ---
    private Drawable iconRecenter;
    private Drawable iconNavigation;

    // --- Lớp chứa kết quả AsyncTask ---
    private static class RouteCalculationResult {
        //... (Giữ nguyên)
        final RoutingResponseDto.RouteResponseDto routeResponse;
        final List<DeliveryAssignment> sortedTasks;
        final List<ArrayList<GeoPoint>> polylines;
        final Exception exception;

        RouteCalculationResult(RoutingResponseDto.RouteResponseDto routeResponse, List<DeliveryAssignment> sortedTasks, List<ArrayList<GeoPoint>> polylines, Exception e) {
            this.routeResponse = routeResponse;
            this.sortedTasks = sortedTasks;
            this.polylines = polylines;
            this.exception = e;
        }
    }

    // --- AsyncTask chính ---
    private static class FetchAndRouteTask extends AsyncTask<Void, Void, RouteCalculationResult> {
        //... (Giữ nguyên)
        private WeakReference<MapFragment> fragmentRef;
        private GeoPoint startLocation;

        FetchAndRouteTask(MapFragment fragment, GeoPoint startLocation) {
            this.fragmentRef = new WeakReference<>(fragment);
            this.startLocation = startLocation;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MapFragment fragment = fragmentRef.get();
            if (fragment != null && fragment.progressBar != null) {
                fragment.progressBar.setVisibility(View.VISIBLE);
                fragment.isRecalculating = true;
            }
        }

        @Override
        protected RouteCalculationResult doInBackground(Void... voids) {
            MapFragment fragment = fragmentRef.get();
            if (fragment == null || startLocation == null) {
                return new RouteCalculationResult(null, null, null, new Exception("Fragment or Start Location is null"));
            }

            try {
                // 1. Lấy danh sách task (chỉ nếu chưa có hoặc đang re-route)
                // --- SỬA LỖI LOGIC: Chỉ lấy task khi mOriginalTasks là null ---
                // Nếu re-route, mOriginalTasks vẫn giữ nguyên
                if (fragment.mOriginalTasks == null) {
                    Log.d(TAG, "Đang lấy danh sách Task...");
                    Response<PageResponse<DeliveryAssignment>> taskResponse = fragment.sessionClient
                            .getTasksToday(fragment.driverId, List.of("IN_PROGRESS"), 0, 100)
                            .execute();

                    if (!taskResponse.isSuccessful() || taskResponse.body() == null || taskResponse.body().content() == null) {
                        throw new IOException("Không thể lấy danh sách nhiệm vụ");
                    }
                    // Lọc các task CÓ lat/lon và ĐANG TIẾN HÀNH
                    fragment.mOriginalTasks = taskResponse.body().content().stream()
                            .filter(task -> "IN_PROGRESS".equals(task.getStatus()) && task.getLat() != null && task.getLon() != null)
                            .collect(Collectors.toList());
                }

                if(fragment.mOriginalTasks.isEmpty()) {
                    return new RouteCalculationResult(null, new ArrayList<>(), new ArrayList<>(), null); // Không có task
                }

                // 2. Xây dựng Yêu cầu Routing
                RoutingRequestDto.RouteRequestDto routeRequest = fragment.buildRoutingRequest(startLocation, fragment.mOriginalTasks);

                // 3. Gọi API Routing
                Log.d(TAG, "Đang gọi API Routing...");
                Response<RoutingResponseDto.RouteResponseDto> routeApiResponse = fragment.routingApi
                        .getOptimalRoute(routeRequest)
                        .execute();

                if (!routeApiResponse.isSuccessful() || routeApiResponse.body() == null) {
                    throw new IOException("Không thể lấy tuyến đường: " + routeApiResponse.message());
                }

                RoutingResponseDto.RouteResponseDto routeResponse = routeApiResponse.body();

                // 4. Xử lý kết quả (Map task và tính Polyline)
                return fragment.processRoutingResponse(routeResponse, fragment.mOriginalTasks);

            } catch (Exception e) {
                Log.e(TAG, "Lỗi trong FetchAndRouteTask: " + e.getMessage());
                return new RouteCalculationResult(null, null, null, e);
            }
        }

        @Override
        protected void onPostExecute(RouteCalculationResult result) {
            super.onPostExecute(result);
            MapFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                return; // Fragment đã bị hủy
            }

            fragment.progressBar.setVisibility(View.GONE);
            fragment.isRecalculating = false; // Hoàn tất tính toán

            if (result.exception != null) {
                Toast.makeText(fragment.getContext(), "Lỗi tải dữ liệu: " + result.exception.getMessage(), Toast.LENGTH_LONG).show();
            } else if (result.sortedTasks.isEmpty()) {
                Toast.makeText(fragment.getContext(), "Không có nhiệm vụ nào cần xử lý.", Toast.LENGTH_LONG).show();
            } else {
                // Tải thành công, gán dữ liệu
                fragment.mRouteResponse = result.routeResponse;
                fragment.mSortedTasks = result.sortedTasks;
                fragment.mPrecalculatedPolylines = result.polylines;
                fragment.isRouteLoaded = true;
                fragment.isNavigating = true; // --- NÂNG CẤP: Bật điều hướng khi có đường mới ---

                // Bắt đầu hiển thị chặng đầu tiên
                fragment.currentLegIndex = 0;
                fragment.displayCurrentLeg(); // Sẽ tự động reset currentStepIndex = 0
                fragment.showTaskSnackbar(fragment.mSortedTasks.get(fragment.currentLegIndex));

                // Bắt đầu theo dõi GPS
                fragment.startGpsTracking();
            }
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(getContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Ánh xạ UI
        coordinatorLayout = (CoordinatorLayout) view;
        mapView = view.findViewById(R.id.map_view_osm);
        fabListTasks = view.findViewById(R.id.fab_list_tasks);
        fabNextTask = view.findViewById(R.id.fab_next_task);
        fabRecenter = view.findViewById(R.id.fab_recenter); // Nút mới
        tvInstruction = view.findViewById(R.id.tv_instruction);
        progressBar = view.findViewById(R.id.progress_bar);

        // --- NÂNG CẤP: Tải icon cho nút điều hướng ---
        iconRecenter = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_mylocation);
        iconNavigation = ContextCompat.getDrawable(getContext(), R.drawable.ic_navigation); // Giả sử bạn có icon này
        fabRecenter.setImageDrawable(iconNavigation); // Bắt đầu ở chế độ điều hướng

        // Lấy driverId
        SessionManager sessionManager = new SessionManager(requireContext());
        driverId = sessionManager.getDriverId();
        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy tài xế.", Toast.LENGTH_LONG).show();
            return view;
        }

        // Khởi tạo API Clients
        sessionClient = RetrofitClient.getRetrofitInstance(getContext()).create(SessionClient.class);
        routingApi = RetrofitClient.getRetrofitInstance(getContext()).create(RoutingApi.class);

        setupOSMMap();
        setupFabListeners();
        checkAndRequestLocation(); // Bắt đầu luồng
        return view;
    }

    private void checkAndRequestLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initializeLocationAndStartRouting();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationAndStartRouting();
            } else {
                Toast.makeText(getContext(), "Cần cấp quyền vị trí để sử dụng bản đồ.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeLocationAndStartRouting() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation == null) {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (lastKnownLocation != null) {
            mCurrentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            updateDriverMarker();
            mapView.getController().animateTo(mCurrentLocation);
            // Có vị trí -> Bắt đầu tải Task và tính đường
            new FetchAndRouteTask(this, mCurrentLocation).execute();
        } else {
            Toast.makeText(getContext(), "Đang chờ tín hiệu GPS... Vui lòng ra ngoài trời.", Toast.LENGTH_LONG).show();
            // Vẫn lắng nghe, khi nào có vị trí đầu tiên sẽ gọi
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL_MS, GPS_UPDATE_DISTANCE_M, this);
        }
    }

    private void startGpsTracking() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL_MS, GPS_UPDATE_DISTANCE_M, this);
    }

    // --- KERNEL ĐIỀU HƯỚNG ---
    @Override
    public void onLocationChanged(@NonNull Location location) {
        mCurrentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        updateDriverMarker();

        // --- NÂNG CẤP: Tự động di chuyển camera ---
        if (isNavigating) {
            mapView.getController().animateTo(mCurrentLocation);
            // Bạn cũng có thể set độ nghiêng (bearing) nếu có la bàn
            // mapView.setMapOrientation(-location.getBearing());
        }

        if (isRouteLoaded) {
            // Kiểm tra lệch hướng
            checkDeviation(mCurrentLocation);
            // --- NÂNG CẤP: Cập nhật trạng thái điều hướng (step-by-step) ---
            updateNavigationState(mCurrentLocation);

        } else if (!isRecalculating) {
            // Lần đầu tiên nhận GPS
            Log.d(TAG, "Nhận được vị trí đầu tiên. Bắt đầu tính toán tuyến đường...");
            new FetchAndRouteTask(this, mCurrentLocation).execute();
        }
    }

    /**
     * NÂNG CẤP: Hàm xử lý logic điều hướng từng bước và tự động chuyển chặng.
     */
    private void updateNavigationState(GeoPoint currentLocation) {
        if (isRecalculating || mPrecalculatedPolylines == null || currentLegIndex >= mPrecalculatedPolylines.size()) {
            return;
        }

        // 1. Kiểm tra xem đã đến điểm cuối của CHẶNG (Leg) chưa
        ArrayList<GeoPoint> currentPolyline = mPrecalculatedPolylines.get(currentLegIndex);
        if (currentPolyline.isEmpty()) return;

        GeoPoint legDestination = currentPolyline.get(currentPolyline.size() - 1);
        double distanceToLegDestination = currentLocation.distanceToAsDouble(legDestination);

        if (distanceToLegDestination <= ARRIVAL_THRESHOLD_METERS) {
            Log.d(TAG, "Đã đến điểm giao hàng. Tự động chuyển chặng...");
            Toast.makeText(getContext(), "Đã đến: " + mSortedTasks.get(currentLegIndex).getReceiverName(), Toast.LENGTH_SHORT).show();
            completeCurrentLeg();
            return; // Hoàn tất, không cần kiểm tra step
        }

        // 2. Nếu chưa đến, kiểm tra xem đã hoàn thành BƯỚC (Step) hiện tại chưa
        RoutingResponseDto.RouteLegDto currentLeg = mRouteResponse.getRoute().getLegs().get(currentLegIndex);
        List<RoutingResponseDto.RouteStepDto> steps = currentLeg.getSteps();

        if (currentStepIndex >= steps.size()) {
            return; // Đã ở bước cuối cùng, chờ đến điểm
        }

        // Lấy điểm cuối của bước hiện tại
        GeoPoint currentStepDestination = getStepDestination(currentLeg, currentStepIndex);
        if (currentStepDestination == null) return;

        double distanceToStepDestination = currentLocation.distanceToAsDouble(currentStepDestination);

        if (distanceToStepDestination <= NEXT_STEP_THRESHOLD_METERS) {
            // Đã hoàn thành bước này, chuyển sang bước tiếp theo
            currentStepIndex++;

            if (currentStepIndex < steps.size()) {
                // Cập nhật UI cho bước tiếp theo
                RoutingResponseDto.RouteStepDto nextStep = steps.get(currentStepIndex);
                tvInstruction.setText(nextStep.getInstruction());
                setTrafficColor(nextStep.getTrafficLevel());
            } else {
                // Đã là bước cuối cùng, báo "Sắp đến"
                tvInstruction.setText("Sắp đến: " + mSortedTasks.get(currentLegIndex).getReceiverName());
            }
        }
        // Nếu chưa đến ngưỡng, không làm gì, giữ nguyên chỉ dẫn
    }

    /**
     * NÂNG CẤP: Hàm trợ giúp lấy tọa độ cuối cùng của một Step
     */
    private GeoPoint getStepDestination(RoutingResponseDto.RouteLegDto leg, int stepIndex) {
        try {
            RoutingResponseDto.RouteStepDto step = leg.getSteps().get(stepIndex);
            List<List<Double>> coordinates = step.getGeometry().getCoordinates();
            if (coordinates == null || coordinates.isEmpty()) {
                return null;
            }
            List<Double> lastCoord = coordinates.get(coordinates.size() - 1);
            return new GeoPoint(lastCoord.get(1), lastCoord.get(0));
        } catch (Exception e) {
            Log.e(TAG, "Không thể lấy điểm cuối của step: " + e.getMessage());
            return null;
        }
    }

    /**
     * NÂNG CẤP: Hàm hoàn thành chặng, được gọi tự động hoặc thủ công
     */
    private void completeCurrentLeg() {
        if (!isRouteLoaded) return;

        currentLegIndex++; // Chuyển sang chặng tiếp theo
        currentStepIndex = 0; // Reset chỉ số bước

        if (currentLegIndex < mSortedTasks.size()) {
            displayCurrentLeg();
            showTaskSnackbar(mSortedTasks.get(currentLegIndex));
        } else {
            Toast.makeText(getContext(), "Đã hoàn thành tất cả nhiệm vụ!", Toast.LENGTH_LONG).show();
            tvInstruction.setText("Đã hoàn thành tất cả nhiệm vụ!");
            fabNextTask.setEnabled(false);
            isNavigating = false; // Tắt điều hướng
            fabRecenter.setImageDrawable(iconRecenter); // Đổi icon
        }
    }


    private void checkDeviation(GeoPoint currentLocation) {
        if (isRecalculating || mPrecalculatedPolylines == null || currentLegIndex >= mPrecalculatedPolylines.size()) {
            return;
        }

        Polyline currentPolyline = new Polyline();
        currentPolyline.setPoints(mPrecalculatedPolylines.get(currentLegIndex));

        boolean onTrack = false;
        for (GeoPoint point : currentPolyline.getPoints()) {
            if (point.distanceToAsDouble(currentLocation) <= DEVIATION_THRESHOLD_METERS) {
                onTrack = true;
                break;
            }
        }

        if (!onTrack) {
            Log.w(TAG, "Phát hiện lệch hướng! Đang tính toán lại...");
            Toast.makeText(getContext(), "Phát hiện lệch hướng, đang tính toán lại...", Toast.LENGTH_SHORT).show();
            // Xóa danh sách task cũ để AsyncTask tải lại (nếu cần)
            // mOriginalTasks = null; // Tùy logic, nếu task có thể thay đổi
            new FetchAndRouteTask(this, currentLocation).execute();
        }
    }

    private void updateDriverMarker() {
        if (mCurrentLocation == null) return;
        if (mDriverMarker == null) {
            mDriverMarker = new Marker(mapView);
            mDriverMarker.setTitle("Vị trí của bạn");
            mDriverMarker.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_person));
            mDriverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(mDriverMarker);
        }
        mDriverMarker.setPosition(mCurrentLocation);
        mapView.invalidate();
    }


    /**
     * Xây dựng đối tượng Request để gọi API Routing (Giữ nguyên)
     */
    private RoutingRequestDto.RouteRequestDto buildRoutingRequest(GeoPoint startPoint, List<DeliveryAssignment> tasks) {
        // Chuyển đổi điểm bắt đầu
        RoutingRequestDto.WaypointDto startWaypoint = RoutingRequestDto.WaypointDto.builder()
                .lat(startPoint.getLatitude())
                .lon(startPoint.getLongitude())
                .parcelId(null)
                .build();

        // Phân nhóm
        Map<String, List<DeliveryAssignment>> groupedTasks = tasks.stream()
                .collect(Collectors.groupingBy(DeliveryAssignment::getDeliveryType));

        List<RoutingRequestDto.PriorityGroupDto> priorityGroups = new ArrayList<>();
        for (Map.Entry<String, List<DeliveryAssignment>> entry : groupedTasks.entrySet()) {
            int priority = mapDeliveryTypeToPriority(entry.getKey());

            List<RoutingRequestDto.WaypointDto> waypoints = entry.getValue().stream()
                    .map(task -> RoutingRequestDto.WaypointDto.builder()
                            .parcelId(task.getParcelId())
                            .lat(task.getLat().doubleValue())
                            .lon(task.getLon().doubleValue())
                            .build())
                    .collect(Collectors.toList());

            priorityGroups.add(RoutingRequestDto.PriorityGroupDto.builder()
                    .priority(priority)
                    .waypoints(waypoints)
                    .build());
        }

        // Xây dựng request
        return RoutingRequestDto.RouteRequestDto.builder()
                .startPoint(startWaypoint)
                .priorityGroups(priorityGroups)
                .steps(true)
                .annotations(true)
                .vehicle("motorbike")
                .mode("balanced")
                .strategy("flexible")
                .build();
    }

    /**
     * Xử lý DTO trả về (Giữ nguyên)
     */
    private RouteCalculationResult processRoutingResponse(RoutingResponseDto.RouteResponseDto response, List<DeliveryAssignment> originalTasks) {
        if (response == null || response.getVisitOrder() == null || response.getRoute() == null) {
            return new RouteCalculationResult(null, null, null, new Exception("Phản hồi tuyến đường không hợp lệ"));
        }

        // 1. Map tra cứu
        Map<String, DeliveryAssignment> originalTaskMap = originalTasks.stream()
                .collect(Collectors.toMap(DeliveryAssignment::getParcelId, task -> task, (task1, task2) -> task1));

        // 2. Map VisitOrder
        List<DeliveryAssignment> sortedTasks = new ArrayList<>();
        for (RoutingResponseDto.RouteResponseDto.VisitOrder visitOrder : response.getVisitOrder()) {
            String parcelId = visitOrder.getWaypoint().getParcelId();
            if (parcelId != null && originalTaskMap.containsKey(parcelId)) {
                sortedTasks.add(originalTaskMap.get(parcelId));
            } else {
                Log.w(TAG, "Không tìm thấy task cho parcelId: " + parcelId + " (trong visitOrder)");
            }
        }

        // 3. Tính toán Polylines
        List<ArrayList<GeoPoint>> allPolylines = new ArrayList<>();

        if (response.getRoute().getLegs().size() != response.getVisitOrder().size()) {
            Log.e(TAG, "Lỗi logic: Số lượng Legs (" + response.getRoute().getLegs().size()
                    + ") không bằng số lượng VisitOrder (" + response.getVisitOrder().size() + ")");
        }

        for (RoutingResponseDto.RouteLegDto leg : response.getRoute().getLegs()) {
            ArrayList<GeoPoint> routePoints = new ArrayList<>();

            if (leg.getParcelId() != null && !originalTaskMap.containsKey(leg.getParcelId())) {
                Log.w(TAG, "Không tìm thấy task cho parcelId: " + leg.getParcelId() + " (trong leg)");
            }

            for (RoutingResponseDto.RouteStepDto step : leg.getSteps()) {
                if (step.getGeometry() != null && step.getGeometry().getCoordinates() != null) {
                    for (List<Double> coord : step.getGeometry().getCoordinates()) {
                        routePoints.add(new GeoPoint(coord.get(1), coord.get(0))); // (lat, lon)
                    }
                }
            }
            allPolylines.add(routePoints);
        }

        return new RouteCalculationResult(response, sortedTasks, allPolylines, null);
    }

    private int mapDeliveryTypeToPriority(String deliveryType) {
        // (Giữ nguyên)
        try {
            DeliveryType type = DeliveryType.valueOf(deliveryType.toUpperCase());
            switch (type) {
                case URGENT: return 0;
                case EXPRESS: return 1;
                case FAST: return 2;
                case NORMAL: return 3;
                case ECONOMY: return 4;
                default: return 3;
            }
        } catch (Exception e) {
            return 3; // Mặc định là NORMAL
        }
    }


    private void setupOSMMap() {
        // (Giữ nguyên)
        if (mapView == null) return;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
    }


    /**
     * Hiển thị chặng hiện tại lên bản đồ.
     */
    private void displayCurrentLeg() {
        if (!isRouteLoaded || mPrecalculatedPolylines == null || mSortedTasks == null) {
            Log.w(TAG, "Dữ liệu chưa sẵn sàng để hiển thị.");
            return;
        }
        if (currentLegIndex >= mSortedTasks.size()) {
            Toast.makeText(getContext(), "Đã hoàn thành tất cả nhiệm vụ!", Toast.LENGTH_LONG).show();
            tvInstruction.setText("Đã hoàn thành tất cả nhiệm vụ!");
            fabNextTask.setEnabled(false);
            isNavigating = false; // --- NÂNG CẤP ---
            fabRecenter.setImageDrawable(iconRecenter); // --- NÂNG CẤP ---
            return;
        }

        // --- NÂNG CẤP: Reset step index khi bắt đầu chặng mới ---
        currentStepIndex = 0;

        // 1. Lấy dữ liệu
        DeliveryAssignment currentTask = mSortedTasks.get(currentLegIndex);
        RoutingResponseDto.RouteLegDto currentLeg = mRouteResponse.getRoute().getLegs().get(currentLegIndex);
        ArrayList<GeoPoint> routePoints = mPrecalculatedPolylines.get(currentLegIndex);

        if (routePoints.isEmpty()) {
            Log.e(TAG, "Lỗi: Polyline rỗng cho chặng " + currentLegIndex);
            completeCurrentLeg(); // Tự động bỏ qua chặng lỗi
            return;
        }

        // 2. Xác định điểm bắt đầu và kết thúc
        GeoPoint startPoint = routePoints.get(0);
        GeoPoint endPoint = routePoints.get(routePoints.size() - 1);

        // 3. Xóa các overlay cũ (trừ marker tài xế)
        mapView.getOverlays().clear();
        updateDriverMarker(); // Thêm lại marker tài xế

        // 4. Thêm Marker Điểm đến
        Marker endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setTitle(currentTask.getReceiverName());
        endMarker.setSnippet("Mã đơn: " + currentTask.getParcelCode());
        endMarker.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_navigation));
        mapView.getOverlays().add(endMarker);

        // 5. Vẽ đường đi
        Polyline roadOverlay = new Polyline();
        roadOverlay.setPoints(routePoints);
        roadOverlay.setColor(Color.BLUE);
        roadOverlay.setWidth(10.0f);
        mapView.getOverlays().add(roadOverlay);

        // 6. Cập nhật UI chỉ đường (bước đầu tiên)
        if (!currentLeg.getSteps().isEmpty()) {
            RoutingResponseDto.RouteStepDto firstStep = currentLeg.getSteps().get(currentStepIndex); // Dùng currentStepIndex
            tvInstruction.setText(firstStep.getInstruction());
            setTrafficColor(firstStep.getTrafficLevel());
        }

        // 7. Zoom bản đồ
        BoundingBox boundingBox = BoundingBox.fromGeoPoints(routePoints);
        mapView.zoomToBoundingBox(boundingBox, true, 100);

        // --- NÂNG CẤP: Tự động di chuyển camera đến vị trí tài xế ---
        if (isNavigating && mCurrentLocation != null) {
            mapView.getController().animateTo(mCurrentLocation);
        }

        mapView.invalidate();
    }

    private void setTrafficColor(String trafficLevel) {
        // (Giữ nguyên)
        if (trafficLevel == null) {
            tvInstruction.setBackgroundColor(Color.parseColor("#99000000")); // Mặc định
            return;
        }
        switch (trafficLevel.toUpperCase()) {
            case "NORMAL":
                tvInstruction.setBackgroundColor(Color.parseColor("#99008000")); // Xanh lá
                break;
            case "SLOW":
                tvInstruction.setBackgroundColor(Color.parseColor("#99FFA500")); // Cam
                break;
            case "CONGESTED":
                tvInstruction.setBackgroundColor(Color.parseColor("#99FF0000")); // Đỏ
                break;
            default:
                tvInstruction.setBackgroundColor(Color.parseColor("#99000000")); // Mặc định
        }
    }

    /**
     * Cài đặt listener cho các nút FAB
     */
    private void setupFabListeners() {
        fabListTasks.setOnClickListener(v -> {
            if (!isRouteLoaded || mSortedTasks == null || mSortedTasks.isEmpty()) {
                Toast.makeText(getContext(), "Đang tải nhiệm vụ, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                return;
            }
            TaskListDialogFragment dialog = TaskListDialogFragment.newInstance((ArrayList<DeliveryAssignment>) mSortedTasks);
            dialog.setOnTaskSelectedListener(this);
            dialog.show(getParentFragmentManager(), "TaskListDialog");
        });

        // --- NÂNG CẤP: Nút Next giờ chỉ là để "Bỏ qua" (Skip) ---
        fabNextTask.setOnClickListener(v -> {
            if (!isRouteLoaded || mSortedTasks == null) return;
            Toast.makeText(getContext(), "Bỏ qua chặng hiện tại...", Toast.LENGTH_SHORT).show();
            completeCurrentLeg(); // Gọi hàm hoàn thành (bỏ qua)
        });

        // --- NÂNG CẤP: Nút Recenter giờ là nút Bật/Tắt Điều hướng ---
        fabRecenter.setOnClickListener(v -> {
            isNavigating = !isNavigating; // Đảo trạng thái
            if (isNavigating) {
                // Bật điều hướng
                fabRecenter.setImageDrawable(iconNavigation);
                if (mCurrentLocation != null) {
                    mapView.getController().animateTo(mCurrentLocation);
                    mapView.getController().setZoom(18.0);
                }
                Toast.makeText(getContext(), "Đã bật chế độ điều hướng.", Toast.LENGTH_SHORT).show();
            } else {
                // Tắt điều hướng
                fabRecenter.setImageDrawable(iconRecenter);
                Toast.makeText(getContext(), "Đã tắt chế độ điều hướng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTaskSelected(DeliveryAssignment task) {
        // (Giữ nguyên)
        if (mSortedTasks == null) return;
        int foundIndex = -1;
        for (int i = 0; i < mSortedTasks.size(); i++) {
            if (mSortedTasks.get(i).getParcelId().equals(task.getParcelId())) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex != -1) {
            currentLegIndex = foundIndex;
            displayCurrentLeg();
            showTaskSnackbar(mSortedTasks.get(currentLegIndex));
        }
    }

    private void showTaskSnackbar(DeliveryAssignment assignment) {
        // (Giữ nguyên)
        if(assignment == null || coordinatorLayout == null) return;
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "", Snackbar.LENGTH_INDEFINITE);
        final View snackbarView = snackbar.getView();
        if (snackbarView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) snackbarView;
            if (viewGroup.getChildCount() > 0) {
                viewGroup.getChildAt(0).setVisibility(View.INVISIBLE);
            }
        }
        View customView = LayoutInflater.from(getContext()).inflate(R.layout.layout_task_snackbar, null);
        TextView tvCustomerName = customView.findViewById(R.id.tv_customer_name);
        TextView tvTaskDetails = customView.findViewById(R.id.tv_task_details);
        ImageButton btnCall = customView.findViewById(R.id.btn_call);

        tvCustomerName.setText(assignment.getReceiverName());
        tvTaskDetails.setText("Mã đơn: " + assignment.getParcelCode() + " | " + assignment.getDeliveryLocation());
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + assignment.getReceiverPhone()));
            startActivity(intent);
        });
        try {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            customView.setLayoutParams(params);
            ((ViewGroup) snackbarView).addView(customView, 0);
        } catch (ClassCastException e) {
            Log.e(TAG, "Lỗi thêm customView vào Snackbar: " + e.getMessage());
            return;
        }
        snackbarView.setPadding(0, 0, 0, 0);
        snackbar.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (locationManager != null && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL_MS, GPS_UPDATE_DISTANCE_M, this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    // Các hàm bắt buộc của LocationListener
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(@NonNull String provider) {}
    @Override
    public void onProviderDisabled(@NonNull String provider) {}
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "MapFragment onActivityResult: truyền sự kiện xuống children...");
        super.onActivityResult(requestCode, resultCode, data);
    }
}