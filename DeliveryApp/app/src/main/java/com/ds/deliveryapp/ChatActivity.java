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
import com.ds.deliveryapp.clients.ChatClient;
import com.ds.deliveryapp.clients.UserClient;
import com.ds.deliveryapp.clients.req.ChatMessagePayload;
import com.ds.deliveryapp.clients.req.CreateProposalDTO;
import com.ds.deliveryapp.clients.req.ProposalResponseRequest;
import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.ProposalTypeConfig;
import com.ds.deliveryapp.clients.res.UserInfo;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.utils.ChatWebSocketListener;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.google.gson.Gson;

import org.json.JSONObject;

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
    private ChatClient mChatClient;

    private UserClient mUserClient;
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
        setContentView(R.layout.activity_chat);

        mAuthManager = new AuthManager(this);

        initViews();

        // 1. Lấy tất cả dữ liệu (Token, UserID, Roles) từ AuthManager
        getInitialDataAndToken();

        if (!validateInitialIntentData()) { return; }

        initRetrofitClients();
        initRecyclerView();

        // 2. Cập nhật Adapter với UserID
        if (mAdapter != null) {
            mAdapter.setCurrentUserId(mCurrentUserId);
        }

        // 3. Bắt đầu chuỗi tải dữ liệu ngay lập tức
        // (Không cần gọi getUserInfoAndProceed() nữa)
        fetchConversationIdAndConnect();
        loadAvailableProposals();

        setupSendButton();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvRecipientName = findViewById(R.id.tv_recipient_name);
        tvRecipientStatus = findViewById(R.id.tv_recipient_status);
        btnAttach = findViewById(R.id.btn_attach);

        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> {
            showProposalMenu();
        });
    }

    /**
     * Lấy Token, UserID, và Roles trực tiếp từ AuthManager (SharedPreferences).
     */
    private void getInitialDataAndToken() {
        // 1. Lấy dữ liệu từ AuthManager
        mJwtToken = mAuthManager.getAccessToken();

        // (Giả sử AuthManager có 2 phương thức này, đọc từ SharedPreferences
        // mà LoginActivity đã lưu)
        mCurrentUserId = mAuthManager.getUserId();
        mCurrentRoles = mAuthManager.getRoles();

        Log.d(TAG, "Auth data loaded from Prefs. UserID: " + mCurrentUserId);

        // 2. Lấy dữ liệu từ Intent (như cũ)
        Log.d(TAG, "Reading data from Intent...");
        Intent intent = getIntent();
        mRecipientId = intent.getStringExtra("RECIPIENT_ID");
        mRecipientName = intent.getStringExtra("RECIPIENT_NAME");
        mParcelId = intent.getStringExtra("PARCEL_ID");
        mParcelCode = intent.getStringExtra("PARCEL_CODE");

        if (mRecipientId == null || mRecipientId.isEmpty()) {
            Log.e(TAG, "CRITICAL: RECIPIENT_ID is missing from Intent.");
            handleFatalError("Missing Recipient ID.");
        }

        Log.d(TAG, "Initial Data - Recipient ID: " + mRecipientId);
        Log.d(TAG, "Initial Data - Parcel Code: " + mParcelCode);
    }

    /**
     * Kiểm tra cả UserID và Roles đã được tải.
     */
    private boolean validateInitialIntentData() {
        if (mJwtToken == null || mJwtToken.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: Token missing.");
            showErrorToastAndFinish("Authentication token not found. Please login.");
            return false;
        }
        if (mCurrentUserId == null || mCurrentUserId.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: UserID missing.");
            showErrorToastAndFinish("User ID not found. Please login.");
            return false;
        }
        if (mCurrentRoles == null || mCurrentRoles.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: Roles missing.");
            showErrorToastAndFinish("User Roles not found. Please login.");
            return false;
        }
        return true;
    }

    private void initRecyclerView() {
        mAdapter = new MessageAdapter(mMessages, mCurrentUserId);
        mAdapter.setListener(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(mAdapter);
    }

    private void initRetrofitClients() {
        mChatClient = RetrofitClient.getChatRetrofitInstance().create(ChatClient.class);
        mUserClient = RetrofitClient.getRetrofitInstance(getApplicationContext()).create(UserClient.class);
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

                    getPartnerInfo(conversation.getPartnerId());
                    mRecipientAvatarUrl = conversation.getPartnerAvatar();

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

    private void getPartnerInfo(String partnerId) {
        Call<UserInfo> call = mUserClient.getUserInfoById(partnerId);
        call.enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(@NonNull Call<UserInfo> call, @NonNull Response<UserInfo> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    UserInfo.UserBasicInfo userInfo = response.body().getResult();
                    mRecipientName = userInfo.getFirstName() + " " + userInfo.getLastName();
                }
            }
            @Override
            public void onFailure(@NonNull Call<UserInfo> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error loading user info", t);
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
     * Tải các loại proposal mà user này có thể TẠO.
     */
    private void loadAvailableProposals() {
        if (mJwtToken == null) return;

        Call<List<ProposalTypeConfig>> call = mChatClient.getAvailableConfigs(mCurrentRoles);
        call.enqueue(new Callback<List<ProposalTypeConfig>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProposalTypeConfig>> call, @NonNull Response<List<ProposalTypeConfig>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Đã tải " + response.body().size() + " proposal khả dụng.");
                    mAvailableProposals = response.body();
                } else {
                    Log.e(TAG, "Lỗi tải proposal configs: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ProposalTypeConfig>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi mạng khi tải proposal configs", t);
            }
        });
    }

    /**
     * Kết nối bằng WebSocket Manager.
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
     * Gửi tin nhắn bằng WebSocket Manager.
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
     * Hiển thị menu khi bấm nút +
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
                    ProposalTypeConfig selectedConfig = mAvailableProposals.get(which);
                    String actionType = selectedConfig.getCreationActionType();
                    if (actionType == null) return;
                    if ("DATE_PICKER".equals(actionType)) {
                        actionType = "POSTPONE_OPTIONS";
                    }

                    switch (actionType) {
                        case "POSTPONE_OPTIONS":
                            showPostponeOptionsDialog(selectedConfig);
                            break;
                        case "TEXT_INPUT":
                            showTextInputDialog(selectedConfig);
                            break;
                        case "ACCEPT_DECLINE":
                        default:
                            sendProposalRequest(selectedConfig.getType(), "{}", selectedConfig.getDescription() + " với mã đơn hàng: " + mParcelCode);
                            break;
                    }
                })
                .show();
    }

    /**
     * (Layer 1): Hiển thị 3 lựa chọn hoãn đơn
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
                        case 0:
                            showSingleDateTimePickerDialog(config, "SPECIFIC");
                            break;
                        case 1:
                            showSingleDateTimePickerDialog(config, "BEFORE");
                            break;
                        case 2:
                            showSingleDateTimePickerDialog(config, "AFTER");
                            break;
                        case 3:
                            showDateTimeRangePickerDialog(config);
                            break;
                    }
                })
                .show();
    }

    /**
     * (Layer 2 - Option 1 & 2):
     * Xử lý chọn 1 mốc Ngày & Giờ
     */
    private void showSingleDateTimePickerDialog(ProposalTypeConfig config, String postponeType) {
        Calendar cal = Calendar.getInstance();
        mSelectedStartTime = null; // Reset

        DatePickerDialog dpd = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    mSelectedStartTime = Calendar.getInstance();
                    mSelectedStartTime.set(year, month, day);

                    TimePickerDialog tpd = new TimePickerDialog(this,
                            (timePicker, hour, minute) -> {
                                mSelectedStartTime.set(Calendar.HOUR_OF_DAY, hour);
                                mSelectedStartTime.set(Calendar.MINUTE, minute);

                                String readableDateTime = String.format("%02d:%02d ngày %02d/%02d/%d",
                                        hour, minute, day, month + 1, year);

                                String resultData = String.format("%d-%02d-%02dT%02d:%02d:00",
                                        year, month + 1, day, hour, minute);

                                String dataJson = "{}";
                                String fallbackContent = "";

                                if ("SPECIFIC".equals(postponeType)) {
                                    dataJson = "{\"specific_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " vào " + readableDateTime;
                                } else if ("AFTER".equals(postponeType)) {
                                    dataJson = "{\"after_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " sau " + readableDateTime;
                                } else {
                                    dataJson = "{\"after_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " trước " + readableDateTime;
                                }

                                sendProposalRequest(config.getType(), dataJson, fallbackContent);
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true // 24-hour format
                    );
                    tpd.setTitle("Chọn Giờ");
                    tpd.show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dpd.setTitle("Chọn Ngày");
        dpd.show();
    }

    /**
     * (Layer 2 - Option 3):
     * Xử lý chọn 2 mốc Ngày & Giờ (Bắt đầu và Kết thúc)
     */
    private void showDateTimeRangePickerDialog(ProposalTypeConfig config) {
        mSelectedStartTime = null; // Reset

        Calendar cal = Calendar.getInstance();
        DatePickerDialog dpdStart = new DatePickerDialog(this, (dpdView, year, month, day) -> {
            mSelectedStartTime = Calendar.getInstance();
            mSelectedStartTime.set(year, month, day);

            TimePickerDialog tpdStart = new TimePickerDialog(this, (tpdView, hour, minute) -> {
                mSelectedStartTime.set(Calendar.HOUR_OF_DAY, hour);
                mSelectedStartTime.set(Calendar.MINUTE, minute);

                DatePickerDialog dpdEnd = new DatePickerDialog(this, (dpdView2, year2, month2, day2) -> {
                    Calendar selectedEndTime = Calendar.getInstance();
                    selectedEndTime.set(year2, month2, day2);

                    TimePickerDialog tpdEnd = new TimePickerDialog(this, (tpdView2, hour2, minute2) -> {
                        selectedEndTime.set(Calendar.HOUR_OF_DAY, hour2);
                        selectedEndTime.set(Calendar.MINUTE, minute2);

                        if (selectedEndTime.before(mSelectedStartTime)) {
                            showErrorToast("Giờ kết thúc phải sau giờ bắt đầu.");
                            return;
                        }

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
     * Hiển thị dialog nhập text (cho TEXT_INPUT)
     */
    private void showTextInputDialog(ProposalTypeConfig config) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(config.getDescription());
        builder.setMessage("Vui lòng nhập lý do:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String resultData = input.getText().toString().trim();
            if (resultData.isEmpty()) {
                showErrorToast("Cần nhập lý do.");
                return;
            }
            String dataJson = "{\"reason\":\"" + resultData + "\"}";
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

        if ("POSTPONE_REQUEST".equals(type) && mParcelId != null) {
            try {
                JSONObject json = new JSONObject(data); // parse data hiện tại
                json.put("parcelId", mParcelId);        // thêm parcelId
                data = json.toString();                 // convert lại thành chuỗi JSON
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi thêm parcelId vào data", e);
                data = "{\"parcelId\":\"" + mParcelId + "\"}"; // fallback nếu lỗi
            }
        }


        CreateProposalDTO payload = new CreateProposalDTO(
                mConversationId, mRecipientId, type, data, fallbackContent,
                mCurrentUserId, mCurrentRoles
        );

        Call<InteractiveProposal> call = mChatClient.createProposal(payload);

        call.enqueue(new Callback<InteractiveProposal>() {
            @Override
            public void onResponse(@NonNull Call<InteractiveProposal> call, @NonNull Response<InteractiveProposal> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Gửi proposal thành công. Chờ WebSocket echo...");
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

        ProposalResponseRequest payload = new ProposalResponseRequest(resultData);

        Call<InteractiveProposal> call = mChatClient.respondToProposal(
                proposalId,
                mCurrentUserId,
                payload
        );

        call.enqueue(new Callback<InteractiveProposal>() {
            @Override
            public void onResponse(@NonNull Call<InteractiveProposal> call, @NonNull Response<InteractiveProposal> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Phản hồi proposal thành công. Chờ WebSocket update...");
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
                String resultData = update.getResultData();
                mAdapter.updateProposalStatus(
                        update.getProposalId(),
                        update.getNewStatus(),
                        resultData
                );
            }
        });
    }

    /* --- CÁC HÀM TIỆN ÍCH --- */

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
