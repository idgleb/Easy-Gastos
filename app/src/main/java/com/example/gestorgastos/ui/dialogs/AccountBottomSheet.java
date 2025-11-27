package com.example.gestorgastos.ui.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.databinding.BottomSheetAccountBinding;
import com.example.gestorgastos.ui.main.MainViewModel;
import com.example.gestorgastos.util.SyncPrefs;
import com.example.gestorgastos.work.SyncWorker;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.gestorgastos.util.NavBarUtils;
import android.util.Log;

public class AccountBottomSheet extends BottomSheetDialogFragment {
    
    private static final String TAG = "AccountBottomSheet";
    
    private BottomSheetAccountBinding binding;
    private MainViewModel viewModel;
    private String userName = "Usuario";
    private String userEmail = "usuario@email.com";
    private String userPlanId = "free";
    private String userUid = null;
    
    public interface OnAccountActionListener {
        void onSettingsClicked();
        void onAboutClicked();
        void onLogoutClicked();
        void onUpgradePlanClicked(String userUid, String currentPlanId);
        void onAdminClicked();
    }
    
    private OnAccountActionListener listener;
    
    private String userRole = "user";
    
    public static AccountBottomSheet newInstance(String userName, String userEmail, String planId, String userUid, String userRole) {
        AccountBottomSheet bottomSheet = new AccountBottomSheet();
        bottomSheet.userName = userName != null ? userName : "Usuario";
        bottomSheet.userEmail = userEmail != null ? userEmail : "usuario@email.com";
        bottomSheet.userPlanId = planId != null ? planId : "free";
        bottomSheet.userUid = userUid;
        bottomSheet.userRole = userRole != null ? userRole : "user";
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
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        setupViews();
        observeViewModel();
    }
    
    private void observeViewModel() {
        // Observar cambios en el usuario actual
        Log.d(TAG, "🔔 Configurando observador de usuario en AccountBottomSheet");
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Log.d(TAG, "👤 Usuario recibido en AccountBottomSheet: " + user.name + ", Plan: " + user.planId);
                updateUserInfo(user);
            } else {
                Log.w(TAG, "⚠️ Usuario es null en AccountBottomSheet");
            }
        });
    }
    
    private void updateUserInfo(UserEntity user) {
        // Actualizar información del usuario
        userName = user.name != null ? user.name : "Usuario";
        userEmail = user.email != null ? user.email : "usuario@email.com";
        userPlanId = user.planId != null ? user.planId : "free";
        userUid = user.uid;
        userRole = user.role != null ? user.role : "user";
        
        Log.d(TAG, "🔄 Actualizando UI de AccountBottomSheet - Plan: " + userPlanId);
        
        // Actualizar vistas
        if (binding != null) {
            binding.tvUserName.setText(userName);
            binding.tvUserEmail.setText(userEmail);
            
            String planLabel;
            if ("free".equalsIgnoreCase(userPlanId)) {
                planLabel = "Plan: Free";
                binding.cardUpgradePlan.setVisibility(View.VISIBLE);
            } else {
                planLabel = "Plan: " + userPlanId;
                binding.cardUpgradePlan.setVisibility(View.GONE);
            }
            binding.tvUserPlan.setText(planLabel);
            Log.d(TAG, "✅ Plan actualizado en UI: " + planLabel);
            
            // Mostrar botón de administración solo si el usuario es admin
            if ("admin".equalsIgnoreCase(userRole)) {
                binding.cardAdmin.setVisibility(View.VISIBLE);
            } else {
                binding.cardAdmin.setVisibility(View.GONE);
            }
        } else {
            Log.w(TAG, "⚠️ binding es null, no se puede actualizar UI");
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Mantener NavigationBar con fondo negro usando NavBarUtils
        NavBarUtils.setConsistentNavBarColors(getDialog(), requireContext());
    }
    
    private void setupViews() {
        // La información del usuario se actualizará en observeViewModel()
        
        // Mostrar información de última sincronización
        long lastSyncMillis = SyncPrefs.getLastSyncMillis(requireContext());
        if (lastSyncMillis > 0) {
            java.text.DateFormat dateTimeFormat =
                    android.text.format.DateFormat.getMediumDateFormat(requireContext());
            java.text.DateFormat timeFormat =
                    android.text.format.DateFormat.getTimeFormat(requireContext());
            java.util.Date date = new java.util.Date(lastSyncMillis);
            String fecha = dateTimeFormat.format(date);
            String hora = timeFormat.format(date);
            String value = "Último intento: " + fecha + " " + hora;
            binding.tvLastSyncValue.setText(value);
        } else {
            binding.tvLastSyncValue.setText("Aún no se ha sincronizado");
        }
        
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

        // Sincronización manual al tocar la tarjeta de última sincronización
        binding.cardLastSync.setOnClickListener(v -> {
            Context context = requireContext().getApplicationContext();

            OneTimeWorkRequest request =
                    new OneTimeWorkRequest.Builder(SyncWorker.class).build();
            WorkManager.getInstance(context).enqueue(request);

            // Actualizar mensaje inmediatamente
            binding.tvLastSyncValue.setText("Sincronizando ahora...");
        });
        
        // Actualizar plan al tocar la tarjeta
        binding.cardUpgradePlan.setOnClickListener(v -> {
            if (listener != null && userUid != null) {
                listener.onUpgradePlanClicked(userUid, userPlanId);
            }
            dismiss();
        });
        
        // Administración al tocar la tarjeta
        binding.cardAdmin.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAdminClicked();
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



