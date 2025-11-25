package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.DialogPremiumRequiredBinding;

public class PremiumRequiredDialog extends DialogFragment {
    
    private DialogPremiumRequiredBinding binding;
    private OnUpgradeClickListener listener;
    
    public interface OnUpgradeClickListener {
        void onUpgradeClicked();
    }
    
    public static PremiumRequiredDialog newInstance() {
        return new PremiumRequiredDialog();
    }
    
    public void setOnUpgradeClickListener(OnUpgradeClickListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Material3DialogTheme);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogPremiumRequiredBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews();
    }
    
    private void setupViews() {
        // Botón cancelar
        binding.btnCancel.setOnClickListener(v -> dismiss());
        
        // Botón actualizar plan
        binding.btnUpgrade.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpgradeClicked();
                // Cerrar el diálogo después de iniciar la acción
                dismiss();
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

