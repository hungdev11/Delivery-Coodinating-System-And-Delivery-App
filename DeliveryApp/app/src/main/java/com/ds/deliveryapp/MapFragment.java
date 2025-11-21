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
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.RoutingResponseDto;
import com.ds.deliveryapp.clients.res.UpdateNotification;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.enums.DeliveryType;
import com.ds.deliveryapp.service.GlobalChatService;

// Models
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.SessionManager;

// UI
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

// OSMDroid
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
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

public class MapFragment extends Fragment implements TaskListDialogFragment.OnTaskSelectedListener, LocationListener, GlobalChatService.UpdateNotificationListener {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // --- CẤU HÌNH ĐIỀU HƯỚNG ---
    private static final double DEVIATION_THRESHOLD_METERS = 50.0; // Ngưỡng lệch 50m
    private static final double ARRIVAL_THRESHOLD_METERS = 25.0; // Vẫn giữ để tham khảo, nhưng không dùng
    private static final double NEXT_STEP_THRESHOLD_METERS = 20.0; // Ngưỡng hoàn thành 1 bước 20m
    private static final long GPS_UPDATE_INTERVAL_MS = 1000; // 1 giây
    private static final float GPS_UPDATE_DISTANCE_M = 5; // 5 mét

    // --- Các biến UI ---
    private FloatingActionButton fabListTasks, fabReloadRoute, fabRecenter;
    private CoordinatorLayout coordinatorLayout;
    private MapView mapView;
    private TextView tvInstruction;
    private ProgressBar progressBar;
    private LinearLayout legNavigationContainer;
    private ImageButton btnPrevLeg, btnNextLeg;
    private TextView tvLegInfo;

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
    private Polygon mDriverAura;

    // --- API Clients ---
    private String driverId;
    private SessionClient sessionClient;
    private RoutingApi routingApi;
    private GlobalChatService globalChatService;

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
        private final WeakReference<MapFragment> fragmentRef;
        private final GeoPoint startLocation;
        private final Gson gson;

        FetchAndRouteTask(MapFragment fragment, GeoPoint startLocation) {
            this.fragmentRef = new WeakReference<>(fragment);
            this.startLocation = startLocation;
            this.gson = new Gson();
        }

        @Override
        protected void onPreExecute() {
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
                return new RouteCalculationResult(null, null, null, new Exception("Fragment hoặc Start Location null"));
            }

