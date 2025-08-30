package com.example.gestorgastos.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.gestorgastos.databinding.BottomSheetAccountBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AccountBottomSheet extends BottomSheetDialogFragment {
    
    private BottomSheetAccountBinding binding;
    private String userName = "Usuario";
    private String userEmail = "usuario@email.com";
    
    public interface OnAccountActionListener {
        void onSettingsClicked();
        void onAboutClicked();
        void onLogoutClicked();
    }
    
    private OnAccountActionListener listener;
    
    public static AccountBottomSheet newInstance(String userName, String userEmail) {
        AccountBottomSheet bottomSheet = new AccountBottomSheet();
        bottomSheet.userName = userName != null ? userName : "Usuario";
        bottomSheet.userEmail = userEmail != null ? userEmail : "usuario@email.com";
        return bottomSheet;
    }
    
    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.listener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViews();
    }
    
    private void setupViews() {
        // Configurar información del usuario
        binding.tvUserName.setText(userName);
        binding.tvUserEmail.setText(userEmail);
        
        // Configurar botón de cerrar
        binding.btnCloseAccount.setOnClickListener(v -> dismiss());
        
        // Configurar opciones
        binding.btnSettings.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSettingsClicked();
            }
            dismiss();
        });
        
        binding.btnAbout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAboutClicked();
            }
            dismiss();
        });
        
        binding.btnLogout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogoutClicked();
            }
            dismiss();
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

