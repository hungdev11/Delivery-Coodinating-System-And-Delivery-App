package com.ds.deliveryapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.model.DeliveryProof;

import java.util.ArrayList;
import java.util.List;

public class ProofAdapter extends RecyclerView.Adapter<ProofAdapter.ProofViewHolder> {

    private List<DeliveryProof> proofs = new ArrayList<>();
    private Context context;

    public ProofAdapter(Context context) {
        this.context = context;
    }

    public void setProofs(List<DeliveryProof> proofs) {
        this.proofs = proofs != null ? proofs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProofViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_proof_thumbnail, parent, false);
        return new ProofViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProofViewHolder holder, int position) {
        DeliveryProof proof = proofs.get(position);
        
        // Load thumbnail - use thumbnail URL for videos, original URL for images
        String thumbnailUrl = proof.getThumbnailUrl();
        Glide.with(context)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.imgThumbnail);

        // Show video indicator if this is a video
        if (proof.isVideo()) {
            holder.iconVideo.setVisibility(View.VISIBLE);
        } else {
            holder.iconVideo.setVisibility(View.GONE);
        }

        // Show proof type
        String typeLabel = "DELIVERED".equals(proof.getType()) ? "Đã giao" : "Trả kho";
        holder.tvType.setText(typeLabel);

        // Click to view full screen
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(proof.getMediaUrl()), 
                    proof.isVideo() ? "video/*" : "image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // If no app can handle, try opening in browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(proof.getMediaUrl()));
                context.startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return proofs.size();
    }

    static class ProofViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        ImageView iconVideo;
        TextView tvType;

        public ProofViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_proof_thumbnail);
            iconVideo = itemView.findViewById(R.id.icon_video_indicator);
            tvType = itemView.findViewById(R.id.tv_proof_type);
        }
    }
}
