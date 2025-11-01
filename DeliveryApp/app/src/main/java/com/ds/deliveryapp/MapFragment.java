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
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.utils.SessionManager;
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
    private String driverId;
    private static final String TAG = "MapFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(getContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        coordinatorLayout = (CoordinatorLayout) view;
        mapView = view.findViewById(R.id.map_view_osm);
        fabListTasks = view.findViewById(R.id.fab_list_tasks);

        // --- Lấy driverId từ SessionManager ---
        SessionManager sessionManager = new SessionManager(requireContext());
        driverId = sessionManager.getDriverId();

        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy tài xế. Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
            return view;
        }

        setupOSMMap();
        fetchTodayTasks(driverId);
        setupFabListener();
        return view;
    }

    private void setupOSMMap() {
        if (mapView == null) return;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(10.8231, 106.6297);
        mapView.getController().setCenter(startPoint);
    }

    // --- Gọi API lấy nhiệm vụ ---
    private void fetchTodayTasks(String driverId) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(getContext());
        SessionClient service = retrofit.create(SessionClient.class);

        Call<PageResponse<DeliveryAssignment>> call =
                service.getTasksToday(driverId, List.of("IN_PROGRESS"), 0, 10);

        call.enqueue(new Callback<PageResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<PageResponse<DeliveryAssignment>> call,
                                   Response<PageResponse<DeliveryAssignment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tasks.clear();
                    tasks.addAll(response.body().content());
                    if (!tasks.isEmpty()) {
                        currentAssignment = tasks.get(0);
                        showTaskSnackbar(currentAssignment);
                        displayTaskOnMap(currentAssignment);
                    } else {
                        Toast.makeText(getContext(), "Không có nhiệm vụ đang xử lý hôm nay.", Toast.LENGTH_SHORT).show();
                        clearMap();
                    }
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<DeliveryAssignment>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                clearMap();
            }
        });
    }

    private void clearMap() {
        if (mapView != null) {
            mapView.getOverlays().clear();
            mapView.invalidate();
        }
    }

    private void showTaskSnackbar(DeliveryAssignment assignment) {
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
            startActivity(intent); });
        try {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
            customView.setLayoutParams(params); ((ViewGroup) snackbarView).addView(customView, 0); }
        catch (ClassCastException e) {
            Log.e(TAG, "Lỗi thêm customView vào Snackbar: " + e.getMessage());
            return;
        }
        snackbarView.setPadding(0, 0, 0, 0); snackbar.show(); }

    private void displayTaskOnMap(DeliveryAssignment assignment) {
        if (mapView == null || assignment == null) return;

        // Điểm 1: vị trí tài xế (tạm thời hardcode, sẽ đổi sang GPS)
        GeoPoint startPoint = new GeoPoint(10.775843, 106.697412);

        // Điểm 2: vị trí giao hàng
        double lat = 0.0;
        double lon = 0.0;

//        try {
//            if (assignment.getLatitude() != null) lat = assignment.getLatitude();
//            if (assignment.getLongitude() != null) lon = assignment.getLongitude();
//        } catch (Exception e) {
//            Log.w(TAG, "Không thể lấy tọa độ task: " + e.getMessage());
//        }

        if (lat == 0.0 || lon == 0.0) {
            Log.w(TAG, "Task " + assignment.getParcelCode() + " không có tọa độ. Dùng tọa độ ngẫu nhiên.");
            lat = 10.8231 + Math.random() * 0.05;
            lon = 10.6297 + Math.random() * 0.05;
        }
        GeoPoint endPoint = new GeoPoint(lat, lon);

        mapView.getOverlays().clear();

        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setTitle("Tài xế (ID: " + driverId + ")");
        startMarker.setSnippet("Vị trí hiện tại");
        mapView.getOverlays().add(startMarker);

        Marker endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setTitle(assignment.getReceiverName());
        endMarker.setSnippet("Mã đơn: " + assignment.getParcelCode());
        mapView.getOverlays().add(endMarker);

        drawRouteBetweenPoints(startPoint, endPoint);
        mapView.invalidate();
    }

    private void drawRouteBetweenPoints(GeoPoint startPoint, GeoPoint endPoint) {
        if (mapView == null) return;
        new GetRouteTask(startPoint, endPoint).execute();
        mapView.getController().animateTo(startPoint);
        mapView.invalidate();
    }

    private class GetRouteTask extends AsyncTask<Void, Void, ArrayList<GeoPoint>> {
        private final GeoPoint start;
        private final GeoPoint end;
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
                    while ((line = reader.readLine()) != null) response.append(line);
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
            if (json.has("routes")) {
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject geometry = route.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray coord = coordinates.getJSONArray(i);
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
            TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(tasks);
            dialog.show(getParentFragmentManager(), "TaskListDialog");
        });
    }

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "MapFragment onActivityResult: truyền sự kiện xuống children...");
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void displayTaskAndRoute(DeliveryAssignment assignment) {
        if (!"IN_PROGRESS".equals(assignment.getStatus())) {
            Toast.makeText(getContext(), "Nhiệm vụ đã hoàn tất hoặc thất bại.", Toast.LENGTH_SHORT).show();
            return;
        }
        this.currentAssignment = assignment;
        displayTaskOnMap(assignment);
    }
}
