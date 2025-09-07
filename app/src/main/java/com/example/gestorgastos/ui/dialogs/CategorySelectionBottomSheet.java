package com.example.gestorgastos.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gestorgastos.databinding.BottomSheetCategorySelectionBinding;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.ui.adapters.CategoryGridAdapter;
import com.example.gestorgastos.ui.categories.CategoryViewModel;
import com.example.gestorgastos.ui.main.MainViewModel;
import android.util.Log;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.example.gestorgastos.util.NavBarUtils;
import java.util.ArrayList;
import java.util.List;

public class CategorySelectionBottomSheet extends BottomSheetDialogFragment {
    
    private BottomSheetCategorySelectionBinding binding;
    private CategoryGridAdapter adapter;
    private CategoryViewModel categoryViewModel;
    private MainViewModel mainViewModel;
    private OnCategorySelectedListener listener;
    
    public interface OnCategorySelectedListener {
        void onCategorySelected(CategoryEntity category);
    }
    
    public static CategorySelectionBottomSheet newInstance() {
        return new CategorySelectionBottomSheet();
    }
    
    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCategorySelectionBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar ViewModels
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        setupViews();
        observeViewModel();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Mantener NavigationBar con fondo negro usando NavBarUtils
        NavBarUtils.setConsistentNavBarColors(getDialog(), requireContext());
        
        // Configurar comportamiento personalizado del BottomSheet
        setupBottomSheetBehavior();
    }
    
    private void setupViews() {
        // Configurar RecyclerView con GridLayoutManager
        adapter = new CategoryGridAdapter();
        binding.rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.rvCategories.setAdapter(adapter);
        
        // Configurar click listener del adaptador
        adapter.setOnCategoryClickListener(category -> {
            if (listener != null) {
                listener.onCategorySelected(category);
            }
            dismiss();
        });
        
        // Configurar botón de cerrar
        binding.btnCloseCategorySelection.setOnClickListener(v -> dismiss());
        
        // Las categorías se cargarán automáticamente desde el ViewModel
    }
    
    private void observeViewModel() {
        // Observar usuario actual
        mainViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Observar categorías del usuario actual
                categoryViewModel.getActiveCategoriesByUser(user.uid).observe(getViewLifecycleOwner(), categories -> {
                    Log.d("CategorySelectionBottomSheet", "Categorías recibidas: " + (categories != null ? categories.size() : "null"));
                    if (categories != null && !categories.isEmpty()) {
                        Log.d("CategorySelectionBottomSheet", "Usando categorías reales de la base de datos");
                        adapter.submitList(categories);
                    } else {
                        // Si no hay categorías reales, mostrar mensaje y usar las de ejemplo
                        Log.d("CategorySelectionBottomSheet", "No hay categorías reales, usando categorías de ejemplo");
                        showEmptyCategoriesMessage();
                        loadExampleCategories();
                    }
                });
            }
        });
    }
    
    private void showEmptyCategoriesMessage() {
        // TODO: Mostrar mensaje de que no hay categorías creadas
        // Por ahora, usar categorías de ejemplo como fallback
    }
    
    private void loadExampleCategories() {
        List<CategoryEntity> exampleCategories = new ArrayList<>();
        
        CategoryEntity cat1 = new CategoryEntity();
        cat1.remoteId = "cat1";
        cat1.name = "Comida";
        cat1.icono = "🍕";
        exampleCategories.add(cat1);
        
        CategoryEntity cat2 = new CategoryEntity();
        cat2.remoteId = "cat2";
        cat2.name = "Transporte";
        cat2.icono = "🚗";
        exampleCategories.add(cat2);
        
        CategoryEntity cat3 = new CategoryEntity();
        cat3.remoteId = "cat3";
        cat3.name = "Entretenimiento";
        cat3.icono = "🎬";
        exampleCategories.add(cat3);
        
        CategoryEntity cat4 = new CategoryEntity();
        cat4.remoteId = "cat4";
        cat4.name = "Salud";
        cat4.icono = "💊";
        exampleCategories.add(cat4);
        
        CategoryEntity cat5 = new CategoryEntity();
        cat5.remoteId = "cat5";
        cat5.name = "Educación";
        cat5.icono = "📚";
        exampleCategories.add(cat5);
        
        CategoryEntity cat6 = new CategoryEntity();
        cat6.remoteId = "cat6";
        cat6.name = "Otros";
        cat6.icono = "📦";
        exampleCategories.add(cat6);
        
        adapter.submitList(exampleCategories);
    }
    
    /**
     * Configura el comportamiento del BottomSheet con NestedScrollView
     */
    private void setupBottomSheetBehavior() {
        try {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                
                // Configurar el BottomSheet para que funcione con NestedScrollView
                behavior.setDraggable(true);
                behavior.setHideable(true);
                behavior.setSkipCollapsed(false);
                
                Log.d("CategorySelectionBottomSheet", "BottomSheet configurado con NestedScrollView");
            }
        } catch (Exception e) {
            Log.e("CategorySelectionBottomSheet", "Error al configurar el BottomSheet", e);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
