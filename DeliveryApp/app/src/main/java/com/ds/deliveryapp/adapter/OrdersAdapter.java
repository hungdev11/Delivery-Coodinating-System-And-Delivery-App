package com.ds.deliveryapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.model.Order;
import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private List<Order> orderList;
    private OnOrderClickListener listener;

    public OrdersAdapter(List<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.tvOrderId.setText("Mã đơn: " + order.getId());
        holder.tvCustomer.setText("Khách hàng: " + order.getCustomerName());
        holder.tvAddress.setText("Địa chỉ: " + order.getAddress());
        holder.tvStatus.setText("Trạng thái: " + order.getStatus());
        holder.tvAmount.setText(order.getFormattedAmount());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvCustomer, tvAddress, tvStatus, tvAmount;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvCustomer = itemView.findViewById(R.id.tvCustomer);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
