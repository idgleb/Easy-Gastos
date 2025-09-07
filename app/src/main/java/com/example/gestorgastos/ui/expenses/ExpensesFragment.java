package com.example.gestorgastos.ui.expenses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gestorgastos.databinding.FragmentExpensesBinding;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.ui.dialogs.CategorySelectionBottomSheet;
import com.example.gestorgastos.ui.dialogs.AmountInputBottomSheet;
import com.example.gestorgastos.ui.categories.CategoryViewModel;
import com.example.gestorgastos.ui.main.MainViewModel;
import com.example.gestorgastos.ui.main.MainActivity;
import com.example.gestorgastos.data.repository.CategoryRepositoryImpl;
import java.util.ArrayList;
import java.util.List;

public class ExpensesFragment extends Fragment implements CategorySelectionBottomSheet.OnCategorySelectedListener, AmountInputBottomSheet.OnExpenseSavedListener {
    private FragmentExpensesBinding binding;
    private ExpenseViewModel viewModel;
    private CategoryViewModel categoryViewModel;
    private MainViewModel mainViewModel;
    private ExpenseAdapter adapter;
    private List<CategoryEntity> categories = new ArrayList<>();
    private List<ExpenseEntity> pendingExpenses = new ArrayList<>();
    private boolean categoriesLoaded = false;
    private boolean expensesLoaded = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExpensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar ViewModels
        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        setupViews();
        observeViewModel();
    }
    
    private void setupViews() {
        // Configurar RecyclerView
        adapter = new ExpenseAdapter();
        binding.recyclerViewExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewExpenses.setAdapter(adapter);
        
        // Configurar click listeners del adaptador
        adapter.setOnExpenseClickListener(new ExpenseAdapter.OnExpenseClickListener() {
            @Override
            public void onExpenseClick(ExpenseEntity expense) {
                // TODO: Mostrar detalles del gasto
                Toast.makeText(requireContext(), "Gasto: $" + expense.monto, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onExpenseEdit(ExpenseEntity expense) {
                // TODO: Implementar edición de gastos con BottomSheet
                Toast.makeText(requireContext(), "Editar gasto: $" + expense.monto, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onExpenseDelete(ExpenseEntity expense) {
                // TODO: Mostrar confirmación antes de eliminar
                viewModel.deleteExpense(expense.idLocal);
            }
        });
        
        // Configurar FAB para agregar gasto
        binding.fabAddExpense.setOnClickListener(v -> {
            showCategorySelectionBottomSheet();
        });
    }
    
    private void observeViewModel() {
        // Observar usuario actual
        mainViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Debug: ver todas las categorías en la base de datos
                ((CategoryRepositoryImpl) categoryViewModel.getCategoryRepository()).debugCategories(user.uid);
                
                // Observar categorías del usuario actual (reactivo automático)
                categoryViewModel.getAllCategoriesByUser(user.uid).observe(getViewLifecycleOwner(), categories -> {
                    this.categories = categories != null ? categories : new ArrayList<>();
                    adapter.updateCategoryCache(this.categories);
                    categoriesLoaded = true;
                    Log.d("ExpensesFragment", "Categorías actualizadas reactivamente: " + this.categories.size());
                    
                    // Intentar mostrar gastos si ya están cargados
                    tryShowExpenses();
                });
                
                // Observar lista de gastos del usuario actual (reactivo automático)
                viewModel.getExpensesByUser(user.uid).observe(getViewLifecycleOwner(), expenses -> {
                    pendingExpenses = expenses != null ? expenses : new ArrayList<>();
                    expensesLoaded = true;
                    Log.d("ExpensesFragment", "Gastos actualizados reactivamente: " + pendingExpenses.size());
                    
                    // Intentar mostrar gastos si las categorías ya están cargadas
                    tryShowExpenses();
                });
            }
        });
        
        // Observar estados de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Observar mensajes de error
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                viewModel.clearMessages();
            }
        });
        
        // Observar mensajes de éxito
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null) {
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });
    }
    
    private void showCategorySelectionBottomSheet() {
        CategorySelectionBottomSheet bottomSheet = CategorySelectionBottomSheet.newInstance();
        bottomSheet.setOnCategorySelectedListener(this);
        bottomSheet.show(getChildFragmentManager(), "CategorySelectionBottomSheet");
    }
    
    // Implementación de CategorySelectionBottomSheet.OnCategorySelectedListener
    @Override
    public void onCategorySelected(CategoryEntity category) {
        showAmountInputBottomSheet(category);
    }
    
    private void showAmountInputBottomSheet(CategoryEntity category) {
        AmountInputBottomSheet bottomSheet = AmountInputBottomSheet.newInstance(category);
        bottomSheet.setOnExpenseSavedListener(this);
        bottomSheet.show(getChildFragmentManager(), "AmountInputBottomSheet");
    }
    
    // Implementación de AmountInputBottomSheet.OnExpenseSavedListener
    @Override
    public void onExpenseSaved(ExpenseEntity expense) {
        Log.d("ExpensesFragment", "onExpenseSaved - ID: " + expense.idLocal + ", Monto: " + expense.monto);
        Log.d("ExpensesFragment", "Insertando nuevo gasto");
        viewModel.insertExpense(expense);
    }
    
    /**
     * Intenta mostrar los gastos solo cuando tanto las categorías como los gastos estén cargados
     */
    private void tryShowExpenses() {
        if (categoriesLoaded && expensesLoaded && adapter != null) {
            Log.d("ExpensesFragment", "Mostrando gastos - Categorías: " + categories.size() + ", Gastos: " + pendingExpenses.size());
            adapter.submitList(pendingExpenses);
        } else {
            Log.d("ExpensesFragment", "Esperando datos - Categorías cargadas: " + categoriesLoaded + ", Gastos cargados: " + expensesLoaded);
        }
    }
    
    /**
     * Método llamado cuando la caché de categorías se actualiza desde MainActivity
     * (Mantenido para compatibilidad, pero ahora se usa LiveData reactivo)
     */
    public void onCategoryCacheUpdated(List<CategoryEntity> categories) {
        // Ya no es necesario, se usa LiveData reactivo automáticamente
        Log.d("ExpensesFragment", "onCategoryCacheUpdated llamado pero usando LiveData reactivo");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
