package com.example.gestorgastos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import java.util.List;

public class CategorySpinnerAdapter extends ArrayAdapter<CategoryEntity> {
    
    private final Context context;
    private final List<CategoryEntity> categories;
    
    public CategorySpinnerAdapter(@NonNull Context context, @NonNull List<CategoryEntity> categories) {
        super(context, 0, categories);
        this.context = context;
        this.categories = categories;
    }
    
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }
    
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }
    
    private View createItemView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_category_spinner, parent, false);
        }
        
        CategoryEntity category = getItem(position);
        if (category != null) {
            TextView ivIcon = convertView.findViewById(R.id.ivCategoryIcon);
            TextView tvName = convertView.findViewById(R.id.tvCategoryName);
            
            tvName.setText(category.name);
            
            // Configurar icono - mostrar el emoji que el usuario ingresó
            if (category.icono != null && !category.icono.isEmpty() && !category.icono.equals("default")) {
                // Si el usuario ingresó un emoji, mostrarlo directamente
                ivIcon.setText(category.icono);
            } else {
                // Usar emoji por defecto
                ivIcon.setText("⭐");
            }
        }
        
        return convertView;
    }
    
    public int getPositionByRemoteId(String remoteId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).remoteId != null && categories.get(i).remoteId.equals(remoteId)) {
                return i;
            }
        }
        return -1;
    }
}
