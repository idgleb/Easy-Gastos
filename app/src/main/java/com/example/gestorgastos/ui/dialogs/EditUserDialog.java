package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.gestorgastos.R;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class EditUserDialog extends DialogFragment {
    
    public static final String TAG = "EditUserDialog";
    
    private static final String ARG_USER_UID = "user_uid";
    private static final String ARG_USER_NAME = "user_name";
    private static final String ARG_USER_EMAIL = "user_email";
    private static final String ARG_USER_PLAN = "user_plan";
    private static final String ARG_USER_ROLE = "user_role";
    
    private UserEntity user;
    private boolean isNewUser;
    
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialAutoCompleteTextView etPlan;
    private MaterialAutoCompleteTextView etRole;
    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilPlan;
    private TextInputLayout tilRole;
    private MaterialButton btnCancel;
    private MaterialButton btnSave;
    private ImageButton btnClose;
    private TextView tvDialogTitle;
    
    private OnUserEditedListener listener;
    
    public interface OnUserEditedListener {
        void onUserEdited(UserEntity editedUser, String password);
        void onDialogCancelled();
    }
    
    public static EditUserDialog newInstance(UserEntity user) {
        EditUserDialog dialog = new EditUserDialog();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID, user.uid);
        args.putString(ARG_USER_NAME, user.name);
        args.putString(ARG_USER_EMAIL, user.email);
        args.putString(ARG_USER_PLAN, user.planId);
        args.putString(ARG_USER_ROLE, user.role);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Material3DialogTheme);
        
        if (getArguments() != null) {
            user = new UserEntity();
            user.uid = getArguments().getString(ARG_USER_UID);
            user.name = getArguments().getString(ARG_USER_NAME);
            user.email = getArguments().getString(ARG_USER_EMAIL);
            user.planId = getArguments().getString(ARG_USER_PLAN);
            user.role = getArguments().getString(ARG_USER_ROLE);
            user.zonaHoraria = com.example.gestorgastos.util.DateTimeUtil.getCurrentZoneId();
            user.isActive = true;
            user.updatedAt = com.example.gestorgastos.util.DateTimeUtil.getCurrentEpochMillis();
            
            // Determinar si es un usuario nuevo (UID vacío o null)
            isNewUser = user.uid == null || user.uid.trim().isEmpty();
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_user, container, false);
        
        // Inicializar vistas
        tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        tilName = view.findViewById(R.id.tilName);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilPlan = view.findViewById(R.id.tilPlan);
        tilRole = view.findViewById(R.id.tilRole);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etPlan = view.findViewById(R.id.etPlan);
        etRole = view.findViewById(R.id.etRole);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
        btnClose = view.findViewById(R.id.btnClose);
        
        // Configurar valores iniciales
        if (user != null) {
            etName.setText(user.name);
            etEmail.setText(user.email);
            etPlan.setText(user.planId);
            etRole.setText(user.role);
            
            // Mostrar campo de contraseña solo para usuarios nuevos
            if (isNewUser) {
                tilPassword.setVisibility(View.VISIBLE);
                tvDialogTitle.setText("Crear Usuario");
            } else {
                tilPassword.setVisibility(View.GONE);
                tvDialogTitle.setText("Editar Usuario");
            }
        }
        
        // Configurar dropdowns
        setupPlanDropdown();
        setupRoleDropdown();
        
        // Configurar listeners
        btnClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDialogCancelled();
            }
            dismiss();
        });
        
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDialogCancelled();
            }
            dismiss();
        });
        
        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveUser();
            }
        });
        
        return view;
    }
    
    private void setupPlanDropdown() {
        List<String> plans = new ArrayList<>();
        plans.add("free");
        plans.add("premium");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_list_item_1, plans);
        
        etPlan.setAdapter(adapter);
        etPlan.setOnClickListener(v -> etPlan.showDropDown());
    }
    
    private void setupRoleDropdown() {
        List<String> roles = new ArrayList<>();
        roles.add("user");
        roles.add("admin");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_list_item_1, roles);
        
        etRole.setAdapter(adapter);
        etRole.setOnClickListener(v -> etRole.showDropDown());
    }
    
    private boolean validateInput() {
        boolean isValid = true;
        
        if (etName.getText() == null || etName.getText().toString().trim().isEmpty()) {
            tilName.setError("El nombre es requerido");
            isValid = false;
        } else {
            tilName.setError(null);
        }
        
        if (etEmail.getText() == null || etEmail.getText().toString().trim().isEmpty()) {
            tilEmail.setError("El email es requerido");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString()).matches()) {
            tilEmail.setError("Email inválido");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }
        
        if (etPlan.getText() == null || etPlan.getText().toString().trim().isEmpty()) {
            tilPlan.setError("El plan es requerido");
            isValid = false;
        } else {
            tilPlan.setError(null);
        }
        
        if (etRole.getText() == null || etRole.getText().toString().trim().isEmpty()) {
            tilRole.setError("El rol es requerido");
            isValid = false;
        } else {
            tilRole.setError(null);
        }

        if (isNewUser) {
            String passwordInput = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (!passwordInput.isEmpty() && passwordInput.length() < 6) {
                tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
                isValid = false;
            } else {
                tilPassword.setError(null);
            }
        }
        
        return isValid;
    }
    
    private void saveUser() {
        if (user == null) {
            user = new UserEntity();
        }
        
        user.name = etName.getText().toString().trim();
        user.email = etEmail.getText().toString().trim();
        user.planId = etPlan.getText().toString().trim();
        user.role = etRole.getText().toString().trim();
        user.updatedAt = com.example.gestorgastos.util.DateTimeUtil.getCurrentEpochMillis();
        
        // Obtener contraseña (solo para usuarios nuevos)
        String password = null;
        if (isNewUser && etPassword.getText() != null) {
            password = etPassword.getText().toString().trim();
            // Si está vacía, se generará una contraseña temporal
            if (password.isEmpty()) {
                password = null;
            }
        }
        
        if (listener != null) {
            listener.onUserEdited(user, password);
        }
        dismiss();
    }
    
    public void setOnUserEditedListener(OnUserEditedListener listener) {
        this.listener = listener;
    }
}