            try {
                // 1. Lấy danh sách task nếu chưa có (Tối ưu: fetch từng page, early stop)
                if (fragment.mOriginalTasks == null || fragment.mOriginalTasks.isEmpty()) {
                    Log.d(TAG, "Đang lấy danh sách Task (tối ưu pagination)...");
                    List<DeliveryAssignment> allTasks = new ArrayList<>();
                    int page = 0;
                    final int pageSize = 20;
                    final int maxTasks = 50; // Limit max tasks thay vì 100
                    final int minTasksWithLocation = 5; // Minimum tasks có lat/lon để routing
                    
                    boolean hasMore = true;
                    int tasksWithLocation = 0;
                    
                    while (hasMore && allTasks.size() < maxTasks) {
                        Response<PageResponse<DeliveryAssignment>> taskResponse = fragment.sessionClient
                                .getSessionTasks(fragment.driverId, List.of("CREATED", "IN_PROGRESS"), page, pageSize)
                                .execute();

                        if (!taskResponse.isSuccessful()) {
                            if (taskResponse.code() == 204 || taskResponse.code() == 404) {
                                // No active session or session ended - cleanup map
                                Log.w(TAG, "No active session found (status: " + taskResponse.code() + "). Session may have ended.");
                                return new RouteCalculationResult(null, new ArrayList<>(), new ArrayList<>(), 
                                    new Exception("Session ended or no active session"));
                            }
                            if (page == 0) {
                                throw new IOException("Không thể lấy danh sách nhiệm vụ: " + taskResponse.code());
                            }
                            break; // Stop if error after first page
                        }
                        
                        if (taskResponse.body() == null || taskResponse.body().content() == null) {
                            if (page == 0) {
                                throw new IOException("Không thể lấy danh sách nhiệm vụ: response body is null");
                            }
                            break; // Stop if error after first page
                        }

                        PageResponse<DeliveryAssignment> pageResponse = taskResponse.body();
                        List<DeliveryAssignment> pageTasks = pageResponse.content();
                        
                        if (pageTasks == null || pageTasks.isEmpty()) {
                            hasMore = false;
                            break;
                        }
                        
                        // Count tasks with location in this page
                        int pageTasksWithLocation = (int) pageTasks.stream()
                                .filter(task -> "IN_PROGRESS".equals(task.getStatus()) && task.getLat() != null && task.getLon() != null)
                                .count();
                        tasksWithLocation += pageTasksWithLocation;
                        
                        allTasks.addAll(pageTasks);
                        hasMore = !pageResponse.last();
                        
                        // Early stop: Đủ tasks có lat/lon cho routing
                        if (tasksWithLocation >= minTasksWithLocation) {
                            Log.d(TAG, "Early stop: Đã có " + tasksWithLocation + " tasks có lat/lon, đủ cho routing");
                            break;
                        }
                        
                        page++;
                        Log.d(TAG, "Fetched page " + page + ": " + pageTasks.size() + " tasks, " + pageTasksWithLocation + " with location");
                    }
                    
                    // Filter chỉ lấy IN_PROGRESS tasks có lat/lon
                    fragment.mOriginalTasks = allTasks.stream()
                            .filter(task -> "IN_PROGRESS".equals(task.getStatus()) && task.getLat() != null && task.getLon() != null)
                            .collect(Collectors.toList());
                    
                    Log.d(TAG, "Tổng số tasks có lat/lon sau filter: " + fragment.mOriginalTasks.size());
                }

                if (fragment.mOriginalTasks == null || fragment.mOriginalTasks.isEmpty()) {
                    return new RouteCalculationResult(null, new ArrayList<>(), new ArrayList<>(), null);
                }

                // 2. Tạo routing payload
                RoutingRequestDto.RouteRequestDto routeRequest = fragment.buildRoutingRequest(startLocation, fragment.mOriginalTasks);
                String payloadJson = gson.toJson(routeRequest);
                Log.d(TAG, "Routing API Payload: " + payloadJson.substring(0, Math.min(1000, payloadJson.length())));

                // 3. Gọi API Routing
                Response<RoutingResponseDto> routeApiResponse = fragment.routingApi
                        .getOptimalRoute(routeRequest)
                        .execute();

                if (!routeApiResponse.isSuccessful() || routeApiResponse.body() == null) {
                    throw new IOException("Không thể lấy tuyến đường: " + routeApiResponse.message());
                }

                RoutingResponseDto routeResponseWrapper = routeApiResponse.body();
                RoutingResponseDto.RouteResponseDto routeResponse = routeResponseWrapper.getResult();
                Log.d(TAG, "Routing API Response: " + gson.toJson(routeResponse).substring(0, Math.min(1000, gson.toJson(routeResponse).length())));

                // 4. Xử lý kết quả
                return fragment.processRoutingResponse(routeResponse, fragment.mOriginalTasks);

            } catch (Exception e) {
                Log.e(TAG, "Lỗi trong FetchAndRouteTask: ", e);
                return new RouteCalculationResult(null, null, null, e);
            }
        }

        @Override
        protected void onPostExecute(RouteCalculationResult result) {
            MapFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                return;
            }

            if (fragment.progressBar != null)
                fragment.progressBar.setVisibility(View.GONE);

            fragment.isRecalculating = false;

            // Re-enable FAB after route calculation completes
            if (fragment.fabReloadRoute != null) {
                fragment.fabReloadRoute.setEnabled(true);
                fragment.fabReloadRoute.setAlpha(1.0f);
            }

            if (result.exception != null) {
                String errorMessage = result.exception.getMessage();
                if (errorMessage != null && errorMessage.contains("Session ended")) {
                    // Session ended - cleanup map
                    Log.w(TAG, "Session ended. Cleaning up map...");
                    fragment.cleanupMap();
                    Toast.makeText(fragment.getContext(), "Phiên đã kết thúc. Đã dọn dẹp bản đồ.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(fragment.getContext(), "Lỗi tải dữ liệu: " + errorMessage, Toast.LENGTH_LONG).show();
                }
                return;
            }

            if (result.sortedTasks == null || result.sortedTasks.isEmpty()) {
                // Check if session ended (no tasks means no active session)
                fragment.checkSessionStatusAndCleanup();
                
                Toast.makeText(fragment.getContext(), "Không có nhiệm vụ nào cần xử lý.", Toast.LENGTH_LONG).show();
                fragment.isRouteLoaded = false;
                fragment.mOriginalTasks = null;
                fragment.mSortedTasks = null;
                fragment.mPrecalculatedPolylines = null;

                if (fragment.mapView != null) {
                    fragment.mapView.getOverlays().clear();
                    fragment.updateDriverMarker();
                }

                if (fragment.tvInstruction != null) {
                    fragment.tvInstruction.setText("Không có nhiệm vụ.");
                }

                // FAB already enabled above

                if (fragment.legNavigationContainer != null) {
                    fragment.legNavigationContainer.setVisibility(View.GONE);
                }
                return;
            }

            // Nếu có route
            fragment.mRouteResponse = result.routeResponse;
            fragment.mSortedTasks = result.sortedTasks;
            fragment.mPrecalculatedPolylines = result.polylines;
            fragment.isRouteLoaded = true;
            fragment.isNavigating = true;

            fragment.currentLegIndex = 0;
            fragment.displayCurrentLeg();
            fragment.updateLegNavigationUI();

            if (!fragment.mSortedTasks.isEmpty()) {
                fragment.showTaskSnackbar(fragment.mSortedTasks.get(fragment.currentLegIndex));
            }

            fragment.startGpsTracking();
        }
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize GlobalChatService and register update notification listener
        globalChatService = GlobalChatService.getInstance(requireContext());
        globalChatService.addListener(this);
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
        fabReloadRoute = view.findViewById(R.id.fab_reload_task); // Dùng ID cũ nhưng gán vào biến mới
        fabRecenter = view.findViewById(R.id.fab_recenter); // Nút mới
        tvInstruction = view.findViewById(R.id.tv_instruction);
        progressBar = view.findViewById(R.id.progress_bar);
        legNavigationContainer = view.findViewById(R.id.leg_navigation_container);
        btnPrevLeg = view.findViewById(R.id.btn_prev_leg);
        btnNextLeg = view.findViewById(R.id.btn_next_leg);
        tvLegInfo = view.findViewById(R.id.tv_leg_info);

        // --- NÂNG CẤP: Tải icon cho nút điều hướng ---
        iconRecenter = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_mylocation);
        // --- SỬA LỖI: Quay lại dùng R.drawable.ic_navigation của bạn, thêm kiểm tra null ---
        try {
            iconNavigation = ContextCompat.getDrawable(getContext(), R.drawable.ic_navigation);
            if (iconNavigation == null) { // Fallback nếu R.drawable.ic_navigation bị null
                iconNavigation = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_send);
            }
        } catch (Exception e) { // Fallback nếu R.drawable.ic_navigation không tồn tại
            iconNavigation = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_send);
        }
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
        setupLegNavigation();
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
            // --- THAY ĐỔI ZOOM: Tăng mức zoom ban đầu ---
            mapView.getController().setZoom(18.0);
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

        // --- NÂNG CẤP: Tự động di chuyển camera (Focus mode) ---
        if (isNavigating) {
            // --- FOCUS MODE: Luôn focus vào vị trí driver và giữ mức zoom 18 ---
            mapView.getController().setZoom(18.0);
            mapView.getController().animateTo(mCurrentLocation);
            // --- MAP ROTATION: Ưu tiên theo hướng route, fallback GPS bearing ---
            float bearing = -1;
            if (isRouteLoaded && mRouteResponse != null && mCurrentLocation != null) {
                // Ưu tiên: Tính bearing từ route geometry (hướng cần đi)
                bearing = calculateRouteBearing(location);
            }
            if (bearing < 0 && location.hasBearing()) {
                // Fallback: Dùng GPS bearing nếu không tính được route bearing
                bearing = location.getBearing();
            }
            if (bearing >= 0) {
                mapView.setMapOrientation(-bearing);
                boolean fromRoute = bearing >= 0 && isRouteLoaded && mRouteResponse != null;
                Log.d(TAG, "Map rotation updated: bearing=" + bearing + " (from route: " + fromRoute + ")");
            }
        }
        // Mode khác (!isNavigating): không auto-rotate, user tự do xoay map

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
     * NÂNG CẤP: Hàm xử lý logic điều hướng từng bước.
     * --- THAY ĐỔI: Đã gỡ bỏ logic tự động chuyển chặng ---
     */
    private void updateNavigationState(GeoPoint currentLocation) {
        if (isRecalculating || mPrecalculatedPolylines == null || mRouteResponse == null || currentLegIndex >= mPrecalculatedPolylines.size()) {
            return;
        }

        // 2. Kiểm tra xem đã hoàn thành BƯỚC (Step) hiện tại chưa
        RoutingResponseDto.RouteLegDto currentLeg = mRouteResponse.getRoute().getLegs().get(currentLegIndex);
        List<RoutingResponseDto.RouteStepDto> steps = currentLeg.getSteps();

        if (currentStepIndex >= steps.size()) {
            return; // Đã ở bước cuối cùng, chờ đến điểm (hoặc chờ reload)
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
                if (mSortedTasks != null && currentLegIndex < mSortedTasks.size()) {
                    tvInstruction.setText("Sắp đến: " + mSortedTasks.get(currentLegIndex).getReceiverName());
                }
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
     * Tính bearing từ vị trí hiện tại đến điểm tiếp theo trong current step geometry
     * Ưu tiên dùng trong focus mode để xoay map theo hướng cần đi
     * @param location Vị trí GPS hiện tại
     * @return Bearing trong độ (0-360) hoặc -1 nếu không tính được
     */
    private float calculateRouteBearing(Location location) {
        if (mRouteResponse == null || mRouteResponse.getRoute() == null || 
            mRouteResponse.getRoute().getLegs() == null || 
            currentLegIndex < 0 || currentLegIndex >= mRouteResponse.getRoute().getLegs().size()) {
            return -1;
        }

        try {
            RoutingResponseDto.RouteLegDto currentLeg = mRouteResponse.getRoute().getLegs().get(currentLegIndex);
            List<RoutingResponseDto.RouteStepDto> steps = currentLeg.getSteps();
            
            if (steps == null || steps.isEmpty() || currentStepIndex < 0 || currentStepIndex >= steps.size()) {
                return -1;
            }

            RoutingResponseDto.RouteStepDto currentStep = steps.get(currentStepIndex);
            if (currentStep.getGeometry() == null || currentStep.getGeometry().getCoordinates() == null ||
                currentStep.getGeometry().getCoordinates().isEmpty()) {
                return -1;
            }

            List<List<Double>> coordinates = currentStep.getGeometry().getCoordinates();
            GeoPoint currentPoint = mCurrentLocation;
            
            // Tìm điểm gần nhất tiếp theo trong step geometry từ vị trí hiện tại
            GeoPoint nextPoint = null;
            double minDistance = Double.MAX_VALUE;
            
            for (List<Double> coord : coordinates) {
                if (coord == null || coord.size() < 2) continue;
                
                GeoPoint point = new GeoPoint(coord.get(1), coord.get(0)); // lat, lon
                double distance = currentPoint.distanceToAsDouble(point);
                
                // Chỉ lấy điểm phía trước (distance > 10m để tránh nhiễu)
                if (distance > 10.0 && distance < minDistance) {
                    minDistance = distance;
                    nextPoint = point;
                }
            }
            
            if (nextPoint == null) {
                // Nếu không tìm thấy điểm tiếp theo, dùng điểm cuối của step
                List<Double> lastCoord = coordinates.get(coordinates.size() - 1);
                nextPoint = new GeoPoint(lastCoord.get(1), lastCoord.get(0));
            }
            
            // Tính bearing giữa vị trí hiện tại và điểm tiếp theo
            double lat1 = Math.toRadians(currentPoint.getLatitude());
            double lon1 = Math.toRadians(currentPoint.getLongitude());
            double lat2 = Math.toRadians(nextPoint.getLatitude());
            double lon2 = Math.toRadians(nextPoint.getLongitude());
            
            double deltaLon = lon2 - lon1;
            double y = Math.sin(deltaLon) * Math.cos(lat2);
            double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
            
            double bearingRad = Math.atan2(y, x);
            double bearingDeg = Math.toDegrees(bearingRad);
            
            // Normalize bearing to 0-360
            float bearing = (float) ((bearingDeg + 360) % 360);
            
            return bearing;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi tính route bearing: " + e.getMessage());
            return -1;
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
            
            // Disable FAB and show loading during recalculation
            if (fabReloadRoute != null) {
                fabReloadRoute.setEnabled(false);
                fabReloadRoute.setAlpha(0.5f);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            
            // Xóa danh sách task cũ để AsyncTask tải lại (nếu cần)
            // mOriginalTasks = null; // Không nên xóa, chỉ cần tính lại đường
            new FetchAndRouteTask(this, currentLocation).execute();
        }
    }

    private void updateDriverMarker() {
        if (mCurrentLocation == null || mapView == null) return;

        // --- KHỞI TẠO MARKER ---
        if (mDriverMarker == null) {
            mDriverMarker = new Marker(mapView);
            mDriverMarker.setTitle("Vị trí của bạn");
            // Dùng icon 'iconNavigation' đã tải an toàn
            if (iconNavigation != null) {
                mDriverMarker.setIcon(iconNavigation);
            }
            // Căn giữa icon mũi tên
            mDriverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        }

        // --- KHỞI TẠO AURA ---
        if (mDriverAura == null) {
            mDriverAura = new Polygon(mapView);
            // Màu xanh sáng, bán trong suốt
            mDriverAura.getFillPaint().setColor(Color.parseColor("#800077FF"));
            // Viền xanh đậm
            mDriverAura.getFillPaint().setColor(Color.parseColor("#FF0033AA"));
            mDriverAura.getFillPaint().setStrokeWidth(2.0f);
        }

        // --- CẬP NHẬT VỊ TRÍ ---
        mDriverMarker.setPosition(mCurrentLocation);
        // Vẽ vòng tròn bán kính 20m xung quanh
        mDriverAura.setPoints(Polygon.pointsAsCircle(mCurrentLocation, 20.0));

        // --- ĐẢM BẢO HIỂN THỊ (QUAN TRỌNG) ---
        // Xóa (nếu có)
        mapView.getOverlays().remove(mDriverAura);
        mapView.getOverlays().remove(mDriverMarker);
        // Thêm lại (Aura vẽ trước, Marker vẽ trên)
        mapView.getOverlays().add(mDriverAura);
        mapView.getOverlays().add(mDriverMarker);

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

        if (response.getRoute() == null || response.getRoute().getLegs() == null) {
            return new RouteCalculationResult(null, null, null, new Exception("Phản hồi tuyến đường không có 'legs'"));
        }

        // Logic so khớp legs và visitOrder (chặng đầu tiên là từ tài xế -> điểm 1)
        // Giả định: legs.size() == visitOrder.size()
        if (response.getRoute().getLegs().size() != sortedTasks.size()) {
            Log.w(TAG, "Lỗi logic: Số lượng Legs (" + response.getRoute().getLegs().size()
                    + ") không bằng số lượng Task đã sắp xếp (" + sortedTasks.size() + ")");
        }

        // Gắn Polyline dựa trên index
        for (int i = 0; i < response.getRoute().getLegs().size(); i++) {
            RoutingResponseDto.RouteLegDto leg = response.getRoute().getLegs().get(i);
            ArrayList<GeoPoint> routePoints = new ArrayList<>();

            if (leg.getSteps() != null) {
                for (RoutingResponseDto.RouteStepDto step : leg.getSteps()) {
                    if (step.getGeometry() != null && step.getGeometry().getCoordinates() != null) {
                        for (List<Double> coord : step.getGeometry().getCoordinates()) {
                            routePoints.add(new GeoPoint(coord.get(1), coord.get(0))); // (lat, lon)
                        }
                    }
                }
            }
            allPolylines.add(routePoints);

            // Nếu số lượng legs và sortedTasks không khớp, dừng lại
            if(i >= sortedTasks.size() - 1) {
                break;
            }
        }

        // Cắt bớt sortedTasks nếu legs ít hơn
        while(sortedTasks.size() > allPolylines.size()){
            sortedTasks.remove(sortedTasks.size() - 1);
        }

        return new RouteCalculationResult(response, sortedTasks, allPolylines, null);
    }

    private int mapDeliveryTypeToPriority(String deliveryType) {
        // (Giữ nguyên)
        try {
            DeliveryType type = DeliveryType.valueOf(deliveryType.toUpperCase());
            switch (type) {
                case URGENT: return 10;
                case EXPRESS: return 4;
                case FAST: return 3;
                case NORMAL: return 2;
                case ECONOMY: return 1;
                default: return 0;
            }
        } catch (Exception e) {
            return 2; // Mặc định là NORMAL
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
     * Setup navigation buttons for leg-by-leg navigation
     */
    private void setupLegNavigation() {
        btnPrevLeg.setOnClickListener(v -> navigateToPreviousLeg());
        btnNextLeg.setOnClickListener(v -> navigateToNextLeg());
    }

    private void navigateToPreviousLeg() {
        if (currentLegIndex > 0) {
            currentLegIndex--;
            displayCurrentLeg();
            updateLegNavigationUI();
        }
    }

    private void navigateToNextLeg() {
        if (mSortedTasks != null && currentLegIndex < mSortedTasks.size() - 1) {
            currentLegIndex++;
            displayCurrentLeg();
            updateLegNavigationUI();
        }
    }

    private void updateLegNavigationUI() {
        if (mSortedTasks == null || mSortedTasks.isEmpty()) {
            if (legNavigationContainer != null) {
                legNavigationContainer.setVisibility(View.GONE);
            }
            return;
        }

        if (legNavigationContainer != null) {
            legNavigationContainer.setVisibility(View.VISIBLE);
        }

        if (tvLegInfo != null) {
            tvLegInfo.setText((currentLegIndex + 1) + " / " + mSortedTasks.size());
        }

        // Enable/disable navigation buttons
        if (btnPrevLeg != null) {
            btnPrevLeg.setEnabled(currentLegIndex > 0);
            btnPrevLeg.setAlpha(currentLegIndex > 0 ? 1.0f : 0.5f);
        }

        if (btnNextLeg != null) {
            btnNextLeg.setEnabled(currentLegIndex < mSortedTasks.size() - 1);
            btnNextLeg.setAlpha(currentLegIndex < mSortedTasks.size() - 1 ? 1.0f : 0.5f);
        }
    }

    /**
     * Hiển thị chặng hiện tại lên bản đồ.
     */
    private void displayCurrentLeg() {
        if (!isRouteLoaded || mPrecalculatedPolylines == null || mSortedTasks == null || mRouteResponse == null) {
            Log.w(TAG, "Dữ liệu chưa sẵn sàng để hiển thị.");
            return;
        }
        if (currentLegIndex >= mSortedTasks.size()) {
            Toast.makeText(getContext(), "Đã hoàn thành tất cả nhiệm vụ!", Toast.LENGTH_LONG).show();
            tvInstruction.setText("Đã hoàn thành tất cả nhiệm vụ!");
            fabReloadRoute.setEnabled(false); // --- THAY ĐỔI: Tắt nút fabReloadRoute
            isNavigating = false;
            fabRecenter.setImageDrawable(iconRecenter);

            // Xóa task, chuẩn bị cho lần reload sau
            mOriginalTasks = null;
            mSortedTasks = null;
            isRouteLoaded = false;

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
            // Bỏ qua chặng lỗi này bằng cách tăng index và gọi lại
            currentLegIndex++;
            displayCurrentLeg();
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
        // --- SỬA LỖI CRASH: Xóa dòng setIcon để dùng icon mặc định (cờ đỏ) ---
        mapView.getOverlays().add(endMarker);

        // 5. Vẽ đường đi
        Polyline roadOverlay = new Polyline();
        roadOverlay.setPoints(routePoints);
        roadOverlay.setColor(Color.BLUE);
        roadOverlay.setWidth(10.0f);
        mapView.getOverlays().add(roadOverlay);

        // 6. Cập nhật UI chỉ đường (bước đầu tiên)
        if (currentLeg.getSteps() != null && !currentLeg.getSteps().isEmpty()) {
            RoutingResponseDto.RouteStepDto firstStep = currentLeg.getSteps().get(currentStepIndex); // Dùng currentStepIndex
            tvInstruction.setText(firstStep.getInstruction());
            setTrafficColor(firstStep.getTrafficLevel());
        } else {
            tvInstruction.setText("Đi thẳng đến điểm tiếp theo.");
        }

        // 7. Zoom bản đồ
        BoundingBox boundingBox = BoundingBox.fromGeoPoints(routePoints);
        mapView.zoomToBoundingBox(boundingBox, true, 100);

        // --- NÂNG CẤP: Tự động di chuyển camera đến vị trí tài xế ---
        if (isNavigating && mCurrentLocation != null) {
            mapView.getController().animateTo(mCurrentLocation);
            mapView.getController().setZoom(18.0); // Ép zoom 18
        }

        // Update leg navigation UI
        updateLegNavigationUI();

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

        // Đặt icon là 'refresh'
        fabReloadRoute.setImageDrawable(ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_rotate));
        fabReloadRoute.setOnClickListener(v -> {
            if (mCurrentLocation == null) {
                Toast.makeText(getContext(), "Chưa có vị trí, không thể tải lại.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isRecalculating) {
                Toast.makeText(getContext(), "Đang tính toán...", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable FAB and show loading
            fabReloadRoute.setEnabled(false);
            fabReloadRoute.setAlpha(0.5f);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            Toast.makeText(getContext(), "Đang tải lại tuyến đường...", Toast.LENGTH_SHORT).show();

            // --- QUAN TRỌNG: Xóa danh sách task cũ để nó tải lại từ server ---
            mOriginalTasks = null;

            // Gọi lại AsyncTask với vị trí hiện tại
            new FetchAndRouteTask(this, mCurrentLocation).execute();
        });

        // --- NÂNG CẤP: Nút Lock/Focus Toggle - Bật/Tắt Focus mode (lock camera vào driver) ---
        fabRecenter.setOnClickListener(v -> {
            isNavigating = !isNavigating; // Toggle lock/focus mode
            if (isNavigating) {
                // Bật Focus mode: Lock camera vào driver, tự động follow và rotate
                fabRecenter.setImageDrawable(iconNavigation);
                if (mCurrentLocation != null) {
                    mapView.getController().animateTo(mCurrentLocation);
                    mapView.getController().setZoom(18.0);
                    // Reset rotation if location has bearing
                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLocation != null && lastLocation.hasBearing()) {
                        mapView.setMapOrientation(-lastLocation.getBearing());
                    }
                }
                Toast.makeText(getContext(), "Đã bật Focus mode - Camera sẽ tự động theo dõi vị trí.", Toast.LENGTH_SHORT).show();
            } else {
                // Tắt Focus mode: Manual camera control
                fabRecenter.setImageDrawable(iconRecenter);
                Toast.makeText(getContext(), "Đã tắt Focus mode - Bạn có thể di chuyển camera tự do.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTaskSelected(DeliveryAssignment task) {
        // (Giữ nguyên)
        if (mSortedTasks == null) return;
        int foundIndex = -1;
        for (int i = 0; i< mSortedTasks.size(); i++) {
            if (mSortedTasks.get(i).getParcelId().equals(task.getParcelId())) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex != -1) {
            currentLegIndex = foundIndex;
            displayCurrentLeg();
            updateLegNavigationUI();
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

        // Check session status first - cleanup map if session ended
        checkSessionStatusAndCleanup();

        // Bắt đầu lại GPS tracking khi quay lại app
        if (locationManager != null && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL_MS, GPS_UPDATE_DISTANCE_M, this);
        }

        // Tối ưu: Chỉ reload route khi thực sự cần
        // (Route chưa được load hoặc có flag forceReload)
        if (!isRecalculating && mCurrentLocation != null) {
            if (!isRouteLoaded) {
                // Route chưa load: Tải route (only if session is still active)
                if (isSessionActive()) {
                    Log.d(TAG, "onResume: Route chưa load, tải route...");
                    new FetchAndRouteTask(this, mCurrentLocation).execute();
                } else {
                    Log.d(TAG, "onResume: Session ended. Skipping route fetch.");
                }
            } else {
                // Route đã load: Không reload, chỉ khôi phục GPS frequency về bình thường
                Log.d(TAG, "onResume: Route đã load, không reload (cache route data)");
                // Khôi phục GPS update frequency về bình thường (1s)
                if (locationManager != null && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.removeUpdates(this);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL_MS, GPS_UPDATE_DISTANCE_M, this);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        // Tối ưu: Không remove GPS updates nếu đang trong navigation mode
        // Chỉ pause GPS khi fragment bị destroy hoàn toàn hoặc không đang navigate
        if (locationManager != null) {
            if (isNavigating && isRouteLoaded) {
                // Đang navigate: Giảm frequency thay vì remove hoàn toàn (interval = 5s)
                locationManager.removeUpdates(this);
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, GPS_UPDATE_DISTANCE_M, this);
                    Log.d(TAG, "onPause: Giảm GPS update frequency (5s) thay vì remove (đang navigate)");
                }
            } else {
                // Không navigate: Remove updates như bình thường
                locationManager.removeUpdates(this);
                Log.d(TAG, "onPause: Removed GPS updates (không đang navigate)");
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu state quan trọng của MapFragment
        outState.putInt("currentLegIndex", currentLegIndex);
        outState.putInt("currentStepIndex", currentStepIndex);
        outState.putBoolean("isRouteLoaded", isRouteLoaded);
        outState.putBoolean("isNavigating", isNavigating);
        if (mCurrentLocation != null) {
            outState.putDouble("currentLat", mCurrentLocation.getLatitude());
            outState.putDouble("currentLon", mCurrentLocation.getLongitude());
        }
        Log.d(TAG, "onSaveInstanceState: Saved state - legIndex=" + currentLegIndex + ", stepIndex=" + currentStepIndex + ", isRouteLoaded=" + isRouteLoaded);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // Restore state khi fragment được recreate
        if (savedInstanceState != null) {
            currentLegIndex = savedInstanceState.getInt("currentLegIndex", 0);
            currentStepIndex = savedInstanceState.getInt("currentStepIndex", 0);
            isRouteLoaded = savedInstanceState.getBoolean("isRouteLoaded", false);
            isNavigating = savedInstanceState.getBoolean("isNavigating", true);
            if (savedInstanceState.containsKey("currentLat") && savedInstanceState.containsKey("currentLon")) {
                double lat = savedInstanceState.getDouble("currentLat");
                double lon = savedInstanceState.getDouble("currentLon");
                mCurrentLocation = new GeoPoint(lat, lon);
            }
            Log.d(TAG, "onViewStateRestored: Restored state - legIndex=" + currentLegIndex + ", stepIndex=" + currentStepIndex + ", isRouteLoaded=" + isRouteLoaded);
            
            // Nếu route đã load và có đủ data, hiển thị lại current leg
            // Note: Route data (mRouteResponse, mPrecalculatedPolylines, mSortedTasks) 
            // có thể không được restore vì không thể serialize
            // Nên chỉ cập nhật UI nếu data vẫn còn trong memory
            if (isRouteLoaded && mRouteResponse != null && mPrecalculatedPolylines != null && mSortedTasks != null) {
                displayCurrentLeg();
            } else {
                // Nếu route data không còn, reset state và sẽ reload khi có GPS
                Log.d(TAG, "onViewStateRestored: Route data not available, resetting state");
                isRouteLoaded = false;
                currentLegIndex = 0;
                currentStepIndex = 0;
                updateLegNavigationUI();
            }
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
    
    /**
     * Check session status and cleanup map if session ended (COMPLETED or FAILED)
     */
    private void checkSessionStatusAndCleanup() {
        if (driverId == null || sessionClient == null) {
            return;
        }
        
        // Check session status asynchronously
        new Thread(() -> {
            try {
                Response<BaseResponse<DeliverySession>> response = sessionClient.getActiveSession(driverId).execute();
                
                if (!response.isSuccessful() || response.body() == null) {
                    // No active session - session ended (COMPLETED or FAILED)
                    Log.w(TAG, "No active session found. Session may have ended. Cleaning up map...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> cleanupMap());
                    }
                    return;
                }
                
                BaseResponse<DeliverySession> baseResponse = response.body();
                if (baseResponse.getResult() == null) {
                    // No active session (result is null)
                    Log.w(TAG, "No active session found: " + baseResponse.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> cleanupMap());
                    }
                    return;
                }
                
                DeliverySession session = baseResponse.getResult();
                String status = session.getStatus();
                
                if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                    // Session ended - cleanup map
                    Log.w(TAG, "Session ended with status: " + status + ". Cleaning up map...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> cleanupMap());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking session status: " + e.getMessage());
                // Don't cleanup on error - might be network issue
            }
        }).start();
    }
    
    /**
     * Check if session is still active (non-blocking check)
     * Returns true if route is loaded (assumes session is active)
     */
    private boolean isSessionActive() {
        // Quick check: if route is loaded, assume session is active
        // This avoids blocking the UI thread
        // Will be updated by checkSessionStatusAndCleanup() if session ended
        return isRouteLoaded;
    }
    
    /**
     * Cleanup map when session ends: remove routes, markers, polylines, reset state
     */
    private void cleanupMap() {
        Log.d(TAG, "🧹 Cleaning up map: removing routes, markers, polylines...");
        
        // Reset route state
        isRouteLoaded = false;
        isNavigating = false;
        isRecalculating = false;
        currentLegIndex = 0;
        currentStepIndex = 0;
        
        // Clear route data
        mOriginalTasks = null;
        mSortedTasks = null;
        mRouteResponse = null;
        mPrecalculatedPolylines = null;
        
        // Clear map overlays (routes, markers, polylines)
        if (mapView != null) {
            mapView.getOverlays().clear();
            // Keep only driver marker and aura
            if (mCurrentLocation != null) {
                updateDriverMarker();
            }
            mapView.invalidate();
        }
        
        // Reset UI
        if (tvInstruction != null) {
            tvInstruction.setText("Phiên đã kết thúc. Không có tuyến đường.");
        }
        
        if (legNavigationContainer != null) {
            legNavigationContainer.setVisibility(View.GONE);
        }
        
        if (fabReloadRoute != null) {
            fabReloadRoute.setEnabled(false);
        }
        
        // Reset map rotation
        if (mapView != null) {
            mapView.setMapOrientation(0);
        }
        
        Log.d(TAG, "✅ Map cleanup completed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister update notification listener
        if (globalChatService != null) {
            globalChatService.removeListener(this);
        }
    }

    // ==================== GlobalChatService.UpdateNotificationListener ====================
    
    @Override
    public void onMessageReceived(com.ds.deliveryapp.clients.res.Message message) {
        // Not used in MapFragment
    }

    @Override
    public void onUnreadCountChanged(int count) {
        // Not used in MapFragment
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        // Not used in MapFragment
    }

    @Override
    public void onError(String error) {
        // Not used in MapFragment
    }

    @Override
    public void onNotificationReceived(String notificationJson) {
        // Not used in MapFragment
    }

    @Override
    public void onUpdateNotificationReceived(UpdateNotification updateNotification) {
        Log.d(TAG, String.format("📥 Update notification received: type=%s, entityType=%s, entityId=%s, action=%s", 
            updateNotification.getUpdateType(), 
            updateNotification.getEntityType(), 
            updateNotification.getEntityId(), 
            updateNotification.getAction()));
        
        // Handle update notification on UI thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                handleUpdateNotification(updateNotification);
            });
        }
    }
    
    /**
     * Handle update notification and refresh route accordingly
     */
    private void handleUpdateNotification(UpdateNotification updateNotification) {
        if (updateNotification == null) {
            return;
        }
        
        UpdateNotification.EntityType entityType = updateNotification.getEntityType();
        UpdateNotification.ActionType action = updateNotification.getAction();
        String entityId = updateNotification.getEntityId();
        
        // Handle SESSION_UPDATE: cleanup map if session ended, reload route if session started
        if (entityType == UpdateNotification.EntityType.SESSION) {
            if (action == UpdateNotification.ActionType.COMPLETED || 
                action == UpdateNotification.ActionType.FAILED || 
                action == UpdateNotification.ActionType.CANCELLED) {
                // Session ended - cleanup map
                Log.d(TAG, "Session ended (action: " + action + "). Cleaning up map...");
                cleanupMap();
            } else if (action == UpdateNotification.ActionType.CREATED || 
                       action == UpdateNotification.ActionType.STATUS_CHANGED) {
                // Session created or status changed - reload route if needed
                Log.d(TAG, "Session updated (action: " + action + "). Reloading route...");
                if (mCurrentLocation != null && !isRecalculating) {
                    // Reload route with current location
                    new FetchAndRouteTask(this, mCurrentLocation).execute();
                }
            }
        }
        // Handle ASSIGNMENT_UPDATE: reload route if assignment status changed
        else if (entityType == UpdateNotification.EntityType.ASSIGNMENT) {
            if (action == UpdateNotification.ActionType.CREATED || 
                action == UpdateNotification.ActionType.UPDATED || 
                action == UpdateNotification.ActionType.STATUS_CHANGED ||
                action == UpdateNotification.ActionType.COMPLETED ||
                action == UpdateNotification.ActionType.FAILED) {
                // Assignment updated - reload route (route might change)
                Log.d(TAG, "Assignment updated (action: " + action + "). Reloading route...");
                if (mCurrentLocation != null && !isRecalculating && isRouteLoaded) {
                    // Reload route with current location
                    new FetchAndRouteTask(this, mCurrentLocation).execute();
                }
            }
        }
        // Handle PARCEL_UPDATE: reload route if parcel status changed (might affect route)
        else if (entityType == UpdateNotification.EntityType.PARCEL) {
            if (action == UpdateNotification.ActionType.STATUS_CHANGED || 
                action == UpdateNotification.ActionType.UPDATED) {
                // Parcel updated - reload route if route is loaded (parcel status might affect route)
                Log.d(TAG, "Parcel updated (action: " + action + "). Reloading route...");
                if (mCurrentLocation != null && !isRecalculating && isRouteLoaded) {
                    // Reload route with current location
                    new FetchAndRouteTask(this, mCurrentLocation).execute();
                }
            }
        }
    }
}
