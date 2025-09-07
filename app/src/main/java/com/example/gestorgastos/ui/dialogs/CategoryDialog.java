package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.Material3DialogTheme);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_category, null);
        
        // Configurar vistas
        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        TextView ivDialogIcon = view.findViewById(R.id.ivDialogIcon);
        TextInputLayout tilCategoryName = view.findViewById(R.id.tilCategoryName);
        TextInputEditText etName = view.findViewById(R.id.etCategoryName);
        TextView tvSelectedIcon = view.findViewById(R.id.tvSelectedIcon);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        
        // Configurar título e icono según el modo
        boolean isEditMode = category != null;
        String title = isEditMode ? "Editar Categoría" : "Nueva Categoría";
        String icon = isEditMode ? "✏️" : "📝";
        
        tvDialogTitle.setText(title);
        ivDialogIcon.setText(icon);
        
        // Si es edición, llenar campos
        if (isEditMode) {
            etName.setText(category.name);
            tvSelectedIcon.setText(category.icono);
        }
        
        // El selector de emojis se configurará en onStart() cuando el diálogo esté completamente creado
        
        // Configurar botones
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String iconText = tvSelectedIcon.getText().toString().trim();
            
            // Validación mejorada
            if (name.isEmpty()) {
                tilCategoryName.setError("El nombre es requerido");
                etName.requestFocus();
                return;
            } else {
                tilCategoryName.setError(null);
            }
            
            if (name.length() < 2) {
                tilCategoryName.setError("El nombre debe tener al menos 2 caracteres");
                etName.requestFocus();
                return;
            } else {
                tilCategoryName.setError(null);
            }
            
            if (iconText.isEmpty()) {
                iconText = "⭐"; // Icono por defecto más atractivo
            }
            
            // Crear o actualizar categoría
            if (category == null) {
                category = new CategoryEntity();
                category.userUid = userUid;
                category.name = name;
                category.icono = iconText;
                category.isActive = true;
                category.updatedAt = System.currentTimeMillis();
                category.syncState = "PENDING";
            } else {
                category.name = name;
                category.icono = iconText;
                category.updatedAt = System.currentTimeMillis();
                category.syncState = "PENDING";
            }
            
            if (listener != null) {
                Log.d("CategoryDialog", "Guardando categoría: " + category.name + ", ID: " + category.idLocal);
                listener.onCategorySaved(category);
            }
            Log.d("CategoryDialog", "Ocultando teclado y cerrando diálogo...");
            hideKeyboardAggressively();
            dismiss();
        });
        
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryCancelled();
            }
            hideKeyboardAggressively();
            dismiss();
        });
        
        // Configurar el diálogo sin título (ya lo tenemos en el layout)
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        
        // Configurar el diálogo para que sea cancelable al hacer clic fuera
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        
        // Configurar para mostrar teclado y enfocar automáticamente
        dialog.setOnShowListener(dialogInterface -> {
            // Enfocar el campo de nombre y mostrar teclado
            etName.requestFocus();
            etName.post(() -> {
                // Mostrar teclado después de que la vista esté completamente cargada
                if (getActivity() != null) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            });
            
            // Agregar listener para clics fuera del contenido del diálogo
            View rootView = dialog.getWindow().getDecorView();
            rootView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Verificar si el clic fue fuera del contenido del diálogo
                    View contentView = dialog.findViewById(android.R.id.content);
                    if (contentView != null) {
                        int[] location = new int[2];
                        contentView.getLocationOnScreen(location);
                        int x = (int) event.getRawX();
                        int y = (int) event.getRawY();
                        
                        // Si el clic fue fuera del contenido, cerrar el diálogo
                        if (x < location[0] || x > location[0] + contentView.getWidth() ||
                            y < location[1] || y > location[1] + contentView.getHeight()) {
                            Log.d("CategoryDialog", "Clic fuera del diálogo detectado");
                            hideKeyboardAggressively();
                            dismiss();
                            return true; // Consumir el evento
                        }
                    }
                }
                return false; // Permitir que el evento continúe
            });
        });
        
        // Configurar para ocultar teclado cuando se cierra el diálogo
        dialog.setOnDismissListener(dialogInterface -> {
            Log.d("CategoryDialog", "OnDismissListener ejecutado");
            // Ocultar teclado de manera más agresiva
            hideKeyboardAggressively();
        });
        
        // Configurar para ocultar teclado cuando se cancela el diálogo (clic fuera)
        dialog.setOnCancelListener(dialogInterface -> {
            Log.d("CategoryDialog", "OnCancelListener ejecutado");
            // Ocultar teclado de manera más agresiva
            hideKeyboardAggressively();
        });
        
        
        return dialog;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            
            // Configurar para mostrar teclado automáticamente
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            
            // Ajustar el diálogo para que se vea bien con el teclado
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            
            // Configurar el selector de emojis ahora que el diálogo está completamente creado
            TextView tvSelectedIcon = dialog.findViewById(R.id.tvSelectedIcon);
            if (tvSelectedIcon != null) {
                setupEmojiPicker(tvSelectedIcon);
            }
        }
    }
    
    /**
     * Configura el selector de emojis para el campo de icono
     */
    private void setupEmojiPicker(TextView tvSelectedIcon) {
        // Configurar click listener para el card del selector
        View cardIconSelector = getDialog().findViewById(R.id.cardIconSelector);
        if (cardIconSelector != null) {
            cardIconSelector.setOnClickListener(v -> {
                String currentEmoji = tvSelectedIcon.getText().toString();
                Log.d("CategoryDialog", "Card de icono clickeado - emoji actual: '" + currentEmoji + "'");
                showEmojiPicker(currentEmoji);
            });
        }
        
        // Configurar click listener para el botón de icono
        View btnIconSelector = getDialog().findViewById(R.id.btnIconSelector);
        if (btnIconSelector != null) {
            btnIconSelector.setOnClickListener(v -> {
                String currentEmoji = tvSelectedIcon.getText().toString();
                Log.d("CategoryDialog", "Botón de icono clickeado - emoji actual: '" + currentEmoji + "'");
                showEmojiPicker(currentEmoji);
            });
        }
    }
    
    /**
     * Muestra el selector de emojis
     */
    private void showEmojiPicker(String currentEmoji) {
        Log.d("CategoryDialog", "DEBUG - showEmojiPicker llamado con emoji: '" + currentEmoji + "'");
        EmojiPickerBottomSheet emojiPicker = EmojiPickerBottomSheet.newInstance(currentEmoji);
        
        emojiPicker.setOnEmojiSelectedListener(emoji -> {
            Log.d("CategoryDialog", "Emoji seleccionado: " + emoji);
            // Actualizar el TextView del icono seleccionado
            TextView tvSelectedIcon = getDialog().findViewById(R.id.tvSelectedIcon);
            if (tvSelectedIcon != null) {
                tvSelectedIcon.setText(emoji);
            }
        });
        
        emojiPicker.show(getParentFragmentManager(), "emoji_picker");
    }
    
    /**
     * Oculta el teclado de manera robusta
     */
    private void hideKeyboard() {
        try {
            if (getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    // Obtener la vista que tiene el foco
                    View currentFocus = getActivity().getCurrentFocus();
                    if (currentFocus != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    } else {
                        // Si no hay vista con foco, usar el método alternativo
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    }
                }
                
                // También configurar el modo de entrada del teclado
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                
                Log.d("CategoryDialog", "Teclado ocultado correctamente");
            }
        } catch (Exception e) {
            Log.e("CategoryDialog", "Error al ocultar teclado", e);
        }
    }
    
    /**
     * Oculta el teclado de manera más agresiva para evitar que aparezca al cancelar
     */
    private void hideKeyboardAggressively() {
        try {
            if (getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    // Forzar ocultación del teclado con múltiples métodos
                    imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus() != null ? 
                        getActivity().getCurrentFocus().getWindowToken() : null, 0);
                    
                    // También intentar con el método alternativo
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                }
                
                // Configurar el modo de entrada del teclado de manera más agresiva
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                
                // Limpiar el foco de cualquier vista
                View currentFocus = getActivity().getCurrentFocus();
                if (currentFocus != null) {
                    currentFocus.clearFocus();
                }
                
                // Forzar que la Activity principal obtenga el foco
                getActivity().getWindow().getDecorView().requestFocus();
                
                Log.d("CategoryDialog", "Teclado ocultado agresivamente");
            }
        } catch (Exception e) {
            Log.e("CategoryDialog", "Error al ocultar teclado agresivamente", e);
        }
    }
}
