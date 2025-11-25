package com.example.gestorgastos.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gestorgastos.databinding.FragmentAdminBinding;
import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.ui.dialogs.EditUserDialog;
import java.util.ArrayList;
import java.util.List;

public class AdminFragment extends Fragment {
    private FragmentAdminBinding binding;
    private AdminViewModel viewModel;
    private UserAdapter adapter;
    private List<UserEntity> users = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        
        setupViews();
        observeViewModel();
    }
    
    private void setupViews() {
        // Configurar RecyclerView
        adapter = new UserAdapter();
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewUsers.setAdapter(adapter);
        
        // Configurar click listeners del adaptador
        adapter.setOnUserClickListener(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(UserEntity user) {
                // Mostrar detalles del usuario
                Toast.makeText(requireContext(), getString(R.string.toast_admin_user_prefix, user.name), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onUserEdit(UserEntity user) {
                showEditUserDialog(user);
            }
            
            @Override
            public void onUserDelete(UserEntity user) {
                // Mostrar confirmación antes de eliminar
                showDeleteConfirmation(user);
            }
        });
        
        // Configurar FAB para agregar usuario
        binding.fabAddUser.setOnClickListener(v -> {
            showAddUserDialog();
        });
        
        // Iniciar animación de pulsación del icono
        startFabPulseAnimation();
    }
    
    private void observeViewModel() {
        // Observar lista de usuarios
        viewModel.getAllUsers().observe(getViewLifecycleOwner(), usersList -> {
            users = usersList != null ? usersList : new ArrayList<>();
            adapter.submitList(users);
            
            // Mostrar/ocultar estado vacío
            if (users.isEmpty()) {
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewUsers.setVisibility(View.GONE);
            } else {
                binding.tvEmptyState.setVisibility(View.GONE);
                binding.recyclerViewUsers.setVisibility(View.VISIBLE);
            }
            
            Log.d("AdminFragment", "Usuarios actualizados: " + users.size());
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
        
        viewModel.getGeneratedPassword().observe(getViewLifecycleOwner(), password -> {
            if (password != null && !password.isEmpty()) {
                showGeneratedPasswordDialog(password);
                viewModel.clearGeneratedPassword();
            }
        });
    }
    
    private void startFabPulseAnimation() {
        if (binding != null && binding.ivFabIcon != null) {
            // Crear animación de escala
            android.view.animation.ScaleAnimation scaleAnimation = new android.view.animation.ScaleAnimation(
                1.0f, 1.4f, // fromX, toX
                1.0f, 1.4f, // fromY, toY
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
    
    private void showGeneratedPasswordDialog(String password) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Contraseña generada")
            .setMessage("Comparte esta contraseña temporal con el usuario:\n\n" + password)
            .setPositiveButton("Copiar", (dialog, which) -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Contraseña temporal", password);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), getString(R.string.toast_copy_password_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.toast_copy_password_error), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cerrar", null)
            .setCancelable(false)
            .show();
    }
    
    private void showEditUserDialog(UserEntity user) {
        EditUserDialog dialog = EditUserDialog.newInstance(user);
        dialog.setOnUserEditedListener(new EditUserDialog.OnUserEditedListener() {
            @Override
            public void onUserEdited(UserEntity editedUser, String password) {
                // Actualizar el usuario en la base de datos (password es null para ediciones)
                viewModel.updateUser(editedUser);
            }
            
            @Override
            public void onDialogCancelled() {
                // No hacer nada, solo cerrar el diálogo
            }
        });
        dialog.show(getParentFragmentManager(), EditUserDialog.TAG);
    }
    
    private void showAddUserDialog() {
        // Crear usuario nuevo con valores por defecto
        UserEntity newUser = new UserEntity();
        newUser.uid = ""; // Se generará en Firestore
        newUser.name = "";
        newUser.email = "";
        newUser.role = "user";
        newUser.planId = "free";
        newUser.zonaHoraria = com.example.gestorgastos.util.DateTimeUtil.getCurrentZoneId();
        newUser.isActive = true;
        newUser.updatedAt = com.example.gestorgastos.util.DateTimeUtil.getCurrentEpochMillis();
        
        EditUserDialog dialog = EditUserDialog.newInstance(newUser);
        dialog.setOnUserEditedListener(new EditUserDialog.OnUserEditedListener() {
            @Override
            public void onUserEdited(UserEntity editedUser, String password) {
                viewModel.createUser(editedUser, password);
            }
            
            @Override
            public void onDialogCancelled() {
                // No hacer nada
            }
        });
        dialog.show(getParentFragmentManager(), EditUserDialog.TAG);
    }
    
    private void showDeleteConfirmation(UserEntity user) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que deseas eliminar a " + user.name + "? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                viewModel.deleteUser(user.uid);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

