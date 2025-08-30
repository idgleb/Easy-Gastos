package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.CategoryEntity;

public class CategoryDialog extends DialogFragment {
    
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_USER_UID = "user_uid";
    
    private CategoryEntity category;
    private String userUid;
    private OnCategoryDialogListener listener;
    
    public interface OnCategoryDialogListener {
        void onCategorySaved(CategoryEntity category);
        void onCategoryCancelled();
    }
    
    public static CategoryDialog newInstance(String userUid) {
        return newInstance(userUid, null);
    }
    
    public static CategoryDialog newInstance(String userUid, CategoryEntity category) {
        CategoryDialog dialog = new CategoryDialog();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID, userUid);
        if (category != null) {
            args.putLong("category_id", category.idLocal);
            args.putString("category_name", category.name);
            args.putString("category_icon", category.icono);
        }
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userUid = getArguments().getString(ARG_USER_UID);
            long categoryId = getArguments().getLong("category_id", 0);
            if (categoryId > 0) {
                category = new CategoryEntity();
                category.idLocal = categoryId;
                category.userUid = userUid; // Agregar userUid
                category.name = getArguments().getString("category_name", "");
                category.icono = getArguments().getString("category_icon", "default");
                category.isActive = true; // Mantener activa
                category.updatedAt = System.currentTimeMillis(); // Timestamp actual
                category.syncState = "PENDING"; // Estado de sincronización
            }
        }
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Buscar el listener en el fragmento padre
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof OnCategoryDialogListener) {
            listener = (OnCategoryDialogListener) parentFragment;
        } else if (context instanceof OnCategoryDialogListener) {
            listener = (OnCategoryDialogListener) context;
        } else {
            throw new RuntimeException("Parent fragment or activity must implement OnCategoryDialogListener");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_category, null);
        
        // Configurar vistas
        EditText etName = view.findViewById(R.id.etCategoryName);
        EditText etIcon = view.findViewById(R.id.etCategoryIcon);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        
        // Si es edición, llenar campos
        if (category != null) {
            etName.setText(category.name);
            etIcon.setText(category.icono);
        }
        
        // Configurar botones
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String icon = etIcon.getText().toString().trim();
            
            if (name.isEmpty()) {
                etName.setError("El nombre es requerido");
                return;
            }
            
            if (icon.isEmpty()) {
                icon = "default";
            }
            
            // Crear o actualizar categoría
            if (category == null) {
                category = new CategoryEntity();
                category.userUid = userUid;
                category.name = name;
                category.icono = icon;
                category.isActive = true;
                category.updatedAt = System.currentTimeMillis();
                category.syncState = "PENDING";
            } else {
                category.name = name;
                category.icono = icon;
                category.updatedAt = System.currentTimeMillis();
                category.syncState = "PENDING";
            }
            
            if (listener != null) {
                Log.d("CategoryDialog", "Guardando categoría: " + category.name + ", ID: " + category.idLocal);
                listener.onCategorySaved(category);
            }
            dismiss();
        });
        
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryCancelled();
            }
            dismiss();
        });
        
        // Configurar título del diálogo
        String title = category != null ? "Editar Categoría" : "Nueva Categoría";
        builder.setTitle(title);
        builder.setView(view);
        
        return builder.create();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
