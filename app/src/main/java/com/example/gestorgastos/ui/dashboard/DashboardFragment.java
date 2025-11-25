package com.example.gestorgastos.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.FragmentDashboardBinding;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.ui.main.MainActivity;
import com.example.gestorgastos.ui.main.MainViewModel;
import com.example.gestorgastos.ui.expenses.ExpenseViewModel;
import com.example.gestorgastos.ui.dialogs.CategorySelectionBottomSheet;
import com.example.gestorgastos.ui.dialogs.AmountInputBottomSheet;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    
    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private ExpenseViewModel expenseViewModel;
    private MainViewModel mainViewModel;
    private NumberFormat currencyFormat;
    private String currentUserUid;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        expenseViewModel = new ViewModelProvider(
                this,
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())
        ).get(ExpenseViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
        
        setupViews();
        setupPieChart();
        observeViewModel();
        observeExpenseCreationState();
        loadUserData();
    }
    
    private void setupViews() {
        binding.btnPreviousMonth.setOnClickListener(v -> {
            Log.d(TAG, "Navegando al mes anterior");
            viewModel.previousMonth();
        });
        
        binding.btnNextMonth.setOnClickListener(v -> {
            Log.d(TAG, "Navegando al mes siguiente");
            viewModel.nextMonth();
        });

        if (binding.btnAddExpense != null) {
            binding.btnAddExpense.setOnClickListener(v -> showCategorySelectionBottomSheet());
        }
    }
    
    
    private void setupPieChart() {
        PieChart pieChart = binding.pieChart;
        
        // Deshabilitar descripción
        pieChart.getDescription().setEnabled(false);
        
        pieChart.setHoleRadius(58f);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setCenterText("Gastos");
        pieChart.setCenterTextSize(18f);
        
        // Ocultar leyenda
        pieChart.getLegend().setEnabled(false);
        
        // Configurar animación
        pieChart.animateY(1400);
    }
    
    private void observeViewModel() {
        // Observar texto del mes/año
        viewModel.getMonthYearText().observe(getViewLifecycleOwner(), monthYear -> {
            if (monthYear != null) {
                String capped = capitalizeFirst(monthYear);
                binding.tvMonthYear.setText(capped);
                updateTotalMonthTitle(capped);
            }
        });
        
        // Observar total del mes
        viewModel.getTotalMonthExpenses().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                binding.tvTotalMonth.setText(currencyFormat.format(total));
            }
        });
        
        
        // Observar resúmenes de categorías
        viewModel.getCategorySummaries().observe(getViewLifecycleOwner(), summaries -> {
            if (summaries != null) {
                Log.d(TAG, "Recibiendo " + summaries.size() + " categorías para el LinearLayout");
                for (int i = 0; i < summaries.size(); i++) {
                    Log.d(TAG, "Categoría " + i + ": " + summaries.get(i).name + " - $" + summaries.get(i).amount);
                }
                updateCategorySummaries(summaries);
                updatePieChart(summaries);
            } else {
                Log.d(TAG, "No hay resúmenes de categorías disponibles");
            }
        });
        
        // Observar estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Aquí podrías mostrar/ocultar un indicador de carga
            Log.d(TAG, "Loading state: " + isLoading);
        });
        
        // Observar errores
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error: " + error);
                // Aquí podrías mostrar un Snackbar o Toast con el error
            }
        });
        
        // Observar si hay gastos
        viewModel.getHasExpenses().observe(getViewLifecycleOwner(), hasExpenses -> {
            if (hasExpenses != null) {
                updateEmptyState(!hasExpenses);
            }
        });
    }

    private void observeExpenseCreationState() {
        if (expenseViewModel == null) {
            return;
        }

        expenseViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Estado de inserción de gasto: " + isLoading);
        });

        expenseViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                expenseViewModel.clearMessages();
                refreshDashboardData();
            }
        });

        expenseViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                expenseViewModel.clearMessages();
            }
        });
    }
    
    private void loadUserData() {
        // Obtener el UID del usuario desde MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            String userUid = mainActivity.getCurrentUserUid();
            
            if (userUid != null && !userUid.isEmpty()) {
                Log.d(TAG, "Cargando datos del dashboard para usuario: " + userUid);
                currentUserUid = userUid;
                viewModel.loadDashboardData(userUid);
            } else {
                Log.w(TAG, "No se pudo obtener el UID del usuario");
                viewModel.clearMessages();
            }
        }
    }

    private void refreshDashboardData() {
        if (currentUserUid != null && !currentUserUid.isEmpty()) {
            viewModel.loadDashboardData(currentUserUid);
        } else {
            loadUserData();
        }
    }
    
    private void updateTotalMonthTitle(String monthYearText) {
        if (monthYearText != null && !monthYearText.isEmpty()) {
            String title = "Total de " + monthYearText;
            binding.tvTotalMonthTitle.setText(title);
            binding.tvChartPeriod.setText(monthYearText);
            binding.tvCategoriesPeriod.setText(monthYearText);
            Log.d(TAG, "Título actualizado: " + title);
            Log.d(TAG, "Badge del gráfico actualizado: " + monthYearText);
            Log.d(TAG, "Badge de categorías actualizado: " + monthYearText);
        }
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        Locale locale = new Locale("es", "ES");
        String first = text.substring(0, 1).toUpperCase(locale);
        String rest = text.substring(1);
        return first + rest;
    }
    
    private void updateCategorySummaries(List<CategorySummary> summaries) {
        // Limpiar el LinearLayout
        binding.llCategorySums.removeAllViews();
        
        if (summaries == null || summaries.isEmpty()) {
            Log.d(TAG, "No hay categorías para mostrar");
            return;
        }
        
        Log.d(TAG, "Actualizando " + summaries.size() + " categorías en LinearLayout");
        
        // Crear vistas para cada categoría
        for (CategorySummary summary : summaries) {
            View categoryView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_summary, binding.llCategorySums, false);
            
            // Bindear los datos
            TextView tvIcon = categoryView.findViewById(R.id.tv_category_icon);
            TextView tvName = categoryView.findViewById(R.id.tv_category_name);
            TextView tvAmount = categoryView.findViewById(R.id.tv_amount);
            TextView tvPercentage = categoryView.findViewById(R.id.tv_percentage);
            
            tvIcon.setText(summary.icon);
            tvName.setText(summary.name);
            tvAmount.setText(currencyFormat.format(summary.amount));
            tvPercentage.setText(String.format("%.1f%%", summary.percentage));
            
            binding.llCategorySums.addView(categoryView);
            Log.d(TAG, "Agregada vista para: " + summary.name);
        }
        
        Log.d(TAG, "Total de vistas agregadas: " + binding.llCategorySums.getChildCount());
    }
    
    // Clase simple para los datos de resumen de categorías
    public static class CategorySummary {
        public String icon;
        public String name;
        public double amount;
        public double percentage;
        public int color;
        
        public CategorySummary(String icon, String name, double amount, double percentage, int color) {
            this.icon = icon;
            this.name = name;
            this.amount = amount;
            this.percentage = percentage;
            this.color = color;
        }
    }
    
    
    
    private void updatePieChart(List<CategorySummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            binding.pieChart.setVisibility(View.GONE);
            return;
        }
        
        binding.pieChart.setVisibility(View.VISIBLE);
        
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        
        for (CategorySummary summary : summaries) {
            // Usar el icono como etiqueta en lugar del nombre
            entries.add(new PieEntry((float) summary.amount, summary.icon));
            
            // Usar colores predefinidos o el color de la categoría
            int color = getCategoryColor(summary.name, summary.color);
            colors.add(color);
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(0f); // Ocultar valores dentro del círculo
        dataSet.setValueTextColor(android.R.color.transparent);
        dataSet.setValueFormatter(new PercentFormatter());
        
        PieData pieData = new PieData(dataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.invalidate();
    }
    
    private int getCategoryColor(String categoryName, int defaultColor) {
        // Sistema completamente dinámico - generar colores únicos para todas las categorías
        return generateDynamicColor(categoryName);
    }
    
    private int generateDynamicColor(String categoryName) {
        // Generar hash único basado en el nombre de la categoría
        int hash = Math.abs(categoryName.hashCode());
        
        // Usar diferentes partes del hash para generar colores completamente diferentes
        int hash1 = hash;
        int hash2 = hash >>> 8;
        int hash3 = hash >>> 16;
        
        // Sistema de colores completamente contrastantes
        // Dividir el hash en 3 partes para RGB
        int red = (hash1 % 256);
        int green = (hash2 % 256);
        int blue = (hash3 % 256);
        
        // Aplicar algoritmo de contraste máximo
        // Asegurar que al menos un canal sea muy alto y otro muy bajo
        int maxChannel = Math.max(Math.max(red, green), blue);
        int minChannel = Math.min(Math.min(red, green), blue);
        
        if (maxChannel - minChannel < 100) {
            // Si no hay suficiente contraste, forzar contraste
            int channelToBoost = hash1 % 3;
            int channelToReduce = (hash2 % 2) == 0 ? (channelToBoost + 1) % 3 : (channelToBoost + 2) % 3;
            
            if (channelToBoost == 0) red = 200 + (hash1 % 55);      // 200-255
            else if (channelToBoost == 1) green = 200 + (hash2 % 55); // 200-255
            else blue = 200 + (hash3 % 55);                         // 200-255
            
            if (channelToReduce == 0) red = 30 + (hash1 % 40);      // 30-70
            else if (channelToReduce == 1) green = 30 + (hash2 % 40); // 30-70
            else blue = 30 + (hash3 % 40);                         // 30-70
        }
        
        // Aplicar variaciones adicionales para máxima diferencia
        red = Math.max(50, Math.min(255, red + (hash1 % 30) - 15));
        green = Math.max(50, Math.min(255, green + (hash2 % 30) - 15));
        blue = Math.max(50, Math.min(255, blue + (hash3 % 30) - 15));
        
        // Asegurar que no sean colores muy similares
        int totalBrightness = red + green + blue;
        if (totalBrightness < 150) {
            // Si es muy oscuro, aclarar significativamente
            red = Math.min(255, red + 80);
            green = Math.min(255, green + 80);
            blue = Math.min(255, blue + 80);
        } else if (totalBrightness > 600) {
            // Si es muy claro, oscurecer
            red = Math.max(50, red - 60);
            green = Math.max(50, green - 60);
            blue = Math.max(50, blue - 60);
        }
        
        // Mantener alpha en 255 (opaco)
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.cardEmptyState.setVisibility(View.VISIBLE);
            binding.pieChart.setVisibility(View.GONE);
        } else {
            binding.cardEmptyState.setVisibility(View.GONE);
            binding.pieChart.setVisibility(View.VISIBLE);
        }
    }

    private void showCategorySelectionBottomSheet() {
        CategorySelectionBottomSheet bottomSheet = CategorySelectionBottomSheet.newInstance();
        bottomSheet.setOnCategorySelectedListener(this::showAmountInputBottomSheet);
        bottomSheet.show(getChildFragmentManager(), "DashboardCategorySelection");
    }

    private void showAmountInputBottomSheet(CategoryEntity category) {
        AmountInputBottomSheet bottomSheet = AmountInputBottomSheet.newInstance(category);
        bottomSheet.setOnExpenseSavedListener(this::onExpenseSavedFromDashboard);
        bottomSheet.show(getChildFragmentManager(), "DashboardAmountInput");
    }

    private void onExpenseSavedFromDashboard(ExpenseEntity expense) {
        if (expenseViewModel != null) {
            expenseViewModel.insertExpense(expense);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}





