package com.example.gestorgastos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestorgastos.R;
import com.example.gestorgastos.data.EmojiCategories;

import java.util.List;

public class EmojiCategoryAdapter extends RecyclerView.Adapter<EmojiCategoryAdapter.CategoryViewHolder> {
    
    private Context context;
    private List<EmojiCategories.EmojiCategory> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = -1;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(EmojiCategories.EmojiCategory category);
    }
    
    public EmojiCategoryAdapter(Context context, List<EmojiCategories.EmojiCategory> categories) {
        this.context = context;
        this.categories = categories;
    }
    
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_emoji_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        EmojiCategories.EmojiCategory category = categories.get(position);
        holder.bind(category, position == selectedPosition);
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryIcon;
        private TextView tvCategoryName;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    selectedPosition = position;
                    notifyDataSetChanged();
                    listener.onCategoryClick(categories.get(position));
                }
            });
        }
        
        public void bind(EmojiCategories.EmojiCategory category, boolean isSelected) {
            tvCategoryIcon.setText(category.icon);
            tvCategoryName.setText(category.name);
            
            // Cambiar apariencia según si está seleccionado
            if (isSelected) {
                tvCategoryIcon.setBackgroundResource(R.drawable.emoji_category_selected_background);
                tvCategoryName.setTextColor(context.getColor(R.color.blue));
            } else {
                tvCategoryIcon.setBackgroundResource(R.drawable.emoji_category_background);
                tvCategoryName.setTextColor(context.getColor(R.color.gris_icono));
            }
        }
    }
}
