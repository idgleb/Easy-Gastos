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
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


