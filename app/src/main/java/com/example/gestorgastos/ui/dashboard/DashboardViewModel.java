package com.example.gestorgastos.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.repository.CategoryRepository;
import com.example.gestorgastos.data.repository.CategoryRepositoryImpl;
import com.example.gestorgastos.data.repository.ExpenseRepository;
import com.example.gestorgastos.data.repository.ExpenseRepositoryImpl;
import com.example.gestorgastos.ui.dashboard.DashboardFragment.CategorySummary;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    
    // Estados de la UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Navegación de meses
    private final MutableLiveData<Calendar> currentMonth = new MutableLiveData<>();
    
    // Datos del dashboard
    private final MutableLiveData<List<CategorySummary>> categorySummaries = new MutableLiveData<>();
    private final MutableLiveData<Double> totalMonthExpenses = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> monthYearText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasExpenses = new MutableLiveData<>(false);
    
    
    public DashboardViewModel(@NonNull Application application) {
        super(application);
        this.categoryRepository = new CategoryRepositoryImpl(application);
        this.expenseRepository = new ExpenseRepositoryImpl(application);
        
        // Inicializar con el mes actual
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        currentMonth.setValue(calendar);
        
        // Observar cambios en el mes actual
        observeCurrentMonth();
    }
    
    // Getters para LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<List<CategorySummary>> getCategorySummaries() {
        return categorySummaries;
    }
    
    public LiveData<Double> getTotalMonthExpenses() {
        return totalMonthExpenses;
    }
    
    
    public LiveData<String> getMonthYearText() {
        return monthYearText;
    }
    
    public LiveData<Boolean> getHasExpenses() {
        return hasExpenses;
    }
    
    
    // Métodos para navegación de meses
    public void previousMonth() {
        Calendar calendar = currentMonth.getValue();
        if (calendar != null) {
            android.util.Log.d("DashboardViewModel", "Navegando al mes anterior: " + calendar.getTime());
            calendar.add(Calendar.MONTH, -1);
            android.util.Log.d("DashboardViewModel", "Nuevo mes: " + calendar.getTime());
            currentMonth.setValue(calendar);
        } else {
            android.util.Log.w("DashboardViewModel", "currentMonth es null, no se puede navegar");
        }
    }
    
    public void nextMonth() {
        Calendar calendar = currentMonth.getValue();
        if (calendar != null) {
            android.util.Log.d("DashboardViewModel", "Navegando al mes siguiente: " + calendar.getTime());
            calendar.add(Calendar.MONTH, 1);
            android.util.Log.d("DashboardViewModel", "Nuevo mes: " + calendar.getTime());
            currentMonth.setValue(calendar);
        } else {
            android.util.Log.w("DashboardViewModel", "currentMonth es null, no se puede navegar");
        }
    }
    
    public void goToCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        currentMonth.setValue(calendar);
    }
    
    // Cargar datos del dashboard para un usuario específico
    public void loadDashboardData(String userUid) {
        android.util.Log.d("DashboardViewModel", "=== INICIANDO CARGA DE DATOS DEL DASHBOARD ===");
        android.util.Log.d("DashboardViewModel", "Usuario UID: " + userUid);
        
        if (userUid == null || userUid.isEmpty()) {
            android.util.Log.e("DashboardViewModel", "Usuario UID es null o vacío");
            errorMessage.setValue("Usuario no válido");
            return;
        }
        
        isLoading.setValue(true);
        
        // Obtener categorías y gastos
        LiveData<List<CategoryEntity>> categoriesLiveData = categoryRepository.getActiveCategoriesByUser(userUid);
        LiveData<List<ExpenseEntity>> expensesLiveData = expenseRepository.getExpensesByUser(userUid);
        
        android.util.Log.d("DashboardViewModel", "LiveData de categorías obtenido: " + (categoriesLiveData != null));
        android.util.Log.d("DashboardViewModel", "LiveData de gastos obtenido: " + (expensesLiveData != null));
        
        // Usar MediatorLiveData para combinar datos
        MediatorLiveData<DashboardData> combinedData = new MediatorLiveData<>();
        
        // Agregar fuentes de datos
        combinedData.addSource(categoriesLiveData, categories -> {
            android.util.Log.d("DashboardViewModel", "Categorías actualizadas: " + (categories != null ? categories.size() : "null"));
            List<ExpenseEntity> expenses = expensesLiveData.getValue();
            Calendar month = currentMonth.getValue();
            DashboardData data = processDashboardData(categories, expenses, month);
            combinedData.setValue(data);
        });
        
        combinedData.addSource(expensesLiveData, expenses -> {
            android.util.Log.d("DashboardViewModel", "Gastos actualizados: " + (expenses != null ? expenses.size() : "null"));
            List<CategoryEntity> categories = categoriesLiveData.getValue();
            Calendar month = currentMonth.getValue();
            DashboardData data = processDashboardData(categories, expenses, month);
            combinedData.setValue(data);
        });
        
        // Agregar el mes actual como fuente para que se actualice cuando cambie
        combinedData.addSource(currentMonth, month -> {
            android.util.Log.d("DashboardViewModel", "Mes actualizado: " + (month != null ? month.getTime() : "null"));
            List<CategoryEntity> categories = categoriesLiveData.getValue();
            List<ExpenseEntity> expenses = expensesLiveData.getValue();
            DashboardData data = processDashboardData(categories, expenses, month);
            combinedData.setValue(data);
        });
        
        // Observar los datos combinados
        combinedData.observeForever(this::updateDashboardData);
        
        android.util.Log.d("DashboardViewModel", "=== FIN INICIALIZACIÓN DE CARGA ===");
    }
    
    private void observeCurrentMonth() {
        currentMonth.observeForever(calendar -> {
            if (calendar != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", new Locale("es", "MX"));
                String monthYear = formatter.format(calendar.getTime());
                monthYearText.setValue(monthYear);
            }
        });
    }
    
    private DashboardData processDashboardData(List<CategoryEntity> categories, List<ExpenseEntity> expenses, Calendar month) {
        if (categories == null) categories = new ArrayList<>();
        if (expenses == null) expenses = new ArrayList<>();
        
        android.util.Log.d("DashboardViewModel", "=== PROCESANDO DATOS DEL DASHBOARD ===");
        android.util.Log.d("DashboardViewModel", "Categorías recibidas: " + categories.size());
        android.util.Log.d("DashboardViewModel", "Gastos recibidos: " + expenses.size());
        
        // Log de categorías
        for (CategoryEntity category : categories) {
            android.util.Log.d("DashboardViewModel", "Categoría: " + category.name + " (ID: " + category.remoteId + ")");
        }
        
        // Log de gastos
        for (ExpenseEntity expense : expenses) {
            android.util.Log.d("DashboardViewModel", "Gasto: $" + expense.monto + " - Categoría ID: " + expense.categoryRemoteId);
        }
        
        // Calcular rangos de fechas
        long monthStart = getMonthStart(month);
        long monthEnd = getMonthEnd(month);
        
        android.util.Log.d("DashboardViewModel", "Rango del mes: " + new java.util.Date(monthStart) + " - " + new java.util.Date(monthEnd));
        
        // Filtrar gastos del mes
        List<ExpenseEntity> monthExpenses = filterExpensesByDateRange(expenses, monthStart, monthEnd);
        
        android.util.Log.d("DashboardViewModel", "Gastos del mes filtrados: " + monthExpenses.size());
        
        // Calcular total del mes
        double totalMonth = calculateTotalExpenses(monthExpenses);
        
        android.util.Log.d("DashboardViewModel", "Total del mes: $" + totalMonth);
        
        // Crear resúmenes por categoría
        List<CategorySummary> summaries = createCategorySummaries(categories, monthExpenses, totalMonth);
        
        android.util.Log.d("DashboardViewModel", "Resúmenes creados: " + summaries.size());
        
        // Determinar si hay gastos
        boolean hasExpenses = !monthExpenses.isEmpty();
        
        android.util.Log.d("DashboardViewModel", "Tiene gastos: " + hasExpenses);
        android.util.Log.d("DashboardViewModel", "=== FIN PROCESAMIENTO ===");
        
        return new DashboardData(summaries, totalMonth, hasExpenses);
    }
    
    private void updateDashboardData(DashboardData data) {
        categorySummaries.setValue(data.categorySummaries);
        totalMonthExpenses.setValue(data.totalMonthExpenses);
        hasExpenses.setValue(data.hasExpenses);
        isLoading.setValue(false);
    }
    
    private List<CategorySummary> createCategorySummaries(
            List<CategoryEntity> categories, List<ExpenseEntity> expenses, double totalMonth) {
        
        android.util.Log.d("DashboardViewModel", "=== CREANDO RESUMENES DE CATEGORÍAS ===");
        android.util.Log.d("DashboardViewModel", "Total del mes para porcentajes: $" + totalMonth);
        
        // Calcular totales por categoría usando categoryRemoteId
        Map<String, Double> categoryTotals = new HashMap<>();
        for (ExpenseEntity expense : expenses) {
            String categoryRemoteId = expense.categoryRemoteId;
            double currentTotal = categoryTotals.getOrDefault(categoryRemoteId, 0.0);
            double newTotal = currentTotal + expense.monto;
            categoryTotals.put(categoryRemoteId, newTotal);
            
            android.util.Log.d("DashboardViewModel", "Gasto agregado a categoría " + categoryRemoteId + 
                ": $" + expense.monto + " (total: $" + newTotal + ")");
        }
        
        android.util.Log.d("DashboardViewModel", "Total de categorías con gastos: " + categoryTotals.size());
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            android.util.Log.d("DashboardViewModel", "Categoría " + entry.getKey() + ": $" + entry.getValue());
        }
        
        List<CategorySummary> summaries = new ArrayList<>();
        
        // Crear resúmenes
        for (CategoryEntity category : categories) {
            double amount = 0.0;
            
            // Intentar encontrar gastos para esta categoría usando diferentes métodos
            if (category.remoteId != null && !category.remoteId.isEmpty()) {
                // Método 1: Usar remoteId directamente
                amount = categoryTotals.getOrDefault(category.remoteId, 0.0);
                android.util.Log.d("DashboardViewModel", "Intentando coincidencia por remoteId: " + category.remoteId);
            }
            
            if (amount == 0.0) {
                // Método 2: Usar fallback "local_" + idLocal
                String fallbackId = "local_" + category.idLocal;
                amount = categoryTotals.getOrDefault(fallbackId, 0.0);
                android.util.Log.d("DashboardViewModel", "Intentando coincidencia por fallback: " + fallbackId);
            }
            
            double percentage = totalMonth > 0 ? (amount / totalMonth) * 100 : 0;
            
            android.util.Log.d("DashboardViewModel", "Procesando categoría: " + category.name + 
                " (ID Local: " + category.idLocal + ", Remote ID: " + category.remoteId + 
                ") - Monto encontrado: $" + amount + " - Porcentaje: " + percentage + "%");
            
            // Solo incluir categorías con gastos
            if (amount > 0) {
                CategorySummary summary = new CategorySummary(
                    category.icono,
                    category.name,
                    amount,
                    percentage,
                    0 // CategoryEntity no tiene color, usar 0 como default
                );
                summaries.add(summary);
                android.util.Log.d("DashboardViewModel", "Resumen agregado: " + category.name + " - $" + amount);
            } else {
                android.util.Log.d("DashboardViewModel", "Categoría sin gastos, omitiendo: " + category.name);
            }
        }
        
        android.util.Log.d("DashboardViewModel", "Total de resúmenes creados: " + summaries.size());
        
        // Ordenar por monto descendente
        summaries.sort((a, b) -> Double.compare(b.amount, a.amount));
        
        android.util.Log.d("DashboardViewModel", "=== FIN CREACIÓN DE RESUMENES ===");
        
        return summaries;
    }
    
    private List<ExpenseEntity> filterExpensesByDateRange(List<ExpenseEntity> expenses, long start, long end) {
        List<ExpenseEntity> filtered = new ArrayList<>();
        android.util.Log.d("DashboardViewModel", "Filtrando gastos entre " + new java.util.Date(start) + " y " + new java.util.Date(end));
        
        for (ExpenseEntity expense : expenses) {
            android.util.Log.d("DashboardViewModel", "Evaluando gasto: $" + expense.monto + " - Fecha: " + new java.util.Date(expense.fechaEpochMillis));
            if (expense.fechaEpochMillis >= start && expense.fechaEpochMillis <= end) {
                filtered.add(expense);
                android.util.Log.d("DashboardViewModel", "Gasto incluido: $" + expense.monto);
            } else {
                android.util.Log.d("DashboardViewModel", "Gasto excluido: $" + expense.monto + " (fuera del rango)");
            }
        }
        
        android.util.Log.d("DashboardViewModel", "Total gastos filtrados: " + filtered.size() + " de " + expenses.size());
        return filtered;
    }
    
    private double calculateTotalExpenses(List<ExpenseEntity> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }
        
        double total = 0.0;
        for (ExpenseEntity expense : expenses) {
            total += expense.monto;
        }
        return total;
    }
    
    private long getMonthStart(Calendar month) {
        Calendar calendar = (Calendar) month.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    
    private long getMonthEnd(Calendar month) {
        Calendar calendar = (Calendar) month.clone();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
    
    
    public void clearMessages() {
        errorMessage.setValue(null);
    }
    
    // Clase para encapsular datos del dashboard
    public static class DashboardData {
        public final List<CategorySummary> categorySummaries;
        public final double totalMonthExpenses;
        public final boolean hasExpenses;
        
        public DashboardData(List<CategorySummary> categorySummaries, 
                           double totalMonthExpenses, boolean hasExpenses) {
            this.categorySummaries = categorySummaries;
            this.totalMonthExpenses = totalMonthExpenses;
            this.hasExpenses = hasExpenses;
        }
    }
}
