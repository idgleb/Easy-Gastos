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
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.FragmentExpensesBinding;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.ui.dialogs.CategorySelectionBottomSheet;
import com.example.gestorgastos.ui.dialogs.AmountInputBottomSheet;
import com.example.gestorgastos.ui.dialogs.EditExpenseDialog;
import com.example.gestorgastos.ui.dialogs.AuthMessageDialog;
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
                Toast.makeText(requireContext(), getString(R.string.toast_expense_value, expense.monto), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onExpenseEdit(ExpenseEntity expense) {
                showEditExpenseDialog(expense);
            }
            
            @Override
            public void onExpenseDelete(ExpenseEntity expense) {
                // TODO: Mostrar confirmación antes de eliminar
                viewModel.deleteExpense(expense.idLocal);
            }
            
            @Override
            public void onUnknownCategoryDetected(String categoryRemoteId) {
                // Sincronizar categorías cuando detectamos una categoría desconocida
                syncCategoriesForUnknownCategory(categoryRemoteId);
            }
        });
        
        // Configurar FAB para agregar gasto
        binding.fabAddExpense.setOnClickListener(v -> {
            showCategorySelectionBottomSheet();
        });
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });
        
        // Configurar colores del indicador de refresh
        binding.swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.appbar_blue),
            ContextCompat.getColor(requireContext(), R.color.blue)
        );
        
        // Iniciar animación de pulsación del icono
        startFabPulseAnimation();
    }
    
    private void refreshData() {
        if (binding == null) {
            return;
        }
        
        mainViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (binding == null) {
                return;
            }
            
            if (user != null) {
                // Sincronizar datos desde Firestore
                mainViewModel.syncUserDataIfNeeded();
                
                // Los datos se actualizarán automáticamente a través de LiveData
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
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showErrorDialog(errorMessage);
                viewModel.clearMessages();
            }
        });
        
        // Observar mensajes de éxito (sin mostrar Toast)
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null) {
                // Solo limpiar el mensaje, no mostrar Toast
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
        
        // Scroll automático hacia el nuevo item después de un breve delay
        binding.recyclerViewExpenses.postDelayed(() -> {
            if (binding.recyclerViewExpenses.getLayoutManager() != null && adapter != null) {
                // Buscar la posición del nuevo gasto en la lista
                int newItemPosition = findExpensePosition(expense);
                if (newItemPosition >= 0) {
                    // Ocultar el nuevo item antes del scroll
                    RecyclerView.ViewHolder viewHolder = binding.recyclerViewExpenses.findViewHolderForAdapterPosition(newItemPosition);
                    if (viewHolder != null) {
                        adapter.hideItem(viewHolder.itemView);
                        Log.d("ExpensesFragment", "Hiding new expense item before scroll");
                    }
                    
                    // Scroll al nuevo item
                    binding.recyclerViewExpenses.smoothScrollToPosition(newItemPosition);
                    Log.d("ExpensesFragment", "Scrolling to new expense at position: " + newItemPosition);
                    
                    // Revelar el nuevo item después del scroll
                    binding.recyclerViewExpenses.postDelayed(() -> {
                        RecyclerView.ViewHolder viewHolderAfterScroll = binding.recyclerViewExpenses.findViewHolderForAdapterPosition(newItemPosition);
                        if (viewHolderAfterScroll != null) {
                            // Pequeño delay adicional para asegurar que el scroll terminó
                            viewHolderAfterScroll.itemView.postDelayed(() -> {
                                adapter.revealItem(viewHolderAfterScroll.itemView);
                                Log.d("ExpensesFragment", "Revealing new expense item after scroll");
                                
                                // Después de revelar, aplicar el efecto highlight
                                viewHolderAfterScroll.itemView.postDelayed(() -> {
                                    adapter.animateNewItem(viewHolderAfterScroll.itemView);
                                    Log.d("ExpensesFragment", "Applying highlight animation to new expense");
                                }, 200);
                            }, 100);
                        }
                    }, 600); // Delay para que termine el scroll
                } else {
                    // Fallback: scroll to top if position not found
                    binding.recyclerViewExpenses.smoothScrollToPosition(0);
                    Log.d("ExpensesFragment", "New expense position not found, scrolling to top");
                }
            }
        }, 300); // 300ms delay para permitir que se actualice la lista
    }
    
    /**
     * Busca la posición de un gasto específico en la lista actual
     */
    private int findExpensePosition(ExpenseEntity targetExpense) {
        if (pendingExpenses == null || pendingExpenses.isEmpty()) {
            return -1;
        }
        
        for (int i = 0; i < pendingExpenses.size(); i++) {
            ExpenseEntity expense = pendingExpenses.get(i);
            // Comparar por ID local si está disponible, o por monto y fecha como fallback
            if (expense.idLocal == targetExpense.idLocal && expense.idLocal > 0) {
                return i;
            } else if (expense.idLocal == 0 && targetExpense.idLocal == 0) {
                // Para gastos nuevos sin ID, comparar por monto y fecha
                if (expense.monto == targetExpense.monto && 
                    expense.fechaEpochMillis == targetExpense.fechaEpochMillis &&
                    expense.categoryRemoteId.equals(targetExpense.categoryRemoteId)) {
                    return i;
                }
            }
        }
        
        return -1; // No encontrado
    }
    
    /**
     * Intenta mostrar los gastos solo cuando tanto las categorías como los gastos estén cargados
     */
    private void tryShowExpenses() {
        if (categoriesLoaded && expensesLoaded && adapter != null) {
            Log.d("ExpensesFragment", "Mostrando gastos - Categorías: " + categories.size() + ", Gastos: " + pendingExpenses.size());
            
            // Desactivar animaciones para la carga inicial
            adapter.setAnimateItems(false);
            adapter.submitList(pendingExpenses);
            
            // Reactivar animaciones después de un breve delay
            binding.recyclerViewExpenses.postDelayed(() -> {
                adapter.setAnimateItems(true);
                adapter.resetAnimationState();
            }, 100);
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
    
    private void startFabPulseAnimation() {
        if (binding != null && binding.ivFabIcon != null) {
            // Crear animación de escala con mayor amplitud
            android.view.animation.ScaleAnimation scaleAnimation = new android.view.animation.ScaleAnimation(
                1.0f, 1.4f, // fromX, toX (40% más grande)
                1.0f, 1.4f, // fromY, toY (40% más grande)
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f, // pivotX
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f  // pivotY
            );
            
            scaleAnimation.setDuration(1000); // 1 segundo
            scaleAnimation.setRepeatCount(android.view.animation.Animation.INFINITE);
            scaleAnimation.setRepeatMode(android.view.animation.Animation.REVERSE);
            scaleAnimation.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            
            // Iniciar animación
            binding.ivFabIcon.startAnimation(scaleAnimation);
        }
    }
    
    private void showEditExpenseDialog(ExpenseEntity expense) {
        EditExpenseDialog dialog = EditExpenseDialog.newInstance(expense, categories);
        dialog.setOnExpenseEditedListener(new EditExpenseDialog.OnExpenseEditedListener() {
            @Override
            public void onExpenseEdited(ExpenseEntity editedExpense) {
                // Actualizar el gasto en la base de datos
                viewModel.updateExpense(editedExpense);
                Toast.makeText(requireContext(), getString(R.string.toast_expense_updated), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onDialogCancelled() {
                // No hacer nada, solo cerrar el diálogo
            }
        });
        dialog.show(getParentFragmentManager(), EditExpenseDialog.TAG);
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
    
    /**
     * Sincroniza categorías cuando se detecta una categoría desconocida
     */
    private void syncCategoriesForUnknownCategory(String categoryRemoteId) {
        Log.d("ExpensesFragment", "Categoría desconocida detectada: " + categoryRemoteId + " - Sincronizando...");
        
        mainViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Forzar sincronización inmediata de categorías desde Firestore
                mainViewModel.syncUserDataIfNeeded();
                
                Log.d("ExpensesFragment", "Sincronización de categorías iniciada para categoría: " + categoryRemoteId);
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
