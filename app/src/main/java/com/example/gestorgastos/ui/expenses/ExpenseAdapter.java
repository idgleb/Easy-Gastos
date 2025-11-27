package com.example.gestorgastos.ui.expenses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.util.DateTimeUtil;
import android.util.Log;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class ExpenseAdapter extends ListAdapter<ExpenseEntity, ExpenseAdapter.ExpenseViewHolder> {
    
    private OnExpenseClickListener listener;
    private Map<String, CategoryEntity> categoryCache = new HashMap<>();
    private int lastAnimatedPosition = -1;
    private boolean animateItems = true;
    
    public interface OnExpenseClickListener {
        void onExpenseClick(ExpenseEntity expense);
        void onExpenseEdit(ExpenseEntity expense);
        void onExpenseDelete(ExpenseEntity expense);
        void onUnknownCategoryDetected(String categoryRemoteId);
    }
    
    public ExpenseAdapter() {
        super(new DiffUtil.ItemCallback<ExpenseEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ExpenseEntity oldItem, @NonNull ExpenseEntity newItem) {
                return oldItem.idLocal == newItem.idLocal;
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull ExpenseEntity oldItem, @NonNull ExpenseEntity newItem) {
                return oldItem.monto == newItem.monto &&
                       oldItem.fechaEpochMillis == newItem.fechaEpochMillis &&
                       oldItem.categoryRemoteId.equals(newItem.categoryRemoteId);
            }
        });
    }
    
    public void setOnExpenseClickListener(OnExpenseClickListener listener) {
        this.listener = listener;
    }
    
            public void updateCategoryCache(List<CategoryEntity> categories) {
            categoryCache.clear();
            for (CategoryEntity category : categories) {
                if (category.remoteId != null && !category.remoteId.isEmpty()) {
                    categoryCache.put(category.remoteId, category);
                }
                // También cachear por idLocal para categorías locales
                categoryCache.put("local_" + category.idLocal, category);
            }
            Log.d("ExpenseAdapter", "Cache actualizado con " + categories.size() + " categorías");
            for (CategoryEntity cat : categories) {
                Log.d("ExpenseAdapter", "Categoría en cache: " + cat.name + " (ID: " + cat.idLocal + ", Activa: " + cat.isActive + ")");
            }
        }
    
    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        ExpenseEntity expense = getItem(position);
        holder.bind(expense);
        
        // Aplicar animación de entrada si es necesario
        if (animateItems && position > lastAnimatedPosition) {
            animateItem(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }
    
    /**
     * Anima un item específico con efecto de entrada
     */
    private void animateItem(View itemView, int position) {
        // Resetear propiedades de animación
        itemView.setAlpha(0f);
        itemView.setTranslationY(50f);
        itemView.setScaleX(0.8f);
        itemView.setScaleY(0.8f);
        
        // Animar entrada
        itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setStartDelay(position * 50) // Delay escalonado
                .start();
    }
    
    /**
     * Anima un item específico con efecto de highlight (para nuevos gastos)
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
     * Desactiva las animaciones para actualizaciones masivas
     */
    public void setAnimateItems(boolean animate) {
        this.animateItems = animate;
    }
    
    /**
     * Resetea el contador de animaciones
     */
    public void resetAnimationState() {
        this.lastAnimatedPosition = -1;
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
    
    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAmount;
        private final TextView tvDate;
        private final TextView tvCategory;
        private final TextView tvCategoryIcon;
        private final View btnEdit;
        private final View btnDelete;
        
        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
            tvCategoryIcon = itemView.findViewById(R.id.tvExpenseCategoryIcon);
            btnEdit = itemView.findViewById(R.id.btnEditExpense);
            btnDelete = itemView.findViewById(R.id.btnDeleteExpense);
            
            // Configurar click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExpenseClick(getItem(position));
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExpenseEdit(getItem(position));
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExpenseDelete(getItem(position));
                }
            });
        }
        
        public void bind(ExpenseEntity expense) {
            // Formatear monto
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
            tvAmount.setText(formatter.format(expense.monto));
            
            // Formatear fecha y hora
            String formattedDateTime = DateTimeUtil.formatDateTime(expense.fechaEpochMillis, DateTimeUtil.getCurrentZoneId());
            tvDate.setText(formattedDateTime);
            
            // Configurar categoría
            if (expense.categoryRemoteId != null && !expense.categoryRemoteId.isEmpty()) {
                String categoryName = getCategoryNameFromId(expense.categoryRemoteId);
                String categoryIcon = getCategoryIconFromId(expense.categoryRemoteId);
                
                // Solo mostrar si encontramos la categoría o si es una categoría de ejemplo
                if (!categoryName.equals("Sin categoría") || isExampleCategory(expense.categoryRemoteId)) {
                    tvCategory.setText(categoryName);
                    tvCategoryIcon.setText(categoryIcon);
                    tvCategory.setVisibility(View.VISIBLE);
                    tvCategoryIcon.setVisibility(View.VISIBLE);
                } else {
                    // Si no encontramos la categoría, mostrar ID abreviado
                    String shortId = expense.categoryRemoteId.length() > 8 
                        ? expense.categoryRemoteId.substring(0, 8) + "..." 
                        : expense.categoryRemoteId;
                    tvCategory.setText("Cat. " + shortId);
                    tvCategoryIcon.setText("❓");
                    tvCategory.setVisibility(View.VISIBLE);
                    tvCategoryIcon.setVisibility(View.VISIBLE);
                    
                    // Notificar que hay una categoría desconocida
                    if (listener != null) {
                        listener.onUnknownCategoryDetected(expense.categoryRemoteId);
                    }
                }
            } else {
                tvCategory.setText("Sin categoría");
                tvCategoryIcon.setText("⭐");
                tvCategory.setVisibility(View.VISIBLE);
                tvCategoryIcon.setVisibility(View.VISIBLE);
            }
        }
        
        // Método mejorado para obtener nombre de categoría
        private String getCategoryNameFromId(String categoryId) {
            // Primero buscar en el cache de categorías reales
            CategoryEntity category = categoryCache.get(categoryId);
            if (category != null) {
                Log.d("ExpenseAdapter", "Categoría encontrada en cache: " + category.name + " (ID: " + categoryId + ")");
                return category.name;
            }
            Log.d("ExpenseAdapter", "Categoría NO encontrada en cache: " + categoryId);
            
            // Si no se encuentra, usar categorías de ejemplo
            switch (categoryId) {
                case "cat1": return "Comida";
                case "cat2": return "Transporte";
                case "cat3": return "Entretenimiento";
                case "cat4": return "Salud";
                case "cat5": return "Educación";
                case "cat6": return "Otros";
                default: return "Sin categoría";
            }
        }
        
        // Método mejorado para obtener icono de categoría
        private String getCategoryIconFromId(String categoryId) {
            // Primero buscar en el cache de categorías reales
            CategoryEntity category = categoryCache.get(categoryId);
            if (category != null && category.icono != null && !category.icono.isEmpty() && !category.icono.equals("default")) {
                return category.icono;
            }
            
            // Si no se encuentra, usar iconos de ejemplo
            switch (categoryId) {
                case "cat1": return "🍕";
                case "cat2": return "🚗";
                case "cat3": return "🎬";
                case "cat4": return "💊";
                case "cat5": return "📚";
                case "cat6": return "📦";
                default: return "⭐";
            }
        }
        
        // Método para verificar si es una categoría de ejemplo
        private boolean isExampleCategory(String categoryId) {
            return categoryId.equals("cat1") || categoryId.equals("cat2") || 
                   categoryId.equals("cat3") || categoryId.equals("cat4") || 
                   categoryId.equals("cat5") || categoryId.equals("cat6");
        }
    }
}
