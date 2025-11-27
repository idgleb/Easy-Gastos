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
                errorMessage.postValue(translateError(error, "create"));
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
                errorMessage.postValue(translateError(error, "update"));
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
                errorMessage.postValue(translateError(error, "delete"));
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
    
    /**
     * Traduce los errores técnicos a mensajes amigables para el usuario
     */
    private String translateError(Exception error, String context) {
        if (error == null) {
            return "Ocurrió un error inesperado. Por favor, intenta de nuevo.";
        }
        
        // Verificar si es un error de Firestore UNAVAILABLE
        if (error instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException firestoreError = 
                (com.google.firebase.firestore.FirebaseFirestoreException) error;
            if (firestoreError.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "📡 Sin conexión a internet\n\n" +
                       "No se pudo conectar con los servidores de Firebase.\n\n" +
                       "Por favor:\n\n" +
                       "• Verifica que tengas conexión a internet activa\n" +
                       "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                       "• Revisa que no estés en modo avión\n" +
                       "• Intenta de nuevo cuando tengas conexión estable";
            }
        }
        
        // Verificar si la causa es UnknownHostException
        Throwable cause = error.getCause();
        while (cause != null) {
            if (cause instanceof java.net.UnknownHostException) {
                return "📡 Sin conexión a internet\n\n" +
                       "No se pudo conectar con los servidores de Firebase.\n\n" +
                       "Por favor:\n\n" +
                       "• Verifica que tengas conexión a internet activa\n" +
                       "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                       "• Revisa que no estés en modo avión\n" +
                       "• Intenta de nuevo cuando tengas conexión estable";
            }
            cause = cause.getCause();
        }
        
        String errorMsg = error.getMessage();
        if (errorMsg == null || errorMsg.isEmpty()) {
            errorMsg = error.getClass().getSimpleName();
        }
        
        String lowerError = errorMsg.toLowerCase();
        
        // Errores de Firestore UNAVAILABLE y resolución de hostname
        if (lowerError.contains("unavailable") || 
            lowerError.contains("unable to resolve host") ||
            lowerError.contains("unknownhostexception") ||
            lowerError.contains("no address associated with hostname") ||
            lowerError.contains("firestore.googleapis.com") ||
            lowerError.contains("eai_nodata")) {
            return "📡 Sin conexión a internet\n\n" +
                   "No se pudo conectar con los servidores de Firebase.\n\n" +
                   "Por favor:\n\n" +
                   "• Verifica que tengas conexión a internet activa\n" +
                   "• Asegúrate de tener WiFi o datos móviles habilitados\n" +
                   "• Revisa que no estés en modo avión\n" +
                   "• Intenta de nuevo cuando tengas conexión estable";
        }
        
        // Errores de red generales
        if (lowerError.contains("network") || lowerError.contains("timeout") || 
            lowerError.contains("connection") || lowerError.contains("unreachable") ||
            lowerError.contains("failed to connect") || lowerError.contains("socket") ||
            lowerError.contains("connection refused") || lowerError.contains("connection reset")) {
            return "📡 Error de conexión\n\n" +
                   "No se pudo conectar con el servidor. Por favor:\n\n" +
                   "• Verifica tu conexión a internet\n" +
                   "• Asegúrate de tener WiFi o datos móviles activos\n" +
                   "• Intenta de nuevo en unos momentos";
        }
        
        // Errores de permisos
        if (lowerError.contains("permission denied") || lowerError.contains("unauthorized")) {
            return "🔒 Sin permisos\n\n" +
                   "No tienes permisos para realizar esta acción.\n\n" +
                   "Por favor, verifica tu sesión e intenta de nuevo.";
        }
        
        // Mensajes según el contexto
        String baseMessage;
        if ("create".equals(context)) {
            baseMessage = "❌ Error al registrar gasto\n\n";
        } else if ("update".equals(context)) {
            baseMessage = "❌ Error al actualizar gasto\n\n";
        } else if ("delete".equals(context)) {
            baseMessage = "❌ Error al eliminar gasto\n\n";
        } else {
            baseMessage = "❌ Error\n\n";
        }
        
        // Si el mensaje es muy técnico, mostrar uno genérico
        if (errorMsg.length() > 100 || lowerError.contains("exception") || 
            lowerError.contains("stacktrace") || lowerError.contains("at ")) {
            return baseMessage + 
                   "Ocurrió un error inesperado al procesar tu solicitud.\n\n" +
                   "Por favor, intenta de nuevo. Si el problema persiste, contacta al soporte técnico.";
        }
        
        // Mostrar el mensaje original pero formateado
        return baseMessage + errorMsg;
    }
}
