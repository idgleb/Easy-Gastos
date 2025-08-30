package com.example.gestorgastos.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.repository.CategoryRepository;
import com.example.gestorgastos.data.repository.CategoryRepositoryImpl;
import com.example.gestorgastos.data.repository.ExpenseRepository;
import com.example.gestorgastos.data.repository.ExpenseRepositoryImpl;
import java.util.List;

public class DashboardViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public DashboardViewModel(@NonNull Application application) {
        super(application);
        this.categoryRepository = new CategoryRepositoryImpl(application);
        this.expenseRepository = new ExpenseRepositoryImpl(application);
    }
    
    // LiveData para el dashboard
    public LiveData<List<CategoryEntity>> getActiveCategoriesByUser(String userUid) {
        return categoryRepository.getActiveCategoriesByUser(userUid);
    }
    
    public LiveData<List<ExpenseEntity>> getExpensesByUser(String userUid) {
        return expenseRepository.getExpensesByUser(userUid);
    }
    
    // Estados de la UI
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    // Métodos de utilidad para cálculos del dashboard
    public double calculateTotalExpenses(List<ExpenseEntity> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }
        
        return expenses.stream()
                .mapToDouble(expense -> expense.monto)
                .sum();
    }
    
    public double calculateTotalExpensesForMonth(List<ExpenseEntity> expenses, long monthStart, long monthEnd) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }
        
        return expenses.stream()
                .filter(expense -> expense.fechaEpochMillis >= monthStart && expense.fechaEpochMillis <= monthEnd)
                .mapToDouble(expense -> expense.monto)
                .sum();
    }
    
    public void clearMessages() {
        errorMessage.setValue(null);
    }
    
    // Métodos para navegación de meses
    public void previousMonth() {
        // TODO: Implementar navegación al mes anterior
        // Por ahora solo limpiar mensajes
        clearMessages();
    }
    
    public void nextMonth() {
        // TODO: Implementar navegación al mes siguiente
        // Por ahora solo limpiar mensajes
        clearMessages();
    }
}
