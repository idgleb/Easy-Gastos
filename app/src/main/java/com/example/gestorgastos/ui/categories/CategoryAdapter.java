package com.example.gestorgastos.ui.categories;

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
import com.example.gestorgastos.data.local.entity.CategoryEntity;

public class CategoryAdapter extends ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder> {
    
    private OnCategoryClickListener listener;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryEntity category);
        void onCategoryEdit(CategoryEntity category);
        void onCategoryDelete(CategoryEntity category);
    }
    
    public CategoryAdapter() {
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
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryEntity category = getItem(position);
        holder.bind(category);
    }
    
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView ivIcon;
        private final TextView tvName;
        private final View btnEdit;
        private final View btnDelete;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            btnEdit = itemView.findViewById(R.id.btnEditCategory);
            btnDelete = itemView.findViewById(R.id.btnDeleteCategory);
            
            // Configurar click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClick(getItem(position));
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryEdit(getItem(position));
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryDelete(getItem(position));
                }
            });
        }
        
        public void bind(CategoryEntity category) {
            tvName.setText(category.name);
            
            // Configurar icono - mostrar el emoji que el usuario ingresó
            if (!category.icono.isEmpty() && !category.icono.equals("default")) {
                // Si el usuario ingresó un emoji, mostrarlo directamente
                ivIcon.setText(category.icono);
            } else {
                // Usar emoji por defecto
                ivIcon.setText("⭐");
            }
        }
    }
    
    /**
     * Oculta un item específico para animación posterior
     */
    public void hideItem(View itemView) {
        itemView.setAlpha(0f);
        itemView.setScaleX(0.8f);
        itemView.setScaleY(0.8f);
        itemView.setTranslationY(30f);
    }
    
    /**
     * Revela un item oculto con animación
     */
    public void revealItem(View itemView) {
        itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }
    
    /**
     * Anima un item específico con efecto de highlight (para nuevas categorías)
     */
    public void animateNewItem(View itemView) {
        // Efecto de highlight más pronunciado para el nuevo item
        itemView.setScaleX(1.2f);
        itemView.setScaleY(1.2f);
        itemView.setAlpha(0.7f);
        
        // Primera animación: zoom in con bounce
        itemView.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .alpha(0.9f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.BounceInterpolator())
                .withEndAction(() -> {
                    // Segunda animación: zoom out suave
                    itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                })
                .start();
    }
    
    /**
     * Busca la posición de una categoría por nombre e icono
     */
    public int findCategoryPositionByNameAndIcon(String categoryName, String categoryIcon) {
        for (int i = 0; i < getItemCount(); i++) {
            CategoryEntity category = getItem(i);
            if (category.name.equals(categoryName) && category.icono.equals(categoryIcon)) {
                return i;
            }
        }
        return -1;
    }
}
