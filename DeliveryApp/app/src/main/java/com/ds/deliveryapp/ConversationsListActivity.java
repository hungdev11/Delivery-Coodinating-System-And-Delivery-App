package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ds.deliveryapp.adapter.ConversationAdapter;
import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.ChatClient;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.configs.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity hiển thị danh sách conversations của user
 */
public class ConversationsListActivity extends AppCompatActivity {

    private static final String TAG = "ConversationsListActivity";

    private RecyclerView rvConversations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton btnBack;
    private ConversationAdapter mAdapter;
    private List<Conversation> mConversations = new ArrayList<>();

    private ChatClient mChatClient;
    private AuthManager mAuthManager;
    private String mCurrentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_list);

        mAuthManager = new AuthManager(this);
        mCurrentUserId = mAuthManager.getUserId();

        if (mCurrentUserId == null || mCurrentUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found. Please login.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        initRetrofitClients();
        initRecyclerView();
        loadConversations();
    }

    private void initViews() {
        rvConversations = findViewById(R.id.rv_conversations);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadConversations();
        });
    }

    private void initRetrofitClients() {
        mChatClient = RetrofitClient.getChatRetrofitInstance().create(ChatClient.class);
    }

    private void initRecyclerView() {
        mAdapter = new ConversationAdapter(mConversations, conversation -> {
            // Click vào conversation -> mở ChatActivity
            openChatActivity(conversation);
        });

        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(mAdapter);
    }

    private void loadConversations() {
        Log.d(TAG, "📥 Loading conversations for user: " + mCurrentUserId);

        Call<List<Conversation>> call = mChatClient.getConversations(mCurrentUserId);
        call.enqueue(new Callback<List<Conversation>>() {
            @Override
            public void onResponse(@NonNull Call<List<Conversation>> call, @NonNull Response<List<Conversation>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Conversation> conversations = response.body();
                    Log.d(TAG, "✅ Loaded " + conversations.size() + " conversations");

                    mConversations.clear();
                    mConversations.addAll(conversations);
                    mAdapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "❌ Failed to load conversations: " + response.code());
                    Toast.makeText(ConversationsListActivity.this, 
                            "Failed to load conversations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Conversation>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "❌ Network error loading conversations", t);
                Toast.makeText(ConversationsListActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChatActivity(Conversation conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("RECIPIENT_ID", conversation.getPartnerId());
        intent.putExtra("RECIPIENT_NAME", conversation.getPartnerName());
        
        // Add parcel info if available
        if (conversation.getCurrentParcelId() != null) {
            intent.putExtra("PARCEL_ID", conversation.getCurrentParcelId());
        }
        if (conversation.getCurrentParcelCode() != null) {
            intent.putExtra("PARCEL_CODE", conversation.getCurrentParcelCode());
        }
        
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload conversations khi quay lại màn hình
        loadConversations();
    }
}
