package com.example.gestorgastos.ui.categories;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.FragmentCategoriesBinding;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.ui.dialogs.CategoryDialog;
import com.example.gestorgastos.ui.dialogs.PremiumRequiredDialog;
import com.example.gestorgastos.ui.dialogs.AuthMessageDialog;
import com.example.gestorgastos.ui.main.MainActivity;
import com.example.gestorgastos.ui.main.MainViewModel;

public class CategoriesFragment extends Fragment implements CategoryDialog.OnCategoryDialogListener {
    private FragmentCategoriesBinding binding;
    private CategoryViewModel viewModel;
    private MainViewModel mainViewModel;
    private CategoryAdapter adapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar ViewModels
        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        setupViews();
        observeViewModel();
    }
    
    private void setupViews() {
        // Configurar RecyclerView
        adapter = new CategoryAdapter();
        binding.rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvCategories.setAdapter(adapter);
        
        // Configurar click listeners del adaptador
        adapter.setOnCategoryClickListener(new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(CategoryEntity category) {
                // TODO: Mostrar detalles de la categoría
                Toast.makeText(requireContext(), getString(R.string.toast_category_prefix, category.name), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onCategoryEdit(CategoryEntity category) {
                // Verificar plan antes de editar
                com.example.gestorgastos.data.local.entity.UserEntity user = 
                    mainViewModel.getCurrentUser().getValue();
                if (user != null && !"free".equalsIgnoreCase(user.planId)) {
                showCategoryDialog(category);
                } else {
                    showPremiumRequiredDialog();
                }
            }
            
            @Override
            public void onCategoryDelete(CategoryEntity category) {
                // Verificar plan antes de eliminar
                com.example.gestorgastos.data.local.entity.UserEntity user = 
                    mainViewModel.getCurrentUser().getValue();
                if (user != null && !"free".equalsIgnoreCase(user.planId)) {
                // TODO: Mostrar confirmación antes de eliminar
                viewModel.deleteCategory(category.idLocal);
                } else {
                    showPremiumRequiredDialog();
                }
            }
        });
        
        // Configurar FAB para agregar categoría
        binding.fabAddCategory.setOnClickListener(v -> {
            // Verificar plan antes de mostrar el diálogo
            com.example.gestorgastos.data.local.entity.UserEntity user = 
                mainViewModel.getCurrentUser().getValue();
            if (user != null && !"free".equalsIgnoreCase(user.planId)) {
            showAddCategoryDialog();
            } else {
                showPremiumRequiredDialog();
            }
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
                // Observar lista de categorías del usuario actual
                viewModel.getActiveCategoriesByUser(user.uid).observe(getViewLifecycleOwner(), categories -> {
                    adapter.submitList(categories);
                });
            }
        });
        
        // Observar estados de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // TODO: Agregar ProgressBar al layout si es necesario
        });
        
        // Observar mensajes de error
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showErrorDialog(errorMessage);
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
    
    private void showAddCategoryDialog() {
        showCategoryDialog(null);
    }
    
    private void showCategoryDialog(CategoryEntity category) {
        String userUid = mainViewModel.getCurrentUserUid();
        if (userUid != null) {
            CategoryDialog dialog = CategoryDialog.newInstance(userUid, category);
            dialog.show(getChildFragmentManager(), "CategoryDialog");
        }
    }
    
    private void showPremiumRequiredDialog() {
        PremiumRequiredDialog dialog = PremiumRequiredDialog.newInstance();
        dialog.setOnUpgradeClickListener(() -> {
            // Iniciar directamente el flujo de actualización de plan
            Log.d("CategoriesFragment", "onUpgradeClicked - Iniciando actualización de plan");
            String userUid = mainViewModel.getCurrentUserUid();
            if (userUid != null && !userUid.isEmpty()) {
                Log.d("CategoriesFragment", "Usuario encontrado: " + userUid);
                if (getActivity() instanceof MainActivity) {
                    Log.d("CategoriesFragment", "Llamando a onUpgradePlanClicked");
                    // Obtener el planId del usuario actual si está disponible
                    com.example.gestorgastos.data.local.entity.UserEntity user = 
                        mainViewModel.getCurrentUser().getValue();
                    String planId = (user != null && user.planId != null) ? user.planId : "free";
                    ((MainActivity) getActivity()).onUpgradePlanClicked(userUid, planId);
                } else {
                    Log.e("CategoriesFragment", "Activity no es MainActivity: " + getActivity());
                }
            } else {
                Log.e("CategoriesFragment", "userUid es null o vacío");
                Toast.makeText(getContext(), getString(R.string.toast_category_user_error), Toast.LENGTH_LONG).show();
            }
        });
        dialog.show(getChildFragmentManager(), "PremiumRequiredDialog");
    }
    
    // Implementación de CategoryDialog.OnCategoryDialogListener
    @Override
    public void onCategorySaved(CategoryEntity category) {
        Log.d("CategoriesFragment", "onCategorySaved - ID: " + category.idLocal + ", Nombre: " + category.name);
        if (category.idLocal == 0) {
            // Nueva categoría
            Log.d("CategoriesFragment", "Insertando nueva categoría");
            viewModel.insertCategory(category);
            
            // Aplicar animación hide/reveal para nueva categoría
            binding.rvCategories.postDelayed(() -> {
                if (binding.rvCategories.getLayoutManager() != null && adapter != null) {
                    // Buscar la posición de la nueva categoría por nombre e icono
                    int newCategoryPosition = findCategoryPositionByNameAndIcon(category.name, category.icono);
                    if (newCategoryPosition >= 0) {
                        // Ocultar la nueva categoría antes del scroll
                        RecyclerView.ViewHolder viewHolder = binding.rvCategories.findViewHolderForAdapterPosition(newCategoryPosition);
                        if (viewHolder != null) {
                            adapter.hideItem(viewHolder.itemView);
                            Log.d("CategoriesFragment", "Hiding new category item before scroll");
                        }
                        
                        // Scroll a la nueva categoría
                        binding.rvCategories.smoothScrollToPosition(newCategoryPosition);
                        Log.d("CategoriesFragment", "Scrolling to new category at position: " + newCategoryPosition);
                        
                        // Revelar la nueva categoría después del scroll
                        binding.rvCategories.postDelayed(() -> {
                            RecyclerView.ViewHolder viewHolderAfterScroll = binding.rvCategories.findViewHolderForAdapterPosition(newCategoryPosition);
                            if (viewHolderAfterScroll != null) {
                                // Pequeño delay adicional para asegurar que el scroll terminó
                                viewHolderAfterScroll.itemView.postDelayed(() -> {
                                    adapter.revealItem(viewHolderAfterScroll.itemView);
                                    Log.d("CategoriesFragment", "Revealing new category item after scroll");
                                    
                                    // Después de revelar, aplicar el efecto highlight
                                    viewHolderAfterScroll.itemView.postDelayed(() -> {
                                        adapter.animateNewItem(viewHolderAfterScroll.itemView);
                                        Log.d("CategoriesFragment", "Applying highlight animation to new category");
                                    }, 200);
                                }, 100);
                            }
                        }, 600); // Delay para que termine el scroll
                    } else {
                        // Fallback: scroll to top if position not found
                        binding.rvCategories.smoothScrollToPosition(0);
                        Log.d("CategoriesFragment", "New category position not found, scrolling to top");
                    }
                }
            }, 300); // 300ms delay para permitir que se actualice la lista
        } else {
            // Editar categoría existente
            Log.d("CategoriesFragment", "Actualizando categoría existente");
            viewModel.updateCategory(category);
        }
    }
    
    @Override
    public void onCategoryCancelled() {
        // No hacer nada, solo cerrar el diálogo
    }
    
    /**
     * Busca la posición de una categoría específica por nombre e icono
     */
    private int findCategoryPositionByNameAndIcon(String categoryName, String categoryIcon) {
        if (adapter == null) {
            return -1;
        }
        
        // Usar el método del adaptador para buscar la posición
        int position = adapter.findCategoryPositionByNameAndIcon(categoryName, categoryIcon);
        Log.d("CategoriesFragment", "Category search result - Name: " + categoryName + ", Icon: " + categoryIcon + ", Position: " + position);
        return position;
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
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


