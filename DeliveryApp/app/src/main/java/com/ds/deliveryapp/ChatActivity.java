package com.ds.deliveryapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.MessageAdapter;
import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.AuthClient;
import com.ds.deliveryapp.clients.ChatClient;
import com.ds.deliveryapp.clients.req.ChatMessagePayload;
import com.ds.deliveryapp.clients.req.CreateProposalDTO;
import com.ds.deliveryapp.clients.req.ProposalResponseRequest;
import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.clients.res.KeycloakUserInfoDto;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.ProposalTypeConfig;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.utils.ChatWebSocketListener;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity này quản lý UI, gọi API (REST),
 * và lắng nghe sự kiện từ ChatWebSocketManager.
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnProposalActionListener, ChatWebSocketListener {

    private static final String TAG = "ChatActivity";
    private static final String SERVER_WEBSOCKET_URL = "ws://192.168.1.6:21511/ws";

    // Views
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private ImageButton btnBack;
    private ImageView ivAvatar;
    private TextView tvRecipientName;
    private TextView tvRecipientStatus;

    // Adapter & Data
    private MessageAdapter mAdapter;
    private String mParcelCode;
    private String mParcelId;
    private final List<Message> mMessages = new ArrayList<>();
    private Calendar mSelectedStartTime;

    // Networking & Auth
    private ChatWebSocketManager mWebSocketManager;
    private AuthClient mAuthClient;
    private ChatClient mChatClient;
    private final Gson mGson = new Gson();
    private AuthManager mAuthManager;

    // State Data
    private String mJwtToken;
    private String mCurrentUserId;
    private List<String> mCurrentRoles = new ArrayList<>();
    private String mRecipientId;
    private String mRecipientName;
    private String mRecipientAvatarUrl;
    private String mConversationId;
    private List<ProposalTypeConfig> mAvailableProposals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Giả định layout này có btn_attach

        mAuthManager = new AuthManager(this);

        initViews();
        getInitialDataAndToken();

        if (!validateInitialIntentData()) { return; }

        initRetrofitClients();
        initRecyclerView();
        getUserInfoAndProceed();
        setupSendButton();
        // (Không cần setupProposalButton nữa)
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        // --- ÁNH XẠ CÁC VIEW MỚI ---
        btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvRecipientName = findViewById(R.id.tv_recipient_name);
        tvRecipientStatus = findViewById(R.id.tv_recipient_status);
        btnAttach = findViewById(R.id.btn_attach); // <-- ÁNH XẠ NÚT +

        btnBack.setOnClickListener(v -> finish());

        // Gán listener cho nút +
        btnAttach.setOnClickListener(v -> {
            showProposalMenu();
        });
    }

    private void getInitialDataAndToken() {
        mJwtToken = mAuthManager.getAccessToken();

        // --- HARDCODED RECIPIENT FOR TESTING ---
        String customerId = "72d01198-4a4e-4743-8cb8-038a9de9ea98";
        String shipperId = "62b08293-e714-45e1-9bec-a4a7e9e1bc71";

        Log.d(TAG, "Reading data from Intent...");
        Intent intent = getIntent();
        //mRecipientId = shipperId;

        mRecipientId = intent.getStringExtra("RECIPIENT_ID");
        mRecipientName = intent.getStringExtra("RECIPIENT_NAME");
        mParcelId = intent.getStringExtra("PARCEL_ID");

        // (Lấy dữ liệu mới cho thanh tiêu đề)
        mParcelCode = intent.getStringExtra("PARCEL_CODE");

        if (mRecipientId == null || mRecipientId.isEmpty()) {
            Log.e(TAG, "CRITICAL: RECIPIENT_ID is missing from Intent.");
            handleFatalError("Missing Recipient ID.");
        }

        Log.d(TAG, "Initial Data - Recipient ID: " + mRecipientId);
        Log.d(TAG, "Initial Data - Parcel Code: " + mParcelCode);
    }

    private boolean validateInitialIntentData() {
        if (mJwtToken == null || mJwtToken.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: Token missing.");
            showErrorToastAndFinish("Authentication token not found. Please login.");
            return false;
        }
        return true;
    }

    private void initRecyclerView() {
        mAdapter = new MessageAdapter(mMessages, mCurrentUserId);
        mAdapter.setListener(this); // Gán listener là Activity này
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(mAdapter);
    }

    private void initRetrofitClients() {
        mChatClient = RetrofitClient.getChatRetrofitInstance().create(ChatClient.class);
        mAuthClient = RetrofitClient.getAuthRetrofitInstance().create(AuthClient.class);
    }

    /**
     * Lấy thông tin user (gọi API /auth/me).
     */
    private void getUserInfoAndProceed() {
        if (mJwtToken == null) return;

        Log.d(TAG, "Fetching user info...");
        String authorizationHeader = "Bearer " + mJwtToken;
        Call<BaseResponse<KeycloakUserInfoDto>> call = mAuthClient.getUserInfo(authorizationHeader);

        call.enqueue(new Callback<BaseResponse<KeycloakUserInfoDto>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<KeycloakUserInfoDto>> call,
                                   @NonNull Response<BaseResponse<KeycloakUserInfoDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    KeycloakUserInfoDto user = response.body().getResult();
                    mCurrentUserId = user.getSub();
                    if (user.getRoles() != null) {
                        mCurrentRoles = user.getRoles();
                    }
                    if (mCurrentUserId == null || mCurrentUserId.isEmpty()) {
                        handleFatalError("Failed to get User ID from token response.");
                        return;
                    }
                    Log.i(TAG, "✅ User info fetched. Current User ID: " + mCurrentUserId);
                    if (mAdapter != null) {
                        mAdapter.setCurrentUserId(mCurrentUserId);
                    }

                    // Bắt đầu chuỗi tải dữ liệu
                    fetchConversationIdAndConnect();
                    loadAvailableProposals();

                } else {
                    handleFatalError("Failed to fetch user info (API Error: " + response.code() + ")");
                }
            }
            @Override
            public void onFailure(@NonNull Call<BaseResponse<KeycloakUserInfoDto>> call, @NonNull Throwable t) {
                handleFatalError("Network error fetching user info: " + t.getMessage());
            }
        });
    }

    /**
     * Lấy ID cuộc trò chuyện (gọi API /conversations/find-by-users).
     */
    private void fetchConversationIdAndConnect() {
        if (mCurrentUserId == null || mRecipientId == null) {
            handleFatalError("Cannot proceed without user information.");
            return;
        }
        Log.d(TAG, "Fetching conversation ID between " + mCurrentUserId + " and " + mRecipientId);

        Call<Conversation> call =
                mChatClient.getConversationBy2Users(mCurrentUserId, mRecipientId);

        call.enqueue(new Callback<Conversation>() {
            @Override
            public void onResponse(@NonNull Call<Conversation> call, @NonNull Response<Conversation> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getConversationId() != null) {
                    Conversation conversation = response.body();
                    mConversationId = conversation.getConversationId();
                    Log.i(TAG, "✅ Conversation ID fetched/found: " + mConversationId);

                    //
                    mRecipientName = conversation.getPartnerName();
                    mRecipientAvatarUrl = conversation.getPartnerAvatar();
                    //
                    if (mAdapter != null) {
                        mAdapter.setRecipientInfo(mRecipientAvatarUrl);
                    }

                    // --- CẬP NHẬT APP BAR TÙY CHỈNH ---
                    runOnUiThread(() -> {
                        if (tvRecipientName != null) {
                            tvRecipientName.setText(mRecipientName);
                        }

                        if (tvRecipientStatus != null) {
                            if (mParcelCode != null && !mParcelCode.isEmpty()) {
                                tvRecipientStatus.setText("Đơn hàng: " + mParcelCode);
                            } else {
                                tvRecipientStatus.setText("Đang hoạt động");
                            }
                        }
                        // (Thêm code Glide/Picasso để tải ivAvatar tại đây)
                    });

                    loadChatHistory();
                    connectWebSocket(); // Bắt đầu kết nối WebSocket

                } else {
                    Log.e(TAG, "Failed to fetch conversation ID (API Error: " + response.code() + ")");
                    showErrorToast("Could not find or create conversation.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<Conversation> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error fetching conversation ID", t);
            }
        });
    }

    /**
     * Tải lịch sử chat (gọi API /conversations/{id}/messages).
     */
    private void loadChatHistory() {
        if (mConversationId == null) return;
        Call<PageResponse<Message>> call =
                mChatClient.getChatHistory(mConversationId, mCurrentUserId, 0, 50);
        call.enqueue(new Callback<PageResponse<Message>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<Message>> call, @NonNull Response<PageResponse<Message>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().content() != null) {
                    List<Message> history = response.body().content();
                    Collections.reverse(history);
                    runOnUiThread(() -> {
                        if (mAdapter != null) mAdapter.setMessages(history);
                        scrollToBottom();
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Call<PageResponse<Message>> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error loading history", t);
            }
        });
    }

    /**
     * HÀM MỚI: Tải các loại proposal mà user này có thể TẠO.
     */
    private void loadAvailableProposals() {
        Log.i(TAG, "in load config {}" + mCurrentRoles.get(0));
        if (mJwtToken == null) return;
        String authorizationHeader = "Bearer " + mJwtToken;

        Call<List<ProposalTypeConfig>> call = mChatClient.getAvailableConfigs(mCurrentRoles);
        call.enqueue(new Callback<List<ProposalTypeConfig>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProposalTypeConfig>> call, @NonNull Response<List<ProposalTypeConfig>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Đã tải " + response.body().size() + " proposal khả dụng.");
                    mAvailableProposals = response.body();
                } else {
                    Log.e(TAG, "Lỗi tải proposal configs: " + response.code());
                    // Không crash, user chỉ không thể tạo proposal
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ProposalTypeConfig>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi mạng khi tải proposal configs", t);
            }
        });
    }

    /**
     * ĐÃ CẬP NHẬT: Kết nối bằng WebSocket Manager.
     */
    private void connectWebSocket() {
        if (mWebSocketManager != null && mWebSocketManager.isConnected()) {
            Log.w(TAG, "WebSocket connection attempt ignored: Already connected.");
            return;
        }

        Log.d(TAG, "Initializing WebSocket Manager...");
        mWebSocketManager = new ChatWebSocketManager(SERVER_WEBSOCKET_URL, mCurrentUserId);
        mWebSocketManager.setListener(this); // <-- Gán Activity này làm listener
        mWebSocketManager.connect(); // Bắt đầu kết nối
    }

    /**
     * Logic gửi tin nhắn TEXT (Chat cũ).
     */
    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                if (mWebSocketManager != null && mWebSocketManager.isConnected()) {
                    sendMessage(content);
                } else {
                    showErrorToast("Not connected to chat. Please wait or try again.");
                }
            }
        });
    }

    /**
     * ĐÃ CẬP NHẬT: Gửi tin nhắn bằng WebSocket Manager.
     */
    private void sendMessage(String content) {
        if (mCurrentUserId == null || mRecipientId == null) {
            showErrorToast("Cannot send: User info missing.");
            return;
        }

        ChatMessagePayload payload = new ChatMessagePayload(content, mRecipientId);

        mWebSocketManager.sendMessage(payload, new ChatWebSocketManager.SendMessageCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "STOMP message sent successfully.");
                Message selfMessage = new Message(
                        null, mCurrentUserId, content, null, ContentType.TEXT, null
                );
                runOnUiThread(() -> {
                    etMessage.setText("");
                    if (mAdapter != null) {
                        mAdapter.addMessage(selfMessage);
                        scrollToBottom();
                    }
                });
            }
            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "Error sending STOMP message", throwable);
                runOnUiThread(() -> showErrorToast("Failed to send message."));
            }
        });
    }

    /**
     * HÀM MỚI: Hiển thị menu khi bấm nút +
     */
    private void showProposalMenu() {
        if (mAvailableProposals == null || mAvailableProposals.isEmpty()) {
            showErrorToast("Không có hành động nào.");
            return;
        }

        CharSequence[] items = new CharSequence[mAvailableProposals.size()];
        for(int i = 0; i < mAvailableProposals.size(); i++) {
            items[i] = mAvailableProposals.get(i).getDescription();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn hành động")
                .setItems(items, (dialog, which) -> {
                    // Lấy config được chọn
                    ProposalTypeConfig selectedConfig = mAvailableProposals.get(which);

                    // Phân luồng dựa trên actionType của config
                    String actionType = selectedConfig.getCreationActionType();
                    if (actionType == null) return;
                    if ("DATE_PICKER".equals(actionType)) {
                        actionType = "POSTPONE_OPTIONS";
                    }


                    switch (actionType) {
                        case "POSTPONE_OPTIONS": // (Đã đổi tên)
                            // (Kịch bản: Khách yêu cầu hoãn đơn)
                            showPostponeOptionsDialog(selectedConfig);
                            break;
                        case "TEXT_INPUT":
                            // (Kịch bản: Shipper báo lý do)
                            // Thu thập data TRƯỚC
                            showTextInputDialog(selectedConfig);
                            break;
                        case "ACCEPT_DECLINE":
                        default:
                            // (Kịch bản: Không cần data, ví dụ: "Shipper đã đến")
                            // Gửi luôn
                            sendProposalRequest(selectedConfig.getType(), "{}", selectedConfig.getDescription() + " với mã đơn hàng: " + mParcelCode);
                            break;
                    }
                })
                .show();
    }

    /**
     * HÀM MỚI (Layer 1): Hiển thị 3 lựa chọn hoãn đơn
     */
    private void showPostponeOptionsDialog(ProposalTypeConfig config) {
        CharSequence[] postponeOptions = {
                "Vào 1 thời điểm cụ thể",
                "Trước 1 thời điểm",
                "Sau 1 thời điểm",
                "Trong 1 khoảng thời gian"
        };

        new AlertDialog.Builder(this)
                .setTitle("Chọn kiểu hoãn đơn")
                .setItems(postponeOptions, (dialog, which) -> {
                    switch (which) {
                        case 0: // "Vào 1 thời điểm cụ thể"
                            // Dùng hàm chọn 1 thời điểm
                            showSingleDateTimePickerDialog(config, "SPECIFIC");
                            break;
                        case 1: // "Trước 1 thời điểm"
                            // Cũng dùng hàm chọn 1 thời điểm, nhưng logic data khác
                            showSingleDateTimePickerDialog(config, "BEFORE");
                            break;
                        case 2: // "Sau 1 thời điểm"
                            // Cũng dùng hàm chọn 1 thời điểm, nhưng logic data khác
                            showSingleDateTimePickerDialog(config, "AFTER");
                            break;
                        case 3: // "Trong 1 khoảng thời gian"
                            // Dùng hàm chọn 2 thời điểm
                            showDateTimeRangePickerDialog(config);
                            break;
                    }
                })
                .show();
    }

    /**
     * HÀM ĐÃ ĐỔI TÊN (Layer 2 - Option 1 & 2):
     * Xử lý chọn 1 mốc Ngày & Giờ
     */
    private void showSingleDateTimePickerDialog(ProposalTypeConfig config, String postponeType) {
        Calendar cal = Calendar.getInstance();
        mSelectedStartTime = null; // Reset

        // 1. Tạo DatePickerDialog
        DatePickerDialog dpd = new DatePickerDialog(this,
                // DateSetListener
                (datePicker, year, month, day) -> {

                    // --- Ngày đã được chọn, lưu lại và hiển thị TimePicker ---
                    mSelectedStartTime = Calendar.getInstance();
                    mSelectedStartTime.set(year, month, day);

                    // 2. Tạo TimePickerDialog
                    TimePickerDialog tpd = new TimePickerDialog(this,
                            // TimeSetListener
                            (timePicker, hour, minute) -> {
                                mSelectedStartTime.set(Calendar.HOUR_OF_DAY, hour);
                                mSelectedStartTime.set(Calendar.MINUTE, minute);

                                // --- Giờ đã được chọn ---
                                String readableDateTime = String.format("%02d:%02d ngày %02d/%02d/%d",
                                        hour, minute, day, month + 1, year);

                                // 3. Định dạng kết quả (ISO 8601 là tốt nhất)
                                // ví dụ: "2025-10-30T14:30:00"
                                String resultData = String.format("%d-%02d-%02dT%02d:%02d:00",
                                        year, month + 1, day, hour, minute);

                                // 4. Tạo JSON data
                                String dataJson = "{}";
                                String fallbackContent = "";

                                if ("SPECIFIC".equals(postponeType)) {
                                    dataJson = "{\"specific_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " vào " + readableDateTime;
                                } else if ("AFTER".equals(postponeType)) { // "AFTER"
                                    dataJson = "{\"after_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " sau " + readableDateTime;
                                } else {
                                    dataJson = "{\"after_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " trước " + readableDateTime;
                                }

                                // 5. Gửi proposal
                                sendProposalRequest(config.getType(), dataJson, fallbackContent);
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true // 24-hour format
                    );
                    tpd.setTitle("Chọn Giờ");
                    tpd.show(); // Hiển thị TimePicker
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dpd.setTitle("Chọn Ngày");
        dpd.show(); // Hiển thị DatePicker TRƯỚC
    }

    /**
     * HÀM MỚI (Layer 2 - Option 3):
     * Xử lý chọn 2 mốc Ngày & Giờ (Bắt đầu và Kết thúc)
     */
    private void showDateTimeRangePickerDialog(ProposalTypeConfig config) {
        mSelectedStartTime = null; // Reset

        // --- BƯỚC 1: CHỌN NGÀY BẮT ĐẦU ---
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dpdStart = new DatePickerDialog(this, (dpdView, year, month, day) -> {
            mSelectedStartTime = Calendar.getInstance();
            mSelectedStartTime.set(year, month, day);

            // --- BƯỚC 2: CHỌN GIỜ BẮT ĐẦU ---
            TimePickerDialog tpdStart = new TimePickerDialog(this, (tpdView, hour, minute) -> {
                mSelectedStartTime.set(Calendar.HOUR_OF_DAY, hour);
                mSelectedStartTime.set(Calendar.MINUTE, minute);

                // --- BƯỚC 3: CHỌN NGÀY KẾT THÚC ---
                DatePickerDialog dpdEnd = new DatePickerDialog(this, (dpdView2, year2, month2, day2) -> {
                    Calendar selectedEndTime = Calendar.getInstance();
                    selectedEndTime.set(year2, month2, day2);

                    // --- BƯỚC 4: CHỌN GIỜ KẾT THÚC ---
                    TimePickerDialog tpdEnd = new TimePickerDialog(this, (tpdView2, hour2, minute2) -> {
                        selectedEndTime.set(Calendar.HOUR_OF_DAY, hour2);
                        selectedEndTime.set(Calendar.MINUTE, minute2);

                        // (Kiểm tra logic EndTime > StartTime...)
                        if (selectedEndTime.before(mSelectedStartTime)) {
                            showErrorToast("Giờ kết thúc phải sau giờ bắt đầu.");
                            return;
                        }

                        // --- BƯỚC 5: GỬI PROPOSAL ---
                        String startTimeStr = String.format("%d-%02d-%02dT%02d:%02d:00",
                                year, month + 1, day, hour, minute);
                        String endTimeStr = String.format("%d-%02d-%02dT%02d:%02d:00",
                                year2, month2 + 1, day2, hour2, minute2);

                        String dataJson = "{\"start_datetime\":\"" + startTimeStr + "\", \"end_datetime\":\"" + endTimeStr + "\"}";
                        String fallback = String.format("%s (Từ %02d:%02d %02d/%d đến %02d:%02d %02d/%d)",
                                config.getDescription(),
                                hour, minute, day, month+1,
                                hour2, minute2, day2, month2+1);

                        sendProposalRequest(config.getType(), dataJson, fallback);

                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
                    tpdEnd.setTitle("Chọn Giờ Kết Thúc");
                    tpdEnd.show();

                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                dpdEnd.setTitle("Chọn Ngày Kết Thúc");
                dpdEnd.show();

            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            tpdStart.setTitle("Chọn Giờ Bắt Đầu");
            tpdStart.show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpdStart.setTitle("Chọn Ngày Bắt Đầu");
        dpdStart.show();
    }
    /**
     * HÀM MỚI: Hiển thị dialog nhập text (cho TEXT_INPUT)
     */
    private void showTextInputDialog(ProposalTypeConfig config) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(config.getDescription());
        builder.setMessage("Vui lòng nhập lý do:");

        // Tạo một EditText
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Cần một layout để bọc EditText cho có padding
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20); // (Left, Top, Right, Bottom)
        input.setLayoutParams(params);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            // 1. User đã nhập text
            String resultData = input.getText().toString().trim();
            if (resultData.isEmpty()) {
                showErrorToast("Cần nhập lý do.");
                return;
            }
            // 2. Tạo JSON data
            String dataJson = "{\"reason\":\"" + resultData + "\"}";
            // 3. Gửi proposal
            sendProposalRequest(config.getType(), dataJson, config.getDescription());
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Gọi API (REST) để tạo Proposal.
     */
    private void sendProposalRequest(String type, String data, String fallbackContent) {
        if (mConversationId == null || mRecipientId == null || mJwtToken == null) {
            showErrorToast("Không thể gửi yêu cầu: Thiếu thông tin.");
            return;
        }

        if ("CONFIRM_REFUSAL".equals(type) && mParcelId != null) {
            data = "{\"parcelId\":\"" + mParcelId + "\"}";
        }

        CreateProposalDTO payload = new CreateProposalDTO(
                mConversationId, mRecipientId, type, data, fallbackContent,
                mCurrentUserId, mCurrentRoles
        );

        String authorizationHeader = "Bearer " + mJwtToken;
        Call<InteractiveProposal> call = mChatClient.createProposal(payload);

        call.enqueue(new Callback<InteractiveProposal>() {
            @Override
            public void onResponse(@NonNull Call<InteractiveProposal> call, @NonNull Response<InteractiveProposal> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Gửi proposal thành công. Chờ WebSocket echo...");
                    // Server sẽ tự động gửi Message (loại PROPOSAL)
                    // qua kênh /user/queue/messages
                    loadChatHistory();
                } else {
                    Log.e(TAG, "Gửi proposal thất bại: " + response.code());
                    showErrorToast("Gửi yêu cầu thất bại.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<InteractiveProposal> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi mạng khi gửi proposal", t);
            }
        });
    }

    /* --- IMPLEMENTS TỪ ADAPTER LISTENER (Khi bấm nút) --- */

    /**
     * Được gọi từ Adapter khi bấm bất kỳ nút phản hồi nào.
     */
    @Override
    public void onProposalRespond(UUID proposalId, String resultData) {
        Log.d(TAG, "Handling RESPOND for proposal: " + proposalId + " with data: " + resultData);
        String authorizationHeader = "Bearer " + mJwtToken;

        // 1. Tạo DTO payload mới
        ProposalResponseRequest payload = new ProposalResponseRequest(resultData);

        // 2. Gọi API /respond mới
        Call<InteractiveProposal> call = mChatClient.respondToProposal(
                proposalId,
                mCurrentUserId,
                payload // Gửi DTO trong body
        );

        call.enqueue(new Callback<InteractiveProposal>() {
            @Override
            public void onResponse(@NonNull Call<InteractiveProposal> call, @NonNull Response<InteractiveProposal> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Phản hồi proposal thành công. Chờ WebSocket update...");
                    // Server sẽ gửi update qua /user/queue/proposal-update
                    loadChatHistory();
                } else {
                    Log.e(TAG, "Phản hồi proposal thất bại: " + response.code());
                    showErrorToast("Thao tác thất bại.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<InteractiveProposal> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi mạng khi phản hồi proposal", t);
                showErrorToast("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    /* --- IMPLEMENTS TỪ WEBSOCKET LISTENER (Khi nhận sự kiện) --- */

    @Override
    public void onWebSocketOpened() {
        runOnUiThread(() -> Log.i(TAG, "ChatActivity: WebSocket Opened."));
    }

    @Override
    public void onWebSocketClosed() {
        runOnUiThread(() -> {
            Log.i(TAG, "ChatActivity: WebSocket Closed.");
            showErrorToast("Chat connection closed.");
        });
    }

    @Override
    public void onWebSocketError(String error) {
        runOnUiThread(() -> {
            Log.e(TAG, "ChatActivity: WebSocket Error: " + error);
            showErrorToast("Chat error. Please try again.");
        });
    }

    @Override
    public void onMessageReceived(Message message) {
        runOnUiThread(() -> {
            if (message != null && mAdapter != null && message.getSenderId() != null) {
                mAdapter.addMessage(message);
                scrollToBottom();
            }
        });
    }

    @Override
    public void onProposalUpdateReceived(ProposalUpdateDTO update) {
        runOnUiThread(() -> {
            if (update != null && mAdapter != null && update.getProposalId() != null) {
                Log.i(TAG, "Updating status for Proposal " + update.getProposalId() + " to " + update.getNewStatus());

                // (Bạn cần cập nhật DTO ProposalUpdateDTO ở server và client
                // để nó chứa cả 'resultData')
                String resultData = update.getResultData(); // Giả sử DTO đã có

                mAdapter.updateProposalStatus(
                        update.getProposalId(),
                        update.getNewStatus(),
                        resultData // <-- Truyền cả kết quả
                );
            }
        });
    }

    /* --- CÁC HÀM TIỆN ÍCH (Giữ nguyên) --- */

    private void scrollToBottom() {
        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            rvMessages.post(() -> rvMessages.smoothScrollToPosition(mAdapter.getItemCount() - 1));
        }
    }

    private void handleFatalError(String message) {
        Log.e(TAG, "Fatal Setup Error: " + message);
        showErrorToastAndFinish("Critical error: " + message);
    }

    private void showErrorToastAndFinish(String message){
        runOnUiThread(() -> {
            Toast.makeText(ChatActivity.this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void showErrorToast(String message) {
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Disconnecting WebSocket Manager.");
        if (mWebSocketManager != null) {
            mWebSocketManager.disconnect();
            mWebSocketManager = null;
        }
    }
}