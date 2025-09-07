package com.example.gestorgastos.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.example.gestorgastos.R;
import com.example.gestorgastos.ui.adapters.EmojiGridAdapter;
import com.example.gestorgastos.ui.adapters.EmojiCategoryAdapter;
import com.example.gestorgastos.data.EmojiCategories;
import com.example.gestorgastos.util.NavBarUtils;

import java.util.List;

public class EmojiPickerBottomSheet extends BottomSheetDialogFragment {
    
    private static final String ARG_SELECTED_EMOJI = "selected_emoji";
    
    private String selectedEmoji;
    private OnEmojiSelectedListener listener;
    
    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }
    
    public static EmojiPickerBottomSheet newInstance(String currentEmoji) {
        EmojiPickerBottomSheet fragment = new EmojiPickerBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_EMOJI, currentEmoji);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedEmoji = getArguments().getString(ARG_SELECTED_EMOJI, "⭐");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_emoji_picker, container, false);
        
        // Configurar NavigationBar consistente
        NavBarUtils.setConsistentNavBarColors(getDialog(), requireContext());
        
        setupViews(view);
        
        return view;
    }
    
    private void setupViews(View view) {
        TextView tvTitle = view.findViewById(R.id.tvEmojiPickerTitle);
        RecyclerView rvCategories = view.findViewById(R.id.rvEmojiCategories);
        GridView gvEmojis = view.findViewById(R.id.gvEmojis);
        MaterialButton btnClose = view.findViewById(R.id.btnCloseEmojiPicker);
        
        tvTitle.setText("Elige icono para la categoría");
        
        // Configurar botón cerrar
        btnClose.setOnClickListener(v -> {
            Log.d("EmojiPickerBottomSheet", "Botón cerrar clickeado");
            dismiss();
        });
        
        // Configurar RecyclerView de categorías
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        EmojiCategoryAdapter categoryAdapter = new EmojiCategoryAdapter(requireContext(), EmojiCategories.getAllCategories());
        rvCategories.setAdapter(categoryAdapter);
        
        // Configurar click listener para categorías
        categoryAdapter.setOnCategoryClickListener(category -> {
            Log.d("EmojiPickerBottomSheet", "Categoría seleccionada: " + category.name);
            updateEmojiGrid(gvEmojis, category.emojis);
        });
        
        // Seleccionar categoría inteligentemente basada en el emoji actual
        List<EmojiCategories.EmojiCategory> categories = EmojiCategories.getAllCategories();
        Log.d("EmojiPickerBottomSheet", "DEBUG - selectedEmoji recibido: '" + selectedEmoji + "'");
        Log.d("EmojiPickerBottomSheet", "DEBUG - selectedEmoji es null: " + (selectedEmoji == null));
        Log.d("EmojiPickerBottomSheet", "DEBUG - selectedEmoji está vacío: " + (selectedEmoji != null && selectedEmoji.trim().isEmpty()));
        
        if (!categories.isEmpty()) {
            int selectedCategoryIndex;
            EmojiCategories.EmojiCategory selectedCategory;
            
            if (selectedEmoji != null && !selectedEmoji.trim().isEmpty()) {
                // Buscar la categoría del emoji actual
                selectedCategoryIndex = EmojiCategories.findCategoryIndexForEmoji(selectedEmoji);
                selectedCategory = categories.get(selectedCategoryIndex);
                Log.d("EmojiPickerBottomSheet", "DEBUG - Emoji encontrado: " + selectedEmoji + " -> Categoría: " + selectedCategory.name + " (índice: " + selectedCategoryIndex + ")");
            } else {
                // Si no hay emoji seleccionado, usar la primera categoría
                selectedCategoryIndex = 0;
                selectedCategory = categories.get(0);
                Log.d("EmojiPickerBottomSheet", "DEBUG - Sin emoji válido -> Primera categoría: " + selectedCategory.name);
            }
            
            Log.d("EmojiPickerBottomSheet", "DEBUG - Estableciendo categoría seleccionada en índice: " + selectedCategoryIndex);
            categoryAdapter.setSelectedPosition(selectedCategoryIndex);
            updateEmojiGrid(gvEmojis, selectedCategory.emojis);
        } else {
            // Fallback: mostrar todos los emojis si no hay categorías
            Log.d("EmojiPickerBottomSheet", "DEBUG - No hay categorías disponibles");
            updateEmojiGrid(gvEmojis, EmojiCategories.getAllEmojis());
        }
        
        // Animar el RecyclerView de categorías para indicar desplazamiento
        animateCategoriesRecyclerView(rvCategories);
    }
    
    private void updateEmojiGrid(GridView gvEmojis, String[] emojis) {
        EmojiGridAdapter adapter = new EmojiGridAdapter(requireContext(), emojis, selectedEmoji);
        gvEmojis.setAdapter(adapter);
        
        // Configurar click listener
        adapter.setOnEmojiClickListener(emoji -> {
            Log.d("EmojiPickerBottomSheet", "Emoji seleccionado: " + emoji);
            if (listener != null) {
                listener.onEmojiSelected(emoji);
            }
            dismiss();
        });
    }
    
    public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
        this.listener = listener;
    }
    
    /**
     * Anima el RecyclerView de categorías con scroll automático para indicar al usuario que puede desplazarse horizontalmente
     */
    private void animateCategoriesRecyclerView(RecyclerView rvCategories) {
        if (rvCategories == null) return;
        
        // Esperar un poco para que el RecyclerView esté completamente renderizado
        rvCategories.postDelayed(() -> {
            try {
                LinearLayoutManager layoutManager = (LinearLayoutManager) rvCategories.getLayoutManager();
                if (layoutManager == null) return;
                
                // Obtener el número total de categorías
                int totalCategories = layoutManager.getItemCount();
                if (totalCategories <= 1) return; // No hay nada que hacer si solo hay una categoría
                
                // Calcular cuántas categorías se pueden ver en pantalla
                int visibleItemCount = layoutManager.findLastVisibleItemPosition() - layoutManager.findFirstVisibleItemPosition() + 1;
                
                // Si todas las categorías son visibles, no hacer scroll
                if (visibleItemCount >= totalCategories) {
                    Log.d("EmojiPickerBottomSheet", "Todas las categorías son visibles, no se necesita scroll");
                    return;
                }
                
                Log.d("EmojiPickerBottomSheet", "Iniciando scroll automático - Total: " + totalCategories + ", Visibles: " + visibleItemCount);
                
                // Hacer scroll hacia la derecha para mostrar más categorías (solo una vez con más amplitud y más lento)
                rvCategories.postDelayed(() -> {
                    // Scroll suave hacia la derecha con más amplitud (mostrar más categorías)
                    rvCategories.smoothScrollBy(300, 0); // 400px hacia la derecha (más amplitud)
                    
                    // Después de un tiempo, volver a la posición inicial
                    rvCategories.postDelayed(() -> {
                        rvCategories.smoothScrollBy(-300, 0); // Volver 400px hacia la izquierda
                    }, 900);
                }, 500); // Esperar 500ms antes de empezar
                
                Log.d("EmojiPickerBottomSheet", "Scroll automático de categorías iniciado");
                
            } catch (Exception e) {
                Log.e("EmojiPickerBottomSheet", "Error al hacer scroll automático en RecyclerView de categorías", e);
            }
        }, 300); // Esperar 300ms para que el RecyclerView esté listo
    }
}
