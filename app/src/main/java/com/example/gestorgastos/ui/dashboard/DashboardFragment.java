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
import androidx.core.content.ContextCompat;
import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.FragmentDashboardBinding;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.ui.main.MainActivity;
import com.example.gestorgastos.ui.main.MainViewModel;
import com.example.gestorgastos.ui.expenses.ExpenseViewModel;
import com.example.gestorgastos.ui.dialogs.CategorySelectionBottomSheet;
import com.example.gestorgastos.ui.dialogs.AmountInputBottomSheet;
import com.example.gestorgastos.ui.dialogs.AuthMessageDialog;
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
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });
        
        // Configurar colores del indicador de refresh
        binding.swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.appbar_blue),
            ContextCompat.getColor(requireContext(), R.color.blue)
        );
    }
    
    private void refreshData() {
        if (binding == null) {
            return;
        }
        
        if (currentUserUid != null) {
            // Sincronizar datos desde Firestore
            mainViewModel.syncUserDataIfNeeded();
            
            // Recargar datos del dashboard
            refreshDashboardData();
            
            // Ocultar el indicador de refresh después de un breve delay
            binding.swipeRefreshLayout.postDelayed(() -> {
                if (binding != null && binding.swipeRefreshLayout != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }, 1500);
        } else {
            if (binding.swipeRefreshLayout != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
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
        
        // Configurar colores según el tema
        int textColor = ContextCompat.getColor(requireContext(), R.color.gris_icono);
        pieChart.setCenterTextColor(textColor);
        
        // Configurar color del agujero central según el tema
        int holeColor = ContextCompat.getColor(requireContext(), R.color.bottom_nav_surface);
        pieChart.setHoleColor(holeColor);
        
        // Configurar color transparente alrededor del agujero
        pieChart.setTransparentCircleRadius(61f);
        int transparentColor = ContextCompat.getColor(requireContext(), R.color.bottom_nav_surface);
        pieChart.setTransparentCircleColor(transparentColor);
        pieChart.setTransparentCircleAlpha(0);
        
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
                showErrorDialog(error);
                viewModel.clearMessages();
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
            if (error != null && !error.isEmpty()) {
                showErrorDialog(error);
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
        
        // Paleta base de 20 colores muy contrastantes y vibrantes
        // Estos colores están distribuidos uniformemente en el círculo cromático
        int[] baseColors = {
            0xFFE91E63, // Rosa vibrante
            0xFF9C27B0, // Púrpura
            0xFF673AB7, // Púrpura profundo
            0xFF3F51B5, // Índigo
            0xFF2196F3, // Azul
            0xFF03A9F4, // Azul claro
            0xFF00BCD4, // Cian
            0xFF009688, // Verde azulado
            0xFF4CAF50, // Verde
            0xFF8BC34A, // Verde lima
            0xFFCDDC39, // Lima
            0xFFFFEB3B, // Amarillo
            0xFFFFC107, // Ámbar
            0xFFFF9800, // Naranja
            0xFFFF5722, // Naranja rojizo
            0xFF795548, // Marrón
            0xFF607D8B, // Azul gris
            0xFF9E9E9E, // Gris
            0xFFF44336, // Rojo
            0xFFE91E63  // Rosa (duplicado para completar 20)
        };
        
        // Seleccionar color base de la paleta
        int baseColorIndex = hash % baseColors.length;
        int baseColor = baseColors[baseColorIndex];
        
        // Extraer componentes RGB del color base
        int red = (baseColor >> 16) & 0xFF;
        int green = (baseColor >> 8) & 0xFF;
        int blue = baseColor & 0xFF;
        
        // Calcular "nivel de variación" basado en el hash
        // Esto permite generar variaciones del color base para categorías adicionales
        int variationLevel = (hash >>> 16) % 3; // 0, 1, o 2
        
        // Aplicar variaciones según el nivel
        // Nivel 0: color base puro (sin variación)
        // Nivel 1: variación ligera (±15)
        // Nivel 2: variación moderada (±30)
        int variation = variationLevel * 15;
        
        // Usar diferentes partes del hash para variar cada canal RGB
        int hash1 = hash;
        int hash2 = hash >>> 8;
        int hash3 = hash >>> 16;
        
        // Aplicar variación a cada canal
        red = Math.max(0, Math.min(255, red + ((hash1 % (variation * 2 + 1)) - variation)));
        green = Math.max(0, Math.min(255, green + ((hash2 % (variation * 2 + 1)) - variation)));
        blue = Math.max(0, Math.min(255, blue + ((hash3 % (variation * 2 + 1)) - variation)));
        
        // Asegurar que el color tenga suficiente saturación (no sea gris)
        // Calcular saturación aproximada
        int max = Math.max(Math.max(red, green), blue);
        int min = Math.min(Math.min(red, green), blue);
        int saturation = max - min;
        
        // Si la saturación es muy baja (color muy gris), aumentar contraste
        if (saturation < 60) {
            // Aumentar el canal más alto y reducir los otros para crear contraste
            if (red == max) {
                red = Math.min(255, red + 40);
                green = Math.max(0, green - 30);
                blue = Math.max(0, blue - 30);
            } else if (green == max) {
                green = Math.min(255, green + 40);
                red = Math.max(0, red - 30);
                blue = Math.max(0, blue - 30);
            } else {
                blue = Math.min(255, blue + 40);
                red = Math.max(0, red - 30);
                green = Math.max(0, green - 30);
            }
        }
        
        // Asegurar que el color no sea muy oscuro ni muy claro
        int totalBrightness = red + green + blue;
        if (totalBrightness < 150) {
            // Si es muy oscuro, aclarar ligeramente
            red = Math.min(255, red + 50);
            green = Math.min(255, green + 50);
            blue = Math.min(255, blue + 50);
        } else if (totalBrightness > 650) {
            // Si es muy claro, oscurecer ligeramente
            red = Math.max(0, red - 40);
            green = Math.max(0, green - 40);
            blue = Math.max(0, blue - 40);
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
    
    private void showErrorDialog(String message) {
        AuthMessageDialog dialog = AuthMessageDialog.newInstance(
            "¡Ups! 😅",
            message,
            AuthMessageDialog.TYPE_ERROR,
            "Entendido"
        );
        dialog.setOnDialogActionListener(new AuthMessageDialog.OnDialogActionListener() {
            @Override
            public void onActionClicked() {
                // No hacer nada, solo cerrar
            }
            
            @Override
            public void onDialogClosed() {
                // No hacer nada, solo cerrar
            }
        });
        dialog.show(getParentFragmentManager(), "ErrorDialog");
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





