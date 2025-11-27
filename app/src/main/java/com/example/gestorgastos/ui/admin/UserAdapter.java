package com.example.gestorgastos.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.UserEntity;

public class UserAdapter extends ListAdapter<UserEntity, UserAdapter.UserViewHolder> {
    
    private OnUserClickListener listener;
    
    public interface OnUserClickListener {
        void onUserClick(UserEntity user);
        void onUserEdit(UserEntity user);
        void onUserDelete(UserEntity user);
    }
    
    public UserAdapter() {
        super(new DiffUtil.ItemCallback<UserEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
                return oldItem.uid.equals(newItem.uid);
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
                // Comparar syncState (puede ser null)
                String oldSyncState = oldItem.syncState != null ? oldItem.syncState : "";
                String newSyncState = newItem.syncState != null ? newItem.syncState : "";
                
                return oldItem.name.equals(newItem.name) &&
                       oldItem.email.equals(newItem.email) &&
                       oldItem.planId.equals(newItem.planId) &&
                       oldItem.role.equals(newItem.role) &&
                       oldSyncState.equals(newSyncState);
            }
        });
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity user = getItem(position);
        holder.bind(user);
    }
    
    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUserName;
        private final TextView tvUserEmail;
        private final TextView tvUserPlan;
        private final TextView tvUserRole;
        private final ImageView ivSyncPending;
        private final View btnEdit;
        private final View btnDelete;
        
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserPlan = itemView.findViewById(R.id.tvUserPlan);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            ivSyncPending = itemView.findViewById(R.id.ivSyncPending);
            btnEdit = itemView.findViewById(R.id.btnEditUser);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            
            // Configurar click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(getItem(position));
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserEdit(getItem(position));
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserDelete(getItem(position));
                }
            });
        }
        
        public void bind(UserEntity user) {
            tvUserName.setText(user.name);
            tvUserEmail.setText(user.email);
            tvUserPlan.setText("Plan: " + user.planId);
            tvUserRole.setText("Rol: " + user.role);
            
            // Mostrar icono de sincronización pendiente si syncState == "PENDING"
            if (user.syncState != null && user.syncState.equals("PENDING")) {
                ivSyncPending.setVisibility(View.VISIBLE);
            } else {
                ivSyncPending.setVisibility(View.GONE);
            }
        }
    }
}

