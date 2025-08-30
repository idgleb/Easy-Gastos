package com.example.gestorgastos.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.CategoryEntity;

public class CategoryGridAdapter extends ListAdapter<CategoryEntity, CategoryGridAdapter.CategoryViewHolder> {
    
    private OnCategoryClickListener listener;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryEntity category);
    }
    
    public CategoryGridAdapter() {
        super(new DiffUtil.ItemCallback<CategoryEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull CategoryEntity oldItem, @NonNull CategoryEntity newItem) {
                return oldItem.idLocal == newItem.idLocal;
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull CategoryEntity oldItem, @NonNull CategoryEntity newItem) {
                return oldItem.name.equals(newItem.name) &&
                       oldItem.icono.equals(newItem.icono) &&
                       oldItem.isActive == newItem.isActive;
            }
        });
    }
    
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_grid, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryEntity category = getItem(position);
        holder.bind(category);
    }
    
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvIcon;
        private final TextView tvName;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            
            // Configurar click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClick(getItem(position));
                }
            });
        }
        
        public void bind(CategoryEntity category) {
            tvName.setText(category.name);
            
            // Configurar icono
            if (category.icono != null && !category.icono.isEmpty() && !category.icono.equals("default")) {
                tvIcon.setText(category.icono);
            } else {
                tvIcon.setText("⭐");
            }
        }
    }
}
