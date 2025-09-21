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

import com.example.gestorgastos.databinding.FragmentCategoriesBinding;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.ui.dialogs.CategoryDialog;
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
                Toast.makeText(requireContext(), "Categoría: " + category.name, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onCategoryEdit(CategoryEntity category) {
                showCategoryDialog(category);
            }
            
            @Override
            public void onCategoryDelete(CategoryEntity category) {
                // TODO: Mostrar confirmación antes de eliminar
                viewModel.deleteCategory(category.idLocal);
            }
        });
        
        // Configurar FAB para agregar categoría
        binding.fabAddCategory.setOnClickListener(v -> {
            showAddCategoryDialog();
        });
        
        // Iniciar animación de pulsación del icono
        startFabPulseAnimation();
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
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


