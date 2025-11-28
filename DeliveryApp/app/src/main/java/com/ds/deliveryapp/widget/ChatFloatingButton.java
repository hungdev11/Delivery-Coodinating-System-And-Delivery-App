package com.ds.deliveryapp.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.deliveryapp.ConversationsListActivity;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.service.GlobalChatService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Draggable floating action button for global chat access with unread indicator.
 * Position is saved and restored between app sessions.
 */
public class ChatFloatingButton extends FrameLayout implements GlobalChatService.GlobalChatListener {

    private static final String PREFS_NAME = "ChatFloatingButtonPrefs";
    private static final String KEY_X_POS = "chat_button_x";
    private static final String KEY_Y_POS = "chat_button_y";

    private FloatingActionButton fabChat;
    private View indicatorDot;
    private GlobalChatService globalChatService;
    
    // Dragging state
    private float dX, dY;
    private boolean isDragging = false;
    private int initialX, initialY;
    private ViewGroup parent;

    public ChatFloatingButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ChatFloatingButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatFloatingButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.widget_chat_float_button, this);
        // After inflation, find views directly from this (the root of the inflated layout)
        fabChat = findViewById(R.id.fab_chat);
        indicatorDot = findViewById(R.id.indicator_dot);
        
        android.util.Log.d("ChatFloatingButton", "init: fabChat=" + (fabChat != null ? "found" : "null") + 
            ", indicatorDot=" + (indicatorDot != null ? "found" : "null") + 
            ", childCount=" + getChildCount());
        
        // Ensure indicator dot is on top after inflation
        if (indicatorDot != null) {
            post(() -> {
                indicatorDot.bringToFront();
                // Set high elevation to ensure it's above FAB
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    indicatorDot.setElevation(12f);
                    indicatorDot.setTranslationZ(12f);
                }
            });
        }

        // Setup click listener
        fabChat.setOnClickListener(v -> {
            if (!isDragging) {
                // Only open chat if we're not dragging
                Intent intent = new Intent(context, ConversationsListActivity.class);
                context.startActivity(intent);
                // Note: Don't clear unread count here - it should be cleared when user opens a conversation
                // or views messages. The indicator should reflect the actual unread state.
            }
        });

        // Setup drag listener on the FAB itself (not the container)
        // This allows us to distinguish between drag and click
        fabChat.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Handle touch on FAB - if dragging, move the container
                boolean handled = handleTouch(ChatFloatingButton.this, event);
                if (handled) {
                    // If we handled the drag, prevent FAB from processing click
                    return true;
                }
                // Otherwise, let FAB handle click normally
                return false;
            }
        });

        // Register with GlobalChatService
        globalChatService = GlobalChatService.getInstance(context);
        globalChatService.addListener(this);
        android.util.Log.d("ChatFloatingButton", "Registered with GlobalChatService, listener count: " + 
            (globalChatService != null ? "registered" : "null"));
        
        // Update initial state on main thread after view is attached
        post(() -> {
            if (globalChatService != null) {
                int initialCount = globalChatService.getUnreadMessageCount();
                android.util.Log.d("ChatFloatingButton", "Initial unread count: " + initialCount + 
                    ", indicatorDot: " + (indicatorDot != null ? "exists" : "null"));
                updateUnreadIndicator(initialCount);
            }
        });
        
        // Restore position after layout is complete
        post(() -> restorePosition());
    }
    
    /**
     * Handle touch events for dragging
     */
    private boolean handleTouch(View view, MotionEvent event) {
        // Get parent container (FrameLayout)
        if (parent == null) {
            parent = (ViewGroup) getParent();
        }
        
        if (parent == null) {
            return false;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Calculate offset from touch point to view position
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                initialX = (int) view.getX();
                initialY = (int) view.getY();
                isDragging = false;
                // Don't intercept yet - let FAB handle click if it's just a tap
                return false; // Return false to let FAB handle click

            case MotionEvent.ACTION_MOVE:
                float newX = event.getRawX() + dX;
                float newY = event.getRawY() + dY;
                
                // Check if moved enough to be considered dragging (threshold: 20dp)
                float threshold = dpToPx(20);
                if (!isDragging && (Math.abs(newX - initialX) > threshold || Math.abs(newY - initialY) > threshold)) {
                    isDragging = true;
                    // Cancel any pending clicks
                    view.cancelPendingInputEvents();
                }
                
                if (isDragging) {
                    // Constrain to parent bounds
                    int parentWidth = parent.getWidth();
                    int parentHeight = parent.getHeight();
                    int buttonWidth = getWidth();
                    int buttonHeight = getHeight();
                    
                    // Limit movement within parent bounds
                    newX = Math.max(0, Math.min(newX, parentWidth - buttonWidth));
                    newY = Math.max(0, Math.min(newY, parentHeight - buttonHeight));
                    
                    // Update position immediately
                    setX(newX);
                    setY(newY);
                }
                return isDragging; // Consume event only when dragging

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    // Save position
                    savePosition();
                    isDragging = false;
                    return true; // Consume event to prevent click
                } else {
                    // Not dragging, allow click to proceed
                    isDragging = false;
                    return false;
                }

            default:
                return false;
        }
    }
    
    /**
     * Save current position to SharedPreferences
     */
    private void savePosition() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putFloat(KEY_X_POS, getX())
            .putFloat(KEY_Y_POS, getY())
            .apply();
    }
    
    /**
     * Restore position from SharedPreferences
     */
    private void restorePosition() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        float savedX = prefs.getFloat(KEY_X_POS, -1);
        float savedY = prefs.getFloat(KEY_Y_POS, -1);
        
        if (savedX >= 0 && savedY >= 0) {
            // Get parent bounds (the FrameLayout container)
            if (parent == null) {
                ViewGroup p = (ViewGroup) getParent();
                if (p != null) {
                    parent = p;
                }
            }
            
            if (parent != null) {
                // Wait for layout to complete
                parent.post(() -> {
                    int parentWidth = parent.getWidth();
                    int parentHeight = parent.getHeight();
                    
                    if (parentWidth > 0 && parentHeight > 0) {
                        int buttonWidth = getWidth();
                        int buttonHeight = getHeight();
                        
                        // Validate saved position is within bounds
                        if (buttonWidth > 0 && buttonHeight > 0) {
                            if (savedX <= parentWidth - buttonWidth && savedY <= parentHeight - buttonHeight) {
                                setX(savedX);
                                setY(savedY);
                            } else {
                                // Reset to default position if saved position is invalid
                                resetToDefaultPosition();
                            }
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Reset button to default position (bottom-right)
     */
    private void resetToDefaultPosition() {
        if (parent != null) {
            parent.post(() -> {
                int parentWidth = parent.getWidth();
                int parentHeight = parent.getHeight();
                int buttonWidth = getWidth();
                int buttonHeight = getHeight();
                
                if (parentWidth > 0 && parentHeight > 0 && buttonWidth > 0 && buttonHeight > 0) {
                    float defaultX = parentWidth - buttonWidth - dpToPx(16);
                    float defaultY = parentHeight - buttonHeight - dpToPx(16);
                    setX(defaultX);
                    setY(defaultY);
                    savePosition();
                }
            });
        }
    }
    
    /**
     * Convert dp to pixels
     */
    private int dpToPx(float dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    @Override
    public void onMessageReceived(com.ds.deliveryapp.clients.res.Message message) {
        // Handled by onUnreadCountChanged
    }

    @Override
    public void onUnreadCountChanged(int count) {
        // Update UI on main thread
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            updateUnreadIndicator(count);
        } else {
            post(() -> updateUnreadIndicator(count));
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        // Update UI if needed
    }

    @Override
    public void onError(String error) {
        // Handle error if needed
    }

    @Override
    public void onNotificationReceived(String notificationJson) {
        // Handle notification if needed
    }

    @Override
    public void onUserStatusUpdate(String userId, boolean isOnline) {
        // User status updates are not relevant for floating button
    }

    @Override
    public void onTypingIndicatorUpdate(String userId, String conversationId, boolean isTyping) {
        // Typing indicators are not relevant for floating button
    }

    private void updateUnreadIndicator(int unreadCount) {
        android.util.Log.d("ChatFloatingButton", "updateUnreadIndicator called: count=" + unreadCount + 
            ", indicatorDot=" + (indicatorDot != null ? "exists" : "null") +
            ", isAttachedToWindow=" + isAttachedToWindow() +
            ", childCount=" + getChildCount());
        
        // Ensure we're on the main thread
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            post(() -> updateUnreadIndicator(unreadCount));
            return;
        }
        
        if (indicatorDot == null) {
            // Try to find it again (in case view wasn't fully inflated)
            indicatorDot = findViewById(R.id.indicator_dot);
            android.util.Log.d("ChatFloatingButton", "Re-finding indicator dot: " + (indicatorDot != null ? "found" : "not found"));
            
            // If still not found, log all child views for debugging
            if (indicatorDot == null) {
                android.util.Log.e("ChatFloatingButton", "❌ Indicator dot not found! Child views: " + getChildCount());
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    android.util.Log.d("ChatFloatingButton", "Child " + i + ": " + child.getClass().getSimpleName() + 
                        ", id=" + child.getId() + ", R.id.indicator_dot=" + R.id.indicator_dot);
                }
            }
        }
        
        if (indicatorDot != null) {
            int visibility = unreadCount > 0 ? View.VISIBLE : View.GONE;
            indicatorDot.setVisibility(visibility);
            
            // Always bring to front and set high elevation to ensure it's above FAB
            indicatorDot.bringToFront();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                indicatorDot.setElevation(12f);
                indicatorDot.setTranslationZ(12f);
            }
            
            // Force a redraw and layout
            indicatorDot.invalidate();
            indicatorDot.requestLayout();
            invalidate();
            requestLayout();
            android.util.Log.d("ChatFloatingButton", "✅ Indicator dot visibility set to: " + 
                (visibility == View.VISIBLE ? "VISIBLE" : "GONE") + ", unreadCount=" + unreadCount +
                ", actualVisibility=" + (indicatorDot.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE") +
                ", elevation=" + (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ? indicatorDot.getElevation() : "N/A"));
        } else {
            android.util.Log.e("ChatFloatingButton", "❌ Indicator dot is null! Cannot update indicator.");
        }
    }

    public void cleanup() {
        if (globalChatService != null) {
            globalChatService.removeListener(this);
        }
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        parent = (ViewGroup) getParent();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        parent = null;
    }
}
