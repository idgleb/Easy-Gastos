package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.ui.dialogs.CategorySelectionBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.DatePickerDialog;

public class EditExpenseDialog extends DialogFragment {
    
    public static final String TAG = "EditExpenseDialog";
    
    private static final String ARG_EXPENSE = "expense";
    private static final String ARG_CATEGORIES = "categories";
    
    private ExpenseEntity expense;
    private List<CategoryEntity> categories;
    private CategoryEntity selectedCategory;
    private long selectedDateMillis;
    
    private TextView tvDialogTitle;
    private TextInputEditText etCategory;
    private TextInputEditText etAmount;
    private TextInputEditText etDate;
    private TextInputLayout tilCategory;
    private TextInputLayout tilAmount;
    private TextInputLayout tilDate;
    private MaterialButton btnCancel;
    private MaterialButton btnSave;
    private ImageButton btnClose;
    
    private OnExpenseEditedListener listener;
    
    public interface OnExpenseEditedListener {
        void onExpenseEdited(ExpenseEntity editedExpense);
        void onDialogCancelled();
    }
    
    public static EditExpenseDialog newInstance(ExpenseEntity expense, List<CategoryEntity> categories) {
        EditExpenseDialog dialog = new EditExpenseDialog();
        Bundle args = new Bundle();
        // Pasar datos individuales en lugar de objetos completos
        args.putLong(ARG_EXPENSE + "_id", expense.idLocal);
        args.putString(ARG_EXPENSE + "_remoteId", expense.remoteId);
        args.putString(ARG_EXPENSE + "_userUid", expense.userUid);
        args.putString(ARG_EXPENSE + "_categoryId", expense.categoryRemoteId);
        args.putDouble(ARG_EXPENSE + "_monto", expense.monto);
        args.putLong(ARG_EXPENSE + "_fecha", expense.fechaEpochMillis);
        args.putSerializable(ARG_CATEGORIES, (java.io.Serializable) categories);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Material3DialogTheme);
        
        if (getArguments() != null) {
            // Reconstruir ExpenseEntity desde los argumentos
            expense = new ExpenseEntity();
            expense.idLocal = getArguments().getLong(ARG_EXPENSE + "_id");
            expense.remoteId = getArguments().getString(ARG_EXPENSE + "_remoteId");
            expense.userUid = getArguments().getString(ARG_EXPENSE + "_userUid");
            expense.categoryRemoteId = getArguments().getString(ARG_EXPENSE + "_categoryId");
            expense.monto = getArguments().getDouble(ARG_EXPENSE + "_monto");
            expense.fechaEpochMillis = getArguments().getLong(ARG_EXPENSE + "_fecha");
            
            categories = (List<CategoryEntity>) getArguments().getSerializable(ARG_CATEGORIES);
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_expense, container, false);
        
        initViews(view);
        setupListeners();
        populateFields();
        
        return view;
    }
    
    private void initViews(View view) {
        tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        etCategory = view.findViewById(R.id.etCategory);
        etAmount = view.findViewById(R.id.etAmount);
        etDate = view.findViewById(R.id.etDate);
        tilCategory = view.findViewById(R.id.tilCategory);
        tilAmount = view.findViewById(R.id.tilAmount);
        tilDate = view.findViewById(R.id.tilDate);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
        btnClose = view.findViewById(R.id.btnClose);
    }
    
    private void setupListeners() {
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDialogCancelled();
            }
            dismiss();
        });
        
        btnSave.setOnClickListener(v -> {
            if (validateAndSave()) {
                dismiss();
            }
        });
        
        btnClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDialogCancelled();
            }
            dismiss();
        });
        
        etCategory.setOnClickListener(v -> {
            showCategorySelection();
        });
        
        etDate.setOnClickListener(v -> {
            showDatePicker();
        });
        
        getDialog().setOnCancelListener(dialog -> {
            if (listener != null) {
                listener.onDialogCancelled();
            }
        });
    }
    
    private void populateFields() {
        if (expense != null) {
            // Encontrar la categoría actual
            for (CategoryEntity category : categories) {
                if (String.valueOf(category.idLocal).equals(expense.categoryRemoteId) || 
                    ("local_" + category.idLocal).equals(expense.categoryRemoteId)) {
                    selectedCategory = category;
                    break;
                }
            }
            
            if (selectedCategory != null) {
                etCategory.setText(selectedCategory.name);
            }
            
            // Formatear el monto
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
            String amountText = formatter.format(expense.monto);
            etAmount.setText(amountText);
            
            // Formatear la fecha
            selectedDateMillis = expense.fechaEpochMillis;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateText = dateFormat.format(new Date(expense.fechaEpochMillis));
            etDate.setText(dateText);
        }
    }
    
    private void showCategorySelection() {
        CategorySelectionBottomSheet bottomSheet = CategorySelectionBottomSheet.newInstance();
        bottomSheet.setOnCategorySelectedListener(new CategorySelectionBottomSheet.OnCategorySelectedListener() {
            @Override
            public void onCategorySelected(CategoryEntity category) {
                selectedCategory = category;
                etCategory.setText(category.name);
                tilCategory.setError(null); // Limpiar error si existe
            }
        });
        bottomSheet.show(getParentFragmentManager(), "CategorySelectionBottomSheet");
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateMillis);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                selectedDateMillis = selectedCalendar.getTimeInMillis();
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String dateText = dateFormat.format(selectedCalendar.getTime());
                etDate.setText(dateText);
                tilDate.setError(null); // Limpiar error si existe
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }
    
    private boolean validateAndSave() {
        boolean isValid = true;
        
        // Validar categoría
        if (selectedCategory == null) {
            tilCategory.setError("Selecciona una categoría");
            isValid = false;
        } else {
            tilCategory.setError(null);
        }
        
        // Validar fecha
        if (selectedDateMillis == 0) {
            tilDate.setError("Selecciona una fecha");
            isValid = false;
        } else {
            tilDate.setError(null);
        }
        
        // Validar monto
        String amountText = etAmount.getText().toString().trim();
        if (amountText.isEmpty()) {
            tilAmount.setError("Ingresa el monto del gasto");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountText);
                if (amount <= 0) {
                    tilAmount.setError("El monto debe ser mayor a 0");
                    isValid = false;
                } else {
                    tilAmount.setError(null);
                }
            } catch (NumberFormatException e) {
                tilAmount.setError("Ingresa un monto válido");
                isValid = false;
            }
        }
        
        if (isValid && listener != null) {
            // Crear gasto editado
            ExpenseEntity editedExpense = new ExpenseEntity();
            editedExpense.idLocal = expense.idLocal;
            editedExpense.remoteId = expense.remoteId;
            editedExpense.userUid = expense.userUid;
            editedExpense.monto = Double.parseDouble(amountText);
            editedExpense.categoryRemoteId = "local_" + selectedCategory.idLocal;
            editedExpense.fechaEpochMillis = selectedDateMillis; // Usar la fecha seleccionada
            editedExpense.updatedAt = System.currentTimeMillis();
            editedExpense.syncState = "PENDING";
            
            listener.onExpenseEdited(editedExpense);
        }
        
        return isValid;
    }
    
    public void setOnExpenseEditedListener(OnExpenseEditedListener listener) {
        this.listener = listener;
    }
}
