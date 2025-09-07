package com.example.gestorgastos.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.gestorgastos.databinding.BottomSheetAmountInputBinding;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import android.util.Log;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.util.DateTimeUtil;
import com.example.gestorgastos.ui.main.MainViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.example.gestorgastos.util.NavBarUtils;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

public class AmountInputBottomSheet extends BottomSheetDialogFragment {
    
    private BottomSheetAmountInputBinding binding;
    private CategoryEntity selectedCategory;
    private String currentAmount = "0";
    private OnExpenseSavedListener listener;
    private MainViewModel mainViewModel;
    
    public interface OnExpenseSavedListener {
        void onExpenseSaved(ExpenseEntity expense);
    }
    
    public static AmountInputBottomSheet newInstance(CategoryEntity category) {
        AmountInputBottomSheet bottomSheet = new AmountInputBottomSheet();
        bottomSheet.selectedCategory = category;
        return bottomSheet;
    }
    
    public void setOnExpenseSavedListener(OnExpenseSavedListener listener) {
        this.listener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAmountInputBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar MainViewModel
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        setupViews();
        updateAmountDisplay();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Configurar el comportamiento del BottomSheet después de que esté completamente creado
        setupBottomSheetBehavior();
        
        // Mantener NavigationBar con fondo negro usando NavBarUtils
        NavBarUtils.setConsistentNavBarColors(getDialog(), requireContext());
    }
    
    private void setupBottomSheetBehavior() {
        // Configurar el BottomSheetBehavior para que aparezca más arriba
        if (getDialog() != null && getDialog().getWindow() != null) {
            try {
                // Obtener el BottomSheetBehavior
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) binding.getRoot().getParent());
                
                // Configurar el comportamiento para que aparezca más arriba
                behavior.setPeekHeight(0); // Sin altura mínima
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Expandido por defecto
                behavior.setSkipCollapsed(true); // Saltar el estado colapsado
                behavior.setHideable(true); // Permitir ocultar
                behavior.setDraggable(true); // Permitir arrastrar
                
                // Configurar la ventana del diálogo
                getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                
                // Configurar para que aparezca más arriba
                getDialog().getWindow().setGravity(android.view.Gravity.BOTTOM);
                
                // Ajustar el padding del sistema para que aparezca más arriba
                if (getDialog().getWindow().getDecorView() != null) {
                    getDialog().getWindow().getDecorView().setPadding(0, 0, 0, 0);
                }
                
                Log.d("AmountInputBottomSheet", "BottomSheetBehavior configurado para aparecer más arriba");
                
            } catch (Exception e) {
                Log.e("AmountInputBottomSheet", "Error al configurar BottomSheetBehavior", e);
                
                // Fallback: configurar solo la ventana
                getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                getDialog().getWindow().setGravity(android.view.Gravity.BOTTOM);
            }
        }
    }
    
    private void setupViews() {
        // Configurar información de la categoría seleccionada
        if (selectedCategory != null) {
            binding.tvSelectedCategoryName.setText(selectedCategory.name);
            if (!selectedCategory.icono.isEmpty() && !selectedCategory.icono.equals("default")) {
                binding.tvSelectedCategoryIcon.setText(selectedCategory.icono);
            } else {
                binding.tvSelectedCategoryIcon.setText("⭐");
            }
        }
        
        // Configurar botón de cerrar
        binding.btnCloseAmountInput.setOnClickListener(v -> dismiss());
        
        // Configurar teclado numérico
        setupNumericKeyboard();
        
        // Configurar botón guardar
        binding.btnSave.setOnClickListener(v -> saveExpense());
    }
    
    private void setupNumericKeyboard() {
        // Números 0-9
        binding.btn0.setOnClickListener(v -> addDigit("0"));
        binding.btn1.setOnClickListener(v -> addDigit("1"));
        binding.btn2.setOnClickListener(v -> addDigit("2"));
        binding.btn3.setOnClickListener(v -> addDigit("3"));
        binding.btn4.setOnClickListener(v -> addDigit("4"));
        binding.btn5.setOnClickListener(v -> addDigit("5"));
        binding.btn6.setOnClickListener(v -> addDigit("6"));
        binding.btn7.setOnClickListener(v -> addDigit("7"));
        binding.btn8.setOnClickListener(v -> addDigit("8"));
        binding.btn9.setOnClickListener(v -> addDigit("9"));
        
        // Punto decimal
        binding.btnDecimal.setOnClickListener(v -> addDecimal());
        
        // Borrar
        binding.btnBackspace.setOnClickListener(v -> removeLastDigit());
    }
    
    private void addDigit(String digit) {
        if (currentAmount.equals("0")) {
            currentAmount = digit;
        } else {
            currentAmount += digit;
        }
        updateAmountDisplay();
        updateSaveButton();
    }
    
    private void addDecimal() {
        if (!currentAmount.contains(".")) {
            currentAmount += ".";
        }
        updateAmountDisplay();
        updateSaveButton();
    }
    
    private void removeLastDigit() {
        if (currentAmount.length() > 1) {
            currentAmount = currentAmount.substring(0, currentAmount.length() - 1);
        } else {
            currentAmount = "0";
        }
        updateAmountDisplay();
        updateSaveButton();
    }
    
    private void updateAmountDisplay() {
        try {
            double amount = Double.parseDouble(currentAmount);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
            binding.tvAmountDisplay.setText(formatter.format(amount));
        } catch (NumberFormatException e) {
            binding.tvAmountDisplay.setText("$0.00");
        }
    }
    
    private void updateSaveButton() {
        try {
            double amount = Double.parseDouble(currentAmount);
            binding.btnSave.setEnabled(amount > 0);
        } catch (NumberFormatException e) {
            binding.btnSave.setEnabled(false);
        }
    }
    
    private void saveExpense() {
        try {
            double amount = Double.parseDouble(currentAmount);
            if (amount <= 0) {
                Log.w("AmountInputBottomSheet", "Monto debe ser mayor a 0");
                return;
            }
            
            // Crear el gasto
            ExpenseEntity expense = new ExpenseEntity();
            expense.userUid = mainViewModel.getCurrentUserUid(); // Obtener del usuario actual
            
            // Manejar categoría seleccionada
            if (selectedCategory != null && selectedCategory.remoteId != null && !selectedCategory.remoteId.isEmpty()) {
                expense.categoryRemoteId = selectedCategory.remoteId;
            } else if (selectedCategory != null && selectedCategory.idLocal > 0) {
                // Si no hay remoteId, usar idLocal como fallback
                expense.categoryRemoteId = "local_" + selectedCategory.idLocal;
            } else {
                // Categoría por defecto
                expense.categoryRemoteId = "default";
            }
            
            expense.monto = amount;
            expense.fechaEpochMillis = Instant.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            expense.updatedAt = System.currentTimeMillis();
            expense.syncState = "PENDING";
            
            Log.d("AmountInputBottomSheet", "Gasto creado - UserUid: " + expense.userUid + 
                  ", CategoryRemoteId: " + expense.categoryRemoteId + 
                  ", Monto: " + expense.monto + 
                  ", Fecha: " + expense.fechaEpochMillis);
            
            if (listener != null) {
                listener.onExpenseSaved(expense);
            }
            
            dismiss();
            
        } catch (NumberFormatException e) {
            Log.e("AmountInputBottomSheet", "Error al parsear monto: " + currentAmount, e);
        } catch (Exception e) {
            Log.e("AmountInputBottomSheet", "Error al crear gasto", e);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
