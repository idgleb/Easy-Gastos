package com.example.gestorgastos.ui.expenses;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.repository.ExpenseRepository;
import com.example.gestorgastos.data.repository.ExpenseRepositoryImpl;
import android.util.Log;
import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {
    private final ExpenseRepository expenseRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        this.expenseRepository = new ExpenseRepositoryImpl(application);
    }
    
    // LiveData para los gastos
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
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    // Operaciones CRUD
    public void insertExpense(ExpenseEntity expense) {
        Log.d("ExpenseViewModel", "insertExpense - Monto: " + expense.monto + ", Categoría: " + expense.categoryRemoteId);
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        expenseRepository.insertExpense(expense, new ExpenseRepository.RepositoryCallback<ExpenseEntity>() {
            @Override
            public void onSuccess(ExpenseEntity result) {
                Log.d("ExpenseViewModel", "Gasto insertado exitosamente");
                isLoading.postValue(false);
                successMessage.postValue("Gasto registrado exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("ExpenseViewModel", "Error al insertar gasto", error);
                isLoading.postValue(false);
                errorMessage.postValue("Error al registrar gasto: " + error.getMessage());
            }
        });
    }
    
    public void updateExpense(ExpenseEntity expense) {
        Log.d("ExpenseViewModel", "updateExpense - ID: " + expense.idLocal + ", Monto: " + expense.monto);
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        expenseRepository.updateExpense(expense, new ExpenseRepository.RepositoryCallback<ExpenseEntity>() {
            @Override
            public void onSuccess(ExpenseEntity result) {
                Log.d("ExpenseViewModel", "Gasto actualizado exitosamente");
                isLoading.postValue(false);
                successMessage.postValue("Gasto actualizado exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("ExpenseViewModel", "Error al actualizar gasto", error);
                isLoading.postValue(false);
                errorMessage.postValue("Error al actualizar gasto: " + error.getMessage());
            }
        });
    }
    
    public void deleteExpense(long idLocal) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        
        expenseRepository.deleteExpense(idLocal, new ExpenseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.postValue(false);
                successMessage.postValue("Gasto eliminado exitosamente");
            }
            
            @Override
            public void onError(Exception error) {
                isLoading.postValue(false);
                errorMessage.postValue("Error al eliminar gasto: " + error.getMessage());
            }
        });
    }
    
    // Métodos de utilidad
    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
    
    public ExpenseEntity createDefaultExpense(String userUid, String categoryRemoteId, double monto, long fechaEpochMillis) {
        ExpenseEntity expense = new ExpenseEntity();
        expense.userUid = userUid;
        expense.categoryRemoteId = categoryRemoteId;
        expense.monto = monto;
        expense.fechaEpochMillis = fechaEpochMillis;
        return expense;
    }
}
